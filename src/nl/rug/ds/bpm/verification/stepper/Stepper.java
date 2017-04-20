package nl.rug.ds.bpm.verification.stepper;

import java.io.File;
import java.util.Set;

/**
 * Created by Heerko Groefsema on 20-Apr-17.
 */
public abstract class Stepper {
	protected File net;
	
	public Stepper(File net) {
		this.net = net;
		if(!(net.exists() && net.isFile())) {
			System.out.println("No such file " + net.toString());
			System.exit(-1);
		}
	}
	
	public abstract String initialMarking();
	public abstract Set<Set<String>> parallelActivatedTransitions(String marking);
	public abstract String fireTransition(String transition);
}
