/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package run;

import infection.AbstractInfection;
import infection.COVID19_Remote_Infection;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import population.Population_Remote_MetaPopulation_COVID19;
import sim.SimulationInterface;
import static sim.SimulationInterface.PROP_NUM_SIM_PER_SET;
import static sim.SimulationInterface.PROP_NUM_SNAP;
import static sim.SimulationInterface.PROP_SNAP_FREQ;

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
        System.out.println(String.format("# simulation: %d with running %d in parallel\n"
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

            Thread_PopRun_COVID19 thread = new Thread_PopRun_COVID19(r, baseDir, numSnap, snapFreq);
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
                executor.submit(thread);

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
    }

}
