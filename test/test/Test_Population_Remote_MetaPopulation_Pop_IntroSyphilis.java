/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.io.File;
import java.io.IOException;
import run.Run_Population_Remote_MetaPopulation_Pop_IntroSyphilis;

public class Test_Population_Remote_MetaPopulation_Pop_IntroSyphilis {

    public static void main(String[] arg) {

        String[] rArg = new String[]{
            // Base Dir            
            "C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\Gen_Results_500_Syphilis_DoubleDuration",
            // Import Dir
            "C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\ImportDir",
            // Num threads
            "8",
            // Num sim
            "500",};

        Run_Population_Remote_MetaPopulation_Pop_IntroSyphilis run = new Run_Population_Remote_MetaPopulation_Pop_IntroSyphilis(rArg);
        run.runSimulation();

        try {
            Run_Population_Remote_MetaPopulation_Pop_IntroSyphilis.decodeCollectionFile(new File(rArg[0]));
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace(System.err);
        }

    }

}
