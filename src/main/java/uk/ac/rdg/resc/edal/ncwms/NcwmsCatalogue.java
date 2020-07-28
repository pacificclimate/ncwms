/*******************************************************************************
 * Copyright (c) 2013 The University of Reading
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package uk.ac.rdg.resc.edal.ncwms;

import java.io.IOException;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import uk.ac.rdg.resc.edal.cache.EdalCache;
import uk.ac.rdg.resc.edal.catalogue.DataCatalogue;
import uk.ac.rdg.resc.edal.catalogue.jaxb.DatasetConfig;
import uk.ac.rdg.resc.edal.catalogue.jaxb.VariableConfig;
import uk.ac.rdg.resc.edal.dataset.Dataset;
import uk.ac.rdg.resc.edal.dataset.DatasetFactory;
import uk.ac.rdg.resc.edal.exceptions.EdalException;
import uk.ac.rdg.resc.edal.graphics.exceptions.EdalLayerNotFoundException;
import uk.ac.rdg.resc.edal.graphics.utils.ColourPalette;
import uk.ac.rdg.resc.edal.graphics.utils.EnhancedVariableMetadata;
import uk.ac.rdg.resc.edal.graphics.utils.LayerNameMapper;
import uk.ac.rdg.resc.edal.graphics.utils.PlottingStyleParameters;
import uk.ac.rdg.resc.edal.graphics.utils.SimpleLayerNameMapper;
import uk.ac.rdg.resc.edal.graphics.utils.SldTemplateStyleCatalogue;
import uk.ac.rdg.resc.edal.graphics.utils.StyleCatalogue;
import uk.ac.rdg.resc.edal.metadata.VariableMetadata;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsConfig;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsDynamicCacheInfo;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsDynamicService;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsSupportedCrsCodes;
import uk.ac.rdg.resc.edal.wms.WmsCatalogue;
import uk.ac.rdg.resc.edal.wms.util.ContactInfo;
import uk.ac.rdg.resc.edal.wms.util.ServerInfo;

import static uk.ac.rdg.resc.edal.ncwms.util.FileLocation.pathJoinStrict;

/**
 * An extension of {@link DataCatalogue} to add WMS-specific capabilities for
 * ncWMS.
 *
 * @author Guy Griffiths
 */
public class NcwmsCatalogue extends DataCatalogue implements WmsCatalogue {
    private StyleCatalogue styleCatalogue;

    private static final String CACHE_NAME = "dynamicDatasetCache";
    private static final MemoryStoreEvictionPolicy EVICTION_POLICY = MemoryStoreEvictionPolicy.LFU;
    private static final PersistenceConfiguration.Strategy PERSISTENCE_STRATEGY = PersistenceConfiguration.Strategy.NONE;
    private static final CacheConfiguration.TransactionalMode TRANSACTIONAL_MODE = CacheConfiguration.TransactionalMode.OFF;
    private Cache dynamicDatasetCache;
    private boolean dynamicCacheEnabled = true;

    private NcwmsIndexDatabase ncwmsIndexDatabase = null;

    public NcwmsCatalogue() {
        super();
    }

    public NcwmsCatalogue(NcwmsConfig config) throws IOException {
        super(config, new SimpleLayerNameMapper());
        this.styleCatalogue = SldTemplateStyleCatalogue.getStyleCatalogue();

        dynamicCacheEnabled = config.getDynamicCacheInfo().isEnabled();

        if (dynamicCacheEnabled) {
            if (EdalCache.cacheManager.cacheExists(CACHE_NAME) == false) {
                NcwmsDynamicCacheInfo cacheInfo = config.getDynamicCacheInfo();
                /*
                 * Configure cache
                 */
                CacheConfiguration cacheConfig = new CacheConfiguration(CACHE_NAME,
                        cacheInfo.getNumberOfDatasets())
                                .memoryStoreEvictionPolicy(EVICTION_POLICY)
                                .persistence(new PersistenceConfiguration()
                                        .strategy(PERSISTENCE_STRATEGY))
                                .transactionalMode(TRANSACTIONAL_MODE);
                if (cacheInfo.getElementLifetimeMinutes() > 0) {
                    cacheConfig.setTimeToLiveSeconds(
                            (long) (cacheInfo.getElementLifetimeMinutes() * 60));
                } else {
                    cacheConfig.eternal(true);
                }
                dynamicDatasetCache = new Cache(cacheConfig);
                EdalCache.cacheManager.addCache(dynamicDatasetCache);
            } else {
                dynamicDatasetCache = EdalCache.cacheManager.getCache(CACHE_NAME);
            }
        }
    }

