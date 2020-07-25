package uk.ac.rdg.resc.edal.ncwms.config;

public interface NcwmsDatabaseDynamicServicesInfo {
    NcwmsIndexDatabaseConfig getIndexDatabase();
    NcwmsDynamicService[] getDynamicServices();
}
