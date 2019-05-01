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
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

/**
 *
 * @author Ben Hui
 */
public class Test_Population_Remote_MetaPopulation_Pop_Preval_CSV {

    File resultDir;
    final Pattern outputFilePattern = Pattern.compile("output_(\\d+)\\.txt");
    final Pattern outputlinePattern = Pattern.compile("(\\d+) : Preval for infection #(\\d+):(.*)");
    final String prevaleSummaryPrefix = "prevalence_summary_inf_";

    HashMap<Integer, double[][]>[] dataStore; // Integer = time, float[class][simNum]

    public Test_Population_Remote_MetaPopulation_Pop_Preval_CSV(File resultDir) {
        this.resultDir = resultDir;
    }

    public void decode() throws FileNotFoundException, IOException {
        System.out.println("Decoding results from " + resultDir.getAbsolutePath());

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

            // Results analysis
            for (int i = 0; i < dataStore.length; i++) {
                File prevalSummaryFile = new File(resultDir, prevaleSummaryPrefix + Integer.toString(i) + ".csv");
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

    public static void main(String[] arg) throws FileNotFoundException, IOException {

        String baseDir = "C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\NG_CT";

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
