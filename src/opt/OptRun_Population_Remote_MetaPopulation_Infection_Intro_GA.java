package opt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import optimisation.AbstractParameterOptimiser;
import optimisation.AbstractResidualFunc;
import optimisation.GeneticAlgorithmOptimiser;
import transform.ParameterConstraintTransform;

public class OptRun_Population_Remote_MetaPopulation_Infection_Intro_GA extends OptRun_Population_Remote_MetaPopulation_Infection_Intro {

    protected int GA_POP_SIZE = 1000;
    public static final String FILENAME_OPT_GA_STORE = "GA_POP.obj";

    public OptRun_Population_Remote_MetaPopulation_Infection_Intro_GA(String[] arg)
            throws IOException {        
        super(arg);        
        // 5: GA_Pop size
        if(arg.length > 5){
            if(!arg[5].isEmpty()){
                GA_POP_SIZE = Integer.parseInt(arg[5]);
            }            
        }
        
    }

    @Override
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
                getNumSteps(), Math.min(NUM_THREADS, NUM_SIM_TOTAL), 
                TARGET_PREVAL, TARGET_WEIGHT,
                OPT_RES_DIR_COLLECTION, OPT_RES_SUM_SQS, getPropModelInitStr());

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
    
       
    /*
    public static void main(String[] arg) throws IOException, ClassNotFoundException {
        OptRun_Population_Remote_MetaPopulation_Infection_Intro_GA run
                = new OptRun_Population_Remote_MetaPopulation_Infection_Intro_GA(arg);

        run.runOptimisation();

    }
    */

}
