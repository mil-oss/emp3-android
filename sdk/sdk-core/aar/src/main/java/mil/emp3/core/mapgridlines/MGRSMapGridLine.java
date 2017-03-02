package mil.emp3.core.mapgridlines;

import android.util.Log;

import org.cmapi.primitives.GeoLabelStyle;
import org.cmapi.primitives.GeoPosition;
import org.cmapi.primitives.GeoStrokeStyle;
import org.cmapi.primitives.IGeoLabelStyle;
import org.cmapi.primitives.IGeoPosition;
import org.cmapi.primitives.IGeoStrokeStyle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mil.emp3.api.Path;
import mil.emp3.api.Text;
import mil.emp3.api.interfaces.ICamera;
import mil.emp3.api.interfaces.IFeature;
import mil.emp3.api.utils.EmpBoundingBox;
import mil.emp3.api.utils.EmpGeoColor;
import mil.emp3.api.utils.FontUtilities;
import mil.emp3.api.utils.GeoLibrary;
import mil.emp3.core.mapgridlines.coordinates.UTMCoordinate;
import mil.emp3.core.mapgridlines.utils.GridLineUtils;
import mil.emp3.mapengine.interfaces.IMapInstance;

/**
 * This class implements the main MGRS grid line generator class.
 */

public class MGRSMapGridLine extends UTMBaseMapGridLine {
    private static final String TAG = MGRSMapGridLine.class.getSimpleName();

    private static final int MGRS_100K_METER_GRID = 100000;
    private static final int MGRS_10K_METER_GRID = 10000;
    private static final int MGRS_1K_METER_GRID = 1000;
    private static final int MGRS_100_METER_GRID = 100;
    private static final int MGRS_10_METER_GRID = 10;
    private static final int MGRS_1_METER_GRID = 1;

    private static final double MAX_MGRS_GRID_ZONE_THRESHOLD = MGRS_100K_METER_GRID * 50;  // Viewing area width in meters after which the MGRS grid zones are displayed.
    private static final double MAX_MGRS_100K_GRID_THRESHOLD = MGRS_100K_METER_GRID * 18;
    private static final double MAX_MGRS_10K_GRID_THRESHOLD = MGRS_10K_METER_GRID * 20;
    private static final double MAX_MGRS_1K_GRID_THRESHOLD = MGRS_1K_METER_GRID * 20;
    private static final double MAX_MGRS_100_GRID_THRESHOLD = MGRS_100_METER_GRID * 20;
    private static final double MAX_MGRS_10_GRID_THRESHOLD = MGRS_10_METER_GRID * 20;
    private static final double MAX_MGRS_1_GRID_THRESHOLD = MGRS_1_METER_GRID * 20;

    private static final String MGRS_GRID_ZONE_MERIDIAN = "gridzone.meridian";
    private static final String MGRS_GRID_ZONE_PARALLELS = "gridzone.parallels";
    private static final String MGRS_GRID_ZONE_LABEL = "gridzone.label";

    private static final String MGRS_GRID_BOX_MERIDIAN = "MGRS.gridbox.meridian";
    private static final String MGRS_GRID_BOX_PARALLELS = "MGRS.gridbox.parallels";
    private static final String MGRS_GRID_LINE_MAJOR_MERIDIAN = "MGRS.gridline.major.meridian";
    private static final String MGRS_GRID_LINE_MAJOR_PARALLELS = "MGRS.gridline.major.parallels";
    private static final String MGRS_GRID_LINE_MINOR_MERIDIAN = "MGRS.gridline.minor.meridian";
    private static final String MGRS_GRID_LINE_MINOR_PARALLELS = "MGRS.gridline.minor.parallels";
    private static final String MGRS_GRID_BOX_LABEL_CENTERED = "MGRS.label.centered";
    private static final String MGRS_GRID_BOX_NORTH_VALUE = "MGRS.north.values";
    private static final String MGRS_GRID_BOX_EAST_VALUE = "MGRS.east.values";

    private static final int TWO_CHARACTER_12PT_PIXEL_WIDTH = FontUtilities.fontPointsToPixels(12) * 2;
    private static final int THREE_CHARACTER_12PT_PIXEL_WIDTH = FontUtilities.fontPointsToPixels(12) * 3;

    private final Map<String, IGeoStrokeStyle> strokeStyleMap;
    private final Map<String, IGeoLabelStyle> labelStyleMap;

    private final String MGRSColumns = "ABCDEFGHJKLMNPQRSTUVWXYZ"; // 24
    private final String[] MGRSRows = {"ABCDEFGHJKLMNPQRSTUV","FGHJKLMNPQRSTUVABCDE"}; // 20 each. The odd zone use the 1st set the even zone use the 2nd.

    /**
     * UTM zones are grouped, and assigned to one of a group of 6
     * sets.
     */
    private static final int NUM_100K_SETS = 6;

    /**
     * The column letters (for easting) of the lower left value, per
     * set.
     */
    private static final String SET_ORIGIN_COLUMN_LETTERS = "AJSAJS";

    /**
     * The row letters (for northing) of the lower left value, per
     * set.
     */
    private static final String SET_ORIGIN_ROW_LETTERS = "AFAFAF";

    private static final int A = 65; // A
    private static final int I = 73; // I
    private static final int O = 79; // O
    private static final int V = 86; // V
    private static final int Z = 90; // Z

    public MGRSMapGridLine(IMapInstance mapInstance) {
        super(mapInstance);
        this.strokeStyleMap = new HashMap<>();
        this.labelStyleMap = new HashMap<>();
        setStyles();
    }

