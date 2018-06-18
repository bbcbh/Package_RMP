package test;

import java.io.IOException;
import sim.Simulation_Remote_MetaPopulation;

public class Test_Population_Remote_MetaPopulation_Sim {

    public static void main(String[] arg) throws IOException, InterruptedException, ClassNotFoundException {
        String[] baseDir = new String[]{
            // NG_CT   
            /*
            "C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\NG_CT", 
            "Srn_Baseline_1000", 
            "Srn_POC_1000",
            */
            // Syphilis
            "C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\Syphilis",
            "Sim_Results_Syphilis",            
        };        
        
        Simulation_Remote_MetaPopulation.main(baseDir);
    }

}
