package nl.rug.ds.bpm.pnml.events;

import nl.rug.ds.bpm.jaxb.specification.Specification;

import java.awt.*;

/**
 * Created by Heerko Groefsema on 10-Apr-17.
 */
public class VerificationEvent {
	private boolean eval;
	private Specification specification;
	
	public VerificationEvent(Specification specification, boolean eval) {
		this.specification = specification;
		this.eval = eval;
	}
	
	public String getId() {
		return specification.getId();
	}
	
	public String getType() {
		return specification.getType();
	}
	
	public String getFormula() {
		return specification.toString();
	}
	
	public boolean getVerificationResult() {
		return eval;
	}
	
	public Specification getSpecification() {
		return specification;
	}
	
	public String toString() {
		return "Specification " + specification.getId() + " " + specification.toString() + " evaluated to " + eval;
	}
}
