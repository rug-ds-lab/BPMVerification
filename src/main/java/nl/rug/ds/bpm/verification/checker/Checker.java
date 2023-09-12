package nl.rug.ds.bpm.verification.checker;

import nl.rug.ds.bpm.expression.CompositeExpression;
import nl.rug.ds.bpm.specification.jaxb.Formula;
import nl.rug.ds.bpm.specification.jaxb.Specification;
import nl.rug.ds.bpm.util.exception.CheckerException;
import nl.rug.ds.bpm.verification.event.VerificationEvent;
import nl.rug.ds.bpm.verification.map.AtomicPropositionMap;
import nl.rug.ds.bpm.verification.model.State;
import nl.rug.ds.bpm.verification.model.Structure;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that implements calling a model checker and parsing its results.
 */
public abstract class Checker {
    protected static File out;
    protected static int idc = 0;

    protected int id;
    protected StringBuilder inputChecker, outputChecker;
    protected File executable;
    protected List<CheckerFormula> formulas;

    /**
     * Creates a Checker.
     *
     * @param executable file that contains the path to the model checker's executable.
     */
    public Checker(File executable) {
        id = idc++;
        this.executable = executable;

        formulas = new ArrayList<>();
        inputChecker = new StringBuilder();
        outputChecker = new StringBuilder();
    }

    /**
     * Sets the path where temporary generated input files are to be stored.
     *
     * @param path the path where temporary generated input files are to be stored.
     */
    public static void setOutputPath(String path) {
        try {
            out = new File(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the input for the Checker.
     *
     * @return the input for the Checker.
     */
    public String getInputChecker() {
        return inputChecker.toString();
    }

    /**
     * Returns the output of the Checker.
     *
     * @return the output of the Checker.
     */
    public String getOutputChecker() {
        return outputChecker.toString();
    }

    /**
     * Returns the list of formulas that the Checker verifies.
     *
     * @return the list of formulas that the Checker verifies.
     */
    public List<CheckerFormula> getFormulas() {
        return formulas;
    }

    /**
     * Adds a new formula that the Checker should verify.
     *
     * @param formula              the input formula in its XML Formula class format.
     * @param specification        the specification in its XML Specification class format.
     * @param atomicPropositionMap the AtomicPropositionMap that is to be used to map between input propositions and output propositions used during model checking.
     */
    public abstract void addFormula(Formula formula, Specification specification, AtomicPropositionMap<CompositeExpression> atomicPropositionMap);

    /**
     * Creates a model from the given Structure using the Checker's input format.
     *
     * @param structure the Structure from which the model is to be generated.
     * @throws CheckerException when the model fails to be generated.
     */
    public abstract void createModel(Structure<? extends State<?>> structure) throws CheckerException;

    /**
     * Checks the model. Must be called after createModel(Structure<? extends State<?>> structure).
     *
     * @return a list of events that describe the results of verification.
     * @throws CheckerException when the model could not be verified.
     */
    public abstract List<VerificationEvent> checkModel() throws CheckerException;

    /**
     * Creates a model from the given Structure using the Checker's input format and checks it.
     *
     * @param Structure the Structure from which the model is to be generated and checked.
     * @throws CheckerException when the model could not be generated or verified.
     */
    public void checkModel(Structure<? extends State<?>> Structure) throws CheckerException {
        createModel(Structure);
        checkModel();
    }

    /**
     * Destroy this instance of the Checker.
     */
    public void destroy() {
        return;
    }
}
