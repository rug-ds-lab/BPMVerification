package nl.rug.ds.bpm.verification.constraints;

import nl.rug.ds.bpm.editor.models.Constraint;
import nl.rug.ds.bpm.editor.models.InputCellConstraint;
import nl.rug.ds.bpm.editor.models.graphModels.ConstrainEdgeCell;
import nl.rug.ds.bpm.editor.models.graphModels.InputCell;
import nl.rug.ds.bpm.editor.models.graphModels.SuperCell;

/**
 * Created by Mark Kloosterhuis.
 */
public class LTLFormula extends Formula {
    public LTLFormula(SuperCell cell, Constraint constraint, String formula, boolean isEdge) {
        this.cell = cell;
        this.f = formula;
        this.isEdge = isEdge;
        this.typeName = "LTL";
        this.constraint = constraint;
    }

    public LTLFormula(Constraint constraint, String formula, ConstrainEdgeCell edge) {
        this(edge, constraint, formula, true);
        this.sourceCell = (InputCell) edge.getSource();
        this.targetCell = (InputCell) edge.getTarget();
    }

    public LTLFormula(Constraint constraint, String formula, InputCellConstraint cellConstraint) {
        this(cellConstraint.getInputCell(), constraint, formula, false);
        this.sourceCell = this.cell;
    }
}
