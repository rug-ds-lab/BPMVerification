package nl.rug.ds.bpm.verification.model.multi;

import nl.rug.ds.bpm.expression.CompositeExpression;
import nl.rug.ds.bpm.expression.ExpressionBuilder;
import nl.rug.ds.bpm.expression.LogicalType;
import nl.rug.ds.bpm.specification.jaxb.SpecificationSet;
import nl.rug.ds.bpm.util.comparator.ComparableComparator;
import nl.rug.ds.bpm.util.exception.ConverterException;
import nl.rug.ds.bpm.verification.model.generic.AbstractStructure;

import java.util.Set;
import java.util.TreeSet;

/**
 * Class that implements a substructure of a multi-structure.
 */
public class Partition extends AbstractStructure<Block> {
    private static int i = 0;
    private final String id;
    private final Set<MultiState> initialSubStates;
    private final SpecificationSet specificationSet;
    private final CompositeExpression conditionExpression;

    /**
     * Creates a substructure.
     *
     * @param specificationSet   the SpecificationSet applicable to the SubStructure.
     * @param atomicPropositions the set of atomic propositions relevant to this substructure.
     */
    public Partition(SpecificationSet specificationSet, Set<String> atomicPropositions) {
        super(specificationSet.getConditions());
        this.specificationSet = specificationSet;
        this.atomicPropositions.addAll(atomicPropositions);

        initialSubStates = new TreeSet<>(new ComparableComparator<MultiState>());

        conditionExpression = new CompositeExpression(LogicalType.AND);

        for (String condition : conditions)
            conditionExpression.addArgument(ExpressionBuilder.parseExpression(condition));

        id = "P" + i++;
    }

    /**
     * Returns the id of this partition.
     *
     * @return the id.
     */
    public String getId() {
        return id;
    }

    /**
     * Add a state to this substructure.
     *
     * @param s the state.
     * @return s if new, otherwise the equaling known state.
     */
    public synchronized MultiState addInitial(MultiState s) throws ConverterException {
        MultiState known = addState(s);
        initialSubStates.add(s);

        return known;
    }

    /**
     * Returns the set of substates that are initial states.
     *
     * @return set of states.
     */
    public Set<MultiState> getInitialSubStates() {
        return initialSubStates;
    }

    public synchronized MultiState addState(MultiState s) {
        if (s.getParent(this) == null) {
            Block ss = createParent(createAtomicPropositions(s.getAtomicPropositions()));
            s.setParent(this, ss);
            ss.addSubState(s);
            states.add(ss);
        }

        return s;
    }

    /**
     * Add a relation from the given current state to the given next state.
     * Assign blocks accordingly, track relations between blocks.
     *
     * @param current a state current to this transition system.
     * @param next    the state that must become accessible from the given current state.
     * @return next if new, otherwise the known equaling state.
     */
    public synchronized MultiState addNext(MultiState current, MultiState next) {
        Block cparent = current.getParent(this);
        Block nparent = next.getParent(this);

        Set<String> nextRelAP = createAtomicPropositions(next.getAtomicPropositions());

        boolean nextIsNew = nparent == null; // If true, next is not yet in this substructure.
        boolean nextEqualsCurrentParent = cparent.getAtomicPropositions().equals(nextRelAP); // If true, next belongs in cparent, else next is an entry state and current an exit state.

        // Initialize nparent if needed
        if (nextIsNew && nextEqualsCurrentParent)
            nparent = cparent;
        else if (nextIsNew)
            nparent = createParent(nextRelAP);

        // Add next to the nparent
        if (nextIsNew) {
            next.setParent(this, nparent);
            nparent.addSubState(next);
        }

        current.addNext(this, next);

        return next;
    }

    /**
     * Returns the SpecificationSet applicable to the Partition.
     *
     * @return the SpecificationSet.
     */
    public SpecificationSet getSpecificationSet() {
        return specificationSet;
    }

    /**
     * Returns true iff the given expression contradicts the conditions of this Partition.
     *
     * @param expression the given expression.
     * @return true iff the given expression contradicts the conditions of this Partition, and false otherwise.
     */
    public boolean contradicts(CompositeExpression expression) {
        return expression.contradicts(conditionExpression);
    }

    /**
     * Returns the set of relevant atomic propositions to this Partition within a given set.
     *
     * @param atomicPropositions a given set of atomic propositions
     * @return the set of relevant atomic propositions to this Partition within a given set.
     */
    public Set<String> createAtomicPropositions(Set<String> atomicPropositions) {
        TreeSet<String> ap = new TreeSet<String>(new ComparableComparator<String>());

        ap.addAll(atomicPropositions);
        ap.retainAll(this.atomicPropositions);

        return ap;
    }

    /**
     * Creates and adds a new Block to this Partition.
     *
     * @param atomicPropositions the set of atomic propositions that hold in this state.
     * @return the created Block.
     */
    public synchronized Block createParent(Set<String> atomicPropositions) {
        TreeSet<String> ap = new TreeSet<String>(new ComparableComparator<String>());
        ap.addAll(atomicPropositions);
        Block state = new Block(ap, this);
        states.add(state);

        return state;
    }

    /**
     * Clears the partition and its blocks from any references to multistates, leaving only the blocks.
     */
    public void clear() {
        initialSubStates.clear();

        for (Block block : states) {
            block.getSubStates().clear();
            block.getEntryStates().clear();
            block.getExitStates().clear();
        }
    }
}
