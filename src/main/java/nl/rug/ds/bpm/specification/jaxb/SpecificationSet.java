package nl.rug.ds.bpm.specification.jaxb;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by p256867 on 6-4-2017.
 */

@XmlRootElement
public class SpecificationSet {
    private List<Condition> conditions;
    private List<Specification> specifications;

    public SpecificationSet() {
        conditions = new ArrayList<>();
        specifications = new ArrayList<>();
    }

    @XmlElementWrapper(name = "conditions")
    @XmlElement(name = "condition")
    public List<Condition> getConditions() { return conditions; }
    
    public void addCondition(Condition condition) {
        conditions.add(condition);
    }

    @XmlElementWrapper(name = "specifications")
    @XmlElement(name = "specification")
    public List<Specification> getSpecifications() { return specifications; }
    public void addSpecification(Specification specification) { specifications.add(specification); }
}
