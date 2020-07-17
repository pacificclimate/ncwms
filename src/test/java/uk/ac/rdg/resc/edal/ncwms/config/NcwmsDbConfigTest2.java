package uk.ac.rdg.resc.edal.ncwms.config;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import uk.ac.rdg.resc.edal.catalogue.jaxb.DatasetConfig;
import uk.ac.rdg.resc.edal.catalogue.jaxb.VariableConfig;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.Properties;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * This test adapted from https://stackoverflow.com/a/44627516
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ NcwmsDbConfig.class, DriverManager.class, Connection.class })
public class NcwmsDbConfigTest2 {
    @Mock
    Connection connection;

    @Mock
    Statement statement;

    // Set up mock ResultSets for datasets and variables queries.
    private static final String[] datasetColumns = {"dataset_id", "location"};
    private static final Object[][] datasetRows = {
        {"1", "/location/1"},
        {"2", "/location/2"},
        {"3", "/location/3"},
    };
    private static final String[] variableColumns =
        {"dataset_id", "variable_id", "variable_name", "range_min", "range_max"};
    private static final Object[][] variableRows = {
        {"1", "11", "var11", 0, 10},
        {"2", "12", "var12", 0, 10},
        {"3", "13", "var13", 0, 10},
    };

    private static ResultSet datasetsResultSet = null;
    private static ResultSet variablesResultSet = null;
    static {
        try {
            datasetsResultSet = MockResultSet.create(datasetColumns, datasetRows);
            variablesResultSet = MockResultSet.create(variableColumns, variableRows);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    // Set up to "mock" environment variables
    @Rule
    public final EnvironmentVariables environmentVariables =
        new EnvironmentVariables();

    @Test
    public void testLoadFromIndexDatabase() throws SQLException, JAXBException, IOException {
        // Mock database connection
        PowerMockito.mockStatic(DriverManager.class);
        PowerMockito.when(DriverManager.getConnection(
            Mockito.eq("jdbc:postgresql://test"),
            Mockito.any(Properties.class)   // Properties object with password
        )).thenReturn(connection);

        // Mock statement creation
        Mockito.when(connection.createStatement()).thenReturn(statement);

        // Mock query executions
        Mockito.when(statement.executeQuery(Mockito.eq("SELECT datasets")))
            .thenReturn(datasetsResultSet);
        Mockito.when(statement.executeQuery(Mockito.eq("SELECT variables")))
            .thenReturn(variablesResultSet);

        // "Mock" environment variables
        environmentVariables.set("INDEX_DATABASE_PASSWORD", "password");

        // Read in the config file with database info
        URL url = this.getClass().getResource("/dbconfig.xml");
        File file = new File(url.getFile());
        NcwmsDbConfig config = NcwmsDbConfig.readFromFile(file);

        // Load info from the (mocked) index database
        config.loadFromIndexDatabase();

        // Verify expected jdbc calls
        Mockito.verify(connection).createStatement();
        Mockito.verify(statement).executeQuery(Mockito.eq("SELECT datasets"));
        Mockito.verify(statement).executeQuery(Mockito.eq("SELECT variables"));

        // Check results
        DatasetConfig[] datasets = config.getDatasets();
        assertEquals(datasetRows.length, datasets.length);
        // Oh for zip. Sigh.
        for (int i = 0; i < datasets.length; i += 1) {
            DatasetConfig dataset = datasets[i];
            final Object[] datasetRow = datasetRows[i];
            assertEquals(datasetRow[0], dataset.getId());
            assertEquals(datasetRow[1], dataset.getLocation());

            // Note: The following depends on a simple 1:1 correspondence
            // between datasetRows and variableRows. Not very realistic, but
            // the association algorithm is tested elsewhere.
            VariableConfig[] variables = dataset.getVariables();
            assertEquals(1, variables.length);
            VariableConfig variable = variables[0];
            Object[] variableRow = variableRows[i];
            assertEquals(variableRow[1], variable.getId());
            assertEquals(variableRow[2], variable.getTitle());
        }
    }
}
