package nl.rug.ds.bpm.verification.model.generic.optimizer.proposition;

import nl.rug.ds.bpm.util.comparator.StringComparator;
import nl.rug.ds.bpm.verification.model.generic.AbstractState;
import nl.rug.ds.bpm.verification.model.generic.AbstractStructure;

import java.util.Set;
import java.util.TreeSet;

/**
 * Class that optimizes a Structure by removing irrelevant atomic propositions.
 */
public class PropositionOptimizer {
    private final AbstractStructure<? extends AbstractState<?>> structure;
    private final TreeSet<String> optimizedPropositions;

    /**
     * Creates a PropositionOptimizer to optimize the given Structure.
     *
     * @param structure the Structure to optimize.
     */
    public PropositionOptimizer(AbstractStructure<? extends AbstractState<?>> structure) {
        this.structure = structure;
        optimizedPropositions = new TreeSet<>(new StringComparator());
    }

    /**
     * Creates a PropositionOptimizer and optimizes the given Structure by removing the given set of unused atomic propositions.
     *
     * @param structure the Structure to optimize.
     * @param AP        the set of atomic propositions to remove.
     */
    public PropositionOptimizer(AbstractStructure<? extends AbstractState<?>> structure, Set<String> AP) {
        this(structure);
        optimize(AP);
    }

    /**
     * Optimizes the given Structure by removing the given set of unused atomic propositions.
     *
     * @param AP the set of atomic propositions to remove.
     */
    public void optimize(Set<String> AP) {
        optimizedPropositions.addAll(AP);

        for (AbstractState<?> s : structure.getStates())
            s.removeAtomicPropositions(AP);

        structure.getAtomicPropositions().removeAll(AP);
    }

    @Override
    public String toString() {
        return "Reduction of " + optimizedPropositions.size() + " propositions " +
                "(" + String.join(", ", optimizedPropositions) + ")";
    }
}
