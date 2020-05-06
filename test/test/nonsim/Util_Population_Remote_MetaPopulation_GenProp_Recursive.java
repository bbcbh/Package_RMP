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
        "POP_PROP_INIT_PREFIX_30",
        "POP_PROP_INIT_PREFIX_39",};

    private String[] entry_key_replacement = {
        "[-0.12, -0.05]",
        "[[0.037, 0, 0, 0.089, 0.011, 0.033, 0.248, 0.033, 0.143, 0.793, 0.143, 1, 1, 1, 1],\n"
        + "[0.041, 0, 0, 0.083, 0.011, 0.033, 0.245, 0.033, 0.143, 0.751, 0.143, 1, 1, 1, 1],\n"
        + "[0.041, 0, 0, 0.083, 0.011, 0.033, 0.245, 0.033, 0.143, 0.751, 0.143, 1, 1, 1, 1],\n"
        + "[0.041, 0, 0, 0.083, 0.011, 0.033, 0.245, 0.033, 0.143, 0.751, 0.143, 1, 1, 1, 1],\n"
        + "[0.041, 0, 0, 0.083, 0.011, 0.033, 0.245, 0.033, 0.143, 0.751, 0.143, 1, 1, 1, 1]]",};

    public void genPropFile(File tarDir, File scrDir)
            throws IOException, ParserConfigurationException, SAXException, TransformerException {
        File propFile = new File(scrDir, PROP_FILE_NAME);

        if (propFile.exists()) {

            tarDir.mkdirs();
            File tarProp = new File(tarDir, PROP_FILE_NAME);
            replacePropEntryByDOM(propFile, tarProp);
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

    private void replacePropEntryByDOM(File propFile, File tarProp)
            throws IOException, ParserConfigurationException, SAXException, TransformerConfigurationException, TransformerException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = (Document) dBuilder.parse(propFile);

        NodeList nList_comment = doc.getElementsByTagName("comment");

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
            NodeList nList_entry = doc.getElementsByTagName("entry");
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
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doc.getDoctype().getSystemId());

        //Write XML to file
        FileOutputStream outStream = new FileOutputStream(tarProp);

        transformer.transform(new DOMSource(doc), new StreamResult(outStream));

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

        Util_Population_Remote_MetaPopulation_GenProp_Recursive util;
        for (File f : recursiveDir) {
            util = new Util_Population_Remote_MetaPopulation_GenProp_Recursive();
            File baseDir = new File(genDir, f.getName());
            util.genPropFile(baseDir, f);
        }

        System.out.println("Prop files generated at " + genDir.getAbsolutePath());

    }

}
