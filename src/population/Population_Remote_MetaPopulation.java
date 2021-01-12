package population;

import availability.AbstractAvailability;
import availability.Availablity_Random_Mixing;
import infection.AbstractInfection;
import infection.ChlamydiaInfection;
import infection.GonorrhoeaInfection;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import person.AbstractIndividualInterface;
import person.MoveablePersonInterface;
import person.Person_Remote_MetaPopulation;
import static population.Abstract_MetaPopulation.FIELDS_META_POP_AWAY_UNTIL_AGE;
import random.MersenneTwisterRandomGenerator;
import random.RandomGenerator;
import relationship.RelationshipMap;
import relationship.RelationshipMapTimeStamp;
import relationship.SingleRelationship;
import relationship.SingleRelationshipTimeStamp;
import util.ArrayUtilsRandomGenerator;
import util.Factory_AwayDuration;
import util.Factory_Relationship_Duration_Max;
import util.PersonClassifier;

/**
 *
 * @author Ben Hui
 * @version 20180517
 *
 * <pre>
 * History
 *
 * 20180517
 *  - Randomising the age a candidate is removed from the population
 *
 * 20180716
 *  - Implement alterative format for FIELDS_REMOTE_METAPOP_NUMBER_PARTNER_LAST_12_MONTHS_DECOMP
 *
 * 20190328
 *  - Add support for circular mobility
 * </pre>
 */
public class Population_Remote_MetaPopulation extends Abstract_MetaPopulation {

    public static final int FIELDS_REMOTE_METAPOP_POP_SIZE = LENGTH_FIELDS_META_POP;
    public static final int FIELDS_REMOTE_METAPOP_POP_TYPE = FIELDS_REMOTE_METAPOP_POP_SIZE + 1;
    public static final int FIELDS_REMOTE_METAPOP_POP_CONNC = FIELDS_REMOTE_METAPOP_POP_TYPE + 1;
    public static final int FIELDS_REMOTE_METAPOP_AGE_GENDER_COMPOSITION = FIELDS_REMOTE_METAPOP_POP_CONNC + 1;
    public static final int FIELDS_REMOTE_METAPOP_AGE_GRP_CLASSIFIER = FIELDS_REMOTE_METAPOP_AGE_GENDER_COMPOSITION + 1;
    public static final int FIELDS_REMOTE_METAPOP_BEHAVOR_GRP_CLASSIFIER = FIELDS_REMOTE_METAPOP_AGE_GRP_CLASSIFIER + 1;
    public static final int FIELDS_REMOTE_METAPOP_NUMBER_PARTNER_LAST_12_MONTHS_DECOMP = FIELDS_REMOTE_METAPOP_BEHAVOR_GRP_CLASSIFIER + 1;
    public static final int FIELDS_REMOTE_METAPOP_INFECTION_LIST = FIELDS_REMOTE_METAPOP_NUMBER_PARTNER_LAST_12_MONTHS_DECOMP + 1;
    public static final int FIELDS_REMOTE_METAPOP_FREQ_OF_SEX_BY_LOCATION = FIELDS_REMOTE_METAPOP_INFECTION_LIST + 1;
    public static final int FIELDS_REMOTE_METAPOP_CONDOM_USAGE_BY_LOCATION = FIELDS_REMOTE_METAPOP_FREQ_OF_SEX_BY_LOCATION + 1;
    public static final int FIELDS_REMOTE_METAPOP_AWAY_FROM_HOME_BY_LOCATION = FIELDS_REMOTE_METAPOP_CONDOM_USAGE_BY_LOCATION + 1;
    public static final int FIELDS_REMOTE_METAPOP_AWAY_FROM_HOME_DURATION_FACTORY = FIELDS_REMOTE_METAPOP_AWAY_FROM_HOME_BY_LOCATION + 1;
    public static final int FIELDS_REMOTE_METAPOP_RELATIONSHIP_DURATION_FACTORY = FIELDS_REMOTE_METAPOP_AWAY_FROM_HOME_DURATION_FACTORY + 1;
    public static final int FIELDS_REMOTE_METAPOP_NEWPERSON_INFECTION_CLASSIFIER = FIELDS_REMOTE_METAPOP_RELATIONSHIP_DURATION_FACTORY + 1;
    public static final int FIELDS_REMOTE_METAPOP_NEWPERSON_INFECTION_PREVAL = FIELDS_REMOTE_METAPOP_NEWPERSON_INFECTION_CLASSIFIER + 1;
    public static final int FIELDS_REMOTE_METAPOP_CIR_MOBILITY_MAP = FIELDS_REMOTE_METAPOP_NEWPERSON_INFECTION_PREVAL + 1;

    public static final int LENGTH_FIELDS_REMOTE_META_POP = FIELDS_REMOTE_METAPOP_CIR_MOBILITY_MAP + 1;

    public static final int RELMAP_GLOBAL_SEXUAL = 0;