    /**
     * @return The NcwmsConfig object used by this catalogue. Package-private
     *         since this should not be accessed by external users
     */
    public NcwmsConfig getConfig() {
        return (NcwmsConfig) super.getConfig();
    }

    public void updateDynamicDatasetCache(NcwmsDynamicCacheInfo cacheInfo) {
        dynamicCacheEnabled = cacheInfo.isEnabled();

        if (!EdalCache.cacheManager.cacheExists(CACHE_NAME)) {
            /*
             * Create cache
             */
            CacheConfiguration cacheConfig = new CacheConfiguration(CACHE_NAME,
                    cacheInfo.getNumberOfDatasets())
                            .memoryStoreEvictionPolicy(EVICTION_POLICY)
                            .persistence(
                                    new PersistenceConfiguration().strategy(PERSISTENCE_STRATEGY))
                            .transactionalMode(TRANSACTIONAL_MODE);
            if (cacheInfo.getElementLifetimeMinutes() > 0) {
                cacheConfig
                        .setTimeToLiveSeconds((long) (cacheInfo.getElementLifetimeMinutes() * 60));
            } else {
                cacheConfig.eternal(true);
            }
            dynamicDatasetCache = new Cache(cacheConfig);
            EdalCache.cacheManager.addCache(dynamicDatasetCache);
        } else {
            /*
             * Configure existing cache
             */
            if (cacheInfo.getElementLifetimeMinutes() > 0) {
                dynamicDatasetCache.getCacheConfiguration()
                        .setTimeToLiveSeconds((long) (cacheInfo.getElementLifetimeMinutes() * 60));
            } else {
                dynamicDatasetCache.getCacheConfiguration().eternal(true);
            }
            dynamicDatasetCache.getCacheConfiguration()
                    .setMaxEntriesInCache(cacheInfo.getNumberOfDatasets());
        }
    }

    public void emptyDynamicDatasetCache() {
        dynamicDatasetCache.removeAll();
    }

    public NcwmsSupportedCrsCodes getSupportedNcwmsCrsCodes() {
        return ((NcwmsConfig) config).getSupportedNcwmsCrsCodes();
    }

    @Override
    public ServerInfo getServerInfo() {
        return ((NcwmsConfig) config).getServerInfo();
    }

    @Override
    public ContactInfo getContactInfo() {
        return ((NcwmsConfig) config).getContactInfo();
    }

