package nl.rug.ds.bpm.verification.modelcheck;

import java.io.File;

/**
 * Created by Heerko Groefsema on 09-Jun-17.
 */
public abstract class CheckerFactory {
	protected File executable;
	
	public CheckerFactory(File executable) {
		this.executable = executable;
	}
	
	public abstract Checker getChecker();

	public void release(Checker checker) { return; }

	public void destroy() { return; }
}
