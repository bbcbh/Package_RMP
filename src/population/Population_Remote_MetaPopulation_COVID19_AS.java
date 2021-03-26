package population;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
        final double[] AGR_GOURPING = {15 * 360, 65 * 360};
        ArrayList<Integer>[] ageSpreadSummary = new ArrayList[AGR_GOURPING.length + 1]; // ArrayList of person id

        // Indivdual         
        for (int i = 0; i < ageSpreadSummary.length; i++) {
            ageSpreadSummary[i] = new ArrayList<>();
        }

        for (AbstractIndividualInterface person : this.getPop()) {
            int arrayId = Arrays.binarySearch(AGR_GOURPING, person.getAge());
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

        for (int loc = 0; loc < household_indices.length; loc++) {
            numHousehold += household_indices[loc].length;
            for (Integer houseId : household_indices[loc]) {
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

        System.out.println("For entire population (pre adjustment):");

        for (int aI = 0; aI < ageSpreadSummary.length; aI++) {
            System.out.println(String.format("#%d: %d (%.1f%%)", aI, ageSpreadSummary[aI].size(), 100f * ageSpreadSummary[aI].size() / getPop().length));
        }

        System.out.println(String.format("# household: %d", numHousehold));
        System.out.println(String.format("# in household: %d (%.1f%%)", numInHouseTotal, 100f * numInHouseTotal / getPop().length));

        Integer[] householdSizeMapping = household_size_map.keySet().toArray(new Integer[household_size_map.size()]);
        Arrays.sort(householdSizeMapping);
        for (Integer hSM : householdSizeMapping) {
            int numHouse = household_size_map.get(hSM).size();
            System.out.println(String.format("Household size of %d: %d (%.1f%% of household)", hSM, numHouse, 100f * numHouse / numHousehold));
        }

        // Determine lone person household, and break if needed
        int numLoner = Math.round(numHousehold * household_spread[HOUSEHOLD_SPREAD_LONE]);

        for (int hSize = 0; hSize < householdSizeMapping.length && numLoner > 0; hSize++) {
            Integer[] householdIndicebySize = household_size_map.get(householdSizeMapping[hSize]).toArray(new Integer[0]);

            ArrayUtilsRandomGenerator.shuffleArray(householdIndicebySize, this.getRNG());

            for (int hIndex = 0; hIndex < householdIndicebySize.length && numLoner > 0; hIndex++) {
                // Break a single person to new household
                int houseId = householdIndicebySize[hIndex];                
                int numPersonInHouse  = householdMap.degreeOf(houseId);               
                
                while(numPersonInHouse > 1){                                      
                    Set<SingleRelationship> edges = householdMap.edgesOf(houseId);
                    
                    SingleRelationship edgeToRemove = edges.iterator().next();                    
                    int[] linkVal = edgeToRemove.getLinksValues();
                    int lonerId = linkVal[0] ==  houseId? linkVal[1] : linkVal[0];
                    
                    // Set up a new house for loner 
                    
                    
                    
                    
                    numPersonInHouse  = householdMap.degreeOf(houseId);      
                }
                
                
                
                
                

            }

        }

        //Work in progress
        System.exit(0);

    }

}
