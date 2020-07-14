package uk.ac.rdg.resc.edal.ncwms.config;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import uk.ac.rdg.resc.edal.catalogue.jaxb.VariableConfig;
import uk.ac.rdg.resc.edal.util.Extents;

public class DatasetFullConfigTest {
    private String id = "id";
    private String title = "title";
    private String location = "location";
    private boolean queryable = false;
    private boolean downloadable = true;
    private String dataReaderClass = "dataReaderClass";
    private String copyrightStatement = "copyrightStatement";
    private String moreInfo = "moreInfo";
    private boolean disabled = false;
    private int updateInterval = 99;
    private String metadataUrl = "metadataUrl";
    private String metadataDesc = "metadataDesc";
    private String metadataMimeType = "metadataMimeType";
    private VariableConfig[] variables = new VariableConfig[]{
            new VariableConfig(
                    "varId", "A Variable", "A data-related quantity",
                    Extents.newExtent(-10f, 10f), "redblue", null, null, null,
                    "linear", 250
            )
    };

    private DatasetFullConfig dataset;

    @Before
    public void setUp() throws Exception {
        dataset = new DatasetFullConfig(
                id,
                title,
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
                metadataMimeType,
                variables
        );
    }

    @Test
    public void testSomeAttributes() {
        assertEquals(dataset.getId(), id);
        assertEquals(dataset.getTitle(), title);
        assertEquals(dataset.getLocation(), location);
        assertEquals(dataset.isQueryable(), queryable);
        assertEquals(dataset.isDownloadable(), downloadable);
        assertEquals(dataset.getMoreInfo(), moreInfo);
        assertArrayEquals(dataset.getVariables(), variables);
    }
}
