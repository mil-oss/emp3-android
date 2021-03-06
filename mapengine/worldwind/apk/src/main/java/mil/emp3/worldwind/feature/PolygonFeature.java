package mil.emp3.worldwind.feature;

import android.util.Log;

import gov.nasa.worldwind.render.Renderable;
import mil.emp3.api.Polygon;
import mil.emp3.worldwind.MapInstance;
import mil.emp3.worldwind.feature.support.BufferGenerator;

/**
 * This class implements the mapping between an EMP Polygon feature and the WW renderables.
 */

public class PolygonFeature extends FeatureRenderableMapping<Polygon> {
    private static final String TAG = PolygonFeature.class.getSimpleName();

    public PolygonFeature(Polygon feature, MapInstance instance) {
        super(feature, instance);
    }

    /**
     * Create a Polygon and add the newly created positions, set the Fill Style, and create WW polygon
     * @param buffer The buffer distance in meters.
     * @return
     */
    protected Renderable generateBuffer(double buffer) {
        try {
            Polygon bufferPolygon = BufferGenerator.generateBufferPolygon(getFeature(), getMapInstance(), buffer);
            if(null != bufferPolygon) {
                return (this.createWWPolygon(bufferPolygon, false));
            }
        } catch(Exception e) {
            Log.e(TAG, "generateBuffer buffer " + buffer, e);
        }
        return null;
    }
}
