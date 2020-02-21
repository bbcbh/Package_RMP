package test.nonsim;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import person.AbstractIndividualInterface;

/**
 *
 * @author Ben Hui
 */
public class Test_Population_Remote_MetaPopulation_Pop_Preval_CSV {

    File resultDir;
    final Pattern outputFilePattern = Pattern.compile("output_(\\d+)\\.txt");
    final Pattern outputlinePattern = Pattern.compile("(\\d+) : Preval for infection #(\\d+):(.*)");

    final Pattern prevalenceFilePattern = Pattern.compile("prevalence_S(\\d+)\\.csv");
    final Pattern prevalenceHeaderPattern = Pattern.compile("Inf #(\\d+) Class #(\\d+) Status #(-?\\d+)");

    final String prevalByClassPrefix = "prevalence_summary_inf_";
    final String prevalTotalPrefix = "prevalence_total_inf_";

    HashMap<Integer, double[][]>[] dataStore; // Integer = time, float[class][simNum]
    HashMap<Integer, int[][]>[] totalCount; // Integer = time, int[simNum][num_inf, num_total]

    int NUM_CLASS = 8;

    public Test_Population_Remote_MetaPopulation_Pop_Preval_CSV(File resultDir) {
        this.resultDir = resultDir;
    }

    public void decode() throws FileNotFoundException, IOException {

        System.out.print("Decoding results from " + resultDir.getAbsolutePath());

        File[] outputFiles = resultDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return prevalenceFilePattern.matcher(file.getName()).matches();
            }
        });

        if (outputFiles.length > 0) {
            System.out.println(" using prevalence CSV.");
            System.out.println(" Number of output files = " + outputFiles.length);

            int numInf = 2;
            int numClassByLoc = 21 * NUM_CLASS;
            int[] numInfectionsStates = {5, 5}; // S, E, A, Y, R
            boolean readFirstLine = false;

            totalCount = new HashMap[numInf];

            for (int i = 0; i < totalCount.length; i++) {
                totalCount[i] = new HashMap<>();
            }

            Arrays.sort(outputFiles, new Comparator<File>() {
                @Override
                public int compare(File t0, File t1) {
                    Matcher m0 = prevalenceFilePattern.matcher(t0.getName());
                    Matcher m1 = prevalenceFilePattern.matcher(t1.getName());
                    m0.matches();
                    m1.matches();
                    String s0 = m0.group(1);
                    String s1 = m1.group(1);
                    return Integer.compare(Integer.parseInt(s0), Integer.parseInt(s1));
                }
            });
            for (File outputPrevalCSV : outputFiles) {

                Matcher fm = prevalenceFilePattern.matcher(outputPrevalCSV.getName());
                fm.matches();
                int simNum = Integer.parseInt(fm.group(1));

                BufferedReader reader = new BufferedReader(new FileReader(outputPrevalCSV));
                String line;
                String[] headerLine = null;

                while ((line = reader.readLine()) != null) {
                    Matcher keyEnt;
                    String[] entries = line.split(",");
                    if (headerLine == null) {
                        if (!readFirstLine) {
                            keyEnt = prevalenceHeaderPattern.matcher(entries[entries.length - 1]);
                            keyEnt.matches();
                            numInf = Integer.parseInt(keyEnt.group(1)) + 1;
                            numClassByLoc = Integer.parseInt(keyEnt.group(2)) + 1;
                            numInfectionsStates = new int[numInf];

                            for (String headerEnt : entries) {
                                keyEnt = prevalenceHeaderPattern.matcher(headerEnt);
                                if (keyEnt.matches()) {
                                    if (Integer.parseInt(keyEnt.group(2)) == 0) { // Only need to check one class
                                        int infIndex = Integer.parseInt(keyEnt.group(1));
                                        int classIndex = Integer.parseInt(keyEnt.group(3));
                                        numInfectionsStates[infIndex] = Math.max(numInfectionsStates[infIndex], classIndex + 2); // Including S                                    
                                    }
                                }
                            }

                            System.out.println("# Infection = " + numInf);
                            System.out.println("# Class by location = " + numClassByLoc);
                            System.out.println("# infection status = " + Arrays.toString(numInfectionsStates));

                            readFirstLine = true;
                            dataStore = new HashMap[numInf];
                            for (int i = 0; i < dataStore.length; i++) {
                                dataStore[i] = new HashMap<>();
                            }
                        }
                        headerLine = entries;

                        //Read the next line
                        line = reader.readLine();
                        if (line != null) {
                            entries = line.split(",");
                        } else {
                            System.err.println("No entry after the header line for " + outputPrevalCSV.getAbsolutePath());
                            headerLine = null;
                        }

                    }
                    if (headerLine != null) {

                        Integer time = Integer.parseInt(entries[0]);
                        int[][] numPerson_inf = new int[numInf][NUM_CLASS];
                        int[][] numPerson_total = new int[numInf][NUM_CLASS];

                        for (int i = 1; i < entries.length; i++) {
                            keyEnt = prevalenceHeaderPattern.matcher(headerLine[i]);

                            if (keyEnt.matches()) {

                                int inf_id = Integer.parseInt(keyEnt.group(1));
                                int class_id = Integer.parseInt(keyEnt.group(2)) % NUM_CLASS;
                                int inf_stat = Integer.parseInt(keyEnt.group(3));

                                numPerson_total[inf_id][class_id] += Integer.parseInt(entries[i]);

                                if (inf_stat != AbstractIndividualInterface.INFECT_S) {
                                    numPerson_inf[inf_id][class_id] += Integer.parseInt(entries[i]);
                                }
                            }
                        }

                        for (int inf_id = 0; inf_id < numInf; inf_id++) {
                            double[][] ent = dataStore[inf_id].get(time);
                            if (ent == null) {
                                ent = new double[NUM_CLASS][outputFiles.length];
                            }

                            for (int cI = 0; cI < NUM_CLASS; cI++) {
                                ent[cI][simNum] = ((double) numPerson_inf[inf_id][cI]) / numPerson_total[inf_id][cI];

                            }

                            dataStore[inf_id].put(time, ent);

                            int[][] totalEnt = totalCount[inf_id].get(time);
                            if (totalEnt == null) {
                                totalEnt = new int[outputFiles.length][2];
                            }

                            for (int cI = 0; cI < NUM_CLASS; cI++) {
                                totalEnt[simNum][0] += numPerson_inf[inf_id][cI];
                                totalEnt[simNum][1] += numPerson_total[inf_id][cI];
                            }
                            
                            totalCount[inf_id].put(time, totalEnt);

                        }

                    }

                }

            }

        } else {
            System.out.println(" using output text file.");
            decodeFromOutputTxt();
        }

        if (totalCount != null) {
            generateTotalPrevalenceCSV();

        }

        if (dataStore != null) {
            generateClassSpecificPrevalCSV();
        }

    }

    protected void generateTotalPrevalenceCSV() throws IOException {
        for (int i = 0; i < totalCount.length; i++) {
            File prevalTotalFile = new File(resultDir, prevalTotalPrefix + Integer.toString(i) + ".csv");
            
            if (prevalTotalFile.exists()) {
                System.out.println("Prevalence for all already existed at " + prevalTotalFile.getAbsolutePath()
                        + ". File not generated. ");
            } else {
                System.out.println("Generrating prevalence for all at " + prevalTotalFile.getAbsolutePath());
                
                Integer[] times = totalCount[i].keySet().toArray(new Integer[totalCount[i].keySet().size()]);
                Arrays.sort(times);
                
                PrintWriter wri = new PrintWriter(new FileWriter(prevalTotalFile));
                boolean header = true;
                StringBuilder printEntry;
                
                for (Integer t : times) {
                    int[][] values = totalCount[i].get(t);
                    printEntry = new StringBuilder();
                    if (header) {
                        printEntry.append("Time");
                        
                        for (int c = 0; c < values.length; c++) {
                            printEntry.append(",Sim ");
                            printEntry.append(c);
                        }
                        wri.println(printEntry.toString());
                        
                        header = false;
                        printEntry = new StringBuilder();
                        
                    }
                    printEntry.append(t.toString());
                    for (int[] value : values) {
                        int[] ent = Arrays.copyOf(value, value.length);
                        printEntry.append(",");
                        printEntry.append(((float) ent[0]) / ent[1]) ;
                    }
                    wri.println(printEntry.toString());
                }
                wri.close();
                
            }

        }
    }

    protected void generateClassSpecificPrevalCSV() throws MathIllegalArgumentException, IOException {
        // Class specific results
        for (int i = 0; i < dataStore.length; i++) {
            File prevalSummaryFile = new File(resultDir, prevalByClassPrefix + Integer.toString(i) + ".csv");

            if (prevalSummaryFile.exists()) {
                System.out.println("Prevalence summary already existed at " + prevalSummaryFile.getAbsolutePath()
                        + ". File not generated. ");
            } else {

                System.out.println("Generrating prevalence summary at " + prevalSummaryFile.getAbsolutePath());

                Integer[] times = dataStore[i].keySet().toArray(new Integer[dataStore[i].keySet().size()]);
                Arrays.sort(times);

                PrintWriter wri = new PrintWriter(new FileWriter(prevalSummaryFile));
                boolean header = true;
                StringBuilder printEntry;
                Percentile data = new Percentile();

                for (Integer t : times) {
                    double[][] values = dataStore[i].get(t);
                    printEntry = new StringBuilder();

                    if (header) {
                        printEntry.append("Time");
                        for (int c = 0; c < values.length; c++) {
                            printEntry.append(",Class_#").append(c).append("_0");
                            printEntry.append(",Class_#").append(c).append("_25");
                            printEntry.append(",Class_#").append(c).append("_50");
                            printEntry.append(",Class_#").append(c).append("_75");
                            printEntry.append(",Class_#").append(c).append("_100");
                        }
                        wri.println(printEntry.toString());
                        header = false;
                        printEntry = new StringBuilder();
                    }
                    printEntry.append(t.toString());
                    for (double[] value : values) {
                        double[] ent = Arrays.copyOf(value, value.length);
                        Arrays.sort(ent);
                        data.setData(ent);
                        printEntry.append(",").append(ent[0]);
                        printEntry.append(",").append(data.evaluate(25));
                        printEntry.append(",").append(data.evaluate(50));
                        printEntry.append(",").append(data.evaluate(75));
                        printEntry.append(",").append(ent[ent.length - 1]);
                    }
                    wri.println(printEntry.toString());
                }
                wri.close();
            }
        }
    }

    protected void decodeFromOutputTxt() throws IOException, NumberFormatException {

        File[] outputFiles = resultDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return outputFilePattern.matcher(file.getName()).matches();
            }
        });

        System.out.println("Number of output files = " + outputFiles.length);

        if (outputFiles.length > 0) {

            Arrays.sort(outputFiles, new Comparator<File>() {
                @Override
                public int compare(File t0, File t1) {
                    Matcher m0 = outputFilePattern.matcher(t0.getName());
                    Matcher m1 = outputFilePattern.matcher(t1.getName());

                    m0.matches();
                    m1.matches();

                    String s0 = m0.group(1);
                    String s1 = m1.group(1);

                    return Integer.compare(Integer.parseInt(s0), Integer.parseInt(s1));

                }
            });

            for (File outputTxt : outputFiles) {
                BufferedReader reader = new BufferedReader(new FileReader(outputTxt));
                int simNum;
                String line;

                Matcher fm = outputFilePattern.matcher(outputTxt.getName());
                fm.matches();
                simNum = Integer.parseInt(fm.group(1));

                while ((line = reader.readLine()) != null) {
                    Matcher m = outputlinePattern.matcher(line);
                    if (m.matches()) {
                        Integer time = new Integer(m.group(1));
                        int infId = Integer.parseInt(m.group(2));
                        String[] entryByClass = m.group(3).split(",");

                        if (dataStore == null) {
                            dataStore = new HashMap[infId + 1];
                        } else if (infId >= dataStore.length) {
                            dataStore = Arrays.copyOf(dataStore, infId + 1);
                        }

                        if (dataStore[infId] == null) {
                            dataStore[infId] = new HashMap<>();
                        }

                        double[][] ent = dataStore[infId].get(time);
                        if (ent == null) {
                            ent = new double[entryByClass.length][outputFiles.length];
                        }

                        for (int cI = 0; cI < entryByClass.length; cI++) {
                            ent[cI][simNum] = Double.parseDouble(entryByClass[cI]);
                        }
                        dataStore[infId].put(time, ent);
                    }

                }

            }
        }
    }

    public static void main(String[] arg) throws FileNotFoundException, IOException {

        String baseDir = "C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\TTANGO\\TTANGO_Srn";

        File[] folders = new File(baseDir).listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        Test_Population_Remote_MetaPopulation_Pop_Preval_CSV decoder;
        for (File f : folders) {
            decoder = new Test_Population_Remote_MetaPopulation_Pop_Preval_CSV(f);
            decoder.decode();
        }

    }

}
