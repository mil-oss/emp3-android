package mil.emp3.dev_test_sdk;

import android.util.Log;
import android.widget.TextView;

import org.cmapi.primitives.IGeoAltitudeMode;
import org.cmapi.primitives.IGeoMilSymbol;
import org.cmapi.primitives.IGeoPosition;

import armyc2.c2sd.renderer.utilities.UnitDef;
import armyc2.c2sd.renderer.utilities.UnitDefTable;
import mil.emp3.api.MilStdSymbol;
import mil.emp3.api.exceptions.EMP_Exception;
import mil.emp3.api.interfaces.IFeature;
import mil.emp3.api.utils.GeoLibrary;

/**
 * This class execute the performance test
 */
class PerformanceTestThread extends Thread {
    private static final String TAG = PerformanceTestThread.class.getSimpleName();
    private long COUNT_PER_INTERVAL = 200;
    private long UPDATE_INTERVAL = 200;
    private String[] aTrackSymbol = {"SFAPMFF--------", "SFAPMFA--------", "SFAPMFJ--------", "SFAPMFL--------", "SFAPMHQ--------", "SFAPMHO--------", "SHAPMFF--------", "SHAPMFA--------", "SHAPMFJ--------", "SHAPMFL--------", "SHAPMHQ--------", "SHAPMHO--------", "SNAPCF---------", "SFAPCF---------", "SFAPCH---------"};
    private int iTrackSymbolCount = this.aTrackSymbol.length;
    private MilStdSymbol.Affiliation[] aAffiliations = {MilStdSymbol.Affiliation.FRIEND, MilStdSymbol.Affiliation.NEUTRAL, MilStdSymbol.Affiliation.HOSTILE};
    private final int iAffCount = aAffiliations.length;
    private int iCount = 2000;
    private boolean bAiffChange = false;
    private java.util.List<IFeature> oFeatureList = new java.util.ArrayList<>();
    private boolean bContinue = true;
    private final int TIME_LIST_SIZE = 100;
    private int iTimelistIndex = 0;
    private long[] alTimeList = new long[TIME_LIST_SIZE];
    private double dTimeSum = 0.0;
    private int iTimeSamples = 0;
    private final MainActivity oMainActivity;
    private String sMessage;
    private boolean bBatchUpdate = false;

    public PerformanceTestThread(MainActivity oMain, int count, boolean bAffChg, boolean bBatch) {
        this.iCount = count;
        this.bAiffChange = bAffChg;
        this.oMainActivity = oMain;
        this.bBatchUpdate = bBatch;
    }

