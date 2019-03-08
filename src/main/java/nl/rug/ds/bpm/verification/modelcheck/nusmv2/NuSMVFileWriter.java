package nl.rug.ds.bpm.verification.modelcheck.nusmv2;

import nl.rug.ds.bpm.util.exception.CheckerException;
import nl.rug.ds.bpm.util.exception.FormulaException;
import nl.rug.ds.bpm.verification.model.kripke.Kripke;
import nl.rug.ds.bpm.verification.model.kripke.State;
import nl.rug.ds.bpm.verification.modelcheck.CheckerFormula;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NuSMVFileWriter {
	private File file;
	private String contents;

	public NuSMVFileWriter(Kripke kripke, List<CheckerFormula> formulas, int id) throws CheckerException {
		try {
			file = File.createTempFile("model" + id, ".smv");
		} catch (IOException e) {
			throw new CheckerException("Failed to write to file" +  file.toString());
		}
		create(kripke, formulas, id);
	}

	public NuSMVFileWriter(Kripke kripke, List<CheckerFormula> formulas, int id, File outputLocation) throws CheckerException {
		file = new File(outputLocation, "model" + id + ".smv");
		create(kripke, formulas, id);
	}

	public File getFile() {
		return file;
	}

	public String getContents() {
		return contents;
	}

	private void create(Kripke kripke, List<CheckerFormula> formulas, int id) throws CheckerException {
		StringBuilder sb = new StringBuilder();

		sb.append("MODULE main\n");
		sb.append(convertVAR(kripke));
		sb.append(convertDEFINE(kripke));
		sb.append(convertASSIGN(kripke));
		sb.append(convertFORMULAS(formulas));

		contents = sb.toString();

		try {
			if (file.length() > 0) {
				file.delete();
				file.createNewFile();
			}
		} catch (IOException e) {
			throw new CheckerException("Failed to overwrite file" +  file.toString());
		}


		try {
			PrintWriter writer = new PrintWriter(file);
			writer.println(contents);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			throw new CheckerException("Failed to write to file" +  file.toString());
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

	private String convertFORMULAS(List<CheckerFormula> formulas) {
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
}
