package run;

import infection.AbstractInfection;
import infection.COVID19_Remote_Infection;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import population.Population_Remote_MetaPopulation_COVID19;
import sim.SimulationInterface;
import static sim.SimulationInterface.PROP_NUM_SIM_PER_SET;
import static sim.SimulationInterface.PROP_NUM_SNAP;
import static sim.SimulationInterface.PROP_SNAP_FREQ;
import sim.Simulation_Remote_MetaPopulation;

/**
 *
 * @author Ben Hui
 */
public class Run_Population_Remote_MetaPopulation_COVID19 {

    public static final String DECODE_FILE_REGEX_INCIDENT_BY_LOC = "Cumul_Incident_loc_%d.csv";
    public static final String DECODE_FILE_REGEX_PREVALENCE_BY_LOC = "Prevalence_loc_%d.csv";
    public static final String DECODE_FILE_REGEX_TEST_BY_LOC = "Cumul_test_loc_%d.csv";
    public static final String DECODE_FILE_REGEX_POSITIVE_TEST_BY_LOC = "Cumul_pos_test_loc_%d.csv";
    public static final String DECODE_FILE_REGEX_IN_QUARANTINE_BY_LOC = "In_quarantine_loc_%d.csv";
    public static final String DECODE_FILE_REGEX_RESPONSE_QUEUE_BY_LOC = "Resposnse_queue_loc_%d.csv";
    public static final String DECODE_FILE_REGEX_LOCKDOWN_INTER_BY_LOC = "lockdown_inter_meta_pop_loc_%d.csv";
    public static final String DECODE_FILE_REGEX_LOCKDOWN_WITHIN_BY_LOC = "lockdown_within_meta_pop_loc_%d.csv";

