package test.nonsim;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class Util_Population_Remote_MetaPopulation_GenProp_Recursive {

    private final String PROP_FILE_NAME = "simSpecificSim.prop";

    private final Date CURRENT_DATE = Calendar.getInstance().getTime();
    private final DateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final String DATE_STAMP_STR = DEFAULT_DATE_FORMAT.format(CURRENT_DATE);

    private String[] entry_key_index = {
        "POP_PROP_INIT_PREFIX_30",};

    private String[] entry_key_replacement = {
        "[-0.12, -0.05]",};

    public static Document parseXMLFile(File xml) throws
            ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        return dBuilder.parse(xml);

    }

    public void setReplacemenEntries(Document xml_doc) {
        NodeList nList_entry = xml_doc.getElementsByTagName("entry");
        entry_key_index = new String[nList_entry.getLength()];
        entry_key_replacement = new String[nList_entry.getLength()];

        for (int entId = 0; entId < nList_entry.getLength(); entId++) {
            Element entryElement = (Element) nList_entry.item(entId);
            entry_key_index[entId] = entryElement.getAttribute("key");
            entry_key_replacement[entId] = entryElement.getTextContent();
        }

    }

    public void genPropFile(File tarDir, File scrDir)
            throws IOException, ParserConfigurationException, SAXException, TransformerException {
        File propFile = new File(scrDir, PROP_FILE_NAME);

        if (propFile.exists()) {

            tarDir.mkdirs();
            File tarProp = new File(tarDir, PROP_FILE_NAME);
            replacePropEntryByDOM(parseXMLFile(propFile), tarProp);
        }

        File[] dirList = scrDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

        //Run recursively until there is no directory
        for (File d : dirList) {
            File newTar = new File(tarDir, d.getName());
            genPropFile(newTar, d);
        }
    }

    private void replacePropEntryByDOM(Document src_doc, File tarProp)
            throws IOException, ParserConfigurationException, SAXException, TransformerConfigurationException, TransformerException {

        NodeList nList_comment = src_doc.getElementsByTagName("comment");

        for (int c = 0; c < nList_comment.getLength(); c++) {
            Node commentNode = nList_comment.item(c);
            String ent = commentNode.getTextContent();
            StringWriter wri = new StringWriter();
            PrintWriter pWri;
            try (BufferedReader lines = new BufferedReader(new StringReader(ent))) {
                pWri = new PrintWriter(wri);
                String srcLine;
                while ((srcLine = lines.readLine()) != null) {
                    if (srcLine.startsWith("Last modification")) {
                        String dateStr = "Last modification " + DATE_STAMP_STR;
                        pWri.println(dateStr);
                    } else {
                        pWri.println(srcLine);
                    }
                }
            }
            pWri.close();
            commentNode.setTextContent(wri.toString().replaceAll("\r", ""));
        }

        if (entry_key_index.length > 0) {
            NodeList nList_entry = src_doc.getElementsByTagName("entry");
            for (int entId = 0; entId < nList_entry.getLength(); entId++) {
                Element entryElement = (Element) nList_entry.item(entId);
                for (int k = 0; k < entry_key_index.length; k++) {
                    if (entry_key_index[k].equals(entryElement.getAttribute("key"))) {
                        entryElement.setTextContent(entry_key_replacement[k]);
                    }
                }
            }
        }

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer;

        transformer = tf.newTransformer();

        //Uncomment if you do not require XML declaration
        //transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");        
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, src_doc.getDoctype().getSystemId());

        //Write XML to file
        FileOutputStream outStream = new FileOutputStream(tarProp);

        transformer.transform(new DOMSource(src_doc), new StreamResult(outStream));

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
            Util_Population_Remote_MetaPopulation_GenProp_Recursive util;
            Document commonReplaceDoc = parseXMLFile(commonReplacementPropFile);
            NodeList nList;
            
            nList = commonReplaceDoc.getElementsByTagName("filepath_tar");
            String ent;
            if(nList.getLength() > 0){
                ent = nList.item(0).getTextContent();
                genDir = new File(ent);                                
            }
            nList = commonReplaceDoc.getElementsByTagName("filepath_src");
            if(nList.getLength() > 0){
                recursiveDir = new File[nList.getLength()];
                for(int f = 0; f < recursiveDir.length; f++){
                    ent = nList.item(f).getTextContent();
                    recursiveDir[f] = new File(ent);                    
                }                                                
            }
            
            for (File f : recursiveDir) {
                util = new Util_Population_Remote_MetaPopulation_GenProp_Recursive();
                File baseDir = new File(genDir, f.getName());
                util.setReplacemenEntries(commonReplaceDoc);
                util.genPropFile(baseDir, f);
            }

            System.out.println("Prop files generated at " + genDir.getAbsolutePath());
        }

    }

}
