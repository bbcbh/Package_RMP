package test;

import java.io.IOException;
import sim.Simulation_Remote_MetaPopulation;

public class Test_Population_Remote_MetaPopulation_Sim {

    public static void main(String[] arg) throws IOException, InterruptedException, ClassNotFoundException {
        String[] baseDir = new String[]{                                                 
            "C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\Test\\",            
            "Syp_Select"
            //"Srn_AC_RM_Base_S",             
            //"Srn_AC_RM_80A_S",
            //"Srn_AC_RM_80R_S", 
            //"Opt_NGCTBehav",
           
        };        
        
        Simulation_Remote_MetaPopulation.main(baseDir);
    }

}
