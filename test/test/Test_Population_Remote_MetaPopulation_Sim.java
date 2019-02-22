package test;

import java.io.IOException;
import sim.Simulation_Remote_MetaPopulation;

public class Test_Population_Remote_MetaPopulation_Sim {

    public static void main(String[] arg) throws IOException, InterruptedException, ClassNotFoundException {
        String[] baseDir = new String[]{                                                 
            "C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\Test\\",
            "Pop_Gen",            
            "Syp_Select",            
            //"Opt_NGCTBehav",                    
            //"Syp_Baseline_10_Yr"
            //"Syp_Sel_MassScr_50_180P10_DS", "Syp_Sel_MassScr_50_180P10_TS",
            //"Syp_Sel_MassScr_50_360P5_DS", "Syp_Sel_MassScr_50_360P5_TS",
            //"Syp_Sel_Baseline", "Syp_Sel_DS", "Syp_Sel_TS"
            
        };        
        
        Simulation_Remote_MetaPopulation.main(baseDir);
    }

}
