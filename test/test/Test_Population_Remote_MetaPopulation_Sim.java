package test;

import java.io.IOException;
import sim.Simulation_Remote_MetaPopulation;

public class Test_Population_Remote_MetaPopulation_Sim {

    public static void main(String[] arg) throws IOException, InterruptedException, ClassNotFoundException {
        String[] baseDir = new String[]{
            "C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\Test\\",              
            //"Syp_Sel_FC_CM_Baseline",                           
            //"Syp_Baseline",            
            //"Opt_NGCTBehav",    
            //"Syp_Test",
            //"Syp_Sel_Baseline_Full",                                                     
            "Srn_RPO_BA_Int",
            "Srn_RPO_BA_Std",
            "Srn_RPO_40_Int",
            "Srn_RPO_40_Std",
            "Srn_RPO_60_Int",
            "Srn_RPO_60_Std",
            "Srn_RPO_80_Int",
            "Srn_RPO_80_Std",
        };

        Simulation_Remote_MetaPopulation.main(baseDir);
    }

}
