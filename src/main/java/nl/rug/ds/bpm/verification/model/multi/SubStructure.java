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
        if (states.size() >= maximum || atomicPropositions.size() >= maximum)
            throw new ConverterException("Maximum state space reached (at " + maximum + " states/propositions)");

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
            TreeSet<String> ap = new TreeSet<String>(new ComparableComparator<String>());
            ap.addAll(s.getAtomicPropositions());
            ap.retainAll(this.atomicPropositions);

            StutterState ss = new StutterState(ap, this);
            ss.addSubState(s);

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

        //If next is new (nparent == null)
        //    If next has the same relevant AP as the parent of current
        //        Add next to current parent
        //    Else next has different relevant AP than the parent of current
        //        Create a new parent for next
        //Else next is not new (nparent != null)
        //    If next has not the same parent as current (cparent != nparent)
        //        If next has the same relevant AP as the parent of current
        //            Merge parents
        //        Else next has different relevant AP than the parent of current
        //            If next is a merge from states of multiple different parents
        //                Create a new parent for next
        //                Remove next from nparent
        //                For any future state of next within cparent, create and assign a new parent
        //            Else next is a merge from single parent
        //                Do nothing
        //    Else next has the same parent as current (cparent == nparent)
        //        Do nothing

        if (nparent == null) {
            TreeSet<String> nap = new TreeSet<String>(new ComparableComparator<String>());
            nap.addAll(next.getAtomicPropositions());
            nap.retainAll(this.atomicPropositions);

            if (nap.equals(cparent.getAtomicPropositions()))
                next.setParent(this, cparent);
            else {
                nparent = createState(nap);
                next.setParent(this, nparent);
            }
        } else if (cparent != nparent) {
            if (cparent.equals(nparent))
                cparent.merge(nparent);
            else {
                TreeSet<State> previousParents = new TreeSet<State>(new ComparableComparator<State>());
                for (State previous : next.getPreviousStates())
                    previousParents.add(((MultiState) previous).getParent(this));

                if (previousParents.size() > 1) {
                    StutterState newparent = createState(next.getParent(this).getAtomicPropositions());
                    StutterState nextparent = createState(next.getParent(this).getAtomicPropositions());
                    next.setParent(this, newparent);

                    for (State nextOfNext : next.getNextStates())
                        ((MultiState) nextOfNext).updateParent(this, nparent, nextparent);

                    if (nextparent.getSubStates().isEmpty())
                        states.remove(nextparent);

                    if (nparent.getSubStates().isEmpty())
                        states.remove(nparent);

                    nparent = newparent;
                }
            }
        }

        return nparent;
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
     * Creates and adds a new StutterState to this SubStructure.
     *
     * @param atomicPropositions the set of atomic propositions that hold in this state.
     * @return the created StutterState.
     */
    public StutterState createState(Set<String> atomicPropositions) {
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
