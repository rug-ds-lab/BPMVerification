package nl.rug.ds.bpm.verification;

import nl.rug.ds.bpm.petrinet.interfaces.net.VerifiableNet;
import nl.rug.ds.bpm.specification.jaxb.BPMSpecification;
import nl.rug.ds.bpm.specification.jaxb.SpecificationSet;
import nl.rug.ds.bpm.util.exception.VerifierException;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.verification.modelcheck.Checker;
import nl.rug.ds.bpm.verification.modelcheck.CheckerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by p256867 on 4-4-2017.
 */
public class NetVerifier extends Verifier {
	private VerifiableNet net;

    public NetVerifier(VerifiableNet net, CheckerFactory checkerFactory) {
    	super();
    	this.checkerFactory = checkerFactory;
    	this.net = net;
    }
	
	protected void verify(boolean reduce) throws VerifierException {
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
				Checker checker = checkerFactory.getChecker();
				verifier.verify(checker);
				checkerFactory.release(checker);

			} catch (Exception e) {
				e.printStackTrace();
				throw new VerifierException("Verification failure");
			}
		}
	}
	
	private List<SetVerifier> getSetVerifiers(BPMSpecification specification) {
    	List<SetVerifier> verifiers = new ArrayList<>();

		loadSpecificationTypes(specification, specificationTypeMap);
		
		for(SpecificationSet specificationSet: specification.getSpecificationSets()) {
			SetVerifier setVerifier = new SetVerifier(net, specification, specificationSet, eventHandler);
			verifiers.add(setVerifier);
		}

		return verifiers;
    }
}
