package nl.rug.ds.bpm.verification.model.multi;

import nl.rug.ds.bpm.expression.CompositeExpression;
import nl.rug.ds.bpm.expression.ExpressionBuilder;
import nl.rug.ds.bpm.expression.LogicalType;
import nl.rug.ds.bpm.specification.jaxb.SpecificationSet;
import nl.rug.ds.bpm.util.comparator.ComparableComparator;
import nl.rug.ds.bpm.util.exception.ConverterException;
import nl.rug.ds.bpm.verification.model.State;
import nl.rug.ds.bpm.verification.model.generic.AbstractStructure;

import java.util.Set;
import java.util.TreeSet;

/**
 * Class that implements a substructure of a multi-structure.
 */
public class SubStructure extends AbstractStructure {
    private final SpecificationSet specificationSet;
    private final CompositeExpression conditionExpression;

    /**
     * Creates a substructure.
     *
     * @param specificationSet   the SpecificationSet applicable to the SubStructure.
     * @param atomicPropositions the set of atomic propositions relevant to this substructure.
     */
    public SubStructure(SpecificationSet specificationSet, Set<String> atomicPropositions) {
        super(specificationSet.getConditions());
        this.specificationSet = specificationSet;
        this.atomicPropositions.addAll(atomicPropositions);

        conditionExpression = new CompositeExpression(LogicalType.AND);

        for (String condition : conditions)
            conditionExpression.addArgument(ExpressionBuilder.parseExpression(condition));
    }

    /**
     * Add a state to this substructure.
     *
     * @param s the state.
     * @return s if new, otherwise the equaling known state.
     */
    @Override
    public synchronized State addInitial(State s) throws ConverterException {
        State known = addState(s);
        initial.add(((MultiState) known).getParent(this));

        return known;
    }

    /**
     * Add a state to this substructure.
     *
     * @param s the state to add. Must be known to the parent structure.
     * @return s if new, otherwise the equaling known state.
     */
    public synchronized State addState(MultiState s) {
        if (s.getParent(this) == null) {
            StutterState ss = new StutterState(createAtomicPropositions(s.getAtomicPropositions()), this);
            ss.addEntryState(s);
            s.setParent(this, ss);
            states.add(ss);
        }

        return s;
    }

    @Override
    public synchronized State addState(State s) {
        return (s.getClass() == MultiState.class ? addState((MultiState) s) : null);
    }

    /**
     * Add a relation from the given current state to the given next state.
     * Assign stutter states accordingly, but don't add relations between stutter states (done after assigning all).
     *
     * @param current a state current to this transition system.
     * @param next    the state that must become accessible from the given current state.
     * @return next if new, otherwise the known equaling state.
     */
    public synchronized State addNext(MultiState current, MultiState next) {
        StutterState cparent = current.getParent(this);
        StutterState nparent = next.getParent(this);

        Set<String> nextRelAP = createAtomicPropositions(next.getAtomicPropositions());

        boolean nextIsNew = nparent == null; // If true, next is not yet in this substructure.
        boolean nextEqualsCurrentParent = cparent.getAtomicPropositions().equals(nextRelAP); // If true, next belongs in cparent, else next is an entry state and current an exit state.
        boolean arcCreatesSink = current == next; // The arc is a back arc from and to the same state.
        boolean arcCreatesLoop = nparent != null && next.isInLoop(this); // The arc is a back arc.
        boolean haveMergableParents = cparent != nparent && cparent.equals(nparent); // The arc connects mergable parents

        // Initialize nparent if needed
        if (nextIsNew && nextEqualsCurrentParent)
            nparent = cparent;
        else if (nextIsNew) {
            nparent = createParent(nextRelAP);
        }

        // Add next to the nparent
        nparent.addSubState(next);
        next.setParent(this, nparent);

        // Add next as an entry state if it differs from cparent
        if (!nextEqualsCurrentParent)
            nparent.addEntryState(next);

        // Merge and split parents if needed
        if (haveMergableParents) {
            cparent.merge(nparent);
            boolean split = cparent.split(createParent(cparent.getAtomicPropositions()));
            while (split)
                split = cparent.split(createParent(cparent.getAtomicPropositions()));
        }

        // Add current as an exit state and split if needed
        if (arcCreatesLoop || arcCreatesSink || !nextEqualsCurrentParent) {
            cparent.addExitState(current);
            boolean split = cparent.split(createParent(cparent.getAtomicPropositions()));
            while (split)
                split = cparent.split(createParent(cparent.getAtomicPropositions()));
        }

        return next;
    }

    @Override
    public synchronized State addNext(State current, State next) {
        State r = null;
        if (current.getClass() == MultiState.class && next.getClass() == MultiState.class)
            r = addNext((MultiState) current, (MultiState) next);
        return r;
    }

    /**
     * Returns the SpecificationSet applicable to the SubStructure.
     *
     * @return the SpecificationSet.
     */
    public SpecificationSet getSpecificationSet() {
        return specificationSet;
    }

    /**
     * Returns true iff the given expression contradicts the conditions of this SubStructure.
     *
     * @param expression the given expression.
     * @return true iff the given expression contradicts the conditions of this SubStructure, and false otherwise.
     */
    public boolean contradicts(CompositeExpression expression) {
        return expression.contradicts(conditionExpression);
    }

    /**
     * Returns the set of relevant atomic propositions to this SubStructure within a given set.
     *
     * @param atomicPropositions a given set of atomic propositions
     * @return the set of relevant atomic propositions to this SubStructure within a given set.
     */
    public synchronized Set<String> createAtomicPropositions(Set<String> atomicPropositions) {
        TreeSet<String> ap = new TreeSet<String>(new ComparableComparator<String>());

        ap.addAll(atomicPropositions);
        ap.retainAll(this.atomicPropositions);

        return ap;
    }

    /**
     * Creates and adds a new StutterState to this SubStructure.
     *
     * @param atomicPropositions the set of atomic propositions that hold in this state.
     * @return the created StutterState.
     */
    public synchronized StutterState createParent(Set<String> atomicPropositions) {
        TreeSet<String> ap = new TreeSet<String>(new ComparableComparator<String>());
        ap.addAll(atomicPropositions);
        StutterState state = new StutterState(ap, this);
        states.add(state);

        return state;
    }

    /**
     * Finalizes this SubStructure by adding relations between stutter states, adding sinks,
     * adding a safety state, and clearing pointers to the full state space.
     */
    public void finalizeStructure() {
        states.add(new StutterState(atomicPropositions, this));

        for (State stutterState : states) {
            for (State child : ((StutterState) stutterState).getSubStates()) {
                for (State nextChild : child.getNextStates()) {
                    StutterState nextParent = ((MultiState) nextChild).getParent(this);
                    if (stutterState.compareTo(nextParent) != 0) {
                        stutterState.addNext(nextParent);
                        nextParent.addPrevious(stutterState);
                    }
                }
            }

            ((StutterState) stutterState).getSubStates().clear();

            if (stutterState.getNextStates().isEmpty()) {
                stutterState.addNext(stutterState);
                stutterState.addPrevious(stutterState);
            }
        }
    }
}
