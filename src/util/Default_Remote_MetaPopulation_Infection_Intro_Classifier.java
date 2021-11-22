package util;

import person.AbstractIndividualInterface;

/**
 *
 * @author Bhui
 */
public class Default_Remote_MetaPopulation_Infection_Intro_Classifier implements PersonClassifier {

    /**
	 * 
	 */
	private static final long serialVersionUID = 9174957982560104230L;

	public Default_Remote_MetaPopulation_Infection_Intro_Classifier() {
    }

    @Override
    public int classifyPerson(AbstractIndividualInterface p) {
        if (p.getAge() == AbstractIndividualInterface.ONE_YEAR_INT * 16) {
            return p.isMale() ? 0 : 1;
        } else {
            return -1;
        }
    }

    @Override
    public int numClass() {
        return 2;
    }

}
