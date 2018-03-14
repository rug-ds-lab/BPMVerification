package nl.rug.ds.bpm.pnml.verifier;

public class PerformanceTest {

	public static void main(String[] args) {
		String folder = "/home/nick/Dropbox/BPM2018-ConditionalSubgraph/Evaluation/synthetic/evalrun/";

		runConversionPerformanceTest(folder);
	}

	public static void runConversionPerformanceTest(String folder) {
		PNMLVerifierPerformanceTest test = new PNMLVerifierPerformanceTest();

		try {
			test.evaluateAll(folder, 5);
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
