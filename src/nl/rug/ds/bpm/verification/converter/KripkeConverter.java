package nl.rug.ds.bpm.verification.converter;

import nl.rug.ds.bpm.specification.jaxb.Condition;
import nl.rug.ds.bpm.verification.model.kripke.Kripke;
import nl.rug.ds.bpm.verification.stepper.Stepper;
import nl.rug.ds.bpm.verification.util.EventHandler;

import java.util.List;

public class KripkeConverter {
    private EventHandler eventHandler;
	private Stepper paralelStepper;
    private Kripke kripke;
    private List<Condition> conditions;

    public KripkeConverter(EventHandler eventHandler, Stepper paralelStepper, List<Condition> conditions) {
        this.eventHandler = eventHandler;
        this.paralelStepper = paralelStepper;
        this.conditions = conditions;
    }

    public Kripke convert() {
        kripke = new Kripke();

		
		
        return kripke;
    }
}
