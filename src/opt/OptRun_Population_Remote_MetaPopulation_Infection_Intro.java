package opt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import optimisation.AbstractParameterOptimiser;
import optimisation.AbstractResidualFunc;
import optimisation.NelderMeadOptimiser;
import transform.ParameterConstraintTransform;

public class OptRun_Population_Remote_MetaPopulation_Infection_Intro {

    public String BASE_DIR_STR = "~/RMP/OptResults";
    public String IMPORT_DIR_STR = "~/RMP/ImportDir";
    public int NUM_THREADS = Runtime.getRuntime().availableProcessors();
    public int NUM_SIM_TOTAL = -1;

    public static final String FILENAME_POP_SELECT = "pop_select.txt";
    public static final String FILENAME_PARAM_CONSTRIANTS = "ParamConstriants.csv";
    public static final String FILENAME_OPT_RESULTS_CSV = "ParamOpt.csv";
    public static final String FILENAME_OPT_RESULTS_OBJ = "ParamOpt.obj";
    public static final String FILENAME_OPT_SIMPLEX = "ParamSimplex.obj";
    public static final String FILENAME_PRE_SIMPLEX_CSV = "Pre_Simplex.csv";
    public static final String FILENAME_PRE_RESIDUE = "Pre_Residue.csv";
    public static final String FILENAME_P0 = "Pre_P0.csv";

    protected File importDir, exportDir;
    protected File[] popFiles;

    protected int NUM_OPT_TO_KEEP = 10;

    protected File[] OPT_RES_DIR_COLLECTION;
    protected double[] OPT_RES_SUM_SQS;
    protected String[] propModelInitStr = null;

    protected double[] TARGET_PREVAL = new double[]{
        0.118, 0.104, 0.074, 0.046, // CT, Male
        0.174, 0.082, 0.060, 0.035, // CT, Female
        0.137, 0.065, 0.040, 0.041, // NG, Male
        0.135, 0.076, 0.028, 0.043 // NG, Female              
    };
    
    protected int NUM_STEPS = 360 * 50;

