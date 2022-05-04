package nl.rug.ds.bpm.verification.model.multi;


import nl.rug.ds.bpm.verification.model.State;
import nl.rug.ds.bpm.verification.model.kripke.KripkeState;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class that implements a Kripke state with multiple parent stutter states.
 */
public class MultiState extends KripkeState {
    private final Map<SubStructure, StutterState> parents;

    /**
     * Creates a multi state.
     *
     * @param atomicPropositions The set of atomic propositions that hold in this state.
     */
    public MultiState(Set<String> atomicPropositions) {
        super(atomicPropositions);
        parents = new HashMap<>();
    }

    /**
     * Creates a multi state.
     *
     * @param marking            a unique atomic proposition representing the state of a Petri net.
     * @param atomicPropositions The set of atomic propositions that hold in this state.
     */
    public MultiState(String marking, Set<String> atomicPropositions) {
        super(marking, atomicPropositions);
        parents = new HashMap<>();
    }

    /**
     * Assigns the given StutterState as the parent of this state within the given substructure.
     *
     * @param subStructure the SubStructure the given parent belongs to.
     * @param stutterState the parent to assign.
     */
    public void setParent(SubStructure subStructure, StutterState stutterState) {
        stutterState.addSubState(this);

        StutterState current = parents.get(subStructure);
        if (current != null)
            current.removeSubState(this);

        parents.put(subStructure, stutterState);
    }

    /**
     * Returns the parentof this state within the given substructure.
     *
     * @param subStructure the SubStructure the returned parent belongs to.
     * @return the parent StutterState.
     */
    public StutterState getParent(SubStructure subStructure) {
        return parents.get(subStructure);
    }

    /**
     * Updates the parent of this and all next states within the given substructure while they belong to current.
     *
     * @param subStructure the SubStructure the given parent belongs to.
     * @param current      the currently assigned parent.
     * @param newparent    the parent to assign.
     */
    public void updateParent(SubStructure subStructure, StutterState current, StutterState newparent) {
        if (parents.get(subStructure) == current) {
            setParent(subStructure, newparent);

            for (State next : getNextStates())
                ((MultiState) next).updateParent(subStructure, current, newparent);
        }
    }

    @Override
    public String toString() {
        return id + ": {" + hash + " | " + parents.values().stream().map(Object::toString).collect(Collectors.joining(",")) + " }";
    }
}
