package population;

import infection.AbstractInfection;
import infection.COVID19_Remote_Infection;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import person.AbstractIndividualInterface;
import person.Person_Remote_MetaPopulation;
import relationship.RelationshipMap;
import relationship.RelationshipMapTimeStamp;
import relationship.SingleRelationship;
import relationship.SingleRelationshipTimeStamp;
import util.ArrayUtilsRandomGenerator;

/**
 *
 * @author Ben Hui
 */
public class Population_Remote_MetaPopulation_COVID19 extends Population_Remote_MetaPopulation {

    public static final int FIELDS_REMOTE_METAPOP_COVID19_HOSUEHOLD_SIZE_DIST = LENGTH_FIELDS_REMOTE_META_POP;
    public static final int FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD = FIELDS_REMOTE_METAPOP_COVID19_HOSUEHOLD_SIZE_DIST + 1;
    public static final int FIELDS_REMOTE_METAPOP_COVID19_NON_HOUSEHOLD_CONTACT_DIST = FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD + 1;

    Object[] DEFAULT_FIELDS_REMOTE_METAPOP_COVID19 = {
        // FIELDS_REMOTE_METAPOP_COVID19_HOSUEHOLD_SIZE_DIST
        // Format: [loc][cumul_proproption_of_pop_1,mean_household_size_1 , cumul_percent_of_pop_2 ...
        new double[][]{},
        // FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD
        // int[loc][] list of key index (i.e. min id) of unqique household
        new Integer[][]{},
        // FIELDS_REMOTE_METAPOP_COVID19_NON_HOUSEHOLD_CONTACT_DIST
        // Format:[loc][cumul_proproption_of_contact_1, per_day_contact_rate_1 ...]
        new double[][]{},};

    public static final int RELMAP_HOUSEHOLD = 0;
    private static final int INF_COVID19 = 0;

    private transient HashMap<List<Integer>, int[]> incidence_collection = new HashMap<>();
    public static final int INCIDENCE_COLLECTION_NUM_TRANMISSION_HOUSHOLD_ATTEMPT = 0;
    public static final int INCIDENCE_COLLECTION_NUM_TRANMISSION_HOUSHOLD_SUC = INCIDENCE_COLLECTION_NUM_TRANMISSION_HOUSHOLD_ATTEMPT + 1;
    public static final int INCIDENCE_COLLECTION_NUM_TRANMISSION_NON_HOUSEHOLD_ATTEMPT = INCIDENCE_COLLECTION_NUM_TRANMISSION_HOUSHOLD_SUC + 1;
    public static final int INCIDENCE_COLLECTION_NUM_TRANMISSION_NON_HOUSEHOLD_SUC = INCIDENCE_COLLECTION_NUM_TRANMISSION_NON_HOUSEHOLD_ATTEMPT + 1;
    public static final int LENGTH_INCIDENCE_COOLECTION = INCIDENCE_COLLECTION_NUM_TRANMISSION_NON_HOUSEHOLD_SUC + 1;

    private transient HashMap<Integer, Double> nonHouseholdContactRate = new HashMap<>();

    public Population_Remote_MetaPopulation_COVID19(long seed) {
        super(seed);

        Object[] orgFields = this.getFields();
        Object[] newFields = Arrays.copyOf(orgFields, orgFields.length + DEFAULT_FIELDS_REMOTE_METAPOP_COVID19.length);
        System.arraycopy(DEFAULT_FIELDS_REMOTE_METAPOP_COVID19, 0, newFields, orgFields.length, DEFAULT_FIELDS_REMOTE_METAPOP_COVID19.length);
        this.setFields(newFields);

        // To disable sexual behaviour / partner forming code
        setAvailability(null);
    }

    protected int modelMaxAge() {
        return 64 * AbstractIndividualInterface.ONE_YEAR_INT;
    }

    private int[] inSameHouseholdAs(AbstractIndividualInterface p) {
        RelationshipMap housholdMap = getRelMap()[RELMAP_HOUSEHOLD];
        if (housholdMap.containsVertex(p.getId())) {
            int[] res = new int[housholdMap.degreeOf(p.getId()) + 1];
            res[0] = p.getId();
            int r = 1;
            for (SingleRelationship e : housholdMap.edgesOf(p.getId())) {
                res[r] = e.getLinksValues()[0] == p.getId()
                        ? e.getLinksValues()[1] : e.getLinksValues()[0];
                r++;
            }
            Arrays.sort(res);
            return res;
        } else {
            return new int[]{p.getId()};
        }
    }