    public static void decodeZipCSVByLoc(File basedir,
            int numPop, int timeSteps) throws FileNotFoundException, IOException, NumberFormatException {

        File srcZipFile;
        Pattern pattern;
        ZipFile srcZip;
        Enumeration<? extends ZipEntry> zipEntryEnum;
        int numSim;
        String line;
        PrintWriter[] pri = new PrintWriter[numPop];

        // SnapStat
        srcZipFile = new File(basedir, Thread_PopRun_COVID19.FILE_REGEX_SNAP_STAT.replaceAll("%d", "All") + ".zip");

        if (srcZipFile.exists()) {

            pattern = Pattern.compile(Thread_PopRun_COVID19.FILE_REGEX_SNAP_STAT.replaceAll("%d", "(\\\\d+)"));
            srcZip = new ZipFile(srcZipFile);
            zipEntryEnum = srcZip.entries();
            numSim = srcZip.size();

            float[][][] prevalEnt = new float[numPop][timeSteps + 1][numSim];
            int[][][] incidentEnt = new int[numPop][timeSteps + 1][numSim];
            int[][][] inQuarantineEnt = new int[numPop][timeSteps + 1][numSim];
            int[][][] inlockdown_inter = new int[numPop][timeSteps + 1][numSim];
            int[][][] inlockdown_within = new int[numPop][timeSteps + 1][numSim];

            while (zipEntryEnum.hasMoreElements()) {
                ZipEntry zipEnt = zipEntryEnum.nextElement();
                Matcher m = pattern.matcher(zipEnt.getName());
                if (m.matches()) {
                    int simIndex = Integer.parseInt(m.group(1));
                    //System.out.println(zipEnt.getName());
                    try (final BufferedReader reader = new BufferedReader(new InputStreamReader(srcZip.getInputStream(zipEnt)))) {
                        int lineNum = 0;
                        while ((line = reader.readLine()) != null) {
                            if (lineNum != 0 && line.length() > 0) {
                                String[] lineEnt = line.split(",");
                                int t = Integer.parseInt(lineEnt[0]);
                                for (int p = 0; p < numPop; p++) {
                                    prevalEnt[p][t][simIndex] = Float.parseFloat(lineEnt[p + numPop + 1]) / Float.parseFloat(lineEnt[p + 1]);
                                    incidentEnt[p][t][simIndex] = Integer.parseInt(lineEnt[1 + p + 4 * numPop + 1]);
                                    inQuarantineEnt[p][t][simIndex] = Integer.parseInt(lineEnt[1 + p + 5 * numPop + 1]);
                                    inlockdown_inter[p][t][simIndex] = Integer.parseInt(lineEnt[1 + p + 6 * numPop + 1]);
                                    inlockdown_within[p][t][simIndex] = Integer.parseInt(lineEnt[1 + p + 7 * numPop + 1]);
                                }
                            }
                            lineNum++;
                        }
                    }
                }
            }

            for (int p = 0; p < numPop; p++) {
                pri[p] = new PrintWriter(new File(basedir, String.format(DECODE_FILE_REGEX_PREVALENCE_BY_LOC, p)));
                pri[p].println("Time, Prevalence by sim");
                for (int t = 0; t < prevalEnt[p].length; t++) {
                    pri[p].print(t);
                    for (int s = 0; s < prevalEnt[p][t].length; s++) {
                        pri[p].print(',');
                        pri[p].print(prevalEnt[p][t][s]);
                    }
                    pri[p].println();
                }
                pri[p].close();

                pri[p] = new PrintWriter(new File(basedir, String.format(DECODE_FILE_REGEX_INCIDENT_BY_LOC, p)));
                pri[p].println("Time, Cumulative incident by sim");
                for (int t = 0; t < incidentEnt[p].length; t++) {
                    pri[p].print(t);
                    for (int s = 0; s < incidentEnt[p][t].length; s++) {
                        pri[p].print(',');
                        pri[p].print(incidentEnt[p][t][s]);
                    }
                    pri[p].println();
                }
                pri[p].close();

                pri[p] = new PrintWriter(new File(basedir, String.format(DECODE_FILE_REGEX_IN_QUARANTINE_BY_LOC, p)));
                pri[p].println("Time, In quarantine by sim");
                for (int t = 0; t < inQuarantineEnt[p].length; t++) {
                    pri[p].print(t);
                    for (int s = 0; s < inQuarantineEnt[p][t].length; s++) {
                        pri[p].print(',');
                        pri[p].print(inQuarantineEnt[p][t][s]);
                    }
                    pri[p].println();
                }
                pri[p].close();

                pri[p] = new PrintWriter(new File(basedir, String.format(DECODE_FILE_REGEX_LOCKDOWN_INTER_BY_LOC, p)));
                pri[p].println("Time, In lockdown (inter) by sim");
                for (int t = 0; t < inlockdown_inter[p].length; t++) {
                    pri[p].print(t);
                    for (int s = 0; s < inlockdown_inter[p][t].length; s++) {
                        pri[p].print(',');
                        pri[p].print(inlockdown_inter[p][t][s]);
                    }
                    pri[p].println();
                }
                pri[p].close();

                pri[p] = new PrintWriter(new File(basedir, String.format(DECODE_FILE_REGEX_LOCKDOWN_WITHIN_BY_LOC, p)));
                pri[p].println("Time, In lockdown (within) by sim");
                for (int t = 0; t < inlockdown_within[p].length; t++) {
                    pri[p].print(t);
                    for (int s = 0; s < inlockdown_within[p][t].length; s++) {
                        pri[p].print(',');
                        pri[p].print(inlockdown_within[p][t][s]);
                    }
                    pri[p].println();
                }
                pri[p].close();

            }
        }

        // Test stat 
        srcZipFile = new File(basedir, Thread_PopRun_COVID19.FILE_REGEX_TEST_STAT.replaceAll("%d", "All") + ".zip");
        if (srcZipFile.exists()) {
            pattern = Pattern.compile(Thread_PopRun_COVID19.FILE_REGEX_TEST_STAT.replaceAll("%d", "(\\\\d+)"));
            srcZip = new ZipFile(srcZipFile);
            zipEntryEnum = srcZip.entries();
            numSim = srcZip.size();

            int[][][] numTest = new int[numPop][timeSteps + 1][numSim];
            int[][][] testPositive = new int[numPop][timeSteps + 1][numSim];
            int[][][] inResponseQueue = new int[numPop][timeSteps + 1][numSim];

            while (zipEntryEnum.hasMoreElements()) {
                ZipEntry zipEnt = zipEntryEnum.nextElement();
                Matcher m = pattern.matcher(zipEnt.getName());
                if (m.matches()) {
                    int simIndex = Integer.parseInt(m.group(1));
                    //System.out.println(zipEnt.getName());
                    try (final BufferedReader reader = new BufferedReader(new InputStreamReader(srcZip.getInputStream(zipEnt)))) {
                        int lineNum = 0;
                        while ((line = reader.readLine()) != null) {
                            if (lineNum != 0 && line.length() > 0) {
                                String[] lineEnt = line.split(",");
                                int t = Integer.parseInt(lineEnt[0]);
                                for (int p = 0; p < numPop; p++) {
                                    numTest[p][t][simIndex] = Integer.parseInt(lineEnt[1 + p]);
                                    testPositive[p][t][simIndex] = Integer.parseInt(lineEnt[1 + numPop + p]);
                                    inResponseQueue[p][t][simIndex] = Integer.parseInt(lineEnt[1 + 2 * numPop + p]);

                                }
                            }
                            lineNum++;
                        }
                    }
                }
            }

            for (int p = 0; p < numPop; p++) {
                pri[p] = new PrintWriter(new File(basedir, String.format(DECODE_FILE_REGEX_TEST_BY_LOC, p)));
                pri[p].println("Time, Cumulative test by sim");
                for (int t = 0; t < numTest[p].length; t++) {
                    pri[p].print(t);
                    for (int s = 0; s < numTest[p][t].length; s++) {
                        pri[p].print(',');
                        pri[p].print(numTest[p][t][s]);
                    }
                    pri[p].println();
                }
                pri[p].close();

                pri[p] = new PrintWriter(new File(basedir, String.format(DECODE_FILE_REGEX_POSITIVE_TEST_BY_LOC, p)));
                pri[p].println("Time, Cumulative positive test by sim");
                for (int t = 0; t < testPositive[p].length; t++) {
                    pri[p].print(t);
                    for (int s = 0; s < testPositive[p][t].length; s++) {
                        pri[p].print(',');
                        pri[p].print(testPositive[p][t][s]);
                    }
                    pri[p].println();
                }
                pri[p].close();

                pri[p] = new PrintWriter(new File(basedir, String.format(DECODE_FILE_REGEX_RESPONSE_QUEUE_BY_LOC, p)));
                pri[p].println("Time, Number in response queue by sim");
                for (int t = 0; t < inResponseQueue[p].length; t++) {
                    pri[p].print(t);
                    for (int s = 0; s < inResponseQueue[p][t].length; s++) {
                        pri[p].print(',');
                        pri[p].print(inResponseQueue[p][t][s]);
                    }
                    pri[p].println();
                }
                pri[p].close();

            }

        }

    }

