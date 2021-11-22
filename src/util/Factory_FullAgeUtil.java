package util;

import person.AbstractIndividualInterface;

/**
 *
 * @author Ben Hui
 */
public class Factory_FullAgeUtil {

    public final static int MAX_AGE = 80;

    public static PersonClassifier genFullAgeClassifier() {
        return new PersonClassifier() {
            /**
			 * 
			 */
			private static final long serialVersionUID = 3621333932681618521L;

			@Override
            public int classifyPerson(AbstractIndividualInterface p) {
                return Math.min((int) p.getAge() / AbstractIndividualInterface.ONE_YEAR_INT, MAX_AGE-1);
            }

            @Override
            public int numClass() {
                return MAX_AGE;
            }
        };
    }

    // From Biddle 2009
    public static float[] getProportionPopAwayFull(int loc) {
        float[] res = new float[MAX_AGE * 2];

        for (int a = 0; a < MAX_AGE; a++) {
            switch (loc) {
                case 2:
                    // Regional
                    if (a <= 10) {
                        res[a] = a * (0.035f - 0.05f) / (10) + 0.05f;
                        res[a + MAX_AGE] = a * (0.035f - 0.05f) / (10) + 0.05f;
                    } else if (a <= 15) {
                        res[a] = (a - 10) * (0.04f - 0.035f) / (5) + 0.035f;
                        res[a + MAX_AGE] = (a - 10) * (0.04f - 0.035f) / (5) + 0.035f;
                    } else if (a <= 20) {
                        res[a] = (a - 15) * (0.09f - 0.04f) / (5) + 0.04f;
                        res[a + MAX_AGE] = (a - 15) * (0.09f - 0.04f) / (5) + 0.04f;
                    } else if (a <= 23) {
                        res[a] = (a - 20) * (0.10f - 0.09f) / (3) + 0.09f;
                        res[a + MAX_AGE] = (a - 20) * (0.075f - 0.09f) / (3) + 0.09f;
                    } else if (a <= 27) {
                        res[a] = (a - 23) * (0.095f - 0.10f) / (4) + 0.10f;
                        res[a + MAX_AGE] = (a - 23) * (0.072f - 0.075f) / (4) + 0.075f;
                    } else if (a <= 30) {
                        res[a] = (a - 27) * (0.090f - 0.095f) / (3) + 0.095f;
                        res[a + MAX_AGE] = (a - 27) * (0.050f - 0.072f) / (3) + 0.072f;
                    } else if (a <= 35) {
                        res[a] = (a - 30) * (0.090f - 0.090f) / (5) + 0.090f;
                        res[a + MAX_AGE] = (a - 30) * (0.050f - 0.050f) / (5) + 0.050f;
                    } else if (a <= 53) {
                        res[a] = (a - 35) * (0.065f - 0.090f) / (18) + 0.090f;
                        res[a + MAX_AGE] = (a - 35) * (0.065f - 0.050f) / (18) + 0.050f;
                    } else {
                        res[a] = 0.065f;
                        res[a + MAX_AGE] = 0.065f;
                    }
                    break;
                case 3:
                    if (a <= 10) {
                        res[a] = a * (0.06f - 0.08f) / (10) + 0.08f;
                        res[a + MAX_AGE] = a * (0.06f - 0.07f) / (10) + 0.07f;
                    } else if (a <= 15) {
                        res[a] = (a - 10) * (0.115f - 0.06f) / (5) + 0.06f;
                        res[a + MAX_AGE] = (a - 10) * (0.115f - 0.06f) / (5) + 0.06f;
                    } else if (a <= 30) {
                        res[a] = (a - 15) * (0.070f - 0.115f) / (15) + 0.115f;
                        res[a + MAX_AGE] = (a - 15) * (0.090f - 0.115f) / (15) + 0.115f;
                    } else if (a <= 55) {
                        res[a] = (a - 30) * (0.100f - 0.070f) / (25) + 0.070f;
                        res[a + MAX_AGE] = (a - 30) * (0.100f - 0.090f) / (25) + 0.090f;
                    } else {
                        res[a] = 0.100f;
                        res[a + MAX_AGE] = 0.100f;
                    }
                    break;
                default:
                    // Default
                    res[a] = 0;
                    res[a + MAX_AGE] = 0;
                    break;
            }

        }

        return res;
    }

}