    public void allolocateHosuehold() {

        int[] popSize = (int[]) getFields()[FIELDS_REMOTE_METAPOP_POP_SIZE];
        RelationshipMap householdlMap = getRelMap()[RELMAP_HOUSEHOLD];

        getFields()[FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD] = new Integer[popSize.length][];
        AbstractIndividualInterface[][] mapping = new AbstractIndividualInterface[popSize.length][];
        int[] mappingIndex = new int[popSize.length];

        PoissonDistribution poi;
        double meanHouseHoldSize;

        for (int p = 0; p < popSize.length; p++) {
            mapping[p] = new Person_Remote_MetaPopulation[popSize[p]];
        }

        for (AbstractIndividualInterface person : getPop()) {
            int homeLoc = ((Person_Remote_MetaPopulation) person).getHomeLocation();
            int index = mappingIndex[homeLoc];
            mapping[homeLoc][index] = person;
            mappingIndex[homeLoc]++;
        }

        for (int loc = 0; loc < mapping.length; loc++) {
            AbstractIndividualInterface[] resident = mapping[loc];
            ArrayUtilsRandomGenerator.shuffleArray(resident, getRNG());
            double[] householdSizeDist = ((double[][]) getFields()[FIELDS_REMOTE_METAPOP_COVID19_HOSUEHOLD_SIZE_DIST])[loc];
            int housePt = 0;

            ArrayList<Integer> uniqueHousehold = new ArrayList<>();

            meanHouseHoldSize = householdSizeDist[housePt + 1];
            poi = new PoissonDistribution(getRNG(), meanHouseHoldSize,
                    PoissonDistribution.DEFAULT_EPSILON, PoissonDistribution.DEFAULT_MAX_ITERATIONS);
            int rPt = 0;

            while (rPt < resident.length) {

                if (rPt >= Math.round(householdSizeDist[housePt] * resident.length)
                        && housePt < householdSizeDist.length - -2) {

                    while (rPt >= Math.round(householdSizeDist[housePt] * resident.length)
                            && housePt < householdSizeDist.length - -2) {
                        housePt += 2;
                    }

                    housePt = Math.min(housePt, householdSizeDist.length - 2);
                    meanHouseHoldSize = householdSizeDist[housePt + 1];
                    poi = new PoissonDistribution(getRNG(), meanHouseHoldSize,
                            PoissonDistribution.DEFAULT_EPSILON, PoissonDistribution.DEFAULT_MAX_ITERATIONS);
                }

                int numInHosuse = poi.sample();
                int firstInHouse = rPt;
                int uniqueHouseholdMinPid = resident[firstInHouse].getId();

                householdlMap.addVertex(resident[firstInHouse].getId());

                while ((rPt + 1) < resident.length && numInHosuse > 1) {
                    rPt++;
                    uniqueHouseholdMinPid = Math.min(uniqueHouseholdMinPid, resident[rPt].getId());
                    for (int r = firstInHouse; r < rPt; r++) {
                        if (!householdlMap.containsVertex(resident[r].getId())) {
                            householdlMap.addVertex(resident[r].getId());
                        }
                        if (!householdlMap.containsVertex(resident[rPt].getId())) {
                            householdlMap.addVertex(resident[rPt].getId());
                        }
                        SingleRelationship rel = new SingleRelationshipTimeStamp(
                                new Integer[]{resident[r].getId(), resident[rPt].getId()});
                        householdlMap.addEdge(resident[r].getId(), resident[rPt].getId(), rel);
                        rel.setDurations(Double.POSITIVE_INFINITY); // Never expires
                    }

                    numInHosuse--;
                }
                uniqueHousehold.add(uniqueHouseholdMinPid);
                rPt++;
            }
            ((Integer[][]) getFields()[FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD])[loc]
                    = uniqueHousehold.toArray(new Integer[uniqueHousehold.size()]);
            Arrays.sort(((Integer[][]) getFields()[FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD])[loc]);
        }

    }

