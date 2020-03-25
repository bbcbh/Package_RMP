package population;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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

    Object[] DEFAULT_FIELDS_REMOTE_METAPOP_COVID19 = {
        // FIELDS_REMOTE_METAPOP_COVID19_HOSUEHOLD_SIZE_DIST
        // Format: [loc][cumul_percent_of_pop_1,mean_household_size_1 , cumul_percent_of_pop_2 ...
        new double[][]{},
        // FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD
        // Integer[] list of id among unqique household
        new Integer[]{},};

    private static final int RELMAP_HOUSEHOLD = 0;

    public Population_Remote_MetaPopulation_COVID19(long seed) {
        super(seed);

        Object[] orgFields = this.getFields();
        Object[] newFields = Arrays.copyOf(orgFields, orgFields.length + DEFAULT_FIELDS_REMOTE_METAPOP_COVID19.length);
        System.arraycopy(DEFAULT_FIELDS_REMOTE_METAPOP_COVID19, 0, newFields, orgFields.length, DEFAULT_FIELDS_REMOTE_METAPOP_COVID19.length);
        this.setFields(newFields);
    }

    public void allolocateHosuehold() {

        int[] popSize = (int[]) getFields()[FIELDS_REMOTE_METAPOP_POP_SIZE];
        RelationshipMap householdlMap = getRelMap()[RELMAP_HOUSEHOLD];
        ArrayList<Integer> uniqueHousehold = new ArrayList<>();

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
                uniqueHousehold.add(resident[firstInHouse].getId());
                householdlMap.addVertex(resident[firstInHouse].getId());

                while ((rPt + 1) < resident.length && numInHosuse > 1) {
                    rPt++;
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
                rPt++;
            }

        }

        getFields()[FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD]
                = uniqueHousehold.toArray(new Integer[uniqueHousehold.size()]);

        Arrays.sort((Integer[]) getFields()[FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD]);

        // Stat         
        ArrayList<ArrayList<Integer>> householdSizeRec = new ArrayList<>(popSize.length);
        for (int i = 0; i < popSize.length; i++) {
            householdSizeRec.add(new ArrayList<>());
        }

        for (Integer h : (Integer[]) getFields()[FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD]) {
            Person_Remote_MetaPopulation p = (Person_Remote_MetaPopulation) getLocalData().get(h);
            int homeLoc = p.getHomeLocation();

            householdSizeRec.get(homeLoc).add(householdlMap.degreeOf(h) + 1);
        }

        for (int loc = 0; loc < popSize.length; loc++) {
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

        Integer[] uniqueHousehold = (Integer[]) getFields()[FIELDS_REMOTE_METAPOP_COVID19_UNIQUE_HOUSEHOLD];

        int k = Arrays.binarySearch(uniqueHousehold, removedPerson.getId());
        if (k >= 0) {
            uniqueHousehold[k] = newPerson.getId();
            Arrays.sort(uniqueHousehold);
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

        pop.initialise();

        pop.allolocateHosuehold();

    }

}
