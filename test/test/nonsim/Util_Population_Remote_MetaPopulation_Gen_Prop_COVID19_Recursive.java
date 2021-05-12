package test.nonsim;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import util.PropValUtils;

/**
 * 
 * @author Ben Hui
 * @deprecated  Use standardised procedure under PropFile_Factory instead.
 */

public class Util_Population_Remote_MetaPopulation_Gen_Prop_COVID19_Recursive {

    private final String PROP_FILE_NAME = "simSpecificSim.prop";

    private Element[] replacement_element = {};
    private Element replacement_comment = null;

    public void setReplacemenEntries(Document xml_doc) {
        NodeList nList_entry = xml_doc.getElementsByTagName("entry");
        replacement_element = new Element[nList_entry.getLength()];

        for (int entId = 0; entId < nList_entry.getLength(); entId++) {
            Element entryElement = (Element) nList_entry.item(entId);
            replacement_element[entId] = entryElement;
        }

        replacement_comment = (Element) xml_doc.getElementsByTagName("comment").item(0);

    }

    public void genPropFile(File tarDir, File scrDir)
            throws IOException, ParserConfigurationException, SAXException, TransformerException {
        PropValUtils.genPropFile(tarDir, scrDir, PROP_FILE_NAME,
                replacement_element, replacement_comment);
    }

    public static void main(String[] arg)
            throws IOException, SAXException, ParserConfigurationException, TransformerException {
        File genDir = new File("C:\\Users\\bhui\\OneDrive - UNSW\\RMP\\Covid19\\HPC_Blank\\Gen_Base");
        File[] recursiveDir = new File[]{
            new File("C:\\Users\\bhui\\OneDrive - UNSW\\RMP\\Covid19\\HPC_Blank\\Gen_Src\\Covid19_No_Response"),
            new File("C:\\Users\\bhui\\OneDrive - UNSW\\RMP\\Covid19\\HPC_Blank\\Gen_Src\\CTE"),
            new File("C:\\Users\\bhui\\OneDrive - UNSW\\RMP\\Covid19\\HPC_Blank\\Gen_Src\\CQE"),
            new File("C:\\Users\\bhui\\OneDrive - UNSW\\RMP\\Covid19\\HPC_Blank\\Gen_Src\\CTE_SymAll"),
            new File("C:\\Users\\bhui\\OneDrive - UNSW\\RMP\\Covid19\\HPC_Blank\\Gen_Src\\CQE_SymAll"),};

        File commonReplacementPropFile = new File("C:\\Users\\bhui\\OneDrive - UNSW\\RMP\\Covid19\\HPC_Blank\\Gen_Src\\replacement.prop");

        if (!commonReplacementPropFile.exists()) {
            javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
            int res = chooser.showOpenDialog(null);
            if (res == javax.swing.JFileChooser.APPROVE_OPTION) {
                commonReplacementPropFile = chooser.getSelectedFile();
            }
        }

        if (commonReplacementPropFile.exists()) {
            Util_Population_Remote_MetaPopulation_Gen_Prop_COVID19_Recursive util;
            Document commonReplaceDoc = PropValUtils.parseXMLFile(commonReplacementPropFile);
            NodeList nList;

            nList = commonReplaceDoc.getElementsByTagName("filepath_tar");
            String ent;
            if (nList.getLength() > 0) {
                ent = nList.item(0).getTextContent();
                genDir = new File(ent);
            }
            nList = commonReplaceDoc.getElementsByTagName("filepath_src");
            if (nList.getLength() > 0) {
                recursiveDir = new File[nList.getLength()];
                for (int f = 0; f < recursiveDir.length; f++) {
                    ent = nList.item(f).getTextContent();
                    recursiveDir[f] = new File(ent);
                }
            }

            for (File f : recursiveDir) {
                util = new Util_Population_Remote_MetaPopulation_Gen_Prop_COVID19_Recursive();
                File baseDir = new File(genDir, f.getName());
                util.setReplacemenEntries(commonReplaceDoc);
                util.genPropFile(baseDir, f);
            }

            System.out.println("Prop files generated at " + genDir.getAbsolutePath());
        }

    }

}
