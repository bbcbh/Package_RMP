package run;

import infection.AbstractInfection;
import infection.SyphilisInfection;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.math3.distribution.BetaDistribution;
import person.AbstractIndividualInterface;
import person.Person_Remote_MetaPopulation;
import population.AbstractFieldsArrayPopulation;
import random.RandomGenerator;
import util.PersonClassifier;
import util.PropValUtils;

/**
 *
 * @author Ben Hui
 * @version 20180815
 *
 * <pre>
 * History
 *
 * 20180517
 * - Change the ordering of import file name by numberical index instead of file name
 * - Add feedback for decodeCollectionFile
 *
 * 20180518
 * - Add support for varying tranmission probability
 *
 * 20180522
 * - Remove main method
 *
 * 20180523
 * - Change messages for repeated simulation run from System.err to System.out
 *
 * 20180612
 * - Renaming of getParamValue to getRunParamValues
 *
 * 20180618
 * - Reset testing rate as an input (PARAM_INDEX_TESTING_RATE_BY_CLASSIFIER)
 *
 * 20180713
 * - Introduce implementation of Abstract_Run_IntroInfection interface
 *
 * 20180718
 * - Adapt usage of updatePopFieldFromString() from Thread_PopRun
 *
 * 20180815
 * -  Add support for Abstract_Run_IntroInfection class
 *
 * 20180907
 *  - Add support for indivudal infection, testing and treatment history, removal of duplciate code that serve the same purpose
 * </pre>
 */
public class Run_Population_Remote_MetaPopulation_Pop_Intro_Syphilis extends Abstract_Run_IntroInfection {

    public String BASE_DIR_STR = "~/RMP/OptResults";
    public String IMPORT_DIR_STR = "~/RMP/ImportDir";
    public int NUM_THREADS = Runtime.getRuntime().availableProcessors();
    public int NUM_SIM_TOTAL = 1000;
    public int NUM_STEPS = 360 * 50;
    public int SAMP_FREQ = 90;

    final Pattern Pattern_importFile = Pattern.compile("\\w*pop_S(\\d+).zip");
    public static final int NUM_COLLECTION = 4;
    public static final String FILENAME_COLLECTION_STORE = "collectionStore.obj";
    public static final String FILENAME_NUM_IN_POP_CSV = "numInPop.csv";
    public static final String FILENAME_NUM_INFECTED_CSV = "numInfected.csv";
    public static final String FILENAME_NUM_INFECTIOUS_CSV = "numInfectious.csv";
    public static final String FILENAME_NEW_INFECT_CSV = "newInfectionStore.csv";
    public static final String FILENAME_INFECTION_HISTORY_OBJ_PREFIX = "infectionHistory";
    public static final String FILENAME_INPUT_PARAM_VALUES = "input_param_values.csv"; // Format: paramter id, value

    public double[] DEFAULT_PARAM_VALUES = {
        // Tranmission Prob 
        // 0 - 4: Male to Female,
        0.18,
        0.18,
        0.18,
        0.09,
        0.18,
        // 5 - 9: Female to Male
        0.15,
        0.15,
        0.15,
        0.075,
        0.15,
        // Duration parameter
        // 10-11: Incubation 
        21,
        28,
        // 12-13: Primary
        45,
        60,
        // 14-15: Secondary
        100,
        140,
        // 16-17: Early Latent
        360 - (28 + 60 + 140),
        720 - (21 + 45 + 100),
        // 18-19: Remission
        6 * 30,
        0,
        // 20-21: Recurrent
        90,
        0,
        // 22-23: Latent
        15 * 360,
        0,
        // 24-25: Immunity
        5 * 360,
        0,
        // Trans. prob SD
        // if 0, not used (i.e. fixed value)
        // if <0, as a proportion of input parameter
        // 26-30: Male to Female
        0,
        0,
        0,
        0,
        0,
        // 31-35: Female to Male
        0,
        0,
        0,
        0,
        0,};

    protected double[] paramVal_Run;

