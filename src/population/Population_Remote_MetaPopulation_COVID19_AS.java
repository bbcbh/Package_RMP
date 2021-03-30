package population;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import person.AbstractIndividualInterface;
import static population.Population_Remote_MetaPopulation_COVID19.RELMAP_HOUSEHOLD;
import relationship.RelationshipMap;
import relationship.SingleRelationship;
import util.ArrayUtilsRandomGenerator;

/**
 * <p>
 * An extension of COVID_19 model with age structure</p>
 *
 * @author Ben Hui
 *
 */
public class Population_Remote_MetaPopulation_COVID19_AS
        extends Population_Remote_MetaPopulation_COVID19 {

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

    public static final double[] AGE_GROUPING = {15 * 360, 65 * 360};

    public Population_Remote_MetaPopulation_COVID19_AS(long seed) {
        super(seed);
    }

    public float[] getHousehold_spread() {
        return household_spread;
    }

    public void setHousehold_spread(float[] household_spread) {
        this.household_spread = household_spread;
    }

    @Override
    public void allolocateCoreHosuehold(float[][] householdSizeDistByLoc, float[][] householdSpreadByLoc, float[][] nonHouseholdContactRateByLoc) {
        super.allolocateCoreHosuehold(householdSizeDistByLoc, householdSpreadByLoc, nonHouseholdContactRateByLoc);

        // Calculate age spread
        /*
        ArrayList<Integer>[] ageSpreadSummary = new ArrayList[AGE_GROUPING.length + 1]; // ArrayList of person id

        // Indivdual         
        for (int i = 0; i < ageSpreadSummary.length; i++) {
            ageSpreadSummary[i] = new ArrayList<>();
        }

        for (AbstractIndividualInterface person : this.getPop()) {
            int arrayId = Arrays.binarySearch(AGE_GROUPING, person.getAge());
            if (arrayId < 0) {
                arrayId = -(arrayId + 1);
            }
            ageSpreadSummary[arrayId].add(person.getId());
        }
         */
        // Household
        RelationshipMap householdMap = getRelMap()[RELMAP_HOUSEHOLD];
        Integer[][] household_indices = ((Integer[][]) getFields()[FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD]);

        final int HOUSEHOLD_MAP_NUM_ADULT = 0;
        final int HOUSEHOLD_MAP_NUM_DEPENDENT = HOUSEHOLD_MAP_NUM_ADULT + 1;
        final int LENGTH_HOUSEHOLD_MAP = HOUSEHOLD_MAP_NUM_DEPENDENT + 1;

        HashMap<Integer, int[]> household_map_all_loc = new HashMap<>();  // K = household id, V = [num_adult, num_dependent];

        final int INDIV_MAP_ADULT_LONER = 0;
        final int INDIV_MAP_ADULT_SINGLE_PARENT = INDIV_MAP_ADULT_LONER + 1;             // Alone in household with multiple dependents
        final int INDIV_MAP_ADULT_MULTI_PARENT = INDIV_MAP_ADULT_SINGLE_PARENT + 1;      // In household with more than one adults and dependents
        final int INDIV_MAP_ADULT_NO_DEPENDENT = INDIV_MAP_ADULT_MULTI_PARENT + 1;       // In household with no dependent 
        final int INDIV_MAP_DEPENDENT = INDIV_MAP_ADULT_NO_DEPENDENT + 1;
        final int INDIV_MAP_DEPENDENT_ALONE = INDIV_MAP_DEPENDENT + 1;                   // Dependent who are living alone - will be 0 by the end
        final int LENGTH_INDIV_MAP = INDIV_MAP_DEPENDENT_ALONE + 1;

        LinkedList<Integer>[][] indivudual_map_by_loc = new LinkedList[household_indices.length][LENGTH_INDIV_MAP];
        for (int loc = 0; loc < indivudual_map_by_loc.length; loc++) {
            for (int type = 0; type < indivudual_map_by_loc[loc].length; type++) {
                indivudual_map_by_loc[loc][type] = new LinkedList<>();
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

                int listPt = person.getAge() < AGE_GROUPING[0] ? INDIV_MAP_DEPENDENT_ALONE : INDIV_MAP_ADULT_LONER;

                indiv_list = indivudual_map_by_loc[loc][listPt];
                if (indiv_list == null) {
                    indivudual_map_by_loc[loc][listPt] = new LinkedList<>();
                    indiv_list = indivudual_map_by_loc[loc][listPt];
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
                    houseStat = new int[LENGTH_HOUSEHOLD_MAP];
                    household_map_all_loc.put(houseId, houseStat);
                    numHousehold++;
                    lastHouseholdNum = Math.min(lastHouseholdNum, houseId);
                    Set<SingleRelationship> house_edges = householdMap.edgesOf(houseId);
                    for (SingleRelationship house_edge : house_edges) {
                        val = house_edge.getLinksValues();
                        int memberId = val[0] == houseId ? val[1] : val[0];
                        if (this.getPersonById(memberId).getAge() < AGE_GROUPING[0]) {
                            houseStat[HOUSEHOLD_MAP_NUM_DEPENDENT]++;
                        } else {
                            houseStat[HOUSEHOLD_MAP_NUM_ADULT]++;
                        }
                    }

                    if (houseStat[HOUSEHOLD_MAP_NUM_ADULT] > 1) {
                        splitable_households.add(houseId);
                    }

                }

                int listPt = -1;
                if (person.getAge() < AGE_GROUPING[0]) {
                    if (houseStat[HOUSEHOLD_MAP_NUM_ADULT] == 0) {
                        listPt = INDIV_MAP_DEPENDENT_ALONE;
                        if (houseStat[HOUSEHOLD_MAP_NUM_DEPENDENT] == 1) {
                            numLonerAlreadyInPop++;
                        }
                    } else {
                        listPt = INDIV_MAP_DEPENDENT;
                    }
                } else {
                    if (houseStat[HOUSEHOLD_MAP_NUM_ADULT] == 1) {
                        if (houseStat[HOUSEHOLD_MAP_NUM_DEPENDENT] == 0) {
                            listPt = INDIV_MAP_ADULT_LONER;
                            numLonerAlreadyInPop++;
                        } else {
                            listPt = INDIV_MAP_ADULT_SINGLE_PARENT;
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
                if (this.getPersonById(split_adult_id).getAge() < AGE_GROUPING[0]) {
                    split_adult_id = -1;
                }
            }
            if (split_adult_id >= 0) {
                numHousehold++;
                lonerToBeAdded.add(split_adult_id);
                edgesToRemove.add(rel);
                householdMap.removeEdge(rel);
                int loc = ((person.MoveablePersonInterface) this.getPersonById(split_adult_id)).getHomeLocation();

                indivudual_map_by_loc[loc][INDIV_MAP_ADULT_LONER].add(split_adult_id);
                boolean removeSuc;

                if (household_map_all_loc.get(split_household_id)[HOUSEHOLD_MAP_NUM_DEPENDENT] == 0) {
                    removeSuc = indivudual_map_by_loc[loc][INDIV_MAP_ADULT_NO_DEPENDENT].remove((Integer) split_adult_id);
                } else {
                    removeSuc = indivudual_map_by_loc[loc][INDIV_MAP_ADULT_MULTI_PARENT].remove((Integer) split_adult_id);
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
                        if (!(this.getPersonById(memId).getAge() < AGE_GROUPING[0])) {
                            if (household_map_all_loc.get(split_household_id)[HOUSEHOLD_MAP_NUM_DEPENDENT] > 0) {
                                removeSuc = indivudual_map_by_loc[loc][INDIV_MAP_ADULT_MULTI_PARENT].remove((Integer) memId);
                                indivudual_map_by_loc[loc][INDIV_MAP_ADULT_SINGLE_PARENT].add(memId);
                            } else {
                                removeSuc = indivudual_map_by_loc[loc][INDIV_MAP_ADULT_NO_DEPENDENT].remove((Integer) memId);
                                indivudual_map_by_loc[loc][INDIV_MAP_ADULT_LONER].add(memId);
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

        for (int loc = 0; loc < household_indices.length; loc++) {

            float[] householdSpreadDist = householdSpreadByLoc[loc];
            float[] nonHouseholdContactRateDist = nonHouseholdContactRateByLoc[loc];

            LinkedList<Integer> list_dependent_alone = indivudual_map_by_loc[loc][INDIV_MAP_DEPENDENT_ALONE];
            LinkedList<Integer> list_multi_adult = indivudual_map_by_loc[loc][INDIV_MAP_ADULT_MULTI_PARENT];
            LinkedList<Integer> list_no_dependent = indivudual_map_by_loc[loc][INDIV_MAP_ADULT_NO_DEPENDENT];
            LinkedList<Integer> list_single_adult = indivudual_map_by_loc[loc][INDIV_MAP_ADULT_SINGLE_PARENT];
            LinkedList<Integer> list_dependent_with_adult = indivudual_map_by_loc[loc][INDIV_MAP_DEPENDENT];

            // Reduce number of household with dependent alone
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

                    int replacement_adult = picked_adult_list.remove(randomPick);
                    list_single_adult.add(replacement_adult);

                    SingleRelationship orgAdultConnect = householdMap.edgesOf(replacement_adult).iterator().next();
                    int[] valAdult = orgAdultConnect.getLinksValues();
                    int orgAdultHouseId = valAdult[0] == replacement_adult ? valAdult[1] : valAdult[0];

                    setContactOptionCore(this.getPersonById(dependent_alone), orgAdultHouseId,
                            householdSpreadDist, nonHouseholdContactRateDist);
                    setContactOptionCore(this.getPersonById(replacement_adult), orgDependhouseId,
                            householdSpreadDist, nonHouseholdContactRateDist);
                    householdMap.removeEdge(orgAdultConnect);
                    householdMap.removeEdge(orgDependConnect);

                    // Adjust house stat
                    household_map_all_loc.get(orgAdultHouseId)[HOUSEHOLD_MAP_NUM_ADULT]--;
                    household_map_all_loc.get(orgAdultHouseId)[HOUSEHOLD_MAP_NUM_DEPENDENT]++;

                    if (household_map_all_loc.get(orgAdultHouseId)[HOUSEHOLD_MAP_NUM_ADULT] == 1) {
                        for (SingleRelationship allHousehold : householdMap.edgesOf(orgAdultHouseId)) {
                            int[] v = allHousehold.getLinksValues();
                            int memId = v[0] == orgAdultHouseId ? v[1] : v[0];
                            if (fromNoDepList) {
                                list_no_dependent.remove((Integer) memId);
                                list_single_adult.add(memId);
                            }
                        }
                    }

                    household_map_all_loc.get(orgDependhouseId)[HOUSEHOLD_MAP_NUM_ADULT]++;
                    household_map_all_loc.get(orgDependhouseId)[HOUSEHOLD_MAP_NUM_DEPENDENT]--;

                }

                // Update dependent
                if (household_map_all_loc.get(orgDependhouseId)[HOUSEHOLD_MAP_NUM_ADULT] > 0) {
                    list_dependent_with_adult.add(list_dependent_alone.removeFirst());                    
                }
            }

        }

        // Checking  
        HashMap<Integer, ArrayList<Integer>> households_by_adults = new HashMap<>();
        HashMap<Integer, ArrayList<Integer>> households_by_children = new HashMap<>();
        HashMap<Integer, ArrayList<Integer>> households_with_dependent = new HashMap<>();
        HashMap<Integer, ArrayList<Integer>> household_size_map_all_loc = new HashMap<>();  // K = size of household, V = {household_id};

        numHousehold = 0;
        household_size_map_all_loc.clear();
        households_by_adults.clear();
        households_by_children.clear();
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
                int numChilds = 0;
                for (SingleRelationship rel : householdMap.edgesOf(houseId)) {
                    int[] val = rel.getLinksValues();
                    int personId = val[0] == houseId ? val[1] : val[0];
                    if (this.getPersonById(personId).getAge() >= AGE_GROUPING[0]) {
                        numAdults++;
                    } else {
                        numChilds++;
                    }
                }

                // Adult household
                ent = households_by_adults.get(numAdults);
                if (ent == null) {
                    ent = new ArrayList<>();
                    households_by_adults.put(numAdults, ent);
                }
                ent.add(houseId);

                // Children household
                ent = households_by_children.get(numChilds);
                if (ent == null) {
                    ent = new ArrayList<>();
                    households_by_children.put(numChilds, ent);
                }
                ent.add(houseId);

                if (numChilds > 0) {
                    ent = households_with_dependent.get(numAdults);
                    if (ent == null) {
                        ent = new ArrayList<>();
                        households_with_dependent.put(numAdults, ent);
                    }
                    ent.add(houseId);

                }

            }
        }

        System.out.println("===== Final =====");

        System.out.println("For entire population (final):");
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

        System.out.println("Number of children in household");
        mapping = households_by_children.keySet().toArray(new Integer[0]);
        Arrays.sort(mapping);
        for (Integer hId : mapping) {
            int numHouse = households_by_children.get(hId).size();
            System.out.println(String.format("Household with %d children: %d (%.1f%% of household)",
                    hId, numHouse, 100f * numHouse / numHousehold));
        }
        System.out.println();

        System.out.println("Number of adults in household with dependent(s)");
        mapping = households_with_dependent.keySet().toArray(new Integer[0]);
        Arrays.sort(mapping);
        for (Integer hId : mapping) {
            int numHouse = households_with_dependent.get(hId).size();
            System.out.println(String.format("Household with children(s) and %d adult(s): %d (%.1f%% of household)",
                    hId, numHouse, 100f * numHouse / numHousehold));
        }
        System.out.println();

        //Work in progress
        System.exit(0);

    }

}
