package nl.rug.ds.bpm.verification.model.generic;

import nl.rug.ds.bpm.verification.model.State;

import java.util.Set;

public abstract class MarkedState<S extends MarkedState<S>> extends AbstractState<S> implements State<S> {
    protected String marking;
    protected String APHash;

    /**
     * Creates a marked state.
     *
     * @param atomicPropositions The set of atomic propositions that hold in this state.
     */
    public MarkedState(Set<String> atomicPropositions) {
        super(atomicPropositions);
        APHash = hash;
    }

    /**
     * Creates a marked  state.
     *
     * @param marking            a unique atomic proposition representing the state of a Petri net.
     * @param atomicPropositions the set of atomic propositions that hold in this state.
     */
    public MarkedState(String marking, Set<String> atomicPropositions) {
        this(atomicPropositions);
        this.marking = marking;
        hash = marking + "=" + APHash;
    }

    /**
     * Returns the hash of the atomic propositions.
     *
     * @return the hash of the atomic propositions.
     */
    public String APHash() {
        return APHash;
    }

    @Override
    public void removeAtomicPropositions(Set<String> APs) {
        atomicPropositions.removeAll(APs);

        APHash = String.join("", atomicPropositions);
        hash = marking + "=" + APHash;
    }

    @Override
    public void addAtomicPropositions(Set<String> APs) {
        atomicPropositions.addAll(APs);

        APHash = String.join("", atomicPropositions);
        hash = marking + "=" + APHash;
    }

    /**
     * Returns true if the same set of atomic propositions hold in this state and the given Kripke state.
     *
     * @param o the given Kripke state.
     * @return true if the same set of atomic propositions hold in this state and the given Kripke state.
     */
    public boolean atomicPropositionsEquals(S o) {
        return APHash.equals(o.APHash());
    }

}
