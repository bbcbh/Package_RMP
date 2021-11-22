package population;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;

import person.AbstractIndividualInterface;
import relationship.RelationshipMap;
import relationship.SingleRelationship;

/**
 * <p>
 * An extension of COVID_19 model with age structure</p>
 *
 * @author Ben Hui
 *
 */
public class Population_Remote_MetaPopulation_COVID19_AS
        extends Population_Remote_MetaPopulation_COVID19 {

    /**
	 * 
	 */
	private static final long serialVersionUID = 744424706512765730L;
	// From 2076.0 Census of Population and Housing: Characteristics of Aboriginal and Torres Strait Islander Australians, 2011
    // 20760_2011.xls    
    public static final int HOUSEHOLD_SPREAD_COUPLE_WITH_CHILDREN = 0;
    public static final int HOUSEHOLD_SPREAD_SINGLE_WITH_CHILDREN = 1;
    public static final int HOUSEHOLD_SPREAD_SINGLE_FAMILY_WITHOUT_CHILDREN = 2;
    public static final int HOUSEHOLD_SPREAD_MULTI_FAMILY = 3;
    public static final int HOUSEHOLD_SPREAD_LONE = 4;
    public static final int HOUSEHOLD_SPREAD_GROUP_HOUSEHOLD = 4;

    float[] household_spread = {
        // Couple family with dependent children
        26.6f,
        // One-parent family with dependent children
        21.4f,
        // Families without dependent children
        27.1f,
        // Multiple family households
        5.5f,
        // Lone person households
        14.1f,
        // Group households
        5.2f
    };

    public static final int[] AGE_GROUPING = {15 * AbstractIndividualInterface.ONE_YEAR_INT,
        65 * AbstractIndividualInterface.ONE_YEAR_INT};

    public Population_Remote_MetaPopulation_COVID19_AS(long seed) {
        super(seed);
    }

    public float[] getHousehold_spread() {
        return household_spread;
    }

    public void setHousehold_spread(float[] household_spread) {
        this.household_spread = household_spread;
    }

    public static boolean isDependent(double age) {
        return age < AGE_GROUPING[0] || age > AGE_GROUPING[1];
    }

    @Override
    public void allolocateCoreHosuehold(float[][] householdSizeDistByLoc, float[][] householdSpreadByLoc, float[][] nonHouseholdContactRateByLoc) {
        super.allolocateCoreHosuehold(householdSizeDistByLoc, householdSpreadByLoc, nonHouseholdContactRateByLoc);

        // Calculate age spread
        //System.out.println("===== Initial =====");
        //printHouseholdAgeStructure();
        // Household
        RelationshipMap householdMap = getRelMap()[RELMAP_HOUSEHOLD];
        Integer[][] household_indices = ((Integer[][]) getFields()[FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD]);

        HashMap<Integer, int[]> household_map_all_loc = new HashMap<>();  // K = household id, V = [num_adult, num_dependent];

        @SuppressWarnings("unchecked")
		LinkedList<Integer>[][] indivdual_map_by_loc = new LinkedList[household_indices.length][LENGTH_INDIV_MAP];
        for (LinkedList<Integer>[] indivudual_map_at_loc : indivdual_map_by_loc) {
            for (int type = 0; type < indivudual_map_at_loc.length; type++) {
                indivudual_map_at_loc[type] = new LinkedList<>();
            }
        }

        LinkedList<Integer> lonerToBeAdded = new LinkedList<>();
        LinkedList<SingleRelationship> edgesToRemove = new LinkedList<>();

        ArrayList<Integer> splitable_households = new ArrayList<>(); // i.e. with at least one adult 

        int numHousehold = 0;
        int lastHouseholdNum = 0;
        int numLonerAlreadyInPop = 0;

        for (AbstractIndividualInterface person : this.getPop()) {
            int loc = ((person.MoveablePersonInterface) person).getHomeLocation();
            LinkedList<Integer> indiv_list;
            int[] val;

            if (!householdMap.containsVertex(person.getId())) {
                householdMap.addVertex(person.getId());
            }
            int deg = householdMap.degreeOf(person.getId());
            if (deg == 0) { // No household - possibly error 
                System.err.println("allolocateCoreHosuehold: Indivdual with no core household - to be added as loner.");
                lonerToBeAdded.add(person.getId());

                int listPt = isDependent(person.getAge()) ? INDIV_MAP_DEPENDENT_ALONE : INDIV_MAP_ADULT_LONER;

                indiv_list = indivdual_map_by_loc[loc][listPt];
                if (indiv_list == null) {
                    indivdual_map_by_loc[loc][listPt] = new LinkedList<>();
                    indiv_list = indivdual_map_by_loc[loc][listPt];
                }
                indiv_list.add(person.getId());

            } else {
                Iterator<SingleRelationship> edgeIt;
                edgeIt = householdMap.edgesOf(person.getId()).iterator();
                SingleRelationship edge = edgeIt.next();

                if (deg > 1) { // Mulitple core household - possibly error
                    System.err.println("allolocateCoreHosuehold: Multiple core household allocated previously. Removing extra edges.");
                    while (edgeIt.hasNext()) {
                        edgesToRemove.add(edgeIt.next());
                    }
                    for (SingleRelationship edgeToRemove : edgesToRemove) {
                        householdMap.removeEdge(edgeToRemove);
                    }
                    edgesToRemove.clear();
                }

                // Household id               
                val = edge.getLinksValues();
                int houseId = val[0] == person.getId() ? val[1] : val[0];

                int[] houseStat = household_map_all_loc.get(houseId);
                if (houseStat == null) {
                    // New household in map                                
                    houseStat = getHouseholdStat(householdMap, houseId);
                    household_map_all_loc.put(houseId, houseStat);
                    numHousehold++;
                    lastHouseholdNum = Math.min(lastHouseholdNum, houseId);
                    if (houseStat[HOUSEHOLD_MAP_NUM_ADULT] > 1) {
                        splitable_households.add(houseId);
                    }

                }

                int listPt = -1;
                if (isDependent(person.getAge())) {
                    if (houseStat[HOUSEHOLD_MAP_NUM_ADULT] == 0) {
                        listPt = INDIV_MAP_DEPENDENT_ALONE;
                        if (houseStat[HOUSEHOLD_MAP_NUM_DEPENDENT] == 1) {
                            numLonerAlreadyInPop++;
                        }
                    } else {
                        if (houseStat[HOUSEHOLD_MAP_NUM_DEPENDENT] == 1) {
                            listPt = INDIV_MAP_DEPENDENT_SINGLE_WITH_ADULT;
                        } else {
                            listPt = INDIV_MAP_DEPENDENT_MULTI_WITH_ADULT;
                        }
                    }
                } else {
                    if (houseStat[HOUSEHOLD_MAP_NUM_ADULT] == 1) {
                        if (houseStat[HOUSEHOLD_MAP_NUM_DEPENDENT] == 0) {
                            listPt = INDIV_MAP_ADULT_LONER;
                            numLonerAlreadyInPop++;
                        } else {
                            listPt = INDIV_MAP_ADULT_SINGLE_WITH_DEPENDENT;
                        }
                    } else {
                        if (houseStat[HOUSEHOLD_MAP_NUM_DEPENDENT] == 0) {
                            listPt = INDIV_MAP_ADULT_NO_DEPENDENT;
                        } else {
                            listPt = INDIV_MAP_ADULT_MULTI_PARENT;
                        }
                    }
                }
                indiv_list = indivdual_map_by_loc[loc][listPt];

                indiv_list.add(person.getId());
            }
        }

        // Determine how many lone person household need to be added
        int numLonerToBeAdded = Math.round(
                numHousehold * household_spread[HOUSEHOLD_SPREAD_LONE] / 100f - numLonerAlreadyInPop - lonerToBeAdded.size());

        while (numLonerToBeAdded > 0 && splitable_households.size() > 0) {
            int split_household_index = getRNG().nextInt(splitable_households.size());
            int split_household_id = splitable_households.get(split_household_index);

            int split_adult_id = -1;
            Iterator<SingleRelationship> edgeMemeberIt = householdMap.edgesOf(split_household_id).iterator();
            SingleRelationship rel = null;

            while (split_adult_id < 0 && edgeMemeberIt.hasNext()) {
                rel = edgeMemeberIt.next();
                int[] val = rel.getLinksValues();
                split_adult_id = val[0] == split_household_id ? val[1] : val[0];
                if (isDependent(this.getPersonById(split_adult_id).getAge())) {
                    split_adult_id = -1;
                }
            }
            if (split_adult_id >= 0) {
                numHousehold++;
                lonerToBeAdded.add(split_adult_id);
                edgesToRemove.add(rel);
                householdMap.removeEdge(rel);
                int loc = ((person.MoveablePersonInterface) this.getPersonById(split_adult_id)).getHomeLocation();

                indivdual_map_by_loc[loc][INDIV_MAP_ADULT_LONER].add(split_adult_id);
                boolean removeSuc;

                if (household_map_all_loc.get(split_household_id)[HOUSEHOLD_MAP_NUM_DEPENDENT] == 0) {
                    removeSuc = indivdual_map_by_loc[loc][INDIV_MAP_ADULT_NO_DEPENDENT].remove((Integer) split_adult_id);
                } else {
                    removeSuc = indivdual_map_by_loc[loc][INDIV_MAP_ADULT_MULTI_PARENT].remove((Integer) split_adult_id);
                }

                if (!removeSuc) {
                    System.err.println("allolocateCoreHosuehold: Check indivudual_map_by_loc for new loner");
                }

                // Update other members of household
                household_map_all_loc.get(split_household_id)[HOUSEHOLD_MAP_NUM_ADULT]--;
                if (household_map_all_loc.get(split_household_id)[HOUSEHOLD_MAP_NUM_ADULT] < 2) {

                    splitable_households.remove(split_household_index);
                    for (SingleRelationship allHousehold : householdMap.edgesOf(split_household_id)) {
                        int[] v = allHousehold.getLinksValues();
                        int memId = v[0] == split_household_id ? v[1] : v[0];
                        if (!(isDependent(this.getPersonById(memId).getAge()))) {
                            if (household_map_all_loc.get(split_household_id)[HOUSEHOLD_MAP_NUM_DEPENDENT] > 0) {
                                removeSuc = indivdual_map_by_loc[loc][INDIV_MAP_ADULT_MULTI_PARENT].remove((Integer) memId);
                                indivdual_map_by_loc[loc][INDIV_MAP_ADULT_SINGLE_WITH_DEPENDENT].add(memId);
                            } else {
                                removeSuc = indivdual_map_by_loc[loc][INDIV_MAP_ADULT_NO_DEPENDENT].remove((Integer) memId);
                                indivdual_map_by_loc[loc][INDIV_MAP_ADULT_LONER].add(memId);
                                numLonerAlreadyInPop++;
                            }
                            if (!removeSuc) {
                                System.err.println("allolocateCoreHosuehold: Check indivudual_map_by_loc for former household member of new loner.");
                            }
                        }
                    }
                }

                numLonerToBeAdded = Math.round(
                        numHousehold * household_spread[HOUSEHOLD_SPREAD_LONE] / 100f
                        - numLonerAlreadyInPop - lonerToBeAdded.size());
            }
        }

        // Set up a new house for loner 
        for (Integer lonerId : lonerToBeAdded) {
            int loc = ((person.MoveablePersonInterface) this.getPersonById(lonerId)).getHomeLocation();
            int newHousehold_index = lastHouseholdNum - 1;
            lastHouseholdNum--;

            // Update RELMAP_HOUSEHOLD                    
            float[] householdSpreadDist = householdSpreadByLoc[loc];
            float[] nonHouseholdContactRateDist = nonHouseholdContactRateByLoc[loc];
            householdMap.addVertex(newHousehold_index);
            householdMap.addVertex(lonerId); // As the previous id is already remove by the remove edge process

            setContactOptionCore(this.getPersonById(lonerId), newHousehold_index,
                    householdSpreadDist, nonHouseholdContactRateDist);

            // Update FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD
            household_indices[loc] = Arrays.copyOf(household_indices[loc], household_indices[loc].length + 1);
            household_indices[loc][household_indices[loc].length - 1] = newHousehold_index;

            // Update household_map_all_loc
            int[] val = new int[]{1, 0};
            household_map_all_loc.put(newHousehold_index, val);
        }

        //System.out.println("===== With loner household =====");
        //printHouseholdAgeStructure();
        // Swap indivduals to reduce number of household with dependent living alone
        for (int loc = 0; loc < household_indices.length; loc++) {

            float[] householdSpreadDist = householdSpreadByLoc[loc];
            float[] nonHouseholdContactRateDist = nonHouseholdContactRateByLoc[loc];

            LinkedList<Integer> list_dependent_alone = indivdual_map_by_loc[loc][INDIV_MAP_DEPENDENT_ALONE];
            LinkedList<Integer> list_multi_adult = indivdual_map_by_loc[loc][INDIV_MAP_ADULT_MULTI_PARENT];
            LinkedList<Integer> list_no_dependent = indivdual_map_by_loc[loc][INDIV_MAP_ADULT_NO_DEPENDENT];
            LinkedList<Integer> list_single_adult_with_dependent = indivdual_map_by_loc[loc][INDIV_MAP_ADULT_SINGLE_WITH_DEPENDENT];
            LinkedList<Integer> list_dependent_with_adults = indivdual_map_by_loc[loc][INDIV_MAP_DEPENDENT_MULTI_WITH_ADULT];
            LinkedList<Integer> list_single_dependent_with_adult = indivdual_map_by_loc[loc][INDIV_MAP_DEPENDENT_SINGLE_WITH_ADULT];

            Integer[] dependent_alone_all = list_dependent_alone.toArray(new Integer[list_dependent_alone.size()]);

            for (Integer dependent_alone : dependent_alone_all) {

                SingleRelationship orgDependConnect = householdMap.edgesOf(dependent_alone).iterator().next();
                int[] valDep = orgDependConnect.getLinksValues();
                int orgDependhouseId = valDep[0] == dependent_alone ? valDep[1] : valDep[0];

                // Check if they need to swap with an adult
                if (household_map_all_loc.get(orgDependhouseId)[HOUSEHOLD_MAP_NUM_ADULT] == 0
                        && list_multi_adult.size() + list_no_dependent.size() > 0) {
                    int randomPick = getRNG().nextInt(list_multi_adult.size() + list_no_dependent.size());
                    LinkedList<Integer> picked_adult_list = null;
                    boolean fromNoDepList = false;
                    if (randomPick < list_multi_adult.size()) {
                        picked_adult_list = list_multi_adult;
                    } else {
                        fromNoDepList = true;
                        randomPick -= list_multi_adult.size();
                        picked_adult_list = list_no_dependent;
                    }

                    Integer replacement_adult = picked_adult_list.remove(randomPick);
                    list_single_adult_with_dependent.add(replacement_adult);

                    SingleRelationship orgAdultConnect = householdMap.edgesOf(replacement_adult).iterator().next();
                    int[] valAdult = orgAdultConnect.getLinksValues();
                    int orgAdultHouseId = valAdult[0] == replacement_adult ? valAdult[1] : valAdult[0];

                    switchHousehold(householdMap, householdSpreadDist, nonHouseholdContactRateDist,
                            dependent_alone, orgAdultHouseId, orgDependConnect);

                    switchHousehold(householdMap, householdSpreadDist, nonHouseholdContactRateDist,
                            replacement_adult, orgDependhouseId, orgAdultConnect);

                    // Adjust house stat
                    household_map_all_loc.get(orgAdultHouseId)[HOUSEHOLD_MAP_NUM_ADULT]--;
                    household_map_all_loc.get(orgAdultHouseId)[HOUSEHOLD_MAP_NUM_DEPENDENT]++;

                    if (household_map_all_loc.get(orgAdultHouseId)[HOUSEHOLD_MAP_NUM_ADULT] == 1) {
                        for (SingleRelationship allHousehold : householdMap.edgesOf(orgAdultHouseId)) {
                            int[] v = allHousehold.getLinksValues();
                            int memId = v[0] == orgAdultHouseId ? v[1] : v[0];
                            if (fromNoDepList) {
                                list_no_dependent.remove((Integer) memId);
                                list_single_adult_with_dependent.add(memId);
                            }
                        }
                    }

                    household_map_all_loc.get(orgDependhouseId)[HOUSEHOLD_MAP_NUM_ADULT]++;
                    household_map_all_loc.get(orgDependhouseId)[HOUSEHOLD_MAP_NUM_DEPENDENT]--;

                    // Update dependent
                    if (household_map_all_loc.get(orgAdultHouseId)[HOUSEHOLD_MAP_NUM_DEPENDENT] == 1) {
                        list_single_dependent_with_adult.add(list_dependent_alone.removeFirst());
                    } else {
                        list_dependent_with_adults.add(list_dependent_alone.removeFirst());
                    }

                } else if (household_map_all_loc.get(orgDependhouseId)[HOUSEHOLD_MAP_NUM_ADULT] > 0) {
                    // Update dependent if no longer dependent alone (e.g. other family member already replace by an adult                    
                    if (household_map_all_loc.get(orgDependhouseId)[HOUSEHOLD_MAP_NUM_DEPENDENT] == 1) {
                        list_single_dependent_with_adult.add(list_dependent_alone.removeFirst());
                    } else {
                        list_dependent_with_adults.add(list_dependent_alone.removeFirst());

                    }

                }
            }
        }

        //System.out.println("===== With loner household and every house with adult =====");
        //printHouseholdAgeStructure();
        
        // Set single parent family
        enforceMinAdultHousehold(1, HOUSEHOLD_SPREAD_SINGLE_WITH_CHILDREN,
                household_indices,
                household_map_all_loc,
                numHousehold,
                householdMap,
                householdSpreadByLoc,
                nonHouseholdContactRateByLoc);

        System.out.println("===== With loner household, every house with adult and single parent =====");
        printHouseholdAgeStructure();
        
        // Set two parents family
        
        enforceMinAdultHousehold(2, HOUSEHOLD_SPREAD_COUPLE_WITH_CHILDREN,
                household_indices,
                household_map_all_loc,
                numHousehold,
                householdMap,
                householdSpreadByLoc,
                nonHouseholdContactRateByLoc);

        System.out.println("===== With loner household, every house with adult, and 1-2 parents =====");
        printHouseholdAgeStructure();
        

        // Check household map 
        if (!checkHouseholdMap(householdMap, household_map_all_loc)) {
            System.out.println("Error in household map");
            System.exit(1);
        }

        System.out.println("===== Final =====");
        printHouseholdAgeStructure();

        //Work in progress
        System.exit(0);

    }

    protected void enforceMinAdultHousehold(int minAdult, int spreadIndex,
            Integer[][] household_indices,
            HashMap<Integer, int[]> household_map_all_loc,
            int numHousehold,
            RelationshipMap householdMap,
            float[][] householdSpreadByLoc,
            float[][] nonHouseholdContactRateByLoc) {

        @SuppressWarnings("unchecked")
		LinkedList<Integer>[] household_multi_dependent_multi_adult = new LinkedList[household_indices.length];
        int numFamilyWithMinAdultWithDependent = 0;
        int numFamilyMoreMinAdultWithDependent = 0;

        for (int loc = 0; loc < household_indices.length; loc++) {
            household_multi_dependent_multi_adult[loc] = new LinkedList<>();

            for (Integer houseId : household_indices[loc]) {
                int[] houseStat = household_map_all_loc.get(houseId);
                if (houseStat[HOUSEHOLD_MAP_NUM_ADULT] == minAdult
                        && houseStat[HOUSEHOLD_MAP_NUM_DEPENDENT] > 0) {
                    numFamilyWithMinAdultWithDependent++;
                }

                if (houseStat[HOUSEHOLD_MAP_NUM_ADULT] > minAdult) {
                    if (houseStat[HOUSEHOLD_MAP_NUM_DEPENDENT] > 1) {
                        household_multi_dependent_multi_adult[loc].add(houseId);
                        numFamilyMoreMinAdultWithDependent++;
                    }
                }
            }
        }

        int numSingleWithDependentToBeAdded = Math.round(
                numHousehold * household_spread[spreadIndex] / 100f - numFamilyWithMinAdultWithDependent);

        while (numSingleWithDependentToBeAdded > 0 && numFamilyMoreMinAdultWithDependent > 0) {

            // Randomly choose a random family with multiple adults, and delicate it to a single parent family
            int singleParentHouseIndex = getRNG().nextInt(numFamilyMoreMinAdultWithDependent);
            int loc = 0;
            while (singleParentHouseIndex >= household_multi_dependent_multi_adult[loc].size()) {
                loc++;
                singleParentHouseIndex = singleParentHouseIndex - household_multi_dependent_multi_adult[loc].size();
            }

            Integer minAdultHouseNum = household_multi_dependent_multi_adult[loc].get(singleParentHouseIndex);
            int[] houseStatMinAdult = household_map_all_loc.get(minAdultHouseNum);                                    
            
            int[] keepAdultAt =  new int[minAdult];
            int keepAdultIndex = 0;
            
            for(int adultIndex = 0; 
                    adultIndex < houseStatMinAdult[HOUSEHOLD_MAP_NUM_ADULT] && keepAdultIndex < keepAdultAt.length;
                    adultIndex++){
                if(getRNG().nextInt(houseStatMinAdult[HOUSEHOLD_MAP_NUM_ADULT] - adultIndex) <  minAdult -keepAdultIndex){
                    keepAdultAt[keepAdultIndex] = adultIndex;
                    keepAdultIndex++;
                }
                
            }                                    

            // Remove it from list - either becasuse it is a single parent or 
            // no more suitable family to swap.
            household_multi_dependent_multi_adult[loc].remove(singleParentHouseIndex);
            numFamilyMoreMinAdultWithDependent--;

            SingleRelationship[] toReplaceWithDependent = new SingleRelationship[houseStatMinAdult[HOUSEHOLD_MAP_NUM_ADULT] 
                    - minAdult];
            int replacePt = 0;
            int adult_rel_pt = 0;

            for (SingleRelationship rel : householdMap.edgesOf(minAdultHouseNum)) {
                int p_id = rel.getLinksValues()[0] == minAdultHouseNum
                        ? rel.getLinksValues()[1] : rel.getLinksValues()[0];

                if (!isDependent(getPersonById(p_id).getAge())) {
                    if (Arrays.binarySearch(keepAdultAt, adult_rel_pt) < 0) {
                        toReplaceWithDependent[replacePt] = rel;
                        replacePt++;
                    }
                    adult_rel_pt++;
                }
            }

            // Replace adult with dependent
            for (int r = 0; r < toReplaceWithDependent.length
                    && household_multi_dependent_multi_adult[loc].size() > 0; r++) {

                SingleRelationship org_adult_connection = toReplaceWithDependent[r];
                Integer candidate_adult_Id = org_adult_connection.getLinksValues()[0] == minAdultHouseNum
                        ? org_adult_connection.getLinksValues()[1] : org_adult_connection.getLinksValues()[0];

                Integer replaceHouseNum = minAdultHouseNum;
                int replaceHouseIndex = -1;

                while (Objects.equals(replaceHouseNum, minAdultHouseNum)) {
                    replaceHouseIndex = getRNG().nextInt(household_multi_dependent_multi_adult[loc].size());
                    replaceHouseNum = household_multi_dependent_multi_adult[loc].get(replaceHouseIndex);
                }
                if (!Objects.equals(replaceHouseNum, minAdultHouseNum)) {
                    int[] houseStatReplace = household_map_all_loc.get(replaceHouseNum);
                    int replaceDependentAt = getRNG().nextInt(houseStatReplace[HOUSEHOLD_MAP_NUM_DEPENDENT]);

                    SingleRelationship org_dep_connection = null;
                    int candidate_dep_Id = -1;

                    for (SingleRelationship test_dep_connection : householdMap.edgesOf(replaceHouseNum)) {
                        int test_candidate_dep_Id = test_dep_connection.getLinksValues()[0] == replaceHouseNum
                                ? test_dep_connection.getLinksValues()[1] : test_dep_connection.getLinksValues()[0];

                        if (isDependent(getPersonById(test_candidate_dep_Id).getAge())) {
                            if (replaceDependentAt == 0) {
                                org_dep_connection = test_dep_connection;
                                candidate_dep_Id = test_candidate_dep_Id;

                            }
                            replaceDependentAt--;
                        }
                    }

                    float[] householdSpreadDist = householdSpreadByLoc[loc];
                    float[] nonHouseholdContactRateDist = nonHouseholdContactRateByLoc[loc];

                    // Move adult to dependent                   
                    switchHousehold(householdMap, householdSpreadDist, nonHouseholdContactRateDist,
                            candidate_adult_Id, replaceHouseNum, org_adult_connection);
                    houseStatReplace[HOUSEHOLD_MAP_NUM_ADULT]++;
                    houseStatMinAdult[HOUSEHOLD_MAP_NUM_ADULT]--;

                    // Move dependent to adult       
                    switchHousehold(householdMap, householdSpreadDist, nonHouseholdContactRateDist,
                            candidate_dep_Id, minAdultHouseNum, org_dep_connection);
                    houseStatReplace[HOUSEHOLD_MAP_NUM_DEPENDENT]--;
                    houseStatMinAdult[HOUSEHOLD_MAP_NUM_DEPENDENT]++;

                    if (houseStatMinAdult[HOUSEHOLD_MAP_NUM_ADULT] == 1) {
                        numSingleWithDependentToBeAdded--;
                        numFamilyWithMinAdultWithDependent++;
                    }
                    if (houseStatReplace[HOUSEHOLD_MAP_NUM_DEPENDENT] <= 1) {
                        household_multi_dependent_multi_adult[loc].remove(replaceHouseIndex);
                        numFamilyMoreMinAdultWithDependent--;
                    }
                }
            }
            //System.out.println("===== Force min parent attempt =====");
            //printHouseholdAgeStructure();
        }
    }

    public void printHouseholdAgeStructure() {

        Integer[][] household_indices = ((Integer[][]) getFields()[FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD]);
        RelationshipMap householdMap = getRelMap()[RELMAP_HOUSEHOLD];

        int numHousehold;
        HashMap<Integer, ArrayList<Integer>> households_by_adults = new HashMap<>();
        HashMap<Integer, ArrayList<Integer>> households_by_dependent = new HashMap<>();
        HashMap<Integer, ArrayList<Integer>> households_with_dependent = new HashMap<>();
        HashMap<Integer, ArrayList<Integer>> household_size_map_all_loc = new HashMap<>();  // K = size of household, V = {household_id};
        numHousehold = 0;
        household_size_map_all_loc.clear();
        households_by_adults.clear();
        households_by_dependent.clear();
        households_with_dependent.clear();
        for (int loc = 0; loc < household_indices.length; loc++) {
            numHousehold += household_indices[loc].length;
            for (Integer houseId : household_indices[loc]) {
                ArrayList<Integer> ent;
                int numInHouse = householdMap.degreeOf(houseId);
                ent = household_size_map_all_loc.get(numInHouse);
                if (ent == null) {
                    ent = new ArrayList<>();
                    household_size_map_all_loc.put(numInHouse, ent);
                }
                ent.add(houseId);
                int numAdults = 0;
                int numDependent = 0;

                int[] stat = getHouseholdStat(householdMap, houseId);
                numAdults = stat[HOUSEHOLD_MAP_NUM_ADULT];
                numDependent = stat[HOUSEHOLD_MAP_NUM_DEPENDENT];
                /*
                for (SingleRelationship rel : householdMap.edgesOf(houseId)) {
                    int[] val = rel.getLinksValues();
                    int personId = val[0] == houseId ? val[1] : val[0];
                    if (!Population_Remote_MetaPopulation_COVID19_AS.isDependent(
                            this.getPersonById(personId).getAge())) {
                        numAdults++;
                    } else {
                        numDependent++;
                    }
                }
                 */

                // Adult household
                ent = households_by_adults.get(numAdults);
                if (ent == null) {
                    ent = new ArrayList<>();
                    households_by_adults.put(numAdults, ent);
                }
                ent.add(houseId);

                // Children household
                ent = households_by_dependent.get(numDependent);
                if (ent == null) {
                    ent = new ArrayList<>();
                    households_by_dependent.put(numDependent, ent);
                }
                ent.add(houseId);

                if (numDependent > 0) {
                    ent = households_with_dependent.get(numAdults);
                    if (ent == null) {
                        ent = new ArrayList<>();
                        households_with_dependent.put(numAdults, ent);
                    }
                    ent.add(houseId);

                }

            }
        }

        System.out.println("For entire population:");
        System.out.println(String.format("# household: %d", numHousehold));
        Integer[] householdSizeMapping = household_size_map_all_loc.keySet().toArray(new Integer[household_size_map_all_loc.size()]);
        Arrays.sort(householdSizeMapping);
        for (Integer hSM : householdSizeMapping) {
            int numHouse = household_size_map_all_loc.get(hSM).size();
            System.out.println(String.format("Household size of %d: %d (%.1f%% of household)", hSM, numHouse, 100f * numHouse / numHousehold));
        }
        System.out.println();
        Integer[] mapping;
        System.out.println("Number of adults in household");
        mapping = households_by_adults.keySet().toArray(new Integer[0]);
        Arrays.sort(mapping);
        for (Integer hId : mapping) {
            int numHouse = households_by_adults.get(hId).size();
            System.out.println(String.format("Household with %d adult(s): %d (%.1f%% of household)",
                    hId, numHouse, 100f * numHouse / numHousehold));
        }
        System.out.println();
        System.out.println("Number of dependents in household");
        mapping = households_by_dependent.keySet().toArray(new Integer[0]);
        Arrays.sort(mapping);
        for (Integer hId : mapping) {
            int numHouse = households_by_dependent.get(hId).size();
            System.out.println(String.format("Household with %d dependent(s): %d (%.1f%% of household)",
                    hId, numHouse, 100f * numHouse / numHousehold));
        }
        System.out.println();
        System.out.println("Number of adults in household with dependent(s)");
        mapping = households_with_dependent.keySet().toArray(new Integer[0]);
        Arrays.sort(mapping);
        for (Integer hId : mapping) {
            int numHouse = households_with_dependent.get(hId).size();
            System.out.println(String.format("Household with dependent(s) and %d adult(s): %d (%.1f%% of household)",
                    hId, numHouse, 100f * numHouse / numHousehold));
        }
        System.out.println();
    }

    protected void switchHousehold(RelationshipMap householdMap,
            float[] householdSpreadDist, float[] nonHouseholdContactRateDist,
            Integer candidate, int newHouseId,
            SingleRelationship org_connection) {

        setContactOptionCore(this.getPersonById(candidate), newHouseId,
                householdSpreadDist, nonHouseholdContactRateDist);
        householdMap.removeEdge(org_connection);

    }

    private boolean checkHouseholdMap(RelationshipMap householdMap,
            HashMap<Integer, int[]> household_map_all_loc) {

        boolean match = true;

        for (Integer houseId : household_map_all_loc.keySet()) {

            int[] stat = getHouseholdStat(householdMap, houseId);
            int[] ent = household_map_all_loc.get(houseId);

            if (stat[0] != ent[0] || stat[1] != ent[1]) {
                match = false;
                System.out.println(String.format("Household stat inconsistency at Household #%d", houseId));
                System.out.println("Household map = " + Arrays.toString(stat));
                System.out.println("Stat entry = " + Arrays.toString(ent));
            }

        }

        return match;

    }

    final int HOUSEHOLD_MAP_NUM_ADULT = 0;
    final int HOUSEHOLD_MAP_NUM_DEPENDENT = HOUSEHOLD_MAP_NUM_ADULT + 1;
    final int LENGTH_HOUSEHOLD_MAP = HOUSEHOLD_MAP_NUM_DEPENDENT + 1;

    protected int[] getHouseholdStat(RelationshipMap householdMap, Integer houseId) {

        int[] stat = new int[LENGTH_HOUSEHOLD_MAP];
        for (SingleRelationship rel : householdMap.edgesOf(houseId)) {
            int personId = rel.getLinks()[0] < 0 ? rel.getLinks()[1] : rel.getLinks()[0];
            if (isDependent(getPersonById(personId).getAge())) {
                stat[HOUSEHOLD_MAP_NUM_DEPENDENT]++;
            } else {
                stat[HOUSEHOLD_MAP_NUM_ADULT]++;
            }
        }
        return stat;
    }

    final int INDIV_MAP_ADULT_LONER = 0;
    final int INDIV_MAP_ADULT_SINGLE_WITH_DEPENDENT = INDIV_MAP_ADULT_LONER + 1;             // Alone in household with multiple dependents
    final int INDIV_MAP_ADULT_MULTI_PARENT = INDIV_MAP_ADULT_SINGLE_WITH_DEPENDENT + 1;      // In household with more than one adults and dependents
    final int INDIV_MAP_ADULT_NO_DEPENDENT = INDIV_MAP_ADULT_MULTI_PARENT + 1;               // In household with no dependent 
    final int INDIV_MAP_DEPENDENT_MULTI_WITH_ADULT = INDIV_MAP_ADULT_NO_DEPENDENT + 1;
    final int INDIV_MAP_DEPENDENT_SINGLE_WITH_ADULT = INDIV_MAP_DEPENDENT_MULTI_WITH_ADULT + 1;
    final int INDIV_MAP_DEPENDENT_ALONE = INDIV_MAP_DEPENDENT_SINGLE_WITH_ADULT + 1;         // Dependent who are living alone - will be 0 by the end
    final int LENGTH_INDIV_MAP = INDIV_MAP_DEPENDENT_ALONE + 1;

    @SuppressWarnings("unused")
	private LinkedList<Integer>[][] generateIndivdual_map_by_loc() {
        RelationshipMap householdMap = getRelMap()[RELMAP_HOUSEHOLD];
        Integer[][] household_indices = ((Integer[][]) getFields()[FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD]);
        @SuppressWarnings("unchecked")
		LinkedList<Integer>[][] indivudual_map_by_loc = new LinkedList[household_indices.length][LENGTH_INDIV_MAP];

        for (LinkedList<Integer>[] indivudual_map_at_loc : indivudual_map_by_loc) {
            for (int type = 0; type < indivudual_map_at_loc.length; type++) {
                indivudual_map_at_loc[type] = new LinkedList<>();
            }
        }

        for (AbstractIndividualInterface person : this.getPop()) {
            int loc = ((person.MoveablePersonInterface) person).getHomeLocation();
            LinkedList<Integer> indiv_list;
            int[] val;

            if (!householdMap.containsVertex(person.getId())) {
                householdMap.addVertex(person.getId());
            }
            int deg = householdMap.degreeOf(person.getId());
            if (deg == 0) { // No household - possibly error 
                System.out.println("generateIndivudual_map_by_loc: Indivdual with no core household");
            } else {
                Iterator<SingleRelationship> edgeIt;
                edgeIt = householdMap.edgesOf(person.getId()).iterator();
                SingleRelationship edge = edgeIt.next();

                if (deg > 1) { // Mulitple core household - possibly error
                    System.out.println("generateIndivudual_map_by_loc: Multiple core household allocated previously. Removing extra edges.");
                }

                // Household id               
                val = edge.getLinksValues();
                int houseId = val[0] == person.getId() ? val[1] : val[0];
                int[] houseStat = getHouseholdStat(householdMap, houseId);
                int listPt = -1;

                if (isDependent(person.getAge())) {
                    if (houseStat[HOUSEHOLD_MAP_NUM_ADULT] == 0) {
                        listPt = INDIV_MAP_DEPENDENT_ALONE;
                    } else {
                        if (houseStat[HOUSEHOLD_MAP_NUM_DEPENDENT] == 1) {
                            listPt = INDIV_MAP_DEPENDENT_SINGLE_WITH_ADULT;
                        } else {
                            listPt = INDIV_MAP_DEPENDENT_MULTI_WITH_ADULT;
                        }
                    }
                } else {
                    if (houseStat[HOUSEHOLD_MAP_NUM_ADULT] == 1) {
                        if (houseStat[HOUSEHOLD_MAP_NUM_DEPENDENT] == 0) {
                            listPt = INDIV_MAP_ADULT_LONER;
                        } else {
                            listPt = INDIV_MAP_ADULT_SINGLE_WITH_DEPENDENT;
                        }
                    } else {
                        if (houseStat[HOUSEHOLD_MAP_NUM_DEPENDENT] == 0) {
                            listPt = INDIV_MAP_ADULT_NO_DEPENDENT;
                        } else {
                            listPt = INDIV_MAP_ADULT_MULTI_PARENT;
                        }
                    }
                }

                indiv_list = indivudual_map_by_loc[loc][listPt];
                indiv_list.add(person.getId());
            }
        }

        return indivudual_map_by_loc;

    }

}
