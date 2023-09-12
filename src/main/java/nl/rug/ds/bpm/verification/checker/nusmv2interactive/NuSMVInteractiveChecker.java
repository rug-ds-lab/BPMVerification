package nl.rug.ds.bpm.verification.checker.nusmv2interactive;

import nl.rug.ds.bpm.util.exception.CheckerException;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.verification.checker.CheckerFormula;
import nl.rug.ds.bpm.verification.checker.nusmv2.NuSMVChecker;
import nl.rug.ds.bpm.verification.checker.nusmv2.NuSMVFileWriter;
import nl.rug.ds.bpm.verification.event.VerificationEvent;
import nl.rug.ds.bpm.verification.model.State;
import nl.rug.ds.bpm.verification.model.Structure;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * Class used to call the NuSMV2 model checker interactively and parse its results.
 */
public class NuSMVInteractiveChecker extends NuSMVChecker {
	private File file;
	private Process proc;
	private NuSMVScanner scanner;

	/**
	 * Creates an interactive NuSMV2 model checker.
	 *
	 * @param checker file that contains the path to the NuSMV2 model checker's executable.
	 */
	public NuSMVInteractiveChecker(File checker) throws CheckerException {
		super(checker);

		try {
			proc = Runtime.getRuntime().exec(executable.getAbsoluteFile() + " -int");
			scanner = new NuSMVScanner(proc);
		} catch (Exception e) {
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
    public void createModel(Structure<? extends State<?>> structure) throws CheckerException {
        inputChecker = new StringBuilder();
        outputChecker = new StringBuilder();

        List<CheckerFormula> formulaList = formulas.stream().filter(f -> f.getFormula().getLanguage().equalsIgnoreCase("fairness")).collect(Collectors.toList());

        NuSMVFileWriter fileWriter;
        if (out == null)
            fileWriter = new NuSMVFileWriter(structure, formulaList, id);
        else
            fileWriter = new NuSMVFileWriter(structure, formulaList, id, out);

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

			List<CheckerFormula> formulaList = new ArrayList<>();
			for (CheckerFormula f: formulas)
				if (f.getFormula().getLanguage().equalsIgnoreCase("ctlspec") || f.getFormula().getLanguage().equalsIgnoreCase("ltlspec"))
					formulaList.add(f);

			for (CheckerFormula formula: formulaList) {
				scanner.writeln((formula.getFormula().getLanguage().equalsIgnoreCase("ctlspec") ? "check_ctlspec" : "check_ltlspec") + " -p " + formula.getCheckerFormula());

				VerificationEvent event = null;
				String line = "";
				while (scanner.hasNext()) {
					line = scanner.next();
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
			}
		}
		catch (Exception e) {
			while (scanner.hasNext())
				outputChecker.append(scanner.next()).append("\n");
			e.printStackTrace();
			throw new CheckerException("Failed to call NuSMV2:\n" + outputChecker);
		}

		for (String line : scanner.getErrors()) {
			String trimmed = line.trim().strip();
			if (!trimmed.isEmpty())
				outputChecker.append(line).append("\n");
		}

		return results;
	}
}