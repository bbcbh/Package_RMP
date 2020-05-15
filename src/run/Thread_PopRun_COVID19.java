package run;

import infection.AbstractInfection;
import infection.COVID19_Remote_Infection;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import person.AbstractIndividualInterface;
import person.Person_Remote_MetaPopulation;
import population.AbstractFieldsArrayPopulation;
import population.Population_Remote_MetaPopulation;
import population.Population_Remote_MetaPopulation_COVID19;
import util.Factory_FullAgeUtil;
import util.PersonClassifier;

/**
 *
 * @author Ben Hui
 */
class Thread_PopRun_COVID19 implements Runnable {

    final int threadId;
    final int numSnap;
    final int snapFreq;
    final File baseDir;

    final PersonClassifier full_AgeGrp_PersonClassifier;

    final boolean printDebug;

    PrintWriter pri;

    Population_Remote_MetaPopulation_COVID19 pop;
    AbstractInfection[] infList;

    public static final int THREAD_PARAM_HOSUEHOLD_SIZE_DIST = 0;
    public static final int THREAD_PARAM_HOSUEHOLD_SPREAD_DIST = THREAD_PARAM_HOSUEHOLD_SIZE_DIST + 1;
    public static final int THREAD_PARAM_NON_HOUSEHOLD_CONTACT_DIST = THREAD_PARAM_HOSUEHOLD_SPREAD_DIST + 1;
    public static final int THREAD_PARAM_TRIGGERS = THREAD_PARAM_NON_HOUSEHOLD_CONTACT_DIST + 1;
    public static final int THREAD_PARAM_TRIGGERED_TEST_RATE = THREAD_PARAM_TRIGGERS + 1;
    public static final int THREAD_PARAM_TRIGGERED_TEST_RESPONSE = THREAD_PARAM_TRIGGERED_TEST_RATE + 1;
    public static final int THREAD_PARAM_TRIGGERED_SYM_RESPONSE = THREAD_PARAM_TRIGGERED_TEST_RESPONSE + 1;
    public static final int THREAD_PARAM_TRIGGERED_HOUSEHOLD_TESTING = THREAD_PARAM_TRIGGERED_SYM_RESPONSE + 1;
    public static final int THREAD_PARAM_TRIGGERED_HOUSEHOLD_QUARANTINE = THREAD_PARAM_TRIGGERED_HOUSEHOLD_TESTING + 1;
    public static final int THREAD_PARAM_TRIGGERED_METAPOP_LOCKDOWN_SETTING = THREAD_PARAM_TRIGGERED_HOUSEHOLD_QUARANTINE + 1;
    public static final int THREAD_PARAM_TRIGGERED_TESTING_RESULT_DELAY = THREAD_PARAM_TRIGGERED_METAPOP_LOCKDOWN_SETTING + 1;
    public static final int THREAD_PARAM_TEST_SENSITIVITY = THREAD_PARAM_TRIGGERED_TESTING_RESULT_DELAY + 1;
    public static final int THREAD_PARAM_MAX_TEST_BY_LOC = THREAD_PARAM_TEST_SENSITIVITY + 1;
    public static final int THREAD_PARAM_TRIGGERED_CONTACT_TRACE_DELAY = THREAD_PARAM_MAX_TEST_BY_LOC + 1;
    public static final int THREAD_PARAM_TRIGGERED_QUARANTINE_DELAY = THREAD_PARAM_TRIGGERED_CONTACT_TRACE_DELAY + 1;
    public static final int THREAD_PARAM_TRIGGRRED_QUARANTINE_DURATION = THREAD_PARAM_TRIGGERED_QUARANTINE_DELAY + 1;
    public static final int THREAD_PARAM_TRIGGRRED_QUARANTINE_EFFECT = THREAD_PARAM_TRIGGRRED_QUARANTINE_DURATION + 1;
    public static final int THREAD_PARAM_TRIGGRRED_CONTACT_HISTORY_INTERVENTIONS = THREAD_PARAM_TRIGGRRED_QUARANTINE_EFFECT + 1;

    public static final int TEST_RES_STAT_ALL = 0;
    public static final int TEST_RES_STAT_POS = 1;
    public static final int TEST_RES_STAT_LENGTH = 2;

    public static final int LOCKDOWN_INTER_METAPOP = 0;
    public static final int LOCKDOWN_WITHIN_METAPOP = LOCKDOWN_INTER_METAPOP + 1;
    public static final int LOCKDOWN_INTER_METAPOP_DURATION = LOCKDOWN_WITHIN_METAPOP + 1;
    public static final int LOCKDOWN_WITHIN_METAPOP_DURATION = LOCKDOWN_INTER_METAPOP_DURATION + 1;

    public static final int SENSITIVITY_INFECTED = 0;
    public static final int SENSITIVITY_INFECTIOUS = SENSITIVITY_INFECTED + 1;
    public static final int SENSITIVITY_SYMPTOMATIC = SENSITIVITY_INFECTIOUS + 1;
    public static final int SENSITIVITY_LATANT = SENSITIVITY_SYMPTOMATIC + 1;
    public static final int SENSITIVITY_INCUBRATION = SENSITIVITY_LATANT + 1;
    public static final int SENSITIVITY_POST_INFECTIOUS = SENSITIVITY_INCUBRATION + 1;

    public static final int DELAY_OPTIONS_MIN = 0;
    public static final int DELAY_OPTIONS_MAX = DELAY_OPTIONS_MIN + 1;

    public static final int TEST_TYPE_CONTACT_BASE = 0;
    public static final int TEST_TYPE_SYM = TEST_TYPE_CONTACT_BASE + 1;
    public static final int TEST_TYPE_SCR = TEST_TYPE_SYM + 1;
    public static final int TEST_TYPE_EXIT_CI = TEST_TYPE_SCR + 1;
    public static final int TEST_TYPE_EXIT_QUARANTINE = TEST_TYPE_EXIT_CI + 1;
    // =  -(n-th level of contact test) if TEST_TYPE < 0

    public static final int MAX_TEST_NUM = 0;
    public static final int MAX_TEST_PERIOD = MAX_TEST_NUM + 1;
    public static final int MAX_TEST_QUEUE_SETTING = MAX_TEST_PERIOD + 1;

    public static final int MAX_TEST_QUEUE_TYPE_NONE = -1; // Default

    public static final int QUARANTINE_EFFECT_HOUSEHOLD_CONTACT_ADJ = 0;
    public static final int QUARANTINE_EFFECT_NON_HOUSEHOLD_CONTACT_ADJ = QUARANTINE_EFFECT_HOUSEHOLD_CONTACT_ADJ + 1;
    public static final int QUARANTINE_EFFECT_DAYS_TO_EXIT_TEST_CI = QUARANTINE_EFFECT_NON_HOUSEHOLD_CONTACT_ADJ + 1;
    public static final int QUARANTINE_EFFECT_DAYS_TO_EXIT_TEST_QUARANTINE = QUARANTINE_EFFECT_DAYS_TO_EXIT_TEST_CI + 1;

    public static final int CONTACT_HIST_HOUSEHOLD_TEST_PROB = 0;
    public static final int CONTACT_HIST_HOUSEHOLD_TEST_WINDOW = CONTACT_HIST_HOUSEHOLD_TEST_PROB + 1;
    public static final int CONTACT_HIST_NON_HOUSEHOLD_TEST_PROB = CONTACT_HIST_HOUSEHOLD_TEST_WINDOW + 1;
    public static final int CONTACT_HIST_NON_HOUSEHOLD_TEST_WINDOW = CONTACT_HIST_NON_HOUSEHOLD_TEST_PROB + 1;
    public static final int CONTACT_HIST_HOUSEHOLD_QUARANTINE_PROB = CONTACT_HIST_NON_HOUSEHOLD_TEST_WINDOW + 1;
    public static final int CONTACT_HIST_HOUSEHOLD_QUARANTINE_WINDOW = CONTACT_HIST_HOUSEHOLD_QUARANTINE_PROB + 1;
    public static final int CONTACT_HIST_NON_HOUSEHOLD_QUARANTINE_PROB = CONTACT_HIST_HOUSEHOLD_QUARANTINE_WINDOW + 1;
    public static final int CONTACT_HIST_NON_HOUSEHOLD_QUARANTINE_WINDOW = CONTACT_HIST_NON_HOUSEHOLD_QUARANTINE_PROB + 1;