    @Override
    public Dataset getDatasetFromId(String datasetId) {
        Dataset dataset = super.getDatasetFromId(datasetId);
        if (dataset != null) {
            return dataset;
        } else {
            /*
             * We may have a dynamic dataset. First check the dynamic dataset
             * cache.
             */
            final Element element = dynamicDatasetCache.get(datasetId);
            if (element != null && element.getObjectValue() != null) {
                return (Dataset) element.getObjectValue();
            }

            /*
             * Check to see if we have a dynamic service defined which this
             * dataset ID can map to. The dynamic service may be normal or database
             * mediated.
             */
            final NcwmsDynamicServiceAndType dynamicServiceWithType = getDynamicServiceAndTypeFromLayerName(datasetId);
            final NcwmsDynamicService dynamicService = dynamicServiceWithType.service;
            final NcwmsDynamicServiceType dynamicServiceType = dynamicServiceWithType.type;

            // Bail out if no enabled dynamic service is available.
            if (dynamicServiceType == NcwmsDynamicServiceType.NONE ||
                dynamicService == null ||
                dynamicService.isDisabled()
            ) {
                return null;
            }

            if (datasetId.equals(dynamicService.getAlias())) {
                /*
                 * Just the alias has been requested. This isn't a dataset!
                 */
                return null;
            }

            /*
             * Strip off the prefix "<alias>" from the dataset id.
             * Note: method joinPathStrict handles cases with and without leading /
             */
            final String datasetPath = datasetId.substring(dynamicService.getAlias().length());

            /*
             * Check if we allow this path or if it is disallowed by the dynamic
             * dataset regex
             */
            if (!dynamicService.getIdMatchPattern().matcher(datasetPath).matches()) {
                return null;
            }

            // Map the datasetPath to its corresponding URL.

            final String datasetLocation;
            try {
                assert dynamicServiceType != NcwmsDynamicServiceType.NONE;
                datasetLocation = dynamicServiceType == NcwmsDynamicServiceType.SIMPLE ?
                    datasetPath :
                    getNcwmsIndexDatabase().getLocationFromId(datasetPath);
            } catch (Exception e) {
                // TODO: Fix this horrible exception handing: way too generic, etc.
                return null;
            }

            final String datasetUrl = pathJoinStrict("/", dynamicService.getServicePath(), datasetLocation);

            try {
                DatasetFactory datasetFactory = DatasetFactory
                        .forName(dynamicService.getDataReaderClass());
                Dataset dynamicDataset = datasetFactory.createDataset(datasetId, datasetUrl);
                /*
                 * Store in the cache
                 */
                if (dynamicDatasetCache != null) {
                    dynamicDatasetCache.put(new Element(datasetId, dynamicDataset));
                }
                return dynamicDataset;
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException
                    | IOException | EdalException e) {
                /*
                 * TODO log error
                 */
                return null;
            }
        }
    }

    private NcwmsIndexDatabase getNcwmsIndexDatabase() {
        if (ncwmsIndexDatabase == null) {
            ncwmsIndexDatabase = new NcwmsIndexDatabase(
                ((NcwmsConfig) config).getDatabaseDynamicServices().getIndexDatabase()
            );
        }
        return ncwmsIndexDatabase;
    }

    @Override
    public EnhancedVariableMetadata getLayerMetadata(final VariableMetadata variableMetadata)
            throws EdalLayerNotFoundException {
        try {
            return super.getLayerMetadata(variableMetadata);
        } catch (EdalLayerNotFoundException e) {
            /*
             * The layer is not defined in the XmlDataCatalogue. However, we may
             * still have a dynamic dataset
             */
            final String layerName = getLayerNameMapper()
                    .getLayerName(variableMetadata.getDataset().getId(), variableMetadata.getId());

            final NcwmsDynamicServiceAndType dynamicServiceWithType = getDynamicServiceAndTypeFromLayerName(layerName);
            if (dynamicServiceWithType.type == NcwmsDynamicServiceType.NONE) {
                throw new EdalLayerNotFoundException("The layer: " + layerName + " doesn't exist");
            }
            /*
             * We have a dynamic dataset. Return sensible defaults
             */
            final NcwmsDynamicService dynamicService = dynamicServiceWithType.service;
            EnhancedVariableMetadata metadata = new EnhancedVariableMetadata() {
                @Override
                public String getId() {
                    return variableMetadata.getId();
                }

                @Override
                public String getTitle() {
                    return variableMetadata.getId();
                }

                @Override
                public PlottingStyleParameters getDefaultPlottingParameters() {
                    return new PlottingStyleParameters(null, ColourPalette.DEFAULT_PALETTE_NAME,
                            null, null, null, false, ColourPalette.MAX_NUM_COLOURS, 1f);
                }

                @Override
                public String getMoreInfo() {
                    return null;
                }

                @Override
                public String getDescription() {
                    return null;
                }

                @Override
                public String getCopyright() {
                    return null;
                }

                @Override
                public boolean isQueryable() {
                    return dynamicService.isQueryable();
                }

                @Override
                public boolean isDownloadable() {
                    return dynamicService.isDownloadable();
                }

                @Override
                public boolean isDisabled() {
                    return dynamicService.isDisabled();
                }
            };
            return metadata;
        }
    }

    // TODO: This should probably be folded into NcwmsDynamicService
    private enum NcwmsDynamicServiceType { NONE, SIMPLE, DATABASE }

    /**
     * An
     */
    private class NcwmsDynamicServiceAndType {
        public NcwmsDynamicServiceType type;
        public NcwmsDynamicService service;

