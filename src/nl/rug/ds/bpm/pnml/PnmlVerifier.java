package nl.rug.ds.bpm.pnml;

import nl.rug.ds.bpm.jaxb.specification.Condition;
import nl.rug.ds.bpm.jaxb.specification.Specification;
import nl.rug.ds.bpm.jaxb.specification.SpecificationSet;
import nl.rug.ds.bpm.pnml.listeners.VerificationEventListener;
import nl.rug.ds.bpm.pnml.listeners.VerificationLogListener;
import nl.rug.ds.bpm.pnml.marshallers.SpecificationUnmarshaller;
import nl.rug.ds.bpm.pnml.util.GroupMap;
import nl.rug.ds.bpm.pnml.util.IDMap;
import nl.rug.ds.bpm.pnml.util.SpecificationTypeMap;
import nl.rug.ds.bpm.verification.models.conditional.ConditionalKripke;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by p256867 on 4-4-2017.
 */
public class PnmlVerifier {
	private EventHandler eventHandler;
	private IDMap idMap;
	private GroupMap groupMap;
	private SpecificationTypeMap specificationTypeMap;
	
	private Set<ConditionalKripke> kripkeStructures;

    public PnmlVerifier() {
    	eventHandler = new EventHandler();
    	specificationTypeMap = new SpecificationTypeMap();
		kripkeStructures = new HashSet<>();
    }
	
    public PnmlVerifier(File pnml, File specification, File nusmv2) {
    	this();
		
		if(!(pnml.exists() && pnml.isFile()))
			eventHandler.logCritical("No such file " + pnml.toString());
		if(!(specification.exists() && specification.isFile()))
			eventHandler.logCritical("No such file " + specification.toString());
		if(!(nusmv2.exists() && nusmv2.isFile() && nusmv2.canExecute()))
			eventHandler.logCritical("Unable to call NuSMV2 binary at " + nusmv2.toString());
		
		eventHandler.logInfo("Loading configuration");
		loadConfiguration();
		
		eventHandler.logInfo("Loading specification");
		loadSpecification(specification);
		
		eventHandler.logInfo("Loading PNML");
		loadPnml(pnml);
		
		eventHandler.logInfo("Calling model checker");
		callModelChecker();
	}
    
	public void addEventListener(VerificationEventListener verificationEventListener) {
    	eventHandler.addEventListener(verificationEventListener);
	}
	
	public void addLogListener(VerificationLogListener verificationLogListener) {
    	eventHandler.addLogListener(verificationLogListener);
	}
		
	private void loadConfiguration() {
		SpecificationUnmarshaller unmarshaller = new SpecificationUnmarshaller(eventHandler, this.getClass().getResourceAsStream("/resources/specificationTypes.xml"));
		unmarshaller.loadSpecificationTypes(specificationTypeMap);
	}
	
	private void loadPnml(File pnml) {
    	
	}
	
	private void loadSpecification(File specification) {
		SpecificationUnmarshaller unmarshaller = new SpecificationUnmarshaller(eventHandler, specification);
		idMap = unmarshaller.getIdMap();
		groupMap = unmarshaller.getGroupMap();
		unmarshaller.loadSpecificationTypes(specificationTypeMap);
		
		for(SpecificationSet specificationSet: unmarshaller.getSpecificationSets()) {
			ConditionalKripke kripke = new ConditionalKripke(eventHandler, specificationSet);
			eventHandler.logInfo("Adding conditional set and model");
			
			eventHandler.logVerbose("Conditions: ");
			for(Condition condition: specificationSet.getConditions())
				eventHandler.logVerbose("\t" + condition.toString());
			
			eventHandler.logVerbose("Specifications:");
			for (Specification s: specificationSet.getSpecifications())
				eventHandler.logVerbose("\t" + specification.toString());
		}
    }
	
	private void callModelChecker() {
    	
	}
}
