package uk.ac.rdg.resc.edal.ncwms.config;

public interface NcwmsDatabaseDynamicServicesInfo {
    NcwmsIndexDatabase getIndexDatabase();
    NcwmsDynamicService[] getDynamicServices();
}
