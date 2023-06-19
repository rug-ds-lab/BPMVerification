package nl.rug.ds.bpm.util.comparator;

import nl.rug.ds.bpm.verification.model.State;

import java.util.Comparator;

public class ReverseStateIdComparator implements Comparator<State<? extends State<?>>> {
        @Override
        public int compare(State<? extends State<?>> a, State<? extends State<?>> b) {
                return -1 * Long.compare(a.getIdNumber(), b.getIdNumber());
        }
}
