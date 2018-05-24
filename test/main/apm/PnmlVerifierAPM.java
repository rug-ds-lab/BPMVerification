package main.apm;

import hub.top.petrinet.PetriNet;
import main.ExtPnmlStepper;
import nl.rug.ds.bpm.event.VerificationEvent;
import nl.rug.ds.bpm.event.listener.VerificationEventListener;
import nl.rug.ds.bpm.log.LogEvent;
import nl.rug.ds.bpm.log.Logger;
import nl.rug.ds.bpm.log.listener.VerificationLogListener;
import nl.rug.ds.bpm.specification.jaxb.BPMSpecification;
import nl.rug.ds.bpm.specification.marshaller.SpecificationUnmarshaller;
import nl.rug.ds.bpm.specification.parser.SetParser;
import nl.rug.ds.bpm.verification.Verifier;
import nl.rug.ds.bpm.verification.checker.CheckerFactory;
import nl.rug.ds.bpm.verification.checker.nusmv2.NuSMVFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Nick van Beest on 02-June-17.
 */
public class PnmlVerifierAPM implements VerificationEventListener, VerificationLogListener {
	private SetParser setParser;
	private CheckerFactory factory;
	private boolean reduce;
	private PetriNet pn;
	private boolean userFriendly;

	private String eventoutput;
	private List<String> feedback;
	private BPMSpecification bpmSpecification;
	
	private Set<String> conditions;
	private Set<String> transitionguards;
	
	public PnmlVerifierAPM(PetriNet pn, String nusmv2, boolean userFriendly) {
		reduce = true;
		this.userFriendly = userFriendly;
		
		setParser = new SetParser();

		//Implement listeners and
		//Add listeners to receive log notifications
		Logger.addLogListener(this);
		
		eventoutput = "";
		feedback = new ArrayList<String>();
		
		this.pn = pn;
		
		//Create the wanted model checker factory
		factory = new NuSMVFactory(new File(nusmv2));
		
		conditions = new HashSet<String>();
		transitionguards = new HashSet<String>();
	}

	public PnmlVerifierAPM(PetriNet pn, String specxml, String nusmv2, boolean userFriendly) {
		this(pn, nusmv2, userFriendly);
		
		addSpecificationFromXML(specxml);
	}
	
	public PnmlVerifierAPM(PetriNet pn, String specxml, String nusmv2, Set<String> conditions, Set<String> transitionguards, boolean userFriendly) {
		this(pn, specxml, nusmv2, userFriendly);
		
		this.conditions = conditions;
		this.transitionguards = transitionguards;
	}

	public PnmlVerifierAPM(PetriNet pn, String[] specifications, String nusmv2, boolean userFriendly) {
		this(pn, nusmv2, userFriendly);
		
		addSpecifications(specifications);
		
		bpmSpecification = getSpecifications();
	}
	
	public PnmlVerifierAPM(PetriNet pn, String[] specifications, String nusmv2, Set<String> conditions, Set<String> transitionguards, boolean userFriendly) {
		this(pn, specifications, nusmv2, userFriendly);

		this.conditions = conditions;
		this.transitionguards = transitionguards;
	}

		
	public List<String> verify() {
		return verify(false);
	}
	
	public List<String> verify(Boolean getAllOutput) {
		//Make step class for specific Petri net type
		ExtPnmlStepper stepper;
		
		if (bpmSpecification == null) {
			bpmSpecification = getSpecifications();
		}
		
		try {
			stepper = new ExtPnmlStepper(pn);
			
			stepper.setConditions(conditions);
			stepper.setTransitionGuards(transitionguards);
			
			//Make a verifier which uses that step class
			Verifier verifier = new Verifier(stepper, factory);
			verifier.addEventListener(this);
			//Start verification
			verifier.verify(bpmSpecification, reduce);
		} 
		catch (Exception e) {
			Logger.log("Verification failure", LogEvent.CRITICAL);
		}
		System.out.println(eventoutput);
		
		return feedback;
	}
	
	public void addSpecificationFromXML(String specxml) {
		SpecificationUnmarshaller unmarshaller;
		try {
			unmarshaller = new SpecificationUnmarshaller(new ByteArrayInputStream(specxml.getBytes("UTF-8")));
			bpmSpecification = unmarshaller.getSpecification();
		} catch (Exception e) {
			Logger.log("Invalid specification xml", LogEvent.CRITICAL);
			return;
		}
	}

	public void addSpecification(String line) {
		setParser.parse(line);
	}

	public void addSpecifications(String[] lines) {
		for (String line: lines)
			addSpecification(line);
	}

	public BPMSpecification getSpecifications() {
		return setParser.getSpecification();
	}
	
	public void setReduction(boolean reduce) {
		this.reduce = reduce;
	}

	public boolean getReduction() {
		return reduce;
	}

	public void setConditions(Set<String> conditions) {
		this.conditions = conditions;
	}
	
	public void setTransitionGuards(Set<String> transitionguards) {
		this.transitionguards = transitionguards;
	}
	
	//Set maximum amount of tokens at a single place
	//Safety feature, prevents infinite models
	//Standard value of 3
	public void setMaximumTokensAtPlaces(int amount) {
		Verifier.setMaximumTokensAtPlaces(amount);
	}

	public int getMaximumTokensAtPlaces() {
		return Verifier.getMaximumTokensAtPlaces();
	}

	//Set maximum size of state space
	//Safety feature, prevents memory issues
	//Standard value of 7 million
	//(equals models of 4 parallel branches with each 50 activities)
	//Lower if on machine with limited memory
	public void setMaximumStates(int amount) {
		Verifier.setMaximumStates(amount);
	}

	public int getMaximumStates() {
		return Verifier.getMaximumStates();
	}
	
	public int getLogLevel() {
		return Logger.getLogLevel();
	}
	
	//Set log level LogEvent.DEBUG to LogEvent.CRITICAL
	public void setLogLevel(int level) {
		Logger.setLogLevel(level);
	}

	//Listener implementations
	@Override
	public void verificationEvent(VerificationEvent event) {
		//Use for user feedback
		//Event returns: specification id, formula, type, result, and specification itself
		if (userFriendly) {
			feedback.add("Specification " + event.getId() + " evaluated " + event.getVerificationResult() + " for " + event.getFormula().getSpecification().getType() + "(" + event.getFormula().getOriginalFormula() + "}");
		}
		else {
			feedback.add(event.toString());
		}
	}
	
	@Override
	public void verificationLogEvent(LogEvent event) {
		//Use for log and textual user feedback
		eventoutput += "[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "] " + event.toString() + "\n";
	}
}
