package nl.rug.ds.bpm.verification.models.conditional;

import nl.rug.ds.bpm.jaxb.specification.SpecificationSet;
import nl.rug.ds.bpm.pnml.EventHandler;
import nl.rug.ds.bpm.verification.models.kripke.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Heerko Groefsema on 07-Apr-17.
 */
public class ConditionalKripke {
	private Kripke kripke;
	private EventHandler eventHandler;
	private SpecificationSet specificationSet;
	
	public ConditionalKripke(EventHandler eventHandler, SpecificationSet specificationSet) {
		this.eventHandler = eventHandler;
		this.specificationSet = specificationSet;
	}
}