    Object[] threadParam = new Object[]{
        // THREAD_PARAM_HOSUEHOLD_SIZE_DIST        
        // Format: [loc]{cumul_proproption_of_pop_1,mean_household_size_1 , cumul_percent_of_pop_2 ...}
        new float[][]{},
        // THREAD_PARAM_HOSUEHOLD_SPREAD_DIST
        // Format: [loc]{probabily at household_id_1,  probabily at household_id_2 ...}
        new float[][]{},
        // THREAD_PARAM_NON_HOUSEHOLD_CONTACT_DIST
        // Format:[loc][cumul_proproption_of_contact_1, per_day_contact_rate_1 ...]
        new float[][]{},
        // THREAD_PARAM_TEST_TRIGGER
        // Format: [loc]{response_trigger...} , where:
        //  response_trigger <= 0 : Trigger at globalTime = - response_trigger
        //  response_trigger >= 1 : Trigger when cumulative number of cases on the location  >= response_trigger 
        //  response_trigger >0 and < 1 : Trigger when cumulative number of cases from all locations  >= 1/response_trigger 
        new float[][]{},
        // THREAD_PARAM_TRIGGERED_TEST_RATE
        // Format: [loc][triggerIndex][test_rate_at_location]          
        new float[][][]{},
        // THREAD_PARAM_TRIGGERED_TEST_RESPONSE
        // Format: [loc][triggerIndex]{ {valid_for_days}, {probability_for_k=0}, {probability_for_k=1} ...}
        // k: 0 = adjustment to non-household contact, 1 = adjustment to household contact, 2 = adjustment to movement
        // valid_for: how long the response valid for
        // probability: {cumul_prob_1, response_1, cumul_prob_2, ...}
        new double[][][][]{},
        // THREAD_PARAM_TRIGGERED_SYM_RESPONSE
        // Format: [loc][triggerIndex][k]{probability}
        // k: 0 = adjustment to non-household contact, 1 = adjustment to household contact, 
        //    2 = adjustment to movement, 3 = seek test rate (per day)
        // probability: {cumul_prob_1, response_1, cumul_prob_2, ...}
        new double[][][][]{},
        // THREAD_PARAM_TRIGGERED_HOUSEHOLD_TESTING
        // Format: [loc][triggerIndex]{
        //               probability_current_household, 
        //               probability_core_household, 
        //               probility_non_core_household, prob_temp}       
        new double[][][]{},
        // THREAD_PARAM_TRIGGERED_HOUSEHOLD_QUARANTINE
        // Format: [loc][triggerIndex]
        // {probability_current_household, probability_core_household, probility_non_core_household, prob_temp, 
        //  stay_quarantine_until_current_household, stay_quarantine_until_core_household, 
        //  stay_quarantine_until_non_core_household, stay_quarantine_until_temp_household } 
        new double[][][]{},
        // THREAD_PARAM_TRIGGERED_METAPOP_LOCKDOWN_SETTING
        // Format:[loc][triggerIndex]{LOCKDOWN_INTER_METAPOP ,LOCKDOWN_WITHIN_METAPOP ,
        //                            LOCKDOWN_INTER_METAPOP_DURATION ,LOCKDOWN_WITHIN_METAPOP_DURATION)}        
        new float[][][]{},
        // THREAD_PARAM_TRIGGERED_TESTING_RESULT_DELAY
        // Format: [loc][triggerIndex]{delay_in_days}
        // Format: [loc][triggerIndex]{delay_in days_min, delay_in_days_max}
        new float[][][]{},
        // THREAD_PARAM_TEST_SENSITIVITY
        // Format: {sensitivity_all stage}
        // Format: {sensitivity_infected, sensitivity_infectious, sensitivity_symptomatic, 
        //          sensitivity_latant, sensitivity_incubation, sensitivity_post_infectious}
        new double[]{},
        // THREAD_PARAM_MAX_TEST_BY_LOC
        // Format: [loc][max_num_test, period]
        // Format: [loc][max_num_test, period, queue_method]
        new int[][]{},
        // THREAD_PARAM_TRIGGERED_CONTACT_TRACE_DELAY
        // Format: [loc][triggerIndex]{delay_in_days}
        // Format: [loc][triggerIndex]{delay_in days_min, delay_in_days_max}
        new float[][][]{},
        // THREAD_PARAM_TRIGGERED_QUARANINE_DELAY
        // Format: [loc][triggerIndex]{delay_in_days}
        // Format: [loc][triggerIndex]{delay_in days_min, delay_in_days_max}
        new float[][][]{},
        // THREAD_PARAM_TRIGGRRED_QUARANTINE_DURATION
        // Format: [loc][triggerIndex]{duration_in_day}
        // Format: [loc][triggerIndex]{duration_in_day_1, cumul_prob_duration_in_day_1, ...}
        new float[][][]{},
        // THREAD_PARAM_TRIGGRRED_QUARANTINE_EFFECT
        // Format: [loc][triggerIndex]{probababiliy_of_household_contact, probababiliy_of_non_household_contact}
        // Format: [loc][triggerIndex]{probababiliy_of_household_contact, probababiliy_of_non_household_contact, 
        //                             days_to_exit_test_resp_case_isolate, days_to_exit_test_resp_quarantine}
        new float[][][]{},
        // THREAD_PARAM_TRIGGRRED_CONTACT_HISTORY_INTERVENTIONS
        // Format: [loc][triggerIndex]{
        //         probabitly_of_household_contacts_test, days_to_trace_household, 
        //         probabitly_of_non_household_contacts_test, days_to_trace_non_household,
        //         probabitly_of_household_contacts_quarantine, days_to_trace_household, 
        //         probabitly_of_non_household_contacts_quarantine, days_to_trace_non_household,
        new float[][][]{},};

    public static final String FILE_REGEX_OUTPUT = "output_%d.txt";
    public static final String FILE_REGEX_TEST_STAT = "testStat_%d.csv";
    public static final String FILE_REGEX_SNAP_STAT = "snapStat_%d.csv";
    public static final String FILE_REGEX_POP_SNAP = null; //"popSnap_%d_%d.csv";

    public Thread_PopRun_COVID19(int threadId, File baseDir, int numSnap, int snapFreq) throws FileNotFoundException {
        this(threadId, baseDir, numSnap, snapFreq, false);
    }

    public Thread_PopRun_COVID19(int threadId, File baseDir, int numSnap, int snapFreq, boolean printDebug) throws FileNotFoundException {
        this.threadId = threadId;
        this.baseDir = baseDir;
        this.numSnap = numSnap;
        this.snapFreq = snapFreq;
        this.printDebug = printDebug;
        this.full_AgeGrp_PersonClassifier = Factory_FullAgeUtil.genFullAgeClassifier();

    }

    public Object[] getThreadParam() {
        return threadParam;
    }

    public Population_Remote_MetaPopulation_COVID19 getPop() {
        return pop;
    }

    public void setPop(Population_Remote_MetaPopulation_COVID19 pop) {
        this.pop = pop;
    }

    public void setInfList(AbstractInfection[] infList) {
        this.infList = infList;
    }

    public int getThreadId() {
        return threadId;
    }