    public static final int PARAM_INDEX_TRAN_MF_INCUB = 0;
    public static final int PARAM_INDEX_TRAN_MF_PRI = PARAM_INDEX_TRAN_MF_INCUB + 1;
    public static final int PARAM_INDEX_TRAN_MF_SEC = PARAM_INDEX_TRAN_MF_PRI + 1;
    public static final int PARAM_INDEX_TRAN_MF_EARLY_LT = PARAM_INDEX_TRAN_MF_SEC + 1;
    public static final int PARAM_INDEX_TRAN_MF_RECURRENT = PARAM_INDEX_TRAN_MF_EARLY_LT + 1;
    public static final int PARAM_INDEX_TRAN_FM_INCUB = PARAM_INDEX_TRAN_MF_RECURRENT + 1;
    public static final int PARAM_INDEX_TRAN_FM_PRI = PARAM_INDEX_TRAN_FM_INCUB + 1;
    public static final int PARAM_INDEX_TRAN_FM_SEC = PARAM_INDEX_TRAN_FM_PRI + 1;
    public static final int PARAM_INDEX_TRAN_FM_EARLY_LT = PARAM_INDEX_TRAN_FM_SEC + 1;
    public static final int PARAM_INDEX_TRAN_FM_RECURRENT = PARAM_INDEX_TRAN_FM_EARLY_LT + 1;
    public static final int PARAM_INDEX_DURATION_INCUBATION_MIN = PARAM_INDEX_TRAN_FM_RECURRENT + 1;
    public static final int PARAM_INDEX_DURATION_INCUBATION_MAX = PARAM_INDEX_DURATION_INCUBATION_MIN + 1;
    public static final int PARAM_INDEX_DURATION_PRIMARY_MIN = PARAM_INDEX_DURATION_INCUBATION_MAX + 1;
    public static final int PARAM_INDEX_DURATION_PRIMARY_MAX = PARAM_INDEX_DURATION_PRIMARY_MIN + 1;
    public static final int PARAM_INDEX_DURATION_SECONDARY_MIN = PARAM_INDEX_DURATION_PRIMARY_MAX + 1;
    public static final int PARAM_INDEX_DURATION_SECONDARY_MAX = PARAM_INDEX_DURATION_SECONDARY_MIN + 1;
    public static final int PARAM_INDEX_DURATION_EARLY_LATENT_MIN = PARAM_INDEX_DURATION_SECONDARY_MAX + 1;
    public static final int PARAM_INDEX_DURATION_EARLY_LATENT_MAX = PARAM_INDEX_DURATION_EARLY_LATENT_MIN + 1;
    public static final int PARAM_INDEX_DURATION_REMISSION_MIN = PARAM_INDEX_DURATION_EARLY_LATENT_MAX + 1;
    public static final int PARAM_INDEX_DURATION_REMISSION_MAX = PARAM_INDEX_DURATION_REMISSION_MIN + 1;
    public static final int PARAM_INDEX_DURATION_RECURRENT_MIN = PARAM_INDEX_DURATION_REMISSION_MAX + 1;
    public static final int PARAM_INDEX_DURATION_RECURRENT_MAX = PARAM_INDEX_DURATION_RECURRENT_MIN + 1;
    public static final int PARAM_INDEX_DURATION_LATENT_MIN = PARAM_INDEX_DURATION_RECURRENT_MAX + 1;
    public static final int PARAM_INDEX_DURATION_LATENT_MAX = PARAM_INDEX_DURATION_LATENT_MIN + 1;
    public static final int PARAM_INDEX_DURATION_IMMUN_MIN = PARAM_INDEX_DURATION_LATENT_MAX + 1;
    public static final int PARAM_INDEX_DURATION_IMMUN_MAX = PARAM_INDEX_DURATION_IMMUN_MIN + 1;
    public static final int PARAM_INDEX_TRAN_MF_INCUB_SD = PARAM_INDEX_DURATION_IMMUN_MAX + 1;
    public static final int PARAM_INDEX_TRAN_MF_PRI_SD = PARAM_INDEX_TRAN_MF_INCUB_SD + 1;
    public static final int PARAM_INDEX_TRAN_MF_SEC_SD = PARAM_INDEX_TRAN_MF_PRI_SD + 1;
    public static final int PARAM_INDEX_TRAN_MF_EARLY_LT_SD = PARAM_INDEX_TRAN_MF_SEC_SD + 1;
    public static final int PARAM_INDEX_TRAN_MF_RECURRENT_SD = PARAM_INDEX_TRAN_MF_EARLY_LT_SD + 1;
    public static final int PARAM_INDEX_TRAN_FM_INCUB_SD = PARAM_INDEX_TRAN_MF_RECURRENT_SD + 1;
    public static final int PARAM_INDEX_TRAN_FM_PRI_SD = PARAM_INDEX_TRAN_FM_INCUB_SD + 1;
    public static final int PARAM_INDEX_TRAN_FM_SEC_SD = PARAM_INDEX_TRAN_FM_PRI_SD + 1;
    public static final int PARAM_INDEX_TRAN_FM_EARLY_LT_SD = PARAM_INDEX_TRAN_FM_SEC_SD + 1;
    public static final int PARAM_INDEX_TRAN_FM_RECURRENT_SD = PARAM_INDEX_TRAN_FM_EARLY_LT_SD + 1;

