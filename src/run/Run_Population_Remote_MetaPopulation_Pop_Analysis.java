package run;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import person.AbstractIndividualInterface;
import person.Person_Remote_MetaPopulation;
import population.Population_Remote_MetaPopulation;
import util.FileZipper;
import util.PersonClassifier;

/**
 *
 * @author Ben Hui
 * @version 20180913
 *
 * <pre>
 * History
 *
 * 20180613:
 *  - Add incident summary
 * 20180618:
 *  - Debug - incidence calculation
 * 20180625:
 *  - Add notification summary
 * 20180703:
 *  - Add prevalence for remote only
 * 20180705:
 *  - Minor formatting change for printing of remote only output
 * 20180913:
 *  - Add support for test gap time
 *  - Formatting change for the CSV output to refect simulation number
 * 20200221
 *  - Add support for num_partners_in_12_months_all
 * </pre>
 */
public class Run_Population_Remote_MetaPopulation_Pop_Analysis {

    public static final int OUTPUT_INDEX_DEMOGRAPHIC = 0;
    public static final int OUTPUT_INDEX_AWAY_FROM_HOME_BY_LOC = OUTPUT_INDEX_DEMOGRAPHIC + 1;
    public static final int OUTPUT_INDEX_NUM_PARTNERS_IN_12_MONTHS = OUTPUT_INDEX_AWAY_FROM_HOME_BY_LOC + 1;
    public static final int OUTPUT_INDEX_PREVALENCE_BY_GENDER_AGE = OUTPUT_INDEX_NUM_PARTNERS_IN_12_MONTHS + 1;
    public static final int OUTPUT_INDEX_INCIDENT_SUMMARY = OUTPUT_INDEX_PREVALENCE_BY_GENDER_AGE + 1;
    public static final int OUTPUT_INDEX_NOTIFICATION_SUMMARY = OUTPUT_INDEX_INCIDENT_SUMMARY + 1;
    public static final int OUTPUT_INDEX_PREVALENCE_BY_GENGER_AGE_REMOTE_ONLY = OUTPUT_INDEX_NOTIFICATION_SUMMARY + 1;
    public static final int OUTPUT_INDEX_PREVALENCE_BY_GENGER_AGE_REGIONAL_ONLY = OUTPUT_INDEX_PREVALENCE_BY_GENGER_AGE_REMOTE_ONLY + 1;
    public static final int OUTPUT_INDEX_GAP_TIME_TESTING = OUTPUT_INDEX_PREVALENCE_BY_GENGER_AGE_REGIONAL_ONLY + 1;
    public static final int OUTPUT_INDEX_NUM_PARTNERS_IN_12_MONTHS_ALL = OUTPUT_INDEX_GAP_TIME_TESTING + 1;

    public static final String[] OUTPUT_FILENAMES = new String[]{
        "demographic.csv",
        "away_from_home_by_location.csv",
        "num_partners_in_12_months.csv",
        "prevalence_by_gender_age.csv",
        "incident_summary.csv",
        "notification_summary.csv",
        "prevalence_by_gender_age_remote_only.csv",
        "prevalence_by_gender_age_regional_only.csv",
        "gap_time_testing.csv",
        "num_partners_in_12_months_all.csv",};
    public static final String[] OUTPUT_FILE_HEADERS = new String[]{
        "Sim, Pop, M 16-19, M 20-24, M 25-29, M 30-34, F 16-19, F 20-24, F 25-29, F 30-34,",
        "Sim, Pop, M 16-19, M 20-24, M 25-29, M 30-34, F 16-19, F 20-24, F 25-29, F 30-34,",
        "Sim, Age_Grp, 0, 1, 2-4, 5",
        "Sim, M 16-19, M 20-24, M 25-29, M 30-34, F 16-19, F 20-24, F 25-29, F 30-34, "
        + "M 16-19, M 20-24, M 25-29, M 30-34, F 16-19, F 20-24, F 25-29, F 30-34,"
        + "M 16-19, M 20-24, M 25-29, M 30-34, F 16-19, F 20-24, F 25-29, F 30-34,",
        "Sim, CT Male, CT Female, NG Male, NG Female",
        "Sim, CT Male, CT Female, NG Male, NG Female",
        "Sim, M 16-19, M 20-24, M 25-29, M 30-34, F 16-19, F 20-24, F 25-29, F 30-34, "
        + "M 16-19, M 20-24, M 25-29, M 30-34, F 16-19, F 20-24, F 25-29, F 30-34,"
        + "M 16-19, M 20-24, M 25-29, M 30-34, F 16-19, F 20-24, F 25-29, F 30-34,",
        "Sim, M 16-19, M 20-24, M 25-29, M 30-34, F 16-19, F 20-24, F 25-29, F 30-34, "
        + "M 16-19, M 20-24, M 25-29, M 30-34, F 16-19, F 20-24, F 25-29, F 30-34,"
        + "M 16-19, M 20-24, M 25-29, M 30-34, F 16-19, F 20-24, F 25-29, F 30-34,",
        "Sim, Id, Ave gap time",
        "Sim,"
    };

