
package test;

import java.io.IOException;
import opt.OptRun_Population_Remote_MetaPopulation_Infection_Intro;
import opt.OptRun_Population_Remote_MetaPopulation_Infection_Intro_GA;


public class Test_Population_Remote_MetaPopulation_Pop_Optimisation {
     public static void main(String[] arg) throws IOException, ClassNotFoundException, InterruptedException {
         
         boolean useGA = true;
         
         
         String[] rArg = new String[]{
             // Base Dir             
             //"C:\\Users\\Bhui\\Desktop\\FTP\\RMP\\OptResults_GA", 
             "C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\OptResults_GA",
             // Import Dir
             "C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\ImportDir", 
             // Num threads
             "1",
             // Num sim
             "4", 
             // Num to keep
             "0",
             // GA_Pop_SIZE
             "500",
                 
         
         };
         
         if(!useGA){         
             rArg[0] = "C:\\Users\\Bhui\\Desktop\\FTP\\RMP\\OptResults";
             OptRun_Population_Remote_MetaPopulation_Infection_Intro.main(rArg);
         }else{
             OptRun_Population_Remote_MetaPopulation_Infection_Intro_GA.main(rArg);
         }
         
         // java -jar "Package_Remote_MetaPopulation.jar" "C:\Users\Bhui\Desktop\FTP\RMP\OptResults" "C:\Users\Bhui\OneDrive - UNSW\RMP\BasePop" "32" "3"
         
     }
}
