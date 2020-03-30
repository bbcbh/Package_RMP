/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package run;

import infection.AbstractInfection;
import infection.COVID19_Remote_Infection;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import person.AbstractIndividualInterface;
import person.Person_Remote_MetaPopulation;
import population.Population_Remote_MetaPopulation;
import population.Population_Remote_MetaPopulation_COVID19;
import relationship.RelationshipMap;
import util.Factory_FullAgeUtil;
import util.PersonClassifier;

/**
 *
 * @author Ben Hui
 */
class Thread_PopRun_COVID19 implements Runnable {

    final int threadId;
    final int numSnap;
    final int snapFreq;
    final File baseDir;

    final PersonClassifier full_AgeGrp_PersonClassifier;
    PrintWriter pri;

    Population_Remote_MetaPopulation_COVID19 pop;
    AbstractInfection[] infList;

    public Thread_PopRun_COVID19(int threadId, File baseDir, int numSnap, int snapFreq) throws FileNotFoundException {

        this.threadId = threadId;
        this.baseDir = baseDir;
        this.numSnap = numSnap;
        this.snapFreq = snapFreq;

        this.full_AgeGrp_PersonClassifier = Factory_FullAgeUtil.genFullAgeClassifier();

        try {
            this.pri = new PrintWriter(new File(baseDir, String.format("output_%d.txt", threadId)));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace(System.err);
            this.pri = new PrintWriter(System.out);
        }
    }

    public Population_Remote_MetaPopulation_COVID19 getPop() {
        return pop;
    }

    public void setPop(Population_Remote_MetaPopulation_COVID19 pop) {
        this.pop = pop;
    }

    public void setInfList(AbstractInfection[] infList) {
        this.infList = infList;
    }

    public int getThreadId() {
        return threadId;
    }