    private void setStyles() {
        EmpGeoColor color;
        IGeoStrokeStyle strokeStyle;
        IGeoLabelStyle labelStyle;

        // Grid zone styles.
        color = new EmpGeoColor(0.6, 255, 255, 0);
        strokeStyle = new GeoStrokeStyle();
        strokeStyle.setStrokeColor(color);
        strokeStyle.setStrokeWidth(3.0);
        strokeStyleMap.put(MGRS_GRID_ZONE_MERIDIAN, strokeStyle);
        strokeStyleMap.put(MGRS_GRID_ZONE_PARALLELS, strokeStyle);

        labelStyle = new GeoLabelStyle();
        color = new EmpGeoColor(1.0, 255, 255, 0);
        labelStyle.setColor(color);
        labelStyle.setSize(12.0);
        labelStyle.setJustification(IGeoLabelStyle.Justification.CENTER);
        labelStyle.setFontFamily("Ariel");
        labelStyle.setTypeface(IGeoLabelStyle.Typeface.REGULAR);
        labelStyleMap.put(MGRS_GRID_ZONE_LABEL, labelStyle);

        // MGRS Grid Box
        color = new EmpGeoColor(0.7, 0, 0, 255);
        strokeStyle = new GeoStrokeStyle();
        strokeStyle.setStrokeColor(color);
        strokeStyle.setStrokeWidth(4.0);
        strokeStyleMap.put(MGRS_GRID_BOX_MERIDIAN, strokeStyle);
        strokeStyleMap.put(MGRS_GRID_BOX_PARALLELS, strokeStyle);

        // MGRS Grid Lines Major
        color = new EmpGeoColor(0.5, 75, 75, 255);
        strokeStyle = new GeoStrokeStyle();
        strokeStyle.setStrokeColor(color);
        strokeStyle.setStrokeWidth(3.0);
        strokeStyleMap.put(MGRS_GRID_LINE_MAJOR_MERIDIAN, strokeStyle);
        strokeStyleMap.put(MGRS_GRID_LINE_MAJOR_PARALLELS, strokeStyle);

        // MGRS Grid Lines Minor
        color = new EmpGeoColor(0.5, 150, 150, 255);
        strokeStyle = new GeoStrokeStyle();
        strokeStyle.setStrokeColor(color);
        strokeStyle.setStrokeWidth(1.0);
        strokeStyleMap.put(MGRS_GRID_LINE_MINOR_MERIDIAN, strokeStyle);
        strokeStyleMap.put(MGRS_GRID_LINE_MINOR_PARALLELS, strokeStyle);

        // MGRS ID
        // Centered
        labelStyle = new GeoLabelStyle();
        color = new EmpGeoColor(0.7, 100, 100, 255);
        labelStyle.setColor(color);
        labelStyle.setSize(12.0);
        labelStyle.setJustification(IGeoLabelStyle.Justification.CENTER);
        labelStyle.setFontFamily("Ariel");
        labelStyle.setTypeface(IGeoLabelStyle.Typeface.BOLD);
        labelStyleMap.put(MGRS_GRID_BOX_LABEL_CENTERED, labelStyle);

        // MGRS northing values
        labelStyle = new GeoLabelStyle();
        color = new EmpGeoColor(1.0, 0, 0, 0);
        labelStyle.setColor(color);
        labelStyle.setSize(8.0);
        labelStyle.setJustification(IGeoLabelStyle.Justification.LEFT);
        labelStyle.setFontFamily("Ariel");
        labelStyle.setTypeface(IGeoLabelStyle.Typeface.REGULAR);
        labelStyleMap.put(MGRS_GRID_BOX_NORTH_VALUE, labelStyle);

        // MGRS easting values.
        labelStyle = new GeoLabelStyle();
        labelStyle.setColor(color);
        labelStyle.setSize(8.0);
        labelStyle.setJustification(IGeoLabelStyle.Justification.LEFT);
        labelStyle.setFontFamily("Ariel");
        labelStyle.setTypeface(IGeoLabelStyle.Typeface.REGULAR);
        labelStyleMap.put(MGRS_GRID_BOX_EAST_VALUE, labelStyle);
    }

    @Override
    protected void processViewChange(EmpBoundingBox mapBounds, ICamera camera, int viewWidth, int viewHeight) {
        double viewWidthInMeters = Math.floor(mapBounds.widthAcrossCenter());

        clearFeatureList();

        if (viewWidthInMeters <= MAX_MGRS_1_GRID_THRESHOLD) {
            Log.i(TAG, "1 threshold. " + viewWidthInMeters);
            createMGRSGridZones(mapBounds, camera, viewWidth, viewHeight, false);
            createMGRSGrid(mapBounds, camera, viewWidth, viewHeight, MGRS_1_METER_GRID);
        } else if (viewWidthInMeters <= MAX_MGRS_10_GRID_THRESHOLD) {
            Log.i(TAG, "10 threshold. " + viewWidthInMeters);
            createMGRSGridZones(mapBounds, camera, viewWidth, viewHeight, false);
            createMGRSGrid(mapBounds, camera, viewWidth, viewHeight, MGRS_10_METER_GRID);
        } else if (viewWidthInMeters <= MAX_MGRS_100_GRID_THRESHOLD) {
            Log.i(TAG, "100 threshold. " + viewWidthInMeters);
            createMGRSGridZones(mapBounds, camera, viewWidth, viewHeight, false);
            createMGRSGrid(mapBounds, camera, viewWidth, viewHeight, MGRS_100_METER_GRID);
        } else if (viewWidthInMeters <= MAX_MGRS_1K_GRID_THRESHOLD) {
            Log.i(TAG, "1K threshold. " + viewWidthInMeters);
            createMGRSGridZones(mapBounds, camera, viewWidth, viewHeight, false);
            createMGRSGrid(mapBounds, camera, viewWidth, viewHeight, MGRS_1K_METER_GRID);
        } else if (viewWidthInMeters <= MAX_MGRS_10K_GRID_THRESHOLD) {
            Log.i(TAG, "10K threshold. " + viewWidthInMeters);
            createMGRSGridZones(mapBounds, camera, viewWidth, viewHeight, false);
            createMGRSGrid(mapBounds, camera, viewWidth, viewHeight, MGRS_10K_METER_GRID);
        } else if (viewWidthInMeters <= MAX_MGRS_100K_GRID_THRESHOLD) {
            Log.i(TAG, "100K threshold. " + viewWidthInMeters);
            createMGRSGridZones(mapBounds, camera, viewWidth, viewHeight, false);
            createMGRSGrid(mapBounds, camera, viewWidth, viewHeight, MGRS_100K_METER_GRID);
        } else if (viewWidthInMeters <= MAX_MGRS_GRID_ZONE_THRESHOLD) {
            Log.i(TAG, "GZD threshold. " + viewWidthInMeters);
            createMGRSGridZones(mapBounds, camera, viewWidth, viewHeight, true);
        } else {
            // The grid turns off.
        }
    }

