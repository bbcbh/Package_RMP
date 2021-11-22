package population;

import java.util.Arrays;
import java.util.HashMap;
import person.AbstractIndividualInterface;
import random.RandomGenerator;

/**
 *
 * @author Ben Hui
 */
public abstract class Abstract_MetaPopulation extends AbstractFieldsArrayPopulation {

    /**
	 * 
	 */
	private static final long serialVersionUID = 5093108335415250635L;
	public static final int FIELDS_META_RNG_INFECTION = LENGTH_FIELDS;
    public static final int FIELDS_META_POP_CURRENT_LOCATION = FIELDS_META_RNG_INFECTION + 1;
    public static final int FIELDS_META_POP_AWAY_UNTIL_AGE = FIELDS_META_POP_CURRENT_LOCATION + 1;
    public static final int LENGTH_FIELDS_META_POP = FIELDS_META_POP_AWAY_UNTIL_AGE + 1;

    public Abstract_MetaPopulation() {
        super();
        Object[] orgFields = this.getFields();
        Object[] newFields = Arrays.copyOf(orgFields, LENGTH_FIELDS_META_POP);
        newFields[FIELDS_META_POP_CURRENT_LOCATION] = new HashMap<>(); // ID, Location       
        newFields[FIELDS_META_POP_AWAY_UNTIL_AGE] = new HashMap<>(); // ID, Age     
        this.setFields(newFields);
    }

    protected void setInfectionRNG(RandomGenerator RNG) {
        getFields()[FIELDS_META_RNG_INFECTION] = RNG;
    }

    public RandomGenerator getInfectionRNG() {
        return (RandomGenerator) getFields()[FIELDS_META_RNG_INFECTION];
    }

    public int getCurrentLocation(AbstractIndividualInterface person) {
        @SuppressWarnings("unchecked")
		HashMap<Integer, Integer> map
                = (HashMap<Integer, Integer>) getFields()[FIELDS_META_POP_CURRENT_LOCATION];
        
        if(map.containsKey(person.getId())){
            return map.get(person.getId());            
        }else{
            return -1;
        }
    }

    public int isAwayFromHomeUntilAge(AbstractIndividualInterface person) {
        @SuppressWarnings("unchecked")
		HashMap<Integer, Integer> map
                = (HashMap<Integer, Integer>) getFields()[FIELDS_META_POP_AWAY_UNTIL_AGE];

        if (map.containsKey(person.getId())) {
            return map.get(person.getId());
        } else {
            return -1;
        }
    }    

    public void movePerson(AbstractIndividualInterface person, int locId, int utilAge) {
        @SuppressWarnings("unchecked")
		HashMap<Integer, Integer> mapCurrentLocation
                = (HashMap<Integer, Integer>) getFields()[FIELDS_META_POP_CURRENT_LOCATION];
        @SuppressWarnings("unchecked")
		HashMap<Integer, Integer> mapAwayUntilAge
                = (HashMap<Integer, Integer>) getFields()[FIELDS_META_POP_AWAY_UNTIL_AGE];                        
        
        mapCurrentLocation.put(person.getId(), locId);
        if(utilAge  > 0){
            mapAwayUntilAge.put(person.getId(), utilAge);
        }else if(mapAwayUntilAge.containsKey(person.getId())){
            mapAwayUntilAge.remove(person.getId());            
        }
        
        
    }

    public void removePersonFromPopulation(AbstractIndividualInterface person) {
        getLocalData().remove(person.getId());
        @SuppressWarnings("unchecked")
		HashMap<Integer, Integer> mapCurrentLocation
                = (HashMap<Integer, Integer>) getFields()[FIELDS_META_POP_CURRENT_LOCATION];
        @SuppressWarnings("unchecked")
		HashMap<Integer, Integer> mapAwayUntilAge
                = (HashMap<Integer, Integer>) getFields()[FIELDS_META_POP_AWAY_UNTIL_AGE];         

        mapCurrentLocation.remove(person.getId());      
        mapAwayUntilAge.remove(person.getId());

    }

}
