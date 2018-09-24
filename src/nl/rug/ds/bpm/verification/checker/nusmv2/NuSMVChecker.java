package nl.rug.ds.bpm.verification.checker.nusmv2;

import nl.rug.ds.bpm.specification.jaxb.Formula;
import nl.rug.ds.bpm.specification.jaxb.Specification;
import nl.rug.ds.bpm.util.exception.CheckerException;
import nl.rug.ds.bpm.util.exception.FormulaException;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.verification.checker.Checker;
import nl.rug.ds.bpm.verification.checker.CheckerFormula;
import nl.rug.ds.bpm.verification.event.EventHandler;
import nl.rug.ds.bpm.verification.event.VerificationEvent;
import nl.rug.ds.bpm.verification.map.GroupMap;
import nl.rug.ds.bpm.verification.map.IDMap;
import nl.rug.ds.bpm.verification.model.kripke.Kripke;
import nl.rug.ds.bpm.verification.model.kripke.State;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by
 */
public class NuSMVChecker extends Checker {
	private File file;
	
    public NuSMVChecker(EventHandler eventHandler, File checker) {
        super(eventHandler, checker);
    }
	
	@Override
	public void addFormula(Formula formula, Specification specification, IDMap idMap, GroupMap groupMap) {
    	NuSMVFormula nuSMVFormula = new NuSMVFormula(formula, specification, idMap, groupMap);
		formulas.add(nuSMVFormula);
		Logger.log("Including specification formula " + nuSMVFormula.getOriginalFormula(), LogEvent.VERBOSE);
	}
	
	@Override
	public void createModel(Kripke kripke) throws CheckerException {
		inputChecker.append("MODULE main\n");
		inputChecker.append(convertVAR(kripke));
		inputChecker.append(convertDEFINE(kripke));
		inputChecker.append(convertASSIGN(kripke));
		inputChecker.append(convertFORMULAS());
		
		try {
			if(out == null)
				file = File.createTempFile("model" + id, ".smv");
			else
				file = new File(out, "model" + id + ".smv");

			PrintWriter writer = new PrintWriter(file, "UTF-8");
			writer.println(inputChecker);
			writer.close();
		} catch (Throwable t) {
			throw new CheckerException("Failed to write to file" +  file.toString());
		}
	}
	
	@Override
	public void checkModel() throws CheckerException {
		try {
			Process proc = Runtime.getRuntime().exec(executable.getAbsoluteFile() + " " + file.getAbsolutePath());
			
			List<VerificationEvent> results = parseResults(proc);
			for (VerificationEvent event: results)
				fireEvent(event);

			List<String> errors = getErrors(proc);
			for (String line : errors)
				outputChecker.append(line + "\n");
		
			proc.waitFor();
			proc.destroy();
		}
		catch (Exception e) {
			throw new CheckerException("Failed to call NuSMV2");
		}
	}

    private String convertVAR(Kripke kripke) {
        StringBuilder v = new StringBuilder("\tVAR\n\t\t state:{");

        Iterator<State> i = kripke.getStates().iterator();
        while (i.hasNext()) {
            v.append(i.next().getID());
            if (i.hasNext()) v.append(",");
        }

        v.append("}; \n");

        return v.toString();
    }

    private String convertDEFINE(Kripke kripke) {
        StringBuilder d = new StringBuilder("\tDEFINE\n");

        Iterator<String> i = kripke.getAtomicPropositions().iterator();
        while (i.hasNext()) {
            String ap = i.next();
            d.append("\t\t " + ap + " := ");

            Iterator<State> j = findStates(kripke, ap).iterator();
            while (j.hasNext()) {
                State s = j.next();
                d.append("( state = " + s.getID() + " )");
                if (j.hasNext()) d.append(" | ");
            }
            d.append(";\n");
        }

        return d.toString();
    }

