package uk.ac.rdg.resc.edal.ncwms.config;

import uk.ac.rdg.resc.edal.catalogue.jaxb.CatalogueConfig;
import uk.ac.rdg.resc.edal.catalogue.jaxb.VariableConfig;
import uk.ac.rdg.resc.edal.dataset.Dataset;

import java.util.Collection;

public class DummyDatasetStorage implements CatalogueConfig.DatasetStorage {

    @Override
    public void datasetLoaded(Dataset dataset, Collection<VariableConfig> collection) {

    }
}
