package mil.emp3.core.mapgridlines;

import android.util.Log;

import org.cmapi.primitives.IGeoBounds;
import org.cmapi.primitives.IGeoPosition;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;

import mil.emp3.api.Camera;
import mil.emp3.api.Path;
import mil.emp3.api.Text;
import mil.emp3.api.interfaces.ICamera;
import mil.emp3.api.interfaces.IFeature;
import mil.emp3.api.utils.EmpBoundingBox;
import mil.emp3.mapengine.events.MapInstanceViewChangeEvent;
import mil.emp3.mapengine.interfaces.ICoreMapGridLineGenerator;
import mil.emp3.mapengine.interfaces.IMapGridLines;
import mil.emp3.mapengine.interfaces.IMapInstance;

/**
 * This abstract class is the base class for all map grid line classes.
 */

public abstract class AbstractMapGridLine implements IMapGridLines, ICoreMapGridLineGenerator {
    private static final String TAG = AbstractMapGridLine.class.getSimpleName();

    private Date lastUpdated;
    private final List<IFeature> featureList = new ArrayList<>();
    protected final ICamera currentCamera;
    protected final ICamera previousCamera;
    private final EmpBoundingBox boundingBox;
    private final IMapInstance mapInstance;
    private int viewWidth;
    private int viewHeight;
    private GridLineGenerationThread generationThread;

    private class GridLineGenerationThread extends java.lang.Thread {
        private final Semaphore processEvent;
        private boolean NotDone = true;

        protected GridLineGenerationThread() {
            this.processEvent = new Semaphore(0);
        }

        @Override
        public void run() {
            while (NotDone) {
                try {
                    this.processEvent.acquire();

                    long startTS = System.currentTimeMillis();
                    processViewChange(boundingBox, currentCamera, viewWidth, viewHeight);
                    Log.i(TAG, "feature generation in " + (System.currentTimeMillis() - startTS) + " ms. " + featureList.size() + " features.");
                    previousCamera.copySettingsFrom(currentCamera);
                    mapInstance.scheduleMapRedraw();

                } catch (InterruptedException e) {
                    NotDone = false;
                }
            }
        }

        public void scheduleProcessing() {
            this.processEvent.release();
        }

        public void exitThread() {
            this.NotDone = false;
            this.interrupt();
            try {
                this.join();
            } catch (InterruptedException e) {
            }
        }
    }

    protected AbstractMapGridLine(IMapInstance mapInstance) {
        this.lastUpdated = new Date();
        this.currentCamera = new Camera();
        this.previousCamera = new Camera();
        this.boundingBox = new EmpBoundingBox();
        this.mapInstance = mapInstance;
        this.generationThread = new GridLineGenerationThread();
        this.generationThread.setPriority(this.generationThread.getPriority() + 1);
        this.generationThread.start();
    }

    @Override
    public void shutdownGenerator() {
        if (null != this.generationThread) {
            this.generationThread.exitThread();
            this.generationThread = null;
        }
    }

    @Override
    public Date getLastUpdated() {
        return this.lastUpdated;
    }

    @Override
    public List<IFeature> getGridFeatures() {
        return this.featureList;
    }

    protected void clearFeatureList() {
        boolean resetTime = (this.featureList.size() > 0);
        this.featureList.clear();

        if (resetTime) {
            this.lastUpdated.setTime(System.currentTimeMillis());
        }
    }

    protected void addFeature(IFeature feature) {
        if (null == feature) {
            throw new InvalidParameterException("feature is null.");
        }
        this.featureList.add(feature);
        this.lastUpdated.setTime(System.currentTimeMillis());
    }

    @Override
    public void mapViewChange(IGeoBounds mapBounds, ICamera camera, int viewWidth, int viewHeight) {
        if ((null == mapBounds) || (null == camera)) {
            clearFeatureList();
            this.mapInstance.scheduleMapRedraw();
            return;
        }

        this.boundingBox.copyFrom(mapBounds);
        this.currentCamera.copySettingsFrom(camera);
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;

        this.generationThread.scheduleProcessing();
    }

    /**
     * This method will not get called if the bounds are null.
     * @param mapBounds
     * @param camera
     * @param viewWidth
     * @param viewHeight
     */
    protected abstract void processViewChange(EmpBoundingBox mapBounds, ICamera camera, int viewWidth, int viewHeight);

    protected abstract void setPathAttributes(Path path, String gridObjectType);

    protected abstract void setLabelAttributes(Text label, String gridObjectType);

    protected IFeature createPathFeature(List<IGeoPosition> positionList, String gridObjectType) {
        Path path = new Path(positionList);

        setPathAttributes(path, gridObjectType);

        return path;
    }

    protected IFeature createLabelFeature(IGeoPosition position, String text, String gridObjectType) {
        Text label = new Text(text);

        label.setPosition(position);
        setLabelAttributes(label, gridObjectType);
        return label;
    }

    protected boolean shouldGridRedraw(ICamera camera) {

        if (Math.abs(this.previousCamera.getLongitude() - camera.getLongitude()) >= 3.0) {
            // Redraw if the longitude changes by 3 deg or more.
            return true;
        }

        if (Math.abs(this.previousCamera.getLatitude() - camera.getLatitude()) >= 4.0) {
            // redraw if the latitude changed by 4 or more deg.
            return true;
        }

        if (Math.abs(this.previousCamera.getAltitude() - camera.getAltitude()) > 1e3) {
            return true;
        }

        if (Math.abs(this.previousCamera.getTilt() - camera.getTilt()) > 2.0) {
            return true;
        }
        return false;
    }
}