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
import java.util.Properties;

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
    private NcwmsIndexDatabase indexDatabase = new NcwmsIndexDatabase();

    /*
     * Used for JAX-B
     */
    @SuppressWarnings("unused")
    protected NcwmsDbConfig() {}

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
        NcwmsIndexDatabase indexDatabase
    ) {
        super(datasets, dynamicServices, contact, serverInfo, cacheInfo, crsCodes);
        this.indexDatabase = indexDatabase;
    }

    public NcwmsIndexDatabase getIndexDatabase() { return indexDatabase; }

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
                    new NcwmsIndexDatabase()
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
     * But reading in the XML doesn't actually do anything with this information.
     * This method does.
     *
     * It's not clear that this shouldn't be part of {@link #readFromFile}.
     */
    public void loadFromIndexDatabase() throws SQLException {
        // Set up a dummy DatasetStorage to handle loaded datasets.
        // Christ only knows what we should actually be doing here.
        setDatasetLoadedHandler(new DummyDatasetStorage());

        // Let's start fake: Get the new "datasets" from a bogus element
        // <result> in the <indexDataset> element. We'll decode it from JSON!
        // And we'll just specify a few parameters with it.
//        JSONObject datasetParams = new JSONObject(getIndexDatabase().getResult());
//        String id = datasetParams.getString("id");
//        String location = datasetParams.getString("location");
//        VariableConfig[] variables = new VariableConfig[]{};
//        DatasetConfig dataset = makeDatasetConfig(
//                id, id, location, true, false, "", "", "", false, -1, null, null, null, variables
//        );
//        addDataset(dataset);

        // Now we're going to get seriouser.
        //
        // We will need to do the following things:
        // - Establish a connection to the database.
        // - Issue at least one query that returns a list of information
        //   defining the "datasets" in the database.
        // - Convert each element of the list of dataset information to a
        //   DatasetConfig.
        // - Add each DatasetConfig to this object.

        // Establish connection to database
        Properties connectionProperties = new Properties();
        connectionProperties.setProperty("password", System.getenv("INDEX_DATABASE_PASSWORD"));
        NcwmsIndexDatabase indexDatabase = getIndexDatabase();
        try (
                Connection connection = DriverManager.getConnection(
                        "jdbc:" + indexDatabase.getUrl(),
                        connectionProperties
                )
        )
        {
            Statement statement = connection.createStatement();

            // Get the results of a query that defines datasets.
            // The SQL for this query is defined externally in the config file in the tag
            // <datasetsQuery>. This query must return one row per dataset.
            // Each row must have columns named
            // - `dataset_id`
            // - `location`
            // The results must be sorted in ascending order of `dataset_id`.
            ResultSet datasetRs = statement.executeQuery(indexDatabase.getDatasetsQuery());

            // Get the results of a query that defines variables.
            // The SQL for this query is defined externally in the config file in the tag
            // <variablesQuery>. This query must return one row per variable.
            // Each row must have columns named
            // - `dataset_id`
            // - `variable_id`
            // - `variable_name`
            // - `range_min`
            // - `range_max`
            // The results must be sorted in ascending order of `dataset_id`.
            ResultSet variableRs = statement.executeQuery(indexDatabase.getVariablesQuery());

            ResultSetToDatasetConfig datasetConfigMaker = new ResultSetToDatasetConfig();
            ResultSetToVariableConfig variableConfigMaker = new ResultSetToVariableConfig();
            ResultSetRunGrouper<DatasetConfig, VariableConfig> grouper =
                    new ResultSetRunGrouper<>("id", datasetConfigMaker, variableConfigMaker);

            ArrayList<DatasetConfig> datasets = grouper.group(datasetRs, variableRs);
            for (DatasetConfig dataset: datasets) {
                addDataset(dataset);
            }
        }
    }
}
