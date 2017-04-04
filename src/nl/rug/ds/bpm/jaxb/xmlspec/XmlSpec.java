package nl.rug.ds.bpm.jaxb.xmlspec;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by p256867 on 7-2-2017.
 */

@XmlRootElement(name = "variabilitySpecification")
public class XmlSpec {
    private List<AtomicProposition> atomicPropositions = new ArrayList<>();
    private List<Specification> specifications = new ArrayList<>();


    @XmlElementWrapper(name = "atomicPropositions")
    @XmlElement(name = "atomicProposition")
    public List<AtomicProposition> getAtomicPropositions() {
        return atomicPropositions;
    }


    @XmlElementWrapper(name = "specifications")
    @XmlElement(name = "specification")
    public List<Specification> getSpecifications() {
        return specifications;
    }
}
