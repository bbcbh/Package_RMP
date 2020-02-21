package opt;

import infection.AbstractInfection;
import infection.GeneralSEIRSInfection;
import static infection.GeneralSEIRSInfection.PARAM_INDEX_TRAN_FEMALE_MALE;
import java.io.File;
import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.GammaDistribution;
import population.Population_Remote_MetaPopulation;
import random.RandomGenerator;
import run.Thread_PopRun;

/**
 * @author Ben Hui
 * @version 20200217
 *
 * <pre> *
 *  20200717
 *      - Re-design callable function for general SEIRS infection
 *
 * </pre>
 */
public class Callable_Opt_Prevalence_General_SEIRS extends Abstract_Callable_Opt_Prevalence {

    public static final int OPT_PARAM_INDEX_TRAN_FEMALE_MALE = 0;
    public static final int OPT_PARAM_INDEX_TRAN_MALE_FEMALE = OPT_PARAM_INDEX_TRAN_FEMALE_MALE + 1;
    public static final int OPT_PARAM_INDEX_ASY_AVE_INF_DUR_FEMALE = OPT_PARAM_INDEX_TRAN_MALE_FEMALE + 1;
    public static final int OPT_PARAM_INDEX_ASY_AVE_INF_DUR_MALE = OPT_PARAM_INDEX_ASY_AVE_INF_DUR_FEMALE + 1;
    public static final int OPT_PARAM_INDEX_ASY_SD_INF_DUR_FEMALE = OPT_PARAM_INDEX_ASY_AVE_INF_DUR_MALE + 1;
    public static final int OPT_PARAM_INDEX_ASY_SD_INF_DUR_MALE = OPT_PARAM_INDEX_ASY_SD_INF_DUR_FEMALE + 1;
    public static final int OPT_PARAM_INDEX_SYM_AVE_INF_DUR_FEMALE = OPT_PARAM_INDEX_ASY_SD_INF_DUR_MALE + 1;
    public static final int OPT_PARAM_INDEX_SYM_AVE_INF_DUR_MALE = OPT_PARAM_INDEX_SYM_AVE_INF_DUR_FEMALE + 1;
    public static final int OPT_PARAM_INDEX_SYM_SD_INF_DUR_FEMALE = OPT_PARAM_INDEX_SYM_AVE_INF_DUR_MALE + 1;
    public static final int OPT_PARAM_INDEX_SYM_SD_INF_DUR_MALE = OPT_PARAM_INDEX_SYM_SD_INF_DUR_FEMALE + 1;
    public static final int OPT_PARAM_INDEX_UNIT_LENGTH = OPT_PARAM_INDEX_SYM_SD_INF_DUR_MALE + 1;

    public Callable_Opt_Prevalence_General_SEIRS(File optOutputDir, File popFile,
            int simId, int numStep, double[] param, String[] propModelInitStr) {
        this.popFile = popFile;
        this.optOutputDir = optOutputDir;
        this.simId = simId;
        this.numStep = numStep;
        this.param = param;
        this.propModelInitStr = propModelInitStr;
    }


    @Override
    public void loadParameters(Thread_PopRun thread, double[] param) {
        if (propModelInitStr != null) {
            initModelPropStr(param, thread);
        }

        RandomGenerator infectionRNG = ((Population_Remote_MetaPopulation) thread.getPop()).getInfectionRNG();

        int numInfection = param.length / OPT_PARAM_INDEX_UNIT_LENGTH;

        AbstractInfection[] inputInfection = new GeneralSEIRSInfection[numInfection];

        for (int i = 0; i < inputInfection.length; i++) {
            int offset = i * OPT_PARAM_INDEX_UNIT_LENGTH;
            GeneralSEIRSInfection infection;
            if (inputInfection.length == 1) {
                infection = new GeneralSEIRSInfection(infectionRNG);
            } else {
                infection = new GeneralSEIRSInfection(new random.MersenneTwisterRandomGenerator(infectionRNG.nextLong()));
            }

            AbstractRealDistribution[] distributions = GeneralSEIRSInfection.getDefaultDistribution(infectionRNG);
            infection.storeDistributions(distributions, GeneralSEIRSInfection.getDefaultDistributionState());

            String key;

            // Tranmission
            double[] trans = new double[2]; // M->F, F->M
            trans[0] = param[offset + OPT_PARAM_INDEX_TRAN_MALE_FEMALE];
            trans[1] = param[offset + OPT_PARAM_INDEX_TRAN_FEMALE_MALE];

            key = AbstractInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", "" + GeneralSEIRSInfection.PARAM_INDEX_TRAN_MALE_FEMALE);
            infection.setParameter(key, new double[]{trans[0], 0});

            key = AbstractInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", "" + GeneralSEIRSInfection.PARAM_INDEX_TRAN_FEMALE_MALE);
            infection.setParameter(key, new double[]{trans[1], 0});

            //Duration
            double[] dur;

            key = AbstractInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", "" + GeneralSEIRSInfection.PARAM_INDEX_ASY_INF_DUR_FEMALE);
            dur = (double[]) infection.getParameter(key);
            dur[0] = param[offset + OPT_PARAM_INDEX_ASY_AVE_INF_DUR_FEMALE];
            dur[1] = param[offset + OPT_PARAM_INDEX_ASY_SD_INF_DUR_FEMALE];
            infection.setParameter(key, dur);

            key = AbstractInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", "" + GeneralSEIRSInfection.PARAM_INDEX_SYM_INF_DUR_FEMALE);
            dur = (double[]) infection.getParameter(key);
            dur[0] = param[offset + OPT_PARAM_INDEX_SYM_AVE_INF_DUR_FEMALE];
            dur[1] = param[offset + OPT_PARAM_INDEX_SYM_SD_INF_DUR_FEMALE];
            infection.setParameter(key, dur);

            key = AbstractInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", "" + GeneralSEIRSInfection.PARAM_INDEX_ASY_INF_DUR_MALE);
            dur = (double[]) infection.getParameter(key);
            dur[0] = param[offset + OPT_PARAM_INDEX_ASY_AVE_INF_DUR_MALE];
            dur[1] = param[offset + OPT_PARAM_INDEX_ASY_SD_INF_DUR_MALE];
            infection.setParameter(key, dur);

            key = AbstractInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", "" + GeneralSEIRSInfection.PARAM_INDEX_SYM_INF_DUR_MALE);
            dur = (double[]) infection.getParameter(key);
            dur[0] = param[offset + OPT_PARAM_INDEX_SYM_AVE_INF_DUR_MALE];
            dur[1] = param[offset + OPT_PARAM_INDEX_SYM_SD_INF_DUR_MALE];
            infection.setParameter(key, dur);

            // Dummy value for now
            key = AbstractInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", "" + GeneralSEIRSInfection.PARAM_INDEX_EXPOSE_DUR_INDEX);
            dur = (double[]) infection.getParameter(key);
            dur[0] = 1;
            dur[1] = 0;
            infection.setParameter(key, dur);

            key = AbstractInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", "" + GeneralSEIRSInfection.PARAM_INDEX_IMMUNE_DUR_INDEX);
            dur = (double[]) infection.getParameter(key);
            dur[0] = 1;
            dur[1] = 0;
            infection.setParameter(key, dur);

            if (thread.getOutputPri() != null) {
                thread.getOutputPri().flush();
            }
            inputInfection[i] = infection;
        }

        thread.getInputParam()[Thread_PopRun.PARAM_INDEX_INFECTIONS] = inputInfection;

    }

}