    private String convertASSIGN(Kripke kripke) {
        StringBuilder a = new StringBuilder("\tASSIGN\n\t\tinit(state) := {");
    
        //Safety
        for(State s: kripke.getSinkStates()) {
            s.addNext(s);
            s.addPrevious(s);
        }

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

    private String convertFORMULAS() {
        StringBuilder f = new StringBuilder();
        for (CheckerFormula formula: formulas) {
	        try {
		        f.append(formula.getCheckerFormula() + "\n");
	        } catch (FormulaException e) {
		        e.printStackTrace();
	        }
        }
        return f.toString();
    }

    private List<State> findStates(Kripke kripke, String ap) {
        List<State> sub = new ArrayList<State>(kripke.getStates().size() / kripke.getAtomicPropositions().size());

        for (State s : kripke.getStates())
            if (s.getAtomicPropositions().contains(ap))
                sub.add(s);

        return sub;
    }

	private List<String> getErrors(Process proc) throws IOException {
		List<String> results = new ArrayList<>();
		
		String line = null;
		//errorstream
		InputStream stderr = proc.getErrorStream();
		InputStreamReader isr = new InputStreamReader(stderr);
		BufferedReader br = new BufferedReader(isr);
		while ((line = br.readLine()) != null) {
			results.add(line);
		}
		br.close();
	
		return results;
	}

	private List<VerificationEvent> parseResults(Process proc) throws IOException {
    	List<VerificationEvent> results = new ArrayList<>();
    	VerificationEvent event = null;

		//inputStream
		String line = null;
		InputStream stdin = proc.getInputStream();
		InputStreamReader in = new InputStreamReader(stdin);
		BufferedReader bir = new BufferedReader(in);

		while ((line = bir.readLine()) != null) {
			if (line.contains("-- specification ")) {
				event = new VerificationEvent(parseFormula(line), line.contains("is true"));
				results.add(event);
				formulas.remove(event.getFormula());
			}
			else if (line.contains("Trace Type: Counterexample")) {
				event.setCounterExample(new ArrayList<List<String>>());
			}
			else if (line.contains("-> State:") && event.getCounterExample() != null) {
				List<String> state = new ArrayList<>();
				int previous = event.getCounterExample().size() - 1;
				if(previous >= 0)
					state.addAll(event.getCounterExample().get(previous));
				event.getCounterExample().add(state);
			}
			else if (line.contains(" = TRUE") && event.getCounterExample() != null) {
				String ap = line.substring(0, line.indexOf(" = TRUE")).trim();
				String id = event.getFormula().getIdMap().getID(ap);
				event.getCounterExample().get(event.getCounterExample().size() - 1).add(id);
			}
			else if (line.contains(" = FALSE") && event.getCounterExample() != null) {
				String ap = line.substring(0, line.indexOf(" = FALSE")).trim();
				String id = event.getFormula().getIdMap().getID(ap);
				event.getCounterExample().get(event.getCounterExample().size() - 1).remove(id);
			}
		}

		bir.close();
		in.close();

		return results;
	}

	private CheckerFormula parseFormula(String line) {
		String formula = line.substring(16, line.indexOf("is")).trim();
		CheckerFormula abstractFormula = null;

		boolean found = false;
		Iterator<CheckerFormula> abstractFormulaIterator = formulas.iterator();
		while (abstractFormulaIterator.hasNext() && !found) {
			CheckerFormula f = abstractFormulaIterator.next();
			if(f.equals(formula)) {
				found = true;
				abstractFormula = f;
			}
		}

		return abstractFormula;
	}

	private void fireEvent(VerificationEvent event) {
    	if (event.getFormula() == null)
			Logger.log("Failed to map formula to original specification", LogEvent.ERROR);
    	else {
			eventHandler.fireEvent(event);
			Logger.log("Specification " + event.getFormula().getSpecification().getId() + " evaluated " + event.getVerificationResult() + " for " + event.getFormula().getOriginalFormula(), LogEvent.INFO);
		}
	}
}
