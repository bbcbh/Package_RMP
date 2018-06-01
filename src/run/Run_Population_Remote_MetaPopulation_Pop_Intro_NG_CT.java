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

/**
 *
 * @author Ben Hui
 * @version 201580531
 *
 * History:
 *
 * <pre>
 * 20180523
 *  - Added support for repeated simulation runs
 * 20180531
 *  - Added support for user-defined input parameter
 * </pre>
 */
public class Run_Population_Remote_MetaPopulation_Pop_Intro_NG_CT {

    public String BASE_DIR_STR = "~/RMP/OptResults";
    public String IMPORT_DIR_STR = "~/RMP/ImportDir";
    public int NUM_THREADS = Runtime.getRuntime().availableProcessors();
    public int NUM_SIM_TOTAL = 1000;
    final int NUM_STEPS = 360 * 50;

    double[] paramVal = {
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
        362.3907737197366,};

    // For Beta distribution, 
    // alpha = mean*(mean*(1-mean)/variance - 1)
    // beta = (1-mean)*(mean*(1-mean)/variance - 1)
    final double SD_CT = 0.02;
    final double SD_NG = 0.02;    

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
    }

    public double[] getParamValues() {
        return paramVal;
    }        

    protected Object[] generateDistributionsFromParam() {
        Object[] generatedDistribution = {
            // Better trans
            // 0: OPT_PARAM_INDEX_TRAN_FEMALE_MALE_CT;        
            new BetaDistribution(paramVal[0]
            * (paramVal[0] * (1 - paramVal[0]) / (SD_CT * SD_CT) - 1),
            (1 - paramVal[0])
            * (paramVal[0] * (1 - paramVal[0]) / (SD_CT * SD_CT) - 1)),
            // 1: OPT_PARAM_INDEX_TRAN_MALE_FEMALE_EXTRA_CT
            0.039826625618951,
            // 2: OPT_PARAM_INDEX_TRAN_FEMALE_MALE_NG
            new BetaDistribution(paramVal[2]
            * (paramVal[2] * (1 - paramVal[2]) / (SD_NG * SD_NG) - 1),
            (1 - paramVal[2])
            * (paramVal[2] * (1 - paramVal[2]) / (SD_NG * SD_NG) - 1)),
            // 3: OPT_PARAM_INDEX_TRAN_MALE_FEMALE_EXTRA_NG 
            paramVal[3],
            // 4: OPT_PARAM_INDEX_AVE_INF_DUR_CT 
            paramVal[4],
            // 5: OPT_PARAM_INDEX_AVE_INF_DUR_NG 
            paramVal[5],};

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

            if (outputPopFile.exists()) {
                System.out.println("Pop file " + outputPopFile.getAbsolutePath() + " already exist. Simulation skipped.");

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
                thread.setOutputPri(outputPrint, false);

                try {
                    thread.importPop();
                } catch (IOException | ClassNotFoundException ex) {
                    ex.printStackTrace(System.err);
                }

                loadParameters(thread, generateDistributionsFromParam());

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
                        + " Using default value of " + paramVal[i] + " instead.");

                param[i] = paramVal[i];
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

        if (outputPrint != null) {
            outputPrint.flush();
        }

    }

}
