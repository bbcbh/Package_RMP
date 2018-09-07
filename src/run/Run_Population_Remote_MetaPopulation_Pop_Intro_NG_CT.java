package run;

import infection.AbstractInfection;
import infection.ChlamydiaInfection;
import infection.GonorrhoeaInfection;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static opt.Callable_Opt_Prevalence.OPT_PARAM_INDEX_AVE_INF_DUR_CT;
import static opt.Callable_Opt_Prevalence.OPT_PARAM_INDEX_AVE_INF_DUR_NG;
import static opt.Callable_Opt_Prevalence.OPT_PARAM_INDEX_TRAN_FEMALE_MALE_CT;
import static opt.Callable_Opt_Prevalence.OPT_PARAM_INDEX_TRAN_FEMALE_MALE_NG;
import static opt.Callable_Opt_Prevalence.OPT_PARAM_INDEX_TRAN_MALE_FEMALE_EXTRA_CT;
import static opt.Callable_Opt_Prevalence.OPT_PARAM_INDEX_TRAN_MALE_FEMALE_EXTRA_NG;
import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.BetaDistribution;
import population.Population_Remote_MetaPopulation;
import random.RandomGenerator;
import util.PersonClassifier;
import util.PropValUtils;

/**
 *
 * @author Ben Hui
 * @version 20180815
 *
 * History:
 *
 * <pre>
 * 20180523
 * - Added support for repeated simulation runs
 * 20180612
 * - Added support for user-defined input parameter
 * 20180614
 * - Minor change to output print format
 * 20180622
 * - Uniting input format for both NG/CT and syphilis
 * 20180713
 * - Introduce implementation of Abstract_Run_IntroInfection interface
 * 20180815
 * - Add support for Abstract_Run_IntroInfection class
 * 20180907
 * - Add support for indivudal infection, testing and treatment history
 * </pre>
 */
public class Run_Population_Remote_MetaPopulation_Pop_Intro_NG_CT extends Abstract_Run_IntroInfection {

    public String BASE_DIR_STR = "~/RMP/OptResults";
    public String IMPORT_DIR_STR = "~/RMP/ImportDir";
    public int NUM_THREADS = Runtime.getRuntime().availableProcessors();
    public int NUM_SIM_TOTAL = 1000;
    public int NUM_STEPS = 360 * 50;
    public int SAMP_FREQ = 90;

    double[] paramVal_Run = {
        /*
        // Best fit        
        // 0: OPT_PARAM_INDEX_TRAN_FEMALE_MALE_CT;
        0.3043159232572388,
        // 1: OPT_PARAM_INDEX_TRAN_MALE_FEMALE_EXTRA_CT
        0.2124334450502472,
        // 2: OPT_PARAM_INDEX_TRAN_FEMALE_MALE_NG
        0.3532267265325191,
        // 3: OPT_PARAM_INDEX_TRAN_MALE_FEMALE_EXTRA_NG 
        0.20895068447018414,
        // 4: OPT_PARAM_INDEX_AVE_INF_DUR_CT 
        377.83717702302926,
        // 5: OPT_PARAM_INDEX_AVE_INF_DUR_NG 
        367.98051191774533,        
         */
        //Better trans
        // 0: OPT_PARAM_INDEX_TRAN_FEMALE_MALE_CT;
        0.2543159232572388,
        // 1: OPT_PARAM_INDEX_TRAN_MALE_FEMALE_EXTRA_CT
        0.039826625618951,
        // 2: OPT_PARAM_INDEX_TRAN_FEMALE_MALE_NG
        0.34918126052590176,
        // 3: OPT_PARAM_INDEX_TRAN_MALE_FEMALE_EXTRA_NG 
        0.21336941210194102,
        // 4: OPT_PARAM_INDEX_AVE_INF_DUR_CT 
        434.04840078376355,
        // 5: OPT_PARAM_INDEX_AVE_INF_DUR_NG 
        362.3907737197366,
        // 6: PARAM_SD_CT - set to zero for fixed value
        0.02,
        // 7: PARAM_SD_NG - set to zero for fixed value
        0.02,};

