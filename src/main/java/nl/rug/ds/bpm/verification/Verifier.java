package nl.rug.ds.bpm.verification;

import nl.rug.ds.bpm.specification.jaxb.BPMSpecification;
import nl.rug.ds.bpm.specification.jaxb.Specification;
import nl.rug.ds.bpm.specification.jaxb.SpecificationSet;
import nl.rug.ds.bpm.specification.jaxb.SpecificationType;
import nl.rug.ds.bpm.specification.map.SpecificationTypeMap;
import nl.rug.ds.bpm.specification.marshaller.SpecificationUnmarshaller;
import nl.rug.ds.bpm.util.exception.ConfigurationException;
import nl.rug.ds.bpm.util.exception.SpecificationException;
import nl.rug.ds.bpm.util.exception.VerifierException;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.verification.event.EventHandler;
import nl.rug.ds.bpm.verification.event.listener.VerificationEventListener;
import nl.rug.ds.bpm.verification.model.kripke.Kripke;
import nl.rug.ds.bpm.verification.modelcheck.CheckerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * Created by p256867 on 4-4-2017.
 */
public abstract class Verifier {
	protected EventHandler eventHandler;
	protected CheckerFactory checkerFactory;
	protected BPMSpecification bpmSpecification;
	protected SpecificationTypeMap specificationTypeMap;

	public Verifier() {
		eventHandler = new EventHandler();
	}

	protected abstract void verify(boolean reduce) throws VerifierException;
	
	public void verify(File specification, boolean doReduction) {
		specificationTypeMap = new SpecificationTypeMap();
		
		try {
			loadSpecification(specification);
			new Thread(() -> {
				try {
					verify(doReduction);
				} catch (VerifierException e) {
					e.printStackTrace();
				}
			}).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void verify(String specxml, boolean doReduction) {
		specificationTypeMap = new SpecificationTypeMap();

		try {
			loadSpecification(specxml);
			new Thread(() -> {
				try {
					verify(doReduction);
				} catch (VerifierException e) {
					e.printStackTrace();
				}
			}).start();
		} catch (SpecificationException e) {
			e.printStackTrace();
		}
	}
	
	public void verify(BPMSpecification bpmSpecification, boolean doReduction) {
		specificationTypeMap = new SpecificationTypeMap();
		
		this.bpmSpecification = bpmSpecification;

		new Thread(() -> {
			try {
				verify(doReduction);
			} catch (VerifierException e) {
				e.printStackTrace();
			}
		}).start();
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

	public void removeEventListener(VerificationEventListener verificationEventListener) {
		eventHandler.removeEventListener(verificationEventListener);
	}

	protected void loadConfiguration() throws ConfigurationException {
		try {
//			InputStream targetStream = new FileInputStream("./resources/specificationTypes.xml");
			
			SpecificationUnmarshaller unmarshaller = new SpecificationUnmarshaller(this.getClass().getResourceAsStream("/specificationTypes.xml"));
			loadSpecificationTypes(unmarshaller.getSpecification(), specificationTypeMap);
		} catch (Exception e) {
			throw new ConfigurationException("Failed to load configuration file");
		}
	}

	protected void loadSpecification(String specxml) throws SpecificationException {
		try {
			SpecificationUnmarshaller unmarshaller = new SpecificationUnmarshaller(new ByteArrayInputStream(specxml.getBytes(StandardCharsets.UTF_8)));
			bpmSpecification = unmarshaller.getSpecification();
		} catch (Exception e) {
			throw new SpecificationException("Invalid specification");
		}
	}

	protected void loadSpecification(File specification) throws SpecificationException {
		if (!(specification.exists() && specification.isFile()))
			throw new SpecificationException("No such file " + specification.toString());
		
		try {
			SpecificationUnmarshaller unmarshaller = new SpecificationUnmarshaller(specification);
			bpmSpecification = unmarshaller.getSpecification();
		} catch (Exception e) {
			throw new SpecificationException("Invalid specification");
		}
	}

	protected void loadSpecificationTypes(BPMSpecification specification, SpecificationTypeMap typeMap) {
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

	public static int getMaximumStates() { return Kripke.getMaximumStates(); }
	
	public static void setMaximumStates(int max) {
		Kripke.setMaximumStates(max);
	}
}
