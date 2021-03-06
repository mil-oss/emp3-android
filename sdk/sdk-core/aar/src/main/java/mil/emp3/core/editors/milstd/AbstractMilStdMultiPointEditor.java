package mil.emp3.core.editors.milstd;

import org.cmapi.primitives.IGeoAltitudeMode;
import org.cmapi.primitives.IGeoMilSymbol;

import mil.emp3.api.MilStdSymbol;
import mil.emp3.api.exceptions.EMP_Exception;
import mil.emp3.api.listeners.IDrawEventListener;
import mil.emp3.api.listeners.IEditEventListener;
import mil.emp3.core.editors.AbstractDrawEditEditor;
import mil.emp3.core.utils.milstd2525.MilStdUtilities;
import mil.emp3.mapengine.interfaces.IMapInstance;

/**
 * This class is the base class for the MilStd Multi Point tactical graphic editors.
 */
public abstract class AbstractMilStdMultiPointEditor extends AbstractDrawEditEditor<MilStdSymbol> {
    protected final armyc2.c2sd.renderer.utilities.SymbolDef symbolDefinition;
    protected final String basicSymbolCode;
    protected final int milstdVersion;
    private final java.util.HashMap<IGeoMilSymbol.Modifier, String> originalModifiers = new java.util.HashMap<>();

    protected AbstractMilStdMultiPointEditor(IMapInstance map, MilStdSymbol feature, IEditEventListener oEventListener, armyc2.c2sd.renderer.utilities.SymbolDef symDef) throws EMP_Exception {
        super(map, feature, oEventListener, true);

        this.milstdVersion = MilStdUtilities.geoMilStdVersionToRendererVersion(this.oFeature.getSymbolStandard());
        this.basicSymbolCode = this.oFeature.getBasicSymbol();
        this.symbolDefinition = symDef;

        // Copy the modifiers if there are any.
        for (IGeoMilSymbol.Modifier modifier: oFeature.getModifiers().keySet()) {
            this.originalModifiers.put(modifier, new String(oFeature.getStringModifier(modifier)));
        }
    }

    protected AbstractMilStdMultiPointEditor(IMapInstance map, MilStdSymbol feature, IDrawEventListener oEventListener, armyc2.c2sd.renderer.utilities.SymbolDef symDef) throws EMP_Exception {
        super(map, feature, oEventListener, true);

        if (this.oFeature.getAltitudeMode() == null) {
            this.oFeature.setAltitudeMode(IGeoAltitudeMode.AltitudeMode.CLAMP_TO_GROUND);
        }
        this.milstdVersion = MilStdUtilities.geoMilStdVersionToRendererVersion(oFeature.getSymbolStandard());
        this.basicSymbolCode = oFeature.getBasicSymbol();
        this.symbolDefinition = symDef;
    }

    @Override
    public void Cancel() {
        if (this.inEditMode()) {
            // Restore the modifiers.
            java.util.HashMap<IGeoMilSymbol.Modifier, String> symbolModifiers = oFeature.getModifiers();
            symbolModifiers.clear();
            for (IGeoMilSymbol.Modifier modifier: this.originalModifiers.keySet()) {
                symbolModifiers.put(modifier, this.originalModifiers.get(modifier));
            }
        }
        super.Cancel();
    }

    protected int getMinPoints() {
        return this.symbolDefinition.getMinPoints();
    }

    protected int getMaxPoints() {
        return this.symbolDefinition.getMaxPoints();
    }
}
