package nl.rug.ds.bpm.verification;

import nl.rug.ds.bpm.specification.jaxb.SpecificationSet;
import nl.rug.ds.bpm.verification.listener.VerificationEventListener;
import nl.rug.ds.bpm.verification.listener.VerificationLogListener;
import nl.rug.ds.bpm.specification.marshaller.SpecificationUnmarshaller;
import nl.rug.ds.bpm.verification.stepper.Stepper;
import nl.rug.ds.bpm.verification.event.EventHandler;
import nl.rug.ds.bpm.verification.map.SpecificationTypeMap;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by p256867 on 4-4-2017.
 */
public class Verifier {
	private EventHandler eventHandler;
	private Stepper stepper;
	private SpecificationTypeMap specificationTypeMap;
	
	private Set<SetVerifier> kripkeStructures;

    public Verifier(Stepper stepper) {
    	this.stepper = stepper;
    	eventHandler = new EventHandler();
    	specificationTypeMap = new SpecificationTypeMap();
		kripkeStructures = new HashSet<>();
    }
	
    public void verify(File specification, File nusmv2) {
		if(!(specification.exists() && specification.isFile()))
			eventHandler.logCritical("No such file " + specification.toString());
		if(!(nusmv2.exists() && nusmv2.isFile() && nusmv2.canExecute()))
			eventHandler.logCritical("Unable to call NuSMV2 binary at " + nusmv2.toString());

		eventHandler.logInfo("Loading configuration");
		loadConfiguration();
		
		eventHandler.logInfo("Loading specification");
		List<SetVerifier> verifiers = loadSpecification(specification);
		
		eventHandler.logInfo("Loading PNML");
		for (SetVerifier verifier: verifiers)
			verifier.buildKripke();
		
		eventHandler.logInfo("Verifying specification sets");
		for (SetVerifier verifier: verifiers)
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
	
	private List<SetVerifier> loadSpecification(File specification) {
    	List<SetVerifier> verifiers = new ArrayList<>();

		SpecificationUnmarshaller unmarshaller = new SpecificationUnmarshaller(eventHandler, specification);
		unmarshaller.loadSpecificationTypes(specificationTypeMap);
		
		for(SpecificationSet specificationSet: unmarshaller.getSpecificationSets()) {
			SetVerifier setVerifier = new SetVerifier(eventHandler, stepper, unmarshaller.getSpecification(), specificationSet);
			verifiers.add(setVerifier);
		}

		return verifiers;
    }
}
