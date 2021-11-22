
package util;

import java.io.Serializable;
import java.util.Arrays;
import person.AbstractIndividualInterface;

/**
 *
 * @author Ben Hui
 */
public class Factory_Relationship_Duration_Max implements Serializable{
    /**
	 * 
	 */
	private static final long serialVersionUID = 6291070444233387796L;
	// From GOANNA, for all
    int[][] numPartLastYearLimit = new int[][]{new int[]{0, 1, 2, 5}, new int[]{1,2,5,10}};
    int[] numPartnerLikelihood = new int[]{8, 8+46, 8+46+38, 8+46+38+7}; 
        
    
    public Factory_Relationship_Duration_Max(){
        
    }
    
    public int gen_maxDurationOfSexualRelationship(AbstractIndividualInterface[] persons,random.RandomGenerator rng){
        
        int pRel = rng.nextInt(numPartnerLikelihood[numPartnerLikelihood.length-1]);
        int index = Arrays.binarySearch(numPartnerLikelihood, pRel);
        
        if(index < 0){
            // index = (-(insertion point) - 1)
            index = -(index + 1);
        }
        
        int numPartInYear = numPartLastYearLimit[0][index] + 
                rng.nextInt(numPartLastYearLimit[1][index] - numPartLastYearLimit[0][index]);
        
        int durMax = numPartInYear == 0? -1: Math.round(365.0f / numPartInYear);        
        
        return durMax;               
        
    }
    
    
}
