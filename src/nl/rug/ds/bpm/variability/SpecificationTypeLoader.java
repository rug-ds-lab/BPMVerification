package nl.rug.ds.bpm.variability;

import nl.rug.ds.bpm.event.EventHandler;
import nl.rug.ds.bpm.specification.jaxb.BPMSpecification;
import nl.rug.ds.bpm.specification.jaxb.Specification;
import nl.rug.ds.bpm.specification.jaxb.SpecificationSet;
import nl.rug.ds.bpm.specification.jaxb.SpecificationType;
import nl.rug.ds.bpm.specification.map.SpecificationTypeMap;
import nl.rug.ds.bpm.specification.marshaller.SpecificationUnmarshaller;

public class SpecificationTypeLoader {
	private SpecificationTypeMap specificationTypeMap;
	private EventHandler eventHandler;
	
	public SpecificationTypeLoader() {
		eventHandler = null;
	}
	
	public SpecificationTypeLoader(EventHandler eventHandler) {
		specificationTypeMap = new SpecificationTypeMap();
		eventHandler = this.eventHandler;
		
		loadSpecificationTypes();
	}
	
	public SpecificationTypeMap getSpecificationTypeMap() {
		return specificationTypeMap;
	}

	public SpecificationType getSpecificationType(String id) {
		return specificationTypeMap.getSpecificationType(id);
	}
	
	private void loadSpecificationTypes() {
//		try {
//			InputStream targetStream = new FileInputStream("./resources/specificationTypes.xml");

			SpecificationUnmarshaller unmarshaller = new SpecificationUnmarshaller(eventHandler, this.getClass().getResourceAsStream("/resources/specificationTypes.xml"));
//			SpecificationUnmarshaller unmarshaller = new SpecificationUnmarshaller(eventHandler, targetStream);
			BPMSpecification specification = unmarshaller.getSpecification();
		
			for (SpecificationType specificationType: specification.getSpecificationTypes()) {
				specificationTypeMap.addSpecificationType(specificationType);
				if (eventHandler != null) {
					eventHandler.logVerbose("Adding specification type " + specificationType.getId());
				}
			}
	
			for (SpecificationSet set: specification.getSpecificationSets()) {
				for (Specification spec: set.getSpecifications()) {
					if(specificationTypeMap.getSpecificationType(spec.getType()) != null) {
						spec.setSpecificationType(specificationTypeMap.getSpecificationType(spec.getType()));
					}
					else {
						if (eventHandler != null) {
							eventHandler.logWarning("No such specification type: " + spec.getType());
						}
					}
				}
			}
//		}
//		catch (Exception e) {
//			e.printStackTrace();
//		}
	}
}
