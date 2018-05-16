package opt;

import infection.AbstractInfection;
import infection.ChlamydiaInfection;
import infection.GonorrhoeaInfection;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.concurrent.Callable;
import person.AbstractIndividualInterface;
import population.Population_Remote_MetaPopulation;
import random.RandomGenerator;
import run.Thread_PopRun;
import util.Default_Remote_MetaPopulation_AgeGrp_Classifier;
import util.Default_Remote_MetaPopulation_Infection_Intro_Classifier;
import util.PersonClassifier;

public class Callable_Opt_Prevalence implements Callable<double[]> {

    private final File popFile;
    private final File optOutputDir;
    private final int simId;
    private final int numStep;
    private final double[] param;
    private boolean outputAsFile = true;

    public static final int OPT_PARAM_INDEX_TRAN_FEMALE_MALE_CT = 0;
    public static final int OPT_PARAM_INDEX_TRAN_MALE_FEMALE_EXTRA_CT = OPT_PARAM_INDEX_TRAN_FEMALE_MALE_CT + 1;
    public static final int OPT_PARAM_INDEX_TRAN_FEMALE_MALE_NG = OPT_PARAM_INDEX_TRAN_MALE_FEMALE_EXTRA_CT + 1;
    public static final int OPT_PARAM_INDEX_TRAN_MALE_FEMALE_EXTRA_NG = OPT_PARAM_INDEX_TRAN_FEMALE_MALE_NG + 1;
    public static final int OPT_PARAM_INDEX_AVE_INF_DUR_CT = OPT_PARAM_INDEX_TRAN_MALE_FEMALE_EXTRA_NG + 1;
    public static final int OPT_PARAM_INDEX_AVE_INF_DUR_NG = OPT_PARAM_INDEX_AVE_INF_DUR_CT + 1;

    public static final int OPT_PARAM_INDEX_INTRO_CT_MALE = OPT_PARAM_INDEX_AVE_INF_DUR_NG + 1;
    public static final int OPT_PARAM_INDEX_INTRO_CT_FEMALE = OPT_PARAM_INDEX_INTRO_CT_MALE + 1;
    public static final int OPT_PARAM_INDEX_INTRO_NG_MALE = OPT_PARAM_INDEX_INTRO_CT_FEMALE + 1;
    public static final int OPT_PARAM_INDEX_INTRO_NG_FEMALE = OPT_PARAM_INDEX_INTRO_NG_MALE + 1;

    private double[] target_preval = new double[]{
        0.118, 0.104, 0.074, 0.046, // CT, Male
        0.174, 0.082, 0.060, 0.035, // CT, Female
        0.137, 0.065, 0.040, 0.041, // NG, Male
        0.135, 0.076, 0.028, 0.043 // NG, Female              
    };

    public Callable_Opt_Prevalence(File optOutputDir, File popFile, int simId, int numStep, double[] param) {
        this.popFile = popFile;
        this.optOutputDir = optOutputDir;
        this.simId = simId;
        this.numStep = numStep;
        this.param = param;
    }

    public void setOutputAsFile(boolean outputAsFile) {
        this.outputAsFile = outputAsFile;
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
        if (param.length >= OPT_PARAM_INDEX_INTRO_NG_FEMALE) {
            float[][] introPreval = new float[][]{
                new float[]{(float) param[OPT_PARAM_INDEX_INTRO_CT_MALE], (float) param[OPT_PARAM_INDEX_INTRO_CT_FEMALE],},
                new float[]{(float) param[OPT_PARAM_INDEX_INTRO_NG_MALE], (float) param[OPT_PARAM_INDEX_INTRO_NG_FEMALE],},};

            PersonClassifier introClassifier = new Default_Remote_MetaPopulation_Infection_Intro_Classifier();

            ((Population_Remote_MetaPopulation) thread.getPop()).getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_NEWPERSON_INFECTION_CLASSIFIER]
                    = new PersonClassifier[]{introClassifier, introClassifier};

            ((Population_Remote_MetaPopulation) thread.getPop()).getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_NEWPERSON_INFECTION_PREVAL]
                    = introPreval;
            if (outputPrint != null) {
                outputPrint.println("Intro prevalence = " + Arrays.deepToString(introPreval));
            }
        }

        if (outputPrint != null) {
            outputPrint.flush();
        }

    }

    public void setTarget_preval(double[] target_preval) {
        this.target_preval = target_preval;
    }

    /**
     * Call function
     *
     * @return The difference between model generated and target prevalence
     * @throws Exception
     */
    @Override
    public double[] call() throws Exception {
        double[] res_single = new double[target_preval.length];
        File outputPopFile = null;
        
        if(optOutputDir != null){
            outputPopFile = new File(optOutputDir, "Opt_" + popFile.getName());
        }
        
        
        Thread_PopRun thread = new Thread_PopRun(outputPopFile, popFile, simId, numStep);
        PrintWriter outputPrint = null;

        if (outputAsFile && optOutputDir != null) {
            try {
                outputPrint = new PrintWriter(new FileWriter(new File(optOutputDir, "output_" + simId + ".txt")));
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
                outputPrint = new PrintWriter(System.out);
            }
        }

        thread.setOutputPri(outputPrint, false);

        // Set up parameter
        thread.importPop();
        loadParameters(thread, param);

        thread.run();

        PersonClassifier prevalClassifer = new PersonClassifier() {
            PersonClassifier ageClassifier = new Default_Remote_MetaPopulation_AgeGrp_Classifier();

            @Override
            public int classifyPerson(AbstractIndividualInterface p) {
                int aI = ageClassifier.classifyPerson(p);
                if (aI >= 0) {
                    return p.isMale() ? aI : (aI + ageClassifier.numClass());
                } else {
                    return -1;
                }
            }

            @Override
            public int numClass() {
                return ageClassifier.numClass() * 2;
            }

        };

        int[] numInGroup, numInfect;

        numInGroup = new int[prevalClassifer.numClass()];
        numInfect = new int[prevalClassifer.numClass() * 2];

        AbstractIndividualInterface[] allPerson = thread.getPop().getPop();

        for (AbstractIndividualInterface person : allPerson) {
            int cI = prevalClassifer.classifyPerson(person);
            numInGroup[cI]++;
            if (person.getInfectionStatus()[0] != AbstractIndividualInterface.INFECT_S) {
                numInfect[cI]++;
            }
            if (person.getInfectionStatus()[1] != AbstractIndividualInterface.INFECT_S) {
                numInfect[prevalClassifer.numClass() + cI]++;
            }
        }

        for (int i = 0; i < target_preval.length; i++) {
            res_single[i]
                    = ((double) numInfect[i]) / numInGroup[i % numInGroup.length] - target_preval[i];
        }

        if (outputPrint != null) {
            outputPrint.println("Residue (i.e. model preval - target preval) = " + Arrays.toString(res_single));
            outputPrint.close();
        }

        return res_single;

    }

}
