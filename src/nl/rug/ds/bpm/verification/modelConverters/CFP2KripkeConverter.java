package nl.rug.ds.bpm.verification.modelConverters;

import nl.rug.ds.bpm.verification.comparators.StringComparator;
import nl.rug.ds.bpm.verification.models.kripke.Kripke;
import nl.rug.ds.bpm.verification.models.kripke.State;

import java.util.*;

public class CFP2KripkeConverter {
	private CFP cfp;
    private Kripke kripke;


    public CFP2KripkeConverter(CFP cfp) {

        this.cfp = cfp;
    }


    public Kripke convert() {
        State.resetStateId();
        int[] m = cpn.getInitialMarking();
        kripke = new Kripke();

		
		
        return kripke;
    }
}
