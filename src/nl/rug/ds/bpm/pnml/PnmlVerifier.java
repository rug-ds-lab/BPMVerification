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

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by p256867 on 4-4-2017.
 */
public class PnmlVerifier {
	private EventHandler eventHandler;
	private IDMap idMap;
	private GroupMap groupMap;
	private SpecificationTypeMap specificationTypeMap;
	
	private Set<SpecificationSetVerifier> kripkeStructures;

    public PnmlVerifier() {
    	eventHandler = new EventHandler();
    	specificationTypeMap = new SpecificationTypeMap();
		kripkeStructures = new HashSet<>();
    }
	
    public void verify(File pnml, File specification, File nusmv2) {
		if(!(pnml.exists() && pnml.isFile()))
			eventHandler.logCritical("No such file " + pnml.toString());
		if(!(specification.exists() && specification.isFile()))
			eventHandler.logCritical("No such file " + specification.toString());
		if(!(nusmv2.exists() && nusmv2.isFile() && nusmv2.canExecute()))
			eventHandler.logCritical("Unable to call NuSMV2 binary at " + nusmv2.toString());

		eventHandler.logInfo("Loading configuration");
		loadConfiguration();
		
		eventHandler.logInfo("Loading specification");
		List<SpecificationSetVerifier> verifiers = loadSpecification(specification);
		
		eventHandler.logInfo("Loading PNML");
		for (SpecificationSetVerifier verifier: verifiers)
			verifier.buildKripke(pnml);
		
		eventHandler.logInfo("Verifying specification sets");
		for (SpecificationSetVerifier verifier: verifiers)
			verifier.verify(nusmv2);
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
	
	private List<SpecificationSetVerifier> loadSpecification(File specification) {
    	List<SpecificationSetVerifier> verifiers = new ArrayList<>();

		SpecificationUnmarshaller unmarshaller = new SpecificationUnmarshaller(eventHandler, specification);
		idMap = unmarshaller.getIdMap();
		groupMap = unmarshaller.getGroupMap(idMap);
		unmarshaller.loadSpecificationTypes(specificationTypeMap);
		
		for(SpecificationSet specificationSet: unmarshaller.getSpecificationSets()) {
			SpecificationSetVerifier setVerifier = new SpecificationSetVerifier(eventHandler, specificationSet, idMap, groupMap);
			verifiers.add(setVerifier);
			eventHandler.logInfo("Adding conditional set and model");
			
			eventHandler.logVerbose("Conditions: ");
			for(Condition condition: specificationSet.getConditions())
				eventHandler.logVerbose("\t" + condition.getCondition());
			
			eventHandler.logVerbose("Specifications:");
			for (Specification s: specificationSet.getSpecifications())
				for(String formula: s.getFormulas())
				eventHandler.logVerbose("\t" + formula);
		}

		return verifiers;
    }
}
