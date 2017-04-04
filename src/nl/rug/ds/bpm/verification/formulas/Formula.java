package nl.rug.ds.bpm.verification.constraints;

import nl.rug.ds.bpm.editor.models.Arrow;
import nl.rug.ds.bpm.editor.models.Constraint;
import nl.rug.ds.bpm.editor.models.graphModels.SuperCell;

/**
 * Created by Mark Kloosterhuis.
 */
public abstract class Formula {
    protected String f;
    protected String typeName;
    protected SuperCell sourceCell;
    protected SuperCell targetCell;
    protected boolean isEdge;
    protected Constraint constraint;
    protected SuperCell cell;

    @Override
    public String toString() {
        return f;
    }

    public String getTypeName() {
        return this.typeName;
    }
    
    public String getName() {
        String name = "";
        if(constraint.getArrow() != null)
            name = constraint.getArrow().getName();
        else
            name = constraint.getId();
        return name;
    }

    public boolean isEdge() {
        return isEdge;
    }

    public Arrow getArrow() {
        return constraint.getArrow();
    }

    public SuperCell getSourceCell() {
        return sourceCell;
    }

    public SuperCell getTargetCell() {
        return targetCell;
    }
    public SuperCell getCell() {
        return cell;
    }

    //unparsed formula
    public String getFormula() {
        return f;
    }
}
