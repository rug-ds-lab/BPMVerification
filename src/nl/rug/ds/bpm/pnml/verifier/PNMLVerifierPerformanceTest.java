package nl.rug.ds.bpm.pnml.verifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ee.ut.pnml.PNMLReader;
import hub.top.petrinet.PetriNet;
import nl.rug.ds.bpm.verification.PerformanceTestVerifier;

/**
 * Created by Nick van Beest on 14 March 2018.
 */
public class PNMLVerifierPerformanceTest {
	private boolean reduce;
	
	public void evaluateAll(String folder, int runcount) throws Exception {
		List<String> models = getPNMLFilesInFolder(folder, false);
		String pnml, guardsfile, specxml;
				
		String result = "";

		double starttime, avgtime;
		Set<Double> times;
		
		for (String m: models) {
			System.out.println(m);
			pnml = folder + m;
			guardsfile = pnml.replace(".pnml", "_guards.txt");
			specxml = pnml.replace(".pnml", ".xml");
			
			// first get the result
			result += getCompositionString(pnml) + " " + verify(pnml, guardsfile, specxml) + " ";
			
			// then execute the performance analysis
			times = new HashSet<Double>();
			for (int i = 0; i < 5; i++) {
				starttime = System.currentTimeMillis();
				
				verify(pnml, guardsfile, specxml);
				
				times.add(System.currentTimeMillis() - starttime);
			}
			avgtime = getAverage(times);
			
			result += avgtime + "\n";
		}
		
		System.out.println(result);
	}
	
	public String verify(String pnml, String guardsfile, String xmlfile) throws Exception {
		ExtPnmlStepper stepper;
		
		PetriNet pn = PNMLReader.parse(new File(pnml));
		
		stepper = new ExtPnmlStepper(pn);
		stepper.setTransitionGuards(getGuardsFromFile(new File(guardsfile)));
			
		PerformanceTestVerifier verifier = new PerformanceTestVerifier(stepper);

		return verifier.verify(xmlfile, reduce);
	}

	private Set<String> getGuardsFromFile(File file) {
		Set<String> guardset = new HashSet<String>();
		
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		       guardset.add(line);
		    }
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		return guardset;
	}
	
	private static String getCompositionString(String pnml) {
		String composition = pnml.substring(pnml.lastIndexOf("/") + 1).replace(".pnml", "");
		
		composition = composition.toUpperCase().replace("-", " ");
		composition = composition.substring(0, 3) + " " + composition.substring(3);
		
		return composition;
	}
	
	private static double getAverage(Set<Double> numbers) {
		double average = 0.0;
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		
		for (Double d: numbers) {
			average += d;
			if (d > max) max = d;
			if (d < min) min = d;
		}
		
		if (numbers.size() > 2) {
			average -= max;
			average -= min;
			
			average /= (numbers.size() - 2);
		}
		else if (numbers.size() > 0) {
			average /= numbers.size();
		}
		
		return average;
	}
	
	private static List<String> getPNMLFilesInFolder(String folder, Boolean inclSubFolders) {
	    File dir = new File(folder);
	    List<String> files = new ArrayList<String>();
	    
		for (File fileEntry: dir.listFiles()) {
	        if (fileEntry.isDirectory()) {
	        	if (inclSubFolders) {
	        		files.addAll(getPNMLFilesInFolder(fileEntry.getAbsolutePath(), inclSubFolders));
	        	}
	        } 
	        else {
	        	if (fileEntry.getName().endsWith(".pnml")) {
	        		files.add(fileEntry.getName());
	        	}
	        }
	    }
		
		return files;
	}
}