    public Run_Population_Remote_MetaPopulation_Pop_Intro_Syphilis(String[] arg) {

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

        // 4: Num step
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

        // 6: Store infection history
        if (arg.length > 6) {
            if (!arg[6].isEmpty()) {
                setStoreInfectionHistory(Boolean.parseBoolean(arg[6]));
            }
        }

        paramVal_Run = Arrays.copyOf(DEFAULT_PARAM_VALUES, DEFAULT_PARAM_VALUES.length);

        System.out.println("BASE_DIR = " + BASE_DIR_STR);
        System.out.println("IMPORT_DIR = " + IMPORT_DIR_STR);
        System.out.println("NUM_THREADS = " + NUM_THREADS);
        System.out.println("NUM_SIM_TOTAL = " + NUM_SIM_TOTAL);
        System.out.println("NUM_STEPS = " + NUM_STEPS);
        System.out.println("SAMP_FREQ = " + SAMP_FREQ);
        System.out.println("STORE_INFECTION_HISTORY = " + isStoreInfectionHistory());

        File inputParamFile = new File(BASE_DIR_STR, FILENAME_INPUT_PARAM_VALUES);

        if (inputParamFile.exists()) {
            System.out.println("Using parameter vaules file at " + inputParamFile.getAbsolutePath());

            try {
                BufferedReader reader = new BufferedReader(new FileReader(inputParamFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] ent = line.split(",");
                    int key = Integer.parseInt(ent[0]);
                    double value = Double.parseDouble(ent[1]);
                    paramVal_Run[key] = value;

                    System.out.println("Input parameter #" + Integer.toString(key)
                            + " = " + Double.toString(value));
                }

            } catch (IOException ex) {
                ex.printStackTrace(System.err);

            }

        }

    }

