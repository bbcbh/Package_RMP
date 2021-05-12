package test.nonsim;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * 
 * @author Ben Hui
 * @deprecated  If possible, use standardised procedure under PropFile_Factory instead.
 */

public class Util_Population_Remote_MetaPopulation_Gen_Prop {

    public static File outputDir = new File("C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\GenProps");
    public static File defaultPropFile = new File("C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\Syp_Baseline\\Syp_Baseline_100\\simSpecificSim.prop");

    public File basePropFile;
    
    public String[] lines_default = null;
    
    public final int[] COMMON_REPLACE_INDEX = new int[]{
        5,72,80};
    public final String[] COMMON_REPLACE_STRING = new String[]{
        "Last modification: 2019-08-31",
        "<entry key=\"PROP_POP_SELECT_CSV\">C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\Syp_BestFit\\Syp_Baseline_Select_100.csv</entry>",
        "<entry key=\"PROP_NUM_SNAP\">438</entry>"};
    
    public final int[] REFERENCE_COVERAGE_INDEX = new int[]{242, 253}; // First line - date setting, last line, entry
    
    
    public final Object[][] genPropSetting = {
        // Format: Directory Name, increase coverage by, massscreeing 
        
    };
    
    

    public void genPropFile() throws IOException {
        // Read Prop File
        ArrayList<String> lineStore = new ArrayList();
        BufferedReader reader = new BufferedReader(new FileReader(basePropFile));
        System.out.println("Reading from " + basePropFile);
        String line;
        while((line = reader.readLine()) != null){
            lineStore.add(line);
        }
        reader.close();
        
        lines_default = lineStore.toArray(new String[lineStore.size()]);
        
        // Replace common replace lines
        for (int r = 0; r < COMMON_REPLACE_INDEX.length; r++){
            lines_default[COMMON_REPLACE_INDEX[r]] = COMMON_REPLACE_STRING[r];
        }

    }

    public Util_Population_Remote_MetaPopulation_Gen_Prop() {
        this.basePropFile = defaultPropFile;
    }

    public Util_Population_Remote_MetaPopulation_Gen_Prop(File basepropFile) {
        this.basePropFile = basepropFile;
    }

    public static void main(String[] arg) throws IOException {
        Util_Population_Remote_MetaPopulation_Gen_Prop genProp = new Util_Population_Remote_MetaPopulation_Gen_Prop();
        genProp.genPropFile();
    }

}
