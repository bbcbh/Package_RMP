package run;

import java.util.Arrays;

/**
 * A common run file for all infection
 *
 * @author Ben Hui
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
            default:
                System.err.println("Error: Illegal arg[0]. Set 0 for NG/CT and 1 for Syphilis");
        }

    }

}