        public NcwmsDynamicServiceAndType(NcwmsDynamicServiceType type, NcwmsDynamicService service) {
            this.type = type;
            this.service = service;
        }
    }

    private NcwmsDynamicServiceAndType getDynamicServiceAndTypeFromLayerName(String layerName) {
        final NcwmsConfig config = (NcwmsConfig) this.config;

        // A simple dynamic service?
        for (NcwmsDynamicService dynamicService : config.getDynamicServices()) {
            if (layerName.startsWith(dynamicService.getAlias()) &&
                dynamicService.getIdMatchPattern().matcher(layerName).matches()
            ) {
                return new NcwmsDynamicServiceAndType(NcwmsDynamicServiceType.SIMPLE, dynamicService);
            }
        }

        // A database dynamic service?
        for (NcwmsDynamicService dynamicService : config.getDatabaseDynamicServices().getDynamicServices()) {
            if (layerName.startsWith(dynamicService.getAlias()) &&
                dynamicService.getIdMatchPattern().matcher(layerName).matches()
            ) {
                return new NcwmsDynamicServiceAndType(NcwmsDynamicServiceType.DATABASE, dynamicService);
            }
        }

        // Nup. Nope. Nada.
        return new NcwmsDynamicServiceAndType(NcwmsDynamicServiceType.NONE, null);
    }

    @Override
    public LayerNameMapper getLayerNameMapper() {
        return layerNameMapper;
    }

    @Override
    public StyleCatalogue getStyleCatalogue() {
        return styleCatalogue;
    }

    @Override
    public String getDatasetTitle(String datasetId) {
        DatasetConfig datasetInfo = config.getDatasetInfo(datasetId);
        if (datasetInfo != null) {
            return datasetInfo.getTitle();
        }
        if (getDynamicServiceAndTypeFromLayerName(datasetId).type !=
            NcwmsDynamicServiceType.NONE) {
            return "Dynamic service from " + datasetId;
        }
        throw new EdalLayerNotFoundException(
            datasetId + " does not refer to an existing dataset");
    }

    @Override
    public boolean isDownloadable(String layerName) {
        VariableConfig xmlVariable = getXmlVariable(layerName);
        if (xmlVariable != null) {
            return xmlVariable.isDownloadable();
        }

        // We may be dealing with a dynamic dataset
        final NcwmsDynamicServiceAndType dynamicServiceWithType = getDynamicServiceAndTypeFromLayerName(layerName);
        if (dynamicServiceWithType.type != NcwmsDynamicServiceType.NONE) {
            return dynamicServiceWithType.service.isDownloadable();
        }

        return false;
    }

    @Override
    public boolean isQueryable(String layerName) {
        VariableConfig xmlVariable = getXmlVariable(layerName);
        if (xmlVariable != null) {
            return xmlVariable.isQueryable();
        }

        // We may be dealing with a dynamic dataset
        final NcwmsDynamicServiceAndType dynamicServiceWithType = getDynamicServiceAndTypeFromLayerName(layerName);
        if (dynamicServiceWithType.type != NcwmsDynamicServiceType.NONE) {
            return dynamicServiceWithType.service.isQueryable();
        }

        return false;
    }

    @Override
    public boolean isDisabled(String layerName) {
        VariableConfig xmlVariable = getXmlVariable(layerName);
        if (xmlVariable != null) {
            return xmlVariable.isDisabled();
        }

        // We may be dealing with a dynamic dataset
        final NcwmsDynamicServiceAndType dynamicServiceWithType = getDynamicServiceAndTypeFromLayerName(layerName);
        if (dynamicServiceWithType.type != NcwmsDynamicServiceType.NONE) {
            return dynamicServiceWithType.service.isDisabled();
        }

        return true;
    }

    private VariableConfig getXmlVariable(String layerName) {
        DatasetConfig datasetInfo = config
                .getDatasetInfo(getLayerNameMapper().getDatasetIdFromLayerName(layerName));
        if (datasetInfo != null) {
            return datasetInfo.getVariableById(
                getLayerNameMapper().getVariableIdFromLayerName(layerName));
        } else {
            return null;
        }
    }
}
