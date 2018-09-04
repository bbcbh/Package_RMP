package run;

import infection.AbstractInfection;
import infection.ChlamydiaInfection;
import infection.GonorrhoeaInfection;
import infection.TreatableInfectionInterface;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import person.AbstractIndividualInterface;
import person.MoveablePersonInterface;
import person.Person_Remote_MetaPopulation;
import population.AbstractPopulation;
import population.Population_Remote_MetaPopulation;
import random.MersenneTwisterRandomGenerator;
import random.RandomGenerator;
import util.ArrayUtilsRandomGenerator;
import util.Default_Remote_MetaPopulation_AgeGrp_Classifier;
import util.FileZipper;
import util.PersonClassifier;
import util.PropValUtils;

/**
 *
 * @author Ben Hui
 * @version 20180904
 *
 * <pre>
 * History:
 *
 * 20180629
 *  - Reimplementation of testing and treament schedule, inclusion of delay.
 * 20180612
 *  - Alterative screening rate from RG
 *  - Remove null reference for default parameter value so getClass() method is always valid
 *  - Addtion of cumulative incidence
 * 20180614
 *  - Change the default treatment delay
 * 20180622
 *  - Implementation of alterative treatement delay format of various length
 * 20180625
 *  - Addition of cumulative notification field
 * 20180628
 *  - Add screening by probability option
 * 20180629
 *  - Change cumulative count to classifier as defined by testing classifier
 *  - Addition of test sensitivity
 * 20180712
 *  - Change the option of treatment delay as location specific
 * 20180713
 *  - Additional parameter options for adjusting parameter for Population_Remote_MetaPopulation
 * 20180716
 *  - Change notification to infectious only
 * 20180717
 *  - Change notficiation by infection state
 * 20180718
 *  - Add method to update population's field value
 * 20180801
 *  - Add default testing classifier
 *  - Changing the definition of PARAM_INDEX_TESTING_RATE_BY_HOME_LOC as boolean
 * 20180816
 *  - Change intro at option so infection is only cleared at start of simulation if number of infection is different
 * 20180817
 *  - Change testing rate definition to include non-annual screening. E.g. if 50% of population are screened biannually
 *    then the screen rate will be express as 2.50. Noted that for screening by probability it will check for all rate
 *    as oppose to only checking the first one
 * 20180821
 *  - Debug: Set current location as home location if current location is not found
 * 20180828
 *  - Change testing rate definition to include the option of retesting of the same scheduled person, and
 *    the implementation of optional screening setting parameter
 * 20180831
 *  - Skip generation of notification and incident file if outputFilePath is not set (e.g. under optimisation)
 *  - Implementation of multiple screening / mass screening
 * 20180904
 *  - Addition of fixed number of test count under multiple screening option
 *
 * </pre>
 */
public class Thread_PopRun implements Runnable {

    private final File outputFilePath;
    private final File importFilePath;
    private final int simId;
    private final int numSteps;
    private int outputFreq = (5 * AbstractIndividualInterface.ONE_YEAR_INT);

    private Population_Remote_MetaPopulation pop = null;
    private PrintWriter outputPri = null; //new PrintWriter(System.out);
    private boolean closeOutputOnFinish = false;

    public static final int PARAM_INDEX_INFECTIONS = 0;
    public static final int PARAM_INDEX_INTRO_CLASSIFIERS = PARAM_INDEX_INFECTIONS + 1;
    public static final int PARAM_INDEX_INTRO_PREVALENCE = PARAM_INDEX_INTRO_CLASSIFIERS + 1;
    public static final int PARAM_INDEX_INTRO_AT = PARAM_INDEX_INTRO_PREVALENCE + 1;
    public static final int PARAM_INDEX_INTRO_PERIODICITY = PARAM_INDEX_INTRO_AT + 1;
    public static final int PARAM_INDEX_TESTING_CLASSIFIER = PARAM_INDEX_INTRO_PERIODICITY + 1;
    public static final int PARAM_INDEX_TESTING_RATE_BY_CLASSIFIER = PARAM_INDEX_TESTING_CLASSIFIER + 1;
    public static final int PARAM_INDEX_TESTING_RATE_BY_HOME_LOC = PARAM_INDEX_TESTING_RATE_BY_CLASSIFIER + 1;
    public static final int PARAM_INDEX_TESTING_TREATMENT_DELAY_BY_LOC = PARAM_INDEX_TESTING_RATE_BY_HOME_LOC + 1;
    public static final int PARAM_INDEX_TESTING_SENSITIVITY = PARAM_INDEX_TESTING_TREATMENT_DELAY_BY_LOC + 1;
    public static final int PARAM_TOTAL = PARAM_INDEX_TESTING_SENSITIVITY + 1;

    public static final int TESTING_OPTION_USE_PROPORTION_TEST_COVERAGE = 0;
    public static final int TESTING_OPTION_FIX_TEST_SCHEDULE = TESTING_OPTION_USE_PROPORTION_TEST_COVERAGE + 1;