    @Override
    public void advanceTimeStep(int deltaT) {

        ArrayList<AbstractIndividualInterface> currentlyInfectious = new ArrayList<>();
        COVID19_Remote_Infection covid19 = (COVID19_Remote_Infection) getInfList()[INF_COVID19];
        ArrayList<ArrayList<AbstractIndividualInterface>> currentlyAt = new ArrayList<>(
                ((int[]) getFields()[FIELDS_REMOTE_METAPOP_POP_SIZE]).length);

        super.advanceTimeStep(deltaT);

        // Transmission
        for (AbstractIndividualInterface p : getPop()) {
            if (covid19.isInfectious(p)) {
                currentlyInfectious.add(p);
            }
            int loc = getCurrentLocation(p);
            while (currentlyAt.size() <= loc) {
                currentlyAt.add(new ArrayList<>());
            }
            currentlyAt.get(loc).add(p);

        }

        for (AbstractIndividualInterface infectious : currentlyInfectious) {

            double[] infStat = covid19.getCurrentlyInfected().get(infectious.getId());
            int ageOfExp = (int) infStat[COVID19_Remote_Infection.PARAM_AGE_OF_EXPOSURE];

            List<Integer> key = new ArrayList<>(2);
            key.add(infectious.getId());
            key.add(ageOfExp);

            int[] ent = incidence_collection.get(key);

            if (ent == null) {
                incidence_collection.put(key, new int[LENGTH_INCIDENCE_COOLECTION]);
                ent = incidence_collection.get(key);
            }

            // Infecting housefold 
            int[] sameHousehold = inSameHouseholdAs(infectious);
            int infectiousLoc = getCurrentLocation(infectious);
            for (int pid : sameHousehold) {
                if (pid != infectious.getId()) {
                    AbstractIndividualInterface target = getLocalData().get(pid);
                    if (infectiousLoc == getCurrentLocation(target)) {
                        if (covid19.couldTransmissInfection(infectious, target)) {
                            ent[INCIDENCE_COLLECTION_NUM_TRANMISSION_HOUSHOLD_ATTEMPT]++;
                            if (infectionAttempt(covid19, infectious, target)) {
                                ent[INCIDENCE_COLLECTION_NUM_TRANMISSION_HOUSHOLD_SUC]++;
                            }
                        }
                    }
                }
            }

            ArrayList<AbstractIndividualInterface> possibleCandidate = new ArrayList<>();
            for (AbstractIndividualInterface candidate : currentlyAt.get(infectiousLoc)) {
                if (Arrays.binarySearch(sameHousehold, candidate.getId()) < 0) {
                    possibleCandidate.add(candidate);
                }
            }

            if (possibleCandidate.size() > 0) {

                // Infecting non-household
                Double nh_contactRate = nonHouseholdContactRate.get(infectious.getId());

                if (nh_contactRate == null) {
                    nh_contactRate = generateNonHouseholdContactRate(infectious);
                }

                int numContact = nh_contactRate.intValue();

                if (nh_contactRate < 1 && nh_contactRate > 0) {
                    if (getRNG().nextDouble() < nh_contactRate) {
                        numContact++;
                    }
                }

                while (numContact > 0 && possibleCandidate.size() > 0) {
                    int possIndex = getRNG().nextInt(possibleCandidate.size());
                    AbstractIndividualInterface target = possibleCandidate.get(possIndex);
                    if (covid19.couldTransmissInfection(infectious, target)) {
                        ent[INCIDENCE_COLLECTION_NUM_TRANMISSION_NON_HOUSEHOLD_ATTEMPT]++;
                        if (infectionAttempt(covid19, infectious, target)) {
                            ent[INCIDENCE_COLLECTION_NUM_TRANMISSION_NON_HOUSEHOLD_SUC]++;
                        }
                    }
                    possibleCandidate.remove(possIndex);
                    numContact--;
                }

            }
        }

    }

    protected Double generateNonHouseholdContactRate(AbstractIndividualInterface infectious) {
        double nh_contactRate;
        double pC = getRNG().nextDouble();
        int homeLoc = ((Person_Remote_MetaPopulation) infectious).getHomeLocation();
        double[] ncDist = ((double[][]) getFields()[FIELDS_REMOTE_METAPOP_COVID19_NON_HOUSEHOLD_CONTACT_DIST])[homeLoc];
        int k = 0;
        while (pC >= ncDist[k] && k + 2 < ncDist.length) {
            k += 3;
        }
        nh_contactRate = ncDist[k + 1];
        if (ncDist[k + 2] > ncDist[k + 1]) {
            if (ncDist[k + 2] <= 1) {
                nh_contactRate += getRNG().nextFloat() * (ncDist[k + 2] - ncDist[k + 1]);
            } else {
                nh_contactRate += getRNG().nextInt((int) (ncDist[k + 2] - ncDist[k + 1]));
            }
        }
        nonHouseholdContactRate.put(infectious.getId(), nh_contactRate);
        return nh_contactRate;
    }

