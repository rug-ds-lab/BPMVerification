package nl.rug.ds.bpm.verification.modelcheck.nusmv2;

import nl.rug.ds.bpm.verification.modelcheck.Checker;
import nl.rug.ds.bpm.verification.modelcheck.CheckerFactory;

import java.io.File;

/**
 * Created by Heerko Groefsema on 09-Jun-17.
 */
public class NuSMVFactory extends CheckerFactory {
	
	public NuSMVFactory(File executable) {
		super(executable);
	}
	
	@Override
	public Checker getChecker() {
		return new NuSMVChecker(eventHandler, executable);
	}
}
