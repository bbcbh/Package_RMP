package population;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

        // Household
        RelationshipMap householdMap = getRelMap()[RELMAP_HOUSEHOLD];
        Integer[][] household_indices = ((Integer[][]) getFields()[FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD]);
        HashMap<Integer, ArrayList<Integer>> household_size_map = new HashMap<>();  // K = size of household , V = {household index};

        int numInHouseTotal = 0;
        int numHousehold = 0;
        int lastHouseholdNum = 0;

        for (int loc = 0; loc < household_indices.length; loc++) {
            numHousehold += household_indices[loc].length;
            for (Integer houseId : household_indices[loc]) {
                lastHouseholdNum = Math.min(lastHouseholdNum, houseId);
                int numInHouse = householdMap.degreeOf(houseId);
                numInHouseTotal += numInHouse;
                ArrayList<Integer> ent = household_size_map.get(numInHouse);
                if (ent == null) {
                    ent = new ArrayList<>();
                    household_size_map.put(numInHouse, ent);
                }
                ent.add(houseId);
            }
        }

        Integer[] householdSizeMapping = household_size_map.keySet().toArray(new Integer[household_size_map.size()]);
        Arrays.sort(householdSizeMapping);

        /*
        System.out.println("For entire population (pre adjustment):");

        for (int aI = 0; aI < ageSpreadSummary.length; aI++) {
            System.out.println(String.format("Age Grp #%d: %d (%.1f%%)", aI, ageSpreadSummary[aI].size(), 100f * ageSpreadSummary[aI].size() / getPop().length));
        }

        System.out.println(String.format("# household: %d", numHousehold));
        System.out.println(String.format("# in household: %d (%.1f%%)", numInHouseTotal, 100f * numInHouseTotal / getPop().length));

        for (Integer hSM : householdSizeMapping) {
            int numHouse = household_size_map.get(hSM).size();
            System.out.println(String.format("Household size of %d: %d (%.1f%% of household)", hSM, numHouse, 100f * numHouse / numHousehold));
        }
         */
        // Determine lone person household, and break if needed        
        int numLonerToBeAdded = Math.round(
                numHousehold * household_spread[HOUSEHOLD_SPREAD_LONE] / 100f
                - (household_size_map.get(1) == null ? 0 : household_size_map.get(1).size()));

        Set<SingleRelationship> edgeToRemoveSet = new HashSet<>();

        for (int hSize = householdSizeMapping.length - 1;
                hSize >= 0 && numLonerToBeAdded > 0; hSize--) {
            Integer[] householdIndicebySize = household_size_map.get(householdSizeMapping[hSize]).toArray(new Integer[0]);

            ArrayUtilsRandomGenerator.shuffleArray(householdIndicebySize, this.getRNG());

            for (int hIndex = 0; hIndex < householdIndicebySize.length && numLonerToBeAdded > 0; hIndex++) {

                // Break a single person to new household
                int houseId = householdIndicebySize[hIndex];
                Set<SingleRelationship> edges = householdMap.edgesOf(houseId);
                Iterator<SingleRelationship> iter = edges.iterator();

                while (iter.hasNext() && numLonerToBeAdded > 0) {
                    SingleRelationship edgeToRemove = iter.next();
                    int[] linkVal = edgeToRemove.getLinksValues();
                    int lonerId = linkVal[0] == houseId ? linkVal[1] : linkVal[0];

                    // Set up a new house for loner 
                    if (this.getPersonById(lonerId).getAge() >= AGE_GROUPING[0]
                            && this.getPersonById(lonerId).getAge() < AGE_GROUPING[AGE_GROUPING.length - 1]) {

                        int loc = ((person.MoveablePersonInterface) this.getPersonById(lonerId)).getHomeLocation();
                        int newHousehold_index = lastHouseholdNum - 1;
                        householdMap.addVertex(newHousehold_index);

                        lastHouseholdNum--;
                        numHousehold++;

                        // Update RELMAP_HOUSEHOLD                    
                        float[] householdSpreadDist = householdSpreadByLoc[loc];
                        float[] nonHouseholdContactRateDist = nonHouseholdContactRateByLoc[loc];
                        setContactOptionCore(this.getPersonById(lonerId), newHousehold_index,
                                householdSpreadDist, nonHouseholdContactRateDist);
                        edgeToRemoveSet.add(edgeToRemove);

                        //Update FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD
                        household_indices[loc] = Arrays.copyOf(household_indices[loc], household_indices[loc].length + 1);
                        household_indices[loc][household_indices[loc].length - 1] = newHousehold_index;

                        ArrayList<Integer> ent = household_size_map.get(1);

                        if (ent == null) {
                            ent = new ArrayList<>();
                            household_size_map.put(1, ent);
                        }
                        ent.add(newHousehold_index);

                        numLonerToBeAdded = Math.round(numHousehold * household_spread[HOUSEHOLD_SPREAD_LONE] / 100f
                                - household_size_map.get(1).size());

                    }
                }
            }

        }

        for (SingleRelationship edgeToRemove : edgeToRemoveSet) {
            householdMap.removeEdge(edgeToRemove);
        }
        edgeToRemoveSet.clear();

        int maxHouseSize = householdSizeMapping[householdSizeMapping.length - 1] + 1;
        int[][] householdSizeByAdults = new int[maxHouseSize][numHousehold]; // by location
        int[] householdSizeByAdultsIndex = new int[householdSizeByAdults.length]; // by location

        for (int loc = 0; loc < household_indices.length; loc++) {
            Arrays.fill(householdSizeByAdultsIndex, 0);
            float[] householdSpreadDist = householdSpreadByLoc[loc];
            float[] nonHouseholdContactRateDist = nonHouseholdContactRateByLoc[loc];

            for (Integer houseId : household_indices[loc]) {
                int numAdults = 0;
                for (SingleRelationship rel : householdMap.edgesOf(houseId)) {
                    int[] val = rel.getLinksValues();
                    int personId = val[0] == houseId ? val[1] : val[0];
                    if (this.getPersonById(personId).getAge() >= AGE_GROUPING[0]) {
                        numAdults++;
                    }
                }
                int hS_Index = Math.min(numAdults, householdSizeByAdultsIndex.length - 1);
                householdSizeByAdults[hS_Index][householdSizeByAdultsIndex[hS_Index]] = houseId;
                householdSizeByAdultsIndex[hS_Index]++;
            }

            // Ensure all household has at least one adult
            for (int noAdult = 0; noAdult < householdSizeByAdultsIndex[0]; noAdult++) {
                int house_Id_no_adult = householdSizeByAdults[0][noAdult];

                SingleRelationship edgeToRemoveNoAdult = householdMap.edgesOf(house_Id_no_adult).iterator().next();
                int[] naeVal = edgeToRemoveNoAdult.getLinksValues();
                int childId = naeVal[0] == house_Id_no_adult ? naeVal[1] : naeVal[0];

                int adultId = -1;
                SingleRelationship edgeToRemoveAdult = null;

                for (int house_size_multi_adult = householdSizeByAdults.length - 1;
                        house_size_multi_adult > 1 && edgeToRemoveAdult == null; house_size_multi_adult--) {

                    for (int sel = 0; sel < householdSizeByAdultsIndex[house_size_multi_adult]
                            && edgeToRemoveAdult == null; sel++) {

                        int house_Id_Adult = householdSizeByAdults[house_size_multi_adult][sel];

                        Iterator<SingleRelationship> potential_adult_edges
                                = householdMap.edgesOf(house_Id_Adult).iterator();

                        while (edgeToRemoveAdult == null && potential_adult_edges.hasNext()) {

                            edgeToRemoveAdult = potential_adult_edges.next();
                            int[] aeVal = edgeToRemoveAdult.getLinksValues();
                            adultId = aeVal[0] == house_Id_Adult ? aeVal[1] : aeVal[0];

                            if (this.getPersonById(adultId).getAge() < AGE_GROUPING[0]) {
                                edgeToRemoveAdult = null;
                                adultId = -1;
                            }
                        }

                        if (edgeToRemoveAdult != null) {

                            setContactOptionCore(this.getPersonById(childId), house_Id_Adult,
                                    householdSpreadDist, nonHouseholdContactRateDist);
                            setContactOptionCore(this.getPersonById(adultId), house_Id_no_adult,
                                    householdSpreadDist, nonHouseholdContactRateDist);

                            edgeToRemoveSet.add(edgeToRemoveNoAdult);
                            edgeToRemoveSet.add(edgeToRemoveAdult);

                            // Update householdSizeByAdults mapping.
                            householdSizeByAdults[house_size_multi_adult - 1][householdSizeByAdultsIndex[house_size_multi_adult - 1]] = house_Id_Adult;
                            householdSizeByAdultsIndex[house_size_multi_adult - 1]++;
                            householdSizeByAdultsIndex[house_size_multi_adult]--;

                        }

                    }

                }

            }
            
            
            
            
            

        }

        for (SingleRelationship edgeToRemove : edgeToRemoveSet) {
            householdMap.removeEdge(edgeToRemove);
        }
        edgeToRemoveSet.clear();

        
        HashMap<Integer, ArrayList<Integer>> households_by_adults = new HashMap<>();
        HashMap<Integer, ArrayList<Integer>> households_by_children = new HashMap<>();
        HashMap<Integer, ArrayList<Integer>> households_with_dependent = new HashMap<>();

        // Checking               
        numHousehold = 0;
        household_size_map.clear();
        households_by_adults.clear();
        households_by_children.clear();
        households_with_dependent.clear();

        for (int loc = 0; loc < household_indices.length; loc++) {
            numHousehold += household_indices[loc].length;
            for (Integer houseId : household_indices[loc]) {
                ArrayList<Integer> ent;
                int numInHouse = householdMap.degreeOf(houseId);
                numInHouseTotal += numInHouse;
                ent = household_size_map.get(numInHouse);
                if (ent == null) {
                    ent = new ArrayList<>();
                    household_size_map.put(numInHouse, ent);
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

        householdSizeMapping = household_size_map.keySet().toArray(new Integer[household_size_map.size()]);
        Arrays.sort(householdSizeMapping);
        for (Integer hSM : householdSizeMapping) {
            int numHouse = household_size_map.get(hSM).size();
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
