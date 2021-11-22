package util;

import person.AbstractIndividualInterface;
import person.Person_Remote_MetaPopulation;

public class Default_Remote_MetaPopulation_Behavor_Classifier implements PersonClassifier {

    /**
	 * 
	 */
	private static final long serialVersionUID = -5795207504232367227L;

	public Default_Remote_MetaPopulation_Behavor_Classifier() {
    }

    @Override
    public int classifyPerson(AbstractIndividualInterface p) {
        int r = -1;

        if (p instanceof Person_Remote_MetaPopulation) {
            Person_Remote_MetaPopulation rp = (Person_Remote_MetaPopulation) p;
            int numPartLastYears = rp.getNumPartnerInPastYear();
            r = 0;
            if (numPartLastYears > 0) {
                r++;
                if (numPartLastYears > 2) {
                    r++;
                    if (numPartLastYears > 5) {
                        r++;
                    }
                }
            }

        }
        return r;
    }

    @Override
    public int numClass() {
        return 4;
    }

}
