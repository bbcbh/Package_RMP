package util;

import person.AbstractIndividualInterface;

public class Default_Remote_MetaPopulation_AgeGrp_Classifier implements PersonClassifier {

    /**
	 * 
	 */
	private static final long serialVersionUID = -3041890112933595553L;

	public Default_Remote_MetaPopulation_AgeGrp_Classifier() {

    }

    @Override
    public int classifyPerson(AbstractIndividualInterface p) {
        int r = -1;
        if (p.getAge() >= 16 * AbstractIndividualInterface.ONE_YEAR_INT) {
            r = 0;
            if (p.getAge() >= 20 * AbstractIndividualInterface.ONE_YEAR_INT) {
                r++;
                if (p.getAge() >= 25 * AbstractIndividualInterface.ONE_YEAR_INT) {
                    r++;
                    if (p.getAge() >= 30 * AbstractIndividualInterface.ONE_YEAR_INT) {
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
