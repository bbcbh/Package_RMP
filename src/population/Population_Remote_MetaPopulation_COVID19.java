package population;

import infection.AbstractInfection;
import infection.COVID19_Remote_Infection;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
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

    /**
	 * 
	 */
	private static final long serialVersionUID = 2375837628195451217L;
	public static final int FIELDS_REMOTE_METAPOP_COVID19_CONTACT_OPTIONS = LENGTH_FIELDS_REMOTE_META_POP;
    public static final int FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD = FIELDS_REMOTE_METAPOP_COVID19_CONTACT_OPTIONS + 1;
    public static final int FIELDS_REMOTE_METAPOP_COVID19_CURRENTLY_IN_HOUSEHOLD = FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD + 1;
    public static final int FIELDS_REMOTE_METAPOP_COVID19_CURRENTLY_IN_QUARANTINE = FIELDS_REMOTE_METAPOP_COVID19_CURRENTLY_IN_HOUSEHOLD + 1;
    public static final int FIELDS_REMOTE_METAPOP_COVID19_META_POP_LOCKDOWN_SETTING = FIELDS_REMOTE_METAPOP_COVID19_CURRENTLY_IN_QUARANTINE + 1;

    Object[] DEFAULT_FIELDS_REMOTE_METAPOP_COVID19 = {
        // FIELDS_REMOTE_METAPOP_COVID19_CONTACT_OPTIONS
        // HashMap<person_id, {non_household_contract_rate, cumul_prob_1, household_id_1...}
        new HashMap<Integer, float[]>(),
        // FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD
        // Integer[loc]{list of household id}
        new Integer[][]{},
        // FIELDS_REMOTE_METAPOP_COVID19_CURRENTLY_IN_HOUSEHOLD
        // HashMap<person_id, until_age>
        new HashMap<Integer, Integer>(),
        // FIELDS_REMOTE_METAPOP_COVID19_CURRENTLY_IN_QUARANTINE
        // HashMap<person_id, {QUARANTINE_UNTIL_AGE,QUARANTINE_PROB_CONTACT_CORE_HOUSHOLD,... }>
        new HashMap<Integer, Number[]>(),
        // FIELDS_REMOTE_METAPOP_COVID19_META_POP_LOCKDOWN_SETTING
        // int[loc]{META_POP_INTER_LOCKDOWN_START, META_POP_INTER_LOCKDOWN_END, METAPOP_INTER_LOCKDOWN_PROPORTION,
        //          META_POP_WITHIN_LOCKDOWN_START, META_POP_WITHIN_LOCKDOWN_END, META_POP_WITHIN_LOCKDOWN_PROPORTION}
        new float[][]{},};

    public static final int RELMAP_HOUSEHOLD = 0;
    private static final int INF_COVID19 = 0;

    public static final int CONTACT_OPTIONS_NON_HOUSEHOLD_CONTACT_RATE = 0;
    public static final int CONTACT_OPTIONS_CORE_HOUSEHOLD_PROB = CONTACT_OPTIONS_NON_HOUSEHOLD_CONTACT_RATE + 1;
    public static final int CONTACT_OPTIONS_CORE_HOUSEHOLD_ID = CONTACT_OPTIONS_CORE_HOUSEHOLD_PROB + 1;

    // FIELDS_REMOTE_METAPOP_COVID19_CURRENTLY_IN_QUARANTINE
    public static final int QUARANTINE_UNTIL_AGE = 0;
    public static final int QUARANTINE_ISOLATION = QUARANTINE_UNTIL_AGE + 1;
    public static final int QUARANTINE_PROB_CONTACT_HOUSEHOLD = QUARANTINE_ISOLATION + 1;
    public static final int QUARANTINE_PROB_CONTACT_NON_HOUSEHOLD = QUARANTINE_PROB_CONTACT_HOUSEHOLD + 1;
    public static final int QUARANTINE_ENTRY_LENGTH = QUARANTINE_PROB_CONTACT_NON_HOUSEHOLD + 1;

    // Incident in real time
    private final transient HashMap<List<Integer>, int[]> incidence_collection = new HashMap<>();
    // For calculation of r0  - incidence only included if infection no longer infectious
    private final transient HashMap<List<Integer>, int[]> incidence_collenction_final = new HashMap<>();

    public static final int INCIDENCE_COLLECTION_NUM_TRANMISSION_HOUSHOLD_ATTEMPT = 0;
    public static final int INCIDENCE_COLLECTION_NUM_TRANMISSION_HOUSHOLD_SUC = INCIDENCE_COLLECTION_NUM_TRANMISSION_HOUSHOLD_ATTEMPT + 1;
    public static final int INCIDENCE_COLLECTION_NUM_TRANMISSION_NON_HOUSEHOLD_ATTEMPT = INCIDENCE_COLLECTION_NUM_TRANMISSION_HOUSHOLD_SUC + 1;
    public static final int INCIDENCE_COLLECTION_NUM_TRANMISSION_NON_HOUSEHOLD_SUC = INCIDENCE_COLLECTION_NUM_TRANMISSION_NON_HOUSEHOLD_ATTEMPT + 1;
    public static final int LENGTH_INCIDENCE_COLLECTION = INCIDENCE_COLLECTION_NUM_TRANMISSION_NON_HOUSEHOLD_SUC + 1;

    private transient int[] cumulative_incident_by_location = null;

    public static final int RESPONSE_ADJ_NON_HOUSEHOLD_CONTACT = 0;
    public static final int RESPONSE_ADJ_HOUSEHOLD_CONTACT = RESPONSE_ADJ_NON_HOUSEHOLD_CONTACT + 1;
    public static final int RESPONSE_MOVEMENT = RESPONSE_ADJ_HOUSEHOLD_CONTACT + 1;

    private final transient HashMap<Integer, double[]> symptomResponse = new HashMap<>();
    public static final int RESPONSE_SYM_SEEK_TEST = RESPONSE_MOVEMENT + 1;
    public static final int SYM_RESPONSE_TOTAL = RESPONSE_SYM_SEEK_TEST + 1;

    private final transient HashMap<Integer, double[]> positiveTestResponse = new HashMap<>();
    public static final int POS_TEST_RESPONSE_VALID_UNTIL_AGE = RESPONSE_MOVEMENT + 1;
    public static final int POS_TEST_RESPONSE_TOTAL = POS_TEST_RESPONSE_VALID_UNTIL_AGE + 1;

    private final transient HashSet<SingleRelationship> TEMP_EDGES = new HashSet<>();

    private final transient HashMap<Integer, Integer> minAgeForNextTest
            = new HashMap<>(); // Id, min age until next test

    private final transient HashMap<List<Integer>, ArrayList<Object[]>> testScheduelInPipeline
            = new HashMap<>(); // Key: Global time, loc  

    public static final int TEST_SCHEDULE_PIPELINE_ENT_PERSON_TESTED = 0;
    public static final int TEST_SCHEDULE_PIPELINE_ENT_AGE_TESTED = TEST_SCHEDULE_PIPELINE_ENT_PERSON_TESTED + 1;
    public static final int TEST_SCHEDULE_PIPELINE_ENT_TEST_TYPE = TEST_SCHEDULE_PIPELINE_ENT_AGE_TESTED + 1;
    public static final int TEST_SCHEDULE_PIPELINE_ENT_LENGTH = TEST_SCHEDULE_PIPELINE_ENT_TEST_TYPE + 1;

    private final transient HashMap<List<Integer>, ArrayList<Object[]>> testOutcomeInPipeline
            = new HashMap<>();   // Key: Global time, loc  

    public static final int TEST_OUTCOME_PIPELINE_ENT_PERSON_TESTED = 0;
    public static final int TEST_OUTCOME_PIPELINE_ENT_AGE_TESTED = TEST_OUTCOME_PIPELINE_ENT_PERSON_TESTED + 1;
    public static final int TEST_OUTCOME_PIPELINE_ENT_TEST_TYPE = TEST_OUTCOME_PIPELINE_ENT_AGE_TESTED + 1;
    public static final int TEST_OUTCOME_PIPELINE_ENT_TEST_RESULT = TEST_OUTCOME_PIPELINE_ENT_TEST_TYPE + 1;
    public static final int TEST_OUTCOME_PIPELINE_ENT_LENGTH = TEST_OUTCOME_PIPELINE_ENT_TEST_RESULT + 1;

    private final transient HashMap<List<Integer>, ArrayList<Integer[]>> quarantineInPipeline
            = new HashMap<>(); // Key: Global time, loc
    public static final int QUARANTINE_PIPELINE_ENT_PERSON_ID = 0;
    public static final int QUARANTINE_PIPELINE_ENT_IN_QUARANTINE_UNTIL = QUARANTINE_PIPELINE_ENT_PERSON_ID + 1;
    public static final int QUARANTINE_PIPELINE_ENT_CASE_ISOLATION = QUARANTINE_PIPELINE_ENT_IN_QUARANTINE_UNTIL + 1;
    public static final int QUARANTINE_PIPELINE_ENT_LENGTH = QUARANTINE_PIPELINE_ENT_CASE_ISOLATION + 1;

    private final transient HashMap<Integer, Float[]> currentlyInLockdown = new HashMap<>();
    public static final int LOCKDOWN_INTER_META_POP_UNTIL_AGE = 0;
    public static final int LOCKDOWN_WITHIN_META_POP_UNTIL_AGE = LOCKDOWN_INTER_META_POP_UNTIL_AGE + 1;
    public static final int LOCKDOWN_ENT_LENGTH = LOCKDOWN_WITHIN_META_POP_UNTIL_AGE + 1;

    private final transient HashMap<Integer, List<List<ArrayList<Integer>>>> infectionContactHistory
            = new HashMap<>(); // Key: Id
    public static final int INFECTION_CONTACT_HISTORY_WINDOW = 14;
    public static final int INFECTION_CONTACT_HISTORY_HOUSEHOLD = 0;
    public static final int INFECTION_CONTACT_HISTORY_NON_HOUSEHOLD = INFECTION_CONTACT_HISTORY_HOUSEHOLD + 1;
    public static final int INFECTION_CONTACT_HISTORY_LENGTH = INFECTION_CONTACT_HISTORY_NON_HOUSEHOLD + 1;

    private final transient HashMap<Integer, Integer[]> testResultHistory = new HashMap<>();
    public static final int TEST_RESULT_HISTORY_AGE_OF_LAST_POSITIVE = 0;
    public static final int TEST_RESULT_HISTORY_AGE_OF_LAST_NEGATIVE = TEST_RESULT_HISTORY_AGE_OF_LAST_POSITIVE + 1;
    public static final int TEST_RESULT_HISTORY_LENGTH = TEST_RESULT_HISTORY_AGE_OF_LAST_NEGATIVE + 1;

    // FIELDS_REMOTE_METAPOP_COVID19_META_POP_LOCKDOWN_SETTING
    public static final int META_POP_INTER_LOCKDOWN_START = 0;
    public static final int META_POP_INTER_LOCKDOWN_END = META_POP_INTER_LOCKDOWN_START + 1;
    public static final int META_POP_INTER_LOCKDOWN_PROPORTION = META_POP_INTER_LOCKDOWN_END + 1;
    public static final int META_POP_WITHIN_LOCKDOWN_START = META_POP_INTER_LOCKDOWN_PROPORTION + 1;
    public static final int META_POP_WITHIN_LOCKDOWN_END = META_POP_WITHIN_LOCKDOWN_START + 1;
    public static final int META_POP_WITHIN_LOCKDOWN_PROPORTION = META_POP_WITHIN_LOCKDOWN_END + 1;
    public static final int META_POP_STAT_META_POP_LENGTH = META_POP_WITHIN_LOCKDOWN_PROPORTION + 1;

    // generateInfectionStat()
    public static final int NUM_STAT_NUM_IN_LOC = 0;
    public static final int NUM_STAT_NUM_INFECTIOUS = NUM_STAT_NUM_IN_LOC + 1;
    public static final int NUM_STAT_NUM_WITH_SYM = NUM_STAT_NUM_INFECTIOUS + 1;
    public static final int NUM_STAT_NUM_INFECTED = NUM_STAT_NUM_WITH_SYM + 1;
    public static final int NUM_STAT_NUM_LENGTH = NUM_STAT_NUM_INFECTED + 1;

    // inSameHousehold
    public static final int HOUSEHOLD_TYPE_CURRENT = 0;
    public static final int HOUSEHOLD_TYPE_CORE = HOUSEHOLD_TYPE_CURRENT + 1;
    public static final int HOUSEHOLD_TYPE_NON_CORE = HOUSEHOLD_TYPE_CORE + 1;
    public static final int HOUSEHOLD_TYPE_TEMP = HOUSEHOLD_TYPE_NON_CORE + 1;
    public static final int HOUSEHOLD_TYPE_LENGTH = HOUSEHOLD_TYPE_TEMP + 1;

    private float[][] DEFAULT_AWAY_FROM_HOME_PROB = null;

    public Population_Remote_MetaPopulation_COVID19(long seed) {
        super(seed);

        Object[] orgFields = super.getFields();
        Object[] newFields = Arrays.copyOf(orgFields, orgFields.length + DEFAULT_FIELDS_REMOTE_METAPOP_COVID19.length);
        System.arraycopy(DEFAULT_FIELDS_REMOTE_METAPOP_COVID19, 0, newFields, orgFields.length, DEFAULT_FIELDS_REMOTE_METAPOP_COVID19.length);
        super.setFields(newFields);

        // To disable sexual behaviour / partner forming code
        super.setAvailability(null);
    }

    public HashMap<Integer, List<List<ArrayList<Integer>>>> getInfectionContactHistory() {
        return infectionContactHistory;
    }

    public HashMap<Integer, Integer[]> getTestResultHistory() {
        return testResultHistory;
    }

    public HashMap<List<Integer>, ArrayList<Object[]>> getTestOutcomeInPipeline() {
        return testOutcomeInPipeline;
    }

    public HashMap<List<Integer>, ArrayList<Object[]>> getTestScheduelInPipeline() {
        return testScheduelInPipeline;
    }

    public HashMap<List<Integer>, ArrayList<Integer[]>> getQuarantineInPipeline() {
        return quarantineInPipeline;
    }

    public HashMap<Integer, double[]> getPositiveTestResponse() {
        return positiveTestResponse;
    }

    public HashMap<Integer, double[]> getSymptomResponse() {
        return symptomResponse;
    }

    @Override
    protected int modelMaxAge() {
        return 80 * AbstractIndividualInterface.ONE_YEAR_INT;
    }

    public HashMap<Integer, Integer> getMinAgeForNextTest() {
        return minAgeForNextTest;
    }

    @SuppressWarnings("unchecked")
	private Integer[] currentlyInSameHouseholdAs(AbstractIndividualInterface p) {

        RelationshipMap housholdMap = getRelMap()[RELMAP_HOUSEHOLD];
        HashMap<Integer, Integer> currentlyAtHouseholdMap
                = (HashMap<Integer, Integer>) getFields()[FIELDS_REMOTE_METAPOP_COVID19_CURRENTLY_IN_HOUSEHOLD];

        assignCurrentlyAtHousehold(p);

        if (housholdMap.containsVertex(p.getId())) {

            Integer householdId = currentlyAtHouseholdMap.get(p.getId());

            if (householdId == null) {
                // Use core 
                householdId = (int) ((HashMap<Integer, float[]>) getFields()[FIELDS_REMOTE_METAPOP_COVID19_CONTACT_OPTIONS]).get(p.getId())[CONTACT_OPTIONS_CORE_HOUSEHOLD_ID];

            }

            LinkedList<Integer> resList = new LinkedList<>();
            resList.add(p.getId());

            for (SingleRelationship e : housholdMap.edgesOf(householdId)) {
                int candidateId = e.getLinksValues()[0] == householdId
                        ? e.getLinksValues()[1] : e.getLinksValues()[0];

                assignCurrentlyAtHousehold(getLocalData().get(candidateId));

                if (householdId.equals(currentlyAtHouseholdMap.get(candidateId))) {
                    resList.add(candidateId);
                }

            }

            return resList.toArray(new Integer[resList.size()]);
        } else {
            return new Integer[]{p.getId()};
        }
    }

    @SuppressWarnings("unchecked")
	public AbstractIndividualInterface[][] currentlyInSameHouseholdAsByType(AbstractIndividualInterface p) {
        AbstractIndividualInterface[][] res = new AbstractIndividualInterface[HOUSEHOLD_TYPE_LENGTH][];

        LinkedList<AbstractIndividualInterface> current = new LinkedList<>();
        LinkedList<AbstractIndividualInterface> core = new LinkedList<>();
        LinkedList<AbstractIndividualInterface> non_core = new LinkedList<>();
        LinkedList<AbstractIndividualInterface> temp = new LinkedList<>();

        RelationshipMap householdMap = getRelMap()[RELMAP_HOUSEHOLD];
        HashMap<Integer, Integer> currentlyAtHouseholdMap
                = (HashMap<Integer, Integer>) getFields()[FIELDS_REMOTE_METAPOP_COVID19_CURRENTLY_IN_HOUSEHOLD];
        HashMap<Integer, float[]> contactOption = (HashMap<Integer, float[]>) getFields()[FIELDS_REMOTE_METAPOP_COVID19_CONTACT_OPTIONS];

        assignCurrentlyAtHousehold(p);

        if (householdMap.containsVertex(p.getId())) {
            Set<SingleRelationship> houseIndexSet = householdMap.edgesOf(p.getId());
            for (SingleRelationship houseRel : houseIndexSet) {
                int houseIndex = houseRel.getLinksValues()[0] == p.getId()
                        ? houseRel.getLinksValues()[1] : houseRel.getLinksValues()[0];

                Set<SingleRelationship> memberSet = householdMap.edgesOf(houseIndex);
                for (SingleRelationship memRel : memberSet) {
                    int memberId = memRel.getLinksValues()[0] == houseIndex
                            ? memRel.getLinksValues()[1] : memRel.getLinksValues()[0];

                    if (memberId != p.getId()) {
                        AbstractIndividualInterface member = getLocalData().get(memberId);
                        assignCurrentlyAtHousehold(member);
                        if (houseIndex == currentlyAtHouseholdMap.get(p.getId())) {
                            if (houseIndex == currentlyAtHouseholdMap.get(memberId)) {
                                current.add(member);
                            } else {
                                if (houseIndex == contactOption.get(memberId)[CONTACT_OPTIONS_CORE_HOUSEHOLD_ID]) {
                                    core.add(member);
                                } else {
                                    if (TEMP_EDGES.contains(houseRel)) {
                                        temp.add(member);
                                    } else {
                                        non_core.add(member);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        res[HOUSEHOLD_TYPE_CURRENT] = current.toArray(new AbstractIndividualInterface[current.size()]);
        res[HOUSEHOLD_TYPE_CORE] = core.toArray(new AbstractIndividualInterface[core.size()]);
        res[HOUSEHOLD_TYPE_NON_CORE] = non_core.toArray(new AbstractIndividualInterface[non_core.size()]);
        res[HOUSEHOLD_TYPE_TEMP] = temp.toArray(new AbstractIndividualInterface[temp.size()]);

        return res;

    }

    public void allolocateCoreHosuehold(float[][] householdSizeDistByLoc,
            float[][] householdSpreadByLoc,
            float[][] nonHouseholdContactRateByLoc) {

        int[] popSize = (int[]) getFields()[FIELDS_REMOTE_METAPOP_POP_SIZE];
        RelationshipMap householdMap = getRelMap()[RELMAP_HOUSEHOLD];

        getFields()[FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD] = new Integer[popSize.length][];
        AbstractIndividualInterface[][] homeLocMapping = new AbstractIndividualInterface[popSize.length][];
        int[] mappingIndex = new int[popSize.length];

        PoissonDistribution poi;
        double meanHouseHoldSize;

        int householdId = -1;

        for (int p = 0; p < popSize.length; p++) {
            homeLocMapping[p] = new Person_Remote_MetaPopulation[popSize[p]];
        }

        for (AbstractIndividualInterface person : getPop()) {
            int homeLoc = ((Person_Remote_MetaPopulation) person).getHomeLocation();

            int index = mappingIndex[homeLoc];
            homeLocMapping[homeLoc][index] = person;
            mappingIndex[homeLoc]++;
        }

        // Setting core home location and non-home contact rate
        for (int loc = 0; loc < homeLocMapping.length; loc++) {
            LinkedList<Integer> householdAtLoc = new LinkedList<>();
            AbstractIndividualInterface[] resident = homeLocMapping[loc];
            ArrayUtilsRandomGenerator.shuffleArray(resident, getRNG());

            float[] householdSizeDist = householdSizeDistByLoc[loc];
            float[] householdSpreadDist = householdSpreadByLoc[loc];
            float[] nonHouseholdContactRateDist = nonHouseholdContactRateByLoc[loc];

            int housePt = 0;

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

                int numInHouse = poi.sample();

                int firstInHouse = rPt;
                householdMap.addVertex(resident[firstInHouse].getId());
                householdMap.addVertex(householdId);

                setContactOptionCore(resident[firstInHouse], householdId, householdSpreadDist,
                        nonHouseholdContactRateDist);

                while ((rPt + 1) < resident.length && numInHouse > 1) {
                    rPt++;
                    if (!householdMap.containsVertex(resident[rPt].getId())) {
                        householdMap.addVertex(resident[rPt].getId());
                    }
                    setContactOptionCore(resident[rPt], householdId, householdSpreadDist,
                            nonHouseholdContactRateDist);

                    numInHouse--;
                }

                householdAtLoc.add(householdId);
                householdId--;
                rPt++;
            }
            ((Integer[][]) getFields()[FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD])[loc]
                    = householdAtLoc.toArray(new Integer[householdAtLoc.size()]);

        }
    }

    public void allocateNonCoreHousehold() {
        // Setting non-core household options
        RelationshipMap householdMap = getRelMap()[RELMAP_HOUSEHOLD];
        @SuppressWarnings("unchecked")
		HashMap<Integer, float[]> contactOptions
                = (HashMap<Integer, float[]>) getFields()[FIELDS_REMOTE_METAPOP_COVID19_CONTACT_OPTIONS];

        for (Integer r : contactOptions.keySet()) {
            float[] contact_option_ent = contactOptions.get(r);
            int homeLoc = ((MoveablePersonInterface) getLocalData().get(r)).getHomeLocation();
            Integer[] householdCandidate = ((Integer[][]) getFields()[FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD])[homeLoc];

            householdCandidate = Arrays.copyOf(householdCandidate, householdCandidate.length);
            int vaildOption = householdCandidate.length;
            int nonCoreHomeIndex = CONTACT_OPTIONS_CORE_HOUSEHOLD_ID + 2; // nh_contract_rate, p_core, home_id_core, p_non_core...
            while (nonCoreHomeIndex < contact_option_ent.length && vaildOption > 0) {
                int testIndex = getRNG().nextInt(vaildOption);
                int possibleHousehold = householdCandidate[testIndex];

                // Test if it is core home location already
                if (possibleHousehold == (int) contact_option_ent[CONTACT_OPTIONS_CORE_HOUSEHOLD_ID]) {
                    vaildOption--;
                    householdCandidate[testIndex] = householdCandidate[vaildOption];
                    householdCandidate[vaildOption] = (int) contact_option_ent[CONTACT_OPTIONS_CORE_HOUSEHOLD_ID];

                    if (vaildOption > 0) {
                        testIndex = getRNG().nextInt(vaildOption);
                        possibleHousehold = householdCandidate[testIndex];
                    } else {
                        possibleHousehold = (int) contact_option_ent[CONTACT_OPTIONS_CORE_HOUSEHOLD_ID];
                    }
                }

                if (possibleHousehold != (int) contact_option_ent[CONTACT_OPTIONS_CORE_HOUSEHOLD_ID]) {
                    contact_option_ent[nonCoreHomeIndex] = possibleHousehold;

                    SingleRelationship rel = new SingleRelationshipTimeStamp(
                            new Integer[]{possibleHousehold, r});
                    householdMap.addEdge(possibleHousehold, r, rel);
                    rel.setDurations(Double.POSITIVE_INFINITY); // Never expires

                }
                vaildOption--;
                householdCandidate[testIndex] = householdCandidate[vaildOption];
                householdCandidate[vaildOption] = possibleHousehold;
                nonCoreHomeIndex += 2;
            }

        }
    }

    protected void setContactOptionCore(AbstractIndividualInterface residentAdded, int householdId,
            float[] householdSpreadDist, float[] nonHouseholdContactRateDist) {

        SingleRelationship rel;
        float[] contact_option_ent;

        RelationshipMap householdMap = getRelMap()[RELMAP_HOUSEHOLD];
        @SuppressWarnings("unchecked")
		HashMap<Integer, float[]> contactOptions
                = (HashMap<Integer, float[]>) getFields()[FIELDS_REMOTE_METAPOP_COVID19_CONTACT_OPTIONS];

        rel = new SingleRelationshipTimeStamp(
                new Integer[]{householdId, residentAdded.getId()});
        householdMap.addEdge(householdId, residentAdded.getId(), rel);
        rel.setDurations(Double.POSITIVE_INFINITY); // Never expires

        contact_option_ent = contactOptions.get(residentAdded.getId());

        if (contact_option_ent == null) {
            contact_option_ent = new float[1 + householdSpreadDist.length * 2];
            contact_option_ent[CONTACT_OPTIONS_NON_HOUSEHOLD_CONTACT_RATE]
                    = generateNonHouseholdContactRate(residentAdded, nonHouseholdContactRateDist);
            int index = CONTACT_OPTIONS_CORE_HOUSEHOLD_PROB;
            float cumul_prob = 0;
            for (int i = 0; i < householdSpreadDist.length; i++) {
                contact_option_ent[index] = cumul_prob + householdSpreadDist[i];
                cumul_prob += householdSpreadDist[i];
                index++;
                if (index == CONTACT_OPTIONS_CORE_HOUSEHOLD_ID) {
                    contact_option_ent[index] = householdId;
                } else {
                    contact_option_ent[index] = Float.NaN;
                }
                index++;
            }
            contactOptions.put(residentAdded.getId(), contact_option_ent);
        }
        
        contact_option_ent[CONTACT_OPTIONS_CORE_HOUSEHOLD_ID] = householdId;
    }

    @Override
    public void movePerson(AbstractIndividualInterface person, int locId, int utilAge) {
        boolean moving = true;

        if (((MoveablePersonInterface) person).getHomeLocation() != locId) {

            Float[] lockdownUntilArr = inLockdownUntil(person);
            moving = inQuarantineUntil(person) != null;
            moving &= lockdownUntilArr == null || Float.isNaN(lockdownUntilArr[LOCKDOWN_INTER_META_POP_UNTIL_AGE])
                    || person.getAge() >= lockdownUntilArr[LOCKDOWN_INTER_META_POP_UNTIL_AGE];

            if (moving) {

                COVID19_Remote_Infection covid19 = (COVID19_Remote_Infection) getInfList()[INF_COVID19];

                if (covid19.hasSymptoms(person)) {
                    double[] sym_resp = symptomResponse.get(person.getId());
                    if (sym_resp != null) {
                        moving = sym_resp[RESPONSE_MOVEMENT] > 0;
                        if (moving && sym_resp[RESPONSE_MOVEMENT] < 1) {
                            moving = getInfectionRNG().nextDouble() < sym_resp[RESPONSE_MOVEMENT];
                        }
                    }
                }
                if (moving) {
                    double[] test_resp = positiveTestResponse.get(person.getId());
                    if (test_resp != null) {
                        if (person.getAge() < test_resp[POS_TEST_RESPONSE_VALID_UNTIL_AGE]) {
                            moving = test_resp[RESPONSE_MOVEMENT] > 0;
                            if (moving && test_resp[RESPONSE_MOVEMENT] < 1) {
                                moving = getInfectionRNG().nextDouble() < test_resp[RESPONSE_MOVEMENT];
                            }
                        } else {
                            positiveTestResponse.remove(person.getId());
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

        ArrayList<AbstractIndividualInterface> currentlyInfected = new ArrayList<>();
        COVID19_Remote_Infection covid19 = (COVID19_Remote_Infection) getInfList()[INF_COVID19];
        final int numOfPop = ((int[]) getFields()[FIELDS_REMOTE_METAPOP_POP_SIZE]).length;
        ArrayList<ArrayList<AbstractIndividualInterface>> currentlyAt = new ArrayList<>(numOfPop);

        @SuppressWarnings("unchecked")
		HashMap<Integer, Integer> currentlyInHousehold
                = (HashMap<Integer, Integer>) getFields()[FIELDS_REMOTE_METAPOP_COVID19_CURRENTLY_IN_HOUSEHOLD];

        RelationshipMap householdMap = getRelMap()[RELMAP_HOUSEHOLD];

        // Clear previous household record
        currentlyInHousehold.clear();

        // Remove temp edges from previous turn
        for (SingleRelationship rel : TEMP_EDGES) {
            householdMap.removeEdge(rel);
        }
        TEMP_EDGES.clear();

        // Initalise incident collection (if haven't do so already
        if (cumulative_incident_by_location == null) {
            cumulative_incident_by_location = new int[numOfPop];
        }

        super.advanceTimeStep(deltaT);

        for (AbstractIndividualInterface p : getPop()) {

            int loc = getCurrentLocation(p);
            while (currentlyAt.size() <= loc) {
                currentlyAt.add(new ArrayList<>());
            }
            currentlyAt.get(loc).add(p);

            if (covid19.isInfected(p)) {

                double[] infStat = covid19.getCurrentlyInfected().get(p.getId());
                currentlyInfected.add(p);

                if (p.getAge() == infStat[COVID19_Remote_Infection.PARAM_INFECTIOUS_END_AGE]) {
                    // Just stop being infections 
                    List<Integer> key = List.of(p.getId(), (int) infStat[COVID19_Remote_Infection.PARAM_AGE_OF_EXPOSURE]);
                    int[] ent = incidence_collection.remove(key);
                    if (ent != null) {
                        incidence_collenction_final.put(key, ent);
                    }

                }
                //assignCurrentlyAtHousehold(p);
            }

        }
        // Transmission
        for (AbstractIndividualInterface infected : currentlyInfected) {

            double[] infStat = covid19.getCurrentlyInfected().get(infected.getId());
            int ageOfExp = (int) infStat[COVID19_Remote_Infection.PARAM_AGE_OF_EXPOSURE];

            List<Integer> key = List.of(infected.getId(), ageOfExp);
            int[] ent = incidence_collection.get(key);

            if (ent == null) {
                incidence_collection.put(key, new int[LENGTH_INCIDENCE_COLLECTION]);
                ent = incidence_collection.get(key);
            }

            assignCurrentlyAtHousehold(infected);

            List<List<ArrayList<Integer>>> contactHistoryById = infectionContactHistory.get(infected.getId());

            if (contactHistoryById == null) {
                contactHistoryById = List.of(
                        new ArrayList<>(INFECTION_CONTACT_HISTORY_WINDOW),
                        new ArrayList<>(INFECTION_CONTACT_HISTORY_WINDOW)
                );
                for (int householdType = 0; householdType < contactHistoryById.size(); householdType++) {
                    for (int d = 0; d < INFECTION_CONTACT_HISTORY_WINDOW; d++) {
                        contactHistoryById.get(householdType).add(new ArrayList<>());
                    }
                }
                infectionContactHistory.put(infected.getId(), contactHistoryById);

            }

            int contactDayIndex = getGlobalTime() % INFECTION_CONTACT_HISTORY_WINDOW;
            ArrayList<Integer> contactHistoryHousehold = contactHistoryById.get(INFECTION_CONTACT_HISTORY_HOUSEHOLD).get(contactDayIndex);
            ArrayList<Integer> contactHistoryNonHousehold = contactHistoryById.get(INFECTION_CONTACT_HISTORY_NON_HOUSEHOLD).get(contactDayIndex);

            contactHistoryHousehold.clear();
            contactHistoryNonHousehold.clear();

            boolean hasHouseholdContact = allowContact(covid19, infected, RESPONSE_ADJ_HOUSEHOLD_CONTACT);
            boolean hasNonHouseholdContact = allowContact(covid19, infected, RESPONSE_ADJ_NON_HOUSEHOLD_CONTACT);

            Integer[] sameHousehold = currentlyInSameHouseholdAs(infected);
            int infectiousLoc = getCurrentLocation(infected);

            // Infecting housefold 
            if (hasHouseholdContact) {
                for (int pid : sameHousehold) {
                    if (pid != infected.getId()) {
                        AbstractIndividualInterface target = getLocalData().get(pid);
                        if (infectiousLoc == getCurrentLocation(target)
                                && allowContact(covid19, target, RESPONSE_ADJ_HOUSEHOLD_CONTACT)) {

                            contactHistoryHousehold.add(target.getId());

                            if (covid19.couldTransmissInfection(infected, target)) {
                                ent[INCIDENCE_COLLECTION_NUM_TRANMISSION_HOUSHOLD_ATTEMPT]++;

                                if (infectionAttempt(covid19, infected, target)) {
                                    ent[INCIDENCE_COLLECTION_NUM_TRANMISSION_HOUSHOLD_SUC]++;

                                    cumulative_incident_by_location[infectiousLoc]++;
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
                    if (Arrays.binarySearch(sameHousehold, candidate.getId()) < 0
                            && allowContact(covid19, candidate, RESPONSE_ADJ_NON_HOUSEHOLD_CONTACT)) {
                        possibleNonHouseholdCandidate.add(candidate);
                    }
                }
                if (possibleNonHouseholdCandidate.size() > 0) {

                    @SuppressWarnings("unchecked")
					Float nh_contactRate = ((HashMap<Integer, float[]>) getFields()[FIELDS_REMOTE_METAPOP_COVID19_CONTACT_OPTIONS]).get(infected.getId())[CONTACT_OPTIONS_NON_HOUSEHOLD_CONTACT_RATE];

                    int numContact = nh_contactRate.intValue();

                    if (nh_contactRate < 1 && nh_contactRate > 0) {
                        if (getRNG().nextFloat() < nh_contactRate) {
                            numContact++;
                        }
                    }

                    while (numContact > 0 && possibleNonHouseholdCandidate.size() > 0) {
                        int possIndex = getRNG().nextInt(possibleNonHouseholdCandidate.size());
                        AbstractIndividualInterface target = possibleNonHouseholdCandidate.get(possIndex);
                        contactHistoryNonHousehold.add(target.getId());
                        if (covid19.couldTransmissInfection(infected, target)) {
                            ent[INCIDENCE_COLLECTION_NUM_TRANMISSION_NON_HOUSEHOLD_ATTEMPT]++;

                            if (infectionAttempt(covid19, infected, target)) {
                                ent[INCIDENCE_COLLECTION_NUM_TRANMISSION_NON_HOUSEHOLD_SUC]++;

                                cumulative_incident_by_location[infectiousLoc]++;
                            }
                        }
                        possibleNonHouseholdCandidate.remove(possIndex);
                        numContact--;
                    }

                }
            }
        }

    }

    protected void assignCurrentlyAtHousehold(AbstractIndividualInterface p) {

        @SuppressWarnings("unchecked")
		HashMap<Integer, Integer> currentlyInHousehold
                = (HashMap<Integer, Integer>) getFields()[FIELDS_REMOTE_METAPOP_COVID19_CURRENTLY_IN_HOUSEHOLD];

        HashSet<SingleRelationship> tempEdges = TEMP_EDGES;

        if (!currentlyInHousehold.containsKey(p.getId())) {

            Integer inQuarantine = inQuarantineUntil(p);

            boolean forcedInCoreHousehold = inQuarantine != null;

            if (!forcedInCoreHousehold) {
                Float[] inLockdownUntil = inLockdownUntil(p);
                if (inLockdownUntil != null) {
                    forcedInCoreHousehold |= (!Float.isNaN(inLockdownUntil[LOCKDOWN_WITHIN_META_POP_UNTIL_AGE])
                            && p.getAge() < inLockdownUntil[LOCKDOWN_WITHIN_META_POP_UNTIL_AGE]);
                }
            }

            if (forcedInCoreHousehold) {
                movePerson(p, ((Person_Remote_MetaPopulation) p).getHomeLocation(), -1);

                @SuppressWarnings("unchecked")
				float[] contactOption = ((HashMap<Integer, float[]>) getFields()[FIELDS_REMOTE_METAPOP_COVID19_CONTACT_OPTIONS]).get(p.getId());

                currentlyInHousehold.put(p.getId(),
                        (int) contactOption[CONTACT_OPTIONS_CORE_HOUSEHOLD_ID]);

            } else {
                int loc = getCurrentLocation(p);
                if (loc == -1) {
                    loc = ((MoveablePersonInterface) p).getHomeLocation();
                }

                RelationshipMap householdMap = getRelMap()[RELMAP_HOUSEHOLD];
                @SuppressWarnings("unchecked")
				HashMap<Integer, float[]> contactOptions
                        = (HashMap<Integer, float[]>) getFields()[FIELDS_REMOTE_METAPOP_COVID19_CONTACT_OPTIONS];

                boolean requireTempEdge = true;

                if (loc == ((MoveablePersonInterface) p).getHomeLocation()) {
                    float[] contactOptionsEnt = contactOptions.get(p.getId());
                    float pHouse = getRNG().nextFloat();
                    int pHouseIndex = CONTACT_OPTIONS_CORE_HOUSEHOLD_PROB;
                    while (pHouseIndex + 1 < contactOptionsEnt.length
                            && pHouse >= contactOptionsEnt[pHouseIndex]) {
                        pHouseIndex += 2;
                    }
                    if (pHouseIndex + 1 < contactOptionsEnt.length) {
                        currentlyInHousehold.put(p.getId(), (int) contactOptionsEnt[pHouseIndex + 1]);
                        requireTempEdge = false;
                    } else {
                        // Sproadic
                        requireTempEdge = true;
                    }
                }
                if (requireTempEdge && tempEdges != null) {
                    Integer[] household_at_loc = ((Integer[][]) getFields()[FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD])[loc];
                    household_at_loc = Arrays.copyOf(household_at_loc, household_at_loc.length);

                    int validOption = household_at_loc.length;

                    int possibleHouseholdIndex = getRNG().nextInt(validOption);
                    int possibleHousehold = household_at_loc[possibleHouseholdIndex];

                    while (validOption > 0 && householdMap.containsEdge(possibleHousehold, p.getId())) {
                        validOption--;
                        household_at_loc[possibleHouseholdIndex] = household_at_loc[validOption];
                        household_at_loc[validOption] = possibleHousehold;

                        possibleHouseholdIndex = getRNG().nextInt(validOption);
                        possibleHousehold = household_at_loc[possibleHouseholdIndex];
                    }

                    SingleRelationship tempEdge = new SingleRelationshipTimeStamp(
                            new Integer[]{possibleHousehold, p.getId()});

                    if (!householdMap.containsVertex(p.getId())) {
                        householdMap.addVertex(p.getId());
                    }
                    householdMap.addEdge(possibleHousehold, p.getId(), tempEdge);

                    currentlyInHousehold.put(p.getId(), possibleHousehold);

                    tempEdges.add(tempEdge);

                }
            }
        }
    }

    public Integer inQuarantineUntil(AbstractIndividualInterface p) {
        Number[] res = inQuarantine(p);
        return res == null ? null : (Integer) res[QUARANTINE_UNTIL_AGE];

    }

    public Number[] inQuarantine(AbstractIndividualInterface p) {
        @SuppressWarnings("unchecked")
		HashMap<Integer, Number[]> quarantineMap
                = (HashMap<Integer, Number[]>) getFields()[FIELDS_REMOTE_METAPOP_COVID19_CURRENTLY_IN_QUARANTINE];
        Number[] quarantineEnt = quarantineMap.get(p.getId());

        if (quarantineEnt != null) {

            if (p.getAge() >= (Integer) quarantineEnt[QUARANTINE_UNTIL_AGE]) {
                quarantineMap.remove(p.getId());
                quarantineEnt = null;
            } else {
                if (getCurrentLocation(p) != ((Person_Remote_MetaPopulation) p).getHomeLocation()) {
                    movePerson(p, ((Person_Remote_MetaPopulation) p).getHomeLocation(), -1);
                }

            }
        }

        return quarantineEnt;
    }

    protected boolean allowContact(COVID19_Remote_Infection covid19,
            AbstractIndividualInterface infectious, int contactType) {

        Number[] quarantineEnt = inQuarantine(infectious);

        boolean hasContact = true;

        if (quarantineEnt != null) {
            hasContact = false;
            int effectIndex = contactType == RESPONSE_ADJ_HOUSEHOLD_CONTACT
                    ? Population_Remote_MetaPopulation_COVID19.QUARANTINE_PROB_CONTACT_HOUSEHOLD
                    : Population_Remote_MetaPopulation_COVID19.QUARANTINE_PROB_CONTACT_NON_HOUSEHOLD;

            if (effectIndex < quarantineEnt.length) {
                float probContact = quarantineEnt[effectIndex].floatValue();
                hasContact = probContact > 0;
                if (probContact < 1) {
                    hasContact = getRNG().nextFloat() < probContact;
                }

            }
        }

        // Check for lockdown
        if (contactType == RESPONSE_ADJ_NON_HOUSEHOLD_CONTACT) {
            Float[] inLockdown = inLockdownUntil(infectious);
            hasContact &= inLockdown == null
                    || Float.isNaN(inLockdown[LOCKDOWN_WITHIN_META_POP_UNTIL_AGE])
                    || infectious.getAge() >= inLockdown[LOCKDOWN_WITHIN_META_POP_UNTIL_AGE];

        }

        if (hasContact) {

            if (covid19.hasSymptoms(infectious) && symptomResponse.containsKey(infectious.getId())) {
                double[] sym_resp = symptomResponse.get(infectious.getId());

                hasContact &= sym_resp[contactType] > 0;

                if (hasContact && sym_resp[contactType] < 1) {
                    hasContact &= getInfectionRNG().nextDouble() < sym_resp[contactType];
                }

            }
            if (positiveTestResponse.containsKey(infectious.getId())) {
                double[] test_resp = positiveTestResponse.get(infectious.getId());
                if (infectious.getAge() < test_resp[POS_TEST_RESPONSE_VALID_UNTIL_AGE]) {

                    hasContact &= test_resp[contactType] > 0;

                    if (hasContact && test_resp[contactType] < 1) {
                        hasContact &= getInfectionRNG().nextDouble() < test_resp[contactType];
                    }

                } else {
                    positiveTestResponse.remove(infectious.getId());
                }

            }
        }

        return hasContact;
    }

    public double[] initiialiseSymInfectionResponse(double[][] sym_infect_response) {
        double[] sym_resp = new double[SYM_RESPONSE_TOTAL];
        Arrays.fill(sym_resp, 1);

        for (int r = 0; r < sym_infect_response.length; r++) {
            if (sym_infect_response[r] != null) {
                switch (sym_infect_response[r].length) {
                    case 0:
                        // Do nothing
                        break;
                    default:
                        int index = 0;
                        sym_resp[r] = sym_infect_response[r][index + 1];
                        if (sym_infect_response[r].length > 1) {
                            double pR = getInfectionRNG().nextDouble();
                            while (index + 1 < sym_infect_response[r].length) {
                                if (pR < sym_infect_response[r][index]) {
                                    sym_resp[r] = sym_infect_response[r][index + 1];
                                }
                                index += 2;
                            }
                        }
                        break;

                }
            }
        }

        return sym_resp;
    }

    protected Float generateNonHouseholdContactRate(AbstractIndividualInterface infectious, float[] ncDist) {
        float nh_contactRate;
        double pC = getRNG().nextFloat();

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

        return nh_contactRate;
    }

    protected boolean infectionAttempt(AbstractInfection inf,
            AbstractIndividualInterface src, AbstractIndividualInterface target) {
        if (inf instanceof COVID19_Remote_Infection) {
            COVID19_Remote_Infection covid19 = (COVID19_Remote_Infection) inf;

            double[] param = covid19.getCurrentlyInfected().get(src.getId());

            double r0 = param[COVID19_Remote_Infection.PARAM_R0_INFECTED];

            double tranmissionProbPerContact;
            if (r0 < 0) {
                tranmissionProbPerContact = -r0;
            } else {

                // For now just assume uniform tranmission probabiliy per day
                double[] infectious_dur = (double[]) covid19.getParameter(AbstractInfection.PARAM_DIST_PARAM_INDEX_REGEX.replaceFirst("\\d+",
                        Integer.toString(COVID19_Remote_Infection.DIST_INFECTIOUS_DUR_INDEX)));
                tranmissionProbPerContact = r0 / infectious_dur[0];
            }

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

        removePersonFromPopulation(removedPerson);

        Person_Remote_MetaPopulation newPerson = new Person_Remote_MetaPopulation(nextId,
                removedPerson.isMale(),
                getGlobalTime(),
                0,
                0,
                getInfList().length,
                (int) removedPerson.getFields()[Person_Remote_MetaPopulation.PERSON_HOME_LOC]);

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

        if (!connected.isEmpty()) {
            householdMap.removeVertex(removedPerson.getId(), false);
            householdMap.addVertex(newPerson.getId());

            for (Integer householdId : connected.toArray(new Integer[connected.size()])) {
                SingleRelationship rel = new SingleRelationshipTimeStamp(new Integer[]{householdId, newPerson.getId(),});
                if (!householdMap.containsVertex(householdId)) {
                    householdMap.addVertex(householdId);
                }
                householdMap.addEdge(householdId, newPerson.getId(), rel);
                rel.setDurations(Double.POSITIVE_INFINITY); // Never expires
            }
        }

        @SuppressWarnings("unchecked")
		HashMap<Integer, float[]> contactOptions
                = (HashMap<Integer, float[]>) getFields()[FIELDS_REMOTE_METAPOP_COVID19_CONTACT_OPTIONS];

        contactOptions.put(newPerson.getId(), contactOptions.get(removedPerson.getId()));
        //contactOptions.remove(removedPerson.getId());

        Float[] lockDownOrginal = inLockdownUntil(removedPerson);

        if (lockDownOrginal != null) {

            boolean hasInf = false;

            Float[] lockdownUntilArr = new Float[LOCKDOWN_ENT_LENGTH];
            Arrays.fill(lockdownUntilArr, Float.NaN);

            for (int f = 0; f < lockdownUntilArr.length; f++) {
                lockdownUntilArr[f] = lockDownOrginal[f] - (float) removedPerson.getAge();

                hasInf |= Float.isInfinite(lockdownUntilArr[f]);
            }

            if (hasInf) {
                currentlyInLockdown.put(newPerson.getId(), lockdownUntilArr);
            }
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

        // Initialise default away from home location
        DEFAULT_AWAY_FROM_HOME_PROB = Arrays.copyOf((float[][]) getFields()[FIELDS_REMOTE_METAPOP_AWAY_FROM_HOME_BY_LOCATION],
                ((float[][]) getFields()[FIELDS_REMOTE_METAPOP_AWAY_FROM_HOME_BY_LOCATION]).length);

        for (int loc = 0; loc < DEFAULT_AWAY_FROM_HOME_PROB.length; loc++) {
            float[] src = ((float[][]) getFields()[FIELDS_REMOTE_METAPOP_AWAY_FROM_HOME_BY_LOCATION])[loc];
            DEFAULT_AWAY_FROM_HOME_PROB[loc] = Arrays.copyOf(src, src.length);
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

                // Last age band is 65+ - assume to be trangular in distribution
                if ((personType + 1) == pop_decom.length / 2
                        || (personType + 1) == pop_decom.length) {

                    int numLastAgeBand = modelMaxAge() - age;
                    int total_dist = ((numLastAgeBand + 1) * numLastAgeBand) / 2;
                    int pAge = getRNG().nextInt(total_dist);

                    int cumulPAge = 0;
                    int cumulPAge_Next = 2;
                    int ageDeltaToSet = 1;
                    while (pAge > cumulPAge) {
                        ageDeltaToSet++;
                        cumulPAge += cumulPAge_Next;
                        cumulPAge_Next++;
                    }
                    age = modelMaxAge() - ageDeltaToSet;

                } else {
                    delta_age = AbstractIndividualInterface.ONE_YEAR_INT;
                    age += getRNG().nextInt(delta_age);
                }

                firstSeekPartnerAge = 0; // Set to 0 for update of status 

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

        float[][] metaPopLockdownSetting = new float[popSizes.length][META_POP_STAT_META_POP_LENGTH];

        for (int loc = 0; loc < popSizes.length; loc++) {
            Arrays.fill(metaPopLockdownSetting[loc], -1);
        }
        getFields()[FIELDS_REMOTE_METAPOP_COVID19_META_POP_LOCKDOWN_SETTING] = metaPopLockdownSetting;

        cumulative_incident_by_location = new int[popSizes.length];

    }

    public void setInfectionList(AbstractInfection[] infList) {
        setInfList(infList);
    }

    public int[][] generateInfectionStat() {
        int[] popSize = (int[]) getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_POP_SIZE];
        COVID19_Remote_Infection covid19 = (COVID19_Remote_Infection) getInfList()[INF_COVID19];

        int[][] numStat = new int[NUM_STAT_NUM_LENGTH][popSize.length];
        int[] num_in_loc = numStat[NUM_STAT_NUM_IN_LOC];
        int[] num_infectious = numStat[NUM_STAT_NUM_INFECTIOUS];
        int[] num_with_sym = numStat[NUM_STAT_NUM_WITH_SYM];
        int[] num_infected = numStat[NUM_STAT_NUM_INFECTED];

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

        return numStat;

    }

    @Override
    public int getCurrentLocation(AbstractIndividualInterface person) {
        int cLoc = super.getCurrentLocation(person);
        if (cLoc < 0) {
            cLoc = ((Person_Remote_MetaPopulation) person).getHomeLocation();
        }
        return cLoc;
    }

    public void printCSVOutputEntry(PrintWriter csvOutput,
            int[][] numStat) {

        int t = getGlobalTime();

        HashMap<List<Integer>, int[]> completedIncidenceCollection = incidence_collenction_final;

        if (numStat == null) {
            numStat = generateInfectionStat();
        }

        double[][] r0_collection = new double[3][completedIncidenceCollection.size()];
        int k = 0;

        for (List<Integer> incidKey : completedIncidenceCollection.keySet()) {
            int[] infectStat = completedIncidenceCollection.get(incidKey);
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

        csvOutput.print(t);
        for (int[] numArr : numStat) {
            for (int i = 0; i < numArr.length; i++) {
                csvOutput.print(',');
                csvOutput.print(numArr[i]);
            }
        }

        csvOutput.print(',');
        csvOutput.print(incidence_collection.size());

        for (int i = 0; i < cumulative_incident_by_location.length; i++) {
            csvOutput.print(',');
            csvOutput.print(cumulative_incident_by_location[i]);

        }

        int[] numInQuarantine = new int[numStat[NUM_STAT_NUM_IN_LOC].length];
        int[] numInIsolation = new int[numStat[NUM_STAT_NUM_IN_LOC].length];
        @SuppressWarnings("unchecked")
		HashMap<Integer, Number[]> quarantineMap
                = (HashMap<Integer, Number[]>) getFields()[FIELDS_REMOTE_METAPOP_COVID19_CURRENTLY_IN_QUARANTINE];

        for (Integer pid : quarantineMap.keySet()) {
            Number[] quarantineEnt = quarantineMap.get(pid);
            Integer inQuantineUntil = (Integer) quarantineEnt[QUARANTINE_UNTIL_AGE];
            boolean isCaseIsolation = (Integer) quarantineEnt[QUARANTINE_ISOLATION] > 0;
            Person_Remote_MetaPopulation rmp = (Person_Remote_MetaPopulation) getLocalData().get(pid);
            if (rmp != null) {
                if (rmp.getAge() < inQuantineUntil) {
                    if (isCaseIsolation) {
                        numInIsolation[rmp.getHomeLocation()]++;
                    } else {
                        numInQuarantine[rmp.getHomeLocation()]++;
                    }
                }
            }
        }

        for (int i = 0; i < numInQuarantine.length; i++) {
            csvOutput.print(',');
            csvOutput.print(numInIsolation[i]);
        }

        for (int i = 0; i < numInQuarantine.length; i++) {
            csvOutput.print(',');
            csvOutput.print(numInQuarantine[i]);
        }

        int[][] numInLockdown = new int[2][numStat[NUM_STAT_NUM_IN_LOC].length]; // inter, within

        for (Integer pid : currentlyInLockdown.keySet()) {
            Person_Remote_MetaPopulation rmp = (Person_Remote_MetaPopulation) getLocalData().get(pid);
            if (rmp != null) {
                Float[] inLockdownUntilEnt = inLockdownUntil(rmp);
                if (inLockdownUntilEnt != null) {
                    if (!Float.isNaN(inLockdownUntilEnt[LOCKDOWN_INTER_META_POP_UNTIL_AGE])
                            && rmp.getAge() < inLockdownUntilEnt[LOCKDOWN_INTER_META_POP_UNTIL_AGE]) {
                        numInLockdown[0][rmp.getHomeLocation()]++;
                    }
                    if (!Float.isNaN(inLockdownUntilEnt[LOCKDOWN_WITHIN_META_POP_UNTIL_AGE])
                            && rmp.getAge() < inLockdownUntilEnt[LOCKDOWN_WITHIN_META_POP_UNTIL_AGE]) {
                        numInLockdown[1][rmp.getHomeLocation()]++;
                    }

                }
            }

        }

        for (int i = 0; i < numInLockdown.length; i++) {
            for (int j = 0; j < numInLockdown[i].length; j++) {
                csvOutput.print(',');
                csvOutput.print(numInLockdown[i][j]);
            }
        }

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
        csvOutput.print("# cumulative incidents");
        for (int i = 1; i < popSize.length; i++) {
            csvOutput.print(',');
        }
        csvOutput.print(',');
        csvOutput.print("# in Isolation");
        for (int i = 1; i < popSize.length; i++) {
            csvOutput.print(',');
        }
        csvOutput.print(',');
        csvOutput.print("# in Quarantine");
        for (int i = 1; i < popSize.length; i++) {
            csvOutput.print(',');
        }
        csvOutput.print(',');
        csvOutput.print("# in Lockdown (inter meta pop)");
        for (int i = 1; i < popSize.length; i++) {
            csvOutput.print(',');
        }
        csvOutput.print(',');
        csvOutput.print("# in Lockdown (within meta pop)");
        for (int i = 1; i < popSize.length; i++) {
            csvOutput.print(',');
        }

        csvOutput.print(',');
        csvOutput.print("# successful tranmission per infection (household),,");
        csvOutput.print(',');
        csvOutput.print("# successful tranmission per infection (non-household),,");
        csvOutput.print(',');
        csvOutput.print("# successful tranmission per infection (all),,");
        csvOutput.println();
    }

    public void printPatientStat(AbstractIndividualInterface patientZero, PrintWriter pri) {
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
        pri.println(String.format("Symptoms dissipate after %d days",
                (int) (param[COVID19_Remote_Infection.PARAM_SYMPTOM_END_AGE]
                - param[COVID19_Remote_Infection.PARAM_SYMPTOM_START_AGE])));
        pri.println(String.format("Infection clear after %d days since exposure.",
                (int) (param[COVID19_Remote_Infection.PARAM_INFECTED_UNTIL_AGE]
                - param[COVID19_Remote_Infection.PARAM_AGE_OF_EXPOSURE])));

        if (Double.isFinite(param[COVID19_Remote_Infection.PARAM_IMMUMED_UNTIL_AGE])) {
            pri.println(String.format("Become susceptible again after %d days since post-infection.",
                    (int) (param[COVID19_Remote_Infection.PARAM_IMMUMED_UNTIL_AGE]
                    - param[COVID19_Remote_Infection.PARAM_INFECTED_UNTIL_AGE])));
        }
    }

    public void printHousholdStat(PrintWriter pri) {

        RelationshipMap householdMap = getRelMap()[RELMAP_HOUSEHOLD];
        int[] popSize = (int[]) getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_POP_SIZE];

        pri.println("==================");
        pri.println("Pop size: " + Arrays.toString(popSize));
        pri.println("Household: ");
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

    }

    public void printPopulationSnapHeader(PrintWriter wri) {
        int[] popSize = (int[]) getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_POP_SIZE];
        wri.print("Time");

        for (int loc = 0; loc < popSize.length; loc++) {
            // 0
            wri.print(',');
            wri.print(String.format("PopSize_%d", loc));
            // 1
            wri.print(',');
            wri.print(String.format("At_home_%d", loc));
            // 2
            wri.print(',');
            wri.print(String.format("Suscepible_%d", loc));
            // 3
            wri.print(',');
            wri.print(String.format("Infected_%d", loc));
            // 4
            wri.print(',');
            wri.print(String.format("Infectious_%d", loc));
            // 5
            wri.print(',');
            wri.print(String.format("Immune_%d", loc));
            // 6
            wri.print(',');
            wri.print(String.format("hasSym_%d", loc));
            // 7            
            wri.print(',');
            wri.print(String.format("In_quarantine_%d", loc));
            // 8            
            wri.print(',');
            wri.print(String.format("In_lockdown_between_%d", loc));
            // 9
            wri.print(',');
            wri.print(String.format("In_lockdown_within_%d", loc));

        }
        wri.println();

    }

    public static final int POPSNAP_POPSIZE = 0;
    public static final int POPSNAP_AT_HOME = POPSNAP_POPSIZE + 1;
    public static final int POPSNAP_SUS = POPSNAP_AT_HOME + 1;
    public static final int POPSNAP_INF = POPSNAP_SUS + 1;
    public static final int POPSNAP_INFECTIOUS = POPSNAP_INF + 1;
    public static final int POPSNAP_IMM = POPSNAP_INFECTIOUS + 1;
    public static final int POPSNAP_SYM = POPSNAP_IMM + 1;
    public static final int POPSNAP_INQ = POPSNAP_SYM + 1;
    public static final int POPSNAP_IN_LOCKDOWN_BETWEEN = POPSNAP_INQ + 1;
    public static final int POPSNAP_IN_LOCKDOWN_WITHIN = POPSNAP_IN_LOCKDOWN_BETWEEN + 1;
    public static final int POPSNAP_LENGTH = POPSNAP_IN_LOCKDOWN_WITHIN + 1;

    public void printPopulationSnapCSV(PrintWriter wri) {
        COVID19_Remote_Infection covid19 = (COVID19_Remote_Infection) getInfList()[INF_COVID19];
        int[] popSize = (int[]) getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_POP_SIZE];
        wri.print(getGlobalTime());

        int[][] statAll = new int[popSize.length][POPSNAP_LENGTH];

        for (AbstractIndividualInterface p : getPop()) {
            Person_Remote_MetaPopulation rmp = (Person_Remote_MetaPopulation) p;
            int homeLoc = rmp.getHomeLocation();

            statAll[homeLoc][POPSNAP_POPSIZE]++;
            if (homeLoc == getCurrentLocation(p)) {
                statAll[homeLoc][POPSNAP_AT_HOME]++;
            }
            int infStat = rmp.getInfectionStatus()[INF_COVID19];
            switch (infStat) {
                case AbstractIndividualInterface.INFECT_S:
                    statAll[homeLoc][POPSNAP_SUS]++;
                    break;
                case COVID19_Remote_Infection.STATUS_IMMUNED:
                    statAll[homeLoc][POPSNAP_IMM]++;
                    break;
                default:
                    statAll[homeLoc][POPSNAP_INF]++;
            }
            if (covid19.isInfectious(p)) {
                statAll[homeLoc][POPSNAP_INFECTIOUS]++;
            }
            if (covid19.hasSymptoms(p)) {
                statAll[homeLoc][POPSNAP_SYM]++;
            }
            Number[] inQuarantine = inQuarantine(p);
            if (inQuarantine != null) {
                statAll[homeLoc][POPSNAP_INQ]++;
            }

            Float[] inLockdown = inLockdownUntil(p);
            if (inLockdown != null) {
                if (p.getAge() < inLockdown[LOCKDOWN_INTER_META_POP_UNTIL_AGE]) {
                    statAll[homeLoc][POPSNAP_IN_LOCKDOWN_BETWEEN]++;
                }
                if (p.getAge() < inLockdown[LOCKDOWN_WITHIN_META_POP_UNTIL_AGE]) {
                    statAll[homeLoc][POPSNAP_IN_LOCKDOWN_WITHIN]++;
                }

            }

        }

        for (int loc = 0; loc < popSize.length; loc++) {
            for (int stat = 0; stat < statAll[loc].length; stat++) {
                wri.print(',');
                wri.print(statAll[loc][stat]);
            }
        }

        wri.println();

    }

    public List<ArrayList<AbstractIndividualInterface>> triggerLockdown(int loc) {
        float[][] metaPopLockdownSetting
                = (float[][]) getFields()[FIELDS_REMOTE_METAPOP_COVID19_META_POP_LOCKDOWN_SETTING];

        List<ArrayList<AbstractIndividualInterface>> res = List.of(new ArrayList<>(), new ArrayList<>());

        if (loc < metaPopLockdownSetting.length) {

            int[] popSize = (int[]) getFields()[FIELDS_REMOTE_METAPOP_POP_SIZE];

            //Integer[] unqiueHouseholdAtLoc = ((Integer[][]) getFields()[FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD])[loc];
            float[] lockdownStat = metaPopLockdownSetting[loc];

            if (home_loc_age_gender_collection == null) {
                updateCollectionHomeLocGenderAge();
            }

            if (lockdownStat[META_POP_INTER_LOCKDOWN_PROPORTION] > 0) {
                fillCurrentlyInLockdown(res.get(0),
                        Math.round(lockdownStat[META_POP_INTER_LOCKDOWN_PROPORTION] * popSize[loc]),
                        home_loc_age_gender_collection[loc],
                        lockdownStat[META_POP_INTER_LOCKDOWN_END] - lockdownStat[META_POP_INTER_LOCKDOWN_START],
                        LOCKDOWN_INTER_META_POP_UNTIL_AGE);
            }

            if (lockdownStat[META_POP_WITHIN_LOCKDOWN_PROPORTION] > 0) {
                fillCurrentlyInLockdown(res.get(1),
                        Math.round(lockdownStat[META_POP_WITHIN_LOCKDOWN_PROPORTION] * popSize[loc]),
                        home_loc_age_gender_collection[loc],
                        lockdownStat[META_POP_WITHIN_LOCKDOWN_END] - lockdownStat[META_POP_WITHIN_LOCKDOWN_START],
                        LOCKDOWN_WITHIN_META_POP_UNTIL_AGE);
            }

        }

        return res;
    }

    public Float[] inLockdownUntil(AbstractIndividualInterface p) {
        Float[] lockdownUntilArr = currentlyInLockdown.get(p.getId());
        if (lockdownUntilArr != null) {
            if (p.getAge() >= lockdownUntilArr[LOCKDOWN_INTER_META_POP_UNTIL_AGE]
                    && p.getAge() >= lockdownUntilArr[LOCKDOWN_WITHIN_META_POP_UNTIL_AGE]) {
                currentlyInLockdown.remove(p.getId());
                lockdownUntilArr = null;
            }
        }
        return lockdownUntilArr;

    }

    private void fillCurrentlyInLockdown(
            ArrayList<AbstractIndividualInterface> inLockDown,
            int numIndivudalToLockDown,
            ArrayList<AbstractIndividualInterface>[] atLocByGenderAge,
            float lockdownUntil, int lockdownUntilIndex) {

        int totalAtHome = 0;
        for (ArrayList<AbstractIndividualInterface> byGenderAge : atLocByGenderAge) {
            totalAtHome += byGenderAge.size();
        }

        int numToLockdown = Math.min(numIndivudalToLockDown, totalAtHome);

        int personIndex = 0;
        while (numToLockdown > 0 && personIndex < totalAtHome) {
            for (ArrayList<AbstractIndividualInterface> byGenderAge : atLocByGenderAge) {
                for (AbstractIndividualInterface candidate : byGenderAge) {
                    int numLeft = totalAtHome - personIndex;

                    boolean inLockdown = numLeft == numToLockdown;

                    if (numLeft > numToLockdown) {
                        inLockdown = getRNG().nextInt(numLeft) < numToLockdown;
                    }

                    if (inLockdown) {

                        Float[] lockdownUntilArr = currentlyInLockdown.get(candidate.getId());

                        if (lockdownUntilArr == null) {
                            lockdownUntilArr = new Float[LOCKDOWN_ENT_LENGTH];
                            Arrays.fill(lockdownUntilArr, Float.NaN);
                            currentlyInLockdown.put(candidate.getId(), lockdownUntilArr);
                        }
                        lockdownUntilArr[lockdownUntilIndex]
                                = (float) candidate.getAge() + lockdownUntil;

                        inLockDown.add(candidate);

                        numToLockdown--;
                    }

                    personIndex++;
                }

            }
        }

    }

}
