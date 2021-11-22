package util;

import java.io.Serializable;
import random.RandomGenerator;

public class Factory_Population_Decomposition implements Serializable{

    /**
	 * 
	 */
	private static final long serialVersionUID = 5046699137149862469L;

	// Default - from 3238.0 Estimates and Projections, Aboriginal and Torres Strait Islander Australians, 2001 to 2026    
    // 2011 data
    public static final int[] POP_COMPOSITION_REMOTE_2011 = {
        // Male: 15-19, 20-24, 25-29, 30-34 .... 70-74, 75+
        6999, 6644, 5848, 4895, //4787, 4491, 3791, 3173, 2434, 1617, 1066, 569, 691,
        // Female
        6586, 6659, 6062, 5087, //4873, 4817, 3882, 3350, 2394, 1766, 1115, 770, 1035
    };

    public static final int[] POP_COMPOSITION_REGIONAL_2011 = {
        // Male: 15-19, 20-24, 25-29, 30-34.... 70-74, 75+
        16753, 12490, 10222, 8085, //8396, 8286, 7226, 6329, 4733, 3296, 2068, 1268, 1212,
        // Female
        15768, 12078, 9878, 8505, //9050, 9112, 7714, 6564, 4848, 3575, 2362, 1516, 1792,
    };

    public static final int[] POP_COMPOSITION_URBAN_2011 = {
        // Male: 15-19, 20-24, 25-29, 30-34 .... 70-74, 75+
        13875, 11846, 9146, 6881, //6880, 6491, 5414, 4468, 3315, 2282, 1410, 797, 868,
        // Female:
        12801, 11449, 9234, 7228, //7314, 7310, 6162, 4928, 3838, 2654, 1659, 1052, 1432
    };
    
    // 2010 data
    public static final int[] POP_COMPOSITION_REMOTE_2010 = {
        // Male: 15-19, 20-24, 25-29, 30-34 .... 70-74, 75+
        7086, 6461, 5756, 4742, //4925,4337,3704,3105,2312,1551,998,542,704
        // Female
        6720, 6559, 5907, 4931, //5046,4588,3807,3185,2351,1645,1076,715,1031

    };

    public static final int[] POP_COMPOSITION_REGIONAL_2010 = {
        // Male: 15-19, 20-24, 25-29, 30-34 .... 70-74, 75+       
        15928, 11768, 9912, 7942,//8585,8018,7235,6066,4482,3081,1944,1202,1148
        // Female       
        14915, 11581, 9480, 8469, //9278, 8779, 7680, 6200, 4650, 3354, 2237, 1419, 1704

    };

    public static final int[] POP_COMPOSITION_URBAN_2010 = {
        // Male: 15-19, 20-24, 25-29, 30-34.... 70-74, 75+
        13655, 11177,8743, 6707, //7000,6219,5302,4301,3176,2096,1299,754,824
        // Female:
        12601, 11047,8692, 7139, //7532, 6983, 6050, 4733, 3614, 2464, 1526, 1038, 1365
    };
    
    

    public static float[] getDecomposition(int[] entries) {
        float[] decomp = new float[entries.length];
        float current;
        float sum = 0;
        for (int i = 0; i < decomp.length; i++) {
            sum += entries[i];
        }

        for (int i = 0; i < decomp.length; i++) {
            current = entries[i] / sum;
            decomp[i] = current;
        }

        return decomp;
    }
    
    public static int[] getDecomposition(int[] entries, int popSize, RandomGenerator rng) {
        int[] decomp = new int[entries.length];
        float current;
        float checkSum = 0;
        float sum = 0;
        
        for (int i = 0; i < decomp.length; i++) {
            sum += entries[i];
        }
        for (int i = 0; i < decomp.length; i++) {
            current = entries[i] / sum;
            decomp[i] = Math.round(current *popSize);
            checkSum+= decomp[i];
        }
        // Allocated random offset if needed
        while(checkSum != popSize){            
            if(checkSum > popSize){
               decomp[rng.nextInt(decomp.length)]--;     
               checkSum--;
            }
            if(checkSum < popSize){
               decomp[rng.nextInt(decomp.length)]++;                
               checkSum++;
            }
            
        }
        

        return decomp;
    }

}
