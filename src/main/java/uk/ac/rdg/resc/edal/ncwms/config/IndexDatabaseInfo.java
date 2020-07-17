package uk.ac.rdg.resc.edal.ncwms.config;

public interface IndexDatabaseInfo {
    String getName();
    String getResult();
    String getUrl();
    String getDatasetsQuery();
    String getVariablesQuery();
    String getDatasetIdColumnName();
}
