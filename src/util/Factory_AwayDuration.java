package util;

import java.io.Serializable;
import person.AbstractIndividualInterface;

/**
 *
 * @author Ben Hui
 */
public class Factory_AwayDuration implements Serializable{        
    
    public Factory_AwayDuration(){
        
    }
    
    public int numberOfDaysStayAway(AbstractIndividualInterface person, random.RandomGenerator rng){
        
        // From
        // Taylor J. Measuring short-term population mobility among indigenous Australians: 
        // options and implications. Australian Geographer 1998;29(1):125-37.
        
        //Prout S. On the move? Indigenous temporary mobility practices in Australia. 
        //CAEPR Working Paper No. 48. Canberra: Centre for Aboriginal Economic Policy Research, ANU, 2008.
        
        
        // 2 weeks to 6 months
        
        return 14 + rng.nextInt(6*30 -14);
        
        
    }
            
    
}
