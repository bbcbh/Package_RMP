package run;

import java.io.File;

import java.io.IOException;
import java.util.Arrays;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import random.MersenneTwisterRandomGenerator;
import util.PropValUtils;

public class Run_Population_Remote_MetaPopulation_Pop_Generate {

    public static final long BASE_SEED = 2251912970037127827l;

    public int NUM_SIM_TOTAL;
    public int NUM_BURN_IN_STEPS;
    public String DIR_PATH;
    public int MAX_THREAD = -1;

    int[] popSize = null;
    int[] popType = null;
    int[][] popConnc = null;

    public void setPopSize(int[] popSize) {
        this.popSize = popSize;
    }

    public int[] getPopSize() {
        return popSize;
    }

    public int[] getPopType() {
        return popType;
    }

    public void setPopType(int[] popType) {
        this.popType = popType;
    }

    public int[][] getPopConnc() {
        return popConnc;
    }

    public void setPopConnc(int[][] popConnc) {
        this.popConnc = popConnc;
    }

    public static void runPopGenerate(String[] arg) throws IOException, ClassNotFoundException, InterruptedException {

        int NUM_SIM_TOTAL = 1000;
        int NUM_BURN_IN_STEPS = 360 * 50;
        String DIR_PATH = "~/RMP/BasePop";
        int MAX_THREAD = -1;
        String popSizeStr = null;
        String popTypeStr = null;
        String popConncStr = null;

        if (arg.length > 1) {
            if (!arg[0].isEmpty()) {
                NUM_SIM_TOTAL = Integer.parseInt(arg[0]);

            }
        }

        if (arg.length > 2) {
            if (!arg[1].isEmpty()) {
                NUM_BURN_IN_STEPS = Integer.parseInt(arg[1]);

            }
        }

        if (arg.length > 2) {
            if (!arg[2].isEmpty()) {
                DIR_PATH = arg[2];
            }
        }

        if (arg.length > 3) {
            if (!arg[3].isEmpty()) {
                MAX_THREAD = Integer.parseInt(arg[3]);
            }
        }

        if (arg.length > 4) {
            if (!arg[4].isEmpty()) {
                popSizeStr = arg[4];
            }
        }

        if (arg.length > 5) {
            if (!arg[5].isEmpty()) {
                popTypeStr = arg[5];
            }
        }

        if (arg.length > 6) {
            if (!arg[6].isEmpty()) {
                popConncStr = arg[6];
            }
        }

        System.out.println("NUM_SIM_TOTAL = " + NUM_SIM_TOTAL);
        System.out.println("NUM_BURN_IN_STEPS = " + NUM_BURN_IN_STEPS);
        System.out.println("DIR_PATH = " + DIR_PATH);
        System.out.println("MAX_THREAD = " + MAX_THREAD);

        Run_Population_Remote_MetaPopulation_Pop_Generate popGen
                = new Run_Population_Remote_MetaPopulation_Pop_Generate(NUM_SIM_TOTAL, NUM_BURN_IN_STEPS, DIR_PATH, MAX_THREAD);

        if (popSizeStr != null) {
            popGen.setPopSize((int[]) PropValUtils.propStrToObject(popSizeStr, int[].class));
        }

        if (popTypeStr != null) {
            popGen.setPopType((int[]) PropValUtils.propStrToObject(popTypeStr, int[].class));
        }

        if (popConncStr != null) {
            popGen.setPopConnc((int[][]) PropValUtils.propStrToObject(popConncStr, int[][].class));
        }

        System.out.println("POP_SIZE = " + Arrays.toString(popGen.getPopSize()));
        System.out.println("POP_TYPE = " + Arrays.toString(popGen.getPopType()));
        System.out.println("POP_CONNC = " + Arrays.deepToString(popGen.getPopConnc()));

        popGen.genPops();

    }

    public Run_Population_Remote_MetaPopulation_Pop_Generate(int NUM_SIM_TOTAL,
            int NUM_BURN_IN_STEPS, String DIR_PATH, int MAX_THREAD) {
        this.NUM_SIM_TOTAL = NUM_SIM_TOTAL;
        this.NUM_BURN_IN_STEPS = NUM_BURN_IN_STEPS;
        this.DIR_PATH = DIR_PATH;
        this.MAX_THREAD = MAX_THREAD;
    }

    private void genPops() throws InterruptedException {

        MersenneTwisterRandomGenerator rng = new MersenneTwisterRandomGenerator(BASE_SEED);
        File destDir = new File(DIR_PATH);
        destDir.mkdirs();

        int NUM_THREADS = Runtime.getRuntime().availableProcessors();

        if (MAX_THREAD > 0 && MAX_THREAD < NUM_THREADS) {
            NUM_THREADS = MAX_THREAD;
        }

        ExecutorService executor = null;
        int numInExe = 0;

        long tic = System.currentTimeMillis();

        for (int s = 0; s < NUM_SIM_TOTAL; s++) {
            if (executor == null) {
                executor = Executors.newFixedThreadPool(NUM_THREADS);
                numInExe = 0;
            }

            File popFile = new File(destDir, "pop_S" + s + ".zip");
            if (popFile.exists()) {                               
                System.out.println(popFile.getAbsolutePath() + " already exist. Thread with RNG seed of " + rng.nextLong() +  " not generated.");
            } else {
                System.out.println("Submiting thread for generation of pop #" + s);
                Thread_PopGenRemote thread = new Thread_PopGenRemote(s, NUM_BURN_IN_STEPS, DIR_PATH, rng.nextLong(),
                        popSize, popType, popConnc);
                executor.submit(thread);
                numInExe++;

                if (numInExe == NUM_THREADS) {
                    executor.shutdown();
                    if (!executor.awaitTermination(3, TimeUnit.DAYS)) {
                        System.out.println("Inf Thread time-out!");
                    }
                    System.out.println("Excution of " + numInExe + " thread(s) terminated.");
                    executor = null;
                    numInExe = 0;
                }

            }
        }

        if (executor != null) {
            executor.shutdown();
            if (!executor.awaitTermination(3, TimeUnit.DAYS)) {
                System.out.println("Inf Thread time-out!");
            }
            System.out.println("Excution of " + numInExe + " thread(s) terminated.");

        }

        System.out.println("Time required for population generation (s) = " + (System.currentTimeMillis() - tic) / 1000f);

    }

}