    public HashMap<List<Integer>, int[]> getIncidence_collection() {
        return incidence_collection;
    }

    protected boolean infectionAttempt(AbstractInfection inf,
            AbstractIndividualInterface src, AbstractIndividualInterface target) {
        if (inf instanceof COVID19_Remote_Infection) {
            COVID19_Remote_Infection covid19 = (COVID19_Remote_Infection) inf;

            double[] param = covid19.getCurrentlyInfected().get(src.getId());

            double r0 = param[COVID19_Remote_Infection.PARAM_R0_INFECTED];

            // For now just assume uniform tranmission probabiliy per day
            double[] infectious_dur = (double[]) covid19.getParameter(AbstractInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceFirst("\\d+",
                    Integer.toString(COVID19_Remote_Infection.DIST_INFECTIOUS_DUR_INDEX)));
            double tranmissionProbPerContact = r0 / infectious_dur[0];

            if (covid19.getRNG().nextDouble() < tranmissionProbPerContact) {
                covid19.infecting(target);
                return true;
            } else {
                return false;
            }

        } else {
            if (inf != null) {
                throw new UnsupportedOperationException(this.getClass().getName()
                        + ".infectionAttempt for <"
                        + inf.getClass().getName() + "> to be implemented");
            } else {
                return false;
            }
        }

    }

    @Override
    public Person_Remote_MetaPopulation replacePerson(Person_Remote_MetaPopulation removedPerson, int nextId) {

        RelationshipMap householdMap = getRelMap()[RELMAP_HOUSEHOLD];
        HashSet<Integer> connected = new HashSet<>();
        if (householdMap.containsVertex(removedPerson.getId())) {
            SingleRelationship[] relArr = new SingleRelationship[0];
            relArr = householdMap.edgesOf(removedPerson.getId()).toArray(relArr);

            for (SingleRelationship rel : relArr) {
                int[] linked = rel.getLinksValues();
                connected.add(linked[0] == removedPerson.getId() ? linked[1] : linked[0]);
            }
        }

        Person_Remote_MetaPopulation newPerson = super.replacePerson(removedPerson, nextId);

        if (!connected.isEmpty()) {
            householdMap.addVertex(newPerson.getId());
        }

        for (Integer sameHouseholdId : connected.toArray(new Integer[connected.size()])) {
            SingleRelationship rel = new SingleRelationshipTimeStamp(new Integer[]{newPerson.getId(), sameHouseholdId});

            if (!householdMap.containsVertex(sameHouseholdId)) {
                householdMap.addVertex(sameHouseholdId);
            }

            householdMap.addEdge(newPerson.getId(), sameHouseholdId, rel);
            rel.setDurations(Double.POSITIVE_INFINITY); // Never expires
        }

        Integer[] uniqueHouseholdAtLoc
                = ((Integer[][]) getFields()[FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD])[newPerson.getHomeLocation()];

        int k = Arrays.binarySearch(uniqueHouseholdAtLoc, removedPerson.getId());
        if (k >= 0) {
            uniqueHouseholdAtLoc[k] = inSameHouseholdAs(newPerson)[0];
            Arrays.sort(uniqueHouseholdAtLoc);
        }

        return newPerson;
    }

