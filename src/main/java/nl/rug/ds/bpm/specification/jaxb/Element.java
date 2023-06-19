package nl.rug.ds.bpm.specification.jaxb;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;

/**
 * Created by Heerko Groefsema on 07-Apr-17.
 */

@XmlRootElement
public class Element {
	private String id;
	
	public Element() {}
	
	public Element(String id) {
		setId(id);
	}
	
	@XmlValue
	public void setId(String id) { this.id = id; }
	public String getId() { return id; }
}