    private void createMGRSGrid(EmpBoundingBox mapBounds, ICamera camera, int viewWidth, int viewHeight, int gridSize) {
        int longitude;
        int latitude;
        int eastZoneNumber;
        double metersPerPixel = mapBounds.widthAcrossCenter() / (double) viewWidth;
        EmpBoundingBox gridZoneBounds = new EmpBoundingBox();
        UTMCoordinate gridZoneUTMCoord = new UTMCoordinate();
        // UTM coordinates are used by the methods called by this method.
        // We allocate this list here so they can be allocated once per execution.
        UTMCoordinate[] tempUTMCoordList = {new UTMCoordinate(), new UTMCoordinate(), new UTMCoordinate(), new UTMCoordinate(), new UTMCoordinate()};
        EmpBoundingBox tempBoundingBox = new EmpBoundingBox();

        if (0 == UTMCoordinate.getZoneNumber(mapBounds.centerLatitude(), mapBounds.centerLongitude())) {
            // The bounding box is in one of the poles. UPS
            // TODO add UPS.
            return;
        }

        double minLatitude = Math.floor(mapBounds.south());
        double maxLatitude = Math.ceil((mapBounds.north()));
        double minLongitude = Math.floor(mapBounds.west());
        double maxLongitude = Math.ceil((mapBounds.east()));

        eastZoneNumber = (UTMCoordinate.getZoneNumber(minLatitude, maxLongitude) % 60) + 1;

        UTMCoordinate.fromLatLong(minLatitude, minLongitude, gridZoneUTMCoord);
        latitude = UTMCoordinate.getZoneSouthLatitude(gridZoneUTMCoord.getZoneNumber(), gridZoneUTMCoord.getZoneLetter());
        longitude = UTMCoordinate.getZoneWestLongitude(gridZoneUTMCoord.getZoneNumber(), gridZoneUTMCoord.getZoneLetter());
        // This will set the gridZoneUTMCoord to the south west corner of the UTM grid zone that is at the south west corner of the viewing area.
        UTMCoordinate.fromLatLong(latitude, longitude, gridZoneUTMCoord);

        latitude = (int) minLatitude;

        // Process grid zones
        while (latitude <= maxLatitude) {
            gridZoneBounds.setSouth(gridZoneUTMCoord.getZoneSouthLatitude());
            gridZoneBounds.setNorth(gridZoneBounds.getSouth() + gridZoneUTMCoord.getGridZoneHeightInDegrees());
            while (gridZoneUTMCoord.getZoneNumber() != eastZoneNumber) {
                Log.i(TAG, "Processing Zone: " + gridZoneUTMCoord.getZoneNumber() + gridZoneUTMCoord.getZoneLetter() + " width:" + gridZoneUTMCoord.getGridZoneWidthInDegrees());
                gridZoneBounds.setWest(gridZoneUTMCoord.getZoneWestLongitude());
                gridZoneBounds.setEast(gridZoneBounds.getWest() + gridZoneUTMCoord.getGridZoneWidthInDegrees());
                createMGRSGridsForGridzone(gridZoneUTMCoord, gridZoneBounds, tempUTMCoordList, mapBounds, mapBounds.intersection(gridZoneBounds, tempBoundingBox), gridSize, metersPerPixel);
                // Set UTMCoordinate to the UTM grid zone to east of the current one.
                UTMCoordinate.fromLatLong(latitude, gridZoneBounds.getEast(), gridZoneUTMCoord);
            }
            latitude = (int) gridZoneBounds.getNorth();
            // This sets gridZoneUTMCoord to the first UTM grid zone in the viewing area of the next row north.
            UTMCoordinate.fromLatLong(latitude, longitude, gridZoneUTMCoord);
        }
    }

