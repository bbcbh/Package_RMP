package test;

import java.io.IOException;
import sim.Simulation_Remote_MetaPopulation;

public class Test_Population_Remote_MetaPopulation_Sim {

    public static void main(String[] arg) throws IOException, InterruptedException, ClassNotFoundException {
        String[] baseDir = new String[]{
            // NG_CT               
            "C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\GenResults", 
            "Srn_Std", 
            "Srn_Intervention",            
            // Syphilis            
            "Syphilis_Testing",   
            "Syphilis_No_Testing",
        };        
        
        Simulation_Remote_MetaPopulation.main(baseDir);
    }

}
