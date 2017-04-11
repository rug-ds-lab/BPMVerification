package nl.rug.ds.bpm.verification.modelCheckers;

import nl.rug.ds.bpm.jaxb.specification.Specification;
import nl.rug.ds.bpm.pnml.EventHandler;
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

    public NuSMVChecker(EventHandler eventHandler, File checker, Kripke kripke, List<Specification> specifications) {
        super(eventHandler, checker, kripke, specifications);
    }

    public void createInputData() {
        inputChecker = new StringBuilder();

        inputChecker.append("MODULE Verify\n");
        inputChecker.append(convertVAR());
        inputChecker.append(convertDEFINE());
        inputChecker.append(convertASSIGN());
        inputChecker.append(convertFORMULAS());
    }

    private String convertVAR() {
        StringBuilder v = new StringBuilder("\tVAR\n\t\t state:{");

        Iterator<State> i = kripke.getStates().iterator();
        while (i.hasNext()) {
            v.append(i.next().getID());
            if (i.hasNext()) v.append(",");
        }

        v.append("}; \n");

        return v.toString();
    }

    private String convertDEFINE() {
        StringBuilder d = new StringBuilder("\tDEFINE\n");

        Iterator<String> i = kripke.getAtomicPropositions().iterator();
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

        Iterator<State> i = kripke.getInitial().iterator();
        while (i.hasNext()) {
            a.append(i.next().getID());
            if (i.hasNext()) a.append(",");
        }
        a.append("};\n");

        a.append("\t\tnext(state) := \n\t\t\tcase\n");
        Iterator<State> j = kripke.getStates().iterator();
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
        List<State> sub = new ArrayList<State>(kripke.getStates().size() / kripke.getAtomicPropositions().size());

        for (State s : kripke.getStates())
            if (s.getAtomicPropositions().contains(ap))
                sub.add(s);

        return sub;
    }

    protected Process createProcess() {
        try {
            Process proc = Runtime.getRuntime().exec(checker.getAbsoluteFile() + " " + file.getAbsolutePath());
            return proc;
        } catch (Throwable t) {
            eventHandler.logError("Could not call model checker NuSMV2");
            eventHandler.logError("No checks were performed");
            return null;
        }
    }

    protected void checkResults(List<String> resultLines) {
        List<String> results = new ArrayList<>();
        resultLines.forEach(line -> {
            if (line.contains("-- specification "))
                results.add(line.replace("-- specification ", "").trim());
        });
        for (String result : results) {
            if (result.contains("is false")) {
                eventHandler.logError("Specification evaluated false for formula " + result.replace("is false", ""));
                Specification specification = getConstraintResult(result.replace("is false", ""));
                if(specification != null)
                    eventHandler.fireEvent(specification, false);
            } else {
                eventHandler.logInfo("Specification evaluated true for formula " + result.replace("is true", ""));
                Specification specification = getConstraintResult(result.replace("is true", ""));
                if(specification != null)
                    eventHandler.fireEvent(specification, true);
            }
        }
    }


    private Specification getConstraintResult(String resultFormula) {
        String formula = trimFormula(resultFormula);
        Specification specification = specifications
                .stream().filter(c -> trimFormula(c.toString()).equalsIgnoreCase(formula))
                .findFirst().orElse(null);
        if (specification == null)
            eventHandler.logError("Specification matching failed for " + formula);
        return specification;
    }
}
