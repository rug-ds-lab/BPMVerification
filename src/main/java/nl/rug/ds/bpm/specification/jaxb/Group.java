package nl.rug.ds.bpm.specification.jaxb;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Heerko Groefsema on 07-Apr-17.
 */

@XmlRootElement
public class Group {
	private String id;
	private List<Element> elements;
	
	public Group() {
		elements = new ArrayList<>();
	}
	
	public Group(String id) {
		this();
		setId(id);
	}
	
	@XmlAttribute(required = true)
	public void setId(String id) { this.id = id; }
	public String getId() { return id; }
	
	@XmlElementWrapper(name = "elements")
	@XmlElement(name = "element")
	public List<Element> getElements() { return elements; }
	public void addElement(Element element) { elements.add(element); }
}