    //protected String[] threadParamValStr = new String[Thread_PopRun.PARAM_TOTAL];
    // For Beta distribution, 
    // alpha = mean*(mean*(1-mean)/variance - 1)
    // beta = (1-mean)*(mean*(1-mean)/variance - 1)
    public Run_Population_Remote_MetaPopulation_Pop_Intro_NG_CT(String[] arg) {
        // 0: Base Dir
        if (arg.length > 0) {
            if (!arg[0].isEmpty()) {
                BASE_DIR_STR = arg[0];
            }
        }
        // 1: Import Dir
        if (arg.length > 1) {
            if (!arg[1].isEmpty()) {
                IMPORT_DIR_STR = arg[1];
            }
        }
        // 2: Num thread
        if (arg.length > 2) {
            if (!arg[2].isEmpty()) {
                NUM_THREADS = Integer.parseInt(arg[2]);
            }
        }

        // 3: Num sim
        if (arg.length > 3) {
            if (!arg[3].isEmpty()) {
                NUM_SIM_TOTAL = Integer.parseInt(arg[3]);
            }
        }
        // 4: Num step - in this case it is PROP_NUM_SNAP * PROP_SNAP_FREQ
        if (arg.length > 4) {
            if (!arg[4].isEmpty()) {
                NUM_STEPS = Integer.parseInt(arg[4]);
            }
        }

        // 5: Sample Freq
        if (arg.length > 5) {
            if (!arg[5].isEmpty()) {
                SAMP_FREQ = Integer.parseInt(arg[5]);
            }
        }

        System.out.println("BASE_DIR = " + BASE_DIR_STR);
        System.out.println("IMPORT_DIR = " + IMPORT_DIR_STR);
        System.out.println("NUM_THREADS = " + NUM_THREADS);
        System.out.println("NUM_SIM_TOTAL = " + NUM_SIM_TOTAL);
        System.out.println("NUM_STEPS = " + NUM_STEPS);
        System.out.println("SAMP_FREQ = " + SAMP_FREQ);
    }

    @Override
    public double[] getRunParamValues() {
        return paramVal_Run;
    }

    protected Object[] generateParam() {
        Object[] generatedDistribution = {
            // Better trans
            // 0: OPT_PARAM_INDEX_TRAN_FEMALE_MALE_CT; 
            paramVal_Run[6] == 0 ? paramVal_Run[0]
            : new BetaDistribution(paramVal_Run[0]
            * (paramVal_Run[0] * (1 - paramVal_Run[0]) / (paramVal_Run[6] * paramVal_Run[6]) - 1),
            (1 - paramVal_Run[0])
            * (paramVal_Run[0] * (1 - paramVal_Run[0]) / (paramVal_Run[6] * paramVal_Run[6]) - 1)),
            // 1: OPT_PARAM_INDEX_TRAN_MALE_FEMALE_EXTRA_CT
            paramVal_Run[1],
            // 2: OPT_PARAM_INDEX_TRAN_FEMALE_MALE_NG
            paramVal_Run[7] == 0 ? paramVal_Run[2]
            : new BetaDistribution(paramVal_Run[2]
            * (paramVal_Run[2] * (1 - paramVal_Run[2]) / (paramVal_Run[7] * paramVal_Run[7]) - 1),
            (1 - paramVal_Run[2])
            * (paramVal_Run[2] * (1 - paramVal_Run[2]) / (paramVal_Run[7] * paramVal_Run[7]) - 1)),
            // 3: OPT_PARAM_INDEX_TRAN_MALE_FEMALE_EXTRA_NG 
            paramVal_Run[3],
            // 4: OPT_PARAM_INDEX_AVE_INF_DUR_CT 
            paramVal_Run[4],
            // 5: OPT_PARAM_INDEX_AVE_INF_DUR_NG 
            paramVal_Run[5],};

        return generatedDistribution;

    }

