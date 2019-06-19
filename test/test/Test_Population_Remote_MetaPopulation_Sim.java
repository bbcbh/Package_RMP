package test;

import java.io.IOException;
import sim.Simulation_Remote_MetaPopulation;

public class Test_Population_Remote_MetaPopulation_Sim {

    public static void main(String[] arg) throws IOException, InterruptedException, ClassNotFoundException {
        String[] baseDir = new String[]{
            "C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\Test\\",
            "Syp_Sel_FC_CM_*",                                                      
            //"Syp_Baseline_P055",
            //"Opt_NGCTBehav",    
            //"Syp_Test",                                                    
            //"Srn_RPO_*",            
            //"C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\Syp_BestFit",
                                                       
        };

        Simulation_Remote_MetaPopulation.main(baseDir);
    }

}
