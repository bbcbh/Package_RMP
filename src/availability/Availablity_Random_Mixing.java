package availability;

import java.util.HashMap;
import person.AbstractIndividualInterface;
import random.RandomGenerator;
import util.ArrayUtilsRandomGenerator;

public class Availablity_Random_Mixing extends AbstractAvailability {

    protected AbstractIndividualInterface[][] available;
    protected AbstractIndividualInterface[][] pairing;

    // Id, [genderIndex][columnIndex]
    protected HashMap<Integer, int[]> mapping = new HashMap<>();
    protected boolean[][] selected = new boolean[2][];

    public Availablity_Random_Mixing(RandomGenerator RNG) {
        super(RNG);
    }

    @Override
    public AbstractIndividualInterface[][] getPairing() {
        return pairing;
    }

    @Override
    public boolean setParameter(String id, Object value) {
        return false;
    }

    @Override
    public Object getParameter(String id) {
        return null;
    }

    @Override
    public void setAvailablePopulation(AbstractIndividualInterface[][] available) {
        mapping.clear();
        // Should already sorted by age and gender
        this.available = available;
        for (int g = 0; g < available.length; g++) {
            selected[g] = new boolean[available[g].length];
            if (mapping != null) {
                for (int i = 0; i < available[g].length; i++) {
                    mapping.put(available[g][i].getId(), new int[]{g, i});
                }
            }
        }
    }

    @Override
    public boolean removeMemberAvailability(AbstractIndividualInterface p) {
        int[] ga = mapping.get(p.getId());
        if (ga != null) {
            return removeMemberAvailability(ga);
        } else {
            return false;
        }
    }

    private boolean removeMemberAvailability(int[] ga) {
        if (selected[ga[0]][ga[1]]) {
            return false;
        } else {
            selected[ga[0]][ga[1]] = true;
            return true;
        }
    }

    @Override
    public boolean memberAvailable(AbstractIndividualInterface p) {
        int[] ga = mapping.get(p.getId());
        if (ga != null) {
            return memberAvailable(ga);
        } else {
            return false;
        }
    }

    private boolean memberAvailable(int[] ga) {
        return !selected[ga[0]][ga[1]];
    }

    private AbstractIndividualInterface getMemberByIndex(int[] ga) {
        return available[ga[0]][ga[1]];
    }

    @Override
    public int generatePairing() {
        pairing = new AbstractIndividualInterface[Math.min(available[0].length, available[1].length)][];
        int pairFormed = 0;
        Integer[] ids = mapping.keySet().toArray(new Integer[mapping.size()]);

        ArrayUtilsRandomGenerator.shuffleArray(ids, getRNG());

        for (Integer selectorId : ids) {
            int[] ga = mapping.get(selectorId);

            // First check if the person is available in the first place
            if (memberAvailable(ga)) {
                AbstractIndividualInterface person = getMemberByIndex(ga);

                // List all partners which are within limit
                int altGender = person.isMale() ? 1 : 0;

                int minIndex = 0;
                int maxIndex = available[altGender].length;

                if (maxIndex > minIndex) {
                    int[][] possiblePartnersIndex
                            = new int[maxIndex - minIndex][2];

                    AbstractIndividualInterface[] possiblePartners
                            = new AbstractIndividualInterface[possiblePartnersIndex.length];
                    int possiblePartnerPt = 0;

                    for (int k = minIndex; k < maxIndex; k++) {
                        int[] partner_testing_index = new int[]{altGender, k};
                        AbstractIndividualInterface partner_testing = getMemberByIndex(partner_testing_index);

                        // Age matched and available
                        boolean possible = memberAvailable(partner_testing_index);

                        if (possible) {
                            // Can be a partner                                
                            possiblePartners[possiblePartnerPt] = partner_testing;
                            possiblePartnersIndex[possiblePartnerPt] = partner_testing_index;
                            possiblePartnerPt++;
                        }

                    }

                    // Forming pairing
                    if (possiblePartnerPt > 0) {
                        // Randomly select one from available partners
                        int sel = getRNG().nextInt(possiblePartnerPt);
                        AbstractIndividualInterface partner = possiblePartners[sel];
                        // Remove selected from availability
                        removeMemberAvailability(possiblePartnersIndex[sel]);
                        // Remove the selector from availability
                        removeMemberAvailability(ga);

                        pairing[pairFormed] = new AbstractIndividualInterface[2];
                        pairing[pairFormed][person.isMale() ? 0 : 1] = person;
                        pairing[pairFormed][partner.isMale() ? 0 : 1] = partner;
                        pairFormed++;
                    }
                }

            }
        }

        return pairFormed;
    }
}