    public void runSimulation() {

        File importDir, exportDir;
        File[] popFiles;

        importDir = new File(IMPORT_DIR_STR);
        exportDir = new File(BASE_DIR_STR);
        exportDir.mkdirs();

        popFiles = importDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(".zip");
            }
        });

        Arrays.sort(popFiles, new Comparator<File>() {
            @Override
            public int compare(File t, File t1) {
                return t.getName().compareTo(t1.getName());
            }
        });

        if (NUM_SIM_TOTAL > 0 && NUM_SIM_TOTAL < popFiles.length) {
            popFiles = Arrays.copyOf(popFiles, NUM_SIM_TOTAL);
        }

        System.out.println(popFiles.length + " population file(s) will be used in simulation");

        ExecutorService executor = null;
        int numInExe = 0;

        for (int sId = 0; sId < popFiles.length; sId++) {
            if (executor == null) {
                executor = Executors.newFixedThreadPool(NUM_THREADS);
                numInExe = 0;
            }
            File importPop = popFiles[sId];
            File outputPopFile = new File(exportDir, "Sim_" + importPop.getName());

            boolean skipPop = getPopSelection() != null && (Arrays.binarySearch(getPopSelection(), sId) < 0);

            if (outputPopFile.exists()) {
                System.out.println("Pop file " + outputPopFile.getAbsolutePath() + " already exist. Simulation skipped.");
            } else if (skipPop) {
                //System.out.println("Simulation for pop file " + outputPopFile.getAbsolutePath() + " skipped due to popSelection.");

            } else {

                PrintWriter outputPrint = null;
                try {
                    outputPrint = new PrintWriter(new FileWriter(new File(exportDir, "output_" + sId + ".txt")));
                } catch (IOException ex) {
                    ex.printStackTrace(System.err);
                    outputPrint = new PrintWriter(System.out);
                }

                // Generate thread
                Thread_PopRun thread = new Thread_PopRun(outputPopFile, importPop, sId, NUM_STEPS);
                thread.setOutputFreq(SAMP_FREQ);
                thread.setOutputPri(outputPrint, false);

                if (isStoreInfectionHistory()) {
                    thread.setIndiv_history(Thread_PopRun.INDIV_HIST_INFECTION, new HashMap<Integer, int[]>());
                }
                if (isStoreTestingHistory()) {
                    thread.setIndiv_history(Thread_PopRun.INDIV_HIST_TEST, new HashMap<Integer, int[]>());
                }
                if (isStoreTreatmentHistory()) {
                    thread.setIndiv_history(Thread_PopRun.INDIV_HIST_TREAT, new HashMap<Integer, int[]>());
                }

                try {
                    thread.importPop();
                } catch (IOException | ClassNotFoundException ex) {
                    ex.printStackTrace(System.err);
                }

                loadParameters(thread, generateParam());

                executor.submit(thread);
                numInExe++;
            }

            if (numInExe == NUM_THREADS) {
                try {
                    executor.shutdown();
                    if (!executor.awaitTermination(2, TimeUnit.DAYS)) {
                        System.err.println("Inf Thread time-out!");
                    }
                } catch (InterruptedException ex) {
                    ex.printStackTrace(System.err);
                }
                executor = null;
            }
        }
        if (executor != null) {
            try {
                executor.shutdown();
                if (!executor.awaitTermination(2, TimeUnit.DAYS)) {
                    System.err.println("Inf Thread time-out!");
                }
            } catch (InterruptedException ex) {
                ex.printStackTrace(System.err);
            }
            executor = null;
        }

    }

    public void loadParameters(Thread_PopRun thread, Object[] paramDist) {
        RandomGenerator infectionRNG = ((Population_Remote_MetaPopulation) thread.getPop()).getInfectionRNG();

        double[] param = new double[paramDist.length];

        for (int i = 0; i < param.length; i++) {

            if (paramDist[i] instanceof Number) {
                param[i] = ((Number) paramDist[i]).doubleValue();
            } else if (paramDist[i] instanceof AbstractRealDistribution) {
                ((AbstractRealDistribution) paramDist[i]).reseedRandomGenerator(infectionRNG.nextLong());
                param[i] = ((AbstractRealDistribution) paramDist[i]).sample();
            } else {
                System.err.println("paramDist #" + i
                        + " is of class " + paramDist[i].getClass().getName() + " and is not defined."
                        + " Using default value of " + paramVal_Run[i] + " instead.");

                param[i] = paramVal_Run[i];
            }

        }

        loadParameters(thread, param);

    }

    public void loadParameters(Thread_PopRun thread, double[] param) {
        ((Population_Remote_MetaPopulation) thread.getPop()).getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_NUMBER_PARTNER_LAST_12_MONTHS_DECOMP]
                = new float[][][]{
                    new float[][]{
                        // 16-19
                        new float[]{0.09f, 0.40f, 0.42f, 0.09f},
                        // 20-24
                        new float[]{0.07f, 0.47f, 0.38f, 0.08f},
                        // 25-29
                        new float[]{0.09f, 0.55f, 0.32f, 0.04f},
                        // 30-35 (descreasing based on linear trends for first 3 grp)
                        new float[]{0.09f, 0.62f, 0.27f, 0.02f},},};

        RandomGenerator infectionRNG = ((Population_Remote_MetaPopulation) thread.getPop()).getInfectionRNG();

        PrintWriter outputPrint = thread.getOutputPri();

        ChlamydiaInfection ct_inf = new ChlamydiaInfection(infectionRNG);
        GonorrhoeaInfection ng_inf = new GonorrhoeaInfection(infectionRNG);

        AbstractInfection[] inputInfection = new AbstractInfection[]{ct_inf, ng_inf};
        thread.getInputParam()[Thread_PopRun.PARAM_INDEX_INFECTIONS] = inputInfection;

        String key;

        // Tranmission
        // CT 
        double[] trans = new double[]{0.16, 0.12}; // M->F, F->M
        if (param.length > OPT_PARAM_INDEX_TRAN_MALE_FEMALE_EXTRA_CT) {
            trans[0] = param[OPT_PARAM_INDEX_TRAN_FEMALE_MALE_CT] + param[OPT_PARAM_INDEX_TRAN_MALE_FEMALE_EXTRA_CT];
            trans[1] = param[OPT_PARAM_INDEX_TRAN_FEMALE_MALE_CT];
        }

        key = ChlamydiaInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", "" + ChlamydiaInfection.DIST_TRANS_MF_INDEX);
        ct_inf.setParameter(key, new double[]{trans[0], 0});

        if (outputPrint != null) {
            outputPrint.println("Trans MF (CT) = " + Arrays.toString((double[]) ct_inf.getParameter(key)));
        }
        key = ChlamydiaInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", "" + ChlamydiaInfection.DIST_TRANS_FM_INDEX);
        ct_inf.setParameter(key, new double[]{trans[1], 0});
        if (outputPrint != null) {
            outputPrint.println("Trans FM (CT) = " + Arrays.toString((double[]) ct_inf.getParameter(key)));
        }

        // NG 
        trans = new double[]{0.4, 0.2}; // M->F, F->M
        if (param.length > OPT_PARAM_INDEX_TRAN_MALE_FEMALE_EXTRA_NG) {
            trans[0] = param[OPT_PARAM_INDEX_TRAN_FEMALE_MALE_NG] + param[OPT_PARAM_INDEX_TRAN_MALE_FEMALE_EXTRA_NG];
            trans[1] = param[OPT_PARAM_INDEX_TRAN_FEMALE_MALE_NG];
        }

        key = GonorrhoeaInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", "" + GonorrhoeaInfection.DIST_TRANS_MF_INDEX);
        ng_inf.setParameter(key, new double[]{trans[0], 0});

        if (outputPrint != null) {

            outputPrint.println("Trans MF (NG) = " + Arrays.toString((double[]) ng_inf.getParameter(key)));
        }
        key = GonorrhoeaInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", "" + GonorrhoeaInfection.DIST_TRANS_FM_INDEX);
        ng_inf.setParameter(key, new double[]{trans[1], 0});
        if (outputPrint != null) {
            outputPrint.println("Trans FM (NG) = " + Arrays.toString((double[]) ng_inf.getParameter(key)));
        }

        // Duration
        double[] dur;

        // CT
        key = ChlamydiaInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", "" + ChlamydiaInfection.DIST_INFECT_ASY_DUR_INDEX);
        dur = (double[]) ct_inf.getParameter(key);

        if (param.length > OPT_PARAM_INDEX_AVE_INF_DUR_CT) {
            dur[0] = param[OPT_PARAM_INDEX_AVE_INF_DUR_CT];
            dur[1] = 35;
        }

        ct_inf.setParameter(key, dur);

        if (outputPrint != null) {

            outputPrint.println("Duration Asy (CT) = " + Arrays.toString((double[]) ct_inf.getParameter(key)));
        }

        key = ChlamydiaInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", "" + ChlamydiaInfection.DIST_INFECT_SYM_DUR_INDEX);
        dur = (double[]) ct_inf.getParameter(key);

        if (param.length > OPT_PARAM_INDEX_AVE_INF_DUR_CT) {
            dur[0] = param[OPT_PARAM_INDEX_AVE_INF_DUR_CT];
            dur[1] = 35;
        }

        if (outputPrint != null) {

            outputPrint.println("Duration Sym (CT) = " + Arrays.toString((double[]) ct_inf.getParameter(key)));
        }

        // NG
        key = GonorrhoeaInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", "" + GonorrhoeaInfection.DIST_INFECT_DUR_INDEX);
        dur = (double[]) ng_inf.getParameter(key);

        if (param.length > OPT_PARAM_INDEX_AVE_INF_DUR_NG) {
            dur[0] = param[OPT_PARAM_INDEX_AVE_INF_DUR_NG];
        }

        ng_inf.setParameter(key, dur);
        if (outputPrint != null) {
            outputPrint.println("Duration Asy (NG) = " + Arrays.toString((double[]) ng_inf.getParameter(key)));
        }

        key = GonorrhoeaInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", "" + GonorrhoeaInfection.DIST_SYM_DUR_INDEX);
        dur = (double[]) ng_inf.getParameter(key);

        if (param.length > OPT_PARAM_INDEX_AVE_INF_DUR_NG) {
            dur[0] = param[OPT_PARAM_INDEX_AVE_INF_DUR_NG];
        }
        if (outputPrint != null) {
            outputPrint.println("Duration Sym (NG) = " + Arrays.toString((double[]) ng_inf.getParameter(key)));
        }

        for (int i = 0; i < threadParamValStr.length; i++) {
            if (threadParamValStr[i] != null && threadParamValStr[i].length() > 0) {
                thread.getInputParam()[i]
                        = PropValUtils.propStrToObject(threadParamValStr[i],
                                thread.getInputParam()[i].getClass());
                if (outputPrint != null) {
                    outputPrint.println("Thread ParamVal #" + i + " = " + threadParamValStr[i]);
                }

            }
        }

        for (int i = 0; i < popParamValStr.length; i++) {
            if (popParamValStr[i] != null) {
                Object orgVal = ((Population_Remote_MetaPopulation) thread.getPop()).getFields()[i];

                if (orgVal == null) {

                    switch (i) {
                        case Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_NEWPERSON_INFECTION_PREVAL:
                            orgVal = new float[0][];
                            break;
                        case Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_NEWPERSON_INFECTION_CLASSIFIER:
                            orgVal = new PersonClassifier[0];
                            break;
                        default:
                            System.err.println("Default class for Pop #" + i + " non defined");
                            break;
                    }

                }

                if (orgVal != null) {
                    ((Population_Remote_MetaPopulation) thread.getPop()).getFields()[i]
                            = PropValUtils.propStrToObject(popParamValStr[i],
                                    orgVal.getClass());

                    if (outputPrint != null) {
                        outputPrint.println("Pop Field #" + i + " = " + popParamValStr[i]);
                    }
                }
            }
        }

        if (outputPrint != null) {
            outputPrint.flush();
        }

    }

}
