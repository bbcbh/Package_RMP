/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.nonsim;

import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import run.Run_Population_Remote_MetaPopulation_Pop_Intro_Syphilis;

/**
 *
 * @author Bhui
 */
public class Test_Population_Remote_MetaPopulation_DecodeCollectionFile {
    
    public static void main(String[] arg) throws IOException, ClassNotFoundException {

        JFileChooser jc = new JFileChooser();
        jc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (jc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {

            File singleSetDir = jc.getSelectedFile();
            System.out.println("Running decodeCollectionFile at " + singleSetDir.getAbsolutePath());
            Run_Population_Remote_MetaPopulation_Pop_Intro_Syphilis.decodeCollectionFile(singleSetDir);
        }

    }
    
}
