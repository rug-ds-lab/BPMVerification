package nl.rug.ds.bpm.verification;

import nl.rug.ds.bpm.event.EventHandler;
import nl.rug.ds.bpm.event.listener.VerificationEventListener;
import nl.rug.ds.bpm.event.listener.VerificationLogListener;
import nl.rug.ds.bpm.specification.jaxb.BPMSpecification;
import nl.rug.ds.bpm.specification.jaxb.Specification;
import nl.rug.ds.bpm.specification.jaxb.SpecificationSet;
import nl.rug.ds.bpm.specification.jaxb.SpecificationType;
import nl.rug.ds.bpm.specification.map.SpecificationTypeMap;
import nl.rug.ds.bpm.specification.marshaller.SpecificationUnmarshaller;
import nl.rug.ds.bpm.verification.checker.CheckerFactory;
import nl.rug.ds.bpm.verification.model.kripke.Kripke;
import nl.rug.ds.bpm.verification.stepper.Marking;
import nl.rug.ds.bpm.verification.stepper.Stepper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
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
	private CheckerFactory checkerFactory;
	private BPMSpecification bpmSpecification;
	private SpecificationTypeMap specificationTypeMap;
	
	private Set<SetVerifier> kripkeStructures;

    public Verifier(Stepper stepper, CheckerFactory checkerFactory) {
    	this.checkerFactory = checkerFactory;
    	this.stepper = stepper;
    	eventHandler = new EventHandler();
    }
	
	public Verifier(Stepper stepper, CheckerFactory checkerFactory, EventHandler eventHandler) {
    	this(stepper, checkerFactory);
		this.eventHandler = eventHandler;
	}
	
	public void verify(File specification, boolean doReduction) {
		specificationTypeMap = new SpecificationTypeMap();
		kripkeStructures = new HashSet<>();
		
		if(!(specification.exists() && specification.isFile()))
			eventHandler.logCritical("No such file " + specification.toString());
		
		SpecificationUnmarshaller unmarshaller = new SpecificationUnmarshaller(eventHandler, specification);
		bpmSpecification = unmarshaller.getSpecification();
		
		verify(doReduction);
	}
	
	public void verify(String specxml, boolean doReduction) {
		specificationTypeMap = new SpecificationTypeMap();
		kripkeStructures = new HashSet<>();
		
		SpecificationUnmarshaller unmarshaller;
		try {
			unmarshaller = new SpecificationUnmarshaller(eventHandler, new ByteArrayInputStream(specxml.getBytes("UTF-8")));
			bpmSpecification = unmarshaller.getSpecification();
		} 
		catch (UnsupportedEncodingException e) {
			eventHandler.logCritical("Invalid specification xml");
			return;
		}
		
		verify(doReduction);
	}
	
	public void verify(BPMSpecification bpmSpecification, boolean doReduction) {
		specificationTypeMap = new SpecificationTypeMap();
		kripkeStructures = new HashSet<>();
		
		this.bpmSpecification = bpmSpecification;
		
		verify(doReduction);
	}

	public void verify(BPMSpecification bpmSpecification) {
    	verify(bpmSpecification, true);
	}
	
    public void verify(File specification) {
    	verify(specification, true);
	}
    
    public void verify(String specxml) {
    	verify(specxml, true);
    }
    
	public void addEventListener(VerificationEventListener verificationEventListener) {
    	eventHandler.addEventListener(verificationEventListener);
	}
	
	public void addLogListener(VerificationLogListener verificationLogListener) {
    	eventHandler.addLogListener(verificationLogListener);
	}
	
	public void removeEventListener(VerificationEventListener verificationEventListener) {
		eventHandler.removeEventListener(verificationEventListener);
	}
	
	public void removeLogListener(VerificationLogListener verificationLogListener) {
		eventHandler.removeLogListener(verificationLogListener);
	}

	private void verify(boolean reduce) {
		eventHandler.logInfo("Loading configuration file");
		loadConfiguration();

		eventHandler.logInfo("Loading specification file");
		List<SetVerifier> verifiers = loadSpecification(bpmSpecification);
		
		eventHandler.logInfo("Verifying specification sets");
		int setid = 0;
		for (SetVerifier verifier: verifiers) {
			eventHandler.logInfo("Verifying set " + ++setid);
			verifier.buildKripke(reduce);
			verifier.verify(checkerFactory.getChecker());
		}
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
	
	private List<SetVerifier> loadSpecification(BPMSpecification specification) {
    	List<SetVerifier> verifiers = new ArrayList<>();

		loadSpecificationTypes(specification, specificationTypeMap);
		
		for(SpecificationSet specificationSet: specification.getSpecificationSets()) {
			SetVerifier setVerifier = new SetVerifier(eventHandler, stepper.clone(), specification, specificationSet);
			verifiers.add(setVerifier);
		}

		return verifiers;
    }

	private void loadSpecificationTypes(BPMSpecification specification, SpecificationTypeMap typeMap) {
		for (SpecificationType specificationType: specification.getSpecificationTypes()) {
			typeMap.addSpecificationType(specificationType);
			eventHandler.logVerbose("Adding specification type " + specificationType.getId());
		}

		for (SpecificationSet set: specification.getSpecificationSets()) {
			for (Specification spec : set.getSpecifications()) {
				if (typeMap.getSpecificationType(spec.getType()) != null)
					spec.setSpecificationType(typeMap.getSpecificationType(spec.getType()));
				else
					eventHandler.logWarning("No such specification type: " + spec.getType());
			}
		}
	}
	
	
	public static void setMaximumTokensAtPlaces(int maximum) {
		Marking.setMaximumTokensAtPlaces(maximum);
	}
	
	public static int getMaximumTokensAtPlaces() {
    	return Marking.getMaximumTokensAtPlaces();
	}
	
	public static void setMaximumStates(int max) {
    	Kripke.setMaximumStates(max);
    }
	
	public static int getMaximumStates() { return Kripke.getMaximumStates(); }
	
	public static void setLogLevel(int logLevel) { EventHandler.setLogLevel(logLevel); }
	
	public static int getLogLevel() { return EventHandler.getLogLevel(); }
}
