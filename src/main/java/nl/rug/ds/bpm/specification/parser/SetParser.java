package nl.rug.ds.bpm.specification.parser;

import nl.rug.ds.bpm.specification.jaxb.*;
import nl.rug.ds.bpm.specification.marshaller.SpecificationUnmarshaller;
import nl.rug.ds.bpm.util.exception.ConfigurationException;

import java.util.HashMap;


/**
 * Created by Heerko Groefsema on 29-May-17.
 */
public class SetParser {
    private HashMap<String, SpecificationType> specificationTypeMap;
    private BPMSpecification bpmSpecification;
    private SpecificationSet specificationSet;
    private Parser parser;
	private int id = 0;
	
	public SetParser() {
        specificationTypeMap = new HashMap<>();
		
		parser = new Parser(specificationTypeMap);
		bpmSpecification = new BPMSpecification();
		specificationSet = new SpecificationSet();
		bpmSpecification.addSpecificationSet(specificationSet);
		
		try {
			loadConfiguration();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void parse(String string) {
		if(string.toLowerCase().startsWith("group"))
			addGroup(string);
		else
			addSpecification(string);
	}
	
	private void addSpecification(String specification) {
		Specification spec = parser.parseSpecification(specification);
		if(spec != null) {
			spec.setId("parsed" + id++);
			specificationSet.addSpecification(spec);
		}
	}
	
	private void addGroup(String group) {
		Group g = parser.parseGroup(group);
		if(group != null)
			bpmSpecification.addGroup(g);
	}
	
	public BPMSpecification getSpecification() {
		return bpmSpecification;
	}
	
	private void loadConfiguration() throws ConfigurationException {
		try {
//			InputStream targetStream = new FileInputStream("./resources/specificationTypes.xml");
			
			SpecificationUnmarshaller unmarshaller = new SpecificationUnmarshaller(this.getClass().getResourceAsStream("/specificationTypes.xml"));
			loadSpecificationTypes(unmarshaller.getSpecification(), specificationTypeMap);
		} catch (Exception e) {
			throw new ConfigurationException("Failed to load configuration file");
		}
	}

    private void loadSpecificationTypes(BPMSpecification specification, HashMap<String, SpecificationType> typeMap) {
        for (SpecificationType specificationType : specification.getSpecificationTypes())
            typeMap.put(specificationType.getId(), specificationType);
    }
}
