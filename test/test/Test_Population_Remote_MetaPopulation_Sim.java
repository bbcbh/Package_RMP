package test;

import java.io.IOException;
import sim.Simulation_Remote_MetaPopulation;

public class Test_Population_Remote_MetaPopulation_Sim {

    public static void main(String[] arg) throws IOException, InterruptedException, ClassNotFoundException {
        String[] baseDir = new String[]{
            "C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\Test\\",
            //"Pop_Gen",            
            "Syp_Baseline",            
            //"Opt_NGCTBehav",                    
            //"Syp_Sel_Baseline",
            //"Syp_Sel_TSA",
            //"Syp_Sel_TSC",
            //"Syp_Sel_TSC",
            //"Syp_Sel_DSA_MSA_50_180P_10",
            //"Syp_Sel_DSC_MSC_50_180P_10",
            //"Syp_Sel_TSC_MSC_50_180P_10",            
        };

        Simulation_Remote_MetaPopulation.main(baseDir);
    }

}
