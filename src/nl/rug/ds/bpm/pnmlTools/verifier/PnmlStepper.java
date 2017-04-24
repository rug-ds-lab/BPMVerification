package nl.rug.ds.bpm.pnmlTools.verifier;

import nl.rug.ds.bpm.verification.stepper.Marking;
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
	public Marking initialMarking() {
		//TODO Nick
		return null;
	}
	
	@Override
	public Set<Set<String>> parallelActivatedTransitions(Marking marking) {
		//TODO Nick
		return null;
	}
	
	@Override
	public Set<Marking> fireTransition(Marking marking, String transition, Set<String> conditions) {
		//TODO Nick
		return null;
	}
	
	
}
