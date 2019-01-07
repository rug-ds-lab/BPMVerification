package nl.rug.ds.bpm.verification.modelcheck;

import nl.rug.ds.bpm.specification.jaxb.Formula;
import nl.rug.ds.bpm.specification.jaxb.Specification;
import nl.rug.ds.bpm.util.exception.CheckerException;
import nl.rug.ds.bpm.util.map.TreeSetMap;
import nl.rug.ds.bpm.verification.event.EventHandler;
import nl.rug.ds.bpm.verification.event.VerificationEvent;
import nl.rug.ds.bpm.verification.map.IDMap;
import nl.rug.ds.bpm.verification.model.kripke.Kripke;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class Checker {
	protected static File out;
	protected static int idc = 0;

	protected int id;
	protected StringBuilder inputChecker, outputChecker;
    protected EventHandler eventHandler;
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
    
    public abstract void addFormula(Formula formula, Specification specification, IDMap idMap, TreeSetMap<String, String> groupMap);
	
	public abstract void createModel(Kripke kripke) throws CheckerException;
	
	public abstract List<VerificationEvent> checkModel() throws CheckerException;
	
	public void checkModel(Kripke kripke) throws CheckerException {
        createModel(kripke);
        checkModel();
    }

    public static void setOutputPath(String path) {
		try {
			out = new File(path);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
    
//    protected StringBuilder inputChecker;
//    protected StringBuilder outputChecker;
//    protected Kripke kripke;
//    protected List<AbstractFormula> formulas;
//    protected File file, modelcheck;
//    protected EventHandler eventHandler;
//    protected List<String> results;
//
//    public Checker(EventHandler eventHandler, File modelcheck, Kripke kripke, List<AbstractFormula> formulas) {
//        this.eventHandler = eventHandler;
//        this.modelcheck = modelcheck;
//        this.kripke = kripke;
//        this.formulas = formulas;
//        outputChecker = new StringBuilder();
//        results = new ArrayList<>();
//    }
//
//    public abstract void createInputData();
//
//    protected void createInputFile() {
//        try {
//            file = File.createTempFile("model", ".smv");
//            PrintWriter writer = new PrintWriter(file, "UTF-8");
//            writer.println(inputChecker);
//            writer.close();
//        } catch (Throwable t) {
//            eventHandler.logCritical("Issue writing temporary file");
//        }
//    }
//
//    public List<String> callModelChecker() {
//        createInputFile();
//        Process proc = createProcess();
//        getInputStream(proc);
//
//        return getResults(results);
//    }
//
//    public abstract Process createProcess();
//
//    public abstract List<String> getResults(List<String> results);

}
