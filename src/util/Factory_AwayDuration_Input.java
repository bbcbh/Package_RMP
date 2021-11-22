package util;

import person.AbstractIndividualInterface;
import random.RandomGenerator;

/**
 *
 * @author Ben Hui
 */
public class Factory_AwayDuration_Input extends Factory_AwayDuration{
    /**
	 * 
	 */
	private static final long serialVersionUID = 792472691079333573L;
	int minDuration = 14;
    int variation = 6*30 -14;

    public Factory_AwayDuration_Input() {
    }
    
    public Factory_AwayDuration_Input(int minDuration, int variation){
        this.minDuration = minDuration;
        this.variation = variation;
    }

    @Override
    public int numberOfDaysStayAway(AbstractIndividualInterface person, RandomGenerator rng) {
        return minDuration + rng.nextInt(variation);
    }
    
    
    
    
    
    
}
