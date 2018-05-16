package test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import population.Population_Remote_MetaPopulation;
import run.Run_Population_Remote_MetaPopulation_Pop_Generate;
import util.FileZipper;

/**
 *
 * @author Ben Hui
 */
public class Test_Population_Remote_MetaPopulation_Pop_Generate {

    public static void main(String[] arg) throws IOException, ClassNotFoundException, InterruptedException {

        int TEST_STEP = 0;
        String[] rArg = new String[]{"", "", "C:\\Users\\Bhui\\Desktop\\VM_FTP\\RMP\\BasePop", "8"};

        Run_Population_Remote_MetaPopulation_Pop_Generate.main(rArg);

        if (TEST_STEP > 0) {
            File importPopFile = new File(rArg[2], "pop_S0.zip");
            importPopFile = FileZipper.unzipFile(importPopFile, importPopFile.getParentFile());
            ObjectInputStream oIStream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(importPopFile)));
            Population_Remote_MetaPopulation savedPop = Population_Remote_MetaPopulation.decodeFromStream(oIStream);
            oIStream.close();

            oIStream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(importPopFile)));
            Population_Remote_MetaPopulation savedPop2 = Population_Remote_MetaPopulation.decodeFromStream(oIStream);
            oIStream.close();

            System.out.println("Pre_step RNG savedPop  = " + savedPop.getRNG().nextLong());
            System.out.println("Pre_step RNG savedPop2 = " + savedPop2.getRNG().nextLong());

            for (int i = 0; i < TEST_STEP; i++) {

                savedPop.advanceTimeStep(1);
                savedPop2.advanceTimeStep(1);

            }
            System.out.println("RNG savedPop  = " + savedPop.getRNG().nextLong());
            System.out.println("RNG savedPop2 = " + savedPop2.getRNG().nextLong());

        }

    }
}
