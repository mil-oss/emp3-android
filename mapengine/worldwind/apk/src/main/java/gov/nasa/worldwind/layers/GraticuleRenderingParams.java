/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers;

import android.graphics.Typeface;

import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.render.Color;
import gov.nasa.worldwind.util.Logger;

/**
 * @author dcollins
 * @version $Id: GraticuleRenderingParams.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class GraticuleRenderingParams extends AVListImpl {
    final static private String TAG = GraticuleRenderingParams.class.getSimpleName();

    public static final String KEY_DRAW_LINES = "DrawGraticule";
    public static final String KEY_LINE_COLOR = "GraticuleLineColor";
    public static final String KEY_LINE_WIDTH = "GraticuleLineWidth";
    public static final String KEY_LINE_STYLE = "GraticuleLineStyle";
    public static final String KEY_LINE_CONFORMANCE = "GraticuleLineConformance";
    public static final String KEY_DRAW_LABELS = "DrawLabels";
    public static final String KEY_LABEL_COLOR = "LabelColor";
    public static final String KEY_LABEL_FONT_TYPE_FACE = "LabelFontTypeFace";
    public static final String KEY_LABEL_FONT_POINT_SIZE = "LabelFontPointSize";
    public static final String VALUE_LINE_STYLE_SOLID = "LineStyleSolid";
    public static final String VALUE_LINE_STYLE_DASHED = "LineStyleDashed";
    public static final String VALUE_LINE_STYLE_DOTTED = "LineStyleDotted";

    public GraticuleRenderingParams() {
    }

    public boolean isDrawLines() {
        Object value = getValue(KEY_DRAW_LINES);
        return value instanceof Boolean ? (Boolean) value : false;
    }

    public void setDrawLines(boolean drawLines) {
        setValue(KEY_DRAW_LINES, drawLines);
    }

    public Color getLineColor() {
        Object value = getValue(KEY_LINE_COLOR);
        return value instanceof Color ? (Color) value : null;
    }

    public void setLineColor(Color color) {
        if (color == null) {
            String message = Logger.makeMessage(TAG, "setLineColor", "nullValue.ColorIsNull");
            Logger.log(Logger.ERROR, message);
            throw new IllegalArgumentException(message);
        }

        setValue(KEY_LINE_COLOR, color);
    }

    public float getLineWidth() {

        Object value = getValue(KEY_LINE_WIDTH);
        return value instanceof Float ? (Float) value : 0;
    }

    public void setLineWidth(double lineWidth) {
        setValue(KEY_LINE_WIDTH, lineWidth);
    }

    public String getLineStyle() {
        Object value = getValue(KEY_LINE_STYLE);
        return value instanceof String ? (String) value : null;
    }

    public void setLineStyle(String lineStyle) {
        if (lineStyle == null) {
            String message = Logger.makeMessage(TAG, "setLineStyle", "nullValue.StringIsNull");
            Logger.log(Logger.ERROR, message);
            throw new IllegalArgumentException(message);
        }

        setValue(KEY_LINE_STYLE, lineStyle);
    }

    public boolean isDrawLabels() {
        Object value = getValue(KEY_DRAW_LABELS);
        return value instanceof Boolean ? (Boolean) value : false;
    }

    public void setDrawLabels(boolean drawLabels) {
        setValue(KEY_DRAW_LABELS, drawLabels);
    }

    public Color getLabelColor() {
        Object value = getValue(KEY_LABEL_COLOR);
        return value instanceof Color ? (Color) value : null;
    }

    public void setLabelColor(Color color) {
        if (color == null) {
            String message = Logger.makeMessage(TAG, "setLabelColor", "nullValue.ColorIsNull");
            Logger.log(Logger.ERROR, message);
            throw new IllegalArgumentException(message);
        }

        setValue(KEY_LABEL_COLOR, color);
    }

    public Typeface getLabelTypeface() {
        Object value = getValue(KEY_LABEL_FONT_TYPE_FACE);
        return value instanceof Typeface ? (Typeface) value : null;
    }

    public void setLabelTypeface(Typeface typeFace) {
        if (typeFace == null) {
            String message = Logger.makeMessage(TAG, "setLabelTypeface", "typeface can not be null.");
            Logger.log(Logger.ERROR, message);
            throw new IllegalArgumentException(message);
        }

        setValue(KEY_LABEL_FONT_TYPE_FACE, typeFace);
    }

    public Float getLabelPointSize() {
        Object value = getValue(KEY_LABEL_FONT_POINT_SIZE);
        return value instanceof Float ? (Float) value : null;
    }

    public void setLablePointSize(float value) {
        if (value == Float.NaN) {
            String message = Logger.makeMessage(TAG, "setLablePointSize", "invalid float value.");
            Logger.log(Logger.ERROR, message);
            throw new IllegalArgumentException(message);
        }

        setValue(KEY_LABEL_FONT_POINT_SIZE, new Float(value));
    }
}
