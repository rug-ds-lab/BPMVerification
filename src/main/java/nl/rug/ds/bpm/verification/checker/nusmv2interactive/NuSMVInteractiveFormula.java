package nl.rug.ds.bpm.verification.checker.nusmv2interactive;

import nl.rug.ds.bpm.expression.CompositeExpression;
import nl.rug.ds.bpm.specification.jaxb.Formula;
import nl.rug.ds.bpm.specification.jaxb.Specification;
import nl.rug.ds.bpm.util.exception.FormulaException;
import nl.rug.ds.bpm.verification.checker.CheckerFormula;
import nl.rug.ds.bpm.verification.map.AtomicPropositionMap;

/**
 * Created by p256867 on 13-4-2017.
 */
public class NuSMVInteractiveFormula extends CheckerFormula {

    public NuSMVInteractiveFormula(Formula formula, Specification specification, AtomicPropositionMap<CompositeExpression> atomicPropositionMap) throws FormulaException {
        super(formula, specification, atomicPropositionMap);

        this.checkerFormula = "\"" + outputFormula + "\"";

    }
}
