package sim;

import run.Run_Population_Remote_MetaPopulation_COVID19;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Pattern;
import opt.OptRun_Population_Remote_MetaPopulation_Infection_Intro;
import opt.OptRun_Population_Remote_MetaPopulation_Infection_Intro_GA;
import run.Run_Population_Remote_MetaPopulation_Pop_Analysis;
import run.Run_Population_Remote_MetaPopulation_Pop_Generate;
import run.Run_Population_Remote_MetaPopulation_Pop_Intro_NG_CT;
import run.Run_Population_Remote_MetaPopulation_Pop_Intro_Syphilis;
import util.PersonClassifier;
import util.PropValUtils;

/**
 * Define a set of simulation using properties file
 *
 * @author Ben Hui
 * @version 20190529
 *
 * <pre>
 * History:
 *
 * 20180612:
 *   - Add popAnalysis method
 * 20180618:
 *   - Redefine input for syphilis imulation runs
 * 20180620:
 *   - Add decode collection file for syphilis simulation runs
 * 20180622:
 *   - Uniting input format for both NG_CT and syphilis
 * 20180718:
 *   - Add support for optimisation
 * 20180815:
 *   - Add support for pop selection
 * 20180829:
 *   - Add support for pop generate
 * 20180831:
 *   - Add support for pop generate with custom popType and popConnc
 * 20180907:
 *   - Add support for PROP_STORE_TESTING_HISTORY and PROP_STORE_TREATMENT_HISTORY
 * 20190529
 *   - Add support for wildcard notiation when running sim directory
 *
 *
 * </pre>
 */
public class Simulation_Remote_MetaPopulation implements SimulationInterface {

    public static final String[] PROP_NAME_RMP = {
        "PROP_RMP_SIM_TYPE", "PROP_STORE_INFECTION_HISTORY", "PROP_STORE_TESTING_HISTORY", "PROP_STORE_TREATMENT_HISTORY",
        "PROP_RMP_OPT_TARGET", "PROP_RMP_OPT_WEIGHT",};
    public static final Class[] PROP_CLASS_RMP = {
        Integer.class, // 0 = NG_CT, 1 = Syphilis
        Boolean.class,
        Boolean.class,
        Boolean.class,
        double[].class,
        double[].class,};

    public static final int PROP_RMP_SIM_TYPE = PROP_NAME.length;
    public static final int PROP_STORE_INFECTION_HISTORY = PROP_RMP_SIM_TYPE + 1;
    public static final int PROP_STORE_TESTING_HISTORY = PROP_STORE_INFECTION_HISTORY + 1;
    public static final int PROP_STORE_TREATMENT_HISTORY = PROP_STORE_TESTING_HISTORY + 1;
    public static final int PROP_RMP_OPT_TARGET = PROP_STORE_TREATMENT_HISTORY + 1;
    public static final int PROP_RMP_OPT_WEIGHT = PROP_RMP_OPT_TARGET + 1;

    public static final String POP_PROP_INIT_PREFIX = "POP_PROP_INIT_PREFIX_";
    protected String[] propModelInitStr = null;

    protected Object[] propVal = new Object[PROP_NAME.length + PROP_NAME_RMP.length];
    protected File baseDir = new File("");

    protected boolean stopNextTurn = false;
    protected String extraFlag = "";

    public void setExtraFlag(String extraFlag) {
        this.extraFlag = extraFlag;
    }

