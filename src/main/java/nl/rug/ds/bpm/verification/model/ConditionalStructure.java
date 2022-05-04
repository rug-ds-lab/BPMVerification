package nl.rug.ds.bpm.verification.model;

import nl.rug.ds.bpm.specification.jaxb.Condition;

import java.util.List;
import java.util.Set;

public interface ConditionalStructure {

    /**
     * Adds the given Conditions to this structure.
     *
     * @param condition the Condition.
     */
    void addCondition(Condition condition);

    /**
     * Adds the given conditions to this structure.
     *
     * @param condition the condition.
     */
    void addCondition(String condition);

    /**
     * Adds the given list of Conditions to this structure.
     *
     * @param conditions the conditions.
     */
    void addConditions(List<Condition> conditions);

    /**
     * Adds the given set of conditions to this structure.
     *
     * @param conditions the conditions.
     */
    void addConditions(Set<String> conditions);

    /**
     * Returns the conditions of this structure.
     *
     * @return the set of conditions.
     */
    Set<String> getConditions();
}
