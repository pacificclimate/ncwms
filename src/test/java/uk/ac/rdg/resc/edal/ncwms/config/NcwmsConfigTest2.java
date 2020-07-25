package uk.ac.rdg.resc.edal.ncwms.config;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class NcwmsConfigTest2 {
    private NcwmsDbConfig config;

    @Before
    public void setUp() throws Exception {
        URL url = this.getClass().getResource("/dbconfig.xml");
        File file = new File(url.getFile());
        config = NcwmsDbConfig.readFromFile(file);
    }

    @Test
    public void testContactInfo() {
        final NcwmsContact contactInfo = config.getContactInfo();
        assertEquals(contactInfo.getName(), "Guy");
        assertEquals(contactInfo.getOrganisation(), "ReSC");
    }

    @Test
    public void testServerInfo() {
        final NcwmsServerInfo serverInfo = config.getServerInfo();
        assertEquals(serverInfo.getName(), "servername");
        assertEquals(serverInfo.getUrl(), "http://google.com");
        assertEquals(serverInfo.getMaxImageWidth(), 100);
        assertEquals(serverInfo.getMaxImageHeight(), 50);
    }

    @Test
    public void testCrsCodes() {
        final NcwmsSupportedCrsCodes crsCodes = config.getSupportedNcwmsCrsCodes();
        assertArrayEquals(crsCodes.getSupportedCrsCodes(), new String[] {"EPSG:9999"});
    }

    @Test
    public void testDynamicServices() {
        final NcwmsDynamicService[] dynamicServices = config.getDynamicServices();
        assertEquals(1, dynamicServices.length);
        final NcwmsDynamicService dynamicService = dynamicServices[0];
        assertEquals("alpha", dynamicService.getAlias());
        assertEquals("/path/to/alpha", dynamicService.getServicePath());
        assertEquals(".*", dynamicService.getDatasetIdMatch());
        assertEquals(false, dynamicService.isDisabled());
        assertEquals(true, dynamicService.isQueryable());
        assertEquals(false, dynamicService.isDownloadable());
    }

    @Test
    public void testDatabaseDynamicServices() {
        final NcwmsDatabaseDynamicServicesConfig databaseDynamicServices = config.getDatabaseDynamicServices();

        final NcwmsIndexDatabaseConfig indexDatabase = databaseDynamicServices.getIndexDatabase();
        assertEquals("for database dynamic services", indexDatabase.getName());
        assertEquals("postgresql://yowza", indexDatabase.getUrl());
        assertEquals("gollum", indexDatabase.getUsername());
        assertEquals("SELECT location", indexDatabase.getIdToLocationQuery());

        final NcwmsDynamicService[] dynamicServices = databaseDynamicServices.getDynamicServices();
        final String[] names = {"alpha", "beta", "gamma"};
        assertEquals(names.length, dynamicServices.length);
        // Alas, still no zip.
        for (int i = 0; i < names.length; i += 1) {
            String name = names[i];
            NcwmsDynamicService dynamicService = dynamicServices[i];
            assertEquals(name, dynamicService.getAlias());
            assertEquals(String.format("/path/to/%s", name), dynamicService.getServicePath());
        }
    }
}
