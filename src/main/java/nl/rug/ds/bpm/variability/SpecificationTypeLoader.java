package nl.rug.ds.bpm.variability;

import nl.rug.ds.bpm.specification.jaxb.BPMSpecification;
import nl.rug.ds.bpm.specification.jaxb.Specification;
import nl.rug.ds.bpm.specification.jaxb.SpecificationSet;
import nl.rug.ds.bpm.specification.jaxb.SpecificationType;
import nl.rug.ds.bpm.specification.marshaller.SpecificationUnmarshaller;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;

import java.util.HashMap;

public class SpecificationTypeLoader {
    private HashMap<String, SpecificationType> specificationTypeMap;

    public SpecificationTypeLoader() {
        specificationTypeMap = new HashMap<>();

        loadSpecificationTypes();
    }

    public HashMap<String, SpecificationType> getSpecificationTypeMap() {
        return specificationTypeMap;
    }

	public SpecificationType getSpecificationType(String id) {
        return specificationTypeMap.get(id);
	}
	
	private void loadSpecificationTypes() {
		try {
//			InputStream targetStream = new FileInputStream("./resources/specificationTypes.xml");
			
			SpecificationUnmarshaller unmarshaller = new SpecificationUnmarshaller(this.getClass().getResourceAsStream("/resources/specificationTypes.xml"));
//			SpecificationUnmarshaller unmarshaller = new SpecificationUnmarshaller(targetStream);
			BPMSpecification specification = unmarshaller.getSpecification();
		
			for (SpecificationType specificationType: specification.getSpecificationTypes()) {
                specificationTypeMap.put(specificationType.getId(), specificationType);
                Logger.log("Adding specification type " + specificationType.getId(), LogEvent.VERBOSE);
			}
	
			for (SpecificationSet set: specification.getSpecificationSets()) {
				for (Specification spec: set.getSpecifications()) {
                    if (specificationTypeMap.get(spec.getType()) != null) {
                        spec.setSpecificationType(specificationTypeMap.get(spec.getType()));
                    } else {
                        Logger.log("No such specification type: " + spec.getType(), LogEvent.WARNING);
                    }
                }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