    private void createTracks() {
        UnitDefTable oDefTable = UnitDefTable.getInstance();
        org.cmapi.primitives.GeoMilSymbol.SymbolStandard eStandard =  org.cmapi.primitives.GeoMilSymbol.SymbolStandard.MIL_STD_2525C;
        int iMilStdVersion = (eStandard == org.cmapi.primitives.GeoMilSymbol.SymbolStandard.MIL_STD_2525B)? armyc2.c2sd.renderer.utilities.MilStdSymbol.Symbology_2525Bch2_USAS_13_14:
                armyc2.c2sd.renderer.utilities.MilStdSymbol.Symbology_2525C;
        java.util.Map<java.lang.String, UnitDef> oDefMap = oDefTable.getAllUnitDefs(iMilStdVersion);
        UnitDef oUnitDef;
        java.util.List<IGeoPosition> oPosList;
        int iNextTrackSymbol = 0;
        int iNextAffiliation = 0;
        String sSymbolCode;
        int iDirectionOfMovement = 0;
        int iSpeed = 600;

        //this.oMainActivity.removeAllFeatures();
        long startTime = System.currentTimeMillis();
        for (int iIndex = 0; iIndex < this.iCount; iIndex++) {
            try {
                oPosList = new java.util.ArrayList<>();
                oPosList.add(this.oMainActivity.getRandomCoordinate());

                sSymbolCode = aTrackSymbol[iNextTrackSymbol];
                iNextTrackSymbol++;
                iNextTrackSymbol %= this.iTrackSymbolCount;

                oUnitDef = oDefMap.get(armyc2.c2sd.renderer.utilities.SymbolUtilities.getBasicSymbolID(sSymbolCode));

                // Allocate the new MilStd Symbol with a MilStd version and the symbol code.
                mil.emp3.api.MilStdSymbol oSPSymbol = new mil.emp3.api.MilStdSymbol(IGeoMilSymbol.SymbolStandard.MIL_STD_2525C, sSymbolCode);

                // Set the symbols affiliation.
                oSPSymbol.setAffiliation(this.aAffiliations[iNextAffiliation]);
                iNextAffiliation++;
                iNextAffiliation %= this.iAffCount;

                // Set the echelon.
                oSPSymbol.setEchelonSymbolModifier(MilStdSymbol.EchelonSymbolModifier.UNIT, MilStdSymbol.Echelon.UNIT);
                // Set the symbols altitude mode.
                //oSPSymbol.setAltitudeMode(IGeoAltitudeMode.AltitudeMode.RELATIVE_TO_GROUND);
                //oSPSymbol.setEchelonSymbolModifier(MilStdSymbol.EchelonSymbolModifier.HQ_BRIGADE);

                // Set the position list with 1 position.
                oSPSymbol.getPositions().clear();
                oSPSymbol.getPositions().addAll(oPosList);

                // Give the feature a name.
                oSPSymbol.setName("Unit " + iIndex);

                // Set Direction of movement.
                oSPSymbol.setModifier(IGeoMilSymbol.Modifier.DIRECTION_OF_MOVEMENT, iDirectionOfMovement + "");

                oSPSymbol.setModifier(IGeoMilSymbol.Modifier.UNIQUE_DESIGNATOR_1, oUnitDef.getDescription());
                iDirectionOfMovement += 10;
                iDirectionOfMovement %= 360;

                // Set Speed.
                oSPSymbol.setModifier(IGeoMilSymbol.Modifier.SPEED, iSpeed + "");

                //Add it to the list we will be adding to the overlay.
                oFeatureList.add(oSPSymbol);
                this.oMainActivity.oFeatureHash.put(oSPSymbol.getGeoId(), oSPSymbol);
            } catch (EMP_Exception e) {
                e.printStackTrace();
            }
        }

        try {
            this.oMainActivity.oRootOverlay.addFeatures(oFeatureList, true);
        } catch (EMP_Exception e) {
            e.printStackTrace();
        }

        for (int iIndex = 0; iIndex < this.TIME_LIST_SIZE; iIndex++) {
            this.alTimeList[iIndex] = 0;
        }

        Log.i(TAG, "Track creation time " + (System.currentTimeMillis() - startTime) + " ms");
    }

    private void removeTracks() {
        try {
            this.oMainActivity.oRootOverlay.removeFeatures(this.oFeatureList);
            for (IFeature feature: this.oFeatureList) {
                this.oMainActivity.oFeatureHash.remove(feature.getGeoId());
            }
        } catch (EMP_Exception e) {
            e.printStackTrace();
        }
        this.oFeatureList.clear();
    }

