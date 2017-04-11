package nl.rug.ds.bpm.verification.modelCheckers;

import nl.rug.ds.bpm.jaxb.specification.Specification;
import nl.rug.ds.bpm.pnml.EventHandler;
import nl.rug.ds.bpm.verification.models.kripke.Kripke;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mark Kloosterhuis.
 */
public abstract class AbstractChecker {
    protected StringBuilder inputChecker;
    protected StringBuilder outputChecker;
    protected Kripke kripke;
    protected List<Specification> specifications;
    protected File file, checker;
    protected EventHandler eventHandler;
    protected List<String> results, errors;

    public AbstractChecker(EventHandler eventHandler, File checker, Kripke kripke, List<Specification> specifications) {
        this.eventHandler = eventHandler;
        this.checker = checker;
        this.kripke = kripke;
        this.specifications = specifications;
        outputChecker = new StringBuilder();
        results = new ArrayList<>();
        errors = new ArrayList<>();
    }

    public abstract void createInputData();

    public String getInputChecker() {
        return inputChecker.toString();
    }

    public String getOutputChecker() {
        return outputChecker.toString();
    }

    protected String convertFORMULAS() {
        StringBuilder f = new StringBuilder();
        for (Specification specification: specifications)
            for(String formula: specification.getFormulas())
                f.append(formula + "\n");
        return f.toString();
    }

    protected void createInputFile() {
        try {
            file = File.createTempFile("model", ".smv");
            PrintWriter writer = new PrintWriter(file, "UTF-8");
            writer.println(inputChecker);
            writer.close();
        } catch (Throwable t) {
            eventHandler.logCritical("Issue writing temporary file");
        }
    }

    public void callModelChecker() {
        createInputFile();
        Process proc = createProcess();
        getInputStream(proc);

        checkResults(results);
    }

    abstract Process createProcess();

    abstract void checkResults(List<String> results);

    protected void getInputStream(Process proc) {
        try {
            String line = null;
            //inputStream
            InputStream stdin = proc.getInputStream();
            InputStreamReader in = new InputStreamReader(stdin);
            BufferedReader bir = new BufferedReader(in);
            while ((line = bir.readLine()) != null) {
                results.add(line);
            }
            bir.close();
            in.close();

            //errorstream
            InputStream stderr = proc.getErrorStream();
            InputStreamReader isr = new InputStreamReader(stderr);
            BufferedReader br = new BufferedReader(isr);
            while ((line = br.readLine()) != null) {
                errors.add(line);
            }

            br.close();
            proc.waitFor();
            file.delete();
            proc.destroy();

        } catch (Throwable t) {
            eventHandler.logError("Could not call model checker");
            eventHandler.logError("No checks were performed");
        }
    }

    protected String trimFormula(String formula) {
        String f = formula.replace("CTLSPEC ", "");
        f = f.replace("LTLSPEC ", "");
        f = f.replace("JUSTICE ", "");
        return f.replaceAll("([\\(\\)\\s+])", "").trim();
    }
}
