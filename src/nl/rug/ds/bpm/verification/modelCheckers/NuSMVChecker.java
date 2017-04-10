package nl.rug.ds.bpm.verification.modelCheckers;

import nl.rug.ds.bpm.editor.core.enums.ConstraintStatus;
import nl.rug.ds.bpm.editor.models.ConstraintResult;
import nl.rug.ds.bpm.editor.models.ModelChecker;
import nl.rug.ds.bpm.verification.constraints.Formula;
import nl.rug.ds.bpm.verification.models.kripke.Kripke;
import nl.rug.ds.bpm.verification.models.kripke.State;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Mark Kloosterhuis.
 */
public class NuSMVChecker extends AbstractChecker {

    private List<Formula> formulas;


    public NuSMVChecker() {
        super("NuSMV");
    }

    public NuSMVChecker(String checkerId) {
        super(checkerId);
    }

    public NuSMVChecker(ModelChecker checkerSettings) {
        super("NuSMV");
        this.checkerSettings = checkerSettings;
        checkerPath = checkerSettings.getLocation();
        inputChecker = new StringBuilder();
    }

    public void createInputData() {
        if (kripkeModel == null)
            inputChecker = new StringBuilder();

        inputChecker.append("MODULE Verify\n");
        inputChecker.append(convertVAR());
        inputChecker.append(convertDEFINE());
        inputChecker.append(convertASSIGN());
        inputChecker.append(convertFORMULAS());
    }

    public void convert(Kripke m) {
        this.kripkeModel = m;
        inputChecker = new StringBuilder();

        createInputData();
    }

    private String convertVAR() {
        StringBuilder v = new StringBuilder("\tVAR\n\t\t state:{");

        Iterator<State> i = kripkeModel.getStates().iterator();
        while (i.hasNext()) {
            v.append(i.next().getID());
            if (i.hasNext()) v.append(",");
        }

        v.append("}; \n");

        return v.toString();
    }

    private String convertDEFINE() {
        StringBuilder d = new StringBuilder("\tDEFINE\n");

        Iterator<String> i = kripkeModel.getAtomicPropositions().iterator();
        while (i.hasNext()) {
            String ap = i.next();
            d.append("\t\t " + ap + " := ");

            Iterator<State> j = findStates(ap).iterator();
            while (j.hasNext()) {
                State s = j.next();
                d.append("( state = " + s.getID() + " )");
                if (j.hasNext()) d.append(" | ");
            }
            d.append(";\n");
        }

        return d.toString();
    }

    private String convertASSIGN() {
        StringBuilder a = new StringBuilder("\tASSIGN\n\t\tinit(state) := {");

        Iterator<State> i = kripkeModel.getInitial().iterator();
        while (i.hasNext()) {
            a.append(i.next().getID());
            if (i.hasNext()) a.append(",");
        }
        a.append("};\n");

        a.append("\t\tnext(state) := \n\t\t\tcase\n");
        Iterator<State> j = kripkeModel.getStates().iterator();
        while (j.hasNext()) {
            State s = j.next();
            a.append("\t\t\t\tstate = " + s.getID() + " : {");

            Iterator<State> k = s.getNextStates().iterator();
            if (k.hasNext())
                while (k.hasNext()) {
                    a.append(k.next().getID());
                    if (k.hasNext()) a.append(",");
                }
            a.append("};\n");
        }
        a.append("\t\t\tesac;\n");

        return a.toString();
    }


    private List<State> findStates(String ap) {
        List<State> sub = new ArrayList<State>(kripkeModel.getStates().size() / kripkeModel.getAtomicPropositions().size());

        for (State s : kripkeModel.getStates())
            if (s.getAtomicPropositions().contains(ap))
                sub.add(s);

        return sub;
    }

    protected Process createProcess() {
        try {
            File location = new File(checkerPath);
            Process proc = Runtime.getRuntime().exec(location.getAbsoluteFile() + " " + file.getAbsolutePath());
            return proc;
        } catch (Throwable t) {
            //t.printStackTrace();
            outputChecker.append("WARNING: Could not callModelChecker NuSMV2.\n");
            outputChecker.append("WARNING: No checks were performed.\n");
            return null;
        }
    }

    protected void checkResults(List<String> resultLines, List<String> errorLines) {
        List<String> results = new ArrayList<>();
        resultLines.forEach(line -> {
            if (line.contains("-- specification "))
                results.add(line.replace("-- specification ", "").trim());
        });
        for (String result : results) {
            if (result.contains("is false")) {
                ConstraintResult constraint = getConstraintResult(result.replace("is false", ""));
                if (constraint != null)
                    constraint.setStatus(ConstraintStatus.Invalid);
            } else {
                ConstraintResult constraint = getConstraintResult(result.replace("is true", ""));
                if (constraint != null)
                    constraint.setStatus(ConstraintStatus.Valid);
            }
        }

    }


    private ConstraintResult getConstraintResult(String resultFormula) {
        String formula = trimFormula(resultFormula);
        ConstraintResult constraint = constraintsResults
                .stream().filter(c -> trimFormula(c.formulaInput).equalsIgnoreCase(formula))
                .filter(c -> c.hasStatus(ConstraintStatus.None)).findFirst().orElse(null);
        if (constraint == null)
            nl.rug.ds.bpm.editor.Console.error("rawoutput matching FAILED! " + formula);
        return constraint;
    }
}