    private void createMGRSGridParallels(UTMCoordinate gridZoneUTMCoord, UTMCoordinate[] tempUTMCoordList, EmpBoundingBox drawBounds, int gridSize) {
        double latitude;
        IGeoPosition WestPos = null;
        IGeoPosition EastPos = null;
        List<IGeoPosition> positionList;
        IFeature gridObject;
        String letter;
        UTMCoordinate westUTMCoord = tempUTMCoordList[0];
        UTMCoordinate eastUTMCoord = tempUTMCoordList[1];
        boolean northernHemesphere = gridZoneUTMCoord.isNorthernHemisphere();
        int parentGridSize = ((gridSize != MGRS_100K_METER_GRID)? gridSize * 10: MGRS_100K_METER_GRID);
        int tempValue;

        latitude = drawBounds.getSouth();

        // Generate the parallel lines of the box (the horizontal lines).
        UTMCoordinate.fromLatLong(latitude, drawBounds.getWest(), westUTMCoord);
        UTMCoordinate.fromLatLong(latitude, ((drawBounds.getEast() == 180.0)? 179.99999999999: drawBounds.getEast()), eastUTMCoord);

        // Make sure that the northing value is a multiple of gridSize.
        tempValue = (((int) Math.floor(westUTMCoord.getNorthing() / gridSize)) * gridSize);
        if ((int) westUTMCoord.getNorthing() != tempValue) {
            westUTMCoord.setNorthing(tempValue);
        }
        eastUTMCoord.setNorthing(westUTMCoord.getNorthing());
        WestPos = westUTMCoord.toLatLong();

        while (latitude < drawBounds.getNorth()) {
            positionList = new ArrayList<>();
            positionList.add(WestPos);
            EastPos = eastUTMCoord.toLatLong();
            positionList.add(EastPos);

            if (gridSize == MGRS_100K_METER_GRID) {
                gridObject = createPathFeature(positionList, MGRS_GRID_BOX_PARALLELS);
            } else if ((Math.floor(westUTMCoord.getNorthing() / MGRS_100K_METER_GRID) * MGRS_100K_METER_GRID) == westUTMCoord.getNorthing()) {
                gridObject = createPathFeature(positionList, MGRS_GRID_BOX_PARALLELS);
            } else if ((Math.floor(westUTMCoord.getNorthing() / parentGridSize) * parentGridSize) == westUTMCoord.getNorthing()) {
                gridObject = createPathFeature(positionList, MGRS_GRID_LINE_MAJOR_PARALLELS);
            } else {
                gridObject = createPathFeature(positionList, MGRS_GRID_LINE_MINOR_PARALLELS);
            }

            addFeature(gridObject);
            westUTMCoord.setNorthing(westUTMCoord.getNorthing() + gridSize);
            // Now we must make sure we didn't move onto the next grid zone.
            if (westUTMCoord.getNorthing() > westUTMCoord.getMaxNorthingForZone()) {
                if (northernHemesphere) {
                    letter = westUTMCoord.getNextLetter();
                } else {
                    letter = westUTMCoord.getPreviousLetter();
                }
                if (null == letter) {
                    break;
                }
                westUTMCoord.setZoneLetter(letter);
                eastUTMCoord.setZoneLetter(letter);
            }
            eastUTMCoord.setNorthing(westUTMCoord.getNorthing());
            WestPos = westUTMCoord.toLatLong();
            latitude = WestPos.getLatitude();
        }
    }

    private void createMGRSGridMeridians(UTMCoordinate gridZoneUTMCoord, UTMCoordinate[] tempUTMCoordList, EmpBoundingBox drawBounds, int gridSize) {
        double longitude;
        List<IGeoPosition> positionList;
        IFeature gridObject;
        int tempValue;
        IGeoPosition TopPos = null;
        IGeoPosition BottomPos = null;
        UTMCoordinate tempUTMCoord1 = tempUTMCoordList[0];
        UTMCoordinate tempUTMCoord2 = tempUTMCoordList[1];
        int parentGridSize = ((gridSize != MGRS_100K_METER_GRID)? gridSize * 10: MGRS_100K_METER_GRID);

        longitude = drawBounds.getWest();

        UTMCoordinate.fromLatLong(drawBounds.getNorth(),longitude, tempUTMCoord1);
        UTMCoordinate.fromLatLong(drawBounds.getSouth(), longitude, tempUTMCoord2);

        // Make sure that the easting value is a multiple of 100K.
        tempValue = (int) Math.floor(tempUTMCoord1.getEasting() / gridSize) * gridSize;
        if ((int) tempUTMCoord1.getEasting() != tempValue) {
            tempUTMCoord1.setEasting(tempValue);
        }
        tempUTMCoord2.setEasting(tempUTMCoord1.getEasting());
        TopPos = tempUTMCoord1.toLatLong();
        longitude = TopPos.getLongitude();

        while (longitude < drawBounds.getEast()) {
            if (longitude > drawBounds.getWest()) {
                positionList = new ArrayList<>();
                positionList.add(TopPos);
                BottomPos = tempUTMCoord2.toLatLong();
                positionList.add(BottomPos);
                if (gridSize == MGRS_100K_METER_GRID) {
                    gridObject = createPathFeature(positionList, MGRS_GRID_BOX_MERIDIAN);
                } else if ((Math.floor(tempUTMCoord1.getEasting() / MGRS_100K_METER_GRID) * MGRS_100K_METER_GRID) == tempUTMCoord1.getEasting()) {
                    gridObject = createPathFeature(positionList, MGRS_GRID_BOX_MERIDIAN);
                } else if ((Math.floor(tempUTMCoord1.getEasting() / parentGridSize) * parentGridSize) == tempUTMCoord1.getEasting()) {
                    gridObject = createPathFeature(positionList, MGRS_GRID_LINE_MAJOR_MERIDIAN);
                } else {
                    gridObject = createPathFeature(positionList, MGRS_GRID_LINE_MINOR_MERIDIAN);
                }
                addFeature(gridObject);
            }
            tempUTMCoord1.setEasting(tempUTMCoord1.getEasting() + gridSize);
            tempUTMCoord2.setEasting(tempUTMCoord1.getEasting());
            TopPos = tempUTMCoord1.toLatLong();
            longitude = TopPos.getLongitude();
        }
    }

