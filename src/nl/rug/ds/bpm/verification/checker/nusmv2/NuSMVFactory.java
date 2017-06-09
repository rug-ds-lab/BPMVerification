package nl.rug.ds.bpm.verification.checker.nusmv2;

import nl.rug.ds.bpm.event.EventHandler;
import nl.rug.ds.bpm.verification.checker.Checker;
import nl.rug.ds.bpm.verification.checker.CheckerFactory;

import java.io.File;

/**
 * Created by Heerko Groefsema on 09-Jun-17.
 */
public class NuSMVFactory extends CheckerFactory {
	
	public NuSMVFactory(EventHandler eventHandler, File executable) {
		super(eventHandler, executable);
	}
	
	@Override
	public Checker getChecker() {
		return new NuSMVChecker(eventHandler, executable);
	}
}
