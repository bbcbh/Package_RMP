package run;

import java.io.IOException;
import java.util.Arrays;
import opt.OptRun_Population_Remote_MetaPopulation_Infection_Intro;
import opt.OptRun_Population_Remote_MetaPopulation_Infection_Intro_GA;
import sim.Simulation_Remote_MetaPopulation;

/**
 * A common run file for all infection
 *
 * @author Ben Hui
 * @version 20180615
 * 
 * <pre>
 * History:
 * 
 * 20180615 
 *  - Combine simulation and optimisaiton as one run file
 * </pre>
 * 
 */
public class Run_Population_Remote_MetaPopulation_Pop_Intro_Infection {

    public static void main(String[] arg) {

        int type = -1;
        String[] rArg = null;
        if (arg.length >= 0) {
            type = Integer.parseInt(arg[0]);
            rArg = Arrays.copyOfRange(arg, 1, arg.length);
        }

        switch (type) {
            case 0:
                Run_Population_Remote_MetaPopulation_Pop_Intro_NG_CT runNGCT = new Run_Population_Remote_MetaPopulation_Pop_Intro_NG_CT(rArg);
                runNGCT.runSimulation();
                break;
            case 1:
                Run_Population_Remote_MetaPopulation_Pop_Intro_Syphilis runSyp = new Run_Population_Remote_MetaPopulation_Pop_Intro_Syphilis(rArg);
                runSyp.runSimulation();
                break;
            case 2:
                try {
                    Simulation_Remote_MetaPopulation.main(rArg);
                } catch (IOException | InterruptedException | ClassNotFoundException ex) {
                    ex.printStackTrace(System.err);
                }
                break;
            case 3:
                try {
                    OptRun_Population_Remote_MetaPopulation_Infection_Intro.main(rArg);
                } catch (IOException | ClassNotFoundException ex) {
                    ex.printStackTrace(System.err);
                }
                break;
            case 4:
                try {
                    OptRun_Population_Remote_MetaPopulation_Infection_Intro_GA.main(rArg);
                } catch (IOException | ClassNotFoundException ex) {
                    ex.printStackTrace(System.err);
                }
                break;
            default:
                System.err.println("Error: Illegal arg[0]. Set 0 for NG/CT Run, 1 for Syphilis, 2 for Simulation, 3 for Optimisation, 4 for Optimisaiton GA");
        }

    }

}
