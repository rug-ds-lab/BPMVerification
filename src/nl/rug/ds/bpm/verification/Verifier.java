package nl.rug.ds.bpm.verification;

import nl.rug.ds.bpm.event.listener.VerificationEventListener;
import nl.rug.ds.bpm.exception.ConfigurationException;
import nl.rug.ds.bpm.exception.SpecificationException;
import nl.rug.ds.bpm.exception.VerifierException;
import nl.rug.ds.bpm.log.LogEvent;
import nl.rug.ds.bpm.log.Logger;
import nl.rug.ds.bpm.net.TransitionGraph;
import nl.rug.ds.bpm.specification.jaxb.BPMSpecification;
import nl.rug.ds.bpm.specification.jaxb.Specification;
import nl.rug.ds.bpm.specification.jaxb.SpecificationSet;
import nl.rug.ds.bpm.specification.jaxb.SpecificationType;
import nl.rug.ds.bpm.specification.map.SpecificationTypeMap;
import nl.rug.ds.bpm.specification.marshaller.SpecificationUnmarshaller;
import nl.rug.ds.bpm.verification.checker.CheckerFactory;
import nl.rug.ds.bpm.verification.model.kripke.Kripke;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by p256867 on 4-4-2017.
 */
public class Verifier {
	private TransitionGraph net;
	private CheckerFactory checkerFactory;
	private BPMSpecification bpmSpecification;
	private SpecificationTypeMap specificationTypeMap;

    public Verifier(TransitionGraph net, CheckerFactory checkerFactory) {
    	this.checkerFactory = checkerFactory;
    	this.net = net;
    }
	
	public static int getMaximumStates() {
		return Kripke.getMaximumStates();
	}
	
	public void verify(File specification, boolean doReduction) throws VerifierException {
		specificationTypeMap = new SpecificationTypeMap();
		
		try {
			loadSpecification(specification);
			verify(doReduction);
		} catch (Exception e) {
			e.printStackTrace();
			throw new VerifierException("Verification failure");
		}
	}
	
	public void verify(String specxml, boolean doReduction) throws VerifierException {
		specificationTypeMap = new SpecificationTypeMap();
		
		try {
			loadSpecification(specxml);
			verify(doReduction);
		} catch (Exception e) {
			e.printStackTrace();
			throw new VerifierException("Verification failure");
		}
	}
	
	public void verify(BPMSpecification bpmSpecification, boolean doReduction) throws VerifierException {
		specificationTypeMap = new SpecificationTypeMap();
		
		this.bpmSpecification = bpmSpecification;
		try {
			verify(doReduction);
		} catch (Exception e) {
			e.printStackTrace();
			throw new VerifierException("Verification failure");
		}
	}
	
	public void verify(BPMSpecification bpmSpecification) throws VerifierException {
    	verify(bpmSpecification, true);
	}
	
	public void verify(File specification) throws VerifierException {
    	verify(specification, true);
	}
	
	public void verify(String specxml) throws VerifierException {
    	verify(specxml, true);
    }
	
	public void addEventListener(VerificationEventListener verificationEventListener) {
		checkerFactory.addEventListener(verificationEventListener);
	}

	public void removeEventListener(VerificationEventListener verificationEventListener) {
		checkerFactory.removeEventListener(verificationEventListener);
	}
	
	private void verify(boolean reduce) throws VerifierException {
		Logger.log("Loading configuration file", LogEvent.INFO);
		try {
			loadConfiguration();
		} catch (Exception e) {
			e.printStackTrace();
			throw new VerifierException("Verification failure");
		}
		
		Logger.log("Loading specification file", LogEvent.INFO);
		List<SetVerifier> verifiers = getSetVerifiers(bpmSpecification);
		
		Logger.log("Verifying specification sets", LogEvent.INFO);
		int setid = 0;
		for (SetVerifier verifier: verifiers) {
			Logger.log("Verifying set " + ++setid, LogEvent.INFO);
			try {
				verifier.buildKripke(reduce);
				verifier.verify(checkerFactory.getChecker());
			} catch (Exception e) {
				e.printStackTrace();
				throw new VerifierException("Verification failure");
			}
		}
	}
	
	private void loadConfiguration() throws ConfigurationException {
		try {
//			InputStream targetStream = new FileInputStream("./resources/specificationTypes.xml");
			
			SpecificationUnmarshaller unmarshaller = new SpecificationUnmarshaller(this.getClass().getResourceAsStream("/resources/specificationTypes.xml"));
			loadSpecificationTypes(unmarshaller.getSpecification(), specificationTypeMap);
		} catch (Exception e) {
			throw new ConfigurationException("Failed to load configuration file");
		}
	}
	
	private void loadSpecification(String specxml) throws SpecificationException {
		try {
			SpecificationUnmarshaller unmarshaller = new SpecificationUnmarshaller(new ByteArrayInputStream(specxml.getBytes("UTF-8")));
			bpmSpecification = unmarshaller.getSpecification();
		} catch (Exception e) {
			throw new SpecificationException("Invalid specification");
		}
	}
	
	private void loadSpecification(File specification) throws SpecificationException {
		if (!(specification.exists() && specification.isFile()))
			throw new SpecificationException("No such file " + specification.toString());
		
		try {
			SpecificationUnmarshaller unmarshaller = new SpecificationUnmarshaller(specification);
			bpmSpecification = unmarshaller.getSpecification();
		} catch (Exception e) {
			throw new SpecificationException("Invalid specification");
		}
	}
	
	private List<SetVerifier> getSetVerifiers(BPMSpecification specification) {
    	List<SetVerifier> verifiers = new ArrayList<>();

		loadSpecificationTypes(specification, specificationTypeMap);
		
		for(SpecificationSet specificationSet: specification.getSpecificationSets()) {
			SetVerifier setVerifier = new SetVerifier(net, specification, specificationSet);
			verifiers.add(setVerifier);
		}

		return verifiers;
    }
	
	public static void setMaximumStates(int max) {
		Kripke.setMaximumStates(max);
	}
	
	private void loadSpecificationTypes(BPMSpecification specification, SpecificationTypeMap typeMap) {
		for (SpecificationType specificationType: specification.getSpecificationTypes()) {
			typeMap.addSpecificationType(specificationType);
			Logger.log("Adding specification type " + specificationType.getId(), LogEvent.VERBOSE);
		}

		for (SpecificationSet set: specification.getSpecificationSets()) {
			for (Specification spec : set.getSpecifications()) {
				if (typeMap.getSpecificationType(spec.getType()) != null)
					spec.setSpecificationType(typeMap.getSpecificationType(spec.getType()));
				else
					Logger.log("No such specification type: " + spec.getType(), LogEvent.WARNING);
			}
		}
	}
}