    Object[] DEFAULT_FIELDS_REMOTE_META_POP = new Object[]{
        // FIELDS_REMOTE_METAPOP_POP_SIZE
        new int[]{},
        // FIELDS_REMOTE_METAPOP_POP_TYPE
        // 1 = urban, 2 = regional, 3 = remote, -1 = undefined
        new int[]{},
        // FIELDS_REMOTE_METAPOP_POP_CONNC
        // Conncection matrix, with self as 0, no-connection as -1 and rest as cost of travel
        new int[][]{},
        // FIELDS_REMOTE_METAPOP_AGE_GENDER_COMPOSITION
        // int[loc_id]{...}        
        // e.g. 
        //  util.Population_Decomposition_Factory.getDecomposition(util.Population_Decomposition_Factory.POP_COMPOSITION_URBAN_2011, 1000)                        
        new int[][]{},
        // FIELDS_REMOTE_METAPOP_AGE_GRP_CLASSIFIER
        new util.Default_Remote_MetaPopulation_AgeGrp_Classifier(),
        // FIELDS_REMOTE_METAPOP_BEHAVOR_GRP_CLASSIFIER
        new util.Default_Remote_MetaPopulation_Behavor_Classifier(),
        // FIELDS_REMOTE_METAPOP_NUMBER_PARTNER_LAST_12_MONTHS_DECOMP
        // float[popId (or 0 if all share the same behavor)][ageIndex]{0, 1, 2-4, 5}       
        // From GOANNA pg, 22
        // Alterative format
        // float[popId][ageIndex][0, 1, 2-4, 5, probability of seeking extra partners as traveller]

        new float[][][]{
            new float[][]{
                // 16-19
                new float[]{0.09f, 0.40f, 0.42f, 0.09f},
                // 20-24
                new float[]{0.07f, 0.47f, 0.38f, 0.08f},
                // 25-29
                new float[]{0.09f, 0.55f, 0.32f, 0.04f},
                // 30-35 (descreasing based on linear trends for first 3 grp)
                new float[]{0.09f, 0.62f, 0.27f, 0.02f},},},
        // FIELDS_REMOTE_METAPOP_INFECTION_LIST
        // To be set prior initisaltion        
        new AbstractInfection[]{new ChlamydiaInfection(this.getInfectionRNG()), new GonorrhoeaInfection(this.getInfectionRNG())},
        // FIELDS_REMOTE_METAPOP_FREQ_OF_SEX_BY_LOCATION
        // From ASHR2: 
        // Badcock PB, Smith AMA, Richters J, et al. Characteristics of heterosexual regular relationships 
        // among a representative sample of adults: the Second Australian Study of Health and Relationships. Sexual health 2014;11(5):427-38.
        new float[]{1.44f / 7},
        // FIELDS_REMOTE_METAPOP_CONDOM_USAGE_BY_LOCATION
        new float[]{0.54f},
        // FIELDS_REMOTE_METAPOP_AWAY_FROM_HOME_BY_LOCATION
        // float[loc][gender+ageIndex]
        // Alt format:
        // float[loc]{[gender+ageIndex], cir_mobility_len_1, proabability_1...} 
        new float[][]{},
        // FIELDS_REMOTE_METAPOP_AWAY_FROM_HOME_DURATION_FACTORY
        new util.Factory_AwayDuration(),
        // FILEDS_REMTOE_METAPOP_RELATIONSHIP_DURATION_FACTORY
        new util.Factory_Relationship_Duration_Max(),
        // FIELDS_REMOTE_METAPOP_INTRO_INF_CLASSIFIER
        // Format: Classifier[infId]
        null,
        // FIELDS_REMOTE_METAPOP_INTRO_INF_PREVAL
        // Format: float[infId][cI]
        null,
        // FIELDS_REMOTE_METAPOP_CIR_MOBILITY_MAP
        // Format: HashMap(Id, int[]{age_1, location_1 ...})
        new HashMap<Integer, int[]>(),};

    // A matrix ArrayList of AbstractIndividualInterface, index by home_loc, gender + age    
    protected transient ArrayList<AbstractIndividualInterface>[][] home_loc_age_gender_collection = null;

    // A matrix of ArrayList of AbstractIndividualInterface, index by current_loc, age and behaviour    
    protected transient ArrayList<AbstractIndividualInterface>[][][] current_loc_age_behaviour_collection = null;

    // A matrix of ArrayList of AbstractIndividualInterface, index by home_loc, gender + age, at home or not    
    protected transient ArrayList<AbstractIndividualInterface>[][][] home_loc_age_gender_home_or_away_collection = null;

    // A HashMap of ArrayList of indivudal for removal 
    protected transient HashMap<Integer, ArrayList<AbstractIndividualInterface>> timeSpecificAgeOutCandidates = null;

    public Population_Remote_MetaPopulation(long seed) {
        super();
        Object[] orgFields = this.getFields();
        Object[] newFields = Arrays.copyOf(orgFields, orgFields.length + DEFAULT_FIELDS_REMOTE_META_POP.length);
        System.arraycopy(DEFAULT_FIELDS_REMOTE_META_POP, 0, newFields, orgFields.length, DEFAULT_FIELDS_REMOTE_META_POP.length);
        this.setFields(newFields);

        setSeed(seed);
        setRNG(new MersenneTwisterRandomGenerator(seed));
        setInfectionRNG(new MersenneTwisterRandomGenerator(seed)); // For infection specific e.g. duration of infection

    }

    public static Population_Remote_MetaPopulation decodeFromStream(java.io.ObjectInputStream inStr)
            throws IOException, ClassNotFoundException {
        int globalTime = inStr.readInt();
        AbstractInfection[] infList = (AbstractInfection[]) inStr.readObject();
        Object[] decoded_fields = (Object[]) inStr.readObject();
        Population_Remote_MetaPopulation pop = new Population_Remote_MetaPopulation((long) decoded_fields[0]);

        if (decoded_fields.length != pop.getFields().length) {
            int oldLen = decoded_fields.length;
            decoded_fields = Arrays.copyOf(decoded_fields, pop.getFields().length);
            for (int i = oldLen; i < decoded_fields.length; i++) {
                decoded_fields[i] = pop.getFields()[i];
            }
        }
        pop.setGlobalTime(globalTime);
        pop.setInfList(infList);
        pop.setFields(decoded_fields);

        return pop;
    }
    
