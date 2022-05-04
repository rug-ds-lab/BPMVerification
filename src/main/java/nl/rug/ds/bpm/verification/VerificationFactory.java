package nl.rug.ds.bpm.verification;

import nl.rug.ds.bpm.petrinet.interfaces.net.VerifiableNet;
import nl.rug.ds.bpm.specification.jaxb.BPMSpecification;
import nl.rug.ds.bpm.specification.marshaller.SpecificationUnmarshaller;
import nl.rug.ds.bpm.util.exception.ConfigurationException;
import nl.rug.ds.bpm.util.exception.SpecificationException;
import nl.rug.ds.bpm.verification.checker.CheckerFactory;
import nl.rug.ds.bpm.verification.verifier.Verifier;
import nl.rug.ds.bpm.verification.verifier.kripke.KripkeVerifier;
import nl.rug.ds.bpm.verification.verifier.multi.MultiVerifier;
import nl.rug.ds.bpm.verification.verifier.stutter.StutterVerifier;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Static factory class to create different Verifiers and load specification files.
 */
public class VerificationFactory {

    /**
     * Creates a standard Verifier.
     * Calls createStutterVerifier.
     *
     * @param net            the VerifiableNet upon which the given specifications should be verified.
     * @param specification  the specification that should be verified on the given VerifiableNet.
     * @param checkerFactory the Factory that supplies the Verifier with Checker instances.
     * @return a Verifier.
     * @throws ConfigurationException when the Verifier failed to load its configuration.
     */
    public static Verifier createVerifier(VerifiableNet net, BPMSpecification specification, CheckerFactory checkerFactory) throws ConfigurationException {
        return createStutterVerifier(net, specification, checkerFactory);
    }

    /**
     * Creates a KripkeVerifier.
     * The most transparent Verifier because it does not optimize the Structure obtained from the given VerifiableNet.
     * Not suitable for VerifiableNets with many parallel executing paths.
     *
     * @param net            the VerifiableNet upon which the given specifications should be verified.
     * @param specification  the specification that should be verified on the given VerifiableNet.
     * @param checkerFactory the Factory that supplies the Verifier with Checker instances.
     * @return a Verifier.
     * @throws ConfigurationException when the Verifier failed to load its configuration.
     */
    public static Verifier createKripkeVerifier(VerifiableNet net, BPMSpecification specification, CheckerFactory checkerFactory) throws ConfigurationException {
        return new KripkeVerifier(net, specification, checkerFactory);
    }

    /**
     * Creates a StutterVerifier.
     * A Verifier that optimizes the Structure obtained from the given VerifiableNet by reducing it into a stutter
     * equivalent model. Not suitable for VerifiableNets that include cyclic behavior.
     *
     * @param net            the VerifiableNet upon which the given specifications should be verified.
     * @param specification  the specification that should be verified on the given VerifiableNet.
     * @param checkerFactory the Factory that supplies the Verifier with Checker instances.
     * @return a Verifier.
     * @throws ConfigurationException when the Verifier failed to load its configuration.
     */
    public static Verifier createStutterVerifier(VerifiableNet net, BPMSpecification specification, CheckerFactory checkerFactory) throws ConfigurationException {
        return new StutterVerifier(net, specification, checkerFactory);
    }

    /**
     * Creates a MultiVerifier.
     * A Verifier that calculates stutter equivalent conditional substructures while calculating a full Kripke structure.
     * Not suitable for VerifiableNets that include large parallel behavior.
     *
     * @param net            the VerifiableNet upon which the given specifications should be verified.
     * @param specification  the specification that should be verified on the given VerifiableNet.
     * @param checkerFactory the Factory that supplies the Verifier with Checker instances.
     * @return a Verifier.
     * @throws ConfigurationException when the Verifier failed to load its configuration.
     */
    public static Verifier createMultiVerifier(VerifiableNet net, BPMSpecification specification, CheckerFactory checkerFactory) throws ConfigurationException {
        return new MultiVerifier(net, specification, checkerFactory);
    }

    /**
     * Loads a BPMSpecification from the given String.
     *
     * @param specification the XML specification as a String.
     * @return a BPMSpecification.
     * @throws SpecificationException when the BPMSpecification failed to load.
     */
    public static BPMSpecification loadSpecification(String specification) throws SpecificationException {
        return loadSpecification(new ByteArrayInputStream(specification.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Loads a BPMSpecification from the given InputStream.
     *
     * @param inputStream the XML specification as a InputStream.
     * @return a BPMSpecification.
     * @throws SpecificationException when the BPMSpecification failed to load.
     */
    public static BPMSpecification loadSpecification(InputStream inputStream) throws SpecificationException {
        try {
            SpecificationUnmarshaller unmarshaller = new SpecificationUnmarshaller(inputStream);
            return unmarshaller.getSpecification();
        } catch (Exception e) {
            throw new SpecificationException("Invalid specification");
        }
    }


    /**
     * Loads a BPMSpecification from the given File.
     *
     * @param specification the XML specification as a File.
     * @return a BPMSpecification.
     * @throws SpecificationException when the BPMSpecification failed to load.
     */
    public static BPMSpecification loadSpecification(File specification) throws SpecificationException {
        if (!(specification.exists() && specification.isFile()))
            throw new SpecificationException("No such file " + specification);

        try {
            SpecificationUnmarshaller unmarshaller = new SpecificationUnmarshaller(specification);
            return unmarshaller.getSpecification();
        } catch (Exception e) {
            throw new SpecificationException("Invalid specification");
        }
    }
}