    /**
     * This method create the feature for the grid line values.
     * @param gridZoneBounds
     * @param mgrsGridUTMCoord      The UTM coordinate of the south west corner of the MGRS grid box. The entire box may not be displayed in the grid zone.
     * @param mgrs100KGridBounds    The bounding box of the MGRS 100K clipped by the west and east of the grid zone.
     * @param mapBounds             The bounding box of the map viewing area.
     * @param gridSize              The MGRS grid size.
     * @param tempUTMCoordList      The list of temp UTM coordinates.
     */
    private void createMGRSGirdValues(EmpBoundingBox gridZoneBounds, UTMCoordinate mgrsGridUTMCoord, EmpBoundingBox mgrs100KGridBounds,
            EmpBoundingBox mapBounds, int gridSize, UTMCoordinate[] tempUTMCoordList) {
        int northValue;
        int eastValue;
        int tempValue;
        double metersNorth;
        double metersEast;
        int startEasting;
        int startNorthing;
        IFeature gridObject;
        IGeoPosition valuePos = new GeoPosition();
        UTMCoordinate northValueUTMCoord = tempUTMCoordList[2]; // We use 2. The caller uses 0 & 1.
        UTMCoordinate eastValueUTMCoord = tempUTMCoordList[3];
        UTMCoordinate tempUTMCoord = tempUTMCoordList[4];
        UTMCoordinate drawMGRSGridUTMCoord;
        int parentGridSize = gridSize * 10;
        EmpBoundingBox drawBounds = new EmpBoundingBox();

        if (null == mgrs100KGridBounds.intersection(mapBounds, drawBounds)) {
            // Nothing to draw.
            return;
        }

        drawMGRSGridUTMCoord = UTMCoordinate.fromLatLong(drawBounds.getSouth(), drawBounds.getWest());

        northValueUTMCoord.copyFrom(drawMGRSGridUTMCoord);
        eastValueUTMCoord.copyFrom(drawMGRSGridUTMCoord);

        // Make sure that the north value northing is at a grid size value.
        tempValue = (int) Math.floor(northValueUTMCoord.getNorthing() / gridSize) * gridSize;
        if ((int) northValueUTMCoord.getNorthing() != tempValue) {
            northValueUTMCoord.setNorthing(tempValue);
        }
        startNorthing = (int) northValueUTMCoord.getNorthing();

        // Make sure that the north value easting is at a parent grid size value.
        tempValue = (int) Math.floor(northValueUTMCoord.getEasting() / parentGridSize) * parentGridSize;
        if ((int) northValueUTMCoord.getEasting() != tempValue) {
            northValueUTMCoord.setEasting(tempValue);
        }

        // Make sure that the east value easting is at a grid size value.
        tempValue = (int) Math.floor(eastValueUTMCoord.getEasting() / gridSize) * gridSize;
        if ((int) eastValueUTMCoord.getEasting() != tempValue) {
            eastValueUTMCoord.setEasting(tempValue);
        }
        startEasting = (int) eastValueUTMCoord.getEasting();

        // Make sure that the east value northing is at a parent grid size value.
        tempValue = (int) Math.floor(eastValueUTMCoord.getNorthing() / parentGridSize) * parentGridSize;
        if ((int) eastValueUTMCoord.getNorthing() != tempValue) {
            eastValueUTMCoord.setNorthing(tempValue);
        }

        //String mgrsGridLabel = get100kID(mgrsGridUTMCoord.getEasting(), mgrsGridUTMCoord.getNorthing(), mgrsGridUTMCoord.getZoneNumber());
        //Log.i(TAG, "Setting Northing values for " + mgrsGridUTMCoord.getZoneNumber() + mgrsGridUTMCoord.getZoneLetter() + " " + mgrsGridLabel);

        // Add the northing values.
        tempUTMCoord.copyFrom(northValueUTMCoord);
        tempUTMCoord.toLatLong(valuePos);
        while (valuePos.getLongitude() < drawBounds.getEast()) {
            while (valuePos.getLatitude() < drawBounds.getNorth()) {
                metersNorth = (tempUTMCoord.getNorthing() - mgrsGridUTMCoord.getNorthing());
                northValue = (int) Math.floor(metersNorth / gridSize);
                if (northValue >= (MGRS_100K_METER_GRID / gridSize)) {
                    break;
                }
                // we use the gird zone center latitude to just check the position longitude.
                if (gridZoneBounds.contains(gridZoneBounds.centerLatitude(), valuePos.getLongitude())) {
                    gridObject = createLabelFeature(GridLineUtils.newPosition(valuePos.getLatitude(), valuePos.getLongitude(), 0.0), northValue + "", MGRS_GRID_BOX_NORTH_VALUE);
                    addFeature(gridObject);
                }
                tempUTMCoord.setNorthing(tempUTMCoord.getNorthing() + gridSize);
                tempUTMCoord.toLatLong(valuePos);
            }
            tempUTMCoord.setNorthing(startNorthing);
            tempUTMCoord.setEasting(tempUTMCoord.getEasting() + parentGridSize);
            tempUTMCoord.toLatLong(valuePos);
        }
/*
        // Add the easting values.
        for (int eastingIndex = 1; eastingIndex < 10; eastingIndex++) {
            meters = eastingIndex * gridSize;
            tempUTMCoord.copyFrom(eastValueUTMCoord);
            tempUTMCoord.setEasting(mgrsGridUTMCoord.getEasting() + meters);
            position = tempUTMCoord.toLatLong();
            if (gridZoneBounds.contains(gridZoneBounds.getNorth(), position.getLongitude()) && mapBounds.contains(position.getLatitude(), position.getLongitude())) {
                gridObject = createLabelFeature(position, eastingIndex + "", MGRS_GRID_BOX_EAST_VALUE);
                addFeature(gridObject);
            }
        }
*/
    }

