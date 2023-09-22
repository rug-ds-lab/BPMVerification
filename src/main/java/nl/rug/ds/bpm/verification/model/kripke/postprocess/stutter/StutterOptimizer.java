package nl.rug.ds.bpm.verification.model.kripke.postprocess.stutter;

import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.verification.model.kripke.KripkeState;
import nl.rug.ds.bpm.verification.model.kripke.KripkeStructure;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that calculates stutter equivalent partitions of Kripke structures and reduces the state space accordingly.
 */
public class StutterOptimizer {
	private int count, eventCount;
	private final KripkeStructure kripke;
	private final Set<KripkeState> stutterStates;
	private final List<Block> toBeProcessed;
	private final List<Block> stable;
	private final List<Block> BL;

	/**
	 * Creates a StutterOptimizer for the given Kripke structure.
	 *
	 * @param kripke The Kripke structure to partition and reduce.
	 */
	public StutterOptimizer(KripkeStructure kripke) {
		this.kripke = kripke;

		toBeProcessed = new LinkedList<>();
		stable = new LinkedList<>();
		BL = new LinkedList<>();

		count = 0;
		eventCount = 160000;
		stutterStates = new HashSet<>();
	}

	/**
	 * Partitions the Kripke structure into stutter equivalent blocks. The Kripke structure must be preprocessed first.
	 */
	public void partition() {
		while (!toBeProcessed.isEmpty()) {
			Logger.log("Checking stutter block (" + (1 + BL.size() + stable.size()) + "/" + (toBeProcessed.size() + BL.size() + stable.size()) + ") for any required splits", LogEvent.VERBOSE);
			Block bAccent = toBeProcessed.get(0);
			// Scan incoming relations
			for (KripkeState entryState : bAccent.getEntry()) {
				// Take start state and raise its flag
				entryState.setFlag(true);
				// Test state's block flag, raise and add to BL if not raised
				if (!entryState.getBlock().getFlag()) {
					BL.add(entryState.getBlock());
					entryState.getBlock().setFlag(true);
				}
			}

			// Scan BL
			for (Block b : BL) {
				boolean isSplitter = false;
				Iterator<KripkeState> i = b.getBottom().iterator();
				while (i.hasNext() && !isSplitter)
					isSplitter = !i.next().getFlag();

				if (isSplitter) {
					Logger.log("Splitting stutter block " + b + " with " + b.size() + " state(s))", LogEvent.DEBUG);

					toBeProcessed.remove(b);
					stable.remove(b);

					Block b2 = b.split();
					toBeProcessed.add(b);
					toBeProcessed.add(b2);

					b.reinit();


					//if additional bottom states are created, clear stable
					if (b2.reinit()) {
						toBeProcessed.addAll(stable);
						stable.clear();
					}

					Logger.log("Split stutter block into block " + b + " and block " + b2 + " with " + b.size() + " and " + b2.size() + " state(s))", LogEvent.DEBUG);
				}
			}
			BL.clear();

			//reset flags
			for (KripkeState entryState : bAccent.getEntry()) {
				entryState.setFlag(false);
				entryState.getBlock().setFlag(false);
			}

			//move to stable
			stable.add(bAccent);
			toBeProcessed.remove(bAccent);
		}
	}

