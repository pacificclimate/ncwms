package uk.ac.rdg.resc.edal.ncwms.config;

import org.apache.commons.lang.NotImplementedException;
import uk.ac.rdg.resc.edal.catalogue.jaxb.DatasetConfig;
import uk.ac.rdg.resc.edal.catalogue.jaxb.VariableConfig;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import static uk.ac.rdg.resc.edal.ncwms.config.DatasetConfigFactory.makeDatasetConfig;

public class ResultSetToDatasetConfig implements TypeTransformer<ResultSet, DatasetConfig, ArrayList<VariableConfig>> {
    public static final String DATASET_ID_COLUMN_NAME = "dataset_id";
    public static final String LOCATION_COLUMN_NAME = "location";

    // Fixed values for making a DatasetConfig
    private boolean queryable = true;
    private boolean downloadable = false;
    private String dataReaderClass = "";
    private String copyrightStatement = "";
    private String moreInfo = "";
    private boolean disabled = false;
    private int updateInterval = -1;
    private String metadataUrl = null;
    private String metadataDesc = null;
    private String metadataMimetype = null;

    public ResultSetToDatasetConfig() {
    }

    public ResultSetToDatasetConfig(
            boolean queryable,
            boolean downloadable,
            String dataReaderClass,
            String copyrightStatement,
            String moreInfo,
            boolean disabled,
            int updateInterval,
            String metadataUrl,
            String metadataDesc,
            String metadataMimetype
    ) {
        this.queryable = queryable;
        this.downloadable = downloadable;
        this.dataReaderClass = dataReaderClass;
        this.copyrightStatement = copyrightStatement;
        this.moreInfo = moreInfo;
        this.disabled = disabled;
        this.updateInterval = updateInterval;
        this.metadataUrl = metadataUrl;
        this.metadataDesc = metadataDesc;
        this.metadataMimetype = metadataMimetype;
    }

    @Override
    public DatasetConfig make(ResultSet from) throws SQLException {
        throw new NotImplementedException();
    }

    @Override
    public DatasetConfig make(ResultSet from, ArrayList<VariableConfig> variables) throws SQLException {
        String id = from.getString(DATASET_ID_COLUMN_NAME);
        String location = from.getString(LOCATION_COLUMN_NAME);
        // This is why we can't have nice things.
        VariableConfig[] variableConfigs = variables.toArray(
                new VariableConfig[variables.size()]
        );
        return makeDatasetConfig(
                id,
                id,
                location,
                queryable,
                downloadable,
                dataReaderClass,
                copyrightStatement,
                moreInfo,
                disabled,
                updateInterval,
                metadataUrl,
                metadataDesc,
                metadataMimetype,
                variableConfigs
        );
    }
}
