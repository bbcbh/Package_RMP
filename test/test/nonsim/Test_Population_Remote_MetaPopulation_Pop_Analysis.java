/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.nonsim;

import java.io.File;
import java.io.IOException;
import run.Run_Population_Remote_MetaPopulation_Pop_Analysis;

/**
 *
 * @author Bhui
 */
public class Test_Population_Remote_MetaPopulation_Pop_Analysis {

    public static void main(String[] arg) throws IOException, ClassNotFoundException {
        File singleSetDir = new File("C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\Syphilis\\Syp_BestFit_075_Key\\Syp_Sel_FC_CM_Baseline");
        System.out.println("Running popAnalysis at " + singleSetDir.getAbsolutePath());
        Run_Population_Remote_MetaPopulation_Pop_Analysis.popAnalysis(singleSetDir.getAbsolutePath());

    }

}