    private void createMGRSGridLabels(UTMCoordinate gridZoneUTMCoord, EmpBoundingBox gridZoneBounds, UTMCoordinate[] tempUTMCoordList,
            EmpBoundingBox mapBounds, EmpBoundingBox drawBounds, int gridSize, double metersPerPixel) {
        double latitude;
        IFeature gridObject;
        String mgrsGridLabel;
        IGeoPosition labelPos;
        IGeoPosition mgrs100KGridBoxSWPos;
        IGeoPosition tempPos = new GeoPosition();
        double initialEasting;
        int tempValue;
        UTMCoordinate mgrsGridBoxCoord = tempUTMCoordList[0];
        UTMCoordinate tempUTMCoord = tempUTMCoordList[1];
        EmpBoundingBox mgrs100KGridBounds = new EmpBoundingBox();
        EmpBoundingBox labelBounds = new EmpBoundingBox();

        UTMCoordinate.fromLatLong(drawBounds.getSouth(), drawBounds.getWest(), mgrsGridBoxCoord);

        if (null != gridZoneBounds.intersection(drawBounds, labelBounds)) {
            if ((labelBounds.heightAcrossCenter() / metersPerPixel) > TWO_CHARACTER_12PT_PIXEL_WIDTH) {
                if ((labelBounds.widthAcrossCenter() / metersPerPixel) > TWO_CHARACTER_12PT_PIXEL_WIDTH) {
                    labelPos = GridLineUtils.newPosition(labelBounds.centerLatitude(), labelBounds.centerLongitude(), 0);
                    gridObject = createLabelFeature(labelPos, gridZoneUTMCoord.getZoneNumber() + gridZoneUTMCoord.getZoneLetter(), MGRS_GRID_ZONE_LABEL);
                    addFeature(gridObject);
                }
            }
        }
        // Make sure that the northing value is a multiple of 100K.
        tempValue = (int) Math.floor(mgrsGridBoxCoord.getNorthing() / MGRS_100K_METER_GRID) * MGRS_100K_METER_GRID;
        if (mgrsGridBoxCoord.getNorthing() != tempValue) {
            mgrsGridBoxCoord.setNorthing(tempValue);
        }

        // Make sure that the easting value is a multiple of 100K.
        tempValue = (int) Math.floor(mgrsGridBoxCoord.getEasting() / MGRS_100K_METER_GRID) * MGRS_100K_METER_GRID;
        mgrsGridBoxCoord.setEasting(tempValue);

        initialEasting = mgrsGridBoxCoord.getEasting();
        mgrs100KGridBoxSWPos = mgrsGridBoxCoord.toLatLong();
        latitude = mgrs100KGridBoxSWPos.getLatitude();

        while (latitude < mapBounds.getNorth()) {
            while (mgrs100KGridBoxSWPos.getLongitude() < drawBounds.getEast()) {
                tempUTMCoord.copyFrom(mgrsGridBoxCoord);
                tempPos = tempUTMCoord.toLatLong();
                mgrs100KGridBounds.setSouth(tempPos.getLatitude());
                // Make sure that the west is the larger of the 100KGrid and Grid zone west value.
                mgrs100KGridBounds.setWest(Math.max(tempPos.getLongitude(), gridZoneBounds.getWest()));
                tempUTMCoord.setNorthing(tempUTMCoord.getNorthing() + MGRS_100K_METER_GRID);
                tempUTMCoord.setEasting(tempUTMCoord.getEasting() + MGRS_100K_METER_GRID);
                tempPos = tempUTMCoord.toLatLong();
                mgrs100KGridBounds.setNorth(tempPos.getLatitude());
                // Make sure that the east is the smaller of the 100KGrid and Grid zone east value.
                mgrs100KGridBounds.setEast(Math.min(tempPos.getLongitude(), gridZoneBounds.getEast()));

                if (gridSize < MGRS_100K_METER_GRID) {
                    createMGRSGirdValues(gridZoneBounds, mgrsGridBoxCoord, mgrs100KGridBounds, mapBounds, gridSize, tempUTMCoordList);
                }

                // Make sure that the west is the larger of the 100KGrid, Grid zone, or draw bounds west value.
                mgrs100KGridBounds.setWest(Math.max(mgrs100KGridBounds.getWest(), drawBounds.getWest()));
                // Make sure that the east is the smaller of the 100KGrid, draw bound or Grid zone east value.
                mgrs100KGridBounds.setEast(Math.min(mgrs100KGridBounds.getEast(), drawBounds.getEast()));

                if (null != mgrs100KGridBounds.intersection(mapBounds, labelBounds)) {
                    if ((labelBounds.heightAcrossCenter() / metersPerPixel) > TWO_CHARACTER_12PT_PIXEL_WIDTH) {
                        if ((labelBounds.widthAcrossCenter() / metersPerPixel) > TWO_CHARACTER_12PT_PIXEL_WIDTH) {
                            labelPos = GridLineUtils.newPosition(labelBounds.centerLatitude(), labelBounds.centerLongitude(), 0);
                            mgrsGridLabel = get100kID(mgrsGridBoxCoord.getEasting(), mgrsGridBoxCoord.getNorthing(), mgrsGridBoxCoord.getZoneNumber());
                            Log.i(TAG, "Label " + mgrsGridBoxCoord.getZoneNumber() + mgrsGridBoxCoord.getZoneLetter() + " " + mgrsGridLabel + "  lat/lng " + labelPos.getLatitude() + "/" + labelPos.getLongitude());
                            gridObject = createLabelFeature(labelPos, mgrsGridLabel, MGRS_GRID_BOX_LABEL_CENTERED);
                            addFeature(gridObject);
                        }
                    }
                }

                mgrsGridBoxCoord.setEasting(mgrsGridBoxCoord.getEasting() + MGRS_100K_METER_GRID);

                mgrs100KGridBoxSWPos = mgrsGridBoxCoord.toLatLong();
            }
            mgrsGridBoxCoord.setNorthing(mgrsGridBoxCoord.getNorthing() + MGRS_100K_METER_GRID);
            mgrsGridBoxCoord.setEasting(initialEasting);
            mgrs100KGridBoxSWPos = mgrsGridBoxCoord.toLatLong();
            latitude = mgrs100KGridBoxSWPos.getLatitude();
        }
    }

    private void createMGRSGridsForGridzone(UTMCoordinate gridZoneUTMCoord, EmpBoundingBox GridZoneBounds, UTMCoordinate[] tempUTMCoordList,
            EmpBoundingBox mapBounds, EmpBoundingBox drawBounds, int gridSize, double metersPerPixel) {

        if (null == drawBounds) {
            return;
        }

        // Within the UTM grid zone we draw the MGRS grid parallels first, then meridians, then the ID from the left to right, bottom up.
        createMGRSGridParallels(gridZoneUTMCoord, tempUTMCoordList, drawBounds, gridSize);

        // Generate the meridian lines of the box (the vertical lines).
        createMGRSGridMeridians(gridZoneUTMCoord, tempUTMCoordList, drawBounds, gridSize);

        // Now generate the MGRS grid IDs.
        createMGRSGridLabels(gridZoneUTMCoord, GridZoneBounds, tempUTMCoordList, mapBounds, drawBounds, gridSize, metersPerPixel);
    }

