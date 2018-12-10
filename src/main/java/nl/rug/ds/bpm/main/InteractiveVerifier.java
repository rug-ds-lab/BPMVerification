package nl.rug.ds.bpm.main;

import nl.rug.ds.bpm.petrinet.interfaces.net.VerifiableNet;
import nl.rug.ds.bpm.petrinet.ptnet.PlaceTransitionNet;
import nl.rug.ds.bpm.pnml.ptnet.jaxb.ptnet.Net;
import nl.rug.ds.bpm.pnml.ptnet.marshaller.PTNetUnmarshaller;
import nl.rug.ds.bpm.specification.jaxb.BPMSpecification;
import nl.rug.ds.bpm.specification.marshaller.SpecificationMarshaller;
import nl.rug.ds.bpm.specification.parser.SetParser;
import nl.rug.ds.bpm.util.exception.SpecificationException;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.util.log.listener.VerificationLogListener;
import nl.rug.ds.bpm.verification.NetVerifier;
import nl.rug.ds.bpm.verification.event.VerificationEvent;
import nl.rug.ds.bpm.verification.event.listener.VerificationEventListener;
import nl.rug.ds.bpm.verification.modelcheck.Checker;
import nl.rug.ds.bpm.verification.modelcheck.CheckerFactory;
import nl.rug.ds.bpm.verification.modelcheck.nusmv2.NuSMVFactory;

import java.io.File;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * Created by Heerko Groefsema on 07-Apr-17.
 */
public class InteractiveVerifier implements VerificationEventListener, VerificationLogListener {
	private SetParser setParser;
	private CheckerFactory factory;
	private boolean reduce;

	public static void main(String[] args) {
		if (args.length > 2) {
			InteractiveVerifier pnmlVerifier = new InteractiveVerifier(args[2]);
			pnmlVerifier.setLogLevel(LogEvent.INFO);
			if (args.length > 3)
				Checker.setOutputPath(args[3]);
			if (args.length > 4)
				pnmlVerifier.setReduction(Boolean.parseBoolean(args[4]));

			pnmlVerifier.verify(args[0], args[1]);
		} else {
			System.out.println("Usage: PNMLVerifier PNML_file Specification_file NuSMV2_binary_path");
		}
	}

	public InteractiveVerifier() {
		reduce = true;
		setParser = new SetParser();
		
		//Implement listeners and
		//Add listeners to receive log notifications
		Logger.addLogListener(this);
	}
	
	public InteractiveVerifier(File nusmv2) {
		this();
		
		//Create the wanted model modelcheck factory
		factory = new NuSMVFactory(nusmv2);
	}

	public InteractiveVerifier(String nusmv2) {
		this(new File(nusmv2));
	}
	
	public InteractiveVerifier(VerifiableNet pn, BPMSpecification specification, String nusmv2) {
		this(nusmv2);
		verify(pn, specification);
	}

	public void verify(String pnml, String specification) {
		try {
			//Load net(s) from pnml file
			PTNetUnmarshaller pnu = new PTNetUnmarshaller(new File(pnml));
			Set<Net> pnset = pnu.getNets();
			//Create Petri net object from the first pnml net
			PlaceTransitionNet pn = new PlaceTransitionNet(pnset.iterator().next());
			//DataDrivenNet pn = new DataDrivenNet(pnset.iterator().next());

			//Make a verifier
			NetVerifier verifier = new NetVerifier(pn, factory);
			verifier.addEventListener(this);
			//Start verification
			verifier.verify(new File(specification), reduce);
		} catch (Exception e) {
			Logger.log("Verification failure", LogEvent.CRITICAL);
		}
	}

	public void verify(VerifiableNet pn, BPMSpecification specification) {
		try {
			//Make a verifier
			NetVerifier verifier = new NetVerifier(pn, factory);
			verifier.addEventListener(this);
			//Start verification
			verifier.verify(specification, reduce);
		} catch (Exception e) {
			Logger.log("Verification failure", LogEvent.CRITICAL);
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
	
	public void saveSpecification(File file) throws SpecificationException {
		SpecificationMarshaller marshaller = new SpecificationMarshaller(getSpecifications(), file);
	}
	
	public void saveSpecification(OutputStream stream) throws SpecificationException {
		SpecificationMarshaller marshaller = new SpecificationMarshaller(getSpecifications(), stream);
	}

	public void setReduction(boolean reduce) {
		this.reduce = reduce;
	}

	public boolean getReduction() {
		return reduce;
	}

	//Set maximum size of state space
	//Safety feature, prevents memory issues
	//Standard value of 7 million
	//(equals models of 4 parallel branches with each 50 activities)
	//Lower if on machine with limited memory
	public void setMaximumStates(int amount) {
		NetVerifier.setMaximumStates(amount);
	}

	public int getMaximumStates() {
		return NetVerifier.getMaximumStates();
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
		System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "] FEEDBACK: " + event.toString());
	}
	
	@Override
	public void verificationLogEvent(LogEvent event) {
		//Use for log and textual user feedback
		System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "] " + event.toString());
	}
}
