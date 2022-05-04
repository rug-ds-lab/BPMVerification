package nl.rug.ds.bpm.verification.model.kripke;

import nl.rug.ds.bpm.specification.jaxb.Condition;
import nl.rug.ds.bpm.verification.model.generic.AbstractStructure;

import java.util.List;
import java.util.Set;

/**
 * Class that implements a Kripke structure transition system.
 */
public class KripkeStructure extends AbstractStructure {

    /**
     * Creates a Kripke structure.
     */
    public KripkeStructure() {
        super();
    }

    /**
     * Creates a Kripke structure given a list of Conditions
     *
     * @param conditions the conditions that apply to this Kripke structure
     */
    public KripkeStructure(List<Condition> conditions) {
        super(conditions);
    }

    /**
     * Creates a Kripke structure given a set of conditions
     *
     * @param conditions the conditions that apply to this Kripke structure
     */
    public KripkeStructure(Set<String> conditions) {
        super(conditions);
    }
}
