package uk.ac.rdg.resc.edal.ncwms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsIndexDatabaseConfig;

import java.sql.*;

/**
 * Encapsulates index database operations.
 */
public class NcwmsIndexDatabase {
    private static final Logger log = LoggerFactory.getLogger(NcwmsIndexDatabase.class);

    private NcwmsIndexDatabaseConfig config = null;
    private Connection connection = null;

    public NcwmsIndexDatabase(NcwmsIndexDatabaseConfig config) {
        this.config = config;
    }

    public NcwmsIndexDatabaseConfig getConfig() {
        return config;
    }

    private Connection getConnection() throws SQLException, ClassNotFoundException {
        if (connection != null) {
            return connection;
        }

        // TODO: Get driver class from config
        // Class.forName(config.getDriverClass());
        Class.forName("org.postgresql.Driver");
        log.debug("Database driver found, yay.");

        final String dbUrl = config.getUrl();
        if (dbUrl == null || dbUrl.equals("")) {
            // TODO: Replace with throw?
            log.warn("Index database URL is null or empty");
        }

        // TODO: Get pwd env var name from config?
        final String password = System.getenv("INDEX_DATABASE_PASSWORD");
        if (password == null) {
            // TODO: Replace with throw?
            log.warn("Environment variable INDEX_DATABASE_PASSWORD should be specified, is not");
        }

        // TODO: Is this try necessary?
        try (
            Connection connection = DriverManager.getConnection(
                "jdbc:" + dbUrl,
                config.getUsername(),
                password
            );
        )
        {
            this.connection = connection;
            return connection;
        }
    }

    public String getLocationFromId(String id) throws Exception {
        PreparedStatement statement = getConnection()
            .prepareStatement(config.getIdToLocationQuery());
        statement.setString(1, id);
        ResultSet results = statement.executeQuery();
        if (!results.next()) {
            throw new Exception("Could not map Id to Location");
        }
        return results.getString("location");
    }
}