    @Override
    public void initialise() {
        int[] popSizes = (int[]) getFields()[FIELDS_REMOTE_METAPOP_POP_SIZE];
        int totalPopSize = 0;

        // Intialise infection using field input
        AbstractInfection[] infList = (AbstractInfection[]) getFields()[FIELDS_REMOTE_METAPOP_INFECTION_LIST];
        updateInfectionList(infList);

        // Initalise population
        int popId = (int) getFields()[FIELDS_NEXT_ID];
        int[][] pop_decom_collection = (int[][]) getFields()[FIELDS_REMOTE_METAPOP_AGE_GENDER_COMPOSITION];

        for (int i = 0; i < popSizes.length; i++) {
            totalPopSize += popSizes[i];
        }

        AbstractIndividualInterface[] pop = new AbstractIndividualInterface[totalPopSize];

        for (int locId = 0; locId < popSizes.length; locId++) {
            int pt = locId;
            int locTotal = 0;
            int[] pop_decom = Arrays.copyOf(pop_decom_collection[pt], pop_decom_collection[pt].length);

            for (int newP = 0; newP < popSizes[locId]; newP++) {
                int pPerson = getRNG().nextInt(popSizes[locId] - locTotal);

                int personType = 0;

                while (pPerson >= pop_decom[personType]) {
                    pPerson = pPerson - pop_decom[personType];
                    personType++;
                }

                // Remove one from selected
                pop_decom[personType]--;
                locTotal++;

                int age, delta_age, firstSeekPartnerAge;

                int kStart = (personType < pop_decom.length / 2) ? 0 : pop_decom.length / 2;

                // One year age band
                age = 0;

                for (int k = kStart; k < personType; k++) {
                    age += AbstractIndividualInterface.ONE_YEAR_INT;
                }

                delta_age = AbstractIndividualInterface.ONE_YEAR_INT;

                age += getRNG().nextInt(delta_age);

                // From GOANNA, pg 22, age of first sex < 16 
                float upBound = 0.85f;
                if (age > 20 * AbstractIndividualInterface.ONE_YEAR_INT) {
                    upBound = 0.64f;
                }
                if (age > 25 * AbstractIndividualInterface.ONE_YEAR_INT) {
                    upBound = 0.63f;
                }

                firstSeekPartnerAge = getRNG().nextFloat()
                        < upBound ? age : 16 * AbstractIndividualInterface.ONE_YEAR_INT;

                pop[popId] = new Person_Remote_MetaPopulation(popId,
                        personType < (pop_decom.length / 2),
                        getGlobalTime(),
                        age,
                        firstSeekPartnerAge,
                        getInfList().length, locId);

                movePerson(pop[popId], locId, -1); // Currently at home            
                popId++;
            }
        }

        getFields()[FIELDS_NEXT_ID] = popId;
        this.setPop(pop);

        // Intialise map and availability
        setRelMap(new RelationshipMap[]{new RelationshipMapTimeStamp()}); // Single relationship map for all location

        // Availability not used in this model
        setAvailability(null);
    }

    public void setInfectionList(AbstractInfection[] infList) {
        setInfList(infList);
    }

