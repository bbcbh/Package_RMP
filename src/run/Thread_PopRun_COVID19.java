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
    public static final int THREAD_PARAM_TEST_TRIGGER = THREAD_PARAM_NON_HOUSEHOLD_CONTACT_DIST + 1;
    public static final int THREAD_PARAM_TRIGGERED_TEST_RATE = THREAD_PARAM_TEST_TRIGGER + 1;
    public static final int THREAD_PARAM_TRIGGERED_TEST_RESPONSE = THREAD_PARAM_TRIGGERED_TEST_RATE + 1;
    public static final int THREAD_PARAM_TRIGGERED_SYM_RESPONSE = THREAD_PARAM_TRIGGERED_TEST_RESPONSE + 1;
    public static final int THREAD_PARAM_TRIGGERED_HOUSEHOLD_TESTING = THREAD_PARAM_TRIGGERED_SYM_RESPONSE + 1;
    public static final int THREAD_PARAM_TRIGGERED_HOUSEHOLD_QUARANTINE = THREAD_PARAM_TRIGGERED_HOUSEHOLD_TESTING + 1;
    public static final int THREAD_PARAM_TRIGGERED_METAPOP_LOCKDOWN_SETTING = THREAD_PARAM_TRIGGERED_HOUSEHOLD_QUARANTINE + 1;
    public static final int THREAD_PARAM_TRIGGERED_TESTING_RESULT_DELAY = THREAD_PARAM_TRIGGERED_METAPOP_LOCKDOWN_SETTING + 1;
    public static final int THREAD_PARAM_TEST_SENSITIVITY = THREAD_PARAM_TRIGGERED_TESTING_RESULT_DELAY + 1;
    public static final int THREAD_PARAM_MAX_RESPONSE_BY_LOC = THREAD_PARAM_TEST_SENSITIVITY + 1;

    public static final int TEST_STAT_ALL = 0;
    public static final int TEST_STAT_POS = 1;
    public static final int TEST_STAT_LENGTH = 2;

    public static final int LOCKDOWN_NUM_INFECTED = 0;
    public static final int LOCKDOWN_NUM_POSITIVE = LOCKDOWN_NUM_INFECTED + 1;
    public static final int LOCKDOWN_DURATION = LOCKDOWN_NUM_POSITIVE + 1;

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
    // =  -(n-th level of contact test) if TEST_TYPE < 0

    public static final int MAX_TEST_NUM = 0;
    public static final int MAX_TEST_PERIOD = MAX_TEST_NUM + 1;
    public static final int MAX_TEST_QUEUE_SETTING = MAX_TEST_PERIOD + 1;

    public static final int MAX_TEST_QUEUE_TYPE_NONE = -1; // Default

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
        //  response_trigger >= 1 : Trigger when cumulative number of cases  >= response_trigger
        //  response trigger <1   : Trigger when numeber of infection case / number of test last day  >= response_trigger
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
        // Format: [loc][triggerIndex]{probability_current_household, probability_core_household, probility_non_core_household, prob_temp}       
        new double[][][]{},
        // THREAD_PARAM_TRIGGERED_HOUSEHOLD_QUARANTINE
        // Format: [loc][triggerIndex]
        // {probability_current_household, probability_core_household, probility_non_core_household, prob_temp, 
        //  stay_quarantine_until_current_household, stay_quarantine_until_core_household, 
        //  stay_quarantine_until_non_core_household, stay_quarantine_until_temp_household } 
        new double[][][]{},
        // THREAD_PARAM_TRIGGERED_METAPOP_LOCKDOWN_SETTING
        // Format:[loc]{number_of_infected, number_of_postive_test, how long (or -1 if indefinite)
        new int[][]{},
        // THREAD_PARAM_TRIGGERED_TESTING_RESULT_DELAY
        // Format: [loc][triggerIndex]{delay_testing_in_days}
        // Format: [loc][triggerIndex]{delay_testing_in days_min, delay_testing_in_days_max}
        // Format: [loc][triggerIndex]{delay_testing_in days_min, delay_testing_in_days_max, prob_next_level}
        new float[][][]{},
        // THREAD_PARAM_TEST_SENSITIVITY
        // Format: {sensitivity_all stage}
        // Format: {sensitivity_infected, sensitivity_infectious, sensitivity_symptomatic, 
        //          sensitivity_latant, sensitivity_incubation, sensitivity_post_infectious}
        new double[]{},
        // THREAD_PARAM_MAX_TEST_BY_LOC
        // Format: [loc][max_num_test, period]
        // Format: [loc][max_num_test, period, queue_method]
        new int[][]{},};

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

        int[][] testing_stat_cumul = new int[TEST_STAT_LENGTH][popSize.length]; // By location
        int[] testing_stat_cumul_all = new int[TEST_STAT_LENGTH];

        float[][] triggers = (float[][]) getThreadParam()[THREAD_PARAM_TEST_TRIGGER];
        float[][][] triggeredTestRate = (float[][][]) getThreadParam()[THREAD_PARAM_TRIGGERED_TEST_RATE];
        double[][][][] triggeredTestResponse = (double[][][][]) getThreadParam()[THREAD_PARAM_TRIGGERED_TEST_RESPONSE];
        double[][][][] triggeredSymResponse = (double[][][][]) getThreadParam()[THREAD_PARAM_TRIGGERED_SYM_RESPONSE];
        double[][][] triggetedHouseholdTestRate = (double[][][]) getThreadParam()[THREAD_PARAM_TRIGGERED_HOUSEHOLD_TESTING];
        double[][][] triggetedHouseholdQuarantineRate = (double[][][]) getThreadParam()[THREAD_PARAM_TRIGGERED_HOUSEHOLD_QUARANTINE];
        int[][] triggeredLockdownSetting = (int[][]) getThreadParam()[THREAD_PARAM_TRIGGERED_METAPOP_LOCKDOWN_SETTING];
        float[][][] triggeredTestResultDelay = (float[][][]) getThreadParam()[THREAD_PARAM_TRIGGERED_TESTING_RESULT_DELAY];

        StringBuilder triggerText = new StringBuilder();

        pop.printCSVOutputHeader(outputCSV);
        int[][] numStat = pop.generateInfectionStat();
        pop.printCSVOutputEntry(outputCSV, numStat);

        // Print testingCSV header 
        testingCSV.print("Time");
        for (int p = 0; p < TEST_STAT_LENGTH; p++) {
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
        int[][] rolling_response_count_by_loc = null;
        int[] rollingSum = new int[popSize.length];

        List<LinkedList<Object[]>> responseQueueList = new ArrayList<>();
        for (int i = 0; i < popSize.length; i++) {
            responseQueueList.add(new LinkedList<>());
        }

        if (getThreadParam()[THREAD_PARAM_MAX_RESPONSE_BY_LOC] != null) {
            int[][] maxTestByLoc = (int[][]) getThreadParam()[THREAD_PARAM_MAX_RESPONSE_BY_LOC];
            if (maxTestByLoc.length > 0) {
                rolling_response_count_by_loc = new int[popSize.length][];
                for (int loc = 0; loc < popSize.length; loc++) {
                    rolling_response_count_by_loc[loc] = new int[maxTestByLoc[loc][MAX_TEST_PERIOD]];
                }
            }
        }

        printCSVTestEntry(testingCSV, testing_stat_cumul, responseQueueList);

        for (int sn = 0; sn < numSnap; sn++) {
            for (int sf = 0; sf < snapFreq; sf++) {

                int[] test_stat_today = new int[2];

                for (AbstractIndividualInterface p : pop.getPop()) {
                    Person_Remote_MetaPopulation rmp = (Person_Remote_MetaPopulation) p;
                    int trigger_loc = pop.getCurrentLocation(p);

                    int testTriggerIndex = testTriggerIndex_by_loc[trigger_loc];

                    Integer testNotUntilAge = pop.getMinAgeForNextTest().get(p.getId());

                    boolean canBeTested = testNotUntilAge == null
                            || p.getAge() > testNotUntilAge;

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
                                ArrayList<Object[]> testSchEnt = getTestScheuduleEnt(symTestDelay, trigger_loc);
                                testSchEnt.add(new Object[]{rmp, rmp.getAge(), TEST_TYPE_SYM});
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
                                    ArrayList<Object[]> testSchEnt = getTestScheuduleEnt(srnTestDelay, trigger_loc);
                                    testSchEnt.add(new Object[]{rmp, rmp.getAge(), TEST_TYPE_SCR});

                                }
                            }
                        }
                    }

                }

                // Lock down
                int[][] metapopStat = (int[][]) pop.getFields()[Population_Remote_MetaPopulation_COVID19.FIELDS_REMOTE_METAPOP_COVID19_META_POP_LOCKDOWN_SETTING];

                if (triggeredLockdownSetting.length > 0) {
                    for (int loc = 0; loc < popSize.length; loc++) {
                        // Only lockdown once
                        if (metapopStat[loc][Population_Remote_MetaPopulation_COVID19.META_POP_STAT_LOCKDOWN_START] == -1) {
                            boolean inLockdown = triggeredLockdownSetting[loc][LOCKDOWN_NUM_INFECTED] >= 0
                                    && numStat[Population_Remote_MetaPopulation_COVID19.NUM_STAT_NUM_INFECTED][loc]
                                    >= triggeredLockdownSetting[loc][LOCKDOWN_NUM_INFECTED];

                            inLockdown |= triggeredLockdownSetting[loc][LOCKDOWN_NUM_POSITIVE] > 0
                                    && testing_stat_cumul[TEST_STAT_POS][loc] >= triggeredLockdownSetting[loc][LOCKDOWN_NUM_POSITIVE];

                            if (inLockdown) {
                                metapopStat[loc][Population_Remote_MetaPopulation_COVID19.META_POP_STAT_LOCKDOWN_START] = pop.getGlobalTime();
                                if (triggeredLockdownSetting[loc][LOCKDOWN_DURATION] > 0) {
                                    metapopStat[loc][Population_Remote_MetaPopulation_COVID19.META_POP_STAT_LOCKDOWN_END]
                                            = pop.getGlobalTime() + triggeredLockdownSetting[loc][LOCKDOWN_DURATION];
                                }
                            }
                        }
                    }
                }

                for (int loc = 0; loc < popSize.length; loc++) {

                    List<Integer> testKey = List.of(pop.getGlobalTime(), loc);

                    // Resposne to test
                    ArrayList<Object[]> testSchArr = pop.getTestScheduelInPipeline().remove(testKey);
                    if (testSchArr != null) {

                        // Reset rolling limit to current timepoint                  
                        int limit = testSchArr.size(); // Default                    
                        int queueType = MAX_TEST_QUEUE_TYPE_NONE;

                        if (rolling_response_count_by_loc != null) {
                            int[] maxResponse = ((int[][]) getThreadParam()[THREAD_PARAM_MAX_RESPONSE_BY_LOC])[loc];
                            int timeIndex = pop.getGlobalTime() % rolling_response_count_by_loc[loc].length;
                            rollingSum[loc] -= rolling_response_count_by_loc[loc][timeIndex];
                            rolling_response_count_by_loc[loc][timeIndex] = 0;
                            limit = Math.max(0, maxResponse[MAX_TEST_NUM] - rollingSum[loc]);
                            if (MAX_TEST_QUEUE_SETTING < maxResponse.length) {
                                queueType = maxResponse[MAX_TEST_QUEUE_SETTING];
                            }
                        }

                        int testCount = 0;

                        for (Object[] ent : testSchArr) {
                            if (testCount >= limit) {
                                // Handle exceed test scheduled
                                switch (queueType) {
                                    default:
                                        // No more test available - test abandoned 
                                        break;                                                                         
                                }

                            } else {

                                Person_Remote_MetaPopulation rmp
                                        = (Person_Remote_MetaPopulation) ent[Population_Remote_MetaPopulation_COVID19.TEST_SCHEDULE_PIPELINE_ENT_PERSON_TESTED];

                                int testType = (Integer) ent[Population_Remote_MetaPopulation_COVID19.TEST_SCHEDULE_PIPELINE_ENT_TEST_TYPE];

                                testing_stat_cumul[TEST_STAT_ALL][loc]++;
                                testing_stat_cumul_all[TEST_STAT_ALL]++;
                                test_stat_today[TEST_STAT_ALL]++;

                                float[] delayOption = triggeredTestResultDelay.length > 0
                                        ? triggeredTestResultDelay[loc][testTriggerIndex_by_loc[loc]] : new float[]{};

                                boolean testPositive;

                                if (delayOption.length == 0) { // Special case for instant test and results for all test
                                    testPositive = covid19.isInfected(rmp);
                                    if (testPositive) {
                                        
                                        double[][] testRes;
                                        if(triggeredTestResponse.length ==0){
                                            testRes = new double[0][];
                                        }else{
                                            testRes = triggeredTestResponse[loc][testTriggerIndex_by_loc[loc]];
                                        }
                                        
                                        setTriggeredIndexCaseTestResponse(rmp, covid19, testRes);
                                    }
                                } else {
                                    testPositive = insertTestingResult(rmp, covid19, testType, delayOption);
                                }

                                if (testPositive) {
                                    testing_stat_cumul[TEST_STAT_POS][loc]++;
                                    testing_stat_cumul_all[TEST_STAT_POS]++;
                                    test_stat_today[TEST_STAT_POS]++;

                                }

                                if (rolling_response_count_by_loc != null) {
                                    int timeIndex = pop.getGlobalTime() % rolling_response_count_by_loc[loc].length;
                                    rolling_response_count_by_loc[loc][timeIndex]++;
                                    rollingSum[loc]++;
                                }

                                testCount++;
                            }

                        }

                    }

                    // Response to positive test results  
                    ArrayList<Object[]> testOutcomeArr = pop.getTestOutcomeInPipeline().remove(testKey);
                    LinkedList<Object[]> responseQueue = responseQueueList.get(loc);

                    // Insert response from pipeline to end of response queue
                    if (testOutcomeArr != null) {
                        for (Object[] ent : testOutcomeArr) {
                            responseQueue.add(ent);
                        }
                    }

                    for (Object[] ent : responseQueue) {

                        Person_Remote_MetaPopulation rmp
                                = (Person_Remote_MetaPopulation) ent[Population_Remote_MetaPopulation_COVID19.TEST_OUTCOME_PIPELINE_ENT_PERSON_TESTED];

                        if ((Boolean) ent[Population_Remote_MetaPopulation_COVID19.TEST_OUTCOME_PIPELINE_ENT_TEST_RESULT]) {

                            int trigger_loc = pop.getCurrentLocation(rmp);
                            if (trigger_loc < 0) {
                                trigger_loc = rmp.getHomeLocation();
                            }

                            // Assume test 100% accurate
                            positiveTestResponse(rmp, covid19,
                                    testTriggerIndex_by_loc[trigger_loc],
                                    (Integer) ent[Population_Remote_MetaPopulation_COVID19.TEST_OUTCOME_PIPELINE_ENT_TEST_TYPE],
                                    triggeredTestResponse.length == 0 ? new double[0][][] : triggeredTestResponse[trigger_loc],
                                    triggetedHouseholdTestRate.length == 0 ? new double[0][] : triggetedHouseholdTestRate[trigger_loc],
                                    triggetedHouseholdQuarantineRate.length == 0 ? new double[0][] : triggetedHouseholdQuarantineRate[trigger_loc],
                                    triggeredTestResultDelay.length == 0 ? new float[0][] : triggeredTestResultDelay[trigger_loc],
                                    testing_stat_cumul, testing_stat_cumul_all, test_stat_today);

                        }

                    }
                    if (responseQueue.size() > 0) {
                        responseQueue.clear();
                    }
                }

                for (int loc = 0; loc < popSize.length; loc++) {
                    // Determine trigger 
                    if (testTriggerIndex_by_loc[loc] + 1 < triggers[loc].length) {

                        int testTriggerIndex_org = testTriggerIndex_by_loc[loc];
                        testTriggerIndex_by_loc[loc] = detectResponseTrigger(
                                testTriggerIndex_by_loc[loc],
                                triggers[loc],
                                testing_stat_cumul_all,
                                test_stat_today);

                        if (testTriggerIndex_by_loc[loc] != testTriggerIndex_org) {
                            // New trigger
                            triggerText.append(pop.getGlobalTime());
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
            printCSVTestEntry(testingCSV, testing_stat_cumul, responseQueueList);

        }

        if (triggerText.toString().length() > 0) {
            testingCSV.println();
            testingCSV.println(triggerText.toString());
        }

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

    public ArrayList<Object[]> getTestScheuduleEnt(int testDelay, int testLoc) {
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
            resultDelay = (int) Math.abs(resultDelayOptions[DELAY_OPTIONS_MIN]);
            if (resultDelayOptions.length > DELAY_OPTIONS_MAX && resultDelayOptions[DELAY_OPTIONS_MAX] > resultDelay) {
                resultDelay += pop.getInfectionRNG().nextInt((int) (resultDelayOptions[DELAY_OPTIONS_MAX] - resultDelay));
            }
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

    protected void positiveTestResponse(Person_Remote_MetaPopulation rmp,
            COVID19_Remote_Infection covid19,
            int testTriggerIndex, int srcTestType,
            double[][][] triggeredTestResponse,
            double[][] triggetedHouseholdTestRate,
            double[][] triggetedHouseholdQuarantineRate,
            float[][] triggeredTestResultDelay,
            int[][] testing_stat_cumul,
            int[] testing_stat_cumul_all,
            int[] test_stat_today) {

        // Single test response
        if (testTriggerIndex < triggeredTestResponse.length) {
            setTriggeredIndexCaseTestResponse(rmp, covid19, triggeredTestResponse[testTriggerIndex]);
        }

        // Household contract tracing 
        if (testTriggerIndex < triggetedHouseholdTestRate.length
                || testTriggerIndex < triggetedHouseholdQuarantineRate.length) {

            AbstractIndividualInterface[][] fromCurrentHousehold = pop.currentlyInSameHouseholdAsByType(rmp);
            double[] householdTestRateLoc = null;
            double[] householdQuarantineRate = null;

            // Household test
            if (testTriggerIndex < triggetedHouseholdTestRate.length) {
                householdTestRateLoc = triggetedHouseholdTestRate[testTriggerIndex];
            }
            // Household quaratine
            if (testTriggerIndex < triggetedHouseholdQuarantineRate.length) {
                householdQuarantineRate = triggetedHouseholdQuarantineRate[testTriggerIndex];
            }

            // probability_current_household, probability_core_household, probility_non_core_household, prob temp            
            for (int type = 0; type < fromCurrentHousehold.length; type++) {
                for (AbstractIndividualInterface candidate : fromCurrentHousehold[type]) {

                    // Household test
                    if (householdTestRateLoc != null) {

                        Integer testNotUntilAge = pop.getMinAgeForNextTest().get(candidate.getId());
                        Integer inQuarantine = pop.inQuarantineUntil(candidate);

                        boolean canBeTested = (testNotUntilAge == null || candidate.getAge() > testNotUntilAge)
                                && inQuarantine == null;

                        if (canBeTested) {
                            boolean contactTesting = householdTestRateLoc[type] >= 1;

                            if (!contactTesting && householdTestRateLoc[type] > 0) {
                                contactTesting = pop.getInfectionRNG().nextDouble()
                                        < householdTestRateLoc[type];
                            }

                            if (contactTesting) {
                                int trigger_loc = pop.getCurrentLocation(candidate);
                                int contactTestDelay = 0;
                                ArrayList<Object[]> testSchEnt = getTestScheuduleEnt(contactTestDelay, trigger_loc);
                                testSchEnt.add(new Object[]{candidate, candidate.getAge(), Math.min(srcTestType, 0) - 1});

                            }
                        }
                    }
                    // Household quaratine
                    if (householdQuarantineRate != null) {
                        boolean quarantine = householdQuarantineRate[type] >= 1;
                        if (!quarantine && householdQuarantineRate[type] > 0) {
                            quarantine = pop.getInfectionRNG().nextDouble() < householdQuarantineRate[type];
                        }
                        if (quarantine) {
                            HashMap<Integer, Integer> qMap
                                    = ((HashMap<Integer, Integer>) pop.getFields()[Population_Remote_MetaPopulation_COVID19.FIELDS_REMOTE_METAPOP_COVID19_CURRENTLY_IN_QUARANTINE]);
                            qMap.put(candidate.getId(), ((int) (candidate.getAge() + householdQuarantineRate[type + fromCurrentHousehold.length])));

                        }
                    }
                }

            }

        }

    }

    protected void setTriggeredIndexCaseTestResponse(Person_Remote_MetaPopulation rmp,
            COVID19_Remote_Infection covid19, double[][] default_test_resp) {
        // Format: [triggerIndex]{ {valid_for_days}, {probability_for_k=0}, {probability_for_k=1} ...}

        double[] test_resp = pop.getTestResponse().get(rmp.getId());

        if (test_resp == null) {
            test_resp = new double[Population_Remote_MetaPopulation_COVID19.TEST_RESPONSE_TOTAL];
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

            // Effectively in quarantine
            HashMap<Integer, Integer> qMap
                    = ((HashMap<Integer, Integer>) pop.getFields()[Population_Remote_MetaPopulation_COVID19.FIELDS_REMOTE_METAPOP_COVID19_CURRENTLY_IN_QUARANTINE]);
            qMap.put(rmp.getId(), minRetestAge);
        }

    }

    protected int detectResponseTrigger(int testTriggerIndex,
            float[] triggers,
            int[] testing_stat_cumul_all,
            int[] test_stat_today) {
        int possibleTriggerIndex = testTriggerIndex;
        for (int i = possibleTriggerIndex + 1; i < triggers.length; i++) {
            if (triggers[i] <= 0) {
                if (pop.getGlobalTime() >= -triggers[i]) {
                    possibleTriggerIndex = i;
                }
            } else if (triggers[i] >= 1) {
                if (testing_stat_cumul_all[TEST_STAT_POS] > triggers[i]) {
                    possibleTriggerIndex = i;
                }
            } else {
                if (((float) test_stat_today[TEST_STAT_POS]) / test_stat_today[TEST_STAT_ALL] > triggers[i]) {
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