    protected final File baseDir;
    protected final String[] propModelInitStr;
    protected final Object[] propVal;
    protected boolean removeAfterZip = true;
    protected boolean clearPrevResult = false;

    public void setRemoveAfterZip(boolean removeAfterZip) {
        this.removeAfterZip = removeAfterZip;
    }

    public void setClearPrevResult(boolean clearPrevResult) {
        this.clearPrevResult = clearPrevResult;
    }

    public Run_Population_Remote_MetaPopulation_COVID19(File baseDir, Object[] propVal, String[] propModelInitStr) {
        this.baseDir = baseDir;
        this.propModelInitStr = propModelInitStr;
        this.propVal = propVal;
    }

    public void generateOneResultSet() throws IOException, InterruptedException {

        if (clearPrevResult) {
            File[] fileToClear = baseDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return !pathname.getName().equals(Simulation_Remote_MetaPopulation.FILENAME_PROP);
                }
            });

            for (File del : fileToClear) {
                del.delete();
            }
        }

        File zipSnapFilename = new File(baseDir, Thread_PopRun_COVID19.FILE_REGEX_OUTPUT.replace("%d", "All") + ".zip");
        int[] popSize = null;

        int numSnap = (int) propVal[PROP_NUM_SNAP];
        int snapFreq = (int) propVal[PROP_SNAP_FREQ];

        if (zipSnapFilename.exists()) {
            System.out.println("Simulation not run at " + baseDir.getAbsolutePath()
                    + "as results already exist at " + zipSnapFilename.getAbsolutePath());

            popSize = (int[]) util.PropValUtils.propStrToObject(
                    propModelInitStr[Population_Remote_MetaPopulation_COVID19.FIELDS_REMOTE_METAPOP_POP_SIZE], int[].class);

        } else {
            int numSimPerSet = (int) propVal[PROP_NUM_SIM_PER_SET];

            long tic = System.currentTimeMillis();

            int numProcess = Math.min((int) propVal[SimulationInterface.PROP_USE_PARALLEL], Runtime.getRuntime().availableProcessors());

            random.MersenneTwisterRandomGenerator rng = new random.MersenneTwisterRandomGenerator(2251913970037127827l);

            System.out.println("Running result as defined in " + baseDir.getAbsolutePath());
            System.out.println(String.format("# simulation: %d, with running %d simulation(s) in parallel\n"
                    + "# time step: %d snapshots x %d days per snap = %d",
                    numSimPerSet, numProcess, numSnap, snapFreq, numSnap * snapFreq));

            System.out.println("Start simulations");

            ExecutorService executor = null;
            int numInExe = 0;

            for (int r = 0; r < numSimPerSet; r++) {

                if (executor == null) {
                    executor = Executors.newFixedThreadPool(numProcess);
                    numInExe = 0;
                }

                // Set up population
                Population_Remote_MetaPopulation_COVID19 pop = new Population_Remote_MetaPopulation_COVID19(rng.nextLong());

                // Population parameter
                for (int f = 0; f < Math.min(propModelInitStr.length, pop.getFields().length); f++) {
                    if (propModelInitStr[f] != null) {
                        pop.getFields()[f]
                                = util.PropValUtils.propStrToObject(propModelInitStr[f], pop.getFieldClass(f));
                    }
                };

                COVID19_Remote_Infection covid19 = new COVID19_Remote_Infection(pop.getInfectionRNG());

                // Infection parameter
                for (int f = pop.getFields().length; f < Math.min(propModelInitStr.length,
                        pop.getFields().length + covid19.DIST_TOTAL); f++) {
                    if (propModelInitStr[f] != null) {
                        String key;
                        key = COVID19_Remote_Infection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999",
                                Integer.toString(f - pop.getFields().length));
                        covid19.setParameter(key, util.PropValUtils.propStrToObject(propModelInitStr[f], double[].class));

                    }
                }

                Thread_PopRun_COVID19 thread = new Thread_PopRun_COVID19(r, baseDir, numSnap, snapFreq, numProcess <= 1);
                thread.setPop(pop);
                thread.setInfList(new AbstractInfection[]{covid19});

                // Thread parameter
                int threadOffset = pop.getFields().length + covid19.DIST_TOTAL;
                for (int f = threadOffset; f < Math.min(propModelInitStr.length, threadOffset + thread.getThreadParam().length); f++) {
                    if (propModelInitStr[f] != null) {
                        int threadIndex = f - threadOffset;
                        thread.getThreadParam()[threadIndex] = util.PropValUtils.propStrToObject(propModelInitStr[f],
                                thread.getThreadParam()[threadIndex].getClass());

                    }
                }

                if (popSize == null) {
                    popSize = (int[]) pop.getFields()[Population_Remote_MetaPopulation_COVID19.FIELDS_REMOTE_METAPOP_POP_SIZE];
                }

                File statFile = new File(baseDir,
                        String.format(Thread_PopRun_COVID19.FILE_REGEX_SNAP_STAT, thread.getThreadId()));
                File testFile = new File(baseDir,
                        String.format(Thread_PopRun_COVID19.FILE_REGEX_TEST_STAT, thread.getThreadId()));

                boolean runSim = !statFile.exists() || statFile.length() == 0
                        || !testFile.exists() || testFile.length() == 0;

                if (runSim) {
                    if (numProcess <= 1) {
                        thread.run();
                    } else {
                        executor.submit(thread);
                        numInExe++;
                        if (numInExe == numProcess) {
                            try {
                                executor.shutdown();
                                if (!executor.awaitTermination(2, TimeUnit.DAYS)) {
                                    System.err.println("Inf Thread time-out!");
                                }
                            } catch (InterruptedException ex) {
                                ex.printStackTrace(System.err);
                            }
                            executor = null;
                        }
                    }
                } else {
                    System.out.println(String.format("Thread #%d skipped as output file %s of size %d and testing file %s of size %d already present.",
                            thread.getThreadId(), statFile.getName(), statFile.length(), testFile.getName(), testFile.length()));
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
            }

            System.out.println(String.format("Simulation completed. Time needed = %.4f min",
                    (System.currentTimeMillis() - tic) / (1000f * 60)));

            // Zip output files
            String[] file_regex = new String[]{
                Thread_PopRun_COVID19.FILE_REGEX_OUTPUT,
                Thread_PopRun_COVID19.FILE_REGEX_SNAP_STAT,
                Thread_PopRun_COVID19.FILE_REGEX_TEST_STAT,
                Thread_PopRun_COVID19.FILE_REGEX_POP_SNAP,
            };

            for (String regex : file_regex) {

                File[] collection = baseDir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.getName().matches(regex.replaceAll("%d", "\\\\d+"));
                    }
                });

                if (collection.length > 0) {
                    String zipFileName = regex.replace("%d", "All") + ".zip";
                    File zipFile = new File(baseDir, zipFileName);

                    Path p = Files.createFile(zipFile.toPath());

                    try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
                        for (File srcFile : collection) {
                            ZipEntry zipEntry = new ZipEntry(srcFile.getName());
                            try {
                                zs.putNextEntry(zipEntry);
                                Files.copy(srcFile.toPath(), zs);
                                zs.closeEntry();
                            } catch (IOException e) {
                                System.err.println(e);
                            }
                        }
                    }

                    System.out.println(String.format("Zipping %d file(s) that matches with '%s' to %s",
                            collection.length, regex, zipFile.getAbsolutePath()));

                    if (this.removeAfterZip) {
                        if (zipFile.exists() && zipFile.length() > 0) {
                            for (File srcFile : collection) {
                                srcFile.delete();
                            }
                        }
                    }

                }

            }

        }

        if (popSize != null) {
            Run_Population_Remote_MetaPopulation_COVID19.decodeZipCSVByLoc(baseDir, popSize.length, numSnap * snapFreq);
        }

    }

}