    // Debug
    public static void main(String[] arg) throws IOException {

        Population_Remote_MetaPopulation_COVID19 pop = new Population_Remote_MetaPopulation_COVID19(2251913970037127827l);

        pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_POP_SIZE] = new int[]{5000, 100, 100, 100, 100};
        pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_POP_TYPE] = new int[]{2, 3, 3, 3, 3};
        pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_POP_CONNC]
                = new int[][]{
                    new int[]{0, 1, 1, 1, 1},
                    new int[]{1, 0, -1, -1, -1},
                    new int[]{1, -1, 0, -1, -1},
                    new int[]{1, -1, -1, 0, -1},
                    new int[]{1, -1, -1, -1, 0}};

        pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_AGE_GENDER_COMPOSITION]
                = new int[][]{
                    util.Factory_Population_Decomposition.getDecomposition(util.Factory_Population_Decomposition.POP_COMPOSITION_REGIONAL_2010, 5000, pop.getRNG()),
                    util.Factory_Population_Decomposition.getDecomposition(util.Factory_Population_Decomposition.POP_COMPOSITION_REMOTE_2010, 100, pop.getRNG()),
                    util.Factory_Population_Decomposition.getDecomposition(util.Factory_Population_Decomposition.POP_COMPOSITION_REMOTE_2010, 100, pop.getRNG()),
                    util.Factory_Population_Decomposition.getDecomposition(util.Factory_Population_Decomposition.POP_COMPOSITION_REMOTE_2010, 100, pop.getRNG()),
                    util.Factory_Population_Decomposition.getDecomposition(util.Factory_Population_Decomposition.POP_COMPOSITION_REMOTE_2010, 100, pop.getRNG())};

        // From Biddle 2009
        pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_AWAY_FROM_HOME_BY_LOCATION]
                = //  Male: 15-19, 20-24, 25-29, 30-34,  Female: 15-19, 20-24, 25-29, 30-34
                new float[][]{
                    new float[]{0.08f, 0.10f, 0.09f, 0.08f, 0.08f, 0.07f, 0.06f, 0.05f},
                    new float[]{0.11f, 0.10f, 0.09f, 0.09f, 0.11f, 0.10f, 0.09f, 0.08f},
                    new float[]{0.11f, 0.10f, 0.09f, 0.09f, 0.11f, 0.10f, 0.09f, 0.08f},
                    new float[]{0.11f, 0.10f, 0.09f, 0.09f, 0.11f, 0.10f, 0.09f, 0.08f},
                    new float[]{0.11f, 0.10f, 0.09f, 0.09f, 0.11f, 0.10f, 0.09f, 0.08f},};

        pop.getFields()[Population_Remote_MetaPopulation_COVID19.FIELDS_REMOTE_METAPOP_COVID19_HOSUEHOLD_SIZE_DIST]
                = new double[][]{
                    new double[]{0.67, 4.7, 1, 3.4},
                    new double[]{0.67, 4.7, 1, 3.4},
                    new double[]{0.67, 4.7, 1, 3.4},
                    new double[]{0.67, 4.7, 1, 3.4},
                    new double[]{0.67, 4.7, 1, 3.4},};

        // From ABS 41590DO004_2014 General Social Survey, Summary Results, Australia, 2014
        pop.getFields()[Population_Remote_MetaPopulation_COVID19.FIELDS_REMOTE_METAPOP_COVID19_NON_HOUSEHOLD_CONTACT_DIST]
                = new double[][]{
                    new double[]{0.037, 0, 0, 0.089, 1 / 90, 1 / 30, 0.248, 1 / 30, 1 / 7, 0.793, 1 / 7, 1, 1, 1, 1},
                    new double[]{0.041, 0, 0, 0.083, 1 / 90, 1 / 30, 0.245, 1 / 30, 1 / 7, 0.751, 1 / 7, 1, 1, 1, 1},
                    new double[]{0.041, 0, 0, 0.083, 1 / 90, 1 / 30, 0.245, 1 / 30, 1 / 7, 0.751, 1 / 7, 1, 1, 1, 1},
                    new double[]{0.041, 0, 0, 0.083, 1 / 90, 1 / 30, 0.245, 1 / 30, 1 / 7, 0.751, 1 / 7, 1, 1, 1, 1},
                    new double[]{0.041, 0, 0, 0.083, 1 / 90, 1 / 30, 0.245, 1 / 30, 1 / 7, 0.751, 1 / 7, 1, 1, 1, 1},};

        pop.initialise();
        pop.allolocateHosuehold();

        RelationshipMap householdMap = pop.getRelMap()[RELMAP_HOUSEHOLD];
        int[] popSize = (int[]) pop.getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_POP_SIZE];
        PrintWriter pri = new PrintWriter(System.out);

        // Household stat                 
        pop.printHousholdStat(pri);

        String key;
        double[] r0 = new double[]{1.5, 3.5};
        COVID19_Remote_Infection covid19 = new COVID19_Remote_Infection(pop.getInfectionRNG());
        key = COVID19_Remote_Infection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", "" + COVID19_Remote_Infection.DIST_RO_RAW_INDEX);
        covid19.setParameter(key, r0);
        pri.println("\"R0\" = " + Arrays.toString(r0));

        pop.setInfList(new AbstractInfection[]{covid19});
        pop.setAvailability(null);

        AbstractIndividualInterface patientZero = pop.getPop()[pop.getRNG().nextInt(pop.getPop().length)];

        // Make sure patientZero has household size > 1
        while (householdMap.degreeOf(patientZero.getId()) < 2 || ((Person_Remote_MetaPopulation) patientZero).getHomeLocation() != 0) {
            patientZero = pop.getPop()[pop.getRNG().nextInt(pop.getPop().length)];
        }

        covid19.infecting(patientZero);

        pri.println();
        pri.println("Patient zero: ");
        pop.printPatientStat(patientZero, pri);

        System.out.println();
        System.out.println("Infection stat:");

        String fileName = String.format("output_%d_%d.csv", (int) (r0[0] * 10), (int) (r0[1] * 10));

        PrintWriter csvOutput = new PrintWriter(new File("C:\\Users\\bhui\\OneDrive - UNSW\\RMP\\Covid19", fileName));

        pop.printCSVOutputHeader(csvOutput);

        for (int t = 0; t < 100; t++) {
            if (t % 1 == 0) {
                pop.printCSVOutputEntry(csvOutput);
            }
            pop.advanceTimeStep(1);
        }

        pri.close();

        csvOutput.close();

    }

    public void printCSVOutputEntry(PrintWriter csvOutput) {

        int[] popSize = (int[]) getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_POP_SIZE];
        COVID19_Remote_Infection covid19 = (COVID19_Remote_Infection) getInfList()[0];
        int t = getGlobalTime();

        int[][] numStat = new int[4][popSize.length];
        int[] num_in_loc = numStat[0];
        int[] num_infectious = numStat[1];
        int[] num_with_sym = numStat[2];
        int[] num_infected = numStat[3];

        for (AbstractIndividualInterface p : getPop()) {
            int loc = getCurrentLocation(p);
            num_in_loc[loc]++;
            if (covid19.isInfectious(p)) {
                num_infectious[loc]++;
            }
            if (covid19.isInfected(p)) {
                num_infected[loc]++;
            }
            if (covid19.hasSymptoms(p)) {
                num_with_sym[loc]++;
            }
        }
        /*
        System.out.println("t = " + t);
        System.out.println("# at location: " + Arrays.toString(num_in_loc));
        System.out.println("# infectious : " + Arrays.toString(num_infectious));
        System.out.println("# symptomatic: " + Arrays.toString(num_with_sym));
        System.out.println("# infected   : " + Arrays.toString(num_infected));
         */
        HashMap<List<Integer>, int[]> incidenceCollection = getIncidence_collection();

        double[][] r0_collection = new double[3][incidenceCollection.size()];
        int k = 0;

        for (List<Integer> incidKey : incidenceCollection.keySet()) {
            int[] infectStat = incidenceCollection.get(incidKey);
            r0_collection[0][k] = infectStat[INCIDENCE_COLLECTION_NUM_TRANMISSION_HOUSHOLD_SUC];
            r0_collection[1][k] = infectStat[INCIDENCE_COLLECTION_NUM_TRANMISSION_NON_HOUSEHOLD_SUC];
            r0_collection[2][k] = r0_collection[0][k] + r0_collection[1][k];
            k++;
        }

        DescriptiveStatistics[] r0_collection_stat = new DescriptiveStatistics[r0_collection.length];
        for (int i = 0; i < r0_collection.length; i++) {
            Arrays.sort(r0_collection[i]);
            r0_collection_stat[i] = new DescriptiveStatistics(r0_collection[i]);
        }

        /*
        System.out.println(String.format("# infection events: %d", incidenceCollection.size()));
        System.out.println(String.format("# successful tranmission per infection (household): %.2f (%.2f, %.2f)",
                r0_collection_stat[0].getMean(),
                r0_collection_stat[0].getPercentile(25),
                r0_collection_stat[0].getPercentile(75)));
        System.out.println(String.format("# successful tranmission per infection (non-household): %.2f (%.2f, %.2f)",
                r0_collection_stat[1].getMean(),
                r0_collection_stat[1].getPercentile(25),
                r0_collection_stat[1].getPercentile(75)));
        System.out.println(String.format("# successful tranmission per infection (all): %.2f (%.2f, %.2f)",
                r0_collection_stat[2].getMean(),
                r0_collection_stat[2].getPercentile(25),
                r0_collection_stat[2].getPercentile(75)));
         */
        csvOutput.print(t);
        for (int[] numArr : numStat) {
            for (int i = 0; i < num_in_loc.length; i++) {
                csvOutput.print(',');
                csvOutput.print(numArr[i]);
            }
        }

        csvOutput.print(',');
        csvOutput.print(incidenceCollection.size());

        for (DescriptiveStatistics des : r0_collection_stat) {
            csvOutput.print(
                    String.format(",%.2f,%.2f,%.2f", des.getMean(),
                            des.getPercentile(25), des.getPercentile(75)));
        }

        csvOutput.println();
    }

    public void printCSVOutputHeader(PrintWriter csvOutput) {
        int[] popSize = (int[]) getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_POP_SIZE];
        csvOutput.print("Time");
        csvOutput.print(',');
        csvOutput.print("# in loc");
        for (int i = 1; i < popSize.length; i++) {
            csvOutput.print(',');
        }
        csvOutput.print(',');
        csvOutput.print("# infectious");
        for (int i = 1; i < popSize.length; i++) {
            csvOutput.print(',');
        }
        csvOutput.print(',');
        csvOutput.print("# symptomatic");
        for (int i = 1; i < popSize.length; i++) {
            csvOutput.print(',');
        }
        csvOutput.print(',');
        csvOutput.print("# infected");
        for (int i = 1; i < popSize.length; i++) {
            csvOutput.print(',');
        }
        csvOutput.print(',');
        csvOutput.print("# infection events");

        csvOutput.print(',');
        csvOutput.print("# successful tranmission per infection (household),,");
        csvOutput.print(',');
        csvOutput.print("# successful tranmission per infection (non-household),,");
        csvOutput.print(',');
        csvOutput.print("# successful tranmission per infection (all),,");
        csvOutput.println();
    }

    public void printPatientStat(AbstractIndividualInterface patientZero, PrintWriter pri) {
        RelationshipMap householdMap = getRelMap()[RELMAP_HOUSEHOLD];
        COVID19_Remote_Infection covid19 = (COVID19_Remote_Infection) getInfList()[0];
        double[] param = covid19.getCurrentlyInfected().get(patientZero.getId());

        pri.println(String.format("Age at exposure = %.1f years old", param[COVID19_Remote_Infection.PARAM_AGE_OF_EXPOSURE] / ONE_YEAR_INT));
        pri.println(String.format("Becomes infectious after %d days since exposure.",
                (int) (param[COVID19_Remote_Infection.PARAM_INFECTIOUS_START_AGE]
                - param[COVID19_Remote_Infection.PARAM_AGE_OF_EXPOSURE])));
        pri.println(String.format("Remains infectious for %d days",
                (int) (param[COVID19_Remote_Infection.PARAM_INFECTIOUS_END_AGE]
                - param[COVID19_Remote_Infection.PARAM_INFECTIOUS_START_AGE])));
        pri.println(String.format("Symptoms appear after %d days since exposure.",
                (int) (param[COVID19_Remote_Infection.PARAM_SYMPTOM_START_AGE]
                - param[COVID19_Remote_Infection.PARAM_AGE_OF_EXPOSURE])));
        pri.println(String.format("Symptoms disspate after %d days",
                (int) (param[COVID19_Remote_Infection.PARAM_SYMPTOM_END_AGE]
                - param[COVID19_Remote_Infection.PARAM_SYMPTOM_START_AGE])));
        pri.println(String.format("Infection clear after %d days since exposure.",
                (int) (param[COVID19_Remote_Infection.PARAM_INFECTED_UNTIL_AGE]
                - param[COVID19_Remote_Infection.PARAM_AGE_OF_EXPOSURE])));

        pri.println(String.format("# within household = %d", householdMap.degreeOf(patientZero.getId())));
    }

    public RelationshipMap printHousholdStat(PrintWriter pri) {

        RelationshipMap householdMap = getRelMap()[RELMAP_HOUSEHOLD];
        int[] popSize = (int[]) getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_POP_SIZE];

        pri.println("==================");
        pri.println("Pop size : " + Arrays.toString(popSize));
        pri.println("Household : ");
        ArrayList<ArrayList<Integer>> householdSizeRec = new ArrayList<>(popSize.length);
        for (int i = 0; i < popSize.length; i++) {
            householdSizeRec.add(new ArrayList<>());
        }

        for (int loc = 0; loc < popSize.length; loc++) {

            Integer[] unqiueHouseholdAtLoc
                    = ((Integer[][]) getFields()[FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD])[loc];

            for (Integer h : unqiueHouseholdAtLoc) {
                householdSizeRec.get(loc).add(householdMap.degreeOf(h) + 1);
            }

            double[] arr = new double[householdSizeRec.get(loc).size()];
            int k = 0;
            for (Integer v : householdSizeRec.get(loc)) {
                arr[k] = v;
                k++;
            }

            Arrays.sort(arr);
            DescriptiveStatistics stat = new DescriptiveStatistics(arr);

            pri.println(String.format("For Loc #%d", loc));
            pri.println(String.format("# household = %d", stat.getN()));
            pri.println(String.format("Size of households = %.3f [%.3f, %.3f] (Min, Max = %d, %d)",
                    stat.getMean(), stat.getPercentile(25), stat.getPercentile(75),
                    (int) stat.getMin(), (int) stat.getMax()));

        }
        return householdMap;
    }

}
