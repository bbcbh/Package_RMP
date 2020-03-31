package test;

import java.io.IOException;
import sim.Simulation_Remote_MetaPopulation;

public class Test_Population_Remote_MetaPopulation_Sim {

    public static void main(String[] arg) throws IOException, InterruptedException, ClassNotFoundException {
        String[] baseDir = new String[]{
            //"C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\Syp_BestFit_050_Non_Key",            
            //"Syp_Sel_FC_CM_*",                          
            //"Srn_RPO_*",                       
            "C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\Test",
            //"Srn_RPO_BA_Std",//"Srn_RPO_BA_Int",
            //"Opt_NGCTBehav",   
            //"OptGA_NGCTBehav", 
            "Covid19",            
            "-skipAnalysis",                
        };

        Simulation_Remote_MetaPopulation.main(baseDir);               
        
    }

}
