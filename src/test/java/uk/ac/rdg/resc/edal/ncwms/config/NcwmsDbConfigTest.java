package uk.ac.rdg.resc.edal.ncwms.config;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import org.xml.sax.SAXException;
import uk.ac.rdg.resc.edal.catalogue.jaxb.CacheInfo;
import uk.ac.rdg.resc.edal.catalogue.jaxb.DatasetConfig;
import uk.ac.rdg.resc.edal.catalogue.jaxb.VariableConfig;
import uk.ac.rdg.resc.edal.util.Extents;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.net.URL;
import java.util.Arrays;

public class NcwmsDbConfigTest {
    private NcwmsDbConfig config;

    @Before
    public void setUp() throws Exception {
        VariableConfig[] variables = new VariableConfig[]{new VariableConfig("varId", "A Variable", "A data-related quantity",
                Extents.newExtent(-10f, 10f), "redblue", null, null, null, "linear", 250)};

        DatasetConfig dataset = new DatasetConfig(variables);
        dataset.setId("datasetId");
        dataset.setTitle("A Dataset");
        dataset.setLocation("/home/guy/Data/FOAM_ONE/FOAM_one.ncml");
        dataset.setCopyrightStatement("copyright message");
        dataset.setDisabled(false);
        dataset.setDownloadable(true);
        dataset.setMetadataDesc("this is metadata");
        dataset.setMetadataMimetype("text/xml");
        dataset.setMetadataUrl("http://www.google.com");
        dataset.setMoreInfo("more info");
        dataset.setQueryable(true);
        DatasetConfig[] datasets = new DatasetConfig[] { dataset };

        NcwmsContact contact = new NcwmsContact("Guy", "ReSC", "5217", "g.g");

        NcwmsServerInfo serverInfo = new NcwmsServerInfo("servername", true, 100, 50,
                "a fake server", Arrays.asList("fake", "con", "front"), "http://google.com",
                true);
        CacheInfo cacheInfo = new CacheInfo(true, 2000, 10.0f);
        String[] codes = {"CRS:187", "EPSG:187"};
        NcwmsSupportedCrsCodes crsCodes = new NcwmsSupportedCrsCodes(codes);

        NcwmsIndexDatabase indexDatabase = new NcwmsIndexDatabase("modelmeta", "result");

        config = new NcwmsDbConfig(
                datasets,
                new NcwmsDynamicService[0],
                contact,
                serverInfo,
                cacheInfo,
                crsCodes,
                indexDatabase
        );
    }

    @Test
    public void testBasics() {
        // Just, you know, in case ...
        DatasetConfig[] datasets = config.getDatasets();
        assertEquals(datasets.length, 1);
        DatasetConfig dataset = datasets[0];
        assertEquals(dataset.getId(), "datasetId");
    }

    @Test
    public void testDatabaseInfo() {
        NcwmsIndexDatabase indexDatabase = config.getIndexDatabase();
        String name = indexDatabase.getName();
        assertEquals(name, "modelmeta");
    }

    @Test
    public void testSerialise() throws JAXBException {
        StringWriter serialiseWriter = new StringWriter();
        config.serialise(serialiseWriter);
        String serialise = serialiseWriter.toString();
        System.out.println(serialise);
    }

    // TODO: Remove?
    @Test
    public void testDeserialise() throws JAXBException, SAXException, FileNotFoundException {
        NcwmsDbConfig config = NcwmsDbConfig.deserialise(new StringReader(XML));
//        System.out.println(config);
        assertEquals(config.getContactInfo().getName(), "Guy");
        assertEquals(config.getServerInfo().getName(), "servername");
        assertEquals(config.getIndexDatabase().getName(), "marvellous");
    }

    private final static String XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<config>" +
                "<contact>" +
                    "<name>Guy</name>" +
                    "<organization>ReSC</organization>" +
                    "<telephone>5217</telephone>" +
                    "<email>g.g</email>" +
                "</contact>" +
                "<server>" +
                    "<title>servername</title>" +
                    "<allowFeatureInfo>true</allowFeatureInfo>" +
                    "<maxImageWidth>100</maxImageWidth>" +
                    "<maxImageHeight>50</maxImageHeight>" +
                    /*
                    "<abstract>a fake server</abstract>" +
                    "<keywords>fake, con, front</keywords>"
                    */
                    "<url>http://google.com</url>" +
                    "<adminpassword>ncWMS</adminpassword>" +
                    "<allowglobalcapabilities>true</allowglobalcapabilities>" +
                "</server>" +
                "<indexDatabase>" +
                    "<name>marvellous</name>" +
                "</indexDatabase>" +
            "</config>";

    @Test
    public void testReadFromFile() throws JAXBException, IOException {
        URL url = this.getClass().getResource("/dbconfig.xml");
        File file = new File(url.getFile());
        NcwmsDbConfig config = NcwmsDbConfig.readFromFile(file);
//        System.out.println(config);
        assertEquals(config.getContactInfo().getName(), "Guy");
        assertEquals(config.getServerInfo().getName(), "servername");
        assertEquals(config.getIndexDatabase().getName(), "marvellous");
    }

    @Test
    public void testLoadFromIndexDatabase()  throws JAXBException, IOException {
        URL url = this.getClass().getResource("/dbconfig.xml");
        File file = new File(url.getFile());
        NcwmsDbConfig config = NcwmsDbConfig.readFromFile(file);
        config.loadFromIndexDatabase();
        DatasetConfig[] datasets = config.getDatasets();
        assertEquals(datasets.length, 1);
        DatasetConfig dataset = datasets[0];
        assertEquals(dataset.getId(), "id");
        assertEquals(dataset.getLocation(), "location");
    }
}
