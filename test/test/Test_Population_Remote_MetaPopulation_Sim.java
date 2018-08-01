package test;

import java.io.IOException;
import sim.Simulation_Remote_MetaPopulation;

public class Test_Population_Remote_MetaPopulation_Sim {

    public static void main(String[] arg) throws IOException, InterruptedException, ClassNotFoundException {
        String[] baseDir = new String[]{                                                 
            "C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\Test\\",            
            //"Syp_Tran_0400",
            "Srn_S_CL","Srn_S_80_CL",
            "Srn_S","Srn_S_80",            
            //"Opt_NGCTBehav",
           
        };        
        
        Simulation_Remote_MetaPopulation.main(baseDir);
    }

}
