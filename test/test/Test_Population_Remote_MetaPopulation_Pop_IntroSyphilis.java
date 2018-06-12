package test;

import java.io.File;
import java.io.IOException;
import run.Run_Population_Remote_MetaPopulation_Pop_Intro_Syphilis;

public class Test_Population_Remote_MetaPopulation_Pop_IntroSyphilis {

    public static void main(String[] arg) {

        String[] baseDirStrCollection = new String[]{
            "C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\Gen_Results_1000_Syphilis_Var_Trans_CSV",
            "C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\Gen_Results_1000_Syphilis_CSV",
            "C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\Gen_Results_1000_Syphilis_Half_Trans_CSV",   
            //"C:\\Users\\Bhui\\Desktop\\FTP\\RMP\\Syphilis\\Gen_Results_1000_Syphilis",
        };

        for (String baseDirStr : baseDirStrCollection) {

            String[] rArg = new String[]{
                // Base Dir            
                baseDirStr,
                // Import Dir
                "C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\ImportDir",
                // Num threads
                "8",
                // Num sim
                "1000",
                // Num Step
                "18000",
                // Sample Freq
                "",
                // Store infection history
                "false"
            };

            File baseDir = new File(baseDirStr);
            if (baseDir.exists() && baseDir.listFiles().length > 1) {
                System.out.println(baseDir.getAbsolutePath() + " has more than 1 file inside. Simulation NOT Run");
            } else {
                Run_Population_Remote_MetaPopulation_Pop_Intro_Syphilis run = new Run_Population_Remote_MetaPopulation_Pop_Intro_Syphilis(rArg);
                run.runSimulation();
            }

            try {
                Run_Population_Remote_MetaPopulation_Pop_Intro_Syphilis.decodeCollectionFile(baseDir);
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace(System.err);
            }
        }

    }

}
