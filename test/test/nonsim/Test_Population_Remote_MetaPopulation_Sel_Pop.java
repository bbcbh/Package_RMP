package test.nonsim;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.swing.JFileChooser;

public class Test_Population_Remote_MetaPopulation_Sel_Pop {

    public static void main(String[] arg) throws IOException {
        //String fromPath = "C:\\Users\\bhui\\OneDrive - UNSW\\RMP\\ImportDir_Syp";
        //String toPath = "C:\\Users\\bhui\\Desktop\\ImportDir_Syp";
        //String selFileStr = "C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\Syphilis\\Syp_Baseline\\Syp_Baseline_075\\Syp_Baseline_Select_100.csv";

        //File baseDir = new File(fromPath);
        //File targetDir = new File(toPath);
        //File selFileTar = new File(selFileStr); 
        File srcDir = null, toDir = null, selFileTar = null;

        JFileChooser jc = new JFileChooser();
        jc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        jc.setDialogTitle("Please select source population diretory");        
        if(jc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION){
            srcDir = jc.getSelectedFile();
        }
        
        jc.setDialogTitle("Please select target population diretory");        
        if(jc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION){
            toDir = jc.getSelectedFile();
        }
        jc.setDialogTitle("Please select select file csv");       
        jc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        if(jc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION){
            selFileTar = jc.getSelectedFile();
        }
        
        
        

        if (srcDir == null || toDir == null || selFileTar == null) {            
            System.out.print("Please define source dir, target directory and pop select files");            

        } else {

            BufferedReader reader = new BufferedReader(new FileReader(selFileTar));

            String line;
            int counter = 0;

            while ((line = reader.readLine()) != null) {
                String popNum = line.trim();
                System.out.println("Copying files assocated with Pop #" + popNum);

                String fname;

                fname = "pop_S" + popNum + ".zip";
                Files.copy(new File(srcDir, fname).toPath(), new File(toDir, fname).toPath(), StandardCopyOption.REPLACE_EXISTING);

                /*
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
                 */
                counter++;
            }
            reader.close();

            System.out.println(counter + " sets copied.");
        }

    }

}
