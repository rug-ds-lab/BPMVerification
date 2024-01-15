package nl.rug.ds.bpm.verification.event;

import nl.rug.ds.bpm.petrinet.interfaces.net.VerifiableNet;
import nl.rug.ds.bpm.specification.jaxb.SpecificationSet;

import java.util.LinkedHashMap;
import java.util.Map;

public class PerformanceEvent {

    private final Map<String, Number> metrics;
    // What was verified
    private VerifiableNet net;
    private SpecificationSet specificationSet;

    /**
     * Creates a new PerformanceEvent.
     *
     * @param net The model used for verification.
     */
    public PerformanceEvent(VerifiableNet net) {
        this.net = net;

        metrics = new LinkedHashMap<>();
    }

    /**
     * Creates a new PerformanceEvent.
     *
     * @param net              The model used for verification.
     * @param specificationSet The set of specifications that were verified on the model.
     */
    public PerformanceEvent(VerifiableNet net, SpecificationSet specificationSet) {
        this(net);
        this.specificationSet = specificationSet;
    }

    /**
     * Returns the model used for verification.
     *
     * @return the model used for verification.
     */
    public VerifiableNet getNet() {
        return net;
    }

    /**
     * Sets the model used for verification.
     *
     * @param net the model used for verification.
     */
    public void setNet(VerifiableNet net) {
        this.net = net;
    }

    /**
     * Returns the set of specifications that were verified on the model.
     *
     * @return the set of specifications that were verified on the model.
     */
    public SpecificationSet getSpecificationSet() {
        return specificationSet;
    }

    /**
     * Sets the set of specifications that were verified on the model.
     *
     * @param specificationSet the set of specifications that were verified on the model.
     */
    public void setSpecificationSet(SpecificationSet specificationSet) {
        this.specificationSet = specificationSet;
    }

    /**
     * Returns the mapped metrics.
     *
     * @return the mapped metrics.
     */
    public Map<String, Number> getMetrics() {
        return metrics;
    }

    /**
     * Adds a metric as a name-value pair.
     *
     * @param metric the metric's name.
     * @param value  the metric's value.
     */
    public void addMetric(String metric, Number value) {
        metrics.put(metric, value);
    }
}