    @Override
    public void run() {
        mil.emp3.api.MilStdSymbol oSPSymbol;
        int iNextFeature = 0;
        long lStartTimestamp;
        long lDeltaTime = 0;
        IGeoPosition oPos, oNewPos;
        int iDOM;
        double dSpeed;
        double dDistance;
        double dRandom;
        long lLastMoved = System.currentTimeMillis();
        long dWaitTime  = this.UPDATE_INTERVAL; // msec
        java.util.List<IFeature> oBatchList = new java.util.ArrayList<>();

        this.createTracks();

        while (bContinue) {
            try {
                oBatchList.clear();
                sleep(dWaitTime);
                lStartTimestamp = System.currentTimeMillis();
                for (int iIndex = 0; iIndex < this.COUNT_PER_INTERVAL; iIndex++) {
                    oSPSymbol = (mil.emp3.api.MilStdSymbol) this.oFeatureList.get(iNextFeature);
                    oPos = oSPSymbol.getPosition();

                    iDOM = Integer.parseInt(oSPSymbol.getStringModifier(IGeoMilSymbol.Modifier.DIRECTION_OF_MOVEMENT));
                    dRandom = Math.random();
                    if (dRandom <= 0.2) {
                        // Make the heading 10Deg CCW
                        iDOM += 350;
                        iDOM %= 360;
                        oSPSymbol.setModifier(IGeoMilSymbol.Modifier.DIRECTION_OF_MOVEMENT, iDOM + "");
                    } else if (dRandom >= 0.8) {
                        // Make the heading 10Deg CW
                        iDOM += 10;
                        iDOM %= 360;
                        oSPSymbol.setModifier(IGeoMilSymbol.Modifier.DIRECTION_OF_MOVEMENT, iDOM + "");
                    }
                    dSpeed = (double) Integer.parseInt(oSPSymbol.getStringModifier(IGeoMilSymbol.Modifier.SPEED));
                    dDistance = dSpeed * 1609.0 * (double)(lStartTimestamp - lLastMoved) / 3.6e6;

                    oNewPos = GeoLibrary.computePositionAt((double) iDOM, dDistance, oPos);
                    oPos.setLatitude(oNewPos.getLatitude());
                    oPos.setLongitude(oNewPos.getLongitude());

                    if (this.bAiffChange) {
                        dRandom = Math.random();
                        if (dRandom <= 0.25) {
                            // Change the Affilication.
                            int iAffIndex = (int) Math.floor(Math.random() * this.iAffCount) % this.iAffCount;
                            oSPSymbol.setAffiliation(this.aAffiliations[iAffIndex]);
                        }
                    }

                    if (this.bBatchUpdate) {
                        oBatchList.add(oSPSymbol);
                    } else {
                        oSPSymbol.apply();
                    }

                    iNextFeature++;
                    iNextFeature %= this.iCount;
                    if (iNextFeature == 0) {
                        lLastMoved = lStartTimestamp;
                        break;
                    }
                }

                if (this.bBatchUpdate) {
                    try {
                        for (IFeature oSymbol: oBatchList) {
                            oSymbol.apply();
                        }
                        //if (oBatchList.size() > 0) {
                        //    this.oMainActivity.oRootOverlay.addFeatures(oBatchList, true);
                        //}
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                    // Cal the time it took to update the COUNT_PER_INTERVAL units.
                lDeltaTime = System.currentTimeMillis() - lStartTimestamp;
                dWaitTime = (lDeltaTime < this.UPDATE_INTERVAL)? this.UPDATE_INTERVAL - lDeltaTime: 50;
                if (this.alTimeList[this.iTimelistIndex] != 0) {
                    iTimeSamples--;
                    this.dTimeSum -= (double) this.alTimeList[this.iTimelistIndex];
                }
                iTimeSamples++;
                this.alTimeList[this.iTimelistIndex] = lDeltaTime;
                this.dTimeSum += (double) lDeltaTime;
                this.iTimelistIndex++;
                this.iTimelistIndex %= this.TIME_LIST_SIZE;
                sMessage = String.format("Update %1$,4d features in %2$,4d msec. Avg of %3$,4d = %4$8.3f msec", this.COUNT_PER_INTERVAL, lDeltaTime, iTimeSamples,
                        this.dTimeSum / (double) iTimeSamples);

                oMainActivity.runOnUiThread(new Runnable() {
                    final String sMsg = PerformanceTestThread.this.sMessage;
                    final TextView oResults = PerformanceTestThread.this.oMainActivity.oResults;
                    @Override
                    public void run() {
                        this.oResults.setText(this.sMsg);
                    }
                });

            } catch (InterruptedException Ex) {

            }
        }
        this.removeTracks();
    }

    public void stopTest() {
        this.bContinue = false;
        this.interrupt();
    }
}
