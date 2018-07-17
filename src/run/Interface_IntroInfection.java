
package run;

/**
 * An interface to define share method for Run_Population_Remote_MetaPopulation_Pop_Intro_NG_CT and Run_Population_Remote_MetaPopulation_Pop_Intro_Syphilis
 * 
 * @author Ben Hui
 * @version  20180713
 */
public interface Interface_IntroInfection {
    
    public String[] getThreadParamValStr();
    public double[] getRunParamValues();
    public String[] getPopParamValStr();
    public void setPopParamValStr(int index, String ent);
    
    
}
