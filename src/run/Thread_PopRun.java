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

/**
 *
 * @author Ben Hui
 * @version 20180602
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
        new PersonClassifier() {
            int numLoc = 5;
            int numGender = 2;
            int numAgeGrp = 4;

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
                    res += loc * numGender * numAgeGrp;
                }

                return res;
            }

            @Override
            public int numClass() {
                return numLoc * numGender * numAgeGrp; // 5 loc * 2 gender * 4 age grp
            }
        },
        // 7: PARAM_INDEX_TESTING_RATE_BY_CLASSIFIER
        // Regional adjustment from GOANNA, p.43  
        // Remote from STRIVE (FNQ, LC's slides)
        // type: float[]           
        // From RG
        // Alterative format
        // if the first rate is negative use proprotion testing rate instead
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
        // From GOANNA, p.43        
        /*
           new float[]{
            // Regional
            0.44f,
            // Remote
            0.48f,
            0.48f,
            0.48f,
            0.48f,},
         */
        new float[0],
        // 9: PARAM_INDEX_TESTING_TREATMENT_DELAY_BY_LOC
        // [min, range]
        // Alterative format
        // [min, cumul_liklihood_1, cumul_delay_range_1, cumul_liklihood_2, cumul_delay_range_2, .... total_liklihood]        
        new int[][]{new int[]{7, 21}},
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

                int[] testing_numPerDay = null;
                int testing_pt = 0;
                AbstractIndividualInterface[] testing_person = null;

                // Treatment
                HashMap<Integer, int[][]> treatmentSchdule = new HashMap();

                long tic = System.currentTimeMillis();
                pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_INFECTION_LIST] = modelledInfections;
                pop.updateInfectionList(modelledInfections);

                AbstractIndividualInterface[] allPerson = pop.getPop();

                for (AbstractIndividualInterface person : allPerson) {
                    Person_Remote_MetaPopulation prm = (Person_Remote_MetaPopulation) person;
                    prm.setNumberOfInfections(modelledInfections.length);
                }

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

                boolean useProportionTestCoverage = ((float[]) inputParam[PARAM_INDEX_TESTING_RATE_BY_CLASSIFIER])[0] < 0;

                for (int t = 0; t < numSteps; t++) {

                    if (!useProportionTestCoverage
                            && (pop.getGlobalTime() - offset) % AbstractIndividualInterface.ONE_YEAR_INT == 0) {
                        ArrayList<AbstractIndividualInterface> testing_schedule = generateTestingSchedule(testRNG);

                        testing_person = testing_schedule.toArray(new AbstractIndividualInterface[testing_schedule.size()]);

                        ArrayUtilsRandomGenerator.shuffleArray(testing_person, testRNG);

                        testing_numPerDay = new int[AbstractIndividualInterface.ONE_YEAR_INT];

                        int minTestPerDay = testing_person.length / AbstractIndividualInterface.ONE_YEAR_INT;

                        Arrays.fill(testing_numPerDay, minTestPerDay);

                        int numExtra = testing_person.length - minTestPerDay * AbstractIndividualInterface.ONE_YEAR_INT;

                        while (numExtra > 0) {
                            testing_numPerDay[testRNG.nextInt(AbstractIndividualInterface.ONE_YEAR_INT)]++;
                            numExtra--;
                        }

                        testing_pt = 0;

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
                    if (!useProportionTestCoverage) {

                        int numTestToday = testing_numPerDay[(pop.getGlobalTime() - offset) % AbstractIndividualInterface.ONE_YEAR_INT];

                        while (numTestToday != 0) {
                            testingPerson(testing_person[testing_pt], treatmentSchdule, testRNG, notificationClassifier);
                            testing_pt++;
                            numTestToday--;
                        }

                    } else {

                        float[] testRatebyClassifier = (float[]) getInputParam()[PARAM_INDEX_TESTING_RATE_BY_CLASSIFIER];
                        float[] testRateByLoc = (float[]) getInputParam()[PARAM_INDEX_TESTING_RATE_BY_HOME_LOC];

                        for (AbstractIndividualInterface person : pop.getPop()) {
                            boolean testToday = false;
                            if (testByClassifier != null) {
                                int cI = testByClassifier.classifyPerson(person);
                                if (cI >= 0) {
                                    float testRate = Math.abs(testRatebyClassifier[cI]);
                                    // Rate is a yearly rate, so have to convert it into daily rate
                                    float dailyRate = (float) (1 - Math.exp(Math.log(1 - testRate) / AbstractIndividualInterface.ONE_YEAR_INT));
                                    testToday = testRNG.nextFloat() < dailyRate;
                                }
                            }
                            if (!testToday && testRateByLoc != null && testRateByLoc.length > 0) {
                                int loc = ((MoveablePersonInterface) person).getHomeLocation();
                                float testByLocRate = Math.abs(testRateByLoc[loc]);
                                float dailyTestByLocRate = (float) (1 - Math.exp(Math.log(1 - testByLocRate) / AbstractIndividualInterface.ONE_YEAR_INT));
                                testToday = testRNG.nextFloat() < dailyTestByLocRate;
                            }
                            if (testToday) {
                                testingPerson(person, treatmentSchdule, testRNG, notificationClassifier);
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
                            pri.print("Inf #" + infId + " Class #" + classId + " Status #" + (statusId-1));
                        }
                    }
                }
                pri.println();
            }

            for (int infId = 0; infId < cumulativeTestAndNotification.length; infId++) {
                pri.print(pop.getGlobalTime());
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

    public ArrayList<AbstractIndividualInterface> generateTestingSchedule(RandomGenerator testRNG) {

        PersonClassifier testByClassifier = (PersonClassifier) getInputParam()[PARAM_INDEX_TESTING_CLASSIFIER];
        float[] testRatebyClassifier = (float[]) getInputParam()[PARAM_INDEX_TESTING_RATE_BY_CLASSIFIER];
        float[] testRateByLoc = (float[]) getInputParam()[PARAM_INDEX_TESTING_RATE_BY_HOME_LOC];
        ArrayList<AbstractIndividualInterface> testing_schedule = new ArrayList<>();
        ArrayList<AbstractIndividualInterface>[] candidateByClassifier = null;
        ArrayList<AbstractIndividualInterface>[] candidateByLocation = null;
        if (testByClassifier != null) {
            candidateByClassifier = new ArrayList[testByClassifier.numClass()];
            for (int i = 0; i < candidateByClassifier.length; i++) {
                candidateByClassifier[i] = new ArrayList<>();
            }
        }
        if (testRateByLoc != null && testRateByLoc.length > 0) {
            candidateByLocation = new ArrayList[((int[]) pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_POP_SIZE]).length];
            for (int loc = 0; loc < candidateByLocation.length; loc++) {
                candidateByLocation[loc] = new ArrayList<>();
            }
        }
        for (AbstractIndividualInterface person : pop.getPop()) {
            if (testByClassifier != null) {
                int cI = testByClassifier.classifyPerson(person);
                if (cI >= 0) {
                    candidateByClassifier[cI].add(person);
                }
            }
            if (candidateByLocation != null) {
                int loc = ((MoveablePersonInterface) person).getHomeLocation();
                candidateByLocation[loc].add(person);
            }
        }
        if (candidateByClassifier != null) {
            selectCandidateForTesting(candidateByClassifier, testRatebyClassifier, testRNG, testing_schedule);
        }
        if (candidateByLocation != null) {
            selectCandidateForTesting(candidateByLocation, testRateByLoc, testRNG, testing_schedule);
        }
        return testing_schedule;
    }

    protected void selectCandidateForTesting(ArrayList<AbstractIndividualInterface>[] candidateCollection,
            float[] rateCollection, RandomGenerator testRNG, ArrayList<AbstractIndividualInterface> testing_schedule) {
        for (int i = 0; i < candidateCollection.length; i++) {
            AbstractIndividualInterface[] candidate = candidateCollection[i].toArray(new AbstractIndividualInterface[candidateCollection[i].size()]);
            int numSel = Math.round(rateCollection[i] * candidate.length);
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

            int[] delaySetting = delaySettingAll[currentLoc < delaySettingAll.length ? currentLoc : 0];

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

}