    private void createMGRSGridZones(EmpBoundingBox mapBounds, ICamera camera, int viewWidth, int viewHeight, boolean displayLabels) {
        double longitude;
        double latitude;
        int intLongitude;
        int startZoneIndex;
        int endZoneIndex;
        int zoneIndex;
        int minRow;
        int maxRow;
        int iIndex;
        List<IGeoPosition> positionList;
        IFeature gridObject;
        EmpBoundingBox labelBounds = new EmpBoundingBox();
        double metersPerPixel = mapBounds.widthAcrossCenter() / viewWidth;

        double minLatitude = Math.floor(mapBounds.south());
        double maxLatitude = Math.ceil((mapBounds.north()));

        startZoneIndex = (int) Math.floor((mapBounds.west() + 180) / 6.0);
        endZoneIndex = (int) Math.ceil((mapBounds.east() + 180) / 6.0);

        double minLongitude = (double) ((startZoneIndex * 6) - 180);
        double maxLongitude = (double) ((endZoneIndex * 6) - 180);

        zoneIndex = startZoneIndex - 1;
        do {
            zoneIndex = ++zoneIndex % 60;
            intLongitude = (zoneIndex * 6) - 180;
            longitude = (double) intLongitude;
            positionList = new ArrayList<>();

            // Meridian
            latitude = Math.max(minLatitude, -80.0);
            positionList.add(GridLineUtils.newPosition(latitude, longitude, 0));

            if (intLongitude < 6 || intLongitude > 36) {
                latitude = Math.min(maxLatitude, 84.0);
                positionList.add(GridLineUtils.newPosition(latitude, longitude, 0));
            } else if (intLongitude == 6) {
                latitude = Math.min(maxLatitude, 56.0);
                positionList.add(GridLineUtils.newPosition(latitude, longitude, 0));

                if (maxLatitude > 56.0) {
                    gridObject = createPathFeature(positionList, MGRS_GRID_ZONE_MERIDIAN);
                    addFeature(gridObject);

                    latitude = Math.min(maxLatitude, 64.0);
                    positionList = new ArrayList<>();
                    positionList.add(GridLineUtils.newPosition(56.0, longitude - 3.0, 0));
                    positionList.add(GridLineUtils.newPosition(latitude, longitude -3.0, 0));

                    if (maxLatitude > 64.0) {
                        gridObject = createPathFeature(positionList, MGRS_GRID_ZONE_MERIDIAN);
                        addFeature(gridObject);

                        latitude = Math.min(maxLatitude, 72.0);
                        positionList = new ArrayList<>();
                        positionList.add(GridLineUtils.newPosition(64.0, longitude, 0));
                        positionList.add(GridLineUtils.newPosition(latitude, longitude, 0));

                        if (maxLatitude > 72.0) {
                            gridObject = createPathFeature(positionList, MGRS_GRID_ZONE_MERIDIAN);
                            addFeature(gridObject);

                            latitude = Math.min(maxLatitude, 84.0);
                            positionList = new ArrayList<>();
                            positionList.add(GridLineUtils.newPosition(72.0, longitude + 3.0, 0));
                            positionList.add(GridLineUtils.newPosition(latitude, longitude + 3.0, 0));
                        }
                    }
                }
            } else {
                latitude = Math.min(maxLatitude, 72.0);
                positionList.add(GridLineUtils.newPosition(latitude, longitude, 0));

                if (maxLatitude > 72.0) {
                    switch (intLongitude) {
                        case 18:
                        case 30:
                            gridObject = createPathFeature(positionList, MGRS_GRID_ZONE_MERIDIAN);
                            addFeature(gridObject);

                            latitude = Math.min(maxLatitude, 84.0);
                            positionList = new ArrayList<>();
                            positionList.add(GridLineUtils.newPosition(72.0, longitude + 3.0, 0));
                            positionList.add(GridLineUtils.newPosition(latitude, longitude + 3.0, 0));
                            break;
                    }
                }
            }

            gridObject = createPathFeature(positionList, MGRS_GRID_ZONE_MERIDIAN);
            addFeature(gridObject);
        } while (zoneIndex != endZoneIndex);

        // Generate parallels
        minRow = (int) Math.floor((Math.max(minLatitude, -80.0) + 80.0) / 8.0);
        maxRow = (int) Math.ceil((Math.min(maxLatitude, 84.0) + 80.0) / 8.0);

        for (iIndex = minRow; iIndex < maxRow; iIndex++) {
            latitude = (double) ((iIndex * 8) - 80);
            positionList = new ArrayList<>();
            positionList.add(GridLineUtils.newPosition(latitude, minLongitude, 0));
            positionList.add(GridLineUtils.newPosition(latitude, maxLongitude, 0));
            gridObject = createPathFeature(positionList, MGRS_GRID_ZONE_PARALLELS);
            addFeature(gridObject);
        }

        if (displayLabels) {
            // Add The grid zone labels
            zoneIndex = startZoneIndex;
            String zoneLetter;

            while (zoneIndex != endZoneIndex) {
                intLongitude = (zoneIndex * 6) - 180;

                for (iIndex = minRow; iIndex < maxRow; iIndex++) {
                    zoneLetter = latBands.charAt(iIndex) + "";
                    labelBounds.setSouth(UTMCoordinate.getZoneSouthLatitude(zoneIndex + 1, zoneLetter));
                    labelBounds.setWest(UTMCoordinate.getZoneWestLongitude(zoneIndex + 1, zoneLetter));
                    labelBounds.setNorth(labelBounds.getSouth() + UTMCoordinate.getGridZoneHeightInDegrees(zoneIndex + 1, zoneLetter));
                    labelBounds.setEast(labelBounds.getWest() + UTMCoordinate.getGridZoneWidthInDegrees(zoneIndex + 1, zoneLetter));

                    if (null != mapBounds.intersection(labelBounds, labelBounds)) {
                        if ((labelBounds.widthAcrossCenter() / metersPerPixel) > THREE_CHARACTER_12PT_PIXEL_WIDTH) {
                            if ((labelBounds.heightAcrossCenter() / metersPerPixel) > THREE_CHARACTER_12PT_PIXEL_WIDTH) {
                                gridObject = createLabelFeature(GridLineUtils.newPosition(labelBounds.centerLatitude(), labelBounds.centerLongitude(), 0), "" + (zoneIndex + 1) + zoneLetter, MGRS_GRID_ZONE_LABEL);
                                addFeature(gridObject);
                            }
                        }
                    }

                }
                zoneIndex = ++zoneIndex % 60;
            }

            // See if the south UPS is visible.
            if (mapBounds.contains(-85.0, -90.0)) {
                gridObject = createLabelFeature(GridLineUtils.newPosition(-85.0, -90.0, 0), "A", MGRS_GRID_ZONE_LABEL);
                addFeature(gridObject);
            }
            if (mapBounds.contains(-85.0, 90.0)) {
                // Add the south UPS.
                gridObject = createLabelFeature(GridLineUtils.newPosition(-85.0, 90.0, 0), "B", MGRS_GRID_ZONE_LABEL);
                addFeature(gridObject);
            }

            // See if the north UPS is visible.
            if (mapBounds.contains(87.0, -90.0)) {
                gridObject = createLabelFeature(GridLineUtils.newPosition(87.0, -90.0, 0), "Y", MGRS_GRID_ZONE_LABEL);
                addFeature(gridObject);
            }
            if (mapBounds.contains(87.0, 90.0)) {
                // Add the north UPS.
                gridObject = createLabelFeature(GridLineUtils.newPosition(87.0, 90.0, 0), "Z", MGRS_GRID_ZONE_LABEL);
                addFeature(gridObject);
            }
        }
    }

