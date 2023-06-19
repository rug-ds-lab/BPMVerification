package nl.rug.ds.bpm.specification.marshaller;

import nl.rug.ds.bpm.specification.jaxb.BPMSpecification;
import nl.rug.ds.bpm.util.exception.SpecificationException;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.File;
import java.io.OutputStream;

/**
 * Created by Heerko Groefsema on 24-Apr-17.
 */
public class SpecificationMarshaller {
	
	public SpecificationMarshaller(BPMSpecification bpmSpecification, File file) throws SpecificationException {
		try {
			JAXBContext context = JAXBContext.newInstance(BPMSpecification.class);
			Marshaller marshaller = context.createMarshaller();
			
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(bpmSpecification, file);
			
		} catch (JAXBException e) {
			throw new SpecificationException("Failed to write" + file.toString());
		}
	}
	
	public SpecificationMarshaller(BPMSpecification bpmSpecification, OutputStream stream) throws SpecificationException {
		try {
			JAXBContext context = JAXBContext.newInstance(BPMSpecification.class);
			Marshaller marshaller = context.createMarshaller();
			
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(bpmSpecification, stream);
			
		} catch (JAXBException e) {
			throw new SpecificationException("Failed to write to output stream");
		}
	}
}
