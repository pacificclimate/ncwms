package uk.ac.rdg.resc.edal.ncwms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.Map;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class NcwmsDatabaseDynamicServicesConfig implements NcwmsDatabaseDynamicServicesInfo {
    private static final Logger log = LoggerFactory.getLogger(NcwmsDatabaseDynamicServicesConfig.class);

    @XmlElement(name = "indexDatabase")
    private NcwmsIndexDatabase indexDatabase = new NcwmsIndexDatabase();

    // dsInternal stores the dynamic services as a HashMap of NcwmsDynamicService.
    // Interfaces deal only in arrays of NcwmsDynamicService, but we store internally
    // as a hashmap. Don't ask why. TODO: Ask why.
    // It is tempting to name dsInternal dynamicServices, but this causes an
    // IllegalAnnotationsException: Class has two properties of the same name.
    private Map<String, NcwmsDynamicService> dsInternal = new LinkedHashMap<String, NcwmsDynamicService>();
    @XmlElement(name = "dynamicService", required = false)
    public void setDynamicServices(NcwmsDynamicService[] dynamicServices) {
        dsInternal = new LinkedHashMap<String, NcwmsDynamicService>();
        for (NcwmsDynamicService dynamicService : dynamicServices) {
            dsInternal.put(dynamicService.getAlias(), dynamicService);
        }
    }

    NcwmsDatabaseDynamicServicesConfig() {
    }

    public NcwmsDatabaseDynamicServicesConfig(
        NcwmsIndexDatabase indexDatabase, NcwmsDynamicService[] dynamicServices
    ) {
        super();
        this.indexDatabase = indexDatabase;
        setDynamicServices(dynamicServices);
    }

    @Override
    public NcwmsIndexDatabase getIndexDatabase() {
        return indexDatabase;
    }

    @Override
    public NcwmsDynamicService[] getDynamicServices() {
        return dsInternal.values().toArray(new NcwmsDynamicService[0]);
    }
}