    public OptRun_Population_Remote_MetaPopulation_Infection_Intro(String[] arg)
            throws IOException {
        // 0: Base Dir
        if (arg.length > 0) {
            if (!arg[0].isEmpty()) {
                BASE_DIR_STR = arg[0];
            }
        }
        // 1: Import Dir
        if (arg.length > 1) {
            if (!arg[1].isEmpty()) {
                IMPORT_DIR_STR = arg[1];
            }
        }
        // 2: Num thread

        if (arg.length > 2) {
            if (!arg[2].isEmpty()) {
                NUM_THREADS = Integer.parseInt(arg[2]);
            }
        }

        // 3: Num sim
        if (arg.length > 3) {
            if (!arg[3].isEmpty()) {
                NUM_SIM_TOTAL = Integer.parseInt(arg[3]);
            }
        }

        // 4: Num to keep
        if (arg.length > 4) {
            if (!arg[4].isEmpty()) {
                NUM_OPT_TO_KEEP = Integer.parseInt(arg[4]);
            }
        }

        OPT_RES_DIR_COLLECTION = new File[NUM_OPT_TO_KEEP];
        OPT_RES_SUM_SQS = new double[NUM_OPT_TO_KEEP];
        Arrays.fill(OPT_RES_SUM_SQS, Double.POSITIVE_INFINITY);

        importDir = new File(IMPORT_DIR_STR);
        exportDir = new File(BASE_DIR_STR);
        exportDir.mkdirs();

        File popSel = new File(exportDir, FILENAME_POP_SELECT);

        if (popSel.exists()) {
            ArrayList<File> popSelArr = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(popSel))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    popSelArr.add(new File(line));
                }
            }
            popFiles = popSelArr.toArray(new File[popSelArr.size()]);

            Files.move(popSel.toPath(),
                    new File(exportDir, FILENAME_POP_SELECT + "_" + System.currentTimeMillis()).toPath());

        } else {
            popFiles = importDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.getName().endsWith(".zip");
                }
            });
        }

        Arrays.sort(popFiles, new Comparator<File>() {
            @Override
            public int compare(File t, File t1) {
                return t.getName().compareTo(t1.getName());
            }
        });

        if (NUM_SIM_TOTAL > 0 && NUM_SIM_TOTAL < popFiles.length) {
            popFiles = Arrays.copyOf(popFiles, NUM_SIM_TOTAL);
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(popSel))) {
            for (int i = 0; i < popFiles.length; i++) {
                writer.println(popFiles[i].getAbsolutePath());
            }
        }
        System.out.println(popFiles.length + " file(s) will be used in optimisation.");
    }

    public void runOptimisation() throws IOException, ClassNotFoundException {
        ParameterConstraintTransform[] constraints;
        AbstractResidualFunc optimisationFunc;

        //<editor-fold defaultstate="collapsed" desc="Intialise constraints">   
        File costrainFile = new File(exportDir, FILENAME_PARAM_CONSTRIANTS);
        try (BufferedReader constraintReader = new BufferedReader(new FileReader(costrainFile))) {
            int lnNum = 0;
            String line;
            while (constraintReader.readLine() != null) {
                lnNum++;
            }
            constraints = new ParameterConstraintTransform[lnNum];
            lnNum = 0;
            BufferedReader constraintReader2 = new BufferedReader(new FileReader(costrainFile));

            while ((line = constraintReader2.readLine()) != null) {
                String[] ent = line.split(",");
                constraints[lnNum] = new transform.ParameterConstraintTransformSineCurve(new double[]{
                    Double.parseDouble(ent[0]), Double.parseDouble(ent[1])});
                lnNum++;
            }
        }
        //</editor-fold>

        optimisationFunc = new Opt_ResidualFunc(popFiles, exportDir,
                NUM_STEPS, NUM_THREADS, TARGET_PREVAL,
                OPT_RES_DIR_COLLECTION, OPT_RES_SUM_SQS, getPropModelInitStr()
        );

        AbstractParameterOptimiser opt = new NelderMeadOptimiser(optimisationFunc);

        // Initial value              
        double[] p0 = null;

        // Default p0, to be replace by imported if necessary
        File preP0 = new File(exportDir, FILENAME_P0);

        if (preP0.exists()) {
            ArrayList<String> p0_Arr = new ArrayList<>();
            try (BufferedReader p0Reader = new BufferedReader(new FileReader(preP0))) {
                String line;
                while ((line = p0Reader.readLine()) != null) {
                    p0_Arr.add(line);
                }
            }
            p0 = new double[p0_Arr.size()];
            int index = 0;
            for (String ent : p0_Arr) {
                p0[index] = Double.parseDouble(ent);
                index++;
            }

            System.out.println("P0 from " + preP0.getAbsolutePath() + " imported");
        }

        //<editor-fold defaultstate="collapsed" desc="Optimisation process">    
        double[] r0 = null;
        opt.setResOptions(false, AbstractParameterOptimiser.RES_OPTIONS_PRINT);
        opt.setFilename(exportDir.getAbsolutePath() + File.separator + FILENAME_OPT_RESULTS_CSV, AbstractParameterOptimiser.FILEPATH_CSV);
        opt.setFilename(exportDir.getAbsolutePath() + File.separator + FILENAME_OPT_RESULTS_OBJ, AbstractParameterOptimiser.FILEPATH_OBJ);
        opt.setFilename(exportDir.getAbsolutePath() + File.separator + FILENAME_OPT_SIMPLEX, NelderMeadOptimiser.FILEPATH_SIMPLEX);

        File preSimplexFile = new File(exportDir, FILENAME_OPT_SIMPLEX);
        File preX0CSV = new File(exportDir, FILENAME_PRE_SIMPLEX_CSV);

        double[][] sX = null;
        double[][] sR = null;
        if (preSimplexFile.exists()) {
            System.out.print("Reading previous simplex....");
            try (ObjectInputStream objStr = new ObjectInputStream(new FileInputStream(preSimplexFile))) {
                sX = (double[][]) objStr.readObject();
                sR = (double[][]) objStr.readObject();
            }
            preSimplexFile.renameTo(new File(exportDir, preSimplexFile.getName() + "_" + System.currentTimeMillis()));
            System.out.println(" done");
        } else if (preX0CSV.exists()) {
            System.out.println("Reading previous simplex CSV ....");
            BufferedReader lines = new BufferedReader(new FileReader(preX0CSV));
            BufferedReader rLines = null;
            File preR0CSV = new File(exportDir, FILENAME_PRE_RESIDUE);
            if (preR0CSV.exists()) {
                rLines = new BufferedReader(new FileReader(preR0CSV));
            }
            String line, rLine;
            int pt = 0;
            while ((line = lines.readLine()) != null) {
                String[] entries = line.split(",");
                if (sX == null) {
                    sX = new double[entries.length + 1][];
                    sR = new double[entries.length + 1][];
                }
                sX[pt] = new double[entries.length];
                for (int i = 0; i < entries.length; i++) {
                    sX[pt][i] = Double.parseDouble(entries[i]);
                }
                if (rLines != null) {
                    rLine = rLines.readLine();
                    if (rLine != null && !rLine.isEmpty()) {
                        entries = rLine.split(",");
                        sR[pt] = new double[entries.length];
                        for (int i = 0; i < entries.length; i++) {
                            sR[pt][i] = Double.parseDouble(entries[i]);
                        }
                    }
                }
                pt++;
            }
        }

        if (sX != null) {
            r0 = sR[0];
            if (p0 == null) {
                p0 = new double[sX[0].length];
            }
            for (int i = 0; i < p0.length; i++) {
                p0[i] = sX[0][i];
                if (constraints[i] != null) {
                    p0[i] = constraints[i].toContrainted(p0[i]);
                }
            }
        }

        System.out.println(
                "P0 = " + Arrays.toString(p0));
        if (r0 != null) {
            System.out.println("R0 = " + Arrays.toString(r0));
        }

        opt.setP0(p0, constraints);

        opt.setR0(r0);

        if (sX != null) {
            for (int i = 0; i < sX.length; i++) {
                if (sX[i] != null) {
                    System.out.println("Loading simplex vertex #" + i);
                    System.out.println(i + ": X = " + Arrays.toString(sX[i]));
                    if (sR[i] == null) {
                        System.out.println(i + ": R to be generated");
                    }
                    sR[i] = ((NelderMeadOptimiser) opt).setPreDefineSimplex(sX[i], sR[i], i);
                    System.out.println(i + ": R = " + Arrays.toString(sR[i]));
                }
            }
        }

        //</editor-fold>
        opt.initialise();
        opt.optimise();

    }

    public String[] getPropModelInitStr() {
        return propModelInitStr;
    }

    public void setPropModelInitStr(String[] propModelInitStr) {
        this.propModelInitStr = propModelInitStr;
    }

    public int getNumSteps() {
        return NUM_STEPS;
    }

    public void setNumSteps(int NUM_STEPS) {
        this.NUM_STEPS = NUM_STEPS;
    }
    
    

}
