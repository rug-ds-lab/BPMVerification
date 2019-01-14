package nl.rug.ds.bpm.verification.modelcheck.nusmv2interactive;

import nl.rug.ds.bpm.specification.jaxb.Formula;
import nl.rug.ds.bpm.specification.jaxb.Specification;
import nl.rug.ds.bpm.util.exception.CheckerException;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.util.map.TreeSetMap;
import nl.rug.ds.bpm.verification.event.VerificationEvent;
import nl.rug.ds.bpm.verification.map.IDMap;
import nl.rug.ds.bpm.verification.model.kripke.Kripke;
import nl.rug.ds.bpm.verification.modelcheck.Checker;
import nl.rug.ds.bpm.verification.modelcheck.CheckerFormula;
import nl.rug.ds.bpm.verification.modelcheck.nusmv2.NuSMVFileWriter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by
 */
public class NuSMVInteractiveChecker extends Checker {
	private File file;
	private Process proc;
	private NuSMVScanner scanner;

	public NuSMVInteractiveChecker(File checker) throws CheckerException {
		super(checker);

		try {
			proc = Runtime.getRuntime().exec(executable.getAbsoluteFile() + " -int");
			scanner = new NuSMVScanner(proc);
		}
		catch (Exception e) {
			throw new CheckerException("Failed to call NuSMV2");
		}
	}

	@Override
	public void destroy() {
		try {
			scanner.close();

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
		List<CheckerFormula> formulaList = formulas.stream().filter(f -> f.getFormula().getLanguage().equalsIgnoreCase("fairness")).collect(Collectors.toList());

		NuSMVFileWriter fileWriter;
		if (out == null)
			fileWriter = new NuSMVFileWriter(kripke, formulaList, id);
		else
			fileWriter = new NuSMVFileWriter(kripke, formulaList, id, out);

		file = fileWriter.getFile();
		inputChecker.append(fileWriter.getContents());
	}

	@Override
	public List<VerificationEvent> checkModel() throws CheckerException {
		List<VerificationEvent> results = new ArrayList<>();
		try {
			scanner.writeln("reset");
			scanner.writeln("read_model -i " + file.getAbsolutePath());
			scanner.writeln("go");

			List<CheckerFormula> formulaList = formulas.stream()
														.filter(f ->
																f.getFormula().getLanguage().equalsIgnoreCase("ctlspec") || f.getFormula().getLanguage().equalsIgnoreCase("ltlspec")
														).collect(Collectors.toList());

			for (CheckerFormula formula: formulaList) {
				scanner.writeln((formula.getFormula().getLanguage().equalsIgnoreCase("ctlspec") ? "check_ctlspec" : "check_ltlspec") + " -p " + formula.getCheckerFormula());

				VerificationEvent event = null;
				String line = "";
				while (scanner.hasNext()) {
					line = scanner.next();

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
			while (scanner.hasNext())
				outputChecker.append(scanner.next() + "\n");

			throw new CheckerException("Failed to call NuSMV2:\n" + outputChecker);
		}

		for (String line : scanner.getErrors())
			outputChecker.append(line + "\n");

		return results;
	}
}