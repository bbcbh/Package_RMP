package sim;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import run.Run_Population_Remote_MetaPopulation_Pop_Intro_NG_CT;
import run.Run_Population_Remote_MetaPopulation_Pop_Intro_Syphilis;
import util.PersonClassifier;
import util.PropValUtils;

/**
 * Define a set of simulation using properties file
 *
 * @author Ben Hui
 * @version 20180531
 */
public class Simulation_Remote_MetaPopulation implements SimulationInterface {

    public static final String[] PROP_NAME_RMP = {
        "PROP_RMP_SIM_TYPE", "PROP_STORE_INFECTION_HISTORY"
    };
    public static final Class[] PROP_CLASS_RMP = {
        Integer.class, // 0 = NG_CT, 1 = Syphilis
        Boolean.class
    };
    public static final int PROP_RMP_SIM_TYPE = PROP_NAME.length;
    public static final int PROP_STORE_INFECTION_HISTORY = PROP_RMP_SIM_TYPE + 1;

    public static final String POP_PROP_INIT_PREFIX = "POP_PROP_INIT_PREFIX_";
    protected String[] propModelInitStr = null;

    protected Object[] propVal = new Object[PROP_NAME.length + PROP_NAME_RMP.length];
    protected File baseDir = new File("");

    protected boolean stopNextTurn = false;

    @Override
    public void loadProperties(Properties prop) {
        for (int i = 0; i < PROP_NAME.length; i++) {
            String ent = prop.getProperty(PROP_NAME[i]);
            if (ent != null) {
                propVal[i] = PropValUtils.propStrToObject(ent, PROP_CLASS[i]);
            }
        }
        for (int i = PROP_NAME.length; i < propVal.length; i++) {
            String ent = prop.getProperty(PROP_NAME_RMP[i - PROP_NAME.length]);
            if (ent != null) {
                propVal[i] = PropValUtils.propStrToObject(ent, PROP_CLASS_RMP[i - PROP_NAME.length]);
            }
        }

        int maxFieldNum = 0;
        for (Iterator<Object> it = prop.keySet().iterator(); it.hasNext();) {
            String k = (String) it.next();
            if (k.startsWith(POP_PROP_INIT_PREFIX)) {
                if (prop.getProperty(k) != null) {
                    maxFieldNum = Math.max(maxFieldNum,
                            Integer.parseInt(k.substring(POP_PROP_INIT_PREFIX.length())));
                }
            }
        }

        if (maxFieldNum >= 0) {
            propModelInitStr = new String[maxFieldNum + 1];
            for (int i = 0; i < propModelInitStr.length; i++) {
                String res = prop.getProperty(POP_PROP_INIT_PREFIX + i);
                if (res != null) {
                    propModelInitStr[i] = res;
                }
            }
        }
    }

    @Override
    public Properties generateProperties() {
        Properties prop = new Properties();
        for (int i = 0; i < PROP_NAME.length; i++) {
            prop.setProperty(PROP_NAME[i], PropValUtils.objectToPropStr(propVal[i], PROP_CLASS[i]));
        }
        for (int i = PROP_CLASS.length; i < propVal.length; i++) {
            prop.setProperty(PROP_NAME_RMP[i - PROP_NAME.length],
                    PropValUtils.objectToPropStr(propVal[i], PROP_CLASS_RMP[i - PROP_CLASS.length]));
        }

        return prop;
    }

    @Override
    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public void setSnapshotSetting(PersonClassifier[] snapshotCountClassifier, boolean[] snapshotCountAccum) {
        throw new UnsupportedOperationException("Not supported in this version.");
    }

    @Override
    public void setStopNextTurn(boolean stopNextTurn) {
        throw new UnsupportedOperationException("Not supported in this version.");
    }

    @Override
    public void generateOneResultSet() throws IOException, InterruptedException {
        int simType = ((Integer) propVal[PROP_RMP_SIM_TYPE]);
        String[] rArg;

        switch (simType) {
            case 0:
                rArg = new String[4];
                // 0: Base Dir
                // 1: Import Dir
                // 2: Num thread
                // 3: Num sim
                rArg[0] = baseDir.getAbsolutePath();
                rArg[1] = propVal[PROP_POP_IMPORT_PATH] == null ? "" : (String) propVal[PROP_POP_IMPORT_PATH];
                rArg[2] = propVal[PROP_USE_PARALLEL] == null ? "" : ((Integer) propVal[PROP_USE_PARALLEL]).toString();
                rArg[3] = propVal[PROP_NUM_SIM_PER_SET] == null ? "" : ((Integer) propVal[PROP_NUM_SIM_PER_SET]).toString();
                rArg[4] = propVal[PROP_NUM_SNAP] == null ? "" : ((Integer) propVal[PROP_NUM_SNAP]).toString();
                Run_Population_Remote_MetaPopulation_Pop_Intro_NG_CT runNGCT = new Run_Population_Remote_MetaPopulation_Pop_Intro_NG_CT(rArg);
                if (propModelInitStr != null) {
                    for (int i = 0; i < propModelInitStr.length; i++) {
                        if (propModelInitStr[i] != null && !propModelInitStr[i].isEmpty()) {
                            runNGCT.getParamValues()[i] = Double.parseDouble(propModelInitStr[i]);
                        }
                    }
                }
                runNGCT.runSimulation();
                break;
            case 1:
                // 0: Base Dir
                // 1: Import Dir
                // 2: Num thread
                // 3: Num sim
                // 4: Num step - in this case it is number of snap
                // 5: Sample Freq
                // 6: Store infection history
                rArg = new String[7];
                rArg[0] = baseDir.getAbsolutePath();
                rArg[1] = propVal[PROP_POP_IMPORT_PATH] == null ? "" : (String) propVal[PROP_POP_IMPORT_PATH];
                rArg[2] = propVal[PROP_USE_PARALLEL] == null ? "" : ((Integer) propVal[PROP_USE_PARALLEL]).toString();
                rArg[3] = propVal[PROP_NUM_SIM_PER_SET] == null ? "" : ((Integer) propVal[PROP_NUM_SIM_PER_SET]).toString();
                rArg[4] = propVal[PROP_NUM_SNAP] == null ? "" : ((Integer) propVal[PROP_NUM_SNAP]).toString();
                rArg[5] = propVal[PROP_SNAP_FREQ] == null ? "" : ((Integer) propVal[PROP_SNAP_FREQ]).toString();
                rArg[6] = propVal[PROP_STORE_INFECTION_HISTORY] == null ? "" : ((Boolean) propVal[PROP_STORE_INFECTION_HISTORY]).toString();
                Run_Population_Remote_MetaPopulation_Pop_Intro_Syphilis runSyp = new Run_Population_Remote_MetaPopulation_Pop_Intro_Syphilis(rArg);

                if (propModelInitStr != null) {
                    for (int i = 0; i < propModelInitStr.length; i++) {
                        if (propModelInitStr[i] != null && !propModelInitStr[i].isEmpty()) {
                            runSyp.getParamValues()[i] = Double.parseDouble(propModelInitStr[i]);
                        }
                    }
                }

                runSyp.runSimulation();

                break;
            default:
                System.err.println("Error: Illegal PROP_RMP_SIM_TYPE. Set 0 for NG/CT and 1 for Syphilis");

        }

    }

}
