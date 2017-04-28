package nl.rug.ds.bpm.pnmlTools.verifier;

import java.io.File;

import nl.rug.ds.bpm.event.VerificationEvent;
import nl.rug.ds.bpm.event.VerificationLogEvent;
import nl.rug.ds.bpm.event.listener.VerificationEventListener;
import nl.rug.ds.bpm.event.listener.VerificationLogListener;
import nl.rug.ds.bpm.verification.Verifier;

/**
 * Created by Heerko Groefsema on 07-Apr-17.
 */
public class PnmlVerifier implements VerificationEventListener, VerificationLogListener {
	
	public static void main(String [ ] args) {
		if(args.length > 2) {
			PnmlVerifier pnmlVerifier = new PnmlVerifier(args[0], args[1], args[2]);
		} 
		else {
			System.out.println("Usage: PNMLVerifier PNML_file Specification_file NuSMV2_binary_path");
		}
	}
	
	public PnmlVerifier(String pnml, String specification, String nusmv2) {
		File pnmlFile = new File(pnml);
		File specificationFile = new File(specification);
		File nusmv2Binary = new File(nusmv2);
		
		//Make step class for specific Petri net type
		ExtPnmlStepper stepper;
		try {
			stepper = new ExtPnmlStepper(pnmlFile);
			
			//Make a verifier which uses that step class
			Verifier verifier = new Verifier(stepper);
			
			//Add listeners to receive log and result notifications
			verifier.addLogListener(this);
			verifier.addEventListener(this);
			
			//Start verification
			verifier.verify(specificationFile, nusmv2Binary);
			
			//Remove listeners
			verifier.removeLogListener(this);
			verifier.removeEventListener(this);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	@Override
	public void verificationEvent(VerificationEvent event) {
		System.out.println(event.toString());
	}
	
	@Override
	public void verificationLogEvent(VerificationLogEvent event) {
		System.out.println(event.toString());
	}
}
