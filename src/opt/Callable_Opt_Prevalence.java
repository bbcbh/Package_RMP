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
import util.PersonClassifier;
import util.PropValUtils;

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
 *
 * </pre>
 */
public class Callable_Opt_Prevalence implements Callable<double[]> {

    private final File popFile;
    private final File optOutputDir;
    private final int simId;
    private final int numStep;
    private final double[] param;
    private boolean outputAsFile = true;
    private final String[] propModelInitStr;

    public static final int OPT_PARAM_INDEX_TRAN_FEMALE_MALE_CT = 0;
    public static final int OPT_PARAM_INDEX_TRAN_MALE_FEMALE_EXTRA_CT = OPT_PARAM_INDEX_TRAN_FEMALE_MALE_CT + 1;
    public static final int OPT_PARAM_INDEX_TRAN_FEMALE_MALE_NG = OPT_PARAM_INDEX_TRAN_MALE_FEMALE_EXTRA_CT + 1;
    public static final int OPT_PARAM_INDEX_TRAN_MALE_FEMALE_EXTRA_NG = OPT_PARAM_INDEX_TRAN_FEMALE_MALE_NG + 1;
    public static final int OPT_PARAM_INDEX_AVE_INF_DUR_CT = OPT_PARAM_INDEX_TRAN_MALE_FEMALE_EXTRA_NG + 1;
    public static final int OPT_PARAM_INDEX_AVE_INF_DUR_NG = OPT_PARAM_INDEX_AVE_INF_DUR_CT + 1;

    public static final int OPT_PARAM_INDEX_TRAVERLER_BEHAVIOUR_16_19 = OPT_PARAM_INDEX_AVE_INF_DUR_NG + 1;
    public static final int OPT_PARAM_INDEX_TRAVERLER_BEHAVIOUR_20_24 = OPT_PARAM_INDEX_TRAVERLER_BEHAVIOUR_16_19 + 1;
    public static final int OPT_PARAM_INDEX_TRAVERLER_BEHAVIOUR_25_29 = OPT_PARAM_INDEX_TRAVERLER_BEHAVIOUR_20_24 + 1;
    public static final int OPT_PARAM_INDEX_TRAVERLER_BEHAVIOUR_30_35 = OPT_PARAM_INDEX_TRAVERLER_BEHAVIOUR_25_29 + 1;
    public static final int OPT_PARAM_TOTAL = OPT_PARAM_INDEX_TRAVERLER_BEHAVIOUR_30_35 + 1;

    private double[] target_preval = new double[]{
        0.118, 0.104, 0.074, 0.046, // CT, Male
        0.174, 0.082, 0.060, 0.035, // CT, Female
        0.137, 0.065, 0.040, 0.041, // NG, Male
        0.135, 0.076, 0.028, 0.043 // NG, Female              
    };

    public Callable_Opt_Prevalence(File optOutputDir, File popFile, int simId, int numStep, 
            double[] param, String[] propModelInitStr) {
        this.popFile = popFile;
        this.optOutputDir = optOutputDir;
        this.simId = simId;
        this.numStep = numStep;
        this.param = param;
        this.propModelInitStr = propModelInitStr;
    }

    public void setOutputAsFile(boolean outputAsFile) {
        this.outputAsFile = outputAsFile;
    }

    public void loadParameters(Thread_PopRun thread, double[] param) {

        PrintWriter outputPrint = thread.getOutputPri();
        
        if(propModelInitStr != null){
            
            
            for(int i = 0; i < propModelInitStr.length; i++){
                if(propModelInitStr[i]!= null && !propModelInitStr[i].isEmpty()){                    
                    if(i < OPT_PARAM_TOTAL){
                        param[i] = Float.parseFloat(propModelInitStr[i]);                                                
                    } else if (i - OPT_PARAM_TOTAL < Thread_PopRun.PARAM_TOTAL) {
                        thread.getInputParam()[i - OPT_PARAM_TOTAL] = 
                                PropValUtils.propStrToObject(propModelInitStr[i], thread.getInputParam()[i - OPT_PARAM_TOTAL].getClass());                                                                       
                    } else{                        
                        int popIndex = i - OPT_PARAM_TOTAL - Thread_PopRun.PARAM_TOTAL;
                        thread.updatePopFieldFromString(popIndex, propModelInitStr[i]);                                                                        
                        
                    }
                    
                    
                    if(outputPrint != null){
                        outputPrint.println("Model init. setting #" + i + " = " + propModelInitStr[i]);
                    }
                    
                    
                }                                
            }            
        }
        
        float[][] defaultBehaviour = 
                ((float[][][]) ((Population_Remote_MetaPopulation) thread.getPop()).getFields()
                [Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_NUMBER_PARTNER_LAST_12_MONTHS_DECOMP])[0];

        ((Population_Remote_MetaPopulation) thread.getPop()).getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_NUMBER_PARTNER_LAST_12_MONTHS_DECOMP]
                = new float[][][]{defaultBehaviour};

        if (param.length > OPT_PARAM_INDEX_TRAVERLER_BEHAVIOUR_16_19) {
            float[][] adjustBehaviour = new float[defaultBehaviour.length][];
            for(int r = 0; r < adjustBehaviour.length; r++){
                adjustBehaviour[r] = Arrays.copyOf(defaultBehaviour[r], 5);
                adjustBehaviour[r][4] = (float) param[OPT_PARAM_INDEX_TRAVERLER_BEHAVIOUR_16_19 + r];                
            }                                                                       

            ((Population_Remote_MetaPopulation) thread.getPop()).getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_NUMBER_PARTNER_LAST_12_MONTHS_DECOMP]
                    = new float[][][]{defaultBehaviour, adjustBehaviour, adjustBehaviour, adjustBehaviour, adjustBehaviour};

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

        if (optOutputDir != null) {
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
            
            // Remote only 
            if (((Population_Remote_MetaPopulation) thread.getPop()).getCurrentLocation(person) != 0) {
                int cI = prevalClassifer.classifyPerson(person);
                numInGroup[cI]++;
                if (person.getInfectionStatus()[0] != AbstractIndividualInterface.INFECT_S) {
                    numInfect[cI]++;
                }
                if (person.getInfectionStatus()[1] != AbstractIndividualInterface.INFECT_S) {
                    numInfect[prevalClassifer.numClass() + cI]++;
                }
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
