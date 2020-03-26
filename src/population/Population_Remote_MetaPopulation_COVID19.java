package population;

import infection.AbstractInfection;
import infection.COVID19_Remote_Infection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import person.AbstractIndividualInterface;
import person.Person_Remote_MetaPopulation;
import relationship.RelationshipMap;
import relationship.SingleRelationship;
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

    private static final int RELMAP_HOUSEHOLD = 0;
    private static final int INF_COVID19 = 0;

    private transient HashMap<int[], int[]> incidence_collection = new HashMap<>();
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
                        SingleRelationship rel = new SingleRelationship(
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

        // Stat         
        ArrayList<ArrayList<Integer>> householdSizeRec = new ArrayList<>(popSize.length);
        for (int i = 0; i < popSize.length; i++) {
            householdSizeRec.add(new ArrayList<>());
        }

        for (int loc = 0; loc < popSize.length; loc++) {

            Integer[] unqiueHouseholdAtLoc = ((Integer[][]) getFields()[FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD])[loc];

            for (Integer h : unqiueHouseholdAtLoc) {
                householdSizeRec.get(loc).add(householdlMap.degreeOf(h) + 1);
            }

            double[] arr = new double[householdSizeRec.get(loc).size()];
            int k = 0;
            for (Integer v : householdSizeRec.get(loc)) {
                arr[k] = v;
                k++;
            }

            Arrays.sort(arr);
            DescriptiveStatistics stat = new DescriptiveStatistics(arr);

            System.out.println(String.format("For loc #%d", loc));
            System.out.println(String.format("# household = %d", stat.getN()));
            System.out.println(String.format("Size of households = %.3f [%.3f, %.3f] (Min, Max = %d, %d)",
                    stat.getMean(), stat.getPercentile(25), stat.getPercentile(75),
                    (int) stat.getMin(), (int) stat.getMax()));

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
            while (currentlyAt.size() < loc) {
                currentlyAt.add(new ArrayList<>());
            }
            currentlyAt.get(loc).add(p);

        }

        for (AbstractIndividualInterface infectious : currentlyInfectious) {

            double[] infStat = covid19.getCurrentlyInfected().get(infectious.getId());
            int ageOfExp = (int) infStat[COVID19_Remote_Infection.PARAM_AGE_OF_EXPOSURE];

            int[] key = new int[]{infectious.getId(), ageOfExp};

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
                    double pC = getRNG().nextDouble();
                    int homeLoc = ((Person_Remote_MetaPopulation) infectious).getHomeLocation();
                    double[] ncDist = ((double[][]) getFields()[FIELDS_REMOTE_METAPOP_COVID19_NON_HOUSEHOLD_CONTACT_DIST])[homeLoc];
                    int k = 0;
                    while (pC >= ncDist[k] && k + 1 < ncDist.length) {
                        k += 2;
                    }
                    nh_contactRate = ncDist[k + 1];
                    nonHouseholdContactRate.put(infectious.getId(), nh_contactRate);
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

    protected boolean infectionAttempt(AbstractInfection inf,
            AbstractIndividualInterface src, AbstractIndividualInterface target) {
        if (inf instanceof COVID19_Remote_Infection) {
            COVID19_Remote_Infection covid19 = (COVID19_Remote_Infection) inf;

            double[] param = covid19.getCurrentlyInfected().get(src.getId());

            double r0 = param[COVID19_Remote_Infection.PARAM_R0_INFECTED];

            // For now just assume uniform tranmission probabiliy per day
            double[] infectious_dur = (double[]) covid19.getParameter(AbstractInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceFirst("\\d+",
                    Integer.toString(COVID19_Remote_Infection.DIST_INFECTIOUS_DUR_INDEX)));
            double tranmissionProbPerContact = 2 * r0 / (infectious_dur[0] + infectious_dur[1]);

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
            SingleRelationship rel = new SingleRelationship(new Integer[]{newPerson.getId(), sameHouseholdId});
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

    // Debug
    public static void main(String[] arg) {

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
                    new double[]{0.67, 4.7, 0.85, 3.4, 1, 2},
                    new double[]{0.67, 4.7, 1, 3.4},
                    new double[]{0.67, 4.7, 1, 3.4},
                    new double[]{0.67, 4.7, 1, 3.4},
                    new double[]{1, 4.7},};

        // From ABS 41590DO004_2014 General Social Survey, Summary Results, Australia, 2014
        pop.getFields()[Population_Remote_MetaPopulation_COVID19.FIELDS_REMOTE_METAPOP_COVID19_NON_HOUSEHOLD_CONTACT_DIST]
                = new double[][]{
                    new double[]{0.037, 0, 0.089, 1 / 90, 0.248, 1 / 30, 0.793, 1 / 7, 1, 1},
                    new double[]{0.041, 0, 0.083, 1 / 90, 0.245, 1 / 30, 0.751, 1 / 7, 1, 1},
                    new double[]{0.041, 0, 0.083, 1 / 90, 0.245, 1 / 30, 0.751, 1 / 7, 1, 1},
                    new double[]{0.041, 0, 0.083, 1 / 90, 0.245, 1 / 30, 0.751, 1 / 7, 1, 1},
                    new double[]{0.041, 0, 0.083, 1 / 90, 0.245, 1 / 30, 0.751, 1 / 7, 1, 1},};

        pop.initialise();
        pop.allolocateHosuehold();

        String key;

        COVID19_Remote_Infection covid19 = new COVID19_Remote_Infection(pop.getInfectionRNG());
        key = COVID19_Remote_Infection.PARAM_DIST_PARAM_INDEX_REGEX.replaceAll("999", "" + COVID19_Remote_Infection.DIST_RO_RAW_INDEX);
        covid19.setParameter(key, new double[]{2.5, 3.5});
        
        pop.getFields()[Population_Remote_MetaPopulation_COVID19.FIELDS_REMOTE_METAPOP_INFECTION_LIST]
                = new AbstractInfection[]{covid19};

        covid19.infecting(pop.getPop()[pop.getRNG().nextInt(pop.getPop().length)]); // Seed

        pop.advanceTimeStep(1);

    }

}
