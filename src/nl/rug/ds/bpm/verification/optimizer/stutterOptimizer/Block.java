package nl.rug.ds.bpm.verification.optimizer.stutterOptimizer;

import nl.rug.ds.bpm.verification.model.kripke.State;

import java.util.*;

/**
 * Created by Heerko Groefsema on 09-Mar-17.
 */
public class Block {
	private boolean flag;
	private List<State> bottom, nonbottom, entry;

	public Block() {
		flag = false;
		bottom = new LinkedList<>();
		nonbottom = new LinkedList<>();
		entry = new LinkedList<>();
	}

	public Block(List<State> bottom, List<State> nonbottom) {
		flag = false;
		this.bottom = bottom;
		this.nonbottom = nonbottom;
		entry = new LinkedList<>();
	}

	public Block split() {
		List<State> bot = new LinkedList<>();
		List<State> nonbot = new LinkedList<>();

		for(State b: bottom)
			if(!b.getFlag())
				bot.add(b);

		//if flag down and next in bot or nonbot, add to nonbot
		//BSF added, so iterate back to front
		ListIterator<State> iterator = nonbottom.listIterator(nonbottom.size());
		while (iterator.hasPrevious()) {
			State nb = iterator.previous();
			if (!nb.getFlag()) {
				boolean isB2 = true;
				Iterator<State> next = nb.getNextStates().iterator();
				while (next.hasNext() && isB2) {
					State n = next.next();
					isB2 = bot.contains(n) || nonbot.contains(n);
				}

				if (isB2)
					nonbot.add(nb);
			}
		}

		//split lists
		bottom.removeAll(bot);
		nonbottom.removeAll(nonbot);

		//keep only B1 entries
		entry.clear();
		for(State s: nonbottom)
			for(State previous: s.getPreviousStates())
				if(previous.getBlock() != this)
					entry.add(previous);
		for(State s: bottom)
			for(State previous: s.getPreviousStates())
				if(previous.getBlock() != this)
					entry.add(previous);

		//nonbot was filled in reverse
		Collections.sort(nonbot, Collections.reverseOrder());
		
		//make B2
		Block block = new Block(bot, nonbot);

		for(State state: bot)
			state.setBlock(block);
		for(State state: nonbot)
			state.setBlock(block);

		return block;
	}

	public void merge(Block b) {
		for(State s: b.getNonbottom())
			s.setBlock(this);

		for(State s: b.getBottom())
			s.setBlock(this);

		nonbottom.addAll(b.getNonbottom());
		bottom.addAll(b.getBottom());
		entry.addAll(b.getEntry());

		flag = flag && b.getFlag();
		b = null;
	}

	public void init() {
		for(State s: nonbottom) {
			boolean isBottom = true;

			Iterator<State> i = s.getNextStates().iterator();
			while (i.hasNext() && isBottom) {
				State state = i.next();
				if(nonbottom.contains(state))
					isBottom = false;
			}

			for(State previous: s.getPreviousStates())
				if(previous.getBlock() != this)
					entry.add(previous);

			if(isBottom)
				bottom.add(s);
		}

		nonbottom.removeAll(bottom);
	}

	public boolean reinit() {
		List<State> newBottom = new ArrayList<>();
		entry.clear();

		for(State s: nonbottom) {
			boolean isBottom = true;

			Iterator<State> i = s.getNextStates().iterator();
			while (i.hasNext() && isBottom) {
				State state = i.next();
				if(nonbottom.contains(state) || bottom.contains(state))
					isBottom = false;
			}

			for(State previous: s.getPreviousStates())
				if(previous.getBlock() != this)
					entry.add(previous);

			if(isBottom)
				newBottom.add(s);
		}

		for(State s: bottom)
			for(State previous: s.getPreviousStates())
				if(previous.getBlock() != this)
					entry.add(previous);

		bottom.addAll(newBottom);
		nonbottom.removeAll(newBottom);

		//return true if new bottom states were found
		return !newBottom.isEmpty();
	}

	public List<State> getBottom() { return bottom;	}

	public List<State> getNonbottom() { return nonbottom; }

	public List<State> getEntry() { return entry; }

	public void addState(State state) {
		nonbottom.add(state);
	}

	public void setFlag(boolean flag) {
		this.flag = flag;
	}

	public boolean getFlag() {
		return flag;
	}

	public int size() { return nonbottom.size() + bottom.size(); }

	public String toString() {
		StringBuilder sb = new StringBuilder("{");

		Iterator<State> bi = bottom.iterator();
		while(bi.hasNext()) {
			sb.append(bi.next().toFriendlyString());
			if (bi.hasNext())
				sb.append(", ");
		}

		Iterator<State> nbi = nonbottom.iterator();
		if(nbi.hasNext())
			sb.append(" | ");
		while(nbi.hasNext()) {
			sb.append(nbi.next().toFriendlyString());
			if (nbi.hasNext())
				sb.append(", ");
		}

		sb.append("}");
		return sb.toString();
	}
}
