package nl.rug.ds.bpm.verification.modelCheckers;

import nl.rug.ds.bpm.editor.core.enums.ConstraintStatus;
import nl.rug.ds.bpm.editor.models.ConstraintResult;
import nl.rug.ds.bpm.editor.models.ModelChecker;
import nl.rug.ds.bpm.verification.models.kripke.Kripke;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Mark Kloosterhuis.
 */
public abstract class AbstractChecker {
    protected StringBuilder inputChecker;
    protected StringBuilder outputChecker;
    protected Kripke kripkeModel;
    protected File file;
    protected List<ConstraintResult> constraintsResults;
    public static int fileNumber = 0;
    protected String checkerId;
    protected ModelChecker checkerSettings;
    protected String checkerPath;
    protected static HashMap<String, HashMap<Integer, List<String>>> resultCache = new HashMap<>();
    protected static HashMap<String, HashMap<Integer, List<String>>> errorCache = new HashMap<>();

    public AbstractChecker(String checkerId) {
        this.checkerId = checkerId;
        outputChecker = new StringBuilder();
        if (!resultCache.containsKey(checkerId)) {
            resultCache.put(checkerId, new HashMap<>());
            errorCache.put(checkerId, new HashMap<>());
        }
    }


    public void setKripkeModel(Kripke model) {
        this.kripkeModel = model;
    }

    public void setConstraintsResults(List<ConstraintResult> constraintsResults) {
        this.constraintsResults = constraintsResults;
    }

    public abstract void createInputData();

    public String getInputChecker() {
        return inputChecker.toString();
    }

    public String getOutputChecker() {
        return outputChecker.toString();
    }


    public String getCheckerId() {
        return checkerId;
    }

    protected String convertFORMULAS() {
        StringBuilder f = new StringBuilder();
        for (ConstraintResult constraint : constraintsResults) {
            if (constraint.getStatus() == ConstraintStatus.None) {
                f.append(checkerSettings.parseFormula(constraint));
            }
        }
        return f.toString();
    }

    protected void createInputFile() {
        try {
            file = File.createTempFile("model", ".smv");
            //file = new File("C:/Checkers/model" + fileNumber + ".smv");
            fileNumber++;
            PrintWriter writer = new PrintWriter(file, "UTF-8");
            writer.println(inputChecker);
            writer.close();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private int getInputHashCode() {
        return inputChecker.toString().hashCode();
    }

    public void callModelChecker() {

        if (!resultCache.get(checkerId).containsKey(getInputHashCode())) {
            createInputFile();
            Process proc = createProcess();
            getInputStream(proc);
        }
        List<String> resultLines = resultCache.get(checkerId).get(getInputHashCode());
        List<String> errorLines = errorCache.get(checkerId).get(getInputHashCode());

        if(resultLines != null)
        {
            checkResults(resultLines, errorLines);
            outputChecker.append(String.join("\n", resultLines));
            outputChecker.append(String.join("\n", errorLines));
        }

    }

    abstract Process createProcess();

    abstract void checkResults(List<String> resultLines, List<String> errorLines);

    protected void getInputStream(Process proc) {
        try {
            String line = null;
            //inputStream
            InputStream stdin = proc.getInputStream();
            InputStreamReader in = new InputStreamReader(stdin);
            BufferedReader bir = new BufferedReader(in);
            StringBuilder resultStr = new StringBuilder();
            List<String> results = new ArrayList<>();
            while ((line = bir.readLine()) != null) {
                results.add(line);
            }
            bir.close();
            in.close();
            resultCache.get(checkerId).put(getInputHashCode(), results);

            //errorstream
            List<String> errors = new ArrayList<>();
            InputStream stderr = proc.getErrorStream();
            InputStreamReader isr = new InputStreamReader(stderr);
            BufferedReader br = new BufferedReader(isr);
            while ((line = br.readLine()) != null) {
                errors.add(line);
            }

            errorCache.get(checkerId).put(getInputHashCode(), errors);

            br.close();
            proc.waitFor();
            file.delete();
            proc.destroy();

        } catch (Throwable t) {
            //t.printStackTrace();
            outputChecker.append("WARNING: Could not callModelChecker " + checkerId + "\n");
            outputChecker.append("WARNING: No checks were performed.\n");
        }
    }

    protected String trimFormula(String formula) {
        return formula.replaceAll("([\\(\\)\\s+])", "").trim();
    }
}
