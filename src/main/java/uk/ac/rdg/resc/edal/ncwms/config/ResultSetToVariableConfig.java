package uk.ac.rdg.resc.edal.ncwms.config;

import org.apache.commons.lang.NotImplementedException;
import uk.ac.rdg.resc.edal.catalogue.jaxb.VariableConfig;
import uk.ac.rdg.resc.edal.domain.Extent;
import uk.ac.rdg.resc.edal.util.Extents;

import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ResultSetToVariableConfig implements TypeTransformer<ResultSet, VariableConfig, Object> {
    public static final String VARIABLE_ID_COLUMN_NAME = "variable_id";
    public static final String VARIABLE_NAME_COLUMN_NAME = "variable_name";
    public static final String RANGE_MIN_COLUMN_NAME = "range_min";
    public static final String RANGE_MAX_COLUMN_NAME = "range_max";

    // Fixed values for making a VariableConfig
    private String paletteName = "x-Occam";
    private Color belowMinColour = new Color(1, 0, 0);
    private Color aboveMaxColour = new Color(1, 0, 0);
    private Color noDataColour = new Color(0);
    private String scaling = "linear";
    private int numColorBands = 250;

    public ResultSetToVariableConfig() {
    }

    public ResultSetToVariableConfig(
            String paletteName,
            Color belowMinColour,
            Color aboveMaxColour,
            Color noDataColour,
            String scaling,
            int numColorBands
    ) {
        this.paletteName = paletteName;
        this.belowMinColour = belowMinColour;
        this.aboveMaxColour = aboveMaxColour;
        this.noDataColour = noDataColour;
        this.scaling = scaling;
        this.numColorBands = numColorBands;
    }

    @Override
    public VariableConfig make(ResultSet from) throws SQLException {
        String variable_id = from.getString(VARIABLE_ID_COLUMN_NAME);
        String variable_name = from.getString(VARIABLE_NAME_COLUMN_NAME);
        Float range_min = from.getFloat(RANGE_MIN_COLUMN_NAME);
        Float range_max = from.getFloat(RANGE_MAX_COLUMN_NAME);
        Extent<Float> colorScaleRange =
                Extents.newExtent(range_min, range_max);
        return new VariableConfig(
                variable_id,
                variable_name,
                variable_name,
                colorScaleRange,
                paletteName,
                belowMinColour,
                aboveMaxColour,
                noDataColour,
                scaling,
                numColorBands
        );
    }

    @Override
    public VariableConfig make(ResultSet from, Object with) throws SQLException {
        throw new NotImplementedException();
    }
}
