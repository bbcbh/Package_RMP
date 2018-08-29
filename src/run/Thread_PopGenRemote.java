package run;

import infection.AbstractInfection;
import infection.ChlamydiaInfection;
import infection.GonorrhoeaInfection;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import population.Population_Remote_MetaPopulation;
import util.FileZipper;

/**
 *
 * @author Ben Hui
 */
class Thread_PopGenRemote implements Runnable {

    int numBurnInSteps;
    String baseDir;
    int simId;
    long seed;
    int[] popSize = new int[]{15000, // Default
        500, 500, 500, 500,
        500, 500, 500, 500,
        500, 500, 500, 500,
        500, 500, 500, 500,
        500, 500, 500, 500,};

    public Thread_PopGenRemote(int simId, int numStep, String dirPath, long seed,
            int[] popSize) {

        this.numBurnInSteps = numStep;
        this.baseDir = dirPath;
        this.simId = simId;
        this.seed = seed;
        if (popSize != null) {
            this.popSize = popSize;
        }
    }

    @Override
    public void run() {
        Population_Remote_MetaPopulation pop;
        pop = new Population_Remote_MetaPopulation(seed);

        int[] popType = new int[popSize.length];
        int[][] popConnc = new int[popSize.length][popSize.length];
        int[][] popDecomp = new int[popSize.length][];
        float[][] popAway = new float[popSize.length][];

        for (int p = 0; p < popSize.length; p++) {
            popType[p] = p == 0 ? 2 : 3;
            popDecomp[p] = p == 0
                    ? util.Factory_Population_Decomposition.getDecomposition(util.Factory_Population_Decomposition.POP_COMPOSITION_REGIONAL_2010, popSize[p], pop.getRNG())
                    : util.Factory_Population_Decomposition.getDecomposition(util.Factory_Population_Decomposition.POP_COMPOSITION_REMOTE_2010, popSize[p], pop.getRNG());

            // From Biddle 2009
            //  Male: 15-19, 20-24, 25-29, 30-34,  Female: 15-19, 20-24, 25-29, 30-34
            popAway[p] = p == 0
                    ? new float[]{0.08f, 0.10f, 0.09f, 0.08f, 0.08f, 0.07f, 0.06f, 0.05f}
                    : new float[]{0.11f, 0.10f, 0.09f, 0.09f, 0.11f, 0.10f, 0.09f, 0.08f};

            for (int c = 0; c < popConnc[p].length; c++) {
                if (c == p) {
                    popConnc[p][c] = 0;
                } else if (p == 0 || c == 0) {
                    popConnc[p][c] = 1;
                } else {
                    popConnc[p][c] = -1;
                }
            }

        }

        pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_POP_SIZE]
                = popSize;

        pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_POP_TYPE]
                = popType;

        pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_POP_CONNC]
                = popConnc;

        pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_AGE_GENDER_COMPOSITION]
                = popDecomp;

        pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_AWAY_FROM_HOME_BY_LOCATION]
                = popAway;

        pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_INFECTION_LIST]
                = new AbstractInfection[]{
                    new ChlamydiaInfection(pop.getInfectionRNG()),
                    new GonorrhoeaInfection(pop.getInfectionRNG())};

        pop.initialise();
        pop.advanceTimeStep(1);
        for (int i = 0; i < numBurnInSteps; i++) {
            pop.advanceTimeStep(1);
        }

        String popName = "pop_S" + simId;
        File zipFile = new File(baseDir, popName + ".zip");
        File tempFile = new File(baseDir, popName);

        //System.out.println("Zipping pop #" + simId + " to " + zipFile.getAbsolutePath());
        try (final ObjectOutputStream outStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile)))) {
            pop.encodePopToStream(outStream);
            outStream.close();
            FileZipper.zipFile(tempFile, zipFile);
        } catch (IOException ex) {
            ex.printStackTrace(System.out);
        }
        System.out.println("File exported to " + zipFile.getAbsolutePath());
        tempFile.delete();
    }

}
