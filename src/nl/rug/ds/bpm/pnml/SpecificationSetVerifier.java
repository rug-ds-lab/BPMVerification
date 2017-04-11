package nl.rug.ds.bpm.pnml;

import nl.rug.ds.bpm.jaxb.specification.Condition;
import nl.rug.ds.bpm.jaxb.specification.Specification;
import nl.rug.ds.bpm.jaxb.specification.SpecificationSet;
import nl.rug.ds.bpm.verification.modelConverters.Pnml2KripkeConverter;
import nl.rug.ds.bpm.verification.modelOptimizers.propositionOptimizer.PropositionOptimizer;
import nl.rug.ds.bpm.verification.models.kripke.*;

import java.io.File;
import java.util.List;

/**
 * Created by Heerko Groefsema on 07-Apr-17.
 */
public class SpecificationSetVerifier {
	private Kripke kripke;
	private EventHandler eventHandler;
	private File checker;
	private List<Specification> specifications;
	private List<Condition> conditions;
	
	public SpecificationSetVerifier(EventHandler eventHandler, SpecificationSet specificationSet) {
		this.eventHandler = eventHandler;
		this.checker = checker;
		specifications = specificationSet.getSpecifications();
		conditions = specificationSet.getConditions();
	}


	public void buildKripke(File pnml) {
		Pnml2KripkeConverter converter = new Pnml2KripkeConverter();
		eventHandler.logInfo("Creating Kripke structure");
		kripke = converter.convert();
		eventHandler.logInfo(kripke.toString(false));
		eventHandler.logVerbose(kripke.toString(true));

		PropositionOptimizer propositionOptimizer = new PropositionOptimizer(kripke);
	}

	public void verify(File nusmv2) {
	}
}
