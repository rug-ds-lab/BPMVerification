package nl.rug.ds.bpm.verification.modelcheck.nusmv2interactive;

import nl.rug.ds.bpm.specification.jaxb.Formula;
import nl.rug.ds.bpm.specification.jaxb.Specification;
import nl.rug.ds.bpm.util.exception.CheckerException;
import nl.rug.ds.bpm.util.exception.FormulaException;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.util.map.TreeSetMap;
import nl.rug.ds.bpm.verification.event.VerificationEvent;
import nl.rug.ds.bpm.verification.map.IDMap;
import nl.rug.ds.bpm.verification.model.kripke.Kripke;
import nl.rug.ds.bpm.verification.model.kripke.State;
import nl.rug.ds.bpm.verification.modelcheck.Checker;
import nl.rug.ds.bpm.verification.modelcheck.CheckerFormula;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Created by
 */
public class NuSMVInteractiveChecker extends Checker {
	private File file;
	private Process proc;
	private Scanner inputStream;
	private PrintStream outputStream;

	public NuSMVInteractiveChecker(File checker) throws CheckerException {
		super(checker);

		try {
			proc = Runtime.getRuntime().exec(executable.getAbsoluteFile() + " -int");

			OutputStream out = proc.getOutputStream();
			outputStream = new PrintStream(out);

			InputStream stdin = proc.getInputStream();
			InputStreamReader in = new InputStreamReader(stdin);
			inputStream = new Scanner(in);

			//Skip welcome message
			while (inputStream.hasNext() && inputStream.hasNext(">")) ;
		}
		catch (Exception e) {
			throw new CheckerException("Failed to call NuSMV2");
		}
	}

	@Override
	public void destroy() {
		try {
			outputStream.println("quit");

			outputStream.close();
			inputStream.close();

			proc.waitFor();
			proc.destroy();
		}
		catch (Exception e) {}
	}

	@Override
	public void addFormula(Formula formula, Specification specification, IDMap idMap, TreeSetMap<String, String> groupMap) {
		NuSMVInteractiveFormula nuSMVFormula = new NuSMVInteractiveFormula(formula, specification, idMap, groupMap);
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

			PrintWriter writer = new PrintWriter(file);
			writer.println(inputChecker);
			writer.close();
		} catch (Throwable t) {
			throw new CheckerException("Failed to write to file" +  file.toString());
		}
	}

	@Override
	public List<VerificationEvent> checkModel() throws CheckerException {
		List<VerificationEvent> results = new ArrayList<>();
		try {
			outputStream.println("read_model -i " + file.getAbsolutePath());
			outputStream.println("go");

			List<CheckerFormula> formulaList = formulas.stream()
														.filter(f ->
																f.getFormula().getLanguage().equalsIgnoreCase("ctlspec") || f.getFormula().getLanguage().equalsIgnoreCase("ltlspec")
														).collect(Collectors.toList());

			outputStream.flush();

			for (CheckerFormula formula: formulaList) {
				outputStream.println((formula.getFormula().getLanguage().equalsIgnoreCase("ctlspec") ? "check_ctlspec" : "check_ltlspec") + " -p " + formula.getCheckerFormula());

				VerificationEvent event = null;
				String line = "";
				while (inputStream.hasNext() && !inputStream.hasNext(">")) {
					line = inputStream.nextLine();

					if (line.contains("-- specification ")) {
						event = new VerificationEvent(formula, line.contains("is true"));
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
			}
		}
		catch (Exception e) {
			throw new CheckerException("Failed to call NuSMV2");
		}

		return results;
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
		StringBuilder fs = new StringBuilder();
		formulas.stream()
				.filter(f -> f.getFormula().getLanguage().equalsIgnoreCase("fairness"))
				.forEach(f -> {
					try {
						fs.append(f.getCheckerFormula() + "\n");
					} catch (FormulaException e) {
						e.printStackTrace();
					}
				});
		return fs.toString();
	}

	private List<State> findStates(Kripke kripke, String ap) {
		List<State> sub = new ArrayList<State>(kripke.getStates().size() / kripke.getAtomicPropositions().size());

		for (State s : kripke.getStates())
			if (s.getAtomicPropositions().contains(ap))
				sub.add(s);

		return sub;
	}
}