package uk.ac.rdg.resc.edal.ncwms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import uk.ac.rdg.resc.edal.catalogue.jaxb.CacheInfo;
import uk.ac.rdg.resc.edal.catalogue.jaxb.DatasetConfig;

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
}
