package uk.ac.rdg.resc.edal.ncwms.config;

import uk.ac.rdg.resc.edal.catalogue.jaxb.DatasetConfig;
import uk.ac.rdg.resc.edal.catalogue.jaxb.VariableConfig;

/**
 * Adds factories for {@link DatasetConfig}.
 */
public class DatasetConfigFactory {
    public static DatasetConfig makeDatasetConfig(
            String id,
            String title,
            String location,
            boolean queryable,
            boolean downloadable,
            String dataReaderClass,
            String copyrightStatement,
            String moreInfo,
            boolean disabled,
            int updateInterval,
            String metadataUrl,
            String metadataDesc,
            String metadataMimetype,
            VariableConfig[] variables
    ) {
        DatasetConfig dataset = new DatasetConfig(variables);
        dataset.setId(id);
        dataset.setTitle(title);
        dataset.setLocation(location);
        dataset.setQueryable(queryable);
        dataset.setDownloadable(downloadable);
        dataset.setDataReaderClass(dataReaderClass);
        dataset.setCopyrightStatement(copyrightStatement);
        dataset.setMoreInfo(moreInfo);
        dataset.setDisabled(disabled);
        dataset.setUpdateInterval(updateInterval);
        dataset.setMetadataUrl(metadataUrl);
        dataset.setMetadataDesc(metadataDesc);
        dataset.setMetadataMimetype(metadataMimetype);
        return dataset;
    }
}
