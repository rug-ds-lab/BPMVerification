package nl.rug.ds.bpm.verification.checker;

import nl.rug.ds.bpm.event.EventHandler;

import java.io.File;

/**
 * Created by Heerko Groefsema on 09-Jun-17.
 */
public abstract class CheckerFactory {
	protected File executable;
	protected EventHandler eventHandler;
	
	public CheckerFactory(EventHandler eventHandler, File executable) {
		if(!(executable.exists() && executable.isFile() && executable.canExecute()))
			eventHandler.logCritical("Unable to call binary at " + executable.toString());
		
		this.eventHandler = eventHandler;
		this.executable = executable;
	}
	
	public abstract Checker getChecker();
}
