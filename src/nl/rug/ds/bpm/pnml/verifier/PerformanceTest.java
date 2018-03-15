package nl.rug.ds.bpm.pnml.verifier;

import java.util.HashSet;
import java.util.Set;

public class PerformanceTest {

	public static void main(String[] args) {
//		String stfolder = "/home/nick/Dropbox/BPM2018-ConditionalSubgraph/Evaluation/synthetic/evalrun/";
//		runConversionPerformanceTest(stfolder, 5);
		
		
		String rlfolder = "/home/nick/Dropbox/BPM2018-ConditionalSubgraph/Evaluation/reallife/";
		String pnml = "complaints.pnml";
		
		Set<String> specs = new HashSet<String>();
		specs.add("complaints1.xml");
		specs.add("complaints2.xml");
		specs.add("complaints3.xml");
		specs.add("complaints4a.xml");
		specs.add("complaints4b.xml");
		specs.add("complaints5.xml");

		runConversionPerformanceTest(rlfolder, pnml, specs, 5);
	}

	public static void runConversionPerformanceTest(String folder, String pnml, Set<String> specs, int runcount) {
		PNMLVerifierPerformanceTest test = new PNMLVerifierPerformanceTest();

		try {
			test.evaluateSpecific(folder, pnml, specs, runcount);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void runConversionPerformanceTest(String folder, int runcount) {
		PNMLVerifierPerformanceTest test = new PNMLVerifierPerformanceTest();

		try {
			test.evaluateAll(folder, runcount);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void singleModelTest() {
		PNMLVerifierPerformanceTest test = new PNMLVerifierPerformanceTest();

		String folder = "/home/nick/Dropbox/BPM2018-ConditionalSubgraph/Evaluation/synthetic/";
		String pnml = "xor4-10.pnml";
		String guardsfile = "xor4-10_guards.txt";
		String specxml = "xor4-10.xml";
					
		try {
			test.verify(folder + pnml, folder + guardsfile, folder + specxml);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void runJarExperiment() {
		String jarfolder = "/home/nick/git/BPMVerification/";
		String modelfolder = "/home/nick/Dropbox/BPM2018-ConditionalSubgraph/Evaluation/synthetic/";
		
		String pnml = "xor4-10.pnml";
		String nusmv2 = "/home/nick/Software/NuSMV-2.5.4/bin/NuSMV";

		PNMLVerifierExperiment exp = new PNMLVerifierExperiment();
		
		exp.run(jarfolder, modelfolder, pnml, nusmv2, 1);
	}
}
