package nl.rug.ds.bpm.pnml;

import nl.rug.ds.bpm.pnml.events.VerificationEvent;
import nl.rug.ds.bpm.pnml.events.VerificationLogEvent;
import nl.rug.ds.bpm.pnml.listeners.VerificationEventListener;
import nl.rug.ds.bpm.pnml.listeners.VerificationLogListener;

import java.io.File;

/**
 * Created by Heerko Groefsema on 07-Apr-17.
 */
public class Verify implements VerificationEventListener, VerificationLogListener {
	
	public static void main(String [ ] args) {
		if(args.length > 2) {
			Verify verify = new Verify(args[0], args[1], args[2]);
		} else
			System.out.println("Usage: PNMLVerifier PNML_file Specification_file NuSMV2_binary_path");
	}
	
	public Verify(String pnml, String specification, String nusmv2) {
		File pnmlFile = new File(pnml);
		File specificationFile = new File(specification);
		File nusmv2Binary = new File(nusmv2);
		
		PnmlVerifier verifier = new PnmlVerifier();
		verifier.addLogListener(this);
		verifier.addEventListener(this);
		verifier.verify(pnmlFile, specificationFile, nusmv2Binary);
	}
	
	@Override
	public void verificationEvent(VerificationEvent event) {
		System.out.println(event.toString());
	}
	
	@Override
	public void verificationLogEvent(VerificationLogEvent event) { System.out.println(event.toString()); }
}