    @Override
    public void run() {
        pri.println("Generating population with seed of " + pop.getSeed());
        int[] popSize = (int[]) pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_POP_SIZE];
        pri.println("Pop Size = " + Arrays.toString(popSize));
        int[] popType = (int[]) pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_POP_TYPE];
        pri.println("Pop type = " + Arrays.toString(popType));
        int[][] popConnc = (int[][]) pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_POP_CONNC];
        pri.println("Pop connc = " + Arrays.deepToString(popConnc));

        pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_AGE_GRP_CLASSIFIER] = full_AgeGrp_PersonClassifier;

        int[][] decomp = (int[][]) pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_AGE_GENDER_COMPOSITION];
        float[][] awayFromHome = (float[][]) pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_AWAY_FROM_HOME_BY_LOCATION];
        double[][] householdSizeDist = (double[][]) pop.getFields()[Population_Remote_MetaPopulation_COVID19.FIELDS_REMOTE_METAPOP_COVID19_HOSUEHOLD_SIZE_DIST];
        double[][] nonHouseholdContactDist = (double[][]) pop.getFields()[Population_Remote_MetaPopulation_COVID19.FIELDS_REMOTE_METAPOP_COVID19_NON_HOUSEHOLD_CONTACT_DIST];

        if (decomp == null || decomp.length == 0) {
            decomp = new int[popSize.length][];
            for (int p = 0; p < popSize.length; p++) {
                decomp[p] = popType[p] == 2
                        ? util.Factory_Population_Decompositiion_Full.getDecomposition(
                                util.Factory_Population_Decompositiion_Full.ALL_POP_COMPOSITION_REGIONAL_2019, popSize[p], pop.getRNG())
                        : util.Factory_Population_Decompositiion_Full.getDecomposition(
                                util.Factory_Population_Decompositiion_Full.ALL_POP_COMPOSITION_REMOTE_2019, popSize[p], pop.getRNG());
            }
            pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_AGE_GENDER_COMPOSITION] = decomp;
        }

        if (awayFromHome == null || awayFromHome.length == 0) {
            awayFromHome = new float[popSize.length][];
            for (int p = 0; p < popSize.length; p++) {                
                awayFromHome[p] = Factory_FullAgeUtil.getProportionPopAwayFull(popType[p]);
            }
            pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_AWAY_FROM_HOME_BY_LOCATION] = awayFromHome;
        }

        if (householdSizeDist == null || householdSizeDist.length == 0) {
            householdSizeDist = new double[popSize.length][];
            for (int p = 0; p < popSize.length; p++) {
                // Bailie and Wayte (2006)
                householdSizeDist[p] = new double[]{0.67, 4.7, 1, 3.4};
            }
            pop.getFields()[Population_Remote_MetaPopulation_COVID19.FIELDS_REMOTE_METAPOP_COVID19_HOSUEHOLD_SIZE_DIST] = householdSizeDist;
        }

        if (nonHouseholdContactDist == null || nonHouseholdContactDist.length == 0) {
            nonHouseholdContactDist = new double[popSize.length][];
            for (int p = 0; p < popSize.length; p++) {
                // From ABS 41590DO004_2014 General Social Survey, Summary Results, Australia, 2014
                nonHouseholdContactDist[p] = popType[p] == 2 ? new double[]{0.037, 0, 0, 0.089, 1 / 90, 1 / 30, 0.248, 1 / 30, 1 / 7, 0.793, 1 / 7, 1, 1, 1, 1} : new double[]{0.041, 0, 0, 0.083, 1 / 90, 1 / 30, 0.245, 1 / 30, 1 / 7, 0.751, 1 / 7, 1, 1, 1, 1};
            }
            pop.getFields()[Population_Remote_MetaPopulation_COVID19.FIELDS_REMOTE_METAPOP_COVID19_NON_HOUSEHOLD_CONTACT_DIST] = nonHouseholdContactDist;
        }

        pop.initialise();
        pop.allolocateHosuehold();

        // Household stat
        pop.printHousholdStat(pri);

        // Intitialise infection and patient zero
        pop.setInfectionList(infList);
        pop.setAvailability(null);
        COVID19_Remote_Infection covid = (COVID19_Remote_Infection) pop.getInfList()[0];
        RelationshipMap householdMap = pop.getRelMap()[Population_Remote_MetaPopulation_COVID19.RELMAP_HOUSEHOLD];
        AbstractIndividualInterface patientZero = pop.getPop()[pop.getRNG().nextInt(pop.getPop().length)];
        // Make sure patientZero has household size > 1
        while (householdMap.degreeOf(patientZero.getId()) < 2 || ((Person_Remote_MetaPopulation) patientZero).getHomeLocation() != 0) {
            patientZero = pop.getPop()[pop.getRNG().nextInt(pop.getPop().length)];
        }
        covid.infecting(patientZero);
        pri.println();
        String key;
        key = COVID19_Remote_Infection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", "" + COVID19_Remote_Infection.DIST_RO_RAW_INDEX);
        double[] r0 = (double[]) covid.getParameter(key);
        pri.println("\"R0\" = " + Arrays.toString(r0));
        pri.println("Patient zero: ");
        pri.println("Id = " + patientZero.getId());
        pop.printPatientStat(patientZero, pri);
        
        boolean priClose = true;

        PrintWriter outputCSV;
        try {
            outputCSV = new PrintWriter(new File(baseDir, String.format("snapStat_%d.csv", this.threadId)));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace(System.err);
            outputCSV = pri;
            priClose = false;
        }
        
        if(priClose){
            pri.close();
        }
        
        pop.printCSVOutputHeader(outputCSV);
        pop.printCSVOutputEntry(outputCSV);
        for (int sn = 0; sn < numSnap; sn++) {
            for (int sf = 0; sf < snapFreq; sf++) {
                pop.advanceTimeStep(1);
            }
            pop.printCSVOutputEntry(outputCSV);                        
            //System.out.println(pop.getGlobalTime());
        }
        
        outputCSV.close();        
        pri.close();
    }

}
