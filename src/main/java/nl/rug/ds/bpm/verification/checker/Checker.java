package nl.rug.ds.bpm.verification.checker;

import nl.rug.ds.bpm.specification.jaxb.Formula;
import nl.rug.ds.bpm.specification.jaxb.Specification;
import nl.rug.ds.bpm.util.exception.CheckerException;
import nl.rug.ds.bpm.verification.event.VerificationEvent;
import nl.rug.ds.bpm.verification.map.AtomicPropositionMap;
import nl.rug.ds.bpm.verification.model.Structure;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class Checker {
	protected static File out;
	protected static int idc = 0;

	protected int id;
	protected StringBuilder inputChecker, outputChecker;
    protected File executable;
    protected List<CheckerFormula> formulas;
    
    public Checker(File executable) {
    	id = idc++;
        this.executable = executable;
        
        formulas = new ArrayList<>();
		inputChecker = new StringBuilder();
		outputChecker = new StringBuilder();
    }
    
	public String getInputChecker() {
		return inputChecker.toString();
	}
	
	public String getOutputChecker() {
		return outputChecker.toString();
	}

    public List<CheckerFormula> getFormulas() {
        return formulas;
    }

    public abstract void addFormula(Formula formula, Specification specification, AtomicPropositionMap atomicPropositionMap);

    public abstract void createModel(Structure structure) throws CheckerException;

    public abstract List<VerificationEvent> checkModel() throws CheckerException;

    public void checkModel(Structure Structure) throws CheckerException {
        createModel(Structure);
        checkModel();
    }

    public void destroy() {
        return;
    }

    public static void setOutputPath(String path) {
        try {
            out = new File(path);
        } catch (Exception e) {
			e.printStackTrace();
		}
	}
}