    public AbstractIndividualInterface getPersonById(int id){
        return getLocalData().get(id);
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
        RandomGenerator rng = getRNG();

        for (int i = 0; i < popSizes.length; i++) {
            totalPopSize += popSizes[i];
        }

        AbstractIndividualInterface[] pop = new AbstractIndividualInterface[totalPopSize];

        for (int locId = 0; locId < popSizes.length; locId++) {
            int pt = locId;
            int locTotal = 0;
            int[] pop_decom = Arrays.copyOf(pop_decom_collection[pt], pop_decom_collection[pt].length);

            for (int newP = 0; newP < popSizes[locId]; newP++) {
                int pPerson = rng.nextInt(popSizes[locId] - locTotal);

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

                if (personType == kStart) {
                    age = AbstractIndividualInterface.ONE_YEAR_INT * 16; // First age group is at 16 rather than 15
                    delta_age = AbstractIndividualInterface.ONE_YEAR_INT * 4;
                } else {
                    age = AbstractIndividualInterface.ONE_YEAR_INT * 15;
                    delta_age = AbstractIndividualInterface.ONE_YEAR_INT * 5;

                    for (int k = kStart; k < personType; k++) {
                        age += AbstractIndividualInterface.ONE_YEAR_INT * 5;
                    }
                }

                age += rng.nextInt(delta_age);

                // From GOANNA, pg 22, age of first sex < 16 
                float upBound = 0.85f;
                if (age > 20 * AbstractIndividualInterface.ONE_YEAR_INT) {
                    upBound = 0.64f;
                }
                if (age > 25 * AbstractIndividualInterface.ONE_YEAR_INT) {
                    upBound = 0.63f;
                }

                firstSeekPartnerAge = rng.nextFloat()
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

        AbstractAvailability[] availByLoc = new AbstractAvailability[popSizes.length];
        for (int i = 0; i < availByLoc.length; i++) {
            availByLoc[i] = new Availablity_Random_Mixing(getRNG());
            availByLoc[i].setRelationshipMap(getRelMap()[RELMAP_GLOBAL_SEXUAL]);
        }

        setAvailability(availByLoc);

    }

    public void updateInfectionList(AbstractInfection[] infList) {
        int infId = 0;
        for (AbstractInfection inf : infList) {
            inf.setRNG(getInfectionRNG());
            inf.setInfectionIndex(infId);
            infId++;
            //System.out.println("Inf " + infId + ": " + inf.toString() + " RNG = " + inf.getRNG());
        }
        setInfList(infList);
    }

    public void updateCollectionCurrentLocAgeBehavior() {
        int[][] pop_decom_collection = (int[][]) getFields()[FIELDS_REMOTE_METAPOP_AGE_GENDER_COMPOSITION];
        PersonClassifier age_classifier = (PersonClassifier) getFields()[FIELDS_REMOTE_METAPOP_AGE_GRP_CLASSIFIER];
        PersonClassifier behav_classifier = (PersonClassifier) getFields()[FIELDS_REMOTE_METAPOP_BEHAVOR_GRP_CLASSIFIER];

        if (current_loc_age_behaviour_collection == null) {
            current_loc_age_behaviour_collection = new ArrayList[pop_decom_collection.length][age_classifier.numClass()][behav_classifier.numClass()];
        }

        for (ArrayList<AbstractIndividualInterface>[][] byLocation : current_loc_age_behaviour_collection) {
            for (ArrayList<AbstractIndividualInterface>[] byLocAgeBehaviour : byLocation) {
                for (int b = 0; b < byLocAgeBehaviour.length; b++) {
                    if (byLocAgeBehaviour[b] == null) {
                        byLocAgeBehaviour[b] = new ArrayList<>();
                    } else {
                        byLocAgeBehaviour[b].clear();
                    }
                }
            }
        }

        AbstractIndividualInterface[] pop = getPop();
        for (AbstractIndividualInterface per : pop) {
            Person_Remote_MetaPopulation person = (Person_Remote_MetaPopulation) per;
            int a = age_classifier.classifyPerson(person);
            int b = behav_classifier.classifyPerson(person);
            int currentLoc = getCurrentLocation(person);
            current_loc_age_behaviour_collection[currentLoc][a][b].add(person);

        }
    }

    public void updateCollectionHomeLocGenderAge() {
        int[][] pop_decom_collection = (int[][]) getFields()[FIELDS_REMOTE_METAPOP_AGE_GENDER_COMPOSITION];
        PersonClassifier age_classifier = (PersonClassifier) getFields()[FIELDS_REMOTE_METAPOP_AGE_GRP_CLASSIFIER];

        if (home_loc_age_gender_collection == null) {
            home_loc_age_gender_collection = new ArrayList[pop_decom_collection.length][2 * age_classifier.numClass()];
        }

        for (ArrayList<AbstractIndividualInterface>[] byLocationDemographic : home_loc_age_gender_collection) {
            for (int b = 0; b < byLocationDemographic.length; b++) {
                if (byLocationDemographic[b] == null) {
                    byLocationDemographic[b] = new ArrayList<>();
                } else {
                    byLocationDemographic[b].clear();
                }
            }
        }

        AbstractIndividualInterface[] pop = getPop();
        for (AbstractIndividualInterface per : pop) {
            Person_Remote_MetaPopulation person = (Person_Remote_MetaPopulation) per;
            int g = person.isMale() ? 0 : age_classifier.numClass();
            int a = age_classifier.classifyPerson(person);
            int loc = (int) person.getFields()[Person_Remote_MetaPopulation.PERSON_HOME_LOC];
            home_loc_age_gender_collection[loc][g + a].add(person);

        }
    }

    public void updateCollectionHomeLocAgeGenderHomeOrAway() {
        int[][] pop_decom_collection = (int[][]) getFields()[FIELDS_REMOTE_METAPOP_AGE_GENDER_COMPOSITION];
        PersonClassifier age_classifier = (PersonClassifier) getFields()[FIELDS_REMOTE_METAPOP_AGE_GRP_CLASSIFIER];

        if (home_loc_age_gender_home_or_away_collection == null) {
            home_loc_age_gender_home_or_away_collection = new ArrayList[pop_decom_collection.length][2 * age_classifier.numClass()][2];
        }
        for (ArrayList<AbstractIndividualInterface>[][] byLocation : home_loc_age_gender_home_or_away_collection) {
            for (ArrayList<AbstractIndividualInterface>[] byLocationDemographic : byLocation) {
                for (int h_a = 0; h_a < byLocationDemographic.length; h_a++) {
                    if (byLocationDemographic[h_a] == null) {
                        byLocationDemographic[h_a] = new ArrayList<>();
                    } else {
                        byLocationDemographic[h_a].clear();
                    }
                }
            }
        }
        AbstractIndividualInterface[] pop = getPop();
        for (AbstractIndividualInterface per : pop) {
            Person_Remote_MetaPopulation person = (Person_Remote_MetaPopulation) per;
            int g = person.isMale() ? 0 : age_classifier.numClass();
            int a = age_classifier.classifyPerson(person);
            int loc = (int) person.getFields()[Person_Remote_MetaPopulation.PERSON_HOME_LOC];
            int currentLoc = getCurrentLocation(person);
            home_loc_age_gender_home_or_away_collection[loc][g + a][loc == currentLoc ? 0 : 1].add(person);

        }
    }

    @Override
    public void advanceTimeStep(int deltaT) {
        incrementTime(deltaT);

        int[][] pop_decom_collection = (int[][]) getFields()[FIELDS_REMOTE_METAPOP_AGE_GENDER_COMPOSITION];
        float[][][] pop_decom_numPartIn12Month = (float[][][]) getFields()[FIELDS_REMOTE_METAPOP_NUMBER_PARTNER_LAST_12_MONTHS_DECOMP];

        AbstractIndividualInterface[][][] availableAllLocation
                = new AbstractIndividualInterface[pop_decom_collection.length][2][getPop().length];
        int[][] availablePt = new int[pop_decom_collection.length][2];

        AbstractIndividualInterface[] travellerSeeking = new AbstractIndividualInterface[getPop().length];
        int travellerSeekingPt = 0;

        if (timeSpecificAgeOutCandidates == null) {
            timeSpecificAgeOutCandidates = new HashMap<>();
        }

        int ageOutFreq = AbstractIndividualInterface.ONE_YEAR_INT;

        if (getGlobalTime() % ageOutFreq == 0) { // Update yearly

            updateCollectionHomeLocGenderAge();

            // Check how many need to be remove from the population
            for (int loc = 0; loc < home_loc_age_gender_collection.length; loc++) {

                for (int ga = 0; ga < home_loc_age_gender_collection[loc].length; ga++) {
                    int inData = pop_decom_collection[loc][ga];
                    int inPop = home_loc_age_gender_collection[loc][ga].size();

                    AbstractIndividualInterface[] candidate
                            = home_loc_age_gender_collection[loc][ga].toArray(new AbstractIndividualInterface[inPop]);
                    int diffDemographpic = inData - inPop;
                    if (diffDemographpic < 0) {
                        candidate = ArrayUtilsRandomGenerator.randomSelect(candidate, -diffDemographpic, getRNG());
                        for (AbstractIndividualInterface toBeRemove : candidate) {
                            int offset = getRNG().nextInt(ageOutFreq);
                            ArrayList<AbstractIndividualInterface> preEnt = timeSpecificAgeOutCandidates.get(this.getGlobalTime() + offset);
                            if (preEnt == null) {
                                preEnt = new ArrayList<>();
                            };
                            preEnt.add(toBeRemove);
                            timeSpecificAgeOutCandidates.put(this.getGlobalTime() + offset, preEnt);
                        }
                    }
                }
            }
        }

        // Set age out person as inf age 
        if (timeSpecificAgeOutCandidates.containsKey(this.getGlobalTime())) {
            AbstractIndividualInterface[] ageOutCandidate
                    = timeSpecificAgeOutCandidates.get(this.getGlobalTime()).toArray(new AbstractIndividualInterface[0]);
            for (AbstractIndividualInterface toBeRemove : ageOutCandidate) {
                toBeRemove.setAge(Double.POSITIVE_INFINITY); // Set to INF age, and remove in the iteration
            }
            timeSpecificAgeOutCandidates.remove(this.getGlobalTime());
        }

        // Remove age out person
        for (int index = 0; index < getPop().length; index++) {
            getPop()[index].incrementTime(deltaT, getInfList());

            Person_Remote_MetaPopulation removeCandidate = (Person_Remote_MetaPopulation) getPop()[index];

            double ageoffset = 0;

            if (removeCandidate.getAge() > (modelMaxAge() + ageoffset)) {
                AbstractIndividualInterface addedPerson;
                int nextId = (int) getFields()[FIELDS_NEXT_ID];
                addedPerson = replacePerson(removeCandidate, nextId);
                // Replace person 
                getPop()[index] = addedPerson;
                getLocalData().put(index, addedPerson);
                getFields()[FIELDS_NEXT_ID] = nextId + 1;
                movePerson(addedPerson, removeCandidate.getHomeLocation(), -1);
            }
        }

        updateCollectionCurrentLocAgeBehavior();

        if (getAvailability() != null) {
            // Check how many need to seek partner, or terminate partnership
            PersonClassifier behav_classifier = (PersonClassifier) getFields()[FIELDS_REMOTE_METAPOP_BEHAVOR_GRP_CLASSIFIER];
            for (int loc = 0; loc < current_loc_age_behaviour_collection.length; loc++) {
                float[][] locBehavour = pop_decom_numPartIn12Month[loc < pop_decom_numPartIn12Month.length ? loc : 0];
                for (int a = 0; a < locBehavour.length; a++) {
                    float totalInLocAgeBehaviour = 0;

                    for (int b = 0; b < behav_classifier.numClass(); b++) {
                        totalInLocAgeBehaviour += current_loc_age_behaviour_collection[loc][a][b].size();
                    }
                    int[] behavInData = new int[behav_classifier.numClass()];
                    int[] behavInPop = new int[behav_classifier.numClass()];

                    int roundOffSum = 0;
                    for (int b = 0; b < behav_classifier.numClass(); b++) {
                        behavInData[b] = Math.round(locBehavour[a][b] * totalInLocAgeBehaviour);
                        roundOffSum += behavInData[b];
                        behavInPop[b] = current_loc_age_behaviour_collection[loc][a][b].size();
                    }

                    roundOffSum = (int) (roundOffSum - totalInLocAgeBehaviour);

                    while (roundOffSum != 0) {
                        int index = getRNG().nextInt(behavInData.length);
                        if (roundOffSum < 0) {
                            behavInData[index]++;
                            roundOffSum++;
                        } else {
                            if (behavInData[index] > 0) {
                                behavInData[index]--;
                                roundOffSum--;
                            }
                        }

                    }
                    for (int b = 0; b < behav_classifier.numClass(); b++) {

                        AbstractIndividualInterface[] collectionArr = new AbstractIndividualInterface[current_loc_age_behaviour_collection[loc][a][b].size()];
                        collectionArr = current_loc_age_behaviour_collection[loc][a][b].toArray(collectionArr);

                        int diffBehav = behavInData[b] - behavInPop[b];

                        int numSeekOrBreakPartnership = Math.max(-diffBehav, 0);

                        if (numSeekOrBreakPartnership > 0) {

                            int extraBelow = 0;
                            int extraAbove = 0;

                            for (int bb = 0; bb < b; bb++) {
                                extraBelow += Math.max(behavInData[bb] - behavInPop[bb], 0);
                            }
                            for (int ba = (b + 1); ba < behav_classifier.numClass(); ba++) {
                                extraAbove += Math.max(behavInData[ba] - behavInPop[ba], 0);
                            }

                            for (int s = 0; s < collectionArr.length && numSeekOrBreakPartnership > 0; s++) {
                                AbstractIndividualInterface seekOrBreak = collectionArr[s];
                                boolean seeking = false;
                                boolean breaking = false;
                                if (getRNG().nextInt(collectionArr.length - s) < numSeekOrBreakPartnership) {
                                    boolean breakingPartnership = getRNG().nextInt(extraBelow + extraAbove) < extraBelow;

                                    if (!breakingPartnership) {
                                        extraAbove--;
                                        numSeekOrBreakPartnership--;
                                        // Form new partnership
                                        int genderPt = seekOrBreak.isMale() ? 0 : 1;

                                        if (((Person_Remote_MetaPopulation) seekOrBreak).getFields()[Person_Remote_MetaPopulation.PERSON_FIRST_SEEK_PARTNER_AGE]
                                                < seekOrBreak.getAge()) {
                                            availableAllLocation[loc][genderPt][availablePt[loc][genderPt]] = seekOrBreak;
                                            availablePt[loc][genderPt]++;
                                        }
                                        seeking = true;

                                    } else {
                                        // Dissolve partnership
                                        if (getRelMap()[RELMAP_GLOBAL_SEXUAL].containsVertex(seekOrBreak.getId())) {
                                            extraBelow--;
                                            numSeekOrBreakPartnership--;

                                            int numEdges = getRelMap()[RELMAP_GLOBAL_SEXUAL].degreeOf(seekOrBreak.getId());
                                            SingleRelationship[] rel = getRelMap()[0].edgesOf(seekOrBreak.getId()).toArray(new SingleRelationship[numEdges]);
                                            if (rel.length > 1) {
                                                Arrays.sort(rel, new Comparator<SingleRelationship>() {
                                                    @Override
                                                    public int compare(SingleRelationship t, SingleRelationship t1) {
                                                        return Double.compare(t.getDurations(), t1.getDurations());
                                                    }
                                                });
                                            }
                                            // Select the shortest one for removal         
                                            SingleRelationship toBeRemoved = rel[0];
                                            removeRelationship(getRelMap()[RELMAP_GLOBAL_SEXUAL],
                                                    (SingleRelationshipTimeStamp) toBeRemoved,
                                                    toBeRemoved.getLinks(getLocalData()));

                                            breaking = true;
                                        }
                                    }
                                }

                                if (!seeking && locBehavour[a].length > behav_classifier.numClass()) {
                                    // Extra chance to seek a partner as traveller
                                    float probSeekExtra = locBehavour[a][behav_classifier.numClass()];
                                    seeking = ((Person_Remote_MetaPopulation) seekOrBreak).getFields()[Person_Remote_MetaPopulation.PERSON_FIRST_SEEK_PARTNER_AGE] < seekOrBreak.getAge();
                                    seeking &= ((Person_Remote_MetaPopulation) seekOrBreak).getHomeLocation() != loc;
                                    if (seeking) {
                                        if (getRNG().nextFloat() < probSeekExtra) {
                                            travellerSeeking[travellerSeekingPt] = seekOrBreak;
                                            travellerSeekingPt++;
                                        }
                                    }
                                }

                            }

                        }

                    }

                }
            }

            // Traveller seeking
            // Set off a one-off sexual contact       
            if (travellerSeekingPt > 0) {
                float[] condomUsageAll = (float[]) getFields()[FIELDS_REMOTE_METAPOP_CONDOM_USAGE_BY_LOCATION];

                for (int i = 0; i < travellerSeekingPt; i++) {
                    Person_Remote_MetaPopulation traveller = (Person_Remote_MetaPopulation) travellerSeeking[i];
                    int currentLoc = getCurrentLocation(traveller);
                    int partnerGender = traveller.isMale() ? 1 : 0;

                    if (availablePt[currentLoc][partnerGender] > 0) {
                        int randAvailPt = getRNG().nextInt(availablePt[currentLoc][partnerGender]);
                        float condomUsage = condomUsageAll[currentLoc < condomUsageAll.length ? currentLoc : 0];

                        if (!(getRNG().nextFloat() < condomUsage)) {
                            AbstractIndividualInterface[] oneOffPartners = new AbstractIndividualInterface[2];
                            oneOffPartners[traveller.isMale() ? 0 : 1] = traveller;
                            oneOffPartners[partnerGender] = availableAllLocation[currentLoc][partnerGender][randAvailPt];
                            SingleRelationship.performAct(oneOffPartners, getGlobalTime(), getInfList(), new boolean[]{true, true});

                            for (AbstractIndividualInterface p : oneOffPartners) {
                                Person_Remote_MetaPopulation person = (Person_Remote_MetaPopulation) p;
                                if (person.getParameter(Person_Remote_MetaPopulation.PERSON_FIRST_SEX_AGE) < 0) {
                                    person.setParameter(Person_Remote_MetaPopulation.PERSON_FIRST_SEX_AGE, (long) person.getAge());
                                }
                            }
                        }
                    }

                }

            }

            // Update (sexual) relationship
            updateSexualRelationships(deltaT);

            // Form pairing for each location
            Factory_Relationship_Duration_Max relMax = (Factory_Relationship_Duration_Max) getFields()[FIELDS_REMOTE_METAPOP_RELATIONSHIP_DURATION_FACTORY];

            for (int loc = 0; loc < current_loc_age_behaviour_collection.length; loc++) {
                AbstractIndividualInterface[][] availableAtLoc = availableAllLocation[loc];

                for (int g = 0; g < availableAtLoc.length; g++) {
                    availableAtLoc[g] = Arrays.copyOf(availableAtLoc[g], availablePt[loc][g]);
                }

                getAvailability()[loc].setAvailablePopulation(availableAtLoc);

                int numPairFormed = getAvailability()[loc].generatePairing();

                AbstractIndividualInterface[][] pairs = getAvailability()[loc].getPairing();

                for (int pairId = 0; pairId < numPairFormed; pairId++) {
                    SingleRelationship rel;
                    int duration = relMax.gen_maxDurationOfSexualRelationship(pairs[pairId], getRNG());
                    rel = formRelationship(pairs[pairId], getRelMap()[RELMAP_GLOBAL_SEXUAL], duration, RELMAP_GLOBAL_SEXUAL);
                    if (rel != null) {
                        performSexAct(rel, pairs[pairId]);
                    }
                }
            }
        }
        // Movement           
        predefinedMove();

        updateCollectionHomeLocAgeGenderHomeOrAway();

        for (int loc = 0; loc < home_loc_age_gender_home_or_away_collection.length; loc++) {
            ArrayList<AbstractIndividualInterface>[][] byLocation = home_loc_age_gender_home_or_away_collection[loc];

            float[] proportionAwayDataByLoc = ((float[][]) getFields()[Population_Remote_MetaPopulation.FIELDS_REMOTE_METAPOP_AWAY_FROM_HOME_BY_LOCATION])[loc];

            for (int ga = 0; ga < byLocation.length; ga++) {
                ArrayList<AbstractIndividualInterface>[] byLocationDemographic = byLocation[ga];
                int numberAtHome = byLocationDemographic[0].size();
                int numberAway = byLocationDemographic[1].size();

                AbstractIndividualInterface[] homeArr = byLocationDemographic[0].toArray(new AbstractIndividualInterface[numberAtHome]);

                float proportionAwayPop = ((float) numberAway) / (numberAtHome + numberAway);
                float proportionAwayData = proportionAwayDataByLoc[ga];

                if (proportionAwayData > proportionAwayPop) {
                    // (Away + numToMove) /  Total = proportionAwayData
                    //  Away /  Total = proportionAwayPop                                        
                    int numToMove = Math.round((proportionAwayData - proportionAwayPop) * (numberAtHome + numberAway));

                    AbstractIndividualInterface[] toMove = util.ArrayUtilsRandomGenerator.randomSelect(homeArr, numToMove, getRNG());

                    for (AbstractIndividualInterface movePerson : toMove) {
                        int dest = determineDestination(movePerson);
                        if (dest != -1) {
                            Factory_AwayDuration awayDur = (Factory_AwayDuration) getFields()[FIELDS_REMOTE_METAPOP_AWAY_FROM_HOME_DURATION_FACTORY];
                            int daysAway = awayDur.numberOfDaysStayAway(movePerson, getRNG());
                            int numCirMob = 0;

                            if (proportionAwayDataByLoc.length > byLocation.length) {
                                // Cir movement                      
                                // Format: {...cir_mobility_len_1, proabability_1...}
                                int numCirMobPt = byLocation.length;
                                float probCirMob = getRNG().nextFloat();
                                while ((numCirMobPt + 1) < proportionAwayDataByLoc.length && probCirMob >= proportionAwayDataByLoc[numCirMobPt + 1]) {
                                    numCirMobPt += 2;
                                }
                                numCirMob = (int) proportionAwayDataByLoc[numCirMobPt];
                                if ((numCirMobPt - 2) >= byLocation.length) {
                                    numCirMob = 1 + (int) proportionAwayDataByLoc[numCirMobPt - 2]
                                            + getRNG().nextInt(numCirMob - (int) proportionAwayDataByLoc[numCirMobPt - 2]);
                                }
                                numCirMob = Math.min(daysAway, numCirMob);
                            }
                            if (numCirMob > 0) {
                                boolean[] prev_destinations = new boolean[home_loc_age_gender_home_or_away_collection.length];

                                prev_destinations[((MoveablePersonInterface) movePerson).getHomeLocation()] = true;
                                prev_destinations[dest] = true; // First destination

                                int[] daysAwayByLoc = new int[numCirMob + 1];
                                Arrays.fill(daysAwayByLoc, daysAway / daysAwayByLoc.length);
                                int rem = daysAway - ((daysAway / daysAwayByLoc.length) * daysAwayByLoc.length);
                                while (rem > 0) {
                                    daysAwayByLoc[getRNG().nextInt(daysAwayByLoc.length)]++;
                                    rem--;
                                }

                                // Format: HashMap(Id, int[]{age_1, location_1 ...})
                                HashMap<Integer, int[]> cir_move_map
                                        = (HashMap<Integer, int[]>) getFields()[FIELDS_REMOTE_METAPOP_CIR_MOBILITY_MAP];

                                int firstDest = dest;

                                int[] cir_ent = new int[daysAwayByLoc.length * 2];
                                int pt = 0;
                                int dayAwayAcc = 0;
                                for (int i = 0; i < daysAwayByLoc.length; i++) {
                                    dest = (i == daysAwayByLoc.length - 1) ? ((MoveablePersonInterface) movePerson).getHomeLocation()
                                            : // The last entry should be home                                     
                                            determineDestination(movePerson, prev_destinations);
                                    if (dest != -1) {
                                        dayAwayAcc += daysAwayByLoc[i];
                                        cir_ent[pt] = (int) (dayAwayAcc + movePerson.getAge());
                                        pt++;
                                        cir_ent[pt] = dest;
                                        pt++;
                                        prev_destinations[dest] = true;
                                    }
                                }
                                if (pt > 0) {
                                    cir_move_map.put(movePerson.getId(), Arrays.copyOf(cir_ent, pt - 2));
                                }
                                dest = firstDest;

                            }
                            movePerson(movePerson, dest, (int) (daysAway + movePerson.getAge()));

                        }
                    }

                }

            }
        }

    }

    protected int modelMaxAge() {
        return 35 * AbstractIndividualInterface.ONE_YEAR_INT;
    }

    protected int determineDestination(AbstractIndividualInterface person) {
        return determineDestination(person, null);
    }

    protected int determineDestination(AbstractIndividualInterface person, boolean[] prev_destinations) {
        int dest = -1;

        HashMap<Integer, int[]> cir_move_map
                = (HashMap<Integer, int[]>) getFields()[FIELDS_REMOTE_METAPOP_CIR_MOBILITY_MAP];

        if (person instanceof MoveablePersonInterface) {
            int homeLoc = ((MoveablePersonInterface) person).getHomeLocation();
            int curLoc = getCurrentLocation(person);
            if (curLoc != homeLoc && !cir_move_map.containsKey(person.getId())) {
                dest = homeLoc;
            } else {
                // Moving away from home 
                int[] connOption = ((int[][]) getFields()[FIELDS_REMOTE_METAPOP_POP_CONNC])[curLoc];
                int[] cumProb = new int[connOption.length];
                int prob = 0;

                for (int i = 0; i < cumProb.length; i++) {
                    if (i != curLoc && connOption[i] > 0
                            && (prev_destinations == null || !prev_destinations[i])) {
                        prob += connOption[i];
                    }
                    cumProb[i] = prob;
                }

                if (prob > 0) {  // More than one destination

                    int pDest = getRNG().nextInt(prob);
                    dest = Arrays.binarySearch(cumProb, pDest);

                    if (dest < 0) {
                        //dest = (-(insertion point) - 1)
                        dest = -(dest + 1);
                    }
                    int curCost = cumProb[dest];

                    while (dest > 0 && curCost == cumProb[dest - 1]) {
                        dest--;
                    }
                }
            }
        }

        return dest;

    }

    protected void updateSexualRelationships(int deltaT) {
        // Update relationship and remove if expired
        SingleRelationship[] relArr;
        relArr = getRelMap()[RELMAP_GLOBAL_SEXUAL].getRelationshipArray();

        if (relArr.length == 0) {
            relArr = getRelMap()[RELMAP_GLOBAL_SEXUAL].edgeSet().toArray(relArr);
        }

        for (SingleRelationship r : relArr) {
            SingleRelationshipTimeStamp rel = (SingleRelationshipTimeStamp) r;
            RelationshipMap relMap = getRelMap()[RELMAP_GLOBAL_SEXUAL];
            AbstractIndividualInterface[] partners = rel.getLinks(getLocalData());
            double expiryTime = rel.incrementTime(deltaT);
            if (expiryTime == 0) { // As indetermined relationship length = -1; 
                removeRelationship(relMap, rel, partners);
            } else {
                performSexAct(rel, partners);
            }
        }
    }

    public Person_Remote_MetaPopulation replacePerson(Person_Remote_MetaPopulation removedPerson, int nextId) {

        removePersonFromPopulation(removedPerson);

        // Dissolved all involved partnership if both partners is age out or
        // if the removedPerson has died
        for (RelationshipMap relMap : getRelMap()) {
            if (relMap.containsVertex(removedPerson.getId())) {
                SingleRelationship[] relArr = new SingleRelationship[0];
                relArr = relMap.edgesOf(removedPerson.getId()).toArray(relArr);

                for (SingleRelationship rel : relArr) {
                    SingleRelationshipTimeStamp r = (SingleRelationshipTimeStamp) rel;
                    AbstractIndividualInterface[] partners = rel.getLinks(getLocalData());

                    if (Double.isInfinite(removedPerson.getAge())
                            || partners[0] == null && partners[1] == null) {
                        removeRelationship(relMap, r, partners);
                    }
                }
                relMap.removeVertex(removedPerson);
            }
        }
        Person_Remote_MetaPopulation newPerson = new Person_Remote_MetaPopulation(nextId,
                removedPerson.isMale(),
                getGlobalTime(),
                AbstractIndividualInterface.ONE_YEAR_INT * 16,
                AbstractIndividualInterface.ONE_YEAR_INT * 16,
                getInfList().length,
                (int) removedPerson.getFields()[Person_Remote_MetaPopulation.PERSON_HOME_LOC]);

        PersonClassifier[] introInf = (PersonClassifier[]) getFields()[FIELDS_REMOTE_METAPOP_NEWPERSON_INFECTION_CLASSIFIER];
        float[][] introPreval = (float[][]) getFields()[FIELDS_REMOTE_METAPOP_NEWPERSON_INFECTION_PREVAL];

        if (introInf != null && introPreval != null) {
            for (int infId = 0; infId < introInf.length; infId++) {
                if (introInf[infId] != null) {
                    int cI = introInf[infId].classifyPerson(newPerson);
                    if (cI >= 0) {
                        float preval = introPreval[infId][cI];
                        if (getRNG().nextFloat() < preval) {
                            getInfList()[infId].infecting(newPerson);
                        }
                    }
                }
            }

        }

        return newPerson;
    }

    @Override
    protected SingleRelationship formRelationship(AbstractIndividualInterface[] pair,
            RelationshipMap relMap, int duration, int mapType) {

        SingleRelationshipTimeStamp rel;
        RelationshipMapTimeStamp rMap = (RelationshipMapTimeStamp) relMap;

        for (AbstractIndividualInterface person : pair) {
            if (!rMap.containsVertex(person.getId())) {
                rMap.addVertex(person.getId());
            }
        }

        rel = new SingleRelationshipTimeStamp(new Integer[]{pair[0].getId(), pair[1].getId()});

        if (!rMap.addEdge(pair[0].getId(), pair[1].getId(), rel)) {
            return null;
        }

        rel.setRelStartTime(getGlobalTime());

        // Forced duration if needed
        if (duration > 0) {
            rel.setDurations(duration);
        }

        for (int p = 0; p < pair.length; p++) {
            Person_Remote_MetaPopulation person = (Person_Remote_MetaPopulation) pair[p];
            person.addPartnerAtAge((int) person.getAge(), pair[(p + 1) % pair.length].getId(), (int) rel.getDurations());
        }

        return rel;

    }

    protected void removeRelationship(RelationshipMap relMap,
            SingleRelationshipTimeStamp toBeRemoved,
            AbstractIndividualInterface[] partners) {
        int duration = getGlobalTime() - toBeRemoved.getRelStartTime();

        for (AbstractIndividualInterface p : partners) {
            Person_Remote_MetaPopulation person = (Person_Remote_MetaPopulation) p;
            person.getPartnerHistoryRelLength()[person.getPartnerHistoryLifetimePt() - 1] = duration;
        }

        relMap.removeEdge(toBeRemoved);

    }

    protected boolean[][] performSexAct(SingleRelationship rel, AbstractIndividualInterface[] partners) {
        boolean[][] res = null;
        if (getCurrentLocation(partners[0]) == getCurrentLocation(partners[1])) { // Only at same location
            int loc = getCurrentLocation(partners[0]);
            float[] condomUsageAll = (float[]) getFields()[FIELDS_REMOTE_METAPOP_CONDOM_USAGE_BY_LOCATION];
            float[] freqSexAll = (float[]) getFields()[FIELDS_REMOTE_METAPOP_FREQ_OF_SEX_BY_LOCATION];

            if (getRNG().nextFloat() < freqSexAll[loc < freqSexAll.length ? loc : 0]) {

                if (!(getRNG().nextFloat() < condomUsageAll[loc < condomUsageAll.length ? loc : 0])) {
                    res = SingleRelationship.performAct(partners, getGlobalTime(), getInfList(), new boolean[]{true, true});
                }
                for (AbstractIndividualInterface p : partners) {
                    Person_Remote_MetaPopulation person = (Person_Remote_MetaPopulation) p;
                    if (person.getParameter(Person_Remote_MetaPopulation.PERSON_FIRST_SEX_AGE) < 0) {
                        person.setParameter(Person_Remote_MetaPopulation.PERSON_FIRST_SEX_AGE, (long) person.getAge());
                    }
                }
            }

        }

        return res;
    }

    // Include return home and circular movement
    public int predefinedMove() {
        HashMap<Integer, Integer> return_home_map
                = (HashMap<Integer, Integer>) getFields()[FIELDS_META_POP_AWAY_UNTIL_AGE];
        int numberReturnHome = 0;

        // Format: HashMap(Id, int[]{age_1, location_1 ...})
        HashMap<Integer, int[]> cir_move_map
                = (HashMap<Integer, int[]>) getFields()[FIELDS_REMOTE_METAPOP_CIR_MOBILITY_MAP];
        int numberCirMove = 0;

        AbstractIndividualInterface person;
        AbstractIndividualInterface[] toReturnHome = new AbstractIndividualInterface[return_home_map.keySet().size()];

        AbstractIndividualInterface[] toCirMove = new AbstractIndividualInterface[cir_move_map.keySet().size()];
        int[] toCirMoveTarget = new int[cir_move_map.keySet().size()];
        int[] toCirMoveUntilAge = new int[cir_move_map.keySet().size()];
        Integer[] toCirMoveKeys = new Integer[cir_move_map.keySet().size()];
        cir_move_map.keySet().toArray(toCirMoveKeys);

        for (int pid : return_home_map.keySet()) {
            person = getLocalData().get(pid);
            int ageToReturnHome = isAwayFromHomeUntilAge(person);
            if (person.getAge() >= ageToReturnHome) {
                toReturnHome[numberReturnHome] = person;
                numberReturnHome++;
            }
        }

        for (int i = 0; i < numberReturnHome; i++) {
            person = toReturnHome[i];
            movePerson(person, ((MoveablePersonInterface) person).getHomeLocation(), -1);
        }

        for (int pid : toCirMoveKeys) {
            int[] moveTarget = cir_move_map.get(pid);
            person = getLocalData().get(pid);
            int ageIndex = 0;
            if (person == null) { // Age out / removed person 
                cir_move_map.remove(pid);
            } else {
                while (ageIndex < moveTarget.length && moveTarget[ageIndex] < person.getAge()) {
                    ageIndex += 2;
                }
                moveTarget = Arrays.copyOfRange(moveTarget, ageIndex, moveTarget.length);

                if (moveTarget.length >= 2) {
                    cir_move_map.put(pid, moveTarget);
                    if (person.getAge() == moveTarget[0]) {
                        toCirMove[numberCirMove] = person;
                        toCirMoveTarget[numberCirMove] = moveTarget[1];

                        if (moveTarget.length > 2) {
                            toCirMoveUntilAge[numberCirMove] = moveTarget[2];
                        } else if (return_home_map.containsKey(pid)) {
                            toCirMoveUntilAge[numberCirMove] = return_home_map.get(pid);
                        } else {
                            toCirMoveUntilAge[numberCirMove] = Integer.MAX_VALUE;
                        }
                        numberCirMove++;
                    }
                } else {
                    cir_move_map.remove(pid);
                }
            }
        }
        for (int i = 0; i < numberCirMove; i++) {
            movePerson(toCirMove[i], toCirMoveTarget[i], toCirMoveUntilAge[i]);
        }

        return numberReturnHome;

    }

}
