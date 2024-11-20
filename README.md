Java class package for IBM (Individual-Based Modeling) of meta-populations in remote Indigenous communities, developed by Ben Hui at the Kirby Institute.

This model has been utilized in various projects, including investigations into the impact of mobility on STI transmission, the dynamics of syphilis outbreaks, and the spread of COVID-19 in these communities.

**Running of model:**

To run the model, the typical approach is to execute the main method from the Simulation_Remote_MetaPopulation class, using a command such as:

  java sim.Simulation_Remote_MetaPopulation **_basedir_** **_other_arguments_**

where 
* **_basedir_**: The working directory for the model, containing the required PROP file (_simSpecificSim.prop_)
* **_other_arguments_**: Additional simulation-specific arguments (if needed).

The _simSpecificSim.prop_ is a XML file formatted according to the java.util.Properties class specifications. It uses methods such as loadFromXML(InputStream) and storeToXML(OutputStream, String, String) to define text-based settings for the model simulation.

Some general settings are described in the SimulationInterface class in the Package_BaseModel, while others are simulation-specific.

The behavior of the simulation depends on the settings in the simSpecificSim.prop file, specifically the value of the PROP_RMP_SIM_TYPE parameter:

* **PROP_RMP_SIM_TYPE = 0:** Simulates the transmission of gonorrhoea and chlamydia in remote Indigenous communities.
* **PROP_RMP_SIM_TYPE = 1:** Simulates the transmission of syphilis in remote Indigenous communities.
* **PROP_RMP_SIM_TYPE = 2:** Performs model optimisation using the Simplex method.
* **PROP_RMP_SIM_TYPE = 3:** Generates a population representing a remote community and stores the output as zip files.
* **PROP_RMP_SIM_TYPE = 4:** Performs model optimization using a genetic algorithm (GA).
* **PROP_RMP_SIM_TYPE = 5:** Simulates the transmission of COVID-19 in remote Indigenous communities.

As this model is no longer in active development, detailed documentation for the full usage of each simulation type is limited (and often unnecessary as it is largely replaced by later models). However, additional usage information is available upon request.

**Addtional info Syphilis model**

The project was funded to provide guidance to the Multi-Jurisdictional Syphilis Outbreak Working Group (MJSO) and the AHPPC Enhanced Response Governance Group (the Governance Group) to assist in the roll out of the national Enhanced Response and developing activity work plans in Aboriginal Community Controlled Health Services and other settings. The MJSO and Governance Group provided surveillance data and feedback on the modelling methods and characteristics of the scenarios implemented. The working group consists of the following members:  

**Governance Group:**  

Prof Brendan Murphy, Chief Medical Officer (CMO) for the Australian Government;  
Ms Celia Street, First Assistant Secretary, Office of Health Protection (OHP), Department of Health (DoH); 
Dr Nathan Ryder, Chair of Multi-jurisdictional Syphilis Outbreak (MJSO) Working Group; 
Dr Dawn Casey, National Aboriginal Community Controlled Health Organisation (NACCHO);  
Dr Russell Waddell, South Australia Department of Health; 
Dr Sonja Bennett, Queensland Department of Health; 
Dr Hugh Heggie, Northern Territory Department of Health;  
Dr Paul Armstrong, Western Australia Department of Health;
Prof James Ward, University of Queensland 
 
MJSO – represented organisations: 
Australian Government Department of Health 
National Aboriginal Community Controlled Health Organisation 
Aboriginal Medical Services Alliance Northern Territory 
Northern Territory Department of Health 
Queensland Aboriginal and Islander Health Council 
Queensland Department of Health 
Cairns and Hinterland Health and Hospital Service (Qld) 
Townsville Health and Hospital Service (Qld) 
Aboriginal Health Council of Western Australia 
Western Australia Department of Health 
Pilbara Public Health Unit (WA) 
Kimberley Public Health Unit (WA) 
Aboriginal Health Council of South Australia  
South Australia Health and Medical Research Institute 
South Australia Department of Health 
Kirby Institute, UNSW Sydney 
Victorian Department of Health and Human Services 
NSW Ministry of Health  
