package nl.rug.ds.bpm.verification.modelConverters;

import nl.rug.ds.bpm.verification.comparators.StringComparator;
import nl.rug.ds.bpm.verification.models.kripke.Kripke;
import nl.rug.ds.bpm.verification.models.kripke.State;

import java.util.*;

public class Pnml2KripkeConverter {
	private PnmlParalelStepper paralelStepper;
    private Kripke kripke;


    public Pnml2KripkeConverter() {
        paralelStepper = new PnmlParalelStepper();
    }

    public Kripke convert() {
        kripke = new Kripke();

		
		
        return kripke;
    }
}
