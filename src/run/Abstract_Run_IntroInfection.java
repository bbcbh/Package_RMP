package run;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * An abstract class to define share method for Run_Population_Remote_MetaPopulation_Pop_Intro_NG_CT and Run_Population_Remote_MetaPopulation_Pop_Intro_Syphilis
 *
 * @author Ben Hui
 * @version 20180713
 *
 * History:
 *
 * <pre>
 * 20180815
 *   - Change from interface to abstract class, implementation of PopSelectionCSV methods
 * </pre>
 */
public abstract class Abstract_Run_IntroInfection {
    
    public abstract double[] getRunParamValues();

    protected String[] popParamValStr = new String[0];
    protected String[] threadParamValStr = new String[Thread_PopRun.PARAM_TOTAL];       
    protected Integer[] popSelectionIndex = null;

    public Integer[] getPopSelection() {
        return popSelectionIndex;
    }

    public Integer[] setPopSelectionCSV(String s) {
        File csv = new File(s);

        ArrayList<Integer> arr = null;

        try {
            try (BufferedReader lines = new BufferedReader(new FileReader(csv))) {
                arr = new ArrayList();
                String line;
                while ((line = lines.readLine()) != null) {
                    arr.add(Integer.parseInt(line));
                }
            }
        } catch (IOException | NumberFormatException ex) {            
            ex.printStackTrace(System.err);            
        }                
        if(arr!= null){            
            popSelectionIndex = arr.toArray(new Integer[arr.size()]);            
            Arrays.sort(popSelectionIndex);                                    
        }
        
        return popSelectionIndex;

    }

    public String[] getPopParamValStr() {
        return popParamValStr;
    }

    public void setPopParamValStr(int index, String ent) {
        if (popParamValStr.length < index) {
            popParamValStr = Arrays.copyOf(popParamValStr, index + 1);
        }
        popParamValStr[index] = ent;
    }

    public String[] getThreadParamValStr() {
        return threadParamValStr;
    }

}
