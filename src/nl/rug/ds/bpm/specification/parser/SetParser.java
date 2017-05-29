package nl.rug.ds.bpm.specification.parser;

import nl.rug.ds.bpm.event.EventHandler;
import nl.rug.ds.bpm.event.listener.VerificationLogListener;
import nl.rug.ds.bpm.specification.jaxb.*;
import nl.rug.ds.bpm.specification.map.SpecificationTypeMap;
import nl.rug.ds.bpm.specification.marshaller.SpecificationUnmarshaller;


/**
 * Created by Heerko Groefsema on 29-May-17.
 */
public class SetParser {
	private EventHandler eventHandler;
	private SpecificationTypeMap specificationTypeMap;
	private BPMSpecification bpmSpecification;
	private SpecificationSet specificationSet;
	private Parser parser;
	
	public SetParser() {
		eventHandler = new EventHandler();
		specificationTypeMap = new SpecificationTypeMap();
		
		parser = new Parser(eventHandler, specificationTypeMap);
		bpmSpecification = new BPMSpecification();
		specificationSet = new SpecificationSet();
		bpmSpecification.addSpecificationSet(specificationSet);
		
		loadConfiguration();
	}
	
	public void parse(String string) {
		if(string.toLowerCase().startsWith("group"))
			addGroup(string);
		else
			addSpecification(string);
	}
	
	private void addSpecification(String specification) {
		Specification spec = parser.parseSpecification(specification);
		if(spec != null)
			specificationSet.addSpecification(spec);
	}
	
	private void addGroup(String group) {
		Group g = parser.parseGroup(group);
		if(group != null)
			bpmSpecification.addGroup(g);
	}
	
	public BPMSpecification getSpecification() {
		return bpmSpecification;
	}
	
	public void addLogListener(VerificationLogListener verificationLogListener) {
		eventHandler.addLogListener(verificationLogListener);
	}
	
	public void removeLogListener(VerificationLogListener verificationLogListener) {
		eventHandler.removeLogListener(verificationLogListener);
	}
	
	private void loadConfiguration() {
//		try {
//			InputStream targetStream = new FileInputStream("./resources/specificationTypes.xml");
		
		SpecificationUnmarshaller unmarshaller = new SpecificationUnmarshaller(eventHandler, this.getClass().getResourceAsStream("/resources/specificationTypes.xml"));
		loadSpecificationTypes(unmarshaller.getSpecification(), specificationTypeMap);
//		}
//		catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
	}
	
	private void loadSpecificationTypes(BPMSpecification specification, SpecificationTypeMap typeMap) {
		for (SpecificationType specificationType: specification.getSpecificationTypes())
			typeMap.addSpecificationType(specificationType);
	}
}
