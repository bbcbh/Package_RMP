/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package run;

import infection.AbstractInfection;
import infection.COVID19_Remote_Infection;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import population.Population_Remote_MetaPopulation_COVID19;
import sim.SimulationInterface;
import static sim.SimulationInterface.PROP_NUM_SIM_PER_SET;
import static sim.SimulationInterface.PROP_NUM_SNAP;
import static sim.SimulationInterface.PROP_SNAP_FREQ;
import util.FileZipper;

/**
 *
 * @author Ben Hui
 */
public class Run_Population_Remote_MetaPopulation_COVID19 {

    protected final File baseDir;
    protected final String[] propModelInitStr;
    protected final Object[] propVal;

    public Run_Population_Remote_MetaPopulation_COVID19(File baseDir, Object[] propVal, String[] propModelInitStr) {
        this.baseDir = baseDir;
        this.propModelInitStr = propModelInitStr;
        this.propVal = propVal;
    }

    public void generateOneResultSet() throws IOException, InterruptedException {

        long tic = System.currentTimeMillis();
        int numSimPerSet = (int) propVal[PROP_NUM_SIM_PER_SET];
        int numSnap = (int) propVal[PROP_NUM_SNAP];
        int snapFreq = (int) propVal[PROP_SNAP_FREQ];
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

            if (numProcess <= 1) {
                thread.run();
            } else {

                File statFile = new File(baseDir,
                        String.format(Thread_PopRun_COVID19.FILE_REGEX_SNAP_STAT, thread.getThreadId()));

                if (!statFile.exists() || statFile.length() == 0) {
                    executor.submit(thread);
                    numInExe++;
                } else {
                    System.out.println(String.format("Thread #%d skipped as output file %s of size %d already present.", 
                            thread.getThreadId(), statFile.getName(), statFile.length()));
                }

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

        if (numProcess > 1) {
            // Zip output files
            String[] file_regex = new String[]{
                Thread_PopRun_COVID19.FILE_REGEX_OUTPUT,
                Thread_PopRun_COVID19.FILE_REGEX_SNAP_STAT,
                Thread_PopRun_COVID19.FILE_REGEX_TEST_STAT
            };

            for (String regex : file_regex) {

                File[] collection = baseDir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        
                        return pathname.getName().matches(regex.replaceAll("%d", "\\\\d"));
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

                    if (zipFile.exists() && zipFile.length() > 0) {
                        for (File srcFile : collection) {
                            srcFile.delete();
                        }
                    }

                }

            }

        }

    }

}
