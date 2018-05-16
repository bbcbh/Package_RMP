/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
 * @author bhui
 */
class Thread_PopGen implements Runnable {

    int numBurnInSteps;
    String baseDir;
    int simId;
    long seed;

    public Thread_PopGen(int simId, int numStep, String dirPath, long seed) {

        this.numBurnInSteps = numStep;
        this.baseDir = dirPath;
        this.simId = simId;
        this.seed = seed;
    }

    @Override
    public void run() {
        Population_Remote_MetaPopulation pop;
        pop = new Population_Remote_MetaPopulation(seed);
        pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_POP_SIZE] = new int[]{30000, 500, 500, 500, 500};
        pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_POP_TYPE] = new int[]{2, 3, 3, 3, 3};
        pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_POP_CONNC]
                = new int[][]{
                    new int[]{0, 1, 1, 1, 1},
                    new int[]{1, 0, -1, -1, -1},
                    new int[]{1, -1, 0, -1, -1},
                    new int[]{1, -1, -1, 0, -1},
                    new int[]{1, -1, -1, -1, 0}};

        pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_AGE_GENDER_COMPOSITION]
                = new int[][]{
                    util.Factory_Population_Decomposition.getDecomposition(util.Factory_Population_Decomposition.POP_COMPOSITION_REGIONAL_2010, 30000, pop.getRNG()),
                    util.Factory_Population_Decomposition.getDecomposition(util.Factory_Population_Decomposition.POP_COMPOSITION_REMOTE_2010, 500, pop.getRNG()),
                    util.Factory_Population_Decomposition.getDecomposition(util.Factory_Population_Decomposition.POP_COMPOSITION_REMOTE_2010, 500, pop.getRNG()),
                    util.Factory_Population_Decomposition.getDecomposition(util.Factory_Population_Decomposition.POP_COMPOSITION_REMOTE_2010, 500, pop.getRNG()),
                    util.Factory_Population_Decomposition.getDecomposition(util.Factory_Population_Decomposition.POP_COMPOSITION_REMOTE_2010, 500, pop.getRNG())};

        // From Biddle 2009
        pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_AWAY_FROM_HOME_BY_LOCATION]
                = //  Male: 15-19, 20-24, 25-29, 30-34,  Female: 15-19, 20-24, 25-29, 30-34
                new float[][]{
                    new float[]{0.08f, 0.10f, 0.09f, 0.08f, 0.08f, 0.07f, 0.06f, 0.05f},
                    new float[]{0.11f, 0.10f, 0.09f, 0.09f, 0.11f, 0.10f, 0.09f, 0.08f},
                    new float[]{0.11f, 0.10f, 0.09f, 0.09f, 0.11f, 0.10f, 0.09f, 0.08f},
                    new float[]{0.11f, 0.10f, 0.09f, 0.09f, 0.11f, 0.10f, 0.09f, 0.08f},
                    new float[]{0.11f, 0.10f, 0.09f, 0.09f, 0.11f, 0.10f, 0.09f, 0.08f},};

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