    @Override
    protected void setPathAttributes(Path path, String gridObjectType) {
        if (strokeStyleMap.containsKey(gridObjectType)) {
            path.setStrokeStyle(strokeStyleMap.get(gridObjectType));
        } else {
            super.setPathAttributes(path, gridObjectType);
        }
    }

    @Override
    protected void setLabelAttributes(Text label, String gridObjectType) {
        if (labelStyleMap.containsKey(gridObjectType)) {
            label.setLabelStyle(labelStyleMap.get(gridObjectType));
            switch (gridObjectType) {
                case MGRS_GRID_BOX_EAST_VALUE:
                    label.setAzimuth(-90.0);
                    break;
            }
        } else {
            super.setLabelAttributes(label, gridObjectType);
        }
    }

    /**
     * Given a UTM zone number, figure out the MGRS 100K set it is in.
     *
     * @param zoneNumber An UTM zone number.
     * @return {number} the 100k set the UTM zone is in.
     */
    private int get100kSetForZone(int zoneNumber) {
        int setParm = zoneNumber % NUM_100K_SETS;
        if (setParm == 0) {
            setParm = NUM_100K_SETS;
        }

        return setParm;
    }

    /**
     * Get the two letter 100k designator for a given UTM easting,
     * northing and zone number value.
     *
     * @param easting
     * @param northing
     * @param zoneNumber
     * @return the two letter 100k designator for the given UTM location.
     */
    private String get100kID(double easting, double northing, int zoneNumber) {
        int setParm = get100kSetForZone(zoneNumber);
        int setColumn = (int) Math.floor(easting / 100000);
        int setRow = (int) Math.floor(northing / 100000) % 20;
        return getLetter100kID(setColumn, setRow, setParm);
    }

    /**
     * Get the two-letter MGRS 100k designator given information
     * translated from the UTM northing, easting and zone number.
     *
     * @param column the column index as it relates to the MGRS
     *        100k set spreadsheet, created from the UTM easting.
     *        Values are 1-8.
     * @param row the row index as it relates to the MGRS 100k set
     *        spreadsheet, created from the UTM northing value. Values
     *        are from 0-19.
     * @param parm the set block, as it relates to the MGRS 100k set
     *        spreadsheet, created from the UTM zone. Values are from
     *        1-60.
     * @return two letter MGRS 100k code.
     */
    private String getLetter100kID(int column, int row, int parm) {
        // colOrigin and rowOrigin are the letters at the origin of the set
        int index = parm - 1;
        char colOrigin = SET_ORIGIN_COLUMN_LETTERS.charAt(index);
        char rowOrigin = SET_ORIGIN_ROW_LETTERS.charAt(index);

        // colInt and rowInt are the letters to build to return
        int colInt = colOrigin + column - 1;
        int rowInt = rowOrigin + row;
        boolean rollover = false;

        if (colInt > Z) {
            colInt = colInt - Z + A - 1;
            rollover = true;
        }

        if ((colInt == I) || ((colOrigin < I) && (colInt > I)) || (((colInt > I) || (colOrigin < I)) && rollover)) {
            colInt++;
        }

        if ((colInt == O) || (colOrigin < O && colInt > O) || ((colInt > O || colOrigin < O) && rollover)) {
            colInt++;

            if (colInt == I) {
                colInt++;
            }
        }

        if (colInt > Z) {
            colInt = colInt - Z + A - 1;
        }

        if (rowInt > V) {
            rowInt = rowInt - V + A - 1;
            rollover = true;
        }
        else {
            rollover = false;
        }

        if (((rowInt == I) || ((rowOrigin < I) && (rowInt > I))) || (((rowInt > I) || (rowOrigin < I)) && rollover)) {
            rowInt++;
        }

        if (((rowInt == O) || ((rowOrigin < O) && (rowInt > O))) || (((rowInt > O) || (rowOrigin < O)) && rollover)) {
            rowInt++;

            if (rowInt == I) {
                rowInt++;
            }
        }

        if (rowInt > V) {
            rowInt = rowInt - V + A - 1;
        }

        //String twoLetter = String.fromCharCode(colInt) + String.fromCharCode(rowInt);
        String twoLetter = Character.toString((char) colInt) + Character.toString((char) rowInt);
        return twoLetter;
    }
}
