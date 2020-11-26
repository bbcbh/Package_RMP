package test;

import java.io.IOException;
import sim.Simulation_Remote_MetaPopulation;

public class Test_Population_Remote_MetaPopulation_Sim {

    public static void main(String[] arg) throws IOException, InterruptedException, ClassNotFoundException {
        String[] baseDir = new String[]{
            "C:\\Users\\bhui\\Documents\\Java_Test",
            //"Srn_RPO_BA_Std",//"Srn_RPO_BA_Int",
            //"Opt_NGCTBehav",   
            //"OptGA_NGCTBehav", 
            //"Syp_Sel_FC_CM_.*", 
            "Srn_RPO_.*",                
            //"Covid19_Test.*",            
            //"-skipAnalysis -noZipRemove -clearPrevResult",                
        };

        Simulation_Remote_MetaPopulation.main(baseDir);

    }

}
