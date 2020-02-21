package opt;

import infection.AbstractInfection;
import infection.ChlamydiaInfection;
import infection.GonorrhoeaInfection;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import person.AbstractIndividualInterface;
import population.Population_Remote_MetaPopulation;
import random.RandomGenerator;
import run.Thread_PopRun;
import util.Default_Remote_MetaPopulation_AgeGrp_Classifier;
import util.PersonClassifier;

/**
 *
 * @author Ben Hui
 * @version 20180717
 *
 * <pre>
 * History:
 *   20180717
 *      - Rework parameter setting for optimisation of traveler behaviour and infection parameter only
 *      - Measure prevalence at remote only
 *   20191106
 *      - Add support for weighing of target prevalence
 *
 * </pre>
 */
public class Callable_Opt_Prevalence extends Abstract_Callable_Opt_Prevalence {

    public static final int OPT_PARAM_INDEX_TRAN_FEMALE_MALE_CT = 0;
    public static final int OPT_PARAM_INDEX_TRAN_MALE_FEMALE_EXTRA_CT = OPT_PARAM_INDEX_TRAN_FEMALE_MALE_CT + 1;
    public static final int OPT_PARAM_INDEX_TRAN_FEMALE_MALE_NG = OPT_PARAM_INDEX_TRAN_MALE_FEMALE_EXTRA_CT + 1;
    public static final int OPT_PARAM_INDEX_TRAN_MALE_FEMALE_EXTRA_NG = OPT_PARAM_INDEX_TRAN_FEMALE_MALE_NG + 1;
    public static final int OPT_PARAM_INDEX_AVE_INF_DUR_CT = OPT_PARAM_INDEX_TRAN_MALE_FEMALE_EXTRA_NG + 1;
    public static final int OPT_PARAM_INDEX_AVE_INF_DUR_NG = OPT_PARAM_INDEX_AVE_INF_DUR_CT + 1;

    // Optional     
    public static final int OPT_PRRAM_INDEX_SYM_SEEK = OPT_PARAM_INDEX_AVE_INF_DUR_NG + 1;

    public static final int OPT_PARAM_INDEX_TRAVERLER_BEHAVIOUR_16_19 = OPT_PRRAM_INDEX_SYM_SEEK + 1;
    public static final int OPT_PARAM_INDEX_TRAVERLER_BEHAVIOUR_20_24 = OPT_PARAM_INDEX_TRAVERLER_BEHAVIOUR_16_19 + 1;
    public static final int OPT_PARAM_INDEX_TRAVERLER_BEHAVIOUR_25_29 = OPT_PARAM_INDEX_TRAVERLER_BEHAVIOUR_20_24 + 1;
    public static final int OPT_PARAM_INDEX_TRAVERLER_BEHAVIOUR_30_35 = OPT_PARAM_INDEX_TRAVERLER_BEHAVIOUR_25_29 + 1;
    public static final int OPT_PARAM_TOTAL = OPT_PARAM_INDEX_TRAVERLER_BEHAVIOUR_30_35 + 1;
    // Addtional fields


    public Callable_Opt_Prevalence(File optOutputDir, File popFile, int simId, int numStep,
            double[] param, String[] propModelInitStr) {
        this.popFile = popFile;
        this.optOutputDir = optOutputDir;
        this.simId = simId;
        this.numStep = numStep;
        this.param = param;
        this.propModelInitStr = propModelInitStr;

        this.target_preval = new double[]{ 
            // From SH
            /*
            0.118, 0.104, 0.074, 0.046, // CT, Male
            0.174, 0.082, 0.060, 0.035, // CT, Female
            0.137, 0.065, 0.040, 0.041, // NG, Male
            0.135, 0.076, 0.028, 0.043, // NG, Female
             */
            // From Silver 2014
            0.205, 0.166, 0.103, 0.070, // CT, Male
            0.265, 0.202, 0.117, 0.076, // CT, Female
            0.216, 0.174, 0.116, 0.081, // NG, Male
            0.201, 0.154, 0.073, 0.070};
        
        this.target_weight = new double[]{
            1, 1, 1, 1, // CT, Male
            1, 1, 1, 1, // CT, Female
            1, 1, 1, 1, // NG, Male
            1, 1, 1, 1};
        
        this.pop_type_incl_for_residue = new int[3]; // Remote only

    }

    @Override
    public void loadParameters(Thread_PopRun thread, double[] param) {

        PrintWriter outputPrint = thread.getOutputPri();

        if (propModelInitStr != null) {
            initModelPropStr(param, thread);
        }

        if (param.length > OPT_PARAM_INDEX_TRAVERLER_BEHAVIOUR_16_19) {
            float[][][] org_behavior = (float[][][]) ((Population_Remote_MetaPopulation) thread.getPop()).getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_NUMBER_PARTNER_LAST_12_MONTHS_DECOMP];

            for (int i = 0; i < org_behavior.length; i++) {
                float[][] adjustBehaviour = Arrays.copyOf(org_behavior[i], org_behavior[i].length);

                for (int r = 0; r < adjustBehaviour.length; r++) {
                    adjustBehaviour[r][4] = (float) param[OPT_PARAM_INDEX_TRAVERLER_BEHAVIOUR_16_19 + r];
                }

                org_behavior[i] = Arrays.copyOf(adjustBehaviour, adjustBehaviour.length);
            }

            ((Population_Remote_MetaPopulation) thread.getPop()).getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_NUMBER_PARTNER_LAST_12_MONTHS_DECOMP]
                    = org_behavior;

            if (outputPrint != null) {
                outputPrint.println("Behaviour setting  = " + Arrays.deepToString((float[][][]) ((Population_Remote_MetaPopulation) thread.getPop()).getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_NUMBER_PARTNER_LAST_12_MONTHS_DECOMP]));
            }

        }

        RandomGenerator infectionRNG = ((Population_Remote_MetaPopulation) thread.getPop()).getInfectionRNG();

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

        if (param.length > OPT_PRRAM_INDEX_SYM_SEEK) {

            ((float[]) thread.getInputParam()[Thread_PopRun.PARAM_INDEX_SYMPTOM_TREAT_STAT])[0] = (float) param[OPT_PRRAM_INDEX_SYM_SEEK];

            if (outputPrint != null) {
                outputPrint.println("Sym treatment stat  = "
                        + Arrays.toString((float[]) thread.getInputParam()[Thread_PopRun.PARAM_INDEX_SYMPTOM_TREAT_STAT]));
            }
        }

        if (outputPrint != null) {
            outputPrint.flush();
        }

    }   

}
