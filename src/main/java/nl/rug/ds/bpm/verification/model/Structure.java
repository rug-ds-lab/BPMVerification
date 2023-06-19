package nl.rug.ds.bpm.verification.model;

import nl.rug.ds.bpm.util.exception.ConverterException;

import java.util.Set;

/**
 * Transition system interface for (Kripke) structures.
 */

public interface Structure<S extends State<S>> {
    /**
     * Add a state to the transition system, and add it to the set of initially accessible states.
     *
     * @param s the state.
     * @return s if new, otherwise the equaling known state.
     */
    S addInitial(S s) throws ConverterException;

    /**
     * Add a state to the transition system.
     *
     * @param s the state
     * @return s if new, otherwise the equaling known state.
     */
    S addState(S s) throws ConverterException;

    /**
     * Add a relation from the given current state to the given next state, add the given next state to the transition system if it is not known.
     *
     * <p>
     * The given current state must be included in this transition system already.
     * The given next state will be added to this transition system if it is not yet known.
     * Adds the (known) given next state as a next state of the given current state.
     * Adds the given current state as a previous state of the (known) given next state.
     * </p>
     *
     * @param current a state current to this transition system.
     * @param next    the state that must become accessible from the given current state.
     * @return either the added next state or an already known state that equals the added state.
     */
    S addNext(S current, S next) throws ConverterException;

    /**
     * Returns the set of atomic propositions that hold throughout the different states included in this transition system.
     *
     * @return the set of atomic propositions that hold throughout the different states included in this transition system.
     */
    Set<String> getAtomicPropositions();

    /**
     * Returns the set of states included in this transition system.
     *
     * @return the set of states included in this transition system.
     */
    Set<S> getStates();

    /**
     * Returns the set of initially accessible states included in this transition system.
     *
     * @return the set of initially accessible states included in this transition system.
     */
    Set<S> getInitial();

    /**
     * Returns the set of sink states included in this transition system, i.e., states with itself as next state.
     *
     * @return the set of sink states included in this transition system, i.e., states with itself as next state.
     */
    Set<S> getSinkStates();

    /**
     * Returns the number of different atomic propositions that hold within the different states of this transition system.
     *
     * @return the number of different atomic propositions that hold within the different states of this transition system.
     */
    long getAtomicPropositionCount();

    /**
     * Returns the number of different states of this transition system.
     *
     * @return the number of different states of this transition system.
     */
    long getStateCount();

    /**
     * Returns the number of different relations as next states within the different states of this transition system.
     *
     * @return the number of different relations as next states within the different states of this transition system.
     */
    long getRelationCount();

    @Override
    String toString();

    /**
     * Returns a string that describes the number of states, relations, and atomic propositions.
     *
     * @return a string that describes the number of states, relations, and atomic propositions.
     */
    String stats();
}
