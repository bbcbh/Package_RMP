/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.nonsim;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import util.PropValUtils;

/**
 *
 * @author Ben Hui
 * @deprecated  If possible, use standardised procedure under PropFile_Factory instead.
 */
public class Util_Population_Remote_MetaPopulation_Gen_Prop_Syphilis {

    public static final String PROP_FILE_NAME = "simSpecificSim.prop";
    public static final String MODIFICATION_FILE_NAME = "propMod.xml";
    public static final File DEFAULT_FILE_BASE_PROP_DIR = new File("C:\\Users\\Bhui\\Documents\\Java_Test\\Prop_Template");
    public static final File DEFAULT_FILE_TARGET_DIR = new File("C:\\Users\\Bhui\\Documents\\Java_Test\\Prop_Gen");

    public static void main(String[] arg) throws IOException, SAXException, ParserConfigurationException, TransformerException {
        boolean genBaseline = true;
        boolean genMassScreenSingleGrp = false;

        File base_prop_dir = DEFAULT_FILE_BASE_PROP_DIR;
        File target_prop_dir = DEFAULT_FILE_TARGET_DIR;

        if (!base_prop_dir.isDirectory()) {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setDialogTitle("Choose directory that contains directory of prop files");
            int resp = fc.showOpenDialog(null);

            if (resp == JFileChooser.APPROVE_OPTION) {
                base_prop_dir = fc.getSelectedFile();
            }
        }

        if (!target_prop_dir.isDirectory()) {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setDialogTitle("Choose directory that directory of prop files save to");
            int resp = fc.showOpenDialog(null);

            if (resp == JFileChooser.APPROVE_OPTION) {
                target_prop_dir = fc.getSelectedFile();
            }
        }

        if (base_prop_dir.isDirectory() && target_prop_dir.isDirectory()) {

            File[] template_dirs = base_prop_dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isDirectory()
                            && (new File(pathname, PROP_FILE_NAME)).exists();
                }
            });

            for (File template_dir : template_dirs) {
                System.out.println("Template dir name = " + template_dir.getName());

                // Baseline model
                if (genBaseline) {
                    final int NUM_GRP = 8; // M 15-19, M 20-24, M 25-29, M 30-34, F 15-19, F 20-24, F 25-29, F 30-34          
                    final int NUM_REMOTE = 10;
                    final int NUM_REGIONAL = 1;
                    final int POP_REGIONAL = 0;
                    final int POP_REMOTE = POP_REGIONAL + 1;
                    final int POP_TOTAL = POP_REMOTE + 1;
                    final String SRN_PREFIX = "POP_PROP_INIT_PREFIX_42";
                    final String WHITESPACE = "									  ";
                    final String FIRST_COVERAGE = "[ 0.12, 0.13, 0.10, 0.10, 0.24, 0.22, 0.21, 0.17, \n"
                            + "									   0.13, 0.14, 0.11, 0.11, 0.26, 0.24, 0.23, 0.19, \n"
                            + "									   0.13, 0.14, 0.11, 0.11, 0.26, 0.24, 0.23, 0.19, \n"
                            + "									   0.13, 0.14, 0.11, 0.11, 0.26, 0.24, 0.23, 0.19, \n"
                            + "									   0.13, 0.14, 0.11, 0.11, 0.26, 0.24, 0.23, 0.19,\n"
                            + "									   0.13, 0.14, 0.11, 0.11, 0.26, 0.24, 0.23, 0.19, \n"
                            + "									   0.13, 0.14, 0.11, 0.11, 0.26, 0.24, 0.23, 0.19,\n"
                            + "									   0.13, 0.14, 0.11, 0.11, 0.26, 0.24, 0.23, 0.19, \n"
                            + "									   0.13, 0.14, 0.11, 0.11, 0.26, 0.24, 0.23, 0.19,\n"
                            + "									   0.13, 0.14, 0.11, 0.11, 0.26, 0.24, 0.23, 0.19,\n"
                            + "									   0.13, 0.14, 0.11, 0.11, 0.26, 0.24, 0.23, 0.19,1,";

                    File coverage_adj_file = new File(template_dir, "Syp_Test_Cov_Adj.csv");
                    BufferedReader adj_reader = new BufferedReader(new FileReader(coverage_adj_file));
                    String line = adj_reader.readLine();
                    String[] header = line.split(",");

                    int[] timestep = new int[header.length - 1];
                    for (int i = 0; i < timestep.length; i++) {
                        timestep[i] = Integer.parseInt(header[i + 1]);
                    }

                    float[][][] coverageMatrix = new float[timestep.length][POP_TOTAL][NUM_GRP]; // float[timestep][grp];

                    adj_reader.readLine(); // "Remote"

                    for (int g = 0; g < NUM_GRP; g++) {
                        String[] ent = adj_reader.readLine().split(",");
                        for (int i = 1; i < ent.length; i++) {
                            coverageMatrix[i - 1][POP_REMOTE][g] = Float.parseFloat(ent[i]);
                        }
                    }

                    adj_reader.readLine(); // Regional

                    for (int g = 0; g < NUM_GRP; g++) {
                        String[] ent = adj_reader.readLine().split(",");
                        for (int i = 1; i < ent.length; i++) {
                            coverageMatrix[i - 1][POP_REGIONAL][g] = Float.parseFloat(ent[i]);
                        }
                    }

                    StringBuilder adj_cov_entry = new StringBuilder();
                    adj_cov_entry.append(FIRST_COVERAGE);                    
                    for (int t = 0; t < timestep.length; t++) {
                        // Time, period, freq, number of time
                        adj_cov_entry.append('\n');
                        adj_cov_entry.append(WHITESPACE);
                        adj_cov_entry.append(' ');
                        adj_cov_entry.append(timestep[t]);
                        adj_cov_entry.append(", 180, 360, ");
                        if (t == timestep.length - 1) {
                            adj_cov_entry.append(-1);
                        } else {
                            adj_cov_entry.append(-timestep[t + 1]);
                        }
                        adj_cov_entry.append(',');                        

                        // Regional
                        for (int i = 0; i < NUM_REGIONAL; i++) {                            
                            for (int g = 0; g < NUM_GRP; g++) {
                                if (g == 0) {
                                    adj_cov_entry.append('\n');
                                    adj_cov_entry.append(WHITESPACE);
                                } else {
                                    adj_cov_entry.append(',');
                                }
                                adj_cov_entry.append(String.format("% .2f",coverageMatrix[t][POP_REGIONAL][g]));
                            }
                            adj_cov_entry.append(',');                            
                            
                        }
                        for (int i = 0; i < NUM_REMOTE; i++) {
                            for (int g = 0; g < NUM_GRP; g++) {
                                if (g == 0) {
                                    adj_cov_entry.append('\n');
                                    adj_cov_entry.append(WHITESPACE);
                                } else {
                                    adj_cov_entry.append(',');
                                }
                                adj_cov_entry.append(String.format("% .2f",coverageMatrix[t][POP_REMOTE][g]));
                            }
                            adj_cov_entry.append(',');                           
                        }                        
                        // Last entry for time step                                                
                        if(t == 0){
                             adj_cov_entry.append("7,");     
                        }else{
                             adj_cov_entry.append(1);     
                             
                             if(t != timestep.length-1){
                                 adj_cov_entry.append(',');
                             }
                             
                             
                        }                        
                    }
                    
                    adj_cov_entry.append("]");
                    
                    //System.out.println(adj_cov_entry.toString());

                    File newDir = new File(target_prop_dir, template_dir.getName() + "_adj");
                    newDir.mkdirs();

                    Document xml_src = PropValUtils.parseXMLFile(new File(template_dir, PROP_FILE_NAME));
                    NodeList nList_entry = xml_src.getElementsByTagName("entry");

                    ArrayList<Element> replaceElement = new ArrayList();

                    boolean addElem = true;

                    for (int n = 0; n < nList_entry.getLength(); n++) {
                        Element elem = (Element) nList_entry.item(n);
                        if (SRN_PREFIX.equals(elem.getAttribute("key"))) {
                            elem.setTextContent(adj_cov_entry.toString());

                            replaceElement.add(elem);
                            addElem = false;
                        }
                    }

                    if (addElem) {
                        Element newEntry = xml_src.createElement("entry");
                        newEntry.setAttribute("key", SRN_PREFIX);
                        newEntry.setTextContent(adj_cov_entry.toString());
                        replaceElement.add(newEntry);
                    }

                    // Store as new .prop file
                    PropValUtils.replacePropEntryByDOM(xml_src, new File(newDir, PROP_FILE_NAME),
                            replaceElement.toArray(new Element[replaceElement.size()]),
                            null);

                }

                // Generate mass screening single group (i.e. 1 mass screen at remote location at a time with varying mass 
                if (genMassScreenSingleGrp) {
                    generateMassScreenSingleGrp(template_dir, target_prop_dir);
                }

            }

        } else {
            System.err.println("Error: base prop directory and/or target prop direct not defined. Exiting.");
            System.exit(1);
        }

    }

    private static void generateMassScreenSingleGrp(File template_dir,
            File target_prop_dir) throws NumberFormatException, FileNotFoundException, IOException {
        String line;
        ArrayList<String> lines_src = new ArrayList();
        BufferedReader srcReader = new BufferedReader(new FileReader(new File(template_dir, PROP_FILE_NAME)));

        while ((line = srcReader.readLine()) != null) {
            lines_src.add(line);
        }

        System.out.println(String.format("Number of lines in <%s>'s prop file = %d.",
                template_dir.getName(), lines_src.size()));

        int[] replace_range = new int[]{273, 285};
        final String NO_SRN = "0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,";
        final String YES_SRN = "0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3,";

        System.out.println(String.format("%2$d lines starting from %1$d is to be replaced.",
                replace_range[0], replace_range[1] - replace_range[0]));

        Pattern massSrcHeaderPattern = Pattern.compile("(\\s*)(\\d+)(.*)");

        for (int overlap = 30; overlap < 360; overlap += 30) {
            String newDirName = String.format("%s_%03d", template_dir.getName(), overlap);
            File newDir = new File(target_prop_dir, newDirName);
            newDir.mkdirs();

            File newProp = new File(newDir, PROP_FILE_NAME);

            PrintWriter newPropWri = new PrintWriter(newProp);

            int counter = 1;
            for (String srcline : lines_src) {
                if (counter < replace_range[0]
                        || counter >= replace_range[1]) {
                    newPropWri.println(srcline);
                    //newPropWri.flush();
                } else if (counter == replace_range[0]) {
                    // Time, period, freq, number of time
                    Matcher m = massSrcHeaderPattern.matcher(srcline);
                    if (m.find()) {
                        String prefix_header = m.group(1);
                        int startTime = Integer.parseInt(m.group(2));
                        String suffix_header = m.group(3);

                        for (int rlocSet = 0; rlocSet < 10; rlocSet++) {
                            newPropWri.print(prefix_header);
                            newPropWri.print(startTime);
                            newPropWri.println(suffix_header);
                            newPropWri.print(prefix_header);
                            newPropWri.println(NO_SRN);
                            for (int rLoc = 0; rLoc < 10; rLoc++) {
                                newPropWri.print(prefix_header);
                                if (rLoc == rlocSet) {
                                    newPropWri.print(YES_SRN);
                                } else {
                                    newPropWri.print(NO_SRN);
                                }
                                if (rLoc == 9) {
                                    newPropWri.println(" 0,");
                                } else {
                                    newPropWri.println();
                                }
                            }
                            //newPropWri.flush();
                            startTime += overlap;
                        }

                    }
                }
                counter++;
            }

            newPropWri.close();

            System.out.println(String.format("New prop generated at <%s>", newDir.getAbsolutePath()));

        }
    }

}
