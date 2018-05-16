/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import optimisation.AbstractParameterOptimiser;
import optimisation.AbstractResidualFunc;
import optimisation.GeneticAlgorithmOptimiser;
import optimisation.ParameterConstraintTransform;

public class OptRun_Population_Remote_MetaPopulation_Infection_Intro_GA {

    public String BASE_DIR_STR = "~/RMP/OptResults";
    public String IMPORT_DIR_STR = "~/RMP/ImportDir";
    public int NUM_THREADS = Runtime.getRuntime().availableProcessors();
    public int NUM_SIM_TOTAL = -1;

    public static final String FILENAME_POP_SELECT = "pop_select.txt";
    public static final String FILENAME_PARAM_CONSTRIANTS = "ParamConstriants.csv";
    public static final String FILENAME_OPT_RESULTS_CSV = "ParamOpt.csv";
    public static final String FILENAME_OPT_RESULTS_OBJ = "ParamOpt.obj";
    public static final String FILENAME_OPT_GA_STORE = "GA_POP.obj";
    public static final String FILENAME_P0 = "Pre_P0.csv";

    File importDir, exportDir;
    File[] popFiles;

    int NUM_OPT_TO_KEEP = 10;
    int GA_POP_SIZE = 1000;

    private File[] OPT_RES_DIR_COLLECTION;
    private double[] OPT_RES_SUM_SQS;

    private double[] TARGET_PREVAL = new double[]{
        0.118, 0.104, 0.074, 0.046, // CT, Male
        0.174, 0.082, 0.060, 0.035, // CT, Female
        0.137, 0.065, 0.040, 0.041, // NG, Male
        0.135, 0.076, 0.028, 0.043 // NG, Female              
    };

    public OptRun_Population_Remote_MetaPopulation_Infection_Intro_GA(String[] arg)
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
        
        // 5: GA_Pop size
        if(arg.length > 5){
            if(!arg[5].isEmpty()){
                GA_POP_SIZE = Integer.parseInt(arg[5]);
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
                constraints[lnNum] = new ParameterConstraintTransform(new double[]{
                    Double.parseDouble(ent[0]), Double.parseDouble(ent[1])});
                lnNum++;
            }
        }
        //</editor-fold>

        optimisationFunc = new Opt_ResidualFunc(popFiles, exportDir,
                Math.min(NUM_THREADS, NUM_SIM_TOTAL), TARGET_PREVAL,
                OPT_RES_DIR_COLLECTION, OPT_RES_SUM_SQS);

        AbstractParameterOptimiser opt = new GeneticAlgorithmOptimiser(optimisationFunc);
        opt.setResOptions(false, AbstractParameterOptimiser.RES_OPTIONS_PRINT);
        opt.setFilename(exportDir.getAbsolutePath() + File.separator + FILENAME_OPT_RESULTS_CSV, AbstractParameterOptimiser.FILEPATH_CSV);
        opt.setFilename(exportDir.getAbsolutePath() + File.separator + FILENAME_OPT_RESULTS_OBJ, AbstractParameterOptimiser.FILEPATH_OBJ);
        opt.setParameter(GeneticAlgorithmOptimiser.PARAM_GA_OPT_POP_FILE, new File(BASE_DIR_STR, FILENAME_OPT_GA_STORE));
        opt.setParameter(GeneticAlgorithmOptimiser.PARAM_GA_OPT_USE_PARALLEL, NUM_THREADS);
        opt.setParameter(GeneticAlgorithmOptimiser.PARAM_GA_OPT_POP_SIZE, GA_POP_SIZE);
        
        System.out.println("NUMBER OF PARAMETERS = " + constraints.length);
        
        
        
        

        // Initial value              
        double[] p0 = new double[]{constraints.length};
        double[] r0 = new double[TARGET_PREVAL.length];
        Arrays.fill(r0, Double.NaN);

        // Default p0, to be replace by imported if necessary
        File preP0 = new File(exportDir, FILENAME_P0);
        File preGAPop = new File(BASE_DIR_STR, FILENAME_OPT_GA_STORE);

        if (!preGAPop.exists()) {

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
        }
        //<editor-fold defaultstate="collapsed" desc="Optimisation process">          

        opt.setP0(p0, constraints);
        opt.setR0(r0);

        //</editor-fold>
        opt.initialise();
        opt.optimise();

    }

    public static void main(String[] arg) throws IOException, ClassNotFoundException {
        OptRun_Population_Remote_MetaPopulation_Infection_Intro_GA run
                = new OptRun_Population_Remote_MetaPopulation_Infection_Intro_GA(arg);

        run.runOptimisation();

    }

}
