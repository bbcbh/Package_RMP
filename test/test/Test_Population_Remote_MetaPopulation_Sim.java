package test;

import java.io.IOException;
import sim.Simulation_Remote_MetaPopulation;

public class Test_Population_Remote_MetaPopulation_Sim {

    public static void main(String[] arg) throws IOException, InterruptedException, ClassNotFoundException {
        String[] baseDir = new String[]{                          
            "C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\GenResults", 
            // NG_CT
            //"Srn_S", 
            //"Srn_I",            
            //"Srn_P",
            "Srn_40R_I", 
            "Srn_60R_I",            
            "Srn_80R_I", 
            //"Srn_40A_S", 
            //"Srn_40A_I", 
            //"Srn_60A_S", 
            //"Srn_60A_I", 
            //"Srn_80A_S", 
            //"Srn_80A_I",                         
            
            
            // Syphilis            
            //"Syphilis_Testing",   
            //"Syphilis_No_Testing",
            //"Syphilis_Testing_LowTran", 
            //"Syp_Tran_03", 
            //"Syp_Tran_04", 
            //"Syp_Tran_05"
        };        
        
        Simulation_Remote_MetaPopulation.main(baseDir);
    }

}
