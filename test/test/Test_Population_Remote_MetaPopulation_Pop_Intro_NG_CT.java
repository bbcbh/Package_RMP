/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import run.Run_Population_Remote_MetaPopulation_Pop_Intro_NG_CT;

public class Test_Population_Remote_MetaPopulation_Pop_Intro_NG_CT {

    public static void main(String[] arg) {

        String[] rArg = new String[]{
            // Base Dir            
            "C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\Gen_Results_1000_Var_Trans_Var_002002",
            // Import Dir
            "C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\ImportDir",
            // Num threads
            "8",
            // Num sim
            "1000",};

        Run_Population_Remote_MetaPopulation_Pop_Intro_NG_CT run = new Run_Population_Remote_MetaPopulation_Pop_Intro_NG_CT(rArg);
        run.runSimulation();

    }

}
