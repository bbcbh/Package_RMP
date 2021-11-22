/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package person;

import infection.AbstractInfection;
import java.io.Serializable;
import java.util.Arrays;
import util.LongFieldsInterface;
import util.Indivdual_PartnerHistoryInterface;

/**
 *
 * @author Ben Hui
 */
public class Person_Remote_MetaPopulation implements AbstractIndividualInterface,
        Serializable, LongFieldsInterface, TreatablePersonInterface, Indivdual_PartnerHistoryInterface, MoveablePersonInterface {

    /**
	 * 
	 */
	private static final long serialVersionUID = 938173184440940632L;
	public static final int PERSON_ID = 0;
    public static final int PERSON_GENDER = PERSON_ID + 1;
    public static final int PERSON_ENTER_POP_AT = PERSON_GENDER + 1;
    public static final int PERSON_CURRENT_AGE = PERSON_ENTER_POP_AT + 1;
    public static final int PERSON_FIRST_SEEK_PARTNER_AGE = PERSON_CURRENT_AGE + 1;    
    public static final int PERSON_FIRST_SEX_AGE = PERSON_FIRST_SEEK_PARTNER_AGE + 1;
    public static final int PERSON_HOME_LOC = PERSON_FIRST_SEX_AGE + 1;
    public final int PERSON_FIELD_LEN = PERSON_HOME_LOC + 1;

    protected long[] compfields = new long[PERSON_FIELD_LEN];
    protected int[] timeUntilNextStage;
    protected int[] infectionStatus;
    protected int[] lastInfectedAtAge;
    protected int[] lastTreatmentAtAge;
    protected boolean[] lastActInfected;

    // Indivdual_PartnerHistoryInterface
    protected int[] partnerHistoryLifetimePID = new int[0];
    protected int[] partnerHistoryLifetimeAtAge = new int[0];
    protected int[] partnerHistoryRelationshipLength = new int[0];
    protected int partnerHistoryLifetimePt = 0;

    public Person_Remote_MetaPopulation(int id, boolean isMale, int enterPopAt, int age,
            int firstSeekPartnerAge, int numInfections, int homeLoc) {
        compfields[PERSON_ID] = id;
        compfields[PERSON_GENDER] = isMale ? 0 : 1;
        compfields[PERSON_ENTER_POP_AT] = enterPopAt;
        compfields[PERSON_CURRENT_AGE] = age;
        compfields[PERSON_FIRST_SEEK_PARTNER_AGE] = firstSeekPartnerAge;
        compfields[PERSON_FIRST_SEX_AGE] = -1;
        compfields[PERSON_HOME_LOC] = homeLoc;

        setNumberOfInfections(numInfections);
    }

    public final void setNumberOfInfections(int numInfections) {
        infectionStatus = new int[numInfections];
        timeUntilNextStage = new int[numInfections];
        lastActInfected = new boolean[numInfections];
        lastInfectedAtAge = new int[numInfections];
        lastTreatmentAtAge = new int[numInfections];

        Arrays.fill(infectionStatus, -1);
        Arrays.fill(timeUntilNextStage, -1);
        Arrays.fill(lastActInfected, false);
        Arrays.fill(lastInfectedAtAge, -1);
        Arrays.fill(lastTreatmentAtAge, -1);
    }

    @Override
    public boolean isMale() {
        return compfields[PERSON_GENDER] == 0;
    }

    @Override
    public double getAge() {
        return compfields[PERSON_CURRENT_AGE];
    }

    @Override
    public int getId() {
        return (int) compfields[PERSON_ID];
    }

    @Override
    public int[] getInfectionStatus() {
        return infectionStatus;
    }

    @Override
    public int getInfectionStatus(int index) {
        return infectionStatus[index];
    }

    @Override
    public double getLastInfectedAtAge(int infectionIndex) {
        return lastInfectedAtAge[infectionIndex];
    }

    @Override
    public Comparable<?> getParameter(String id) {
        try {
            return getParameter(Integer.parseInt(id));
        } catch (NumberFormatException ex) {
            throw new UnsupportedOperationException(getClass().getName()
                    + ".getParameter: '" + id + "' not support yet. Try getFields() instead.");
        }
    }

    public long getParameter(int id) {
        return compfields[id];
    }

    @Override
    public Comparable<?> setParameter(String id, Comparable<?> value) {
        try {
            int idN = Integer.parseInt(id);
            long valueN = ((Number) value).longValue();
            return setParameter(idN, valueN);
        } catch (NumberFormatException ex) {
            throw new UnsupportedOperationException(getClass().getName()
                    + ".setParameter: '" + id + "' not support yet. Try setting using getFields() instead.");
        }

    }

    public long setParameter(int id, long value) {
        long org = compfields[id];
        compfields[id] = value;
        return org;
    }

    @Override
    public double getTimeUntilNextStage(int index) {
        return timeUntilNextStage[index];
    }

    @Override
    public void setAge(double age) {
        setParameter(PERSON_CURRENT_AGE, (int) age);
    }

    @Override
    public void setInfectionStatus(int index, int newInfectionStatus) {
        infectionStatus[index] = newInfectionStatus;
    }

    @Override
    public void setLastActInfectious(int infectionIndex, boolean lastActInf) {
        lastActInfected[infectionIndex] = lastActInf;
    }

    @Override
    public void setTimeUntilNextStage(int index, double newTimeUntilNextStage) {
        timeUntilNextStage[index] = (int) newTimeUntilNextStage;
    }

    @Override
    public int getEnterPopulationAt() {
        return (int) compfields[PERSON_ENTER_POP_AT];
    }

    @Override
    public double getStartingAge() {
        return (int) compfields[PERSON_FIRST_SEEK_PARTNER_AGE];
    }

    @Override
    public void setEnterPopulationAt(int enterPopulationAt) {
        compfields[PERSON_ENTER_POP_AT] = enterPopulationAt;
    }

    @Override
    public void setLastInfectedAtAge(int infectionIndex, double age) {
        lastInfectedAtAge[infectionIndex] = (int) age;
    }

    @Override
    public long[] getFields() {
        return compfields;
    }

    @Override
    public void setFields(long[] newFields) {
        compfields = newFields;
    }

    @Override
    public int getLastTreatedAt() {
        int lastTreatMax = -1;
        for (int lastTr : lastTreatmentAtAge) {
            lastTreatMax = Math.max(lastTreatMax, lastTr);
        }
        return lastTreatMax;
    }

    public int getLastTreatedAt(int infectionId) {
        return lastTreatmentAtAge[infectionId];
    }

    @Override
    public void setLastTreatedAt(int lastTreatedAt) {
        Arrays.fill(lastTreatmentAtAge, lastTreatedAt);
    }

    public void setLastTreatedAt(int infectionId, int lastTreatedAt) {
        lastTreatmentAtAge[infectionId] = lastTreatedAt;
    }

    /**
     * Age the person by deltaT days.
     *
     * @param deltaT
     * @param infectionList
     * @return -1 if the person have yet to reach sexual debut age
     */
    @Override
    public int incrementTime(int deltaT, AbstractInfection[] infectionList) {
        // Ageing               
        compfields[PERSON_CURRENT_AGE] += deltaT;

        // Changing infection status
        if (compfields[PERSON_FIRST_SEEK_PARTNER_AGE] > compfields[PERSON_CURRENT_AGE]) {
            return -1;
        } else {
            int timeSkip = Integer.MAX_VALUE;

            for (int INF_ID = 0; INF_ID < infectionList.length; INF_ID++) {
                if (getInfectionStatus(INF_ID) != AbstractIndividualInterface.INFECT_S) {
                    if (timeUntilNextStage[INF_ID] > 0) {
                        timeUntilNextStage[INF_ID] -= deltaT;
                    }
                    // Self-progress
                    if (timeUntilNextStage[INF_ID] <= 0) {
                        timeSkip = Math.min((int) infectionList[INF_ID].advancesState(this), timeSkip);
                    }
                }
                // Infection from last act
                if (lastActInfected[INF_ID]) {
                    timeSkip = Math.min((int) infectionList[INF_ID].infecting(this), timeSkip);
                    if (getInfectionStatus(INF_ID) != AbstractIndividualInterface.INFECT_S) {
                        lastInfectedAtAge[INF_ID] = (int) compfields[PERSON_CURRENT_AGE];
                    }
                    lastActInfected[INF_ID] = false;
                }
            }
            timeSkip = Math.min(deltaT, timeSkip);
            return timeSkip;
        }

    }

    @Override
    public int[] getPartnerHistoryLifetimePID() {
        return partnerHistoryLifetimePID;
    }

    @Override
    public int[] getPartnerHistoryLifetimeAtAge() {
        return partnerHistoryLifetimeAtAge;
    }

    @Override
    public int[] getPartnerHistoryRelLength() {
        return partnerHistoryRelationshipLength;
    }

    @Override
    public int getPartnerHistoryLifetimePt() {
        return partnerHistoryLifetimePt;
    }

    @Override
    public void setPartnerHistoryLifetimePt(int partnerHistoryLifetimePt) {
        this.partnerHistoryLifetimePt = partnerHistoryLifetimePt;
    }

    @Override
    public void addPartnerAtAge(int age, int partnerId, int relLength) {
        ensureHistoryLength(getPartnerHistoryLifetimePt() + 1);
        partnerHistoryLifetimeAtAge[getPartnerHistoryLifetimePt()] = age;
        partnerHistoryLifetimePID[getPartnerHistoryLifetimePt()] = partnerId;
        partnerHistoryRelationshipLength[getPartnerHistoryLifetimePt()] = relLength;
        partnerHistoryLifetimePt++;
    }

    @Override
    public void ensureHistoryLength(int ensuredHistoryLength) {
        if (partnerHistoryLifetimePID.length < ensuredHistoryLength) {
            partnerHistoryLifetimePID = Arrays.copyOf(partnerHistoryLifetimePID, ensuredHistoryLength);
            partnerHistoryLifetimeAtAge = Arrays.copyOf(partnerHistoryLifetimeAtAge, ensuredHistoryLength);
            partnerHistoryRelationshipLength = Arrays.copyOf(partnerHistoryRelationshipLength, ensuredHistoryLength);
        }
    }

    @Override
    public int numPartnerFromAge(double ageToCheck) {
        // Including current partner 
        int count = 0;
        for (int i = partnerHistoryLifetimePt - 1; i >= 0; i--) {
            if (partnerHistoryLifetimeAtAge[i] >= ageToCheck
                    || (partnerHistoryLifetimeAtAge[i] + partnerHistoryRelationshipLength[i] >= ageToCheck)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int getNumPartnerInPastYear() {
        return numPartnerFromAge(getAge() - ONE_YEAR_INT);
    }

    @Override
    public void copyPartnerHistory(Indivdual_PartnerHistoryInterface clone) {
        partnerHistoryLifetimePID = Arrays.copyOf(clone.getPartnerHistoryLifetimePID(), clone.getPartnerHistoryLifetimePID().length);
        partnerHistoryLifetimeAtAge = Arrays.copyOf(clone.getPartnerHistoryLifetimeAtAge(), clone.getPartnerHistoryLifetimeAtAge().length);
        partnerHistoryRelationshipLength = Arrays.copyOf(clone.getPartnerHistoryRelLength(), clone.getPartnerHistoryRelLength().length);
        partnerHistoryLifetimePt = clone.getPartnerHistoryLifetimePt();
    }   

    @Override
    public int getHomeLocation() {
        return (int) getFields()[PERSON_HOME_LOC];
    }



}
