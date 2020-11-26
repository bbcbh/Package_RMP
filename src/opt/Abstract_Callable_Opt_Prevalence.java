/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opt;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.concurrent.Callable;
import person.AbstractIndividualInterface;
import population.Population_Remote_MetaPopulation;
import run.Thread_PopRun;
import util.Default_Remote_MetaPopulation_AgeGrp_Classifier;
import util.PersonClassifier;
import util.PropValUtils;

/**
 *
 * @author Ben Hui
 */
public abstract class Abstract_Callable_Opt_Prevalence implements Callable<double[]> {

    protected File popFile;
    protected File optOutputDir;
    protected int simId;
    protected int numStep;
    protected double[] param;
    protected boolean outputAsFile = true;
    protected boolean printOutput = false;

    protected double[] target_weight;
    protected double[] target_preval;

    protected int[] pop_type_incl_for_residue = null;

    // Addtional fields
    protected String[] propModelInitStr;

    public Abstract_Callable_Opt_Prevalence() {
    }

    public void setOutputAsFile(boolean outputAsFile) {
        this.outputAsFile = outputAsFile;
    }

    public void setTarget_weight(double[] target_weight) {
        this.target_weight = target_weight;
    }

    public void setPrintOutput(boolean printOutput) {
        this.printOutput = printOutput;
    }

    public abstract void loadParameters(Thread_PopRun thread, double[] param);

    public void setTarget_preval(double[] target_preval) {
        this.target_preval = target_preval;
    }

    protected void initModelPropStr(double[] param, Thread_PopRun thread) throws NumberFormatException {

        PrintWriter outputPrint = thread.getOutputPri();
        for (int i = 0; i < propModelInitStr.length; i++) {
            if (propModelInitStr[i] != null && !propModelInitStr[i].isEmpty()) {
                if (i < Callable_Opt_Prevalence.OPT_PARAM_TOTAL) {
                    try {
                        param[i] = Float.parseFloat(propModelInitStr[i]);
                    } catch (NumberFormatException ex) {                        
                        param[i] = Float.NaN;
                    }
                } else if (i - Callable_Opt_Prevalence.OPT_PARAM_TOTAL < Thread_PopRun.PARAM_TOTAL) {
                    thread.getInputParam()[i - Callable_Opt_Prevalence.OPT_PARAM_TOTAL] = PropValUtils.propStrToObject(propModelInitStr[i], thread.getInputParam()[i - Callable_Opt_Prevalence.OPT_PARAM_TOTAL].getClass());
                } else {
                    int popIndex = i - Callable_Opt_Prevalence.OPT_PARAM_TOTAL - Thread_PopRun.PARAM_TOTAL;
                    thread.updatePopFieldFromString(popIndex, propModelInitStr[i]);
                }
                if (outputPrint != null) {
                    outputPrint.println("Model init. setting #" + i + " = " + propModelInitStr[i]);
                }
            }
        }
    }

    /**
     * Call function
     *
     * @return The difference between model generated and target prevalence
     * @throws Exception
     */
    @Override
    public double[] call() throws Exception {
        double[] res_single = new double[target_preval.length];
        File outputPopFile = null;
        if (optOutputDir != null) {
            outputPopFile = new File(optOutputDir, "Opt_" + popFile.getName());
        }
        Thread_PopRun thread = new Thread_PopRun(outputPopFile, popFile, simId, numStep);
        PrintWriter outputPrint = null;
        if (outputAsFile && optOutputDir != null) {
            try {
                outputPrint = new PrintWriter(new FileWriter(new File(optOutputDir, "output_" + simId + ".txt")));
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
                outputPrint = new PrintWriter(System.out);
            }
        }
        if (printOutput) {
            outputPrint = new PrintWriter(System.out);
        }
        thread.setOutputPri(outputPrint, false);
        // Set up parameter
        thread.importPop();
        loadParameters(thread, param);
        thread.run();
        PersonClassifier prevalClassifer = new PersonClassifier() {
            PersonClassifier ageClassifier = new Default_Remote_MetaPopulation_AgeGrp_Classifier();

            @Override
            public int classifyPerson(AbstractIndividualInterface p) {
                int aI = ageClassifier.classifyPerson(p);
                if (aI >= 0) {
                    return p.isMale() ? aI : (aI + ageClassifier.numClass());
                } else {
                    return -1;
                }
            }

            @Override
            public int numClass() {
                return ageClassifier.numClass() * 2;
            }
        };

        int[] numInGroup;
        int[] numInfect;

        numInGroup = new int[prevalClassifer.numClass()];
        numInfect = new int[prevalClassifer.numClass() * thread.getPop().getInfList().length];
        AbstractIndividualInterface[] allPerson = thread.getPop().getPop();

        for (AbstractIndividualInterface person : allPerson) {
            // Remote only
            int loc = ((Population_Remote_MetaPopulation) thread.getPop()).getCurrentLocation(person);
            int popType = ((int[]) ((Population_Remote_MetaPopulation) thread.getPop()).getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_POP_TYPE])[loc];

            if (pop_type_incl_for_residue == null
                    || Arrays.binarySearch(pop_type_incl_for_residue, popType) >= 0) {
                int cI = prevalClassifer.classifyPerson(person);
                numInGroup[cI]++;
                if (person.getInfectionStatus()[0] != AbstractIndividualInterface.INFECT_S) {
                    numInfect[cI]++;
                }
                if (person.getInfectionStatus()[1] != AbstractIndividualInterface.INFECT_S) {
                    numInfect[prevalClassifer.numClass() + cI]++;
                }
            }
        }
        for (int i = 0; i < target_preval.length; i++) {
            res_single[i] = target_weight[i] * ((double) numInfect[i]) / numInGroup[i % numInGroup.length] - target_preval[i];
        }
        if (outputPrint != null) {
            outputPrint.println("Residue (i.e. model preval - target preval) = " + Arrays.toString(res_single));
            outputPrint.close();
        }
        return res_single;
    }

}
