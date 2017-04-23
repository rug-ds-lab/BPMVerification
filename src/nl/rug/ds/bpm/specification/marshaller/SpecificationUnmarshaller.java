package nl.rug.ds.bpm.specification.marshaller;

import nl.rug.ds.bpm.verification.event.EventHandler;
import nl.rug.ds.bpm.verification.map.SpecificationTypeMap;
import nl.rug.ds.bpm.specification.jaxb.*;

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
	
	public void loadSpecificationTypes(SpecificationTypeMap typeMap) {
		for (SpecificationType specificationType: specification.getSpecificationTypes()) {
			typeMap.addSpecificationType(specificationType);
			eventHandler.logVerbose("Adding specification type " + specificationType.getId());
		}

		for (SpecificationSet set: specification.getSpecificationSets())
			for (Specification spec: set.getSpecifications())
				if(typeMap.getSpecificationType(spec.getType()) != null)
					spec.setSpecificationType(typeMap.getSpecificationType(spec.getType()));
				else
					eventHandler.logWarning("No such specification type: " + spec.getType());
	}
	
	public BPMSpecification getSpecification() { return specification; }
	public List<SpecificationSet> getSpecificationSets() { return specification.getSpecificationSets();	}
}
