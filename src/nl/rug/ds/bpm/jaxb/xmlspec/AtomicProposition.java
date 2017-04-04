package nl.rug.ds.bpm.jaxb.xmlspec;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by p256867 on 7-2-2017.
 */

@XmlRootElement
public class AtomicProposition {
    private String id, name;

    public AtomicProposition() {}

    @XmlAttribute
    public void setId(String id) { this.id = id; }
    public String getId() { return id; }

    @XmlAttribute
    public void setName(String name) { this.name = name; }
    public String getName() { return name; }
}
