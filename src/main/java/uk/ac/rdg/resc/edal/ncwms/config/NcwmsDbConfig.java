package uk.ac.rdg.resc.edal.ncwms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.rdg.resc.edal.catalogue.jaxb.CacheInfo;
import uk.ac.rdg.resc.edal.catalogue.jaxb.DatasetConfig;
import uk.ac.rdg.resc.edal.catalogue.jaxb.VariableConfig;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.sql.*;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Deals purely with the (de)serialisation of the ncWMS config file. This
 * extends {@link NcwmsConfig} to add index database interfacing.
 *
 * @author Rod Glover
 */
@XmlType(propOrder = { "indexDatabase" })
@XmlRootElement(name = "config")
public class NcwmsDbConfig extends NcwmsConfig {
    private static final Logger log = LoggerFactory.getLogger(NcwmsDbConfig.class);

    @XmlElement(name="indexDatabase")
    private NcwmsIndexDatabaseConfig indexDatabase = new NcwmsIndexDatabaseConfig();

    /*
     * Used for JAX-B
     */
    @SuppressWarnings("unused")
    public NcwmsDbConfig() {}

    public NcwmsDbConfig(File configFile) throws IOException, JAXBException {
        super(configFile);
    }

    public NcwmsDbConfig(
        DatasetConfig[] datasets,
        NcwmsDynamicService[] dynamicServices,
        NcwmsContact contact,
        NcwmsServerInfo serverInfo,
        CacheInfo cacheInfo,
        NcwmsSupportedCrsCodes crsCodes,
        NcwmsIndexDatabaseConfig indexDatabase
    ) {
        super(datasets, dynamicServices, new NcwmsDatabaseDynamicServicesConfig(), contact, serverInfo, cacheInfo, crsCodes);
        this.indexDatabase = indexDatabase;
    }

    public NcwmsIndexDatabaseConfig getIndexDatabase() { return indexDatabase; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("\n");
        sb.append("Index Database Info\n");
        sb.append("--------------------\n");
        sb.append(indexDatabase.toString());
        sb.append("\n");
        return sb.toString();
    }

    public static NcwmsDbConfig deserialise(Reader xmlConfig) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(NcwmsDbConfig.class);

        Unmarshaller unmarshaller = context.createUnmarshaller();
        Source source = new StreamSource(xmlConfig);
        NcwmsDbConfig config = unmarshaller.unmarshal(source, NcwmsDbConfig.class).getValue();
        return config;
    }

    public static NcwmsDbConfig readFromFile(File configFile) throws JAXBException, IOException {
        NcwmsDbConfig config;
        if (!configFile.exists()) {
            /*
             * If the file doesn't exist, create it with some default values
             */
            log.warn("No config file exists in the given location (" + configFile.getAbsolutePath()
                    + ").  Creating one with defaults");
            config = new NcwmsDbConfig(
                    new DatasetConfig[0],
                    new NcwmsDynamicService[0],
                    new NcwmsContact(),
                    new NcwmsServerInfo(),
                    new CacheInfo(),
                    new NcwmsSupportedCrsCodes(),
                    new NcwmsIndexDatabaseConfig()
            );
            config.configFile = configFile;
            config.save();
        } else {
            /*
             * Otherwise read the file
             */
            config = deserialise(new FileReader(configFile));
            config.configFile = configFile;
        }
        return config;
    }

    /**
     * Load datasets from index database. The config read from XML contains an
     * <indexDatabase> section that specifies how to get data from the database.
     * Just reading in the XML doesn't actually do anything with this information.
     * This method does.
     * 
     * Loading datasets is done by issuing two queries via jdbc:
     * 
     * 1. The datasets query (tag <datasetsQuery>). This query must return 
     *    one row per dataset. Each row must have columns named
     *    - `dataset_id`
     *    - `location`
     *    The results must be sorted in ascending order of `dataset_id`.
     *    
     * 2. The variables query (tag <variablesQuery>). This query must return
     *    one row per variable. Each row must have columns named
     *    - `dataset_id`
     *    - `variable_id`
     *    - `variable_name`
     *    - `range_min`
     *    - `range_max`
     *    The results must be sorted in ascending order of `dataset_id`.
     *
     */
    public void loadFromIndexDatabase() throws SQLException {
        // This is one way to ensure that the needed driver(s) (in this case,
        // postgresql) are available in the runtime. There are likely better
        // ways to do this, but I haven't the patience at the moment.
        // TODO: Do this better.
        try {
            Class.forName("org.postgresql.Driver");
            log.debug("org.postgresql.Driver found");
        } catch (ClassNotFoundException e) {
            log.error("No jdbc postgresql driver");
            return;
        }

        log.debug("Drivers available");
        final Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            final Driver driver = drivers.nextElement();
            log.debug(String.format("- %s", driver.getClass().getName()));
        }

        // Set up a dummy DatasetStorage to handle loaded datasets.
        // Christ only knows what we should actually be doing here.
        // TODO: Figure this out!!!
        setDatasetLoadedHandler(new DummyDatasetStorage());

        NcwmsIndexDatabaseConfig indexDatabase = getIndexDatabase();
        final String dbUrl = indexDatabase.getUrl();
        if (dbUrl == null || dbUrl.equals("")) {
            // TODO: Replace with throw?
            log.warn("Index database URL is null or empty");
            return;
        }

        final String password = System.getenv("INDEX_DATABASE_PASSWORD");
        if (password == null) {
            // TODO: Replace with throw?
            log.warn("Environment variable INDEX_DATABASE_PASSWORD should be specified, is not");
        }

        try (
            // Establish connection to index database
            Connection connection = DriverManager.getConnection(
                "jdbc:" + dbUrl,
                indexDatabase.getUsername(),
                password
            )
        ) {
            // Issue database queries
            ResultSet datasetRs = connection.createStatement()
                .executeQuery(indexDatabase.getDatasetsQuery());
            ResultSet variableRs = connection.createStatement()
                .executeQuery(indexDatabase.getVariablesQuery());

            // Construct configurations from query results
            ResultSetToDatasetConfig datasetConfigMaker = new ResultSetToDatasetConfig();
            ResultSetToVariableConfig variableConfigMaker = new ResultSetToVariableConfig();
            ResultSetRunGrouper<DatasetConfig, VariableConfig> grouper =
                new ResultSetRunGrouper<>(
                    ResultSetToDatasetConfig.DATASET_ID_COLUMN_NAME,
                    datasetConfigMaker,
                    variableConfigMaker
                );
            ArrayList<DatasetConfig> datasets = grouper.group(datasetRs, variableRs);

            // Add resulting dataset configurations to the ncwms config
            for (DatasetConfig dataset: datasets) {
                addDataset(dataset);
            }
        }
    }
}
