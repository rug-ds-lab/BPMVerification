package nl.rug.ds.bpm.verification.model.multi.postprocess.stutter;

import nl.rug.ds.bpm.util.comparator.ComparableComparator;
import nl.rug.ds.bpm.util.exception.ConverterException;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.verification.model.multi.Block;
import nl.rug.ds.bpm.verification.model.multi.MultiState;
import nl.rug.ds.bpm.verification.model.multi.Partition;

import java.util.*;
import java.util.concurrent.RecursiveAction;

/**
 * Class that creates and initiates stutter equivalence actions for a set of substructures in parallel.
 */
public class MultiStutterMergeSplitAction extends RecursiveAction {
    private Partition partition;

    /**
     * Creates a RecursiveAction that initiates stutter equivalence actions for a given set of substructures.
     *
     * @param partitions the given set of substructures.
     */
    public MultiStutterMergeSplitAction(Set<Partition> partitions) {
        computeInitial(partitions);
    }

    /**
     * Creates a RecursiveAction that to calculate stutter equivalence for a given substructure.
     *
     * @param partition the given set of substructure.
     */
    public MultiStutterMergeSplitAction(Partition partition) {
        this.partition = partition;
    }

    private void computeInitial(Set<Partition> partitions) {
        Set<MultiStutterMergeSplitAction> actions = new HashSet<>();

        for (Partition partition : partitions)
            actions.add(new MultiStutterMergeSplitAction(partition));

        invokeAll(actions);
    }

    @Override
    protected void compute() {
        initialize();
        merge();
        split();
        connect();
    }

    /**
     * Initializes the blocks as per Groote's stutter equivalence algorithm.
     */
    private void initialize() {
        Logger.log("Initializing blocks of partition " + partition.getId(), LogEvent.INFO);

        for (Block block : partition.getStates()) {
            block.initialize();
            Logger.log("Block " + block, LogEvent.DEBUG);
        }
    }

    /**
     * Merges and re-initializes any connected blocks that are equal.
     */
    private void merge() {
        Logger.log("Merging blocks of partition " + partition.getId(), LogEvent.INFO);
        Set<Block> remove = new TreeSet<>(new ComparableComparator<>());

        Iterator<Block> blockIterator = partition.getStates().iterator();
        Block block = (blockIterator.hasNext() ? blockIterator.next() : null);
        boolean merged = true; //start true to not trigger iterator.next() again

        while (blockIterator.hasNext() || merged) {
            if (!merged)
                block = blockIterator.next();
            merged = false;

            if (block != null && !remove.contains(block)) {
                for (Block next : block.getNextParents()) {
                    if (block.canMerge(next) && !remove.contains(next)) {

                        if (Logger.getLogLevel() == LogEvent.DEBUG)
                            Logger.log("Merging stutter blocks " + block + " and " + next, LogEvent.DEBUG);


                        merged = true;
                        block.merge(next);
                        remove.add(next);
                    }
                }
                if (merged) {
                    block.initialize();

                    if (Logger.getLogLevel() == LogEvent.DEBUG)
                        Logger.log("Merger result " + block, LogEvent.DEBUG);
                }
            }
        }

        partition.getStates().removeAll(remove);
    }

    /**
     * Splits blocks according to Groote's stutter equivalence algorithm.
     */
    private void split() {
        Logger.log("Splitting blocks of partition " + partition.getId(), LogEvent.INFO);

        List<Block> toBeProcessed = new LinkedList<>(partition.getStates());
        List<Block> stable = new LinkedList<>();
        List<Block> BL = new LinkedList<>();

        while (!toBeProcessed.isEmpty()) {
            Logger.log("Checking stutter block (" + (1 + stable.size()) + "/" + (toBeProcessed.size() + stable.size()) + ") for any required splits", LogEvent.VERBOSE);

            Block bAccent = toBeProcessed.get(0);
            // Scan incoming relations
            for (MultiState entryState : bAccent.getEntryStates()) {
                // Take start state and raise its flag
                entryState.setFlag(partition, true);
                // Test state's block flag, raise and add to BL if not raised
                Block parent = entryState.getParent(partition);
                if (!parent.getFlag()) {
                    parent.setFlag(true);
                    BL.add(parent);
                }
            }

            // Scan BL
            for (Block b : BL) {
                boolean isSplitter = false;
                Iterator<MultiState> i = b.getExitStates().iterator();
                while (i.hasNext() && !isSplitter)
                    isSplitter = !i.next().getFlag(partition);

                if (isSplitter) {
                    if (!toBeProcessed.remove(b))
                        stable.remove(b);

                    Block b2 = b.split();
                    toBeProcessed.add(b);
                    toBeProcessed.add(b2);

                    b.initialize();

                    //if additional bottom states are created, clear stable
                    if (b2.initialize()) {
                        toBeProcessed.addAll(stable);
                        stable.clear();
                    }

                    if (Logger.getLogLevel() == LogEvent.DEBUG)
                        Logger.log("Split stutter block into block " + b + " and block " + b2 + " state(s))", LogEvent.DEBUG);
                }
            }
            BL.clear();

            //reset flags
            for (MultiState entryState : bAccent.getEntryStates()) {
                entryState.setFlag(partition, false);
                entryState.getParent(partition).setFlag(false);
            }

            //move to stable
            stable.add(bAccent);
            toBeProcessed.remove(bAccent);
        }
    }

    /**
     * Connects blocks using the connectivity of its substates as well as strongly connected components.
     */
    private void connect() {
        Logger.log("Connecting blocks of partition " + partition.getId(), LogEvent.INFO);

        for (Block current : partition.getStates()) {
            for (Block next : current.getNextParents()) {
                current.addNext(next);
                next.addPrevious(current);
            }

            if (current.containsCycle()) {
                current.addNext(current);
                current.addPrevious(current);
            }
        }

        for (MultiState init : partition.getInitialSubStates()) {
            try {
                partition.addInitial(init.getParent(partition));
            } catch (ConverterException e) {
                Logger.log("Failed to add block as initial state.", LogEvent.ERROR);
            }
        }

        Block safety = partition.createParent(partition.getAtomicPropositions());
        safety.addNext(safety);
        safety.addPrevious(safety);
    }
}
