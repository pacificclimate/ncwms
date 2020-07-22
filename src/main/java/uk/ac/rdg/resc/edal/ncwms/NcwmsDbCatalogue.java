package uk.ac.rdg.resc.edal.ncwms;

import uk.ac.rdg.resc.edal.ncwms.config.NcwmsDbConfig;
import uk.ac.rdg.resc.edal.wms.WmsCatalogue;

import java.io.IOException;

/**
 * This class extends {@link NcwmsCatalogue} to accept configuration values
 * of type {@link NcwmsDbCatalogue}. It is very simple because the extension
 * is of {@link NcwmsDbCatalogue} only adds (not modifies) attributes and
 * methods.
 */
public class NcwmsDbCatalogue extends NcwmsCatalogue implements WmsCatalogue {
    public NcwmsDbCatalogue() {
        super();
    }

    public NcwmsDbCatalogue(NcwmsDbConfig config) throws IOException {
        super(config);
    }

    /**
     * @return The NcwmsDbConfig object used by this catalogue.
     */
    public NcwmsDbConfig getConfig() {
        return (NcwmsDbConfig) super.getConfig();
    }
}
