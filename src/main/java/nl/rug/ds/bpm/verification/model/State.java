package nl.rug.ds.bpm.verification.model;

import java.util.Set;

/**
 * Transition system state interface.
 */

public interface State<S extends State<S>> extends Comparable<S> {

    /**
     * Sets the unique id of the state.
     */
    void setId();

    /**
     * Returns the unique id of this state.
     *
     * @return the unique id of this state.
     */
    String getId();

    /**
     * Returns the unique state number.
     *
     * @return the unique state number.
     */
    long getIdNumber();

    /**
     * Returns the hash code representing this state.
     *
     * @return the hash code representing this state.
     */
    String hash();

    /**
     * Returns the set of atomic proportions that hold in this state.
     *
     * @return the set of atomic proportions that hold in this state.
     */
    Set<String> getAtomicPropositions();

    /**
     * Removes a set of atomic propositions from holding in this state and recreates hashes.
     *
     * @param APs the set of atomic propositions to remove.
     */
    void removeAtomicPropositions(Set<String> APs);

    /**
     * Adds a set of atomic propositions that hold in this state and recreates hashes.
     *
     * @param APs the set of atomic propositions to add.
     */
    void addAtomicPropositions(Set<String> APs);

    /**
     * Add a state as a next state that is accessible from this state.
     *
     * @param s the next state.
     * @return true if the set of next states did not already contain the given state.
     */
    boolean addNext(S s);

    /**
     * Add a set of states as next states that are accessible from this state.
     *
     * @param s the set of next states.
     * @return true if the set of next states changed as a result of this call.
     */
    boolean addNext(Set<S> s);

    /**
     * Returns the set of next states that are accessible from this state.
     *
     * @return the set of next states that are accessible from this state.
     */
    Set<S> getNextStates();

    /**
     * Add a state as a previous state from which this state is accessible.
     *
     * @param s the previous state.
     * @return true if the set of previous states did not already contain the given state.
     */
    boolean addPrevious(S s);

    /**
     * Add a set of states as previous states from which this state is accessible.
     *
     * @param s the set of previous states.
     * @return true if the set of previous states changed as a result of this call.
     */
    boolean addPrevious(Set<S> s);

    /**
     * Returns the set of previous states from which this state is accessible.
     *
     * @return the set of previous states from which this state is accessible.
     */
    Set<S> getPreviousStates();

    @Override
    String toString();

    @Override
    boolean equals(Object arg0);
}
