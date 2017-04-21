package nl.rug.ds.bpm.pnml;

import nl.rug.ds.bpm.verification.stepper.Stepper;

import java.io.File;
import java.util.Set;

/**
 * Created by Heerko Groefsema on 07-Apr-17.
 */
public class PnmlStepper extends Stepper{
	
	public PnmlStepper(File pnml) {
		super(pnml);
	}
	
	@Override
	public String initialMarking() {
		return null;
	}
	
	@Override
	public Set<Set<String>> parallelActivatedTransitions(String marking) {
		return null;
	}
	
	@Override
	public Set<String> fireTransition(String transition, Set<String> conditions) {
		return null;
	}
}