    public static final int TESTING_TIMERANGE_START = 0;
    public static final int TESTING_TIMERANGE_DURATION = TESTING_TIMERANGE_START + 1;
    public static final int TESTING_TIMERANGE_PERIOD = TESTING_TIMERANGE_DURATION + 1;
    public static final int TESTING_TIMERANGE_MAX_COUNT = TESTING_TIMERANGE_PERIOD + 1;
    public static final int TESTING_TIMERANGE_LENGTH = TESTING_TIMERANGE_MAX_COUNT + 1;

    protected int[] cumulativeIncident;
    protected int[][][] cumulativeTestAndNotification;   // cumulativeTestAndNotification[infectionId][classId][1+infStatus]

    public static final String FILE_PREFIX_INCIDENCE = "incident_S";
    public static final String FILE_PREFIX_TEST_AND_NOTIFICATION = "test_and_notification_S";

    protected Object[] inputParam = new Object[]{
        // 1: PARAM_INDEX_INFECTIONS
        new AbstractInfection[]{new ChlamydiaInfection(null), new GonorrhoeaInfection(null)},
        // 2: PARAM_INDEX_INTRO_CLASSIFIERS
        new PersonClassifier[]{new DEFAULT_PREVAL_CLASSIFIER(), new DEFAULT_PREVAL_CLASSIFIER()},
        // 3: PARAM_INDEX_INTRO_PREVALENCE
        // From STRIVE 
        new float[][]{
            new float[]{0.118f, 0.104f, 0.074f, 0.046f, 0.174f, 0.082f, 0.060f, 0.035f},
            new float[]{0.137f, 0.065f, 0.040f, 0.041f, 0.135f, 0.076f, 0.028f, 0.043f},},
        // 4: PARAM_INDEX_INTRO_AT
        new int[]{
            360 * 50 + 1,
            360 * 50 + 1,},
        // 5: PARAM_INDEX_INTRO_PERIODICITY
        new int[]{
            -1,
            -1,},
        // 6: PARAM_INDEX_TESTING_CLASSIFIER
        // type: PersonClassifier
        new DEFAULT_TESTING_CLASSIFIER(),
        // 7: PARAM_INDEX_TESTING_RATE_BY_CLASSIFIER
        // Regional adjustment from GOANNA, p.43  
        // Remote from STRIVE (FNQ, LC's slides)
        // type: float[]           
        // From RG
        // Alterative format
        //    - if the first rate is negative use proprotion testing rate instead (for backward compatbility)
        //    - if length of test rate > number of class in testing classifiers
        //       [ RATE_BY_CLASSIFIER_DEFAULT, test_setting_default 
        //         start_time_1, duration_1, periodcity_1, max_test_count_1 (-1 for inf)
        //         RATE_BY_CLASSIFIER_SET_1, test_setting_1, ...]                 
        //     
        //      see  TESTING_OPTION_* for test_setting

        new float[]{
            // Regional        
            // Male
            0.130f * (0.44f / 0.48f),
            0.140f * (0.44f / 0.48f),
            0.110f * (0.44f / 0.48f),
            0.110f * (0.44f / 0.48f),
            // Female            
            0.260f * (0.44f / 0.48f),
            0.240f * (0.44f / 0.48f),
            0.230f * (0.44f / 0.48f),
            0.190f * (0.44f / 0.48f),
            // Remote #1 
            // Male            
            0.130f,
            0.140f,
            0.110f,
            0.110f,
            // Female            
            0.260f,
            0.240f,
            0.230f,
            0.190f,
            // Remote #2 
            // Male            
            0.130f,
            0.140f,
            0.110f,
            0.110f,
            // Female            
            0.260f,
            0.240f,
            0.230f,
            0.190f,
            // Remote #3 
            // Male            
            0.130f,
            0.140f,
            0.110f,
            0.110f,
            // Female            
            0.260f,
            0.240f,
            0.230f,
            0.190f,
            // Remote #4                 
            // Male            
            0.130f,
            0.140f,
            0.110f,
            0.110f,
            // Female            
            0.260f,
            0.240f,
            0.230f,
            0.190f,},
        // 8: PARAM_INDEX_TESTING_RATE_BY_HOME_LOC        
        true,
        // 9: PARAM_INDEX_TESTING_TREATMENT_DELAY_BY_LOC
        // [min, range]
        // Alterative format
        // [min, cumul_liklihood_1, cumul_delay_range_1, cumul_liklihood_2, cumul_delay_range_2, .... total_liklihood]        
        new int[][]{
            new int[]{0, 90, 7, 100},
            new int[]{0, 111, 0, 122, 2, 191, 5, 327, 113, 405},
            new int[]{0, 111, 0, 122, 2, 191, 5, 327, 113, 405},
            new int[]{0, 111, 0, 122, 2, 191, 5, 327, 113, 405},
            new int[]{0, 111, 0, 122, 2, 191, 5, 327, 113, 405},},
        // 10: PARAM_INDEX_TESTING_SENSITIVITY
        0.98f

    };

