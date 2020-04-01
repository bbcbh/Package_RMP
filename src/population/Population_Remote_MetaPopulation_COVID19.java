package population;

import infection.AbstractInfection;
import infection.COVID19_Remote_Infection;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import person.AbstractIndividualInterface;
import person.MoveablePersonInterface;
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
    public static final int FIELDS_REMOTE_METAPOP_COVID19_SYMPTOMATIC_RESPONSE = FIELDS_REMOTE_METAPOP_COVID19_NON_HOUSEHOLD_CONTACT_DIST + 1;
    public static final int FIELDS_REMOTE_METAPOP_COVID19_TEST_RESPONSE = FIELDS_REMOTE_METAPOP_COVID19_SYMPTOMATIC_RESPONSE + 1;

    Object[] DEFAULT_FIELDS_REMOTE_METAPOP_COVID19 = {
        // FIELDS_REMOTE_METAPOP_COVID19_HOSUEHOLD_SIZE_DIST
        // Format: [loc][cumul_proproption_of_pop_1,mean_household_size_1 , cumul_percent_of_pop_2 ...
        new double[][]{},
        // FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD
        // int[loc][] list of key index (i.e. min id) of unqique household
        new Integer[][]{},
        // FIELDS_REMOTE_METAPOP_COVID19_NON_HOUSEHOLD_CONTACT_DIST
        // Format:[loc][cumul_proproption_of_contact_1, per_day_contact_rate_1 ...]
        new double[][]{},
        // FIELDS_REMOTE_METAPOP_COVID19_SYMPTOMATIC_RESPONSE
        // Format: [k] {probability}
        // k: 0 = adjustment to non-household contact, 1 = adjustment to household contact, 2 = adjustment to movement
        // probability: {cumul_prob_1, response_1, cumul_prob_2, ...}
        new double[][]{},
        // FIELDS_REMOTE_METAPOP_COVID19_TEST_RESPONSE
        // Format: [triggerIndex]{ {valid_for_days}, {probability_for_k=0}, {probability_for_k=1} ...}
        // k: 0 = adjustment to non-household contact, 1 = adjustment to household contact, 2 = adjustment to movement
        // valid_for: how long the response valid for
        // probability: {cumul_prob_1, response_1, cumul_prob_2, ...}
        new double[][]{},};

    public static final int RELMAP_HOUSEHOLD = 0;
    private static final int INF_COVID19 = 0;

    private final transient HashMap<List<Integer>, int[]> incidence_collection = new HashMap<>();
    public static final int INCIDENCE_COLLECTION_NUM_TRANMISSION_HOUSHOLD_ATTEMPT = 0;
    public static final int INCIDENCE_COLLECTION_NUM_TRANMISSION_HOUSHOLD_SUC = INCIDENCE_COLLECTION_NUM_TRANMISSION_HOUSHOLD_ATTEMPT + 1;
    public static final int INCIDENCE_COLLECTION_NUM_TRANMISSION_NON_HOUSEHOLD_ATTEMPT = INCIDENCE_COLLECTION_NUM_TRANMISSION_HOUSHOLD_SUC + 1;
    public static final int INCIDENCE_COLLECTION_NUM_TRANMISSION_NON_HOUSEHOLD_SUC = INCIDENCE_COLLECTION_NUM_TRANMISSION_NON_HOUSEHOLD_ATTEMPT + 1;
    public static final int LENGTH_INCIDENCE_COOLECTION = INCIDENCE_COLLECTION_NUM_TRANMISSION_NON_HOUSEHOLD_SUC + 1;

    private final transient HashMap<Integer, Double> nonHouseholdContactRate = new HashMap<>();

    public static final int RESPONSE_ADJ_NON_HOUSEHOLD_CONTACT = 0;
    public static final int RESPONSE_ADJ_HOUSEHOLD_CONTACT = RESPONSE_ADJ_NON_HOUSEHOLD_CONTACT + 1;
    public static final int RESPONSE_MOVEMENT = RESPONSE_ADJ_HOUSEHOLD_CONTACT + 1;

    private final transient HashMap<Integer, double[]> symptomResponse = new HashMap<>();
    public static final int SYM_RESPONSE_TOTAL = RESPONSE_MOVEMENT + 1;

    private final transient HashMap<Integer, double[]> testResponse = new HashMap<>();
    public static final int TEST_RESPONSE_VALID_UNTIL_AGE = RESPONSE_MOVEMENT + 1;
    public static final int TEST_RESPONSE_TOTAL = TEST_RESPONSE_VALID_UNTIL_AGE + 1;

    public Population_Remote_MetaPopulation_COVID19(long seed) {
        super(seed);

        Object[] orgFields = super.getFields();
        Object[] newFields = Arrays.copyOf(orgFields, orgFields.length + DEFAULT_FIELDS_REMOTE_METAPOP_COVID19.length);
        System.arraycopy(DEFAULT_FIELDS_REMOTE_METAPOP_COVID19, 0, newFields, orgFields.length, DEFAULT_FIELDS_REMOTE_METAPOP_COVID19.length);
        super.setFields(newFields);

        // To disable sexual behaviour / partner forming code
        super.setAvailability(null);
    }

    public HashMap<Integer, double[]> getTestResponse() {
        return testResponse;
    }
    
    @Override
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
    public void movePerson(AbstractIndividualInterface person, int locId, int utilAge) {

        boolean moving = true;

        if (((MoveablePersonInterface) person).getHomeLocation() != locId) {

            COVID19_Remote_Infection covid19 = (COVID19_Remote_Infection) getInfList()[INF_COVID19];

            if (covid19.hasSymptoms(person)) {
                double[] sym_resp = symptomResponse.get(person.getId());
                if (sym_resp != null) {
                    moving = sym_resp[RESPONSE_MOVEMENT] > 0;
                    if (moving && sym_resp[RESPONSE_MOVEMENT] < 1) {
                        moving = getInfectionRNG().nextDouble() < sym_resp[RESPONSE_MOVEMENT];
                    }
                }
                if (moving) {
                    double[] test_resp = testResponse.get(person.getId());
                    if (test_resp != null) {
                        if (person.getAge() < test_resp[TEST_RESPONSE_VALID_UNTIL_AGE]) {
                            moving = test_resp[RESPONSE_MOVEMENT] > 0;
                            if (moving && test_resp[RESPONSE_MOVEMENT] < 1) {
                                moving = getInfectionRNG().nextDouble() < test_resp[RESPONSE_MOVEMENT];
                            }
                        } else {
                            testResponse.remove(person.getId());
                        }
                    }
                }

            }
        }

        if (moving) {
            super.movePerson(person, locId, utilAge);
        }
    }

    @Override
    public void advanceTimeStep(int deltaT) {

        ArrayList<AbstractIndividualInterface> currentlyInfectious = new ArrayList<>();
        COVID19_Remote_Infection covid19 = (COVID19_Remote_Infection) getInfList()[INF_COVID19];
        ArrayList<ArrayList<AbstractIndividualInterface>> currentlyAt = new ArrayList<>(
                ((int[]) getFields()[FIELDS_REMOTE_METAPOP_POP_SIZE]).length);

        super.advanceTimeStep(deltaT);

        double[][] sym_infect_response = (double[][]) getFields()[FIELDS_REMOTE_METAPOP_COVID19_SYMPTOMATIC_RESPONSE];

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

            if (sym_infect_response.length > 0) {
                if (covid19.isInfected(p)) {
                    double[] infectParam = covid19.getCurrentlyInfected().get(p.getId());

                    // Symptom just appeared
                    if (infectParam[COVID19_Remote_Infection.PARAM_SYMPTOM_START_AGE] == (int) p.getAge()) {
                        double[] sym_resp = symptomResponse.get(p.getId());
                        if (sym_resp == null) {
                            sym_resp = initiialiseSymInfectionResponse();
                            symptomResponse.put(p.getId(), sym_resp);
                        }
                    }
                }
            }

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

            boolean hasNonHouseholdContact = true;
            boolean hasHouseholdContact = true;

            if (covid19.hasSymptoms(infectious) && symptomResponse.containsKey(infectious.getId())) {
                double[] sym_resp = symptomResponse.get(infectious.getId());

                hasNonHouseholdContact &= sym_resp[RESPONSE_ADJ_NON_HOUSEHOLD_CONTACT] > 0;

                if (hasNonHouseholdContact && sym_resp[RESPONSE_ADJ_NON_HOUSEHOLD_CONTACT] < 1) {
                    hasNonHouseholdContact &= getInfectionRNG().nextDouble() < sym_resp[RESPONSE_ADJ_NON_HOUSEHOLD_CONTACT];
                }

                hasHouseholdContact &= sym_resp[RESPONSE_ADJ_HOUSEHOLD_CONTACT] > 0;

                if (hasHouseholdContact && sym_resp[RESPONSE_ADJ_HOUSEHOLD_CONTACT] < 1) {
                    hasHouseholdContact &= getInfectionRNG().nextDouble() < sym_resp[RESPONSE_ADJ_HOUSEHOLD_CONTACT];
                }
            }

            if (testResponse.containsKey(infectious.getId())) {
                double[] test_resp = testResponse.get(infectious.getId());
                if (infectious.getAge() < test_resp[TEST_RESPONSE_VALID_UNTIL_AGE]) {

                    hasNonHouseholdContact &= test_resp[RESPONSE_ADJ_NON_HOUSEHOLD_CONTACT] > 0;

                    if (hasNonHouseholdContact && test_resp[RESPONSE_ADJ_NON_HOUSEHOLD_CONTACT] < 1) {
                        hasNonHouseholdContact &= getInfectionRNG().nextDouble() < test_resp[RESPONSE_ADJ_NON_HOUSEHOLD_CONTACT];
                    }

                    hasHouseholdContact &= test_resp[RESPONSE_ADJ_HOUSEHOLD_CONTACT] > 0;

                    if (hasHouseholdContact && test_resp[RESPONSE_ADJ_HOUSEHOLD_CONTACT] < 1) {
                        hasHouseholdContact &= getInfectionRNG().nextDouble() < test_resp[RESPONSE_ADJ_HOUSEHOLD_CONTACT];
                    }
                } else {
                    testResponse.remove(infectious.getId());
                }

            }

            int[] sameHousehold = inSameHouseholdAs(infectious);
            int infectiousLoc = getCurrentLocation(infectious);

            // Infecting housefold 
            if (hasHouseholdContact) {
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
            }

            // Infecting non-household
            if (hasNonHouseholdContact) {
                ArrayList<AbstractIndividualInterface> possibleNonHouseholdCandidate = new ArrayList<>();
                for (AbstractIndividualInterface candidate : currentlyAt.get(infectiousLoc)) {
                    if (Arrays.binarySearch(sameHousehold, candidate.getId()) < 0) {
                        possibleNonHouseholdCandidate.add(candidate);
                    }
                }
                if (possibleNonHouseholdCandidate.size() > 0) {
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

                    while (numContact > 0 && possibleNonHouseholdCandidate.size() > 0) {
                        int possIndex = getRNG().nextInt(possibleNonHouseholdCandidate.size());
                        AbstractIndividualInterface target = possibleNonHouseholdCandidate.get(possIndex);
                        if (covid19.couldTransmissInfection(infectious, target)) {
                            ent[INCIDENCE_COLLECTION_NUM_TRANMISSION_NON_HOUSEHOLD_ATTEMPT]++;
                            if (infectionAttempt(covid19, infectious, target)) {
                                ent[INCIDENCE_COLLECTION_NUM_TRANMISSION_NON_HOUSEHOLD_SUC]++;
                            }
                        }
                        possibleNonHouseholdCandidate.remove(possIndex);
                        numContact--;
                    }

                }
            }
        }

    }

    protected double[] initiialiseSymInfectionResponse() {
        double[] sym_resp = new double[SYM_RESPONSE_TOTAL];
        double[][] sym_infect_response = (double[][]) getFields()[FIELDS_REMOTE_METAPOP_COVID19_SYMPTOMATIC_RESPONSE];
        Arrays.fill(sym_resp, 1);

        for (int r = 0; r < sym_infect_response.length; r++) {
            if (sym_infect_response[r] != null) {
                switch (sym_infect_response[r].length) {
                    case 0:
                        // Do nothing
                        break;
                    default:
                        double pR = getInfectionRNG().nextDouble();
                        int index = 0;
                        while (index + 1 < sym_infect_response[r].length) {
                            if (pR < sym_infect_response[r][index]) {
                                sym_resp[r] = sym_infect_response[r][index + 1];
                            }
                            index += 2;
                        }
                        break;

                }
            }
        }

        return sym_resp;
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
