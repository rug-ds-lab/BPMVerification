package nl.rug.ds.bpm.verification.modelConverters;

import nl.rug.ds.bpm.pnml.EventHandler;
import nl.rug.ds.bpm.pnml.util.IDMap;

import java.io.File;

/**
 * Created by Heerko Groefsema on 07-Apr-17.
 */
public class PnmlParalelStepper {
	private IDMap specIdMap, pnmlIdMap;
	private EventHandler eventHandler;
	
	public PnmlParalelStepper(EventHandler eventHandler, IDMap specIdMap, File pnml) {
		this.eventHandler = eventHandler;
		this.specIdMap = specIdMap;

		pnmlIdMap = new IDMap("t", specIdMap.getIdToAp(), specIdMap.getApToId());
	}
}
