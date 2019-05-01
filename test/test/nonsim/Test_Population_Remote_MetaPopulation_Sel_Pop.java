package test.nonsim;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Test_Population_Remote_MetaPopulation_Sel_Pop {

    public static void main(String[] arg) throws IOException {
        String fromPath = "C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\Syphilis\\Syp_Baseline_FC_CM_90_Dur_7_42";
        String toPath = "C:\\Users\\Bhui\\Desktop\\FTP\\RMP\\Syp_Baseline_FC_CM_90_Dur_7_42";
        String selFileStr = "Syp_Baseline_Select_100.csv";

        File baseDir = new File(fromPath);
        File targetDir = new File(toPath);
        
        File selFile = new File(fromPath, selFileStr);
        File selFileTar = new File(toPath, selFileStr);
        
        Files.copy(selFile.toPath(), selFileTar.toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        BufferedReader reader = new BufferedReader(new FileReader(selFileTar));
        
        String line;
        int counter = 0;
        
        while((line = reader.readLine()) != null){
            String popNum = line.trim();
            System.out.println("Copying files assocated with Pop #" + popNum);
            
            String fname;
            
            fname = "incident_S" + popNum + ".csv";                       
            Files.copy(new File(fromPath, fname).toPath(), new File(toPath, fname).toPath(), StandardCopyOption.REPLACE_EXISTING);
                        
            fname = "output_" + popNum + ".txt";                       
            Files.copy(new File(fromPath, fname).toPath(), new File(toPath, fname).toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            fname = "prevalence_S"+ popNum + ".csv";
            Files.copy(new File(fromPath, fname).toPath(), new File(toPath, fname).toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            fname = "test_and_notification_S"+ popNum + ".csv";
            Files.copy(new File(fromPath, fname).toPath(), new File(toPath, fname).toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            fname = "Sim_pop_S" + popNum + ".zip";
            Files.copy(new File(fromPath, fname).toPath(), new File(toPath, fname).toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            counter++;
        }
        
        
        System.out.println(counter + " sets copied.");
        
        
        
        
        
        
        

    }

}