    final static Pattern PATTERN_SIM_NUM = Pattern.compile("\\d+");

    public static void popAnalysis(String dir) throws IOException, ClassNotFoundException {
        File baseDir = new File(dir);

        System.out.println("Decoding results in " + baseDir.getAbsolutePath());

        File[] popZipFiles = baseDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(".zip");
            }
        });

        Arrays.sort(popZipFiles, new Comparator<File>() {

            @Override
            public int compare(File t, File t1) {
                return t.getName().compareTo(t1.getName());
            }
        });

        PrintWriter[] wri = new PrintWriter[OUTPUT_FILENAMES.length];

        for (int i = 0; i < wri.length; i++) {
            File outputFile = new File(baseDir, OUTPUT_FILENAMES[i]);
            if (outputFile.exists()) {
                System.out.println("Output file " + outputFile.getAbsolutePath() + " already exist! Result not overwritten.");
                wri[i] = null;

            } else {
                wri[i] = new PrintWriter(outputFile);
                wri[i].println(OUTPUT_FILE_HEADERS[i]);
            }

        }

        System.out.println("Number of population zip = " + popZipFiles.length);

        int[] simNumber = new int[popZipFiles.length];

        // OUTPUT_INDEX_DEMOGRAPHIC
        // OUTPUT_INDEX_AWAY_FROM_HOME_BY_LOC
        // [simNum][popNum][gender_age_index]
        int[][][] numAwayByLoc = new int[popZipFiles.length][][];
        int[][][] numByHomeLoc = new int[popZipFiles.length][][];

        // OUTPUT_INDEX_NUM_PARTNERS_IN_12_MONTHS
        // [simNum][age_index][0, 1, 2-4, 5]
        int[][][] numPartInLast12Months = new int[popZipFiles.length][][];

        // OUTPUT_INDEX_PREVALENCE_BY_GENDER_AGE
        // [simNum][gender_age_index]{total, CT, NG}        
        int[][][] numInfectedTotal = new int[popZipFiles.length][][];

        //OUTPUT_INDEX_PREVALENCE_BY_GENGER_AGE_REMOTE_ONLY
        // [simNum][gender_age_index]{total, CT, NG}        
        int[][][] numInfecteRemoteOnly = new int[popZipFiles.length][][];

        //OUTPUT_INDEX_PREVALENCE_BY_GENGER_AGE_REGIONAL_ONLY
        // [simNum][gender_age_index]{total, CT, NG}        
        int[][][] numInfecteRegionalOnly = new int[popZipFiles.length][][];

        //OUTPUT_INDEX_NUM_PARTNERS_IN_12_MONTHS_ALL
        // [simNum][0, 1, 2, ...]
        int[][] numPartInLast12Months_all = new int[popZipFiles.length][];
        int numPartInLast12Months_all_max = 10;

        PersonClassifier Classifier_ageGrp = new util.Default_Remote_MetaPopulation_AgeGrp_Classifier();
        PersonClassifier Classifier_behavor = new util.Default_Remote_MetaPopulation_Behavor_Classifier();

        for (int popId = 0; popId < popZipFiles.length; popId++) {
            File popZipFile = popZipFiles[popId];
            simNumber[popId] = popId;

            Matcher m = PATTERN_SIM_NUM.matcher(popZipFiles[popId].getName());
            if (m.find()) {
                simNumber[popId] = Integer.parseInt(m.group());
            }

            System.out.print("Decoding " + popZipFile.getAbsolutePath() + "...");
            File tempPop = FileZipper.unzipFile(popZipFile, baseDir);
            ObjectInputStream oIStream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(tempPop)));
            Population_Remote_MetaPopulation pop = Population_Remote_MetaPopulation.decodeFromStream(oIStream);
            oIStream.close();
            tempPop.delete();

            int numMetaPop = ((int[]) pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_POP_SIZE]).length;

            numAwayByLoc[popId] = new int[numMetaPop][Classifier_ageGrp.numClass() * 2];
            numByHomeLoc[popId] = new int[numMetaPop][Classifier_ageGrp.numClass() * 2];
            numPartInLast12Months[popId] = new int[Classifier_ageGrp.numClass()][Classifier_behavor.numClass()];
            numPartInLast12Months_all[popId] = new int[numPartInLast12Months_all_max];
            numInfectedTotal[popId] = new int[Classifier_ageGrp.numClass() * 2][pop.getInfList().length + 1];
            numInfecteRemoteOnly[popId] = new int[Classifier_ageGrp.numClass() * 2][pop.getInfList().length + 1];
            numInfecteRegionalOnly[popId] = new int[Classifier_ageGrp.numClass() * 2][pop.getInfList().length + 1];

            AbstractIndividualInterface[] persons = pop.getPop();

            for (AbstractIndividualInterface person : persons) {
                Person_Remote_MetaPopulation rmp_person = (Person_Remote_MetaPopulation) person;
                int homeLoc, currentLoc, genderOffset;
                int ageIndex, behavIndex;

                homeLoc = rmp_person.getHomeLocation();
                currentLoc = pop.getCurrentLocation(rmp_person);
                ageIndex = Classifier_ageGrp.classifyPerson(rmp_person);
                behavIndex = Classifier_behavor.classifyPerson(rmp_person);
                genderOffset = rmp_person.isMale() ? 0 : Classifier_ageGrp.numClass();

                numByHomeLoc[popId][homeLoc][genderOffset + ageIndex]++;
                if (homeLoc != currentLoc) {
                    numAwayByLoc[popId][homeLoc][genderOffset + ageIndex]++;
                }

                numPartInLast12Months[popId][ageIndex][behavIndex]++;

                int numPartLastYears = rmp_person.getNumPartnerInPastYear();
                int[] ent = numPartInLast12Months_all[popId];
                if (ent.length <= numPartLastYears) {
                    numPartInLast12Months_all[popId] = Arrays.copyOf(ent, numPartLastYears + 1);
                    numPartInLast12Months_all_max = Math.max(numPartInLast12Months_all_max, numPartInLast12Months_all[popId].length);
                }

                numPartInLast12Months_all[popId][numPartLastYears]++;

                numInfectedTotal[popId][genderOffset + ageIndex][0]++;

                if (currentLoc != 0) {
                    numInfecteRemoteOnly[popId][genderOffset + ageIndex][0]++;
                } else {
                    numInfecteRegionalOnly[popId][genderOffset + ageIndex][0]++;
                }

                for (int infId = 0; infId < pop.getInfList().length; infId++) {
                    if (rmp_person.getInfectionStatus()[infId] != AbstractIndividualInterface.INFECT_S) {
                        numInfectedTotal[popId][genderOffset + ageIndex][infId + 1]++;

                        if (currentLoc != 0) {
                            numInfecteRemoteOnly[popId][genderOffset + ageIndex][infId + 1]++;
                        } else {
                            numInfecteRegionalOnly[popId][genderOffset + ageIndex][infId + 1]++;
                        }
                    }
                }

            }

            System.out.println(" done");

        }

        // OUTPUT_INDEX_AWAY_FROM_HOME_BY_LOC
        // [simNum][popNum][gender_age_index]
        //int[][][] numAwayByLoc = new int[popZipFiles.length][][];
        //int[][][] numByHomeLoc = new int[popZipFiles.length][][];
        for (int s = 0; s < numByHomeLoc.length; s++) {
            for (int p = 0; p < numByHomeLoc[s].length; p++) {

                if (wri[OUTPUT_INDEX_DEMOGRAPHIC] != null) {

                    wri[OUTPUT_INDEX_DEMOGRAPHIC].print(simNumber[s]);
                    wri[OUTPUT_INDEX_DEMOGRAPHIC].print(',');
                    wri[OUTPUT_INDEX_DEMOGRAPHIC].print(p);
                }

                if (wri[OUTPUT_INDEX_AWAY_FROM_HOME_BY_LOC] != null) {

                    wri[OUTPUT_INDEX_AWAY_FROM_HOME_BY_LOC].print(simNumber[s]);
                    wri[OUTPUT_INDEX_AWAY_FROM_HOME_BY_LOC].print(',');
                    wri[OUTPUT_INDEX_AWAY_FROM_HOME_BY_LOC].print(p);
                }

                for (int ga = 0; ga < numByHomeLoc[s][p].length; ga++) {
                    if (wri[OUTPUT_INDEX_DEMOGRAPHIC] != null) {
                        wri[OUTPUT_INDEX_DEMOGRAPHIC].print(',');
                        wri[OUTPUT_INDEX_DEMOGRAPHIC].print(numByHomeLoc[s][p][ga]);
                    }

                    if (wri[OUTPUT_INDEX_AWAY_FROM_HOME_BY_LOC] != null) {
                        wri[OUTPUT_INDEX_AWAY_FROM_HOME_BY_LOC].print(',');

                        if (numByHomeLoc[s][p][ga] > 0) {
                            wri[OUTPUT_INDEX_AWAY_FROM_HOME_BY_LOC].print(((float) numAwayByLoc[s][p][ga]) / numByHomeLoc[s][p][ga]);
                        } else {
                            wri[OUTPUT_INDEX_AWAY_FROM_HOME_BY_LOC].print(0);
                        }
                    }
                }

                if (wri[OUTPUT_INDEX_DEMOGRAPHIC] != null) {
                    wri[OUTPUT_INDEX_DEMOGRAPHIC].println();
                }

                if (wri[OUTPUT_INDEX_AWAY_FROM_HOME_BY_LOC] != null) {
                    wri[OUTPUT_INDEX_AWAY_FROM_HOME_BY_LOC].println();
                }

            }

        }

        // OUTPUT_INDEX_NUM_PARTNERS_IN_12_MONTHS
        // [simNum][age_index][0, 1, 2-4, 5]
        //int[][][] numPartInLast12Months = new int[popZipFiles.length][][];
        if (wri[OUTPUT_INDEX_NUM_PARTNERS_IN_12_MONTHS] != null) {
            for (int s = 0; s < numPartInLast12Months.length; s++) {
                for (int a = 0; a < numPartInLast12Months[s].length; a++) {
                    wri[OUTPUT_INDEX_NUM_PARTNERS_IN_12_MONTHS].print(simNumber[s]);
                    wri[OUTPUT_INDEX_NUM_PARTNERS_IN_12_MONTHS].print(',');
                    wri[OUTPUT_INDEX_NUM_PARTNERS_IN_12_MONTHS].print(a);
                    int total = 0;
                    for (int b = 0; b < numPartInLast12Months[s][a].length; b++) {
                        total += numPartInLast12Months[s][a][b];
                    }
                    for (int b = 0; b < numPartInLast12Months[s][a].length; b++) {
                        wri[OUTPUT_INDEX_NUM_PARTNERS_IN_12_MONTHS].print(',');
                        wri[OUTPUT_INDEX_NUM_PARTNERS_IN_12_MONTHS].print(((float) numPartInLast12Months[s][a][b]) / total);
                    }
                    wri[OUTPUT_INDEX_NUM_PARTNERS_IN_12_MONTHS].println();
                }
            }
        }

        // OUTPUT_INDEX_PREVALENCE_BY_GENDER_AGE
        // [simNum][gender_age_index]{total, CT, NG}    
        if (wri[OUTPUT_INDEX_PREVALENCE_BY_GENDER_AGE] != null) {
            for (int s = 0; s < numInfectedTotal.length; s++) {
                wri[OUTPUT_INDEX_PREVALENCE_BY_GENDER_AGE].print(simNumber[s]);
                for (int a = 0; a < numInfectedTotal[s].length; a++) {
                    wri[OUTPUT_INDEX_PREVALENCE_BY_GENDER_AGE].print(',');
                    wri[OUTPUT_INDEX_PREVALENCE_BY_GENDER_AGE].print(numInfectedTotal[s][a][0]);
                }
                for (int infId = 1; infId < numInfectedTotal[s][0].length; infId++) {
                    for (int a = 0; a < numInfectedTotal[s].length; a++) {
                        wri[OUTPUT_INDEX_PREVALENCE_BY_GENDER_AGE].print(',');
                        wri[OUTPUT_INDEX_PREVALENCE_BY_GENDER_AGE].print(((float) numInfectedTotal[s][a][infId]) / numInfectedTotal[s][a][0]);
                    }
                }
                wri[OUTPUT_INDEX_PREVALENCE_BY_GENDER_AGE].println();
            }
        }

        // OUTPUT_INDEX_PREVALENCE_BY_GENGER_AGE_REMOTE_ONLY
        // [simNum][gender_age_index]{total, CT, NG}    
        if (wri[OUTPUT_INDEX_PREVALENCE_BY_GENGER_AGE_REMOTE_ONLY] != null) {

            for (int s = 0; s < numInfecteRemoteOnly.length; s++) {
                wri[OUTPUT_INDEX_PREVALENCE_BY_GENGER_AGE_REMOTE_ONLY].print(simNumber[s]);
                for (int a = 0; a < numInfecteRemoteOnly[s].length; a++) {
                    wri[OUTPUT_INDEX_PREVALENCE_BY_GENGER_AGE_REMOTE_ONLY].print(',');
                    wri[OUTPUT_INDEX_PREVALENCE_BY_GENGER_AGE_REMOTE_ONLY].print(numInfecteRemoteOnly[s][a][0]);
                }
                for (int infId = 1; infId < numInfecteRemoteOnly[s][0].length; infId++) {
                    for (int a = 0; a < numInfecteRemoteOnly[s].length; a++) {
                        wri[OUTPUT_INDEX_PREVALENCE_BY_GENGER_AGE_REMOTE_ONLY].print(',');
                        wri[OUTPUT_INDEX_PREVALENCE_BY_GENGER_AGE_REMOTE_ONLY].print(((float) numInfecteRemoteOnly[s][a][infId]) / numInfecteRemoteOnly[s][a][0]);
                    }
                }
                wri[OUTPUT_INDEX_PREVALENCE_BY_GENGER_AGE_REMOTE_ONLY].println();
            }
        }

        // OUTPUT_INDEX_PREVALENCE_BY_GENGER_AGE_REGIONAL_ONLY
        // [simNum][gender_age_index]{total, CT, NG}    
        if (wri[OUTPUT_INDEX_PREVALENCE_BY_GENGER_AGE_REGIONAL_ONLY] != null) {
            for (int s = 0; s < numInfecteRegionalOnly.length; s++) {
                wri[OUTPUT_INDEX_PREVALENCE_BY_GENGER_AGE_REGIONAL_ONLY].print(simNumber[s]);
                for (int a = 0; a < numInfecteRegionalOnly[s].length; a++) {
                    wri[OUTPUT_INDEX_PREVALENCE_BY_GENGER_AGE_REGIONAL_ONLY].print(',');
                    wri[OUTPUT_INDEX_PREVALENCE_BY_GENGER_AGE_REGIONAL_ONLY].print(numInfecteRegionalOnly[s][a][0]);
                }
                for (int infId = 1; infId < numInfecteRegionalOnly[s][0].length; infId++) {
                    for (int a = 0; a < numInfecteRegionalOnly[s].length; a++) {
                        wri[OUTPUT_INDEX_PREVALENCE_BY_GENGER_AGE_REGIONAL_ONLY].print(',');
                        wri[OUTPUT_INDEX_PREVALENCE_BY_GENGER_AGE_REGIONAL_ONLY].print(((float) numInfecteRegionalOnly[s][a][infId]) / numInfecteRegionalOnly[s][a][0]);
                    }
                }
                wri[OUTPUT_INDEX_PREVALENCE_BY_GENGER_AGE_REGIONAL_ONLY].println();
            }
        }

        //OUTPUT_INDEX_INCIDENT_SUMMARY
        File[] singleIncidentCSV = baseDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                String fName = file.getName();
                return fName.startsWith("incident_S");
            }
        });
        System.out.println("Number of incident file = " + singleIncidentCSV.length);
        generateSummaryCSV(singleIncidentCSV, wri, OUTPUT_INDEX_INCIDENT_SUMMARY);

        // OUTPUT_INDEX_NOTIFICATION_SUMMARY
        File[] singleNotificationCSV = baseDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                String fName = file.getName();
                return fName.startsWith("notification_S");
            }
        });
        System.out.println("Number of notification file = " + singleIncidentCSV.length);
        generateSummaryCSV(singleNotificationCSV, wri, OUTPUT_INDEX_NOTIFICATION_SUMMARY);

        // OUTPUT_INDEX_GAP_TIME_TESTING
        if (wri[OUTPUT_INDEX_GAP_TIME_TESTING] != null) {
            int fileIndex = OUTPUT_INDEX_GAP_TIME_TESTING;
            File[] singleTestingHistory = baseDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    String fName = file.getName();
                    return fName.startsWith("indiv_history_test_");
                }
            });
            System.out.println("Number of testing history file = " + singleTestingHistory.length);

            String line;
            for (int s = 0; s < singleTestingHistory.length; s++) {
                int simNum = s;
                Matcher m = PATTERN_SIM_NUM.matcher(singleTestingHistory[s].getName());
                if (m.find()) {
                    simNum = Integer.parseInt(m.group());
                }
                BufferedReader reader = new BufferedReader(new FileReader(singleTestingHistory[s]));

                while ((line = reader.readLine()) != null) {
                    String[] ent = line.split(",");
                    // Only include indivudal with more than 1 test in lifetime

                    if (ent.length > 3) {
                        int numGap = 0;
                        float gapTimeSum = 0;
                        int preAge = Integer.parseInt(ent[2]);
                        for (int k = 3; k < ent.length; k++) {
                            int curAge = Integer.parseInt(ent[k]);
                            gapTimeSum += curAge - preAge;
                            preAge = curAge;
                            numGap++;
                        }
                        wri[fileIndex].print(simNum);
                        wri[fileIndex].print(',');
                        wri[fileIndex].print(ent[0]); // ID
                        wri[fileIndex].print(',');
                        wri[fileIndex].print(gapTimeSum / numGap);
                        wri[fileIndex].println();
                    }
                }
                reader.close();
            }
        }

        //OUTPUT_INDEX_NUM_PARTNERS_IN_12_MONTHS_ALL
        // [simNum][0, 1, 2, ...]
        if (wri[OUTPUT_INDEX_NUM_PARTNERS_IN_12_MONTHS_ALL] != null) {
            for (int i = 0; i < numPartInLast12Months_all_max; i++) {
                wri[OUTPUT_INDEX_NUM_PARTNERS_IN_12_MONTHS_ALL].print(',');
                wri[OUTPUT_INDEX_NUM_PARTNERS_IN_12_MONTHS_ALL].print(i);
            }
            wri[OUTPUT_INDEX_NUM_PARTNERS_IN_12_MONTHS_ALL].println();

            for (int s = 0; s < numPartInLast12Months_all.length; s++) {
                wri[OUTPUT_INDEX_NUM_PARTNERS_IN_12_MONTHS_ALL].print(simNumber[s]);

                for (int i = 0; i < numPartInLast12Months_all[s].length; i++) {
                    wri[OUTPUT_INDEX_NUM_PARTNERS_IN_12_MONTHS_ALL].print(',');
                    wri[OUTPUT_INDEX_NUM_PARTNERS_IN_12_MONTHS_ALL].print(numPartInLast12Months_all[s][i]);
                }
                wri[OUTPUT_INDEX_NUM_PARTNERS_IN_12_MONTHS_ALL].println();
            }

        }

        for (PrintWriter pWri : wri) {
            if (pWri != null) {
                pWri.close();
            }
        }

    }

    public static void generateSummaryCSV(File[] singleSimulationCSV,
            PrintWriter[] wri, int fileIndex) throws IOException, NumberFormatException {
        if (wri[fileIndex] != null) {

            for (int s = 0; s < singleSimulationCSV.length; s++) {
                int simNum = s;
                Matcher m = PATTERN_SIM_NUM.matcher(singleSimulationCSV[s].getName());
                if (m.find()) {
                    simNum = Integer.parseInt(m.group());
                }

                wri[fileIndex].print(simNum);
                BufferedReader reader = new BufferedReader(new FileReader(singleSimulationCSV[s]));
                String lastline = reader.readLine();
                String currentline = reader.readLine();
                String nextLine;
                while ((nextLine = reader.readLine()) != null) {
                    lastline = currentline;
                    currentline = nextLine;
                }
                reader.close();

                if (lastline != null && currentline != null) {
                    String[] lastLineArr = lastline.split(",");
                    String[] currentLineArr = currentline.split(",");

                    for (int c = 1; c < currentLineArr.length; c++) {
                        wri[fileIndex].print(',');
                        float numIncidencePerYear
                                = (Integer.parseInt(currentLineArr[c]) - Integer.parseInt(lastLineArr[c]))
                                / ((Integer.parseInt(currentLineArr[0]) - Integer.parseInt(lastLineArr[0])) / 365f);
                        wri[fileIndex].print(numIncidencePerYear);

                    }

                }
                wri[fileIndex].println();

            }
        }
    }

}
