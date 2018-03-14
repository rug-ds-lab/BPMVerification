package nl.rug.ds.bpm.pnml.verifier;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Nick van Beest on 14-March-18.
 */
public class PNMLVerifierExperiment {
	
	public static void main(String[] args) {
		
	}
	
	public void runAll(String jarfolder, String modelfolder, String nusmv, int runcount) {
		List<String> models = getPNMLFilesInFolder(modelfolder, false);
		
		for (String m: models) {
			run(jarfolder, modelfolder, m, nusmv, runcount);
		}
	}
	
	public void run(String jarfolder, String modelfolder, String pnml, String nusmv, int runcount) {
		String guardsfile = pnml.replace(".pnml", "_guards.txt");
		String specxml = pnml.replace(".pnml", "xml");
		
		pnml = modelfolder + pnml;
		guardsfile = modelfolder + guardsfile;
		specxml = modelfolder + specxml;
				
		Process proc = null;
		try {
			Set<Double> exectimes = new HashSet<Double>();
			
			double starttime;
			for (int i = 0; i < 5; i++) {
				starttime = System.currentTimeMillis();
				
				proc = Runtime.getRuntime().exec("java -jar " + jarfolder + "BPM.jar " + pnml + " " + specxml + " " + nusmv + " " + guardsfile);
				
				proc.waitFor();
				
				exectimes.add(System.currentTimeMillis() - starttime);
			}
			
			double totaltime = getAverage(exectimes);
			
			// Then retreive the process output
			InputStream in = proc.getInputStream();
			InputStream err = proc.getErrorStream();
			
			byte b[] = new byte[in.available()];
	        in.read(b, 0, b.length);
	        System.out.println(new String(b));
	        
	        System.out.println(exectimes);
	        System.out.println("Time: " + totaltime);
			in.close();
			err.close();
			
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private double getAverage(Set<Double> numbers) {
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
	
	private List<String> getPNMLFilesInFolder(String folder, Boolean inclSubFolders) {
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
