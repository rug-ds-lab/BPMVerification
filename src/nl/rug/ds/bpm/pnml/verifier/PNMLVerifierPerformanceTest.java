package nl.rug.ds.bpm.pnml.verifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
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
		
		String fullspec, partialspec;
		String result = "";

		double starttime, avgtime;
		Set<Double> times;
		
		for (String m: models) {
			pnml = folder + m;
			guardsfile = pnml.replace(".pnml", "_guards.txt");
			specxml = pnml.replace(".pnml", ".xml");
			
			// first get the result
			fullspec = verify(pnml, "", specxml);
			partialspec = verify(pnml, guardsfile, specxml);
			
			result += getCompositionString(pnml) + " " + fullspec + " " + getConditionFromSpec(specxml) + " " + partialspec + " ";
			
			// then execute the performance analyses:
			// no guards
			times = new HashSet<Double>();
			for (int i = 0; i < runcount; i++) {
				starttime = System.currentTimeMillis();
				
				verify(pnml, "", specxml);
				times.add(System.currentTimeMillis() - starttime);
			}
			avgtime = getAverage(times);
			result += Math.round(avgtime) + " ";
			
			//guards
			times = new HashSet<Double>();
			for (int i = 0; i < runcount; i++) {
				starttime = System.currentTimeMillis();
				
				verify(pnml, guardsfile, specxml);
				times.add(System.currentTimeMillis() - starttime);
			}
			avgtime = getAverage(times);
			result += Math.round(avgtime) + "\n";
		}
		
		System.out.println(result);
	}
	
	public String verify(String pnml, String guardsfile, String xmlfile) throws Exception {
		ExtPnmlStepper stepper;
		
		PetriNet pn = PNMLReader.parse(new File(pnml));
		
		stepper = new ExtPnmlStepper(pn);
		
		if (guardsfile.length() > 0)
			stepper.setTransitionGuards(getGuardsFromFile(new File(guardsfile)));
			
		PerformanceTestVerifier verifier = new PerformanceTestVerifier(stepper);

		return verifier.verify(xmlfile, reduce);
	}

	private Set<String> getGuardsFromFile(File file) throws Exception {
		Set<String> guardset = new HashSet<String>();
		
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line;
		while ((line = br.readLine()) != null) {
			guardset.add(line);
		}
		br.close();
		
		return guardset;
	}
	
	private String getConditionFromSpec(String specxml) throws Exception {
		String content = new String(Files.readAllBytes(Paths.get(specxml)), "UTF-8");
		String condition = "[" + content.substring(content.indexOf("<condition>") + 11, content.indexOf("</condition>")).replace(" ", "") + "]";
		
		return condition;
	}
	
	private static String getCompositionString(String pnml) {
		String composition = pnml.substring(pnml.lastIndexOf("/") + 1).replace(".pnml", "");

		composition = composition.toUpperCase().replace("-", " ");
		
		if (composition.startsWith("OR")) {
			composition = composition.substring(0, 2) + " " + composition.substring(2);
		}
		else {
			composition = composition.substring(0, 3) + " " + composition.substring(3);
		}
		
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
