package test;

import java.io.IOException;
import sim.Simulation_Remote_MetaPopulation;

public class Test_Population_Remote_MetaPopulation_Sim {

    public static void main(String[] arg) throws IOException, InterruptedException, ClassNotFoundException {
        String[] baseDir = new String[]{
            "C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\Test\\",
            //"Pop_Gen",            
            //"Syp_Baseline",    
            //"Opt_NGCTBehav",    
            //"Syp_Test",
            "Syp_Sel_Baseline_Full",
            "Syp_Sel_Plus10A_Full",
            "Syp_Sel_Plus20A_Full",
            "Syp_Sel_Plus10A_MSR_50_360P_5_Full",
            "Syp_Sel_Plus20A_MSR_50_360P_5_Full",                       
        };

        Simulation_Remote_MetaPopulation.main(baseDir);
    }

}