    public Thread_PopRun(File outputPath, File importPath, int simId, int numSteps) {
        this.outputFilePath = outputPath;
        this.importFilePath = importPath;
        this.simId = simId;
        this.numSteps = numSteps;
    }

    public void setOutputFreq(int outputFreq) {
        this.outputFreq = outputFreq;
    }

    public int getOutputFreq() {
        return this.outputFreq;
    }

    private class DEFAULT_TESTING_CLASSIFIER implements PersonClassifier {

        int numLoc = 5;
        int numGender = 2;
        int numAgeGrp = 4;

        public DEFAULT_TESTING_CLASSIFIER() {
        }

        public DEFAULT_TESTING_CLASSIFIER(int numLoc) {
            this.numLoc = numLoc;
        }

        @Override
        public int classifyPerson(AbstractIndividualInterface p) {
            double a = p.getAge();
            // Age
            int res = -1;
            if (a >= 16 * AbstractIndividualInterface.ONE_YEAR_INT) {
                res = 0;
                if (a >= 20 * AbstractIndividualInterface.ONE_YEAR_INT) {
                    res++;
                }
                if (a >= 25 * AbstractIndividualInterface.ONE_YEAR_INT) {
                    res++;
                }
                if (a >= 30 * AbstractIndividualInterface.ONE_YEAR_INT) {
                    res++;
                }
                if (a >= 35 * AbstractIndividualInterface.ONE_YEAR_INT) {
                    res = -1;
                }
            }
            if (res >= 0) {
                // Gender
                if (!p.isMale()) {
                    res += numAgeGrp;
                }
                // Location
                int loc = ((MoveablePersonInterface) p).getHomeLocation();

                // Use current location instead for testing purpose
                if (!(Boolean) getInputParam()[PARAM_INDEX_TESTING_RATE_BY_HOME_LOC]) {
                    if (pop != null) {
                        loc = pop.getCurrentLocation(p);
                    }
                }
                res += loc * numGender * numAgeGrp;
            }

            return res;
        }

        @Override
        public int numClass() {
            return numLoc * numGender * numAgeGrp; // 5 loc * 2 gender * 4 age grp
        }

    }

    private class DEFAULT_PREVAL_CLASSIFIER implements PersonClassifier {

        PersonClassifier ageGrpClassifier = new Default_Remote_MetaPopulation_AgeGrp_Classifier();

        @Override
        public int classifyPerson(AbstractIndividualInterface p) {
            int aI = ageGrpClassifier.classifyPerson(p);
            return p.isMale() ? aI : (aI + ageGrpClassifier.numClass());

        }

        @Override
        public int numClass() {
            return 2 * ageGrpClassifier.numClass();
        }

    }

    public Object[] getInputParam() {
        return inputParam;
    }

    public PrintWriter getOutputPri() {
        return outputPri;
    }

    public void setOutputPri(PrintWriter outputPri, boolean closeOutputOnFinish) {
        this.closeOutputOnFinish = closeOutputOnFinish;
        this.outputPri = outputPri;
    }

    public AbstractPopulation getPop() {
        return pop;
    }

    public int getSimId() {
        return simId;
    }

