package uk.ac.rdg.resc.edal.ncwms.config;

import uk.ac.rdg.resc.edal.catalogue.jaxb.DatasetConfig;
import uk.ac.rdg.resc.edal.catalogue.jaxb.VariableConfig;

/*
 * Extends {@link DatasetConfig} with a constructor including all the dataset
 * attributes. FFS.
 */
public class DatasetFullConfig extends DatasetConfig {
    public DatasetFullConfig(
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
        super(variables);
        setId(id);
        setTitle(title);
        setLocation(location);
        setQueryable(queryable);
        setDownloadable(downloadable);
        setDataReaderClass(dataReaderClass);
        setCopyrightStatement(copyrightStatement);
        setMoreInfo(moreInfo);
        setDisabled(disabled);
        setUpdateInterval(updateInterval);
        setMetadataUrl(metadataUrl);
        setMetadataDesc(metadataDesc);
        setMetadataMimetype(metadataMimetype);
    }
}