	/**
	 * Reduces the Kripke structure into the calculated partitions. The Kripke structure must be partitioned first.
	 */
	public void reduce() {
		Map<Block, KripkeState> stateMap = new HashMap<>();

		// Assign state to each block
		for (Block b : stable) {
			if (b.size() > 0) {
				KripkeState s = b.getBottom().get(0);
				stateMap.put(b, s);
				stutterStates.addAll(b.getBottom().stream().filter(state -> state != s).collect(Collectors.toSet()));
				stutterStates.addAll(b.getNonbottom());
			}
		}

		// Remove empty blocks
		// If they're not assigned a state, they're empty.
		stable.retainAll(stateMap.keySet());

		// Remap relations to assigned states
		for (Block b : stable) {
			Logger.log("Merging states in stutter block with size " + (b.getBottom().size() + b.getNonbottom().size()), LogEvent.VERBOSE);

			KripkeState s = stateMap.get(b);

			Set<KripkeState> previous = new HashSet<>(b.getBottom().stream().flatMap(prev -> prev.getPreviousStates().stream()).filter(state -> state.getBlock() != b).map(state -> stateMap.get(state.getBlock())).toList());
			Set<KripkeState> next = new HashSet<>(b.getBottom().stream().flatMap(prev -> prev.getNextStates().stream()).filter(state -> state.getBlock() != b).map(state -> stateMap.get(state.getBlock())).toList());

			previous.addAll(b.getNonbottom().stream().flatMap(prev -> prev.getPreviousStates().stream()).filter(state -> state.getBlock() != b).map(state -> stateMap.get(state.getBlock())).toList());
			next.addAll(b.getNonbottom().stream().flatMap(prev -> prev.getNextStates().stream()).filter(state -> state.getBlock() != b).map(state -> stateMap.get(state.getBlock())).toList());

			if (next.isEmpty()) {
				next.add(s);
				previous.add(s);
			}

			s.getNextStates().clear();
			s.getNextStates().addAll(next);

			s.getPreviousStates().clear();
			s.getPreviousStates().addAll(previous);

			// If block contains initial states, remove them and add the assigned state
			Set<KripkeState> initRem = kripke.getInitial().stream().filter(state -> state.getBlock() == b).collect(Collectors.toSet());

			if (!initRem.isEmpty()) {
				kripke.getInitial().removeAll(initRem);
				kripke.getInitial().add(s);
			}
		}

		kripke.getStates().clear();
		kripke.getStates().addAll(stateMap.values());

		for (KripkeState state : kripke.getStates())
			state.resetBlock();
	}

	/**
	 * Assigns states in the Kripke structure to initial, non-stutter equivalent, partitions.
	 */
	public void preprocess() {
		for (KripkeState s : kripke.getInitial()) {
			Block b = new Block();
			b.addState(s);
			s.setBlock(b);
			toBeProcessed.add(b);

			count++;
			treeSearchPreprocess(s);
		}

		for (KripkeState sink : kripke.getSinkStates().stream().filter(state -> state.getBlock().size() > 1).collect(Collectors.toSet())) {
			Block b = new Block();
			sink.getBlock().getNonbottom().remove(sink);
			sink.setBlock(b);
			b.addState(sink);
			toBeProcessed.add(b);
		}

		for (Block b : toBeProcessed) {
			b.init();
			Logger.log("Created stutter block with " + b.size() + " state(s))", LogEvent.DEBUG);
		}
	}

	private void treeSearchPreprocess(KripkeState s) {
		for (KripkeState next : s.getNextStates()) {
			if (next.getBlock() == null) {
				if (s.atomicPropositionsEquals(next)) {
					s.getBlock().addState(next);
					next.setBlock(s.getBlock());
				} else {
					Block b = new Block();
					b.addState(next);
					next.setBlock(b);
					toBeProcessed.add(b);
				}

				count++;
				if (count >= eventCount) {
					Logger.log("Partitioning states into stutter blocks (at " + count + " states)", LogEvent.VERBOSE);
					eventCount += 160000;
				}

				treeSearchPreprocess(next);
			} else {
				if (s.atomicPropositionsEquals(next) && s.getBlock() != next.getBlock()) {
					Block merge = next.getBlock();
					toBeProcessed.remove(merge);
					s.getBlock().merge(merge);
				}
				//else already preprocessed correctly
			}
		}
	}

	/**
	 * Returns a String representation of the state of the optimization.
	 *
	 * @param fullOutput Iff true, includes the status of the partitions.
	 * @return a String representation of the state of the optimization.
	 */
	public String toString(boolean fullOutput) {
		StringBuilder sb = new StringBuilder();
		sb.append("Reduction of ").append(stutterStates.size()).append(" states");
		if (fullOutput) {
			sb.append("\nUnstable Block Partitions:\n");
			for (Block b : toBeProcessed)
				sb.append(b.toString()).append("\n");
			sb.append("Stable Block Partitions:\n");
			for (Block b : stable)
				sb.append(b.toString()).append("\n");
		}

		return sb.toString();
	}

	/**
	 * Returns the removed states.
	 *
	 * @return a set containing the removed states.
	 */
	public Set<KripkeState> getStutterStates() {
		return stutterStates;
	}

	/**
	 * Returns a String representation of the state of the optimization.
	 *
	 * @return a String representation of the state of the optimization.
	 */
	public String toString() {
		return toString(true);
	}
}
