package nl.rug.ds.bpm.verification.modelcheck.nusmv2;

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

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by
 */
public class NuSMVChecker extends Checker {
	private File file;

	public NuSMVChecker(File checker) {
		super(checker);
	}

	@Override
	public void addFormula(Formula formula, Specification specification, IDMap idMap, TreeSetMap<String, String> groupMap) {
		NuSMVFormula nuSMVFormula = new NuSMVFormula(formula, specification, idMap, groupMap);
		formulas.add(nuSMVFormula);
		Logger.log("Including specification formula " + nuSMVFormula.getOriginalFormula(), LogEvent.VERBOSE);
	}

	@Override
	public void createModel(Kripke kripke) throws CheckerException {
		NuSMVFileWriter fileWriter;
		if (out == null)
			fileWriter = new NuSMVFileWriter(kripke, formulas, id);
		else
			fileWriter = new NuSMVFileWriter(kripke, formulas, id, out);

		file = fileWriter.getFile();
		inputChecker.append(fileWriter.getContents());
	}

	@Override
	public List<VerificationEvent> checkModel() throws CheckerException {
		List<VerificationEvent> results;
		try {
			Process proc = Runtime.getRuntime().exec(executable.getAbsoluteFile() + " " + file.getAbsolutePath());

			results = parseResults(proc);

			List<String> errors = getErrors(proc);
			for (String line : errors)
				outputChecker.append(line + "\n");

			proc.waitFor();
			proc.destroy();
		}
		catch (Exception e) {
			throw new CheckerException("Failed to call NuSMV2");
		}

		return results;
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
}
