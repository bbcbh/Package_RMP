package test;

import java.io.IOException;
import sim.Simulation_Remote_MetaPopulation;

public class Test_Population_Remote_MetaPopulation_Sim {

    public static void main(String[] arg) throws IOException, InterruptedException, ClassNotFoundException {
        String[] baseDir = new String[]{
            "C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\NG_CT", 
            "Srn_Baseline", "Srn_POC"
        };        
        
        Simulation_Remote_MetaPopulation.main(baseDir);
    }

}
