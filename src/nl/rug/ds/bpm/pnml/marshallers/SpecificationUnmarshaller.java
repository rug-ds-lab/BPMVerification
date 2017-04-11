package nl.rug.ds.bpm.pnml.marshallers;

import nl.rug.ds.bpm.jaxb.specification.*;
import nl.rug.ds.bpm.pnml.EventHandler;
import nl.rug.ds.bpm.pnml.util.GroupMap;
import nl.rug.ds.bpm.pnml.util.IDMap;
import nl.rug.ds.bpm.pnml.util.SpecificationTypeMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * Created by Heerko Groefsema on 07-Apr-17.
 */
public class SpecificationUnmarshaller {
	private EventHandler eventHandler;
	private BPMSpecification specification;
	
	public SpecificationUnmarshaller(EventHandler eventHandler, File file) {
		this.eventHandler = eventHandler;
		try {
			JAXBContext context = JAXBContext.newInstance(BPMSpecification.class);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			
			specification = (BPMSpecification) unmarshaller.unmarshal(file);
		} catch (Exception e) {
			eventHandler.logCritical("Failed to load " + file.toString());
		}
	}
	
	public SpecificationUnmarshaller(EventHandler eventHandler, InputStream is) {
		this.eventHandler = eventHandler;
		try {
			JAXBContext context = JAXBContext.newInstance(BPMSpecification.class);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			
			specification = (BPMSpecification) unmarshaller.unmarshal(is);
		} catch (Exception e) {
			eventHandler.logCritical("Failed to read input stream");
		}
	}
	
	public IDMap getIdMap() {
		IDMap idMap = new IDMap();
		
		for (SpecificationSet specificationSet: specification.getSpecificationSets())
			for (Specification s: specificationSet.getSpecifications())
				for (InputElement inputElement: s.getInputElements()) {
					idMap.addID(inputElement.getElement());
					eventHandler.logVerbose("Mapping " + inputElement.getElement() + " to " + idMap.getAP(inputElement.getElement()));
					inputElement.setElement(idMap.getAP(inputElement.getElement()));
				}
		
		for (Group group: specification.getGroups()) {
			group.setId(idMap.getAP(group.getId()));
			for (Element element : group.getElements()) {
				idMap.addID(element.getId());
				eventHandler.logVerbose("Mapping " + element.getId() + " to " + idMap.getAP(element.getId()));
				element.setId(idMap.getAP(element.getId()));
			}
		}
		
		return idMap;
	}
	
	public GroupMap getGroupMap() {
		GroupMap groupMap = new GroupMap();
		
		for (Group group: specification.getGroups()) {
			groupMap.addGroup(group.getId());
			eventHandler.logVerbose("New group " + group.getId());
			for (Element element: group.getElements()) {
				groupMap.addToGroup(group.getId(), element.getId());
				eventHandler.logVerbose("\t " + element.getId());
			}
		}
		return groupMap;
	}
	
	public void loadSpecificationTypes(SpecificationTypeMap typeMap) {
		for (SpecificationType specificationType: specification.getSpecificationTypes()) {
			typeMap.addSpecificationType(specificationType);
			eventHandler.logVerbose("Adding specification type " + specificationType.getId());
		}

		for (SpecificationSet set: specification.getSpecificationSets())
			for (Specification spec: set.getSpecifications())
				spec.setSpecificationType(typeMap.getSpecificationType(spec.getType()));
	}
	
	public List<SpecificationSet> getSpecificationSets() { return specification.getSpecificationSets();	}
}
