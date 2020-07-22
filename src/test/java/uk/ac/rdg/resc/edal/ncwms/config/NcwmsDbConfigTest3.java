package uk.ac.rdg.resc.edal.ncwms.config;

import org.junit.Test;
import uk.ac.rdg.resc.edal.catalogue.jaxb.DatasetConfig;
import uk.ac.rdg.resc.edal.catalogue.jaxb.VariableConfig;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

/**
 * These tests must be run with environment variable INDEX_DATABASE_PASSWORD set,
 * because this is a live database test (specifically, the config file specifies
 * ce_meta_ro@monsoon.pcic.uvic.ca/ce_meta). The database contents do not matter
 * (the config file specifies queries that return constant values), but the ability
 * to access the database does.
 *
 * TODO: Replace with DbUnit or other test database infrastructure.
 */
public class NcwmsDbConfigTest3 {
    @Test
    public void testLoadFromIndexDatabase() throws SQLException, JAXBException, IOException {
        // Read in the config file with database info
        URL url = this.getClass().getResource("/config-pcic-dummy.xml");
        File file = new File(url.getFile());
        NcwmsDbConfig config = NcwmsDbConfig.readFromFile(file);

        // Load info from the index database
        config.loadFromIndexDatabase();

        // Check results
        DatasetConfig[] datasets = config.getDatasets();
        assertEquals(1, datasets.length);
        DatasetConfig dataset = datasets[0];
        assertEquals("alpha", dataset.getId());
        assertEquals("/dummy/alpha.nc", dataset.getLocation());
        VariableConfig[] variables = dataset.getVariables();
        assertEquals(1, variables.length);
        VariableConfig variable = variables[0];
        assertEquals("foo", variable.getId());
        assertEquals("foo_name", variable.getTitle());
    }
}