    @Override
    public void run() {
        try {
            if (pop == null) {
                importPop();
            }
            if (pop != null) {

                AbstractInfection[] modelledInfections = (AbstractInfection[]) getInputParam()[PARAM_INDEX_INFECTIONS];
                int[] introAt = (int[]) getInputParam()[PARAM_INDEX_INTRO_AT];
                int[] introPeriodicity = (int[]) getInputParam()[PARAM_INDEX_INTRO_PERIODICITY];
                int offset = pop.getGlobalTime();

                // Testing 
                random.RandomGenerator testRNG = new MersenneTwisterRandomGenerator(pop.getSeed());

                int[][] testing_numPerDay = null;
                int[] testing_pt = null;
                AbstractIndividualInterface[][] testing_person = null;

                // Treatment
                HashMap<Integer, int[][]> treatmentSchdule = new HashMap();

                long tic = System.currentTimeMillis();
                pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_INFECTION_LIST] = modelledInfections;
                pop.updateInfectionList(modelledInfections);

                AbstractIndividualInterface[] allPerson = pop.getPop();

                for (AbstractIndividualInterface person : allPerson) {
                    if (modelledInfections.length != person.getInfectionStatus().length) {
                        Person_Remote_MetaPopulation prm = (Person_Remote_MetaPopulation) person;
                        prm.setNumberOfInfections(modelledInfections.length);
                    }
                }

                int numPop = ((int[]) pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_POP_SIZE]).length;

                getInputParam()[PARAM_INDEX_TESTING_CLASSIFIER] = new DEFAULT_TESTING_CLASSIFIER(numPop);

                PersonClassifier testByClassifier = (PersonClassifier) getInputParam()[PARAM_INDEX_TESTING_CLASSIFIER];
                PersonClassifier incidenceClassifier, notificationClassifier;

                if (testByClassifier == null) {
                    incidenceClassifier = new PersonClassifier() {
                        @Override
                        public int classifyPerson(AbstractIndividualInterface p) {
                            return p.isMale() ? 0 : 1;
                        }

                        @Override
                        public int numClass() {
                            return 2;
                        }
                    };
                    notificationClassifier = new PersonClassifier() {
                        @Override
                        public int classifyPerson(AbstractIndividualInterface p) {
                            return p.isMale() ? 0 : 1;
                        }

                        @Override
                        public int numClass() {
                            return 2;
                        }
                    };

                } else {
                    incidenceClassifier = testByClassifier;
                    notificationClassifier = testByClassifier;
                }

                cumulativeIncident = new int[pop.getInfList().length * incidenceClassifier.numClass()];
                cumulativeTestAndNotification = new int[pop.getInfList().length][][];
                for (int i = 0; i < cumulativeTestAndNotification.length; i++) {
                    cumulativeTestAndNotification[i] = new int[notificationClassifier.numClass()][pop.getInfList()[i].getNumState() + 1];
                }

                // [RATE_BY_CLASSIFIER_DEFAULT, test_setting_default 
                //         start_time_1, duration_1, periodcity_1, max_count_1
                //         RATE_BY_CLASSIFIER_SET_1, test_setting_1, ...] 
                int numberOfTestRateOptions = 0;
                int numberOfTestRateIndex = 0;

                while (numberOfTestRateIndex < ((float[]) inputParam[PARAM_INDEX_TESTING_RATE_BY_CLASSIFIER]).length) {
                    numberOfTestRateOptions++;
                    numberOfTestRateIndex += testByClassifier.numClass() + 1;    // RATE_BY_CLASSIFIER_SET_n, test_setting_n                
                    numberOfTestRateIndex += TESTING_TIMERANGE_LENGTH;                                  // start_time_n+1, duration_n+1, periodcity_n+1,
                }

                boolean[] testing_use_daily_rate = new boolean[numberOfTestRateOptions];
                boolean[] testing_same_targetTest = new boolean[numberOfTestRateOptions];

                int[] testing_schedule_count = new int[numberOfTestRateOptions];              
                int[] testing_schedule_freq = new int[numberOfTestRateOptions];

                float[][] testing_rate_by_classifier = new float[numberOfTestRateOptions][];
                float[][] testing_set_time_range = new float[numberOfTestRateOptions][];

                testing_pt = new int[numberOfTestRateOptions];
                testing_person = new AbstractIndividualInterface[numberOfTestRateOptions][];
                testing_numPerDay = new int[numberOfTestRateOptions][];

                int testing_rate_set_index = 0;

                for (int testing_set_num = 0; testing_set_num < numberOfTestRateOptions; testing_set_num++) {

                    testing_rate_by_classifier[testing_set_num] = Arrays.copyOfRange(
                            (float[]) inputParam[PARAM_INDEX_TESTING_RATE_BY_CLASSIFIER],
                            testing_rate_set_index, testing_rate_set_index + testByClassifier.numClass() + 1);

                    int testSetting = testing_rate_by_classifier[testing_set_num].length > testByClassifier.numClass()
                            ? (int) testing_rate_by_classifier[testing_set_num][testByClassifier.numClass()] : 0;
                    testing_use_daily_rate[testing_set_num]
                            = (testSetting & (1 << TESTING_OPTION_USE_PROPORTION_TEST_COVERAGE)) > 0;
                    testing_same_targetTest[testing_set_num] = (testSetting & (1 << TESTING_OPTION_FIX_TEST_SCHEDULE)) > 0;

                    // If testing_schedule_freq < 1 then it will be annual 
                    testing_schedule_freq[testing_set_num] = Math.max(Math.abs((int) testing_rate_by_classifier[testing_set_num][0]), 1);

                    if (testing_set_num == 0) {
                        testing_set_time_range[testing_set_num]                                 
                                = new float[]{offset, AbstractIndividualInterface.ONE_YEAR_INT / testing_schedule_freq[testing_set_num], 
                                    AbstractIndividualInterface.ONE_YEAR_INT / testing_schedule_freq[testing_set_num],  -1}; // Default background                          
                       
                    } else {
                        testing_set_time_range[testing_set_num] = Arrays.copyOfRange(
                                (float[]) inputParam[PARAM_INDEX_TESTING_RATE_BY_CLASSIFIER],
                                testing_rate_set_index - TESTING_TIMERANGE_LENGTH, testing_rate_set_index);
                        // start_time_n+1, duration_n+1, periodcity_n+1, testScheduleCount_n+1                           

                    }

                    testing_rate_set_index += testByClassifier.numClass() + 1;
                    testing_rate_set_index += TESTING_TIMERANGE_LENGTH;

                }

                boolean[] testing_in_timestep = new boolean[numberOfTestRateOptions];

                for (int t = 0; t < numSteps; t++) {

                    for (int testing_set_num = 0; testing_set_num < testing_rate_by_classifier.length; testing_set_num++) {
                        float[] time_range = testing_set_time_range[testing_set_num];
                        testing_in_timestep[testing_set_num] = pop.getGlobalTime() >= time_range[TESTING_TIMERANGE_START]
                                && (time_range[TESTING_TIMERANGE_MAX_COUNT] < 0 // Inf count 
                                    || testing_schedule_count[testing_set_num] < time_range[TESTING_TIMERANGE_MAX_COUNT] // fix count testing 
                                );

                        if (testing_in_timestep[testing_set_num]) {
                            int offset_time = pop.getGlobalTime() - (int) time_range[TESTING_TIMERANGE_START];
                            if (!testing_use_daily_rate[testing_set_num] && (offset_time == 0
                                    || (time_range[TESTING_TIMERANGE_PERIOD] > 0 && offset_time % (int) time_range[TESTING_TIMERANGE_PERIOD] == 0))) {
                                if (!testing_same_targetTest[testing_set_num] || testing_schedule_count[testing_set_num] == 0) {
                                    ArrayList<AbstractIndividualInterface> testing_schedule = generateTestingSchedule(testRNG, testing_rate_by_classifier[testing_set_num]);
                                    testing_person[testing_set_num] = testing_schedule.toArray(new AbstractIndividualInterface[testing_schedule.size()]);
                                }
                                ArrayUtilsRandomGenerator.shuffleArray(testing_person[testing_set_num], testRNG);
                                testing_numPerDay[testing_set_num] = new int[(int) time_range[TESTING_TIMERANGE_DURATION]];
                                int minTestPerDay = testing_person[testing_set_num].length / (int) time_range[TESTING_TIMERANGE_DURATION];
                                Arrays.fill(testing_numPerDay[testing_set_num], minTestPerDay);
                                int numExtra = testing_person[testing_set_num].length - minTestPerDay * (int) time_range[TESTING_TIMERANGE_DURATION];
                                while (numExtra > 0) {
                                    testing_numPerDay[testing_set_num][testRNG.nextInt((int) time_range[TESTING_TIMERANGE_DURATION])]++;
                                    numExtra--;
                                }
                                testing_pt[testing_set_num] = 0;
                                testing_schedule_count[testing_set_num]++;
                            }
                        }
                    }

                    for (int infId = 0; infId < introAt.length; infId++) {
                        if (introAt[infId] == pop.getGlobalTime()
                                || (introPeriodicity[infId] > 0 && (pop.getGlobalTime() > introAt[infId])
                                && ((pop.getGlobalTime() - introAt[infId]) % introPeriodicity[infId] == 0))) {

                            introInfection(allPerson, infId);
                        }
                        // Record incident
                        for (AbstractIndividualInterface p : pop.getPop()) {
                            if (p.getLastInfectedAtAge(infId) == p.getAge()) {
                                int cI = incidenceClassifier.classifyPerson(p);
                                if (cI >= 0) {
                                    cumulativeIncident[infId * incidenceClassifier.numClass() + cI]++;
                                }
                            }
                        }
                    }

                    // Testing
                    for (int testing_set_num = 0; testing_set_num < testing_numPerDay.length; testing_set_num++) {
                        if (testing_in_timestep[testing_set_num]) {

                            if (!testing_use_daily_rate[testing_set_num] && testing_person[testing_set_num] != null) {
                                int dayIndex = (pop.getGlobalTime() - (int) testing_set_time_range[testing_set_num][TESTING_TIMERANGE_START]);
                                if ((int) testing_set_time_range[testing_set_num][TESTING_TIMERANGE_PERIOD] > 0) {
                                    dayIndex = dayIndex % (int) testing_set_time_range[testing_set_num][TESTING_TIMERANGE_PERIOD];
                                }
                                if (dayIndex < testing_numPerDay[testing_set_num].length) {
                                    int numTestToday = testing_numPerDay[testing_set_num][dayIndex];
                                    while (numTestToday != 0) {
                                        testingPerson(testing_person[testing_set_num][testing_pt[testing_set_num]], treatmentSchdule, testRNG, notificationClassifier);
                                        testing_pt[testing_set_num]++;
                                        numTestToday--;
                                    }
                                }

                            } else if (testing_use_daily_rate[testing_set_num]) {
                                for (AbstractIndividualInterface person : pop.getPop()) {
                                    boolean testToday = false;
                                    int cI = testByClassifier.classifyPerson(person);
                                    if (cI >= 0) {
                                        float testRate = Math.abs(testing_rate_by_classifier[testing_set_num][cI]);
                                        float dailyRate;
                                        // Rate                                          
                                        int testFreq = (int) testRate; // Screen freq (by year). 
                                        testRate = testRate - testFreq;
                                        float srcPeriod = ((AbstractIndividualInterface.ONE_YEAR_INT / ((testFreq < 1 ? 1 : testFreq))));
                                        dailyRate = (float) (1 - Math.exp(Math.log(1 - testRate) / srcPeriod));
                                        testToday = testRNG.nextFloat() < dailyRate;
                                    }
                                    if (testToday) {
                                        testingPerson(person, treatmentSchdule, testRNG, notificationClassifier);
                                    }

                                }

                            }
                        }
                    }

                    if (treatmentSchdule.containsKey(pop.getGlobalTime())) {
                        treatingToday(treatmentSchdule.remove(pop.getGlobalTime()));
                    }

                    pop.advanceTimeStep(1);

                    if (outputPri != null
                            && outputFreq > 0 && (pop.getGlobalTime() - offset) % outputFreq == 0) {
                        generateOutput();
                    }
                }

                if (outputFilePath != null) {

                    String popName = "pop_S" + simId;

                    File tempFile = new File(outputFilePath.getParentFile(), popName);

                    try (ObjectOutputStream outStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile)))) {
                        pop.encodePopToStream(outStream);
                    }
                    FileZipper.zipFile(tempFile, outputFilePath);
                    tempFile.delete();

                }

                if (outputPri != null) {
                    outputPri.println("File exported to " + outputFilePath.getAbsolutePath());
                    outputPri.println("Time required = " + ((System.currentTimeMillis() - tic) / 1000f));
                }

            }
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace(System.err);
        }

        if (outputPri != null && closeOutputOnFinish) {
            outputPri.close();
        }

    }

    protected void generateOutput() {
        // Print number of infected
        int[][] num_total = new int[pop.getInfList().length][];
        int[][] num_infect = new int[pop.getInfList().length][];

        for (AbstractIndividualInterface person : pop.getPop()) {
            for (int infId = 0; infId < pop.getInfList().length; infId++) {
                PersonClassifier prevalClassifer = ((PersonClassifier[]) getInputParam()[PARAM_INDEX_INTRO_CLASSIFIERS])[infId];
                if (num_total[infId] == null) {
                    num_total[infId] = new int[prevalClassifer.numClass()];
                }
                if (num_infect[infId] == null) {
                    num_infect[infId] = new int[prevalClassifer.numClass()];
                }

                int cI = prevalClassifer.classifyPerson(person);

                if (cI >= 0) {
                    num_total[infId][cI]++;

                    if (person.getInfectionStatus()[infId] != AbstractIndividualInterface.INFECT_S) {
                        num_infect[infId][cI]++;
                    }
                }
            }
        }

        for (int infId = 0; infId < pop.getInfList().length; infId++) {
            outputPri.print(pop.getGlobalTime() + " : Preval for infection #" + infId + ":");
            for (int n = 0; n < num_total[infId].length; n++) {
                if (n != 0) {
                    outputPri.print(", ");
                }
                outputPri.print(((float) num_infect[infId][n]) / num_total[infId][n]);
            }
            outputPri.println();
        }
        outputPri.flush();

        if (outputFilePath != null) {

            File incidentFileName = new File(outputFilePath.getParent(), FILE_PREFIX_INCIDENCE + simId + ".csv");
            try (PrintWriter pri = new PrintWriter(new FileWriter(incidentFileName, true))) {
                pri.print(pop.getGlobalTime());
                for (int i = 0; i < cumulativeIncident.length; i++) {
                    pri.print(',');
                    pri.print(cumulativeIncident[i]);
                }
                pri.println();
            } catch (IOException ex) {
                ex.printStackTrace(outputPri);
            }

            // cumulativeTestAndNotification[infectionId][classId][1+infStatus]
            File notificationFileName = new File(outputFilePath.getParent(),
                    FILE_PREFIX_TEST_AND_NOTIFICATION + simId + ".csv");
            boolean writeFirstLine = !notificationFileName.exists();

            try (PrintWriter pri = new PrintWriter(new FileWriter(notificationFileName, true))) {
                if (writeFirstLine) {
                    pri.print("Time");
                    for (int infId = 0; infId < cumulativeTestAndNotification.length; infId++) {
                        for (int classId = 0; classId < cumulativeTestAndNotification[infId].length; classId++) {
                            for (int statusId = 0; statusId < cumulativeTestAndNotification[infId][classId].length; statusId++) {
                                pri.print(',');
                                pri.print("Inf #" + infId + " Class #" + classId + " Status #" + (statusId - 1));
                            }
                        }
                    }
                    pri.println();
                }

                pri.print(pop.getGlobalTime());
                for (int infId = 0; infId < cumulativeTestAndNotification.length; infId++) {
                    for (int classId = 0; classId < cumulativeTestAndNotification[infId].length; classId++) {
                        for (int statusId = 0; statusId < cumulativeTestAndNotification[infId][classId].length; statusId++) {
                            pri.print(',');
                            pri.print(cumulativeTestAndNotification[infId][classId][statusId]);
                        }
                    }
                }
                pri.println();

            } catch (IOException ex) {
                ex.printStackTrace(outputPri);
            }
        }

    }

    public ArrayList<AbstractIndividualInterface> generateTestingSchedule(RandomGenerator testRNG,
            float[] testRatebyClassifier) {

        PersonClassifier testByClassifier = (PersonClassifier) getInputParam()[PARAM_INDEX_TESTING_CLASSIFIER];
        //float[] testRatebyClassifier = (float[]) getInputParam()[PARAM_INDEX_TESTING_RATE_BY_CLASSIFIER];

        ArrayList<AbstractIndividualInterface> testing_schedule = new ArrayList<>();
        ArrayList<AbstractIndividualInterface>[] candidateByClassifier = null;

        if (testByClassifier != null) {
            candidateByClassifier = new ArrayList[testByClassifier.numClass()];
            for (int i = 0; i < candidateByClassifier.length; i++) {
                candidateByClassifier[i] = new ArrayList<>();
            }
        }

        for (AbstractIndividualInterface person : pop.getPop()) {
            if (testByClassifier != null) {
                int cI = testByClassifier.classifyPerson(person);
                if (cI >= 0) {
                    candidateByClassifier[cI].add(person);
                }
            }
        }
        if (candidateByClassifier != null) {
            selectCandidateForTesting(candidateByClassifier, testRatebyClassifier, testRNG, testing_schedule);
        }

        return testing_schedule;
    }

    protected void selectCandidateForTesting(ArrayList<AbstractIndividualInterface>[] candidateCollection,
            float[] rateCollection, RandomGenerator testRNG, ArrayList<AbstractIndividualInterface> testing_schedule) {
        for (int i = 0; i < candidateCollection.length; i++) {
            AbstractIndividualInterface[] candidate = candidateCollection[i].toArray(new AbstractIndividualInterface[candidateCollection[i].size()]);
            float rateAbs = Math.abs(rateCollection[i]);
            int freqOffset = (int) rateAbs;
            int numSel = Math.round((rateAbs - freqOffset) * candidate.length);
            candidate = ArrayUtilsRandomGenerator.randomSelect(candidate, numSel, testRNG);
            testing_schedule.addAll(Arrays.asList(candidate));
        }
    }

    public void testingPerson(AbstractIndividualInterface person,
            HashMap<Integer, int[][]> treatmentSchdule, random.RandomGenerator testRNG,
            PersonClassifier notificationClassifier) {
        Person_Remote_MetaPopulation rmp_person = (Person_Remote_MetaPopulation) person;

        boolean toBeTreated = false;

        float testSen = (float) getInputParam()[PARAM_INDEX_TESTING_SENSITIVITY];

        int currentLoc = pop.getCurrentLocation(rmp_person);
        if (currentLoc < 0) {
            currentLoc = rmp_person.getHomeLocation();
        }

        int notificationCI = -1;

        if (notificationClassifier != null) {
            notificationCI = notificationClassifier.classifyPerson(rmp_person);
        }

        //if (pop.getCurrentLocation(rmp_person) == rmp_person.getHomeLocation()) {
        for (int infId = 0; infId < rmp_person.getInfectionStatus().length; infId++) {
            toBeTreated |= (rmp_person.getInfectionStatus()[infId] != AbstractIndividualInterface.INFECT_S)
                    && testRNG.nextFloat() < testSen;

            if (notificationCI >= 0) {
                // cumulativeTestAndNotification[infectionId][classId][1+infStatus]                                 
                int status = rmp_person.getInfectionStatus()[infId] + 1;
                cumulativeTestAndNotification[infId][notificationCI][status]++;
            }

        }
        //}             

        if (toBeTreated) {

            int[][] delaySettingAll = (int[][]) getInputParam()[PARAM_INDEX_TESTING_TREATMENT_DELAY_BY_LOC];
            int[] delaySetting = null;

            delaySetting = delaySettingAll[currentLoc < delaySettingAll.length ? currentLoc : 0];

            int delay = delaySetting[0];

            if (delaySetting.length == 2) {
                if (delaySetting[1] > 0) {
                    delay += testRNG.nextInt(delaySetting[1]);
                }
            } else {
                int totalLiklihood = delaySetting[delaySetting.length - 1];
                int currentProbPt = 1;
                int prob = testRNG.nextInt(totalLiklihood);

                while (currentProbPt + 1 < delaySetting.length
                        && delaySetting[currentProbPt] < prob) {
                    delay += delaySetting[currentProbPt + 1]; // Min delay;                    
                    currentProbPt += 2;
                }
                if (currentProbPt + 1 >= delaySetting.length) {
                    delay = -1; // Missed out on treatment together
                } else if (delaySetting[currentProbPt + 1] > 0) {
                    delay += testRNG.nextInt(delaySetting[currentProbPt + 1]);
                }

            }

            if (delay >= 0) {

                int[][] schedule = treatmentSchdule.get(pop.getGlobalTime() + delay);

                int[] ent = new int[]{rmp_person.getId(), currentLoc};

                if (schedule == null) {
                    schedule = new int[][]{ent};
                } else {
                    schedule = Arrays.copyOf(schedule, schedule.length + 1);
                    schedule[schedule.length - 1] = ent;
                }

                treatmentSchdule.put(pop.getGlobalTime() + delay, schedule);
            }

        }

    }

    public void treatingToday(int[][] treatmentSchdule) {
        for (AbstractIndividualInterface person : pop.getPop()) {
            Person_Remote_MetaPopulation rmp_person = (Person_Remote_MetaPopulation) person;
            for (int[] id_loc : treatmentSchdule) {

                int currentLoc = pop.getCurrentLocation(rmp_person);

                if (id_loc[0] == rmp_person.getId()) {
                    if (id_loc[1] == currentLoc) {
                        for (int infId = 0; infId < rmp_person.getInfectionStatus().length; infId++) {
                            if (pop.getInfList()[infId] instanceof TreatableInfectionInterface) {
                                if (rmp_person.getInfectionStatus()[infId] != AbstractIndividualInterface.INFECT_S) {
                                    ((TreatableInfectionInterface) pop.getInfList()[infId]).applyTreatmentAt(rmp_person, pop.getGlobalTime());
                                    if (rmp_person.getInfectionStatus()[infId] != AbstractIndividualInterface.INFECT_S) {
                                        rmp_person.getInfectionStatus()[infId] = AbstractIndividualInterface.INFECT_S;
                                        rmp_person.setTimeUntilNextStage(infId, Double.POSITIVE_INFINITY);
                                    }
                                    rmp_person.setLastTreatedAt((int) rmp_person.getAge());
                                }
                            }

                        }

                    } else {
                        // Miss out on treatment due to location

                    }

                }

            }
        }

    }

    public void importPop() throws ClassNotFoundException, IOException {
        if (outputPri != null) {
            outputPri.println("Importing pop file from " + importFilePath.getAbsolutePath());
        }
        File tempPop = FileZipper.unzipFile(importFilePath, importFilePath.getParentFile());

        try (ObjectInputStream oIStream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(tempPop)))) {
            pop = Population_Remote_MetaPopulation.decodeFromStream(oIStream);
        }
        tempPop.delete();
    }

    // Introduction of infection
    public void introInfection(AbstractIndividualInterface[] allPerson, int infId) {
        int[] numInPop;
        int[] numAlreadyInfect;
        int[] numNewInfection;

        AbstractInfection[] modelledInfections = (AbstractInfection[]) getInputParam()[PARAM_INDEX_INFECTIONS];
        float[][] introPrevalence = (float[][]) getInputParam()[PARAM_INDEX_INTRO_PREVALENCE];
        PersonClassifier[] introClassifiers = (PersonClassifier[]) getInputParam()[PARAM_INDEX_INTRO_CLASSIFIERS];

        numInPop = new int[introClassifiers[infId].numClass()];
        numAlreadyInfect = new int[introClassifiers[infId].numClass()];
        for (AbstractIndividualInterface person : allPerson) {
            int cId = introClassifiers[infId].classifyPerson(person);
            if (cId >= 0) {
                numInPop[cId]++;
                if (person.getInfectionStatus()[infId] != AbstractIndividualInterface.INFECT_S) {
                    numAlreadyInfect[cId]++;
                }
            }
        }

        numNewInfection = new int[introClassifiers[infId].numClass()];
        for (int cId = 0; cId < numNewInfection.length; cId++) {
            if (introPrevalence[infId][cId] < 1) {        // Ratio vs absolute introduction    
                numNewInfection[cId] = Math.max(0, Math.round(numInPop[cId] * introPrevalence[infId][cId]) - numAlreadyInfect[cId]);
            } else {
                numNewInfection[cId] = (int) introPrevalence[infId][cId];
            }
        }

        for (AbstractIndividualInterface person : allPerson) {

            int cId = introClassifiers[infId].classifyPerson(person);
            if (cId >= 0) {
                if (numNewInfection[cId] != 0) {
                    if (pop.getInfectionRNG().nextInt(numInPop[cId]) < numNewInfection[cId]) {
                        //System.out.println("Infecting " + person.getId());
                        modelledInfections[infId].infecting(person);
                        numNewInfection[cId]--;
                    }
                    numInPop[cId]--;
                }
            }

        }
    }

    public void updatePopFieldFromString(int fieldIndex, String fieldEntry) {
        Object orgVal = ((Population_Remote_MetaPopulation) getPop()).getFields()[fieldIndex];

        if (orgVal == null) {
            switch (fieldIndex) {
                case Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_NEWPERSON_INFECTION_PREVAL:
                    orgVal = new float[0][];
                    break;
                case Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_NEWPERSON_INFECTION_CLASSIFIER:
                    orgVal = new PersonClassifier[0];
                    break;
                default:
                    System.err.println("Default class for Pop #" + fieldIndex + " non defined");
                    break;
            }

        }

        if (orgVal != null) {
            ((Population_Remote_MetaPopulation) getPop()).getFields()[fieldIndex]
                    = PropValUtils.propStrToObject(fieldEntry, orgVal.getClass());

        }

    }

}