    @Override
    public void run() {
        try {
            this.pri = new PrintWriter(new File(baseDir, String.format(FILE_REGEX_OUTPUT, threadId)));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace(System.err);
            this.pri = new PrintWriter(System.out);
        }

        pri.println("Generating population with seed of " + pop.getSeed());
        int[] popSize = (int[]) pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_POP_SIZE];
        pri.println("Pop Size = " + Arrays.toString(popSize));
        int[] popType = (int[]) pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_POP_TYPE];
        pri.println("Pop type = " + Arrays.toString(popType));
        int[][] popConnc = (int[][]) pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_POP_CONNC];
        pri.println("Pop connc = " + Arrays.deepToString(popConnc));

        pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_AGE_GRP_CLASSIFIER] = full_AgeGrp_PersonClassifier;

        int[][] decomp = (int[][]) pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_AGE_GENDER_COMPOSITION];
        float[][] awayFromHome = (float[][]) pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_AWAY_FROM_HOME_BY_LOCATION];

        if (decomp == null || decomp.length == 0) {
            decomp = new int[popSize.length][];
            for (int p = 0; p < popSize.length; p++) {
                decomp[p] = popType[p] == 2
                        ? util.Factory_Population_Decompositiion_Full.getDecomposition(
                                util.Factory_Population_Decompositiion_Full.ALL_POP_COMPOSITION_REGIONAL_2019, popSize[p], pop.getRNG())
                        : util.Factory_Population_Decompositiion_Full.getDecomposition(
                                util.Factory_Population_Decompositiion_Full.ALL_POP_COMPOSITION_REMOTE_2019, popSize[p], pop.getRNG());
            }
            pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_AGE_GENDER_COMPOSITION] = decomp;
        }

        if (awayFromHome == null || awayFromHome.length == 0) {
            awayFromHome = new float[popSize.length][];
            for (int p = 0; p < popSize.length; p++) {
                awayFromHome[p] = Factory_FullAgeUtil.getProportionPopAwayFull(popType[p]);
            }
            pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_AWAY_FROM_HOME_BY_LOCATION] = awayFromHome;
        }

        pop.initialise();
        pop.allolocateCoreHosuehold((float[][]) getThreadParam()[THREAD_PARAM_HOSUEHOLD_SIZE_DIST],
                (float[][]) getThreadParam()[THREAD_PARAM_HOSUEHOLD_SPREAD_DIST],
                (float[][]) getThreadParam()[THREAD_PARAM_NON_HOUSEHOLD_CONTACT_DIST]);

        // Household stat (core)
        pri.println("==================");
        pri.println("Household Stat (Core)");
        pop.printHousholdStat(pri);

        // Household stat (total)
        pop.allocateNonCoreHousehold();
        pri.println("==================");
        pri.println("Household Stat (All)");
        pop.printHousholdStat(pri);

        // Intitialise infection and patient zero
        pop.setInfectionList(infList);
        pop.setAvailability(null);
        COVID19_Remote_Infection covid19 = (COVID19_Remote_Infection) pop.getInfList()[0];

        String key;
        key = COVID19_Remote_Infection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", "" + COVID19_Remote_Infection.DIST_RO_RAW_INDEX);
        double[] r0 = (double[]) covid19.getParameter(key);
        pri.println("==================");
        pri.println("\"R0\" = " + Arrays.toString(r0));

        AbstractIndividualInterface[] patientZeroByLoc = new AbstractIndividualInterface[popSize.length];
        int[] pop_remaining = Arrays.copyOf(popSize, popSize.length);

        // Setting patient zero
        for (AbstractIndividualInterface p : pop.getPop()) {
            Person_Remote_MetaPopulation patientZero = (Person_Remote_MetaPopulation) p;

            if (patientZeroByLoc[patientZero.getHomeLocation()] == null
                    && pop_remaining[patientZero.getHomeLocation()] > 0) {
                boolean sel;
                sel = pop.getRNG().nextInt(pop_remaining[patientZero.getHomeLocation()]) < 1;
                if (sel) {
                    patientZeroByLoc[patientZero.getHomeLocation()] = patientZero;
                    covid19.infecting(patientZero);
                }
                pop_remaining[patientZero.getHomeLocation()]--;
            }

        }

        pri.println("==================");

        for (int loc = 0; loc < patientZeroByLoc.length; loc++) {
            pri.println(String.format("Patient zero at Loc #%d: ", loc));
            pri.println("Id = " + patientZeroByLoc[loc].getId());
            pop.printPatientStat(patientZeroByLoc[loc], pri);

            pri.println();

        }

        boolean priClose = true;

        PrintWriter outputCSV, testingCSV, popStatCSV;
        try {
            outputCSV = new PrintWriter(new File(baseDir, String.format(FILE_REGEX_SNAP_STAT, this.threadId)));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace(System.err);
            outputCSV = pri;
            priClose = false;
        }
        try {
            testingCSV = new PrintWriter(new File(baseDir, String.format(FILE_REGEX_TEST_STAT, this.threadId)));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace(System.err);
            testingCSV = pri;
            priClose = false;
        }

        if (priClose) {
            pri.close();
        }

        // Testing and response 
        int[] testTriggerIndex_by_loc = new int[popSize.length];

        int[][] testing_res_stat_cumul = new int[TEST_RES_STAT_LENGTH][popSize.length]; // By location
        int[] testing_res_stat_cumul_all = new int[TEST_RES_STAT_LENGTH];

        float[][] triggers = (float[][]) getThreadParam()[THREAD_PARAM_TRIGGERS];
        float[][][] triggeredTestRate = (float[][][]) getThreadParam()[THREAD_PARAM_TRIGGERED_TEST_RATE];
        double[][][][] triggeredSymResponse = (double[][][][]) getThreadParam()[THREAD_PARAM_TRIGGERED_SYM_RESPONSE];

        int[][] triggers_at_time = new int[triggers.length][triggers[0].length];

        StringBuilder triggerText = new StringBuilder();

        pop.printCSVOutputHeader(outputCSV);
        int[][] numStat = pop.generateInfectionStat();
        pop.printCSVOutputEntry(outputCSV, numStat);

        // Print testingCSV header 
        testingCSV.print("Time");
        for (int p = 0; p < TEST_RES_STAT_LENGTH; p++) {
            for (int i = 0; i < popSize.length; i++) {
                testingCSV.print(',');
                testingCSV.print("Loc_");
                testingCSV.print(i);
            }
        }

        // Response in queue
        for (int i = 0; i < popSize.length; i++) {
            testingCSV.print(',');
            testingCSV.print("Resp queue Loc_");
            testingCSV.print(i);
        }

        testingCSV.println();

        // Resposne queue 
        int[][] rolling_test_count_by_loc = null;
        int[] rollingSum = new int[popSize.length];

        List<LinkedList<Object[]>> responseQueueList = new ArrayList<>();
        for (int i = 0; i < popSize.length; i++) {
            responseQueueList.add(new LinkedList<>());
        }

        if (getThreadParam()[THREAD_PARAM_MAX_TEST_BY_LOC] != null) {
            int[][] maxTestByLoc = (int[][]) getThreadParam()[THREAD_PARAM_MAX_TEST_BY_LOC];
            if (maxTestByLoc.length > 0) {
                rolling_test_count_by_loc = new int[popSize.length][];
                for (int loc = 0; loc < popSize.length; loc++) {
                    rolling_test_count_by_loc[loc] = new int[maxTestByLoc[loc][MAX_TEST_PERIOD]];
                }
            }
        }

        printCSVTestEntry(testingCSV, testing_res_stat_cumul, responseQueueList);

        ArrayList<Integer> testResultInWaiting = new ArrayList<>(); // id

        for (int sn = 0; sn < numSnap; sn++) {
            for (int sf = 0; sf < snapFreq; sf++) {

                int[] test_res_stat_today = new int[2];

                for (AbstractIndividualInterface p : pop.getPop()) {
                    Person_Remote_MetaPopulation rmp = (Person_Remote_MetaPopulation) p;
                    int trigger_loc = pop.getCurrentLocation(p);

                    int testTriggerIndex = testTriggerIndex_by_loc[trigger_loc];

                    boolean canBeTested = isValidTestCandidate(p);

                    // Setting sym response
                    if (testTriggerIndex < triggeredSymResponse[trigger_loc].length
                            && covid19.isInfected(p) && covid19.hasSymptoms(p)) {

                        double[] infectParam = covid19.getCurrentlyInfected().get(p.getId());
                        double[] sym_resp = pop.getSymptomResponse().get(p.getId());

                        // Symptom just appeared                        
                        if (infectParam[COVID19_Remote_Infection.PARAM_SYMPTOM_START_AGE] == (int) p.getAge()) {
                            if (sym_resp == null) {
                                sym_resp = pop.initiialiseSymInfectionResponse(triggeredSymResponse[trigger_loc][testTriggerIndex]);
                                pop.getSymptomResponse().put(p.getId(), sym_resp);
                            }
                        }

                        // Sym seek test
                        if (Population_Remote_MetaPopulation_COVID19.RESPONSE_SYM_SEEK_TEST
                                < triggeredSymResponse[trigger_loc][testTriggerIndex].length
                                && canBeTested) {

                            double seekTestProb = sym_resp[Population_Remote_MetaPopulation_COVID19.RESPONSE_SYM_SEEK_TEST];

                            if (pop.getInfectionRNG().nextDouble() < seekTestProb) {
                                int symTestDelay = 0;
                                int test_type = TEST_TYPE_SYM;

                                insertTestIntoSchdule(rmp, symTestDelay, test_type);
                            }

                        }

                    }

                    // Testing (Screening)                             
                    if (canBeTested && testTriggerIndex < triggeredTestRate[trigger_loc].length) {
                        float[] testRate = triggeredTestRate[trigger_loc][testTriggerIndex];

                        if (testRate[rmp.getHomeLocation()] > 0) {
                            float pTest = pop.getRNG().nextFloat();
                            float dailyRate = (float) (1 - Math.exp(Math.log(1 - testRate[rmp.getHomeLocation()])
                                    / AbstractFieldsArrayPopulation.ONE_YEAR_INT));
                            if (pTest < dailyRate) {
                                // Only test at home
                                if (rmp.getHomeLocation() == pop.getCurrentLocation(rmp)) {
                                    int srnTestDelay = 0;
                                    insertTestIntoSchdule(rmp, srnTestDelay, TEST_TYPE_SCR);
                                }
                            }
                        }
                    }

                }

                // Lock down                                               
                if (((float[][][]) getThreadParam()[THREAD_PARAM_TRIGGERED_METAPOP_LOCKDOWN_SETTING]).length > 0) {
                    for (int loc = 0; loc < popSize.length; loc++) {
                        int triggerIndex = testTriggerIndex_by_loc[loc];

                        if (triggers_at_time[loc][triggerIndex] == pop.getGlobalTime()) {
                            boolean inLockdownInterMetaPop
                                    = setMetaPopLockdownSetting(loc, triggerIndex,
                                            LOCKDOWN_INTER_METAPOP);

                            boolean inLockdownWithinMetaPop
                                    = setMetaPopLockdownSetting(loc, triggerIndex,
                                            LOCKDOWN_WITHIN_METAPOP);

                            if (inLockdownInterMetaPop || inLockdownWithinMetaPop) {
                                pop.triggerLockdown(loc);
                            }
                        }

                    }
                }

                for (int loc = 0; loc < popSize.length; loc++) {

                    List<Integer> timeLocKey = List.of(pop.getGlobalTime(), loc);

                    // Test schedule               
                    ArrayList<Object[]> testScheduleArr = pop.getTestScheduelInPipeline().remove(timeLocKey);
                    // Response schedule
                    ArrayList<Object[]> testResponseArr = pop.getTestOutcomeInPipeline().remove(timeLocKey);
                    // Place in quarantine
                    ArrayList<Integer[]> putInQuarantine = pop.getQuarantineInPipeline().remove(timeLocKey);

                    // Reset rolling limit to current timepoint         
                    int testLimit = Integer.MAX_VALUE;
                    int testQueueType = MAX_TEST_QUEUE_TYPE_NONE;

                    if (rolling_test_count_by_loc != null) {
                        int[] maxResponse = ((int[][]) getThreadParam()[THREAD_PARAM_MAX_TEST_BY_LOC])[loc];
                        int timeIndex = pop.getGlobalTime() % rolling_test_count_by_loc[loc].length;
                        rollingSum[loc] -= rolling_test_count_by_loc[loc][timeIndex];
                        rolling_test_count_by_loc[loc][timeIndex] = 0;
                        testLimit = Math.max(0, maxResponse[MAX_TEST_NUM] - rollingSum[loc]);
                        if (MAX_TEST_QUEUE_SETTING < maxResponse.length) {
                            testQueueType = maxResponse[MAX_TEST_QUEUE_SETTING];
                        }
                    }

                    int testCount = 0;

                    while (testScheduleArr != null || testResponseArr != null
                            || putInQuarantine != null) {

                        if (testScheduleArr != null) {
                            // Modification of test queue - if any

                            // Running test schedule - first round
                            for (Object[] ent : testScheduleArr) {

                                AbstractIndividualInterface testPerson = ((AbstractIndividualInterface) ent[Population_Remote_MetaPopulation_COVID19.TEST_OUTCOME_PIPELINE_ENT_PERSON_TESTED]);

                                if (isValidTestCandidate(testPerson)) {

                                    int pid = testPerson.getId();
                                    int waitingIndex = Collections.binarySearch(testResultInWaiting, pid);

                                    if (waitingIndex < 0) {
                                        if (testCount >= testLimit) {
                                            // Handle exceed test scheduled
                                            handleExcessTest(ent, testQueueType);
                                        } else {
                                            testResultInWaiting.add(~waitingIndex, pid);
                                            runTestSchedule(ent, covid19, testTriggerIndex_by_loc[loc]);

                                            if (rolling_test_count_by_loc != null) {
                                                int timeIndex = pop.getGlobalTime() % rolling_test_count_by_loc[loc].length;
                                                rolling_test_count_by_loc[loc][timeIndex]++;
                                                rollingSum[loc]++;
                                            }
                                            testCount++;
                                        }
                                    }
                                }
                            }

                        }

                        if (testResponseArr != null) {
                            // Modification of resposne queue - if any

                            for (Object[] ent : testResponseArr) {
                                Person_Remote_MetaPopulation rmp
                                        = (Person_Remote_MetaPopulation) ent[Population_Remote_MetaPopulation_COVID19.TEST_OUTCOME_PIPELINE_ENT_PERSON_TESTED];

                                int remIndex = Collections.binarySearch(testResultInWaiting, rmp.getId());
                                testResultInWaiting.remove(remIndex);

                                testing_res_stat_cumul[TEST_RES_STAT_ALL][loc]++;
                                testing_res_stat_cumul_all[TEST_RES_STAT_ALL]++;
                                test_res_stat_today[TEST_RES_STAT_ALL]++;

                                Integer[] testHist = pop.getTestResultHistory().get(rmp.getId());
                                if (testHist == null) {
                                    testHist = new Integer[Population_Remote_MetaPopulation_COVID19.TEST_RESULT_HISTORY_LENGTH];
                                    Arrays.fill(testHist, 0);
                                    pop.getTestResultHistory().put(rmp.getId(), testHist);
                                }

                                if ((Boolean) ent[Population_Remote_MetaPopulation_COVID19.TEST_OUTCOME_PIPELINE_ENT_TEST_RESULT]) {

                                    // Positive test result
                                    testing_res_stat_cumul[TEST_RES_STAT_POS][loc]++;
                                    testing_res_stat_cumul_all[TEST_RES_STAT_POS]++;
                                    test_res_stat_today[TEST_RES_STAT_POS]++;

                                    int trigger_loc = pop.getCurrentLocation(rmp);
                                    if (trigger_loc < 0) {
                                        trigger_loc = rmp.getHomeLocation();
                                    }

                                    int triggerIndex = testTriggerIndex_by_loc[trigger_loc];

                                    testHist[Population_Remote_MetaPopulation_COVID19.TEST_RESULT_HISTORY_AGE_OF_LAST_POSITIVE]
                                            = (int) rmp.getAge();

                                    runPositiveTestResponse(rmp, covid19,
                                            (Integer) ent[Population_Remote_MetaPopulation_COVID19.TEST_OUTCOME_PIPELINE_ENT_TEST_TYPE],
                                            triggerIndex);
                                } else {

                                    // Negative test result                                        
                                    testHist[Population_Remote_MetaPopulation_COVID19.TEST_RESULT_HISTORY_AGE_OF_LAST_NEGATIVE]
                                            = (int) rmp.getAge();
                                    int testType = (int) ent[Population_Remote_MetaPopulation_COVID19.TEST_OUTCOME_PIPELINE_ENT_TEST_TYPE];

                                    if (testType == TEST_TYPE_EXIT_CI
                                            || testType == TEST_TYPE_EXIT_QUARANTINE) {

                                        // Leave quarantine immediately
                                        HashMap<Integer, Number[]> qMap
                                                = ((HashMap<Integer, Number[]>) pop.getFields()[Population_Remote_MetaPopulation_COVID19.FIELDS_REMOTE_METAPOP_COVID19_CURRENTLY_IN_QUARANTINE]);

                                        qMap.remove(rmp.getId());

                                    }

                                }

                            }
                        }

                        if (putInQuarantine != null) {
                            HashMap<Integer, Number[]> qMap
                                    = ((HashMap<Integer, Number[]>) pop.getFields()[Population_Remote_MetaPopulation_COVID19.FIELDS_REMOTE_METAPOP_COVID19_CURRENTLY_IN_QUARANTINE]);

                            float[][][] triggeredQuarantineEffect = (float[][][]) getThreadParam()[THREAD_PARAM_TRIGGRRED_QUARANTINE_EFFECT];
                            float[] quarantineEffect = triggeredQuarantineEffect.length == 0
                                    ? null : triggeredQuarantineEffect[loc][testTriggerIndex_by_loc[loc]];

                            for (Integer[] inQ : putInQuarantine) {
                                Number[] ent = new Number[Population_Remote_MetaPopulation_COVID19.QUARANTINE_ENTRY_LENGTH];
                                Arrays.fill(ent, 0);

                                boolean isCaseIsolation
                                        = inQ[Population_Remote_MetaPopulation_COVID19.QUARANTINE_PIPELINE_ENT_CASE_ISOLATION] > 0;

                                ent[Population_Remote_MetaPopulation_COVID19.QUARANTINE_UNTIL_AGE]
                                        = inQ[Population_Remote_MetaPopulation_COVID19.QUARANTINE_PIPELINE_ENT_IN_QUARANTINE_UNTIL];

                                if (quarantineEffect != null) {

                                    if (!isCaseIsolation) {
                                        ent[Population_Remote_MetaPopulation_COVID19.QUARANTINE_PROB_CONTACT_HOUSEHOLD]
                                                = quarantineEffect[QUARANTINE_EFFECT_HOUSEHOLD_CONTACT_ADJ];
                                        ent[Population_Remote_MetaPopulation_COVID19.QUARANTINE_PROB_CONTACT_NON_HOUSEHOLD]
                                                = quarantineEffect[QUARANTINE_EFFECT_NON_HOUSEHOLD_CONTACT_ADJ];

                                    }

                                    if (QUARANTINE_EFFECT_DAYS_TO_EXIT_TEST_QUARANTINE < quarantineEffect.length) {
                                        int daysToExitTest = isCaseIsolation
                                                ? (int) quarantineEffect[QUARANTINE_EFFECT_DAYS_TO_EXIT_TEST_CI]
                                                : (int) quarantineEffect[QUARANTINE_EFFECT_DAYS_TO_EXIT_TEST_QUARANTINE];
                                        int exitTestType = isCaseIsolation ? TEST_TYPE_EXIT_CI : TEST_TYPE_EXIT_QUARANTINE;

                                        AbstractIndividualInterface testPerson = pop.getPersonById(inQ[Population_Remote_MetaPopulation_COVID19.QUARANTINE_PIPELINE_ENT_PERSON_ID]);

                                        insertTestIntoSchdule((Person_Remote_MetaPopulation) testPerson, daysToExitTest, exitTestType);

                                    }

                                }

                                qMap.put(inQ[Population_Remote_MetaPopulation_COVID19.QUARANTINE_PIPELINE_ENT_PERSON_ID], ent);
                            }
                        }

                        // Test schedule - mulitple rounds 
                        testScheduleArr = pop.getTestScheduelInPipeline().remove(timeLocKey);
                        // Resposnse schedule - mulitple rounds
                        testResponseArr = pop.getTestOutcomeInPipeline().remove(timeLocKey);
                        // Place in quarantine - multiple rounds
                        putInQuarantine = pop.getQuarantineInPipeline().remove(timeLocKey);

                    }

                }

                for (int loc = 0; loc < popSize.length; loc++) {
                    // Determine trigger 
                    if (testTriggerIndex_by_loc[loc] + 1 < triggers[loc].length) {

                        int testTriggerIndex_org = testTriggerIndex_by_loc[loc];
                        testTriggerIndex_by_loc[loc] = detectResponseTrigger(
                                testTriggerIndex_by_loc[loc],
                                triggers[loc],
                                new int[]{testing_res_stat_cumul[TEST_RES_STAT_ALL][loc],
                                    testing_res_stat_cumul[TEST_RES_STAT_POS][loc]},
                                testing_res_stat_cumul_all);

                        if (testTriggerIndex_by_loc[loc] != testTriggerIndex_org) {
                            triggers_at_time[loc][testTriggerIndex_by_loc[loc]] = pop.getGlobalTime() + 1;
                            // New trigger
                            triggerText.append(pop.getGlobalTime() + 1);
                            triggerText.append(',');
                            triggerText.append(String.format("Level %d response triggered at location %d.\n",
                                    testTriggerIndex_by_loc[loc], loc));
                        }

                    }
                }

                // Debug
                if (printDebug) {
                    for (int loc = 0; loc < patientZeroByLoc.length; loc++) {
                        AbstractIndividualInterface patientZero = patientZeroByLoc[loc];
                        double[] stat = covid19.getCurrentlyInfected().get(patientZero.getId());
                        int ageExp = -1;
                        if (stat != null) {
                            ageExp = (int) stat[COVID19_Remote_Infection.PARAM_AGE_OF_EXPOSURE];
                        }

                        System.out.println(String.format("%d: PZ at Loc #%d Inf_Stat =(%b, %b, %b). Age of exp = %d",
                                pop.getGlobalTime(),
                                loc,
                                covid19.isInfected(patientZero),
                                covid19.isInfectious(patientZero),
                                covid19.hasSymptoms(patientZero),
                                ageExp));
                    }
                }

                pop.advanceTimeStep(1);

            }

            pop.printCSVOutputEntry(outputCSV, pop.generateInfectionStat());
            printCSVTestEntry(testingCSV, testing_res_stat_cumul, responseQueueList);

        }

        /*
        if (triggerText.toString().length() > 0) {
            testingCSV.println();
            testingCSV.println(triggerText.toString());
        }
         */
        outputCSV.close();
        testingCSV.close();

        try {
            // Printing end pop_stat file
            if (FILE_REGEX_POP_SNAP != null) {
                popStatCSV = new PrintWriter(new File(baseDir, String.format(FILE_REGEX_POP_SNAP,
                        this.threadId, getPop().getGlobalTime())));
                pop.printPopulationSnapCSV(popStatCSV);
                popStatCSV.close();
            }

        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }

        pri.close();
    }

    private void insertTestIntoSchdule(Person_Remote_MetaPopulation rmp, int testDelay, int test_type) {
        if (rmp != null) {
            int home_loc = pop.getCurrentLocation(rmp);
            if (home_loc < 0) {
                home_loc = rmp.getHomeLocation();
            }
            ArrayList<Object[]> testSchEnt = getTestScheuduleEnt(testDelay, home_loc);
            Object[] testEnt = new Object[]{rmp, rmp.getAge(), test_type};

            int index = Collections.binarySearch(testSchEnt, testEnt, new Comparator<Object[]>() {
                @Override
                public int compare(Object[] o1, Object[] o2) {
                    AbstractIndividualInterface p1 = (AbstractIndividualInterface) o1[0];
                    AbstractIndividualInterface p2 = (AbstractIndividualInterface) o2[0];
                    return Integer.compare(p1.getId(), p2.getId());
                }
            });

            if (index < 0) {
                testSchEnt.add(~index, testEnt);
            }
        }
    }

    private boolean isValidTestCandidate(AbstractIndividualInterface testPerson) {
        boolean validTestCandidate;
        int pid = testPerson.getId();
        Integer[] testHist = pop.getTestResultHistory().get(pid);
        Integer minRetestAge = pop.getMinAgeForNextTest().get(pid);
        validTestCandidate = minRetestAge == null
                || testPerson.getAge() > minRetestAge;
        validTestCandidate &= (testHist == null)
                || (testHist[Population_Remote_MetaPopulation_COVID19.TEST_RESULT_HISTORY_AGE_OF_LAST_NEGATIVE]
                > testHist[Population_Remote_MetaPopulation_COVID19.TEST_RESULT_HISTORY_AGE_OF_LAST_POSITIVE]);
        return validTestCandidate;
    }

    private boolean setMetaPopLockdownSetting(int loc, int triggerIndex, int lockdownType) {
        float[][][] triggeredLockdownSetting
                = (float[][][]) getThreadParam()[THREAD_PARAM_TRIGGERED_METAPOP_LOCKDOWN_SETTING];

        float[][] metaPopLockdownSetting
                = (float[][]) pop.getFields()[Population_Remote_MetaPopulation_COVID19.FIELDS_REMOTE_METAPOP_COVID19_META_POP_LOCKDOWN_SETTING];

        int lockdownDur = lockdownType == LOCKDOWN_INTER_METAPOP
                ? LOCKDOWN_INTER_METAPOP_DURATION : LOCKDOWN_WITHIN_METAPOP_DURATION;
        int lockdown_start = lockdownType == LOCKDOWN_INTER_METAPOP
                ? Population_Remote_MetaPopulation_COVID19.META_POP_INTER_LOCKDOWN_START
                : Population_Remote_MetaPopulation_COVID19.META_POP_WITHIN_LOCKDOWN_START;
        int lockdown_prob = lockdownType == LOCKDOWN_INTER_METAPOP
                ? Population_Remote_MetaPopulation_COVID19.META_POP_INTER_LOCKDOWN_PROPORTION_HOUSEHOLD
                : Population_Remote_MetaPopulation_COVID19.META_POP_WITHIN_LOCKDOWN_PROPORTION_HOUSEHOLD;
        int lockdown_end = lockdownType == LOCKDOWN_INTER_METAPOP
                ? Population_Remote_MetaPopulation_COVID19.META_POP_INTER_LOCKDOWN_END
                : Population_Remote_MetaPopulation_COVID19.META_POP_WITHIN_LOCKDOWN_END;

        boolean inLockdown
                = triggeredLockdownSetting[loc][triggerIndex][lockdownType] > 0
                && lockdownDur < triggeredLockdownSetting[loc][triggerIndex].length;

        if (inLockdown) {
            metaPopLockdownSetting[loc][lockdown_start]
                    = pop.getGlobalTime();
            metaPopLockdownSetting[loc][lockdown_prob]
                    = triggeredLockdownSetting[loc][triggerIndex][lockdownType];
            if (triggeredLockdownSetting[loc][triggerIndex][lockdownDur] > 0) {
                metaPopLockdownSetting[loc][lockdown_end]
                        = pop.getGlobalTime() + (int) triggeredLockdownSetting[loc][triggerIndex][lockdownDur];
            } else {
                metaPopLockdownSetting[loc][lockdown_end] = Float.POSITIVE_INFINITY;
            }
        }
        return inLockdown;
    }

    private void handleExcessTest(Object[] testSchduled, int testQueueType) {
        // Handle exceed test scheduled
        switch (testQueueType) {
            default:
                // No more test available - test abandoned
                break;
        }
    }

    private boolean runTestSchedule(Object[] testSchduled,
            COVID19_Remote_Infection covid19, int triggerIndex) {

        Person_Remote_MetaPopulation rmp
                = (Person_Remote_MetaPopulation) testSchduled[Population_Remote_MetaPopulation_COVID19.TEST_SCHEDULE_PIPELINE_ENT_PERSON_TESTED];
        int testType = (Integer) testSchduled[Population_Remote_MetaPopulation_COVID19.TEST_SCHEDULE_PIPELINE_ENT_TEST_TYPE];
        int loc = pop.getCurrentLocation(rmp);

        float[][][] triggeredTestResultDelay = (float[][][]) getThreadParam()[THREAD_PARAM_TRIGGERED_TESTING_RESULT_DELAY];

        float[] delayOption = triggeredTestResultDelay.length > 0
                ? triggeredTestResultDelay[loc][triggerIndex] : new float[]{};

        boolean testPositive;
        if (delayOption.length == 0) { // Special case for instant test and results for all test
            testPositive = covid19.isInfected(rmp);
            if (testPositive) {
                setTriggeredIndexCaseTestResponse(rmp, covid19, triggerIndex);
            }
        } else {
            testPositive = insertTestingResult(rmp, covid19, testType, delayOption);
        }
        return testPositive;
    }

    private ArrayList<Object[]> getTestScheuduleEnt(int testDelay, int testLoc) {
        List<Integer> testSchKey = List.of((int) (pop.getGlobalTime() + testDelay), testLoc);
        ArrayList<Object[]> testSchEnt = pop.getTestScheduelInPipeline().get(testSchKey);
        if (testSchEnt == null) {
            testSchEnt = new ArrayList<>();
            pop.getTestScheduelInPipeline().put(testSchKey, testSchEnt);
        }
        return testSchEnt;
    }

    protected void printCSVTestEntry(PrintWriter testingCSV, int[][] testing_stat_cumul,
            List<LinkedList<Object[]>> responseQueueList) {
        testingCSV.print(pop.getGlobalTime());
        for (int[] testing_stat_cumul_type : testing_stat_cumul) {
            for (int loc = 0; loc < testing_stat_cumul_type.length; loc++) {
                testingCSV.print(',');
                testingCSV.print(testing_stat_cumul_type[loc]);
            }
        }

        for (int loc = 0; loc < responseQueueList.size(); loc++) {
            testingCSV.print(',');
            testingCSV.print(responseQueueList.get(loc).size());
        }

        testingCSV.println();
    }

    protected boolean insertTestingResult(Person_Remote_MetaPopulation rmp,
            COVID19_Remote_Infection covid19, int testType,
            float[] resultDelayOptions) {

        // Index case testing for testResponse = null  or if resultDelayOptions[0] < 0        
        boolean hasPositiveTest = covid19.isInfected(rmp);

        double[] test_sensitivity = (double[]) getThreadParam()[THREAD_PARAM_TEST_SENSITIVITY];

        if (hasPositiveTest) {

            if (test_sensitivity != null
                    && test_sensitivity.length > 0) {
                double pTest;
                pTest = test_sensitivity[SENSITIVITY_INFECTED];

                if (test_sensitivity.length > 1) {
                    pTest = 0;
                    double[] inf_param = covid19.getCurrentlyInfected().get(rmp.getId());

                    // For all infection 
                    if (rmp.getAge() < inf_param[COVID19_Remote_Infection.PARAM_INFECTIOUS_START_AGE]) {
                        pTest = Math.max(pTest, test_sensitivity[SENSITIVITY_LATANT]);
                    } else if (rmp.getAge() < inf_param[COVID19_Remote_Infection.PARAM_INFECTIOUS_END_AGE]) {
                        pTest = Math.max(pTest, test_sensitivity[SENSITIVITY_INFECTIOUS]);
                    } else if (rmp.getAge() < inf_param[COVID19_Remote_Infection.PARAM_INFECTED_UNTIL_AGE]) {
                        pTest = Math.max(pTest, test_sensitivity[SENSITIVITY_POST_INFECTIOUS]);
                    }

                    // For symptomatic infection 
                    if (inf_param[COVID19_Remote_Infection.PARAM_SYMPTOM_START_AGE] > 0) {
                        if (rmp.getAge() < inf_param[COVID19_Remote_Infection.PARAM_SYMPTOM_START_AGE]) {
                            pTest = Math.max(pTest, test_sensitivity[SENSITIVITY_INCUBRATION]);
                        } else if (covid19.hasSymptoms(rmp)) {
                            pTest = Math.max(pTest, test_sensitivity[SENSITIVITY_SYMPTOMATIC]);
                        }
                    }
                }

                hasPositiveTest &= pTest >= 1;
                if (!hasPositiveTest && pTest > 0) {
                    hasPositiveTest &= pop.getInfectionRNG().nextDouble() < pTest;
                }

            }
        }

        Object[] testOutcomeEnt = new Object[Population_Remote_MetaPopulation_COVID19.TEST_OUTCOME_PIPELINE_ENT_LENGTH];

        testOutcomeEnt[Population_Remote_MetaPopulation_COVID19.TEST_OUTCOME_PIPELINE_ENT_PERSON_TESTED] = rmp;
        testOutcomeEnt[Population_Remote_MetaPopulation_COVID19.TEST_OUTCOME_PIPELINE_ENT_TEST_RESULT] = hasPositiveTest;
        testOutcomeEnt[Population_Remote_MetaPopulation_COVID19.TEST_OUTCOME_PIPELINE_ENT_TEST_TYPE] = testType;
        testOutcomeEnt[Population_Remote_MetaPopulation_COVID19.TEST_OUTCOME_PIPELINE_ENT_AGE_TESTED] = rmp.getAge();

        int currentLoc = pop.getCurrentLocation(rmp);
        int resultDelay = 0;
        List<Integer> testOutcomeKey;

        if (resultDelayOptions.length > 0) {
            resultDelay = getDelay(resultDelayOptions);
        }

        testOutcomeKey = List.of(pop.getGlobalTime() + resultDelay, currentLoc);

        ArrayList<Object[]> testResultAt = pop.getTestOutcomeInPipeline().get(testOutcomeKey);

        if (testResultAt == null) {
            testResultAt = new ArrayList<>();
            pop.getTestOutcomeInPipeline().put(testOutcomeKey, testResultAt);
        }

        int index = Collections.binarySearch(testResultAt, testOutcomeEnt,
                new Comparator<Object[]>() {

            @Override
            public int compare(Object[] o1, Object[] o2) {
                AbstractIndividualInterface p1, p2;
                p1 = (AbstractIndividualInterface) o1[Population_Remote_MetaPopulation_COVID19.TEST_OUTCOME_PIPELINE_ENT_PERSON_TESTED];
                p2 = (AbstractIndividualInterface) o2[Population_Remote_MetaPopulation_COVID19.TEST_OUTCOME_PIPELINE_ENT_PERSON_TESTED];
                return Integer.compare(p1.getId(), p2.getId());
            }
        });

        if (index < 0) {
            testResultAt.add(~index, testOutcomeEnt);
        }

        return hasPositiveTest;
    }

    protected void runPositiveTestResponse(Person_Remote_MetaPopulation rmp,
            COVID19_Remote_Infection covid19,
            int srcTestType,
            int triggerIndex) {

        int trigger_loc = pop.getCurrentLocation(rmp);
        if (trigger_loc < 0) {
            trigger_loc = rmp.getHomeLocation();
        }

        double[][][] triggetedHouseholdTestRate = (double[][][]) getThreadParam()[THREAD_PARAM_TRIGGERED_HOUSEHOLD_TESTING];
        double[][][] triggetedHouseholdQuarantineRate = (double[][][]) getThreadParam()[THREAD_PARAM_TRIGGERED_HOUSEHOLD_QUARANTINE];
        float[][][] triggeredContactDelay = (float[][][]) getThreadParam()[THREAD_PARAM_TRIGGERED_CONTACT_TRACE_DELAY];
        float[][][] triggeredQuarantineDelay = (float[][][]) getThreadParam()[THREAD_PARAM_TRIGGERED_QUARANTINE_DELAY];
        float[][][] triggeredQuarantineDuration = (float[][][]) getThreadParam()[THREAD_PARAM_TRIGGRRED_QUARANTINE_DURATION];
        float[][][] triggeredContactHistoryInterventions = (float[][][]) getThreadParam()[THREAD_PARAM_TRIGGRRED_CONTACT_HISTORY_INTERVENTIONS];

        double[] householdTestRate = triggetedHouseholdTestRate.length == 0
                ? new double[0] : triggetedHouseholdTestRate[trigger_loc][triggerIndex];
        double[] householdQuarantineRate = triggetedHouseholdQuarantineRate.length == 0
                ? new double[0] : triggetedHouseholdQuarantineRate[trigger_loc][triggerIndex];
        float[] contactTraceDelayOptions = triggeredContactDelay.length == 0
                ? new float[0] : triggeredContactDelay[trigger_loc][triggerIndex];
        float[] quarantineDelayOptions = triggeredQuarantineDelay.length == 0
                ? new float[0] : triggeredQuarantineDelay[trigger_loc][triggerIndex];
        float[] quarantineDurationOptions = triggeredQuarantineDuration.length == 0
                ? new float[0] : triggeredQuarantineDuration[trigger_loc][triggerIndex];
        float[] contactHistoryInterventions = triggeredContactHistoryInterventions.length == 0
                ? new float[0] : triggeredContactHistoryInterventions[trigger_loc][triggerIndex];

        // Single test response
        setTriggeredIndexCaseTestResponse(rmp, covid19, triggerIndex);

        // Contract tracing - only applied on non-exit test
        if (srcTestType != TEST_TYPE_EXIT_QUARANTINE && srcTestType != TEST_TYPE_EXIT_CI) {

            int contactTraceDelay = 0;

            if (contactTraceDelayOptions.length > 0) {
                contactTraceDelay = getDelay(contactTraceDelayOptions);
            }

            int quaratineDelay = 0;
            if (quarantineDelayOptions.length > 0) {
                quaratineDelay = getDelay(quarantineDelayOptions);
            }

            // Household based
            if (householdTestRate.length > 0 || householdQuarantineRate.length > 0) {

                AbstractIndividualInterface[][] fromCurrentHousehold = pop.currentlyInSameHouseholdAsByType(rmp);

                // probability_current_household, probability_core_household, probility_non_core_household, prob temp            
                for (int type = 0; type < fromCurrentHousehold.length; type++) {
                    for (AbstractIndividualInterface candidate : fromCurrentHousehold[type]) {

                        // Household test
                        if (householdTestRate.length > 0) {
                            Integer inQuarantine = pop.inQuarantineUntil(candidate);
                            boolean canBeTested = isValidTestCandidate(candidate)
                                    && inQuarantine == null;

                            if (canBeTested) {
                                boolean contactTesting = householdTestRate[type] >= 1;

                                if (!contactTesting && householdTestRate[type] > 0) {
                                    contactTesting = pop.getInfectionRNG().nextDouble()
                                            < householdTestRate[type];
                                }

                                if (contactTesting) {
                                    insertTestIntoSchdule((Person_Remote_MetaPopulation) candidate,
                                            contactTraceDelay, Math.min(srcTestType, 0) - 1);
                                }
                            }
                        }
                        // Household quaratine
                        if (householdQuarantineRate.length > 0) {
                            boolean putInQuarantine = householdQuarantineRate[type] >= 1;
                            if (!putInQuarantine && householdQuarantineRate[type] > 0) {
                                putInQuarantine = pop.getInfectionRNG().nextDouble() < householdQuarantineRate[type];
                            }
                            if (putInQuarantine) {
                                boolean isCaseIsolation = false;
                                int inQuarntineDuration = (int) householdQuarantineRate[type + fromCurrentHousehold.length];
                                inQuarntineDuration = getInQuaratineDuration(quarantineDurationOptions, inQuarntineDuration);

                                int q_delay = contactTraceDelay + quaratineDelay;
                                Person_Remote_MetaPopulation candidate_rmp = (Person_Remote_MetaPopulation) candidate;

                                insertQuaratineInSchdule(candidate_rmp, q_delay, (int) candidate_rmp.getAge() + inQuarntineDuration,
                                        isCaseIsolation);

                            }
                        }
                    }
                }
            }

            // Contract history based
            if (contactHistoryInterventions.length > 0) {
                List<List<ArrayList<Integer>>> contactHistory = pop.getInfectionContactHistory().get(rmp.getId());

                int inQuarantineDuration = 14;
                inQuarantineDuration = getInQuaratineDuration(quarantineDurationOptions, inQuarantineDuration);

                // Testing contact history of household and non-household
                if (CONTACT_HIST_HOUSEHOLD_TEST_WINDOW < contactHistoryInterventions.length
                        && contactHistoryInterventions[CONTACT_HIST_HOUSEHOLD_TEST_PROB] > 0) {
                    int contractHistoryWindow = Math.min(Population_Remote_MetaPopulation_COVID19.INFECTION_CONTACT_HISTORY_WINDOW,
                            (int) contactHistoryInterventions[CONTACT_HIST_HOUSEHOLD_TEST_WINDOW]);
                    ArrayList<Integer> candidates = getContactHistoryCandidate(contractHistoryWindow,
                            contactHistory.get(Population_Remote_MetaPopulation_COVID19.INFECTION_CONTACT_HISTORY_HOUSEHOLD));
                    contactHistoryInterventions(candidates,
                            contactHistoryInterventions[CONTACT_HIST_HOUSEHOLD_TEST_PROB],
                            true, false, contactTraceDelay, inQuarantineDuration,srcTestType);
                }
                if (CONTACT_HIST_NON_HOUSEHOLD_TEST_WINDOW < contactHistoryInterventions.length
                        && contactHistoryInterventions[CONTACT_HIST_NON_HOUSEHOLD_TEST_PROB] > 0) {
                    int contractHistoryWindow = Math.min(Population_Remote_MetaPopulation_COVID19.INFECTION_CONTACT_HISTORY_WINDOW,
                            (int) contactHistoryInterventions[CONTACT_HIST_NON_HOUSEHOLD_TEST_WINDOW]);
                    ArrayList<Integer> candidates = getContactHistoryCandidate(contractHistoryWindow,
                            contactHistory.get(Population_Remote_MetaPopulation_COVID19.INFECTION_CONTACT_HISTORY_NON_HOUSEHOLD));
                    contactHistoryInterventions(candidates,
                            contactHistoryInterventions[CONTACT_HIST_NON_HOUSEHOLD_TEST_PROB],
                            true, false, contactTraceDelay, inQuarantineDuration, srcTestType);
                }

                // Quarantine contact history of household and non-household                
                if (CONTACT_HIST_HOUSEHOLD_QUARANTINE_WINDOW < contactHistoryInterventions.length
                        && contactHistoryInterventions[CONTACT_HIST_HOUSEHOLD_QUARANTINE_PROB] > 0) {
                    int contractHistoryWindow = Math.min(Population_Remote_MetaPopulation_COVID19.INFECTION_CONTACT_HISTORY_WINDOW,
                            (int) contactHistoryInterventions[CONTACT_HIST_HOUSEHOLD_QUARANTINE_WINDOW]);
                    ArrayList<Integer> candidates = getContactHistoryCandidate(contractHistoryWindow,
                            contactHistory.get(Population_Remote_MetaPopulation_COVID19.INFECTION_CONTACT_HISTORY_HOUSEHOLD));
                    contactHistoryInterventions(candidates,
                            contactHistoryInterventions[CONTACT_HIST_HOUSEHOLD_QUARANTINE_PROB],
                            false, true, contactTraceDelay + quaratineDelay, inQuarantineDuration, srcTestType);
                }
                if (CONTACT_HIST_NON_HOUSEHOLD_QUARANTINE_WINDOW < contactHistoryInterventions.length
                        && contactHistoryInterventions[CONTACT_HIST_NON_HOUSEHOLD_QUARANTINE_PROB] > 0) {
                    int contractHistoryWindow = Math.min(Population_Remote_MetaPopulation_COVID19.INFECTION_CONTACT_HISTORY_WINDOW,
                            (int) contactHistoryInterventions[CONTACT_HIST_NON_HOUSEHOLD_QUARANTINE_WINDOW]);
                    ArrayList<Integer> candidates = getContactHistoryCandidate(contractHistoryWindow,
                            contactHistory.get(Population_Remote_MetaPopulation_COVID19.INFECTION_CONTACT_HISTORY_NON_HOUSEHOLD));
                    contactHistoryInterventions(candidates,
                            contactHistoryInterventions[CONTACT_HIST_NON_HOUSEHOLD_QUARANTINE_PROB],
                            false, true, contactTraceDelay + quaratineDelay, inQuarantineDuration, srcTestType);
                }

            }

        }

    }

    private int getInQuaratineDuration(float[] quarantineDurationOptions, int inQuarantineDuration) {
        if (quarantineDurationOptions.length > 0) {
            int index = 0;
            inQuarantineDuration = (int) quarantineDurationOptions[index];
            index += 2;
            if (quarantineDurationOptions.length > 0) {
                float pDur = pop.getInfectionRNG().nextFloat();
                while (index < quarantineDurationOptions.length
                        && pDur >= quarantineDurationOptions[index - 1]) {
                    inQuarantineDuration = (int) quarantineDurationOptions[index];
                    index += 2;
                }
            }
        }
        return inQuarantineDuration;
    }

    private void contactHistoryInterventions(
            ArrayList<Integer> candidates,
            float complianceProb,
            boolean isContactTesting,
            boolean isContactQuarantine,
            int interventionDelay,
            int quarantineDuration,
            int srcTestType) {

        for (Integer cid : candidates) {
            boolean comply = complianceProb > 0;
            if (complianceProb < 1) {
                comply &= pop.getRNG().nextFloat() < complianceProb;
            }
            if (comply) {
                Person_Remote_MetaPopulation targetCandidiate = (Person_Remote_MetaPopulation) pop.getPersonById(cid);
                if (targetCandidiate != null) {
                    if (isContactTesting) {
                        comply &= isValidTestCandidate(targetCandidiate);
                        if (comply) {
                            insertTestIntoSchdule(targetCandidiate,
                                    interventionDelay, Math.min(srcTestType, 0) - 1);
                        }
                    }
                    if (isContactQuarantine) {
                        boolean isCaseIsolation = false;
                        int inQuarntineUntilAge = (int) targetCandidiate.getAge() + quarantineDuration;
                        insertQuaratineInSchdule(targetCandidiate, interventionDelay,
                                inQuarntineUntilAge, isCaseIsolation);

                    }
                }
            }
        }
    }

    private ArrayList<Integer> getContactHistoryCandidate(int traceBack, List<ArrayList<Integer>> contactHistoryByType) {
        ArrayList<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < traceBack; i++) {
            int windowIndex = (pop.getGlobalTime() - i)
                    % Population_Remote_MetaPopulation_COVID19.INFECTION_CONTACT_HISTORY_WINDOW;
            ArrayList<Integer> contacts = contactHistoryByType.get(windowIndex);
            for (Integer cid : contacts) {
                int index = Collections.binarySearch(candidates, cid);
                if (index < 0) {
                    candidates.add(~index, cid);
                }
            }
        }
        return candidates;
    }

    private ArrayList<Integer[]> getQuarantineScheduleEnt(int quarantineDelay, int loc) {
        int inQ_time = pop.getGlobalTime() + quarantineDelay;
        List<Integer> quarnatineSchKey = List.of(inQ_time, loc);
        ArrayList<Integer[]> inQ = pop.getQuarantineInPipeline().get(quarnatineSchKey);
        if (inQ == null) {
            inQ = new ArrayList<>();
            pop.getQuarantineInPipeline().put(quarnatineSchKey, inQ);
        }
        return inQ;
    }

    private int getDelay(float[] delayOptions) {
        int delay = (int) Math.abs(delayOptions[DELAY_OPTIONS_MIN]);
        if (delayOptions.length > DELAY_OPTIONS_MAX && delayOptions[DELAY_OPTIONS_MAX] > delay) {
            delay += pop.getInfectionRNG().nextInt((int) (delayOptions[DELAY_OPTIONS_MAX] - delay));
        }
        return delay;
    }

    protected void setTriggeredIndexCaseTestResponse(Person_Remote_MetaPopulation rmp,
            COVID19_Remote_Infection covid19, int triggerIndex) {

        double[] test_resp = pop.getTestResponse().get(rmp.getId());

        if (test_resp == null) {
            test_resp = new double[Population_Remote_MetaPopulation_COVID19.TEST_RESPONSE_TOTAL];
        }

        // Special resposne for null delay
        double[][][][] triggeredTestResponse = (double[][][][]) getThreadParam()[THREAD_PARAM_TRIGGERED_TEST_RESPONSE];
        double[][] default_test_resp;
        if (triggeredTestResponse.length == 0) {
            default_test_resp = new double[0][];
        } else {
            int currentLoc = pop.getCurrentLocation(rmp);
            if (currentLoc == -1) {
                currentLoc = rmp.getHomeLocation();
            }
            default_test_resp = triggeredTestResponse[currentLoc][triggerIndex];
        }

        // Assume will not be tested again until symptom disspates or within the 14 days period
        int minRetestAge = (int) Math.max(rmp.getAge() + default_test_resp[0][0],
                covid19.getCurrentlyInfected().get(rmp.getId())[COVID19_Remote_Infection.PARAM_SYMPTOM_END_AGE]);

        test_resp[Population_Remote_MetaPopulation_COVID19.TEST_RESPONSE_VALID_UNTIL_AGE]
                = minRetestAge;

        for (int k = 1; k < default_test_resp.length; k++) {
            test_resp[k - 1] = 1; // No response
            boolean correctResp = false;
            for (int rp = 0; rp < default_test_resp[k].length && !correctResp; rp += 2) {
                correctResp = default_test_resp[k][rp] >= 1;
                if (default_test_resp[k][rp] < 1) {
                    correctResp = pop.getInfectionRNG().nextDouble() < default_test_resp[k][rp];
                }
                if (correctResp) {
                    test_resp[k - 1] = default_test_resp[k][rp + 1];
                }
            }
        }

        pop.getTestResponse().put(rmp.getId(), test_resp);
        pop.getMinAgeForNextTest().put(rmp.getId(), minRetestAge);

        if (test_resp[Population_Remote_MetaPopulation_COVID19.RESPONSE_ADJ_HOUSEHOLD_CONTACT] == 0
                && test_resp[Population_Remote_MetaPopulation_COVID19.RESPONSE_ADJ_NON_HOUSEHOLD_CONTACT] == 0) {

            int trigger_loc = pop.getCurrentLocation(rmp);
            float[][][] triggeredQuarantineDelay = (float[][][]) getThreadParam()[THREAD_PARAM_TRIGGERED_QUARANTINE_DELAY];
            float[][][] triggeredQuarantineDuration = (float[][][]) getThreadParam()[THREAD_PARAM_TRIGGRRED_QUARANTINE_DURATION];

            float[] quarantineDelayOptions = triggeredQuarantineDelay.length == 0
                    ? new float[0] : triggeredQuarantineDelay[trigger_loc][triggerIndex];
            float[] quarantineDurationOptions = triggeredQuarantineDuration.length == 0
                    ? new float[0] : triggeredQuarantineDuration[trigger_loc][triggerIndex];

            int quaratineDelay = 0;
            if (quarantineDelayOptions.length > 0) {
                quaratineDelay = getDelay(quarantineDelayOptions);
            }

            boolean isCaseIsolation = true;

            int inQuarntineUntilAge = (int) minRetestAge;
            
           

            if (quarantineDurationOptions.length > 0) {
                int index = 0;
                inQuarntineUntilAge = (int) quarantineDurationOptions[index];
                index += 2;
                if (quarantineDurationOptions.length > 0) {
                    float pDur = pop.getInfectionRNG().nextFloat();
                    while (index < quarantineDurationOptions.length
                            && pDur >= quarantineDurationOptions[index - 1]) {
                        inQuarntineUntilAge = (int) quarantineDurationOptions[index];
                        index += 2;
                    }
                }

                inQuarntineUntilAge += (int) rmp.getAge();
            }

            insertQuaratineInSchdule(rmp, quaratineDelay,
                    inQuarntineUntilAge, isCaseIsolation);

        }

    }

    private void insertQuaratineInSchdule(Person_Remote_MetaPopulation rmp,
            int quaratineDelay, int inQuarntineUntilAge,
            boolean isCaseIsolation) {
        ArrayList<Integer[]> inQ = getQuarantineScheduleEnt(quaratineDelay, rmp.getHomeLocation());

        Integer[] qPiplineEnt
                = new Integer[Population_Remote_MetaPopulation_COVID19.QUARANTINE_PIPELINE_ENT_LENGTH];
        qPiplineEnt[Population_Remote_MetaPopulation_COVID19.QUARANTINE_PIPELINE_ENT_PERSON_ID]
                = rmp.getId();
        qPiplineEnt[Population_Remote_MetaPopulation_COVID19.QUARANTINE_PIPELINE_ENT_IN_QUARANTINE_UNTIL]
                = inQuarntineUntilAge;
        qPiplineEnt[Population_Remote_MetaPopulation_COVID19.QUARANTINE_PIPELINE_ENT_CASE_ISOLATION]
                = isCaseIsolation ? 1 : -1;

        inQ.add(qPiplineEnt);
    }

    protected int detectResponseTrigger(int testTriggerIndex,
            float[] triggers,
            int[] testing_stat_cumul_location,
            int[] testing_stat_cumul_all) {
        int possibleTriggerIndex = testTriggerIndex;
        for (int i = possibleTriggerIndex + 1; i < triggers.length; i++) {
            if (triggers[i] <= 0) {
                if (pop.getGlobalTime() >= -triggers[i]) {
                    possibleTriggerIndex = i;
                }
            } else if (triggers[i] >= 1) {
                if (testing_stat_cumul_location[TEST_RES_STAT_POS] >= triggers[i]) {
                    possibleTriggerIndex = i;
                }
            } else {
                if (testing_stat_cumul_all[TEST_RES_STAT_POS] >= 1 / triggers[i]) {
                    possibleTriggerIndex = i;
                }
            }
        }
        if (testTriggerIndex != possibleTriggerIndex) {
            testTriggerIndex = possibleTriggerIndex;
        }
        return testTriggerIndex;
    }

}
