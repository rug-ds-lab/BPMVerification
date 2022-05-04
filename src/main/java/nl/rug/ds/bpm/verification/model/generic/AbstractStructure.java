package nl.rug.ds.bpm.verification.model.generic;

import nl.rug.ds.bpm.specification.jaxb.Condition;
import nl.rug.ds.bpm.util.comparator.ComparableComparator;
import nl.rug.ds.bpm.util.exception.ConverterException;
import nl.rug.ds.bpm.verification.model.ConditionalStructure;
import nl.rug.ds.bpm.verification.model.State;
import nl.rug.ds.bpm.verification.model.Structure;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * An abstract transition system representing a (Kripke) structure.
 */
public abstract class AbstractStructure implements Structure, ConditionalStructure {
    protected static long maximum = Long.MAX_VALUE;

    protected TreeSet<String> atomicPropositions;
    protected TreeSet<State> states;
    protected TreeSet<State> initial;

    protected HashSet<String> conditions;

    /**
     * Creates an abstract structure.
     */
    public AbstractStructure() {
        atomicPropositions = new TreeSet<String>(new ComparableComparator<String>());
        states = new TreeSet<State>(new ComparableComparator<State>());
        initial = new TreeSet<State>(new ComparableComparator<State>());
        conditions = new HashSet<>();
    }

    /**
     * Creates an abstract ConditionalStructure
     *
     * @param conditions the conditions that apply to this structure
     */
    public AbstractStructure(Set<String> conditions) {
        this();
        addConditions(conditions);
    }

    /**
     * Creates an abstract ConditionalStructure
     *
     * @param conditions the conditions that apply to this structure
     */
    public AbstractStructure(List<Condition> conditions) {
        this();
        addConditions(conditions);
    }

    /**
     * Gets the maximum number of states in this structure.
     *
     * @return the currently set maximum number of states.
     */
    public static long getMaximum() {
        return maximum;
    }

    /**
     * Sets the maximum number of states in this structure.
     *
     * @param maximum the maximum number of states.
     */
    public static void setMaximum(long maximum) {
        AbstractStructure.maximum = maximum;
    }

    /**
     * Add a state to the transition system, and add it to the set of initially accessible states.
     *
     * @param s the state.
     * @return s if new, otherwise the equaling known state.
     */
    public synchronized State addInitial(State s) throws ConverterException {
        if (states.size() >= maximum || atomicPropositions.size() >= maximum)
            throw new ConverterException("Maximum state space reached (at " + maximum + " states/propositions)");

        State known = addState(s);
        initial.add(known);

        return known;
    }

    /**
     * Add a state to the transition system.
     *
     * @param s the state
     * @return s if new, otherwise the equaling known state.
     */
    public synchronized State addState(State s) throws ConverterException {
        if (states.size() >= maximum || atomicPropositions.size() >= maximum)
            throw new ConverterException("Maximum state space reached (at " + maximum + " states/propositions)");

        State known = states.ceiling(s);

        if (!s.equals(known)) {
            known = s;
            states.add(s);
            atomicPropositions.addAll(s.getAtomicPropositions());
        }

        return known;
    }

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
    public synchronized State addNext(State current, State next) throws ConverterException {
        if (states.size() >= maximum || atomicPropositions.size() >= maximum)
            throw new ConverterException("Maximum state space reached (at " + maximum + " states/propositions)");

        State known = addState(next);

        current.addNext(known);
        known.addPrevious(current);

        return known;
    }

    /**
     * Returns the set of atomic propositions that hold throughout the different states included in this transition system.
     *
     * @return the set of atomic propositions that hold throughout the different states included in this transition system.
     */
    public TreeSet<String> getAtomicPropositions() {
        return atomicPropositions;
    }

    /**
     * Returns the set of states included in this transition system.
     *
     * @return the set of states included in this transition system.
     */
    public Set<State> getStates() {
        return states;
    }

    /**
     * Returns the set of initially accessible states included in this transition system.
     *
     * @return the set of initially accessible states included in this transition system.
     */
    public TreeSet<State> getInitial() {
        return initial;
    }

    /**
     * Returns the set of sink states included in this transition system, i.e., states with itself as next state.
     *
     * @return the set of sink states included in this transition system, i.e., states with itself as next state.
     */
    public Set<State> getSinkStates() {
        return states.stream().filter(s -> s.getNextStates().size() == 1).filter(s -> s.getNextStates().iterator().next() == s).collect(Collectors.toSet());
    }

    /**
     * Returns the number of different atomic propositions that hold within the different states of this transition system.
     *
     * @return the number of different atomic propositions that hold within the different states of this transition system.
     */
    public long getAtomicPropositionCount() {
        return atomicPropositions.size();
    }

    /**
     * Returns the number of different states of this transition system.
     *
     * @return the number of different states of this transition system.
     */
    public long getStateCount() {
        return states.size();
    }

    /**
     * Returns the number of different relations as next states within the different states of this transition system.
     *
     * @return the number of different relations as next states within the different states of this transition system.
     */
    public long getRelationCount() {
        return states.stream().map(n -> n.getNextStates().size()).count();
    }

    @Override
    public String toString() {
        String c = "Conditions: {" + String.join(", ", conditions) + "}\n";
        String ap = "Atomic Propositions: {" + String.join(", ", atomicPropositions) + "}\n\n";

        StringBuilder st = new StringBuilder("States:\n");
        for (State s : states)
            st.append(s).append("\n");

        StringBuilder rel = new StringBuilder("\nRelations:\n");
        for (State s : states) {
            if (!s.getNextStates().isEmpty()) {
                rel.append(s).append(" -> ");
                for (State t : s.getNextStates())
                    rel.append(t).append(" ");
                rel.append("\n");
            }
        }

        return c + ap + st + rel + stats() + "\n";
    }

    /**
     * Returns a string that describes the number of states, relations, and atomic propositions.
     *
     * @return a string that describes the number of states, relations, and atomic propositions.
     */
    public String stats() {
        return "|S| = " + getStateCount() + ", |R| = " + getRelationCount() + ", |AP| = " + getAtomicPropositionCount() + "\n";
    }

    /**
     * Adds the given Conditions to this structure.
     *
     * @param condition the Condition.
     */
    @Override
    public void addCondition(Condition condition) {
        this.conditions.add(condition.getCondition());
    }

    /**
     * Adds the given conditions to this structure.
     *
     * @param condition the condition.
     */
    @Override
    public void addCondition(String condition) {
        this.conditions.add(condition);
    }

    /**
     * Adds the given list of Conditions to this structure.
     *
     * @param conditions the conditions.
     */
    @Override
    public void addConditions(List<Condition> conditions) {
        for (Condition condition : conditions)
            addCondition(condition);
    }

    /**
     * Adds the given set of conditions to this structure.
     *
     * @param conditions the conditions.
     */
    @Override
    public void addConditions(Set<String> conditions) {
        this.conditions.addAll(conditions);
    }

    /**
     * Returns the conditions of this structure.
     *
     * @return the set of conditions.
     */
    @Override
    public Set<String> getConditions() {
        return conditions;
    }

}
