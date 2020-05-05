package test.nonsim;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Util_Population_Remote_MetaPopulation_GenProp_Recursive {

    private final String PROP_FILE_NAME = "simSpecificSim.prop";

    private final Date date = Calendar.getInstance().getTime();
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    String strDate = dateFormat.format(date);

    private final String[] LINE_SRC_SUFFIX = new String[]{
        "Last modification:",
        };
    private final String[] LINE_TARGET = new String[]{
        "Last modification: " + dateFormat.format(date),
        };

    public void genPropFile(File tarDir, File scrDir) throws IOException {
        File propFile = new File(scrDir, PROP_FILE_NAME);

        if (propFile.exists()) {
            int repPt = 0;
            tarDir.mkdirs();
            File tarProp = new File(tarDir, PROP_FILE_NAME);
            PrintWriter pWri;
            try (BufferedReader src = new BufferedReader(new FileReader(propFile))) {
                pWri = new PrintWriter(tarProp);
                String srcLine;
                while ((srcLine = src.readLine()) != null) {
                    if (repPt < LINE_SRC_SUFFIX.length
                            && srcLine.trim().startsWith(LINE_SRC_SUFFIX[repPt])) {
                        if (LINE_TARGET[repPt].length() > 0) {
                            pWri.println(LINE_TARGET[repPt]);
                        }
                        repPt++;
                    } else {
                        pWri.println(srcLine.trim());
                    }
                }
            }
            pWri.close();
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

    public static void main(String[] arg) throws IOException {
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
