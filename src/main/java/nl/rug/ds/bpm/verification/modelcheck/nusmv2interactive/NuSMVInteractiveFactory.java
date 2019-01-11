package nl.rug.ds.bpm.verification.modelcheck.nusmv2interactive;

import nl.rug.ds.bpm.util.exception.CheckerException;
import nl.rug.ds.bpm.verification.modelcheck.Checker;
import nl.rug.ds.bpm.verification.modelcheck.CheckerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Heerko Groefsema on 09-Jun-17.
 */
public class NuSMVInteractiveFactory extends CheckerFactory {
	private List<Checker> checkerPool;
	
	public NuSMVInteractiveFactory(File executable) {
		super(executable);
		checkerPool = new ArrayList<>();
	}

	public NuSMVInteractiveFactory(File executable, int poolSize) {
		this(executable);

		while (checkerPool.size() < poolSize) {
			try {
				checkerPool.add(new NuSMVInteractiveChecker(executable));
			} catch (CheckerException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public synchronized Checker getChecker() {
		Checker checker = null;
		try {
			checker = (checkerPool.size() > 0 ? checkerPool.remove(0) : new NuSMVInteractiveChecker(executable));
		} catch (CheckerException e) {
			e.printStackTrace();
		}
		return checker;
	}

	@Override
	public synchronized void release(Checker checker) {
		checkerPool.add(checker);
	}

	@Override
	public void destroy() {
		for (Checker checker: checkerPool)
			checker.destroy();
		checkerPool.clear();
	}
}
