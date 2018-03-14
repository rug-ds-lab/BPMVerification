package nl.rug.ds.bpm.verification;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import nl.rug.ds.bpm.exception.ConfigurationException;
import nl.rug.ds.bpm.exception.SpecificationException;
import nl.rug.ds.bpm.exception.VerifierException;
import nl.rug.ds.bpm.log.LogEvent;
import nl.rug.ds.bpm.log.Logger;
import nl.rug.ds.bpm.specification.jaxb.BPMSpecification;
import nl.rug.ds.bpm.specification.jaxb.Specification;
import nl.rug.ds.bpm.specification.jaxb.SpecificationSet;
import nl.rug.ds.bpm.specification.jaxb.SpecificationType;
import nl.rug.ds.bpm.specification.map.SpecificationTypeMap;
import nl.rug.ds.bpm.specification.marshaller.SpecificationUnmarshaller;
import nl.rug.ds.bpm.verification.model.kripke.Kripke;
import nl.rug.ds.bpm.verification.stepper.Marking;
import nl.rug.ds.bpm.verification.stepper.Stepper;

/**
 * Created by Nick van Beest on 14 March 2018.
 */
public class PerformanceTestVerifier {
	private Stepper stepper;
	private BPMSpecification bpmSpecification;
	private SpecificationTypeMap specificationTypeMap;
	
    public PerformanceTestVerifier(Stepper stepper) {
    	this.stepper = stepper;
    }
	
	public static int getMaximumStates() {
		return Kripke.getMaximumStates();
	}
	
	public String verify(String specxml, boolean doReduction) throws VerifierException {
		specificationTypeMap = new SpecificationTypeMap();
		
		try {
			loadSpecification(specxml);
			return verify(doReduction);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return "";
	}
	
	private String verify(boolean reduce) throws VerifierException {
		String result = "";
		try {
			loadConfiguration();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		
		List<SetVerifier> verifiers = getSetVerifiers(bpmSpecification);
		
		for (SetVerifier verifier: verifiers) {
			try {
				verifier.buildKripke(reduce);
				
				result += verifier.getKripke().getStateCount() + " " + verifier.getKripke().getRelationCount() + " " + verifier.getKripke().getAtomicPropositions().size();
			} 
			catch (Exception e) {
				e.printStackTrace();
				throw new VerifierException("Verification failure");
			}
		}
		
		return result;
	}
	
	private void loadConfiguration() throws ConfigurationException {
		try {
			InputStream targetStream = new FileInputStream("./resources/specificationTypes.xml");
			
			SpecificationUnmarshaller unmarshaller = new SpecificationUnmarshaller(targetStream); // this.getClass().getResourceAsStream("/resources/specificationTypes.xml"));
			loadSpecificationTypes(unmarshaller.getSpecification(), specificationTypeMap);
		} 
		catch (Exception e) {
			throw new ConfigurationException("Failed to load configuration file");
		}
	}
	
	private void loadSpecification(String specxml) throws SpecificationException {
		try {
			InputStream targetStream = new FileInputStream(specxml);

			SpecificationUnmarshaller unmarshaller = new SpecificationUnmarshaller(targetStream);
//			SpecificationUnmarshaller unmarshaller = new SpecificationUnmarshaller(new ByteArrayInputStream(specxml.getBytes("UTF-8")));
			bpmSpecification = unmarshaller.getSpecification();
		} catch (Exception e) {
			e.printStackTrace();
			throw new SpecificationException("Invalid specification");
		}
	}
	
	private List<SetVerifier> getSetVerifiers(BPMSpecification specification) {
    	List<SetVerifier> verifiers = new ArrayList<>();

		loadSpecificationTypes(specification, specificationTypeMap);
		
		for(SpecificationSet specificationSet: specification.getSpecificationSets()) {
			SetVerifier setVerifier = new SetVerifier(stepper.clone(), specification, specificationSet);
			verifiers.add(setVerifier);
		}

		return verifiers;
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
