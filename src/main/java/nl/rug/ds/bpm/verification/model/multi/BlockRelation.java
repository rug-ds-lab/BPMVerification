package nl.rug.ds.bpm.verification.model.multi;

public class BlockRelation implements Comparable<BlockRelation> {
    private MultiState start, end;

    public BlockRelation(MultiState start, MultiState end) {
        this.start = start;
        this.end = end;
    }

    public MultiState getStart() {
        return start;
    }

    public MultiState getEnd() {
        return end;
    }

    @Override
    public int compareTo(BlockRelation o) {
        int c = start.compareTo(o.getStart());
        return (c == 0 ? end.compareTo(o.getEnd()) : c);
    }
}
