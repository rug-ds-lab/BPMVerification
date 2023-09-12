package nl.rug.ds.bpm.verification.checker.nusmv2;

import nl.rug.ds.bpm.expression.CompositeExpression;
import nl.rug.ds.bpm.specification.jaxb.Formula;
import nl.rug.ds.bpm.specification.jaxb.Specification;
import nl.rug.ds.bpm.util.exception.CheckerException;
import nl.rug.ds.bpm.util.exception.FormulaException;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.verification.checker.Checker;
import nl.rug.ds.bpm.verification.checker.CheckerFormula;
import nl.rug.ds.bpm.verification.event.VerificationEvent;
import nl.rug.ds.bpm.verification.map.AtomicPropositionMap;
import nl.rug.ds.bpm.verification.model.State;
import nl.rug.ds.bpm.verification.model.Structure;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class used to call the NuSMV2 model checker and parse its results.
 */
public class NuSMVChecker extends Checker {
	protected final static Pattern regexSpecificationResultLine = Pattern.compile("^\\s*--\\s*specification\\s*(.*)\\s+is\\s(true|false)$");
	protected final static Pattern regexSpecificationCounterExampleStateLine = Pattern.compile("^\\s+state\\s=\\s(.*)\\s*$");
	protected final static Pattern regexSpecificationCounterExampleAPLine = Pattern.compile("^\\s+(.*)\\s=\\s(FALSE|TRUE)\\s*$");

	private File file;

	/**
	 * Creates a NuSMV2 model checker.
	 *
	 * @param checker file that contains the path to the NuSMV2 model checker's executable.
	 */
	public NuSMVChecker(File checker) {
		super(checker);
	}

	@Override
	public void addFormula(Formula formula, Specification specification, AtomicPropositionMap<CompositeExpression> atomicPropositionMap) {
		try {
			NuSMVFormula nuSMVFormula = new NuSMVFormula(formula, specification, atomicPropositionMap);
			formulas.add(nuSMVFormula);
			Logger.log("Including specification " + specification.getId() + " with the formula " + nuSMVFormula.getInputFormula(), LogEvent.VERBOSE);
		} catch (FormulaException e) {
			Logger.log("Failed to include specification " + specification.getId(), LogEvent.ERROR);
		}
	}

	@Override
	public void createModel(Structure<? extends State<?>> structure) throws CheckerException {
		NuSMVFileWriter fileWriter;
		if (out == null)
			fileWriter = new NuSMVFileWriter(structure, formulas, id);
		else
			fileWriter = new NuSMVFileWriter(structure, formulas, id, out);

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
		} catch (Exception e) {
			e.printStackTrace();
			throw new CheckerException("Failed to call NuSMV2:");
		}

		return results;
	}

	/**
	 * Reads and returns the error stream of the given process.
	 *
	 * @param proc the process for which to read the error stream.
	 * @return a list of lines read.
	 * @throws IOException when reading the error stream fails.
	 */
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

	/**
	 * Reads and parses the results returned by the model checker.
	 *
	 * @param proc the process from which to read the results.
	 * @return a list of events that describe the results of verification.
	 * @throws IOException when reading the results fails.
	 */
	private List<VerificationEvent> parseResults(Process proc) throws IOException {
		List<VerificationEvent> results = new ArrayList<>();
		VerificationEvent event = null;

		//inputStream
		String line = null;
		InputStream stdin = proc.getInputStream();
		InputStreamReader in = new InputStreamReader(stdin);
		BufferedReader bir = new BufferedReader(in);

		while ((line = bir.readLine()) != null) {
			Logger.log(line, LogEvent.DEBUG);

			Matcher regexSpecificationResultLineMatcher = regexSpecificationResultLine.matcher(line);
			Matcher regexSpecificationCounterExampleStateLineMatcher = regexSpecificationCounterExampleStateLine.matcher(line);
			Matcher regexSpecificationCounterExampleAPLineMatcher = regexSpecificationCounterExampleAPLine.matcher(line);

			if (regexSpecificationResultLineMatcher.matches()) {
				event = createResult(regexSpecificationResultLineMatcher.group(1), regexSpecificationResultLineMatcher.group(2));
				results.add(event);
			} else if (regexSpecificationCounterExampleStateLineMatcher.matches()) {
				addCounterExampleState(event);
			} else if (regexSpecificationCounterExampleAPLineMatcher.matches()) {
				setCounterExampleStateAP(event, regexSpecificationCounterExampleAPLineMatcher.group(1), regexSpecificationCounterExampleAPLineMatcher.group(2));
			}
		}

		bir.close();
		in.close();

		return results;
	}

	/**
	 * Creates a VerificationEvent that describes a result of verification.
	 *
	 * @param formula the formula that the result pertains to.
	 * @param result  the result of verification.
	 * @return a VerificationEvent that describes a result of verification.
	 */
	protected VerificationEvent createResult(String formula, String result) {
		VerificationEvent event = new VerificationEvent(parseFormula(formula), Boolean.parseBoolean(result));

		if (event.getFormula() != null)
			formulas.remove(event.getFormula());

		return event;
	}

	/**
	 * Adds a counter example state to the given event.
	 *
	 * @param event the event to which the counter example state is to be added.
	 */
	protected void addCounterExampleState(VerificationEvent event) {
		Logger.log("Adding counter example state", LogEvent.DEBUG);
		if (event != null) {
			List<List<String>> trace = event.getCounterExample();
			List<String> e = new ArrayList<>();
			if (!trace.isEmpty()) e.addAll(trace.get(trace.size() - 1));
			trace.add(e);
		}
	}

	/**
	 * Sets the value of an atomic proposition to the last counter example state of the given event.
	 *
	 * @param event the event to which the value of an atomic proposition is to be added.
	 * @param ap    the atomic proposition to be added.
	 * @param value the value of the atomic proposition.
	 */
	protected void setCounterExampleStateAP(VerificationEvent event, String ap, String value) {
		boolean val = Boolean.parseBoolean(value);
		Logger.log("Adding counter example AP " + (val ? "" : "!") + ap, LogEvent.DEBUG);

		if (event != null) {
			List<List<String>> trace = event.getCounterExample();
			if (trace.isEmpty()) addCounterExampleState(event);

			String id = (event.getFormula() == null ? ap : event.getFormula().getAtomicPropositionMap().getID(ap).getOriginalExpression());
			List<String> e = trace.get(event.getCounterExample().size() - 1);

			if (val) e.add(id);
			else e.remove(id);
		}
	}

	/**
	 * Matches and returns the first CheckerFormula that equals the given formula in String format.
	 *
	 * @param formula the given formula in String format.
	 * @return the first CheckerFormula that equals the given formula in String format, or null.
	 */
	protected CheckerFormula parseFormula(String formula) {
		return formulas.stream().filter(f -> f.equals(formula.trim())).findFirst().orElse(null);
	}
}