    public double[] getRunParamValues() {
        return paramVal_Run;
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
                Matcher m = Pattern_importFile.matcher(file.getName());
                return m.matches();
            }
        });

        Arrays.sort(popFiles, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                Matcher m1 = Pattern_importFile.matcher(f1.getName());
                Matcher m2 = Pattern_importFile.matcher(f2.getName());
                m1.find();
                m2.find();
                Integer n1 = Integer.valueOf(m1.group(1));
                Integer n2 = Integer.valueOf(m2.group(1));
                return n1.compareTo(n2);
            }
        });

        if (NUM_SIM_TOTAL > 0 && NUM_SIM_TOTAL < popFiles.length) {
            popFiles = Arrays.copyOf(popFiles, NUM_SIM_TOTAL);
        }
        System.out.println(popFiles.length + " population file(s) will be used in simulation");

        ExecutorService executor = null;
        int numInExe = 0;
        final int numPopFiles = popFiles.length;

        File previouStoreFile = new File(exportDir, FILENAME_COLLECTION_STORE);

        final HashMap<Integer, HashMap<Integer, Integer>> collection_NumIndividuals; // Time, # New infection by sim
        final HashMap<Integer, HashMap<Integer, Integer>> collection_NumInfected;
        final HashMap<Integer, HashMap<Integer, Integer>> collection_NumInfectious;
        final HashMap<Integer, HashMap<Integer, Integer>> collection_NewInfection;
        final HashMap[] collectionsArray;

        final HashMap<Integer, HashMap<Integer, int[]>> collection_InfectionHistory = new HashMap<>(); // [Sim_id] -> Id, Infection at
        final HashMap<Integer, ArrayList<int[]>> timeToDX = new HashMap<>(); // SimId -> {[time, id, age, timeToDx]}

        if (previouStoreFile.exists()) {

            HashMap[] collectionsArrayRead;

            try {
                ObjectInputStream objIn = new ObjectInputStream(new FileInputStream(previouStoreFile));
                collectionsArrayRead = (HashMap[]) objIn.readObject();

                Files.move(previouStoreFile.toPath(),
                        new File(exportDir, FILENAME_COLLECTION_STORE
                                + "_" + Long.toString(System.currentTimeMillis())).toPath(),
                        StandardCopyOption.ATOMIC_MOVE
                );

            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace(System.err);
                collectionsArrayRead = new HashMap[NUM_COLLECTION];
                for (int i = 0; i < collectionsArrayRead.length; i++) {
                    collectionsArrayRead[i] = new HashMap();
                }
            }

            collectionsArray = collectionsArrayRead;
            collection_NumIndividuals = collectionsArrayRead[0];
            collection_NumInfected = collectionsArrayRead[1];
            collection_NumInfectious = collectionsArrayRead[2];
            collection_NewInfection = collectionsArrayRead[3];

        } else {
            collection_NumIndividuals = new HashMap(); // Time, # New infection by sim
            collection_NumInfected = new HashMap();
            collection_NumInfectious = new HashMap();
            collection_NewInfection = new HashMap();

            collectionsArray = new HashMap[]{
                collection_NumIndividuals,
                collection_NumInfected,
                collection_NumInfectious,
                collection_NewInfection, //collection_InfectionHistory
            };
        }
        for (File importPop : popFiles) {
            int sId;

            Matcher m = Pattern_importFile.matcher(importPop.getName());
            if (m.find()) {
                sId = Integer.parseInt(m.group(1));
            } else {
                sId = (int) (System.currentTimeMillis() / 1000);
                System.err.println("Ill-formed population file name " + importPop.getName()
                        + "\nUse system time of " + Integer.toString(sId) + "instead.");
            }
            if (executor == null) {
                executor = Executors.newFixedThreadPool(NUM_THREADS);
                numInExe = 0;
            }

            timeToDX.put(sId, new ArrayList<>());

            // Check if there is a previous entry
            File infectionHistoryStore = new File(BASE_DIR_STR,
                    FILENAME_INFECTION_HISTORY_OBJ_PREFIX + "_" + sId + ".obj");

            if (isStoreInfectionHistory() && !infectionHistoryStore.exists()) {
                collection_InfectionHistory.put(sId, new HashMap<>());
            }

            final File outputPopFile = new File(exportDir, "Sim_" + importPop.getName());

            boolean skipPop = getPopSelection() != null && (Arrays.binarySearch(getPopSelection(), sId) < 0);

            if (outputPopFile.exists()) {
                System.out.println("Output pop " + outputPopFile.getAbsolutePath()
                        + " already existed. Skipping simulation");
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
                final ArrayList<int[]> timeToDiagsArr = timeToDX.get(sId);

                // Generate thread
                Thread_PopRun thread = new Thread_PopRun(outputPopFile, importPop, sId, NUM_STEPS) {
                    @Override
                    protected void generateOutput() {
                        super.generateOutput();
                        int snapFreq = super.getOutputFreq();

                        int numNewInf = 0;
                        int numInfected = 0;
                        int numInfectious = 0;

                        for (AbstractIndividualInterface person : super.getPop().getPop()) {
                            Person_Remote_MetaPopulation rmp = (Person_Remote_MetaPopulation) person;

                            if (super.getPop().getInfList()[0].isInfected(rmp)) {
                                numInfected++;
                            }
                            if (super.getPop().getInfList()[0].isInfectious(rmp)) {
                                numInfectious++;
                            }

                            double lastInfAge = person.getLastInfectedAtAge(0);
                            if (lastInfAge > 0) {
                                if ((rmp.getAge() - lastInfAge) < snapFreq) {
                                    numNewInf++;
                                }

                                HashMap<Integer, int[]> collection_InfectionHistoryEnt
                                        = collection_InfectionHistory.get(super.getSimId());

                                if (collection_InfectionHistoryEnt != null) {
                                    int[] infectionHistoryEnt = collection_InfectionHistoryEnt.get(rmp.getId());
                                    if (infectionHistoryEnt == null
                                            || infectionHistoryEnt[infectionHistoryEnt.length - 1] != (int) lastInfAge) {

                                        if (infectionHistoryEnt == null) {
                                            infectionHistoryEnt = new int[1];
                                        } else {
                                            infectionHistoryEnt = Arrays.copyOf(infectionHistoryEnt, infectionHistoryEnt.length + 1);
                                        }
                                        infectionHistoryEnt[infectionHistoryEnt.length - 1] = (int) lastInfAge;
                                        collection_InfectionHistoryEnt.put(rmp.getId(), infectionHistoryEnt);
                                    }
                                }
                            }
                        }

                        if (!collection_NumIndividuals.containsKey(super.getPop().getGlobalTime())) {
                            collection_NumIndividuals.put(super.getPop().getGlobalTime(), new HashMap<>());
                            collection_NumInfected.put(super.getPop().getGlobalTime(), new HashMap<>());
                            collection_NumInfectious.put(super.getPop().getGlobalTime(), new HashMap<>());
                        }
                        collection_NumIndividuals.get(super.getPop().getGlobalTime()).put(super.getSimId(), super.getPop().getPop().length);
                        collection_NumInfected.get(super.getPop().getGlobalTime()).put(super.getSimId(), numInfected);
                        collection_NumInfectious.get(super.getPop().getGlobalTime()).put(super.getSimId(), numInfectious);

                        if (!collection_NewInfection.containsKey(super.getPop().getGlobalTime())) {
                            collection_NewInfection.put(super.getPop().getGlobalTime(), new HashMap<>());
                        }
                        collection_NewInfection.get(super.getPop().getGlobalTime()).put(super.getSimId(), numNewInf);

                    }

                    @Override
                    public void testingPerson(AbstractIndividualInterface person, HashMap<Integer, int[][]> treatmentSchdule,
                            RandomGenerator testRNG, PersonClassifier notificationClassifier, int testing_option) {
                        if (this.getPop().getInfList()[0].isInfectious(person)) {
                            int[] ent = new int[]{this.getPop().getGlobalTime(), person.getId(), (int) person.getAge(),
                                (int) (person.getAge() - person.getLastInfectedAtAge(0))};
                            timeToDiagsArr.add(ent);
                        }
                        super.testingPerson(person, treatmentSchdule, testRNG, notificationClassifier, testing_option);
                    }

                };

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
                    // Skip thread
                    thread = null;
                }

                if (thread != null) {

                    setSyphilisSetting(thread);

                    if (NUM_THREADS > 1) {
                        executor.submit(thread);
                        numInExe++;
                    } else {
                        thread.run();
                        exportCollectionFiles(exportDir, collectionsArray, collection_InfectionHistory, timeToDX);
                    }
                }

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
                exportCollectionFiles(exportDir, collectionsArray, collection_InfectionHistory, timeToDX);
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
            exportCollectionFiles(exportDir, collectionsArray, collection_InfectionHistory, timeToDX);
        }

    }

    public void exportCollectionFiles(File exportDir,
            final HashMap[] collectionsArray,
            HashMap<Integer, HashMap<Integer, int[]>> infectionHistory,
            HashMap<Integer, ArrayList<int[]>> timeToDX) {

        final HashMap[] infectionHistoryArr = new HashMap[infectionHistory.size()];
        Integer[] keyArr = infectionHistory.keySet().toArray(new Integer[infectionHistory.size()]);
        Arrays.sort(keyArr);
        for (int i = 0; i < infectionHistoryArr.length; i++) {
            infectionHistoryArr[i] = infectionHistory.get(keyArr[i]);
        }
        exportCollectionFiles(exportDir, collectionsArray, infectionHistoryArr);

        for (Integer sId : timeToDX.keySet()) {
            File srcFile = new File(exportDir, "Time2Diag_S" + Integer.toString(sId) + ".csv");
            if (timeToDX.get(sId).size() > 0 && !srcFile.exists()) {
                try {
                    PrintWriter wri = new PrintWriter(srcFile);
                    // int[] ent = new int[]{this.getPop().getGlobalTime(), person.getId(),  (int) person.getAge(), 
                    //        (int) (person.getAge() - person.getLastInfectedAtAge(0))};                            
                    wri.println("Time, PersonId, PersonAge, Time to Dx (days)");
                    for (int[] t2dx : timeToDX.get(sId)) {
                        if (t2dx != null) {
                            String line = String.format("%d,%d,%d,%d", t2dx[0], t2dx[1], t2dx[2], t2dx[3]);

                            /*                        
                        boolean firstEntry = true;
                        for(int ent : t2dx){
                            if(!firstEntry){
                                wri.print(',');
                            }else{
                                firstEntry = true;
                            }
                            wri.print(ent);
                        }
                             */
                            wri.println(line);
                        }
                    }
                    wri.close();

                } catch (IOException ex) {
                    ex.printStackTrace(System.err);
                }
            }
        }

    }

    public void exportCollectionFiles(
            File exportDir,
            final HashMap[] collectionsArray,
            final HashMap[] infectionHistory) {

        try (ObjectOutputStream objStr = new ObjectOutputStream(new FileOutputStream(new File(exportDir, FILENAME_COLLECTION_STORE)))) {
            objStr.writeObject(collectionsArray);
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
        for (int i = 0; i < infectionHistory.length; i++) {
            if (infectionHistory[i] != null) {
                // Check if there is a previous entry
                File infectionHistoryStore = new File(BASE_DIR_STR,
                        FILENAME_INFECTION_HISTORY_OBJ_PREFIX + "_" + i + ".obj");

                if (!infectionHistoryStore.exists()) {
                    try (ObjectOutputStream objStr = new ObjectOutputStream(new FileOutputStream(infectionHistoryStore))) {
                        objStr.writeObject(collectionsArray);
                    } catch (IOException ex) {
                        ex.printStackTrace(System.err);
                    }

                }

            }

        }

    }

    public static void collectionToCSV(HashMap<Integer, HashMap<Integer, Integer>> collection, PrintWriter pri) {
        // Decode newInfectionCollection
        Integer[] keys = collection.keySet().toArray(new Integer[collection.size()]);
        Arrays.sort(keys);
        for (Integer k : keys) {
            HashMap<Integer, Integer> entRow = collection.get(k);
            pri.print(k.intValue());
            for (int ent : entRow.keySet()) {
                pri.print(',');
                pri.print(Integer.toString(entRow.get(ent)));
            }
            pri.println();
        }
    }

    /*
    public static void collectionToCSV(HashMap<Integer, int[]> collection, PrintWriter pri) {
        // Decode newInfectionCollection
        Integer[] keys = collection.keySet().toArray(new Integer[collection.size()]);
        Arrays.sort(keys);
        for (Integer k : keys) {
            int[] entRow = collection.get(k);
            pri.print(k.intValue());
            for (int ent : entRow) {
                pri.print(',');
                pri.print(Integer.toString(ent));
            }
            pri.println();
        }
    }
     */
    public static void collectionToCSV_StringKey(HashMap<String, int[]> collection, PrintWriter pri) {
        String[] keys = collection.keySet().toArray(new String[collection.size()]);
        Arrays.sort(keys);
        for (String k : keys) {
            int[] entRow = collection.get(k);
            pri.print(k);
            for (int ent : entRow) {
                pri.print(',');
                pri.print(Integer.toString(ent));
            }
            pri.println();
        }
    }

    public static void decodeCollectionFile(File baseDir) throws FileNotFoundException, IOException, ClassNotFoundException {
        String[] collectionFileName = new String[]{
            FILENAME_NUM_IN_POP_CSV,
            FILENAME_NUM_INFECTED_CSV,
            FILENAME_NUM_INFECTIOUS_CSV,
            FILENAME_NEW_INFECT_CSV,};

        File previouStoreFile = new File(baseDir, FILENAME_COLLECTION_STORE);

        boolean suc = false;

        if (previouStoreFile.exists()) {
            HashMap[] collectionsArrayRead;
            ObjectInputStream objIn = new ObjectInputStream(new FileInputStream(previouStoreFile));
            collectionsArrayRead = (HashMap[]) objIn.readObject();

            for (int i = 0; i < collectionsArrayRead.length; i++) {
                System.out.println("Decoding collection #" + i + " to file " + collectionFileName[i]);
                try (PrintWriter pri = new PrintWriter(new File(baseDir, collectionFileName[i]))) {
                    run.Run_Population_Remote_MetaPopulation_Pop_Intro_Syphilis.collectionToCSV(collectionsArrayRead[i], pri);
                }
            }

            suc = true;
        }

        if (!suc) {
            System.err.println("Error(s) in decoding " + previouStoreFile.getAbsolutePath());

        }

    }

    private class CLASSIFIER_SYPHILIS_PREVAL implements PersonClassifier {

        AbstractFieldsArrayPopulation pop;

        public CLASSIFIER_SYPHILIS_PREVAL(AbstractFieldsArrayPopulation pop) {
            this.pop = pop;
        }

        @Override
        public int classifyPerson(AbstractIndividualInterface p) {

            if (pop.getRelMap()[0].containsVertex(p.getId())) {
                int deg = pop.getRelMap()[0].degreeOf(p.getId());
                return deg > 0 ? 0 : -1;
            } else {
                return -1;
            }
        }

        @Override
        public int numClass() {
            return 1;
        }

    };

    protected void setSyphilisSetting(Thread_PopRun thread) {
        // Set syphilis setting
        SyphilisInfection syphilis = new SyphilisInfection(null);
        syphilis.setInfectionIndex(0);
        RandomGenerator rng = ((population.Population_Remote_MetaPopulation) thread.getPop()).getInfectionRNG();

        // Tranmission   
        double[] generateParam = new double[2];
        generateParam = setSyphilisTranProb(SyphilisInfection.DIST_INDEX_TRANS_INCUBATION_MF, PARAM_INDEX_TRAN_MF_INCUB, PARAM_INDEX_TRAN_MF_INCUB_SD,
                generateParam, rng, syphilis);
        setSyphilisTranProb(SyphilisInfection.DIST_INDEX_TRANS_PRI_MF, PARAM_INDEX_TRAN_MF_PRI, PARAM_INDEX_TRAN_MF_PRI_SD, generateParam, rng, syphilis);
        setSyphilisTranProb(SyphilisInfection.DIST_INDEX_TRANS_SEC_MF, PARAM_INDEX_TRAN_MF_SEC, PARAM_INDEX_TRAN_MF_SEC_SD, generateParam, rng, syphilis);
        setSyphilisTranProb(SyphilisInfection.DIST_INDEX_TRANS_RECURRENT_MF, PARAM_INDEX_TRAN_MF_RECURRENT, PARAM_INDEX_TRAN_MF_RECURRENT_SD, generateParam, rng, syphilis);
        setSyphilisTranProb(SyphilisInfection.DIST_INDEX_TRANS_EARLY_LATENT_MF, PARAM_INDEX_TRAN_MF_EARLY_LT, PARAM_INDEX_TRAN_MF_EARLY_LT_SD, generateParam, rng, syphilis);

        generateParam = setSyphilisTranProb(SyphilisInfection.DIST_INDEX_TRANS_INCUBATION_FM, PARAM_INDEX_TRAN_FM_INCUB, PARAM_INDEX_TRAN_FM_INCUB_SD, generateParam, rng, syphilis);
        setSyphilisTranProb(SyphilisInfection.DIST_INDEX_TRANS_PRI_FM, PARAM_INDEX_TRAN_FM_PRI, PARAM_INDEX_TRAN_FM_PRI_SD, generateParam, rng, syphilis);
        setSyphilisTranProb(SyphilisInfection.DIST_INDEX_TRANS_SEC_FM, PARAM_INDEX_TRAN_FM_SEC, PARAM_INDEX_TRAN_FM_SEC_SD, generateParam, rng, syphilis);
        setSyphilisTranProb(SyphilisInfection.DIST_INDEX_TRANS_RECURRENT_FM, PARAM_INDEX_TRAN_FM_RECURRENT, PARAM_INDEX_TRAN_FM_RECURRENT_SD, generateParam, rng, syphilis);
        setSyphilisTranProb(SyphilisInfection.DIST_INDEX_TRANS_EARLY_LATENT_FM, PARAM_INDEX_TRAN_FM_EARLY_LT, PARAM_INDEX_TRAN_FM_EARLY_LT_SD, generateParam, rng, syphilis);

        // Duration               
        String key;
        key = SyphilisInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", Integer.toString(SyphilisInfection.DIST_INDEX_DURATION_INCUBATION));
        syphilis.setParameter(key, new double[]{paramVal_Run[PARAM_INDEX_DURATION_INCUBATION_MIN], paramVal_Run[PARAM_INDEX_DURATION_INCUBATION_MAX]});
        key = SyphilisInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", Integer.toString(SyphilisInfection.DIST_INDEX_DURATION_PRIMARY));
        syphilis.setParameter(key, new double[]{paramVal_Run[PARAM_INDEX_DURATION_PRIMARY_MIN], paramVal_Run[PARAM_INDEX_DURATION_PRIMARY_MAX]});
        key = SyphilisInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", Integer.toString(SyphilisInfection.DIST_INDEX_DURATION_SECONDARY));
        syphilis.setParameter(key, new double[]{paramVal_Run[PARAM_INDEX_DURATION_SECONDARY_MIN], paramVal_Run[PARAM_INDEX_DURATION_SECONDARY_MAX]});
        key = SyphilisInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", Integer.toString(SyphilisInfection.DIST_INDEX_DURATION_EARLY_LATENT));
        syphilis.setParameter(key, new double[]{paramVal_Run[PARAM_INDEX_DURATION_EARLY_LATENT_MIN], paramVal_Run[PARAM_INDEX_DURATION_EARLY_LATENT_MAX]});
        key = SyphilisInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", Integer.toString(SyphilisInfection.DIST_INDEX_DURATION_LATENT));
        syphilis.setParameter(key, new double[]{paramVal_Run[PARAM_INDEX_DURATION_LATENT_MIN], paramVal_Run[PARAM_INDEX_DURATION_LATENT_MAX]});
        key = SyphilisInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", Integer.toString(SyphilisInfection.DIST_INDEX_DURATION_REMISSION));
        syphilis.setParameter(key, new double[]{paramVal_Run[PARAM_INDEX_DURATION_REMISSION_MIN], paramVal_Run[PARAM_INDEX_DURATION_REMISSION_MAX]});
        key = SyphilisInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", Integer.toString(SyphilisInfection.DIST_INDEX_DURATION_RECURRENT));
        syphilis.setParameter(key, new double[]{paramVal_Run[PARAM_INDEX_DURATION_RECURRENT_MIN], paramVal_Run[PARAM_INDEX_DURATION_RECURRENT_MAX]});
        key = SyphilisInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", Integer.toString(SyphilisInfection.DIST_INDEX_DURATION_IMMUN));
        syphilis.setParameter(key, new double[]{paramVal_Run[PARAM_INDEX_DURATION_IMMUN_MIN], paramVal_Run[PARAM_INDEX_DURATION_IMMUN_MAX]});

        CLASSIFIER_SYPHILIS_PREVAL prevalClass
                = new CLASSIFIER_SYPHILIS_PREVAL((AbstractFieldsArrayPopulation) thread.getPop());

        thread.getInputParam()[Thread_PopRun.PARAM_INDEX_INFECTIONS]
                = new AbstractInfection[]{syphilis};

        // Dummy classifier for any individual 
        thread.getInputParam()[Thread_PopRun.PARAM_INDEX_INTRO_CLASSIFIERS]
                = new PersonClassifier[]{prevalClass};
        thread.getInputParam()[Thread_PopRun.PARAM_INDEX_INTRO_PREVALENCE]
                = new float[][]{new float[]{1}};
        thread.getInputParam()[Thread_PopRun.PARAM_INDEX_INTRO_AT]
                = new int[]{360 * 50 + 1};
        thread.getInputParam()[Thread_PopRun.PARAM_INDEX_INTRO_PERIODICITY]
                = new int[]{-1};

        // Print output
        PrintWriter outputPrint = thread.getOutputPri();

        for (int i = 0; i < threadParamValStr.length; i++) {
            if (threadParamValStr[i] != null && threadParamValStr[i].length() > 0) {
                thread.getInputParam()[i]
                        = PropValUtils.propStrToObject(threadParamValStr[i],
                                thread.getInputParam()[i].getClass());
                outputPrint.println("Thread ParamVal #" + i + " = " + threadParamValStr[i]);

            }
        }

        for (int i = 0; i < popParamValStr.length; i++) {
            if (popParamValStr[i] != null) {

                /*
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

                   
                }
                 */
                thread.updatePopFieldFromString(i, popParamValStr[i]);
                if (outputPrint != null) {
                    outputPrint.println("Pop Field #" + i + " = " + popParamValStr[i]);
                }
            }
        }

        outputPrint.println("Syphilis infection state: ");
        for (int i = 0; i < SyphilisInfection.DIST_TOTAL; i++) {
            key = SyphilisInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", Integer.toString(i));

            String distClass;
            try {
                distClass = syphilis.getDistributionClass(i).getName();
            } catch (NullPointerException ex) {
                distClass = "Fixed";
            }

            double[] param = (double[]) syphilis.getParameter(key);

            outputPrint.println("Dist #" + i + " = " + distClass + ": " + Arrays.toString(param));
        }

        outputPrint.flush();

    }

    public double[] setSyphilisTranProb(int infectionIndex,
            int tran_parameter_Index,
            int tran_parameterSD_Index,
            double[] defaultTranProb,
            RandomGenerator rng,
            SyphilisInfection syphilis) {
        String parameterKey = SyphilisInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", Integer.toString(infectionIndex));
        if (paramVal_Run[tran_parameterSD_Index] > 0) {
            // Beta parameter
            double[] betaParam = generatedBetaParam(new double[]{paramVal_Run[tran_parameter_Index], paramVal_Run[tran_parameterSD_Index]});
            BetaDistribution dist = new BetaDistribution(rng, betaParam[0], betaParam[1]);
            syphilis.setParameter(parameterKey, new double[]{dist.sample(), 0});
        } else if (paramVal_Run[tran_parameterSD_Index] < 0) {
            syphilis.setParameter(parameterKey, new double[]{defaultTranProb[0] * -paramVal_Run[tran_parameterSD_Index],
                defaultTranProb[1] * -paramVal_Run[tran_parameterSD_Index]});
        } else {
            syphilis.setParameter(parameterKey, new double[]{paramVal_Run[tran_parameter_Index], 0});
        }

        return (double[]) syphilis.getParameter(parameterKey);
    }

    private static double[] generatedBetaParam(double[] input) {
        // For Beta distribution, 
        // alpha = mean*(mean*(1-mean)/variance - 1)
        // beta = (1-mean)*(mean*(1-mean)/variance - 1)
        double[] res = new double[2];
        double var = input[1] * input[1];
        double rP = input[0] * (1 - input[0]) / var - 1;
        //alpha
        res[0] = rP * input[0];
        //beta
        res[1] = rP * (1 - input[0]);
        return res;

    }
}