    @Override
    public void loadProperties(Properties prop) {
        for (int i = 0; i < PROP_NAME.length; i++) {
            String ent = prop.getProperty(PROP_NAME[i]);
            if (ent != null && ent.length() > 0) {
                propVal[i] = PropValUtils.propStrToObject(ent, PROP_CLASS[i]);
            }
        }
        for (int i = PROP_NAME.length; i < propVal.length; i++) {
            String ent = prop.getProperty(PROP_NAME_RMP[i - PROP_NAME.length]);
            if (ent != null && ent.length() > 0) {
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
                if (res != null && res.length() > 0) {
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
                rArg = new String[6];
                // 0: Base Dir
                // 1: Import Dir
                // 2: Num thread
                // 3: Num sim
                // 4: Num step - in this case it is PROP_NUM_SNAP * PROP_SNAP_FREQ
                // 5: Sample Freq
                rArg[0] = baseDir.getAbsolutePath();
                rArg[1] = propVal[PROP_POP_IMPORT_PATH] == null ? "" : (String) propVal[PROP_POP_IMPORT_PATH];
                rArg[2] = propVal[PROP_USE_PARALLEL] == null ? "" : ((Integer) propVal[PROP_USE_PARALLEL]).toString();
                rArg[3] = propVal[PROP_NUM_SIM_PER_SET] == null ? "" : ((Integer) propVal[PROP_NUM_SIM_PER_SET]).toString();
                rArg[4] = propVal[PROP_NUM_SNAP] == null ? ""
                        : Integer.toString(((Integer) propVal[PROP_NUM_SNAP]) * ((Integer) propVal[PROP_SNAP_FREQ]));
                rArg[5] = propVal[PROP_SNAP_FREQ] == null ? "" : ((Integer) propVal[PROP_SNAP_FREQ]).toString();

                Run_Population_Remote_MetaPopulation_Pop_Intro_NG_CT runNGCT = new Run_Population_Remote_MetaPopulation_Pop_Intro_NG_CT(rArg);

                runNGCT.setStoreInfectionHistory(propVal[PROP_STORE_INFECTION_HISTORY] == null ? false : ((Boolean) propVal[PROP_STORE_INFECTION_HISTORY]));
                runNGCT.setStoreInfectionHistory(propVal[PROP_STORE_TESTING_HISTORY] == null ? false : ((Boolean) propVal[PROP_STORE_TESTING_HISTORY]));
                runNGCT.setStoreInfectionHistory(propVal[PROP_STORE_TREATMENT_HISTORY] == null ? false : ((Boolean) propVal[PROP_STORE_TREATMENT_HISTORY]));

                if (propVal[PROP_POP_SELECT_CSV] != null) {
                    runNGCT.setPopSelectionCSV((String) propVal[PROP_POP_SELECT_CSV]);
                }

                if (propModelInitStr != null) {
                    for (int i = 0; i < propModelInitStr.length; i++) {
                        if (propModelInitStr[i] != null && !propModelInitStr[i].isEmpty()) {
                            if (i < runNGCT.getRunParamValues().length) {
                                runNGCT.getRunParamValues()[i] = Double.parseDouble(propModelInitStr[i]);
                            } else if (i - runNGCT.getRunParamValues().length < runNGCT.getThreadParamValStr().length) {
                                runNGCT.getThreadParamValStr()[i - runNGCT.getRunParamValues().length] = propModelInitStr[i];
                            } else {
                                runNGCT.setPopParamValStr(i - runNGCT.getRunParamValues().length - runNGCT.getThreadParamValStr().length,
                                        propModelInitStr[i]);
                            }
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
                // 4: Num step - in this case it is PROP_NUM_SNAP * PROP_SNAP_FREQ
                // 5: Sample Freq
                // 6: Store infection history
                rArg = new String[7];
                rArg[0] = baseDir.getAbsolutePath();
                rArg[1] = propVal[PROP_POP_IMPORT_PATH] == null ? "" : (String) propVal[PROP_POP_IMPORT_PATH];
                rArg[2] = propVal[PROP_USE_PARALLEL] == null ? "" : ((Integer) propVal[PROP_USE_PARALLEL]).toString();
                rArg[3] = propVal[PROP_NUM_SIM_PER_SET] == null ? "" : ((Integer) propVal[PROP_NUM_SIM_PER_SET]).toString();
                rArg[4] = propVal[PROP_NUM_SNAP] == null ? ""
                        : Integer.toString(((Integer) propVal[PROP_NUM_SNAP]) * ((Integer) propVal[PROP_SNAP_FREQ]));
                rArg[5] = propVal[PROP_SNAP_FREQ] == null ? "" : ((Integer) propVal[PROP_SNAP_FREQ]).toString();
                rArg[6] = propVal[PROP_STORE_INFECTION_HISTORY] == null ? "" : ((Boolean) propVal[PROP_STORE_INFECTION_HISTORY]).toString();

                Run_Population_Remote_MetaPopulation_Pop_Intro_Syphilis runSyp = new Run_Population_Remote_MetaPopulation_Pop_Intro_Syphilis(rArg);

                runSyp.setStoreInfectionHistory(propVal[PROP_STORE_INFECTION_HISTORY] == null ? false : ((Boolean) propVal[PROP_STORE_INFECTION_HISTORY]));
                runSyp.setStoreTestingHistory(propVal[PROP_STORE_TESTING_HISTORY] == null ? false : ((Boolean) propVal[PROP_STORE_TESTING_HISTORY]));
                runSyp.setStoreTreatmentHistory(propVal[PROP_STORE_TREATMENT_HISTORY] == null ? false : ((Boolean) propVal[PROP_STORE_TREATMENT_HISTORY]));

                if (propVal[PROP_POP_SELECT_CSV] != null) {
                    runSyp.setPopSelectionCSV((String) propVal[PROP_POP_SELECT_CSV]);
                }

                if (propModelInitStr != null) {
                    for (int i = 0; i < propModelInitStr.length; i++) {
                        if (propModelInitStr[i] != null && !propModelInitStr[i].isEmpty()) {
                            if (i < runSyp.getRunParamValues().length) {
                                runSyp.getRunParamValues()[i] = Double.parseDouble(propModelInitStr[i]);
                            } else if (i - runSyp.getRunParamValues().length < runSyp.getThreadParamValStr().length) {
                                runSyp.getThreadParamValStr()[i - runSyp.getRunParamValues().length] = propModelInitStr[i];
                            } else {
                                runSyp.setPopParamValStr(i - runSyp.getRunParamValues().length - runSyp.getThreadParamValStr().length,
                                        propModelInitStr[i]);
                            }
                        }
                    }
                }

                runSyp.runSimulation();

                try {
                    Run_Population_Remote_MetaPopulation_Pop_Intro_Syphilis.decodeCollectionFile(baseDir);
                } catch (IOException | ClassNotFoundException ex) {
                    ex.printStackTrace(System.err);
                }
                break;
            case 2:
                // 0: Base Dir
                // 1: Import Dir
                // 2: Num thread
                // 3: Num sim
                // 4: Num to keep
                rArg = new String[5];
                rArg[0] = baseDir.getAbsolutePath();
                rArg[1] = propVal[PROP_POP_IMPORT_PATH] == null ? "" : (String) propVal[PROP_POP_IMPORT_PATH];
                rArg[2] = propVal[PROP_USE_PARALLEL] == null ? "" : ((Integer) propVal[PROP_USE_PARALLEL]).toString();
                rArg[3] = propVal[PROP_NUM_SIM_PER_SET] == null ? "" : ((Integer) propVal[PROP_NUM_SIM_PER_SET]).toString();
                rArg[4] = "0";

                OptRun_Population_Remote_MetaPopulation_Infection_Intro optRun
                        = new OptRun_Population_Remote_MetaPopulation_Infection_Intro(rArg);

                // Num step - in this case it is PROP_NUM_SNAP * PROP_SNAP_FREQ
                if (propVal[PROP_NUM_SNAP] != null && propVal[PROP_SNAP_FREQ] != null) {
                    optRun.setNumSteps(((Integer) propVal[PROP_NUM_SNAP]) * ((Integer) propVal[PROP_SNAP_FREQ]));
                }

                optRun.setPropModelInitStr(propModelInitStr);

                if (propVal[PROP_RMP_OPT_TARGET] != null) {
                    optRun.setTARGET_PREVAL((double[]) propVal[PROP_RMP_OPT_TARGET]);
                }
                if (propVal[PROP_RMP_OPT_WEIGHT] != null) {
                    optRun.setTARGET_WEIGHT((double[]) propVal[PROP_RMP_OPT_WEIGHT]);
                }

                try {
                    optRun.runOptimisation();
                } catch (IOException | ClassNotFoundException ex) {
                    ex.printStackTrace(System.err);
                }

                break;
            case 3:
                // 0: Num sim total
                // 1: Num burn in step
                // 2: Base Dir
                // 3: Num thread
                // 4: Popsize as string - propModelInitStr[0]
                // 5: PopType as string - propModelInitStr[1]
                // 6: Popconnc as string - propModelInitStr[2] 

                rArg = new String[7];
                rArg[0] = propVal[PROP_NUM_SIM_PER_SET] == null ? "" : ((Integer) propVal[PROP_NUM_SIM_PER_SET]).toString();
                rArg[1] = "";
                rArg[2] = baseDir.getAbsolutePath();
                rArg[3] = propVal[PROP_USE_PARALLEL] == null ? "" : ((Integer) propVal[PROP_USE_PARALLEL]).toString();
                rArg[4] = (propModelInitStr.length < 1 || propModelInitStr[0] == null) ? "" : propModelInitStr[0];
                rArg[5] = (propModelInitStr.length < 2 || propModelInitStr[1] == null) ? "" : propModelInitStr[1];
                rArg[6] = (propModelInitStr.length < 3 || propModelInitStr[2] == null) ? "" : propModelInitStr[2];
                try {
                    Run_Population_Remote_MetaPopulation_Pop_Generate.runPopGenerate(rArg);
                } catch (ClassNotFoundException ex) {
                    ex.printStackTrace(System.err);
                }

                break;

            case 4:
                // 0: Base Dir
                // 1: Import Dir
                // 2: Num thread
                // 3: Num sim
                // 4: Num to keep
                // 5: GA_Pop size
                rArg = new String[6];
                rArg[0] = baseDir.getAbsolutePath();
                rArg[1] = propVal[PROP_POP_IMPORT_PATH] == null ? "" : (String) propVal[PROP_POP_IMPORT_PATH];
                rArg[2] = propVal[PROP_USE_PARALLEL] == null ? "" : ((Integer) propVal[PROP_USE_PARALLEL]).toString();
                rArg[3] = propVal[PROP_NUM_SIM_PER_SET] == null ? "" : ((Integer) propVal[PROP_NUM_SIM_PER_SET]).toString();
                rArg[4] = "0";
                rArg[5] = "1000";

                OptRun_Population_Remote_MetaPopulation_Infection_Intro_GA optGA
                        = new OptRun_Population_Remote_MetaPopulation_Infection_Intro_GA(rArg);

                // Num step - in this case it is PROP_NUM_SNAP * PROP_SNAP_FREQ
                if (propVal[PROP_NUM_SNAP] != null && propVal[PROP_SNAP_FREQ] != null) {
                    optGA.setNumSteps(((Integer) propVal[PROP_NUM_SNAP]) * ((Integer) propVal[PROP_SNAP_FREQ]));
                }

                optGA.setPropModelInitStr(propModelInitStr);
                if (propVal[PROP_RMP_OPT_TARGET] != null) {
                    optGA.setTARGET_PREVAL((double[]) propVal[PROP_RMP_OPT_TARGET]);
                }
                if (propVal[PROP_RMP_OPT_WEIGHT] != null) {
                    optGA.setTARGET_WEIGHT((double[]) propVal[PROP_RMP_OPT_WEIGHT]);
                }

                try {
                    optGA.runOptimisation();
                } catch (IOException | ClassNotFoundException ex) {
                    ex.printStackTrace(System.err);
                }

                break;
            case 5: 
                try {

                Run_Population_Remote_MetaPopulation_COVID19 run
                        = new Run_Population_Remote_MetaPopulation_COVID19(baseDir, propVal, propModelInitStr);

                run.setRemoveAfterZip(!extraFlag.contains("-noZipRemove"));

                if (extraFlag.contains("-clearPrevResult")) {
                    if (baseDir.getName().equals("Covid19_Test_Default")) {
                        run.setClearPrevResult(true);
                    } else {
                        System.out.print("Clear previous result? Y to confirm: ");
                        java.io.BufferedReader in = new java.io.BufferedReader(new InputStreamReader(System.in));
                        if (in.readLine().equals("Y")) {
                            run.setClearPrevResult(true);
                        }
                    }

                }

                run.generateOneResultSet();

            } catch (IOException | InterruptedException ex) {
                ex.printStackTrace(System.err);
            }
            break;
            default:
                System.err.println("Error: Illegal arg[0]. Set 0 for NG/CT Run, "
                        + "1 for Syphilis, 2 for Optimisation, 3 for population file generation, "
                        + "4 for Optimisaiton using GA, 5 for COVID19");

        }
    }

    public static void main(String[] arg) throws IOException, InterruptedException, ClassNotFoundException {

        File resultsDir = new File(arg[0]);
        File[] singleSimDir;
        File propFile = new File(resultsDir, Simulation_Remote_MetaPopulation.FILENAME_PROP);

        if (!propFile.exists()) {
            System.out.println("Checking for result folder(s) at " + resultsDir);
            if (arg.length > 1) {
                ArrayList<File> dirList = new ArrayList<>();
                for (int i = 1; i < arg.length; i++) {
                    if (!arg[i].startsWith("-")) {

                        final Pattern regEx = Pattern.compile(arg[i]);

                        File[] matchedDir = resultsDir.listFiles(new FileFilter() {
                            @Override
                            public boolean accept(File file) {
                                return file.isDirectory() && regEx.matcher(file.getName()).matches()
                                        && new File(file, Simulation_Remote_MetaPopulation.FILENAME_PROP).exists();
                            }
                        });

                        dirList.addAll(Arrays.asList(matchedDir));

                        /*
                        
                        if (arg[i].endsWith("*")) {

                            final String startWith = arg[i];
                            File[] matchedDir = resultsDir.listFiles(new FileFilter() {
                                @Override
                                public boolean accept(File file) {
                                    return file.isDirectory() && file.getName().startsWith(startWith.substring(0, startWith.length() - 1))
                                            && new File(file, Simulation_Remote_MetaPopulation.FILENAME_PROP).exists();
                                }
                            });

                            dirList.addAll(Arrays.asList(matchedDir));

                        } else {
                            dirList.add(new File(resultsDir, arg[i]));
                        }
                         */
                    }
                }

                singleSimDir = dirList.toArray(new File[dirList.size()]);

            } else {
                singleSimDir = resultsDir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return file.isDirectory() && new File(file, Simulation_Remote_MetaPopulation.FILENAME_PROP).exists();
                    }
                });
            }
        } else {
            singleSimDir = new File[]{resultsDir};
        }
        System.out.println("# results set to be generated = " + singleSimDir.length);

        for (File singleSetDir : singleSimDir) {
            Simulation_Remote_MetaPopulation sim = new Simulation_Remote_MetaPopulation();
            Path propFilePath = new File(singleSetDir, Simulation_Remote_MetaPopulation.FILENAME_PROP).toPath();
            Properties prop;
            prop = new Properties();
            try (InputStream inStr = java.nio.file.Files.newInputStream(propFilePath)) {
                prop.loadFromXML(inStr);
            }
            sim.setBaseDir(singleSetDir);
            sim.loadProperties(prop);
            if (arg[arg.length - 1].startsWith("-")) {
                sim.setExtraFlag(arg[arg.length - 1]);
            }
            sim.generateOneResultSet();
            if (!arg[arg.length - 1].contains("-skipAnalysis")) {
                Run_Population_Remote_MetaPopulation_Pop_Analysis.popAnalysis(singleSetDir.getAbsolutePath());
            }

        }

    }

}
