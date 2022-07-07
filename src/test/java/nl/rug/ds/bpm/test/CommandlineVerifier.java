package nl.rug.ds.bpm.test;

import nl.rug.ds.bpm.petrinet.ddnet.DataDrivenNet;
import nl.rug.ds.bpm.petrinet.ptnet.PlaceTransitionNet;
import nl.rug.ds.bpm.pnml.ptnet.jaxb.ptnet.Net;
import nl.rug.ds.bpm.pnml.ptnet.marshaller.PTNetUnmarshaller;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.util.log.listener.VerificationLogListener;
import nl.rug.ds.bpm.variability.SpecificationToXML;
import nl.rug.ds.bpm.variability.VariabilitySpecification;
import nl.rug.ds.bpm.verification.VerificationFactory;
import nl.rug.ds.bpm.verification.checker.Checker;
import nl.rug.ds.bpm.verification.checker.CheckerFactory;
import nl.rug.ds.bpm.verification.checker.nusmv2.NuSMVFactory;
import nl.rug.ds.bpm.verification.event.VerificationEvent;
import nl.rug.ds.bpm.verification.event.listener.VerificationEventListener;
import nl.rug.ds.bpm.verification.verifier.Verifier;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class CommandlineVerifier implements VerificationEventListener, VerificationLogListener {

	public static void main(String[] args) {
		CommandlineVerifier pnmlVerifier = new CommandlineVerifier(args);
	}

	public CommandlineVerifier(String[] args) {
		Logger.addLogListener(this);

		if(args.length == 0)
			printUsage();
		else
			commandlineCall(args[0], Arrays.copyOfRange(args, 1, args.length));
	}

	public void commandlineCall(String option, String[] args) {
		switch (option) {
			case "-ptnet":
				ptVerify(args);
				break;
			case "-ddnet":
				ddVerify(args);
				break;
			case "-spec":
				specGen(args);
				break;
			default:
				printUsage();
				break;
		}
	}

	private void ptVerify(String[] args) {
		try {
			if (args.length > 2) {
				boolean reduce = true;
				File net = new File(args[0]);
				File spec = new File(args[1]);
				File nusmv2 = new File(args[2]);

				//Create the wanted model modelcheck factory
				CheckerFactory factory = new NuSMVFactory(nusmv2);
				//Load net(s) from pnml file
				PTNetUnmarshaller pnu = new PTNetUnmarshaller(net);
				Set<Net> pnset = pnu.getNets();
				//Create Petri net object from the first pnml net
				PlaceTransitionNet pn = new PlaceTransitionNet(pnset.iterator().next());

				if (args.length > 3)
					Checker.setOutputPath(args[3]);
				if (args.length > 4)
					reduce = Boolean.parseBoolean(args[4]);
				if (args.length > 5)
					Logger.setLogLevel(Integer.parseInt(args[5]));
				else
					Logger.setLogLevel(LogEvent.INFO);

				//Make a verifier
				Verifier verifier;
//				verifier = VerificationFactory.createMultiVerifier(pn, VerificationFactory.loadSpecification(spec), factory);
//				if (reduce)
				verifier = VerificationFactory.createStutterVerifier(pn, VerificationFactory.loadSpecification(spec), factory);
//				else
//					verifier = VerificationFactory.createKripkeVerifier(pn, VerificationFactory.loadSpecification(spec), factory);

				verifier.addEventListener(this);

				//Start verification
				verifier.verify();
			}
			else {
				printUsage();
			}
		} catch (Exception e) {
			Logger.log("Verification failure", LogEvent.CRITICAL);
		}
	}

	private void ddVerify(String[] args) {
		try {
			if (args.length > 2) {
				boolean reduce = true;
				File net = new File(args[0]);
				File spec = new File(args[1]);
				File nusmv2 = new File(args[2]);

				//Create the wanted model modelcheck factory
				CheckerFactory factory = new NuSMVFactory(nusmv2);
				//Load net(s) from pnml file
				PTNetUnmarshaller pnu = new PTNetUnmarshaller(net);
				Set<Net> pnset = pnu.getNets();
				//Create Petri net object from the first pnml net
				DataDrivenNet pn = new DataDrivenNet(pnset.iterator().next());

				if (args.length > 3)
					Checker.setOutputPath(args[3]);
				if (args.length > 4)
					reduce = Boolean.parseBoolean(args[4]);
				if (args.length > 5)
					Logger.setLogLevel(Integer.parseInt(args[5]));
				else
					Logger.setLogLevel(LogEvent.INFO);

				//Make a verifier
				Verifier verifier;
				if (reduce)
					verifier = VerificationFactory.createStutterVerifier(pn, VerificationFactory.loadSpecification(spec), factory);
				else
					verifier = VerificationFactory.createKripkeVerifier(pn, VerificationFactory.loadSpecification(spec), factory);

				verifier.addEventListener(this);

				//Start verification
				verifier.verify();
			}
			else {
				printUsage();
			}
		} catch (Exception e) {
			Logger.log("Verification failure", LogEvent.CRITICAL);
		}
	}

	private void specGen(String[] args) {
		try {
			if (args.length > 1) {
				File out = new File(args[0]);
				List<String> in = new ArrayList<>();

				for (int i = 1; i < args.length; i++)
					in.add(args[i]);

				VariabilitySpecification vs = new VariabilitySpecification(in, "silent");

				FileWriter fileWriter = new FileWriter(out);
				fileWriter.write(SpecificationToXML.getOutput(vs, "silent")[0]);
				fileWriter.close();
			}
			else {
				printUsage();
			}
		}
		catch (Exception e) {
			Logger.log("Generation failure", LogEvent.CRITICAL);
		}
	}

	public void printUsage() {
		System.out.println("Usage:");
		System.out.println("  BPMVerification -help");
		System.out.println("  BPMVerification -spec <output_file> <pnml_file>...");
		System.out.println("  BPMVerification -ptnet <pnml_file> <specification_file> <NuSMV_binary> [<output_path> [reduce{true|false} [log_level{0|1|2|3|4|5}]]]");
		System.out.println("  BPMVerification -ddnet <pnml_file> <specification_file> <NuSMV_binary> [<output_path> [reduce{true|false} [log_level{0|1|2|3|4|5}]]]");
	}

	//Listener implementations
	@Override
	public void verificationEvent(VerificationEvent event) {
		//Use for user feedback
		//Event returns: specification id, formula, type, result, and specification itself
		System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "] FEEDBACK: " + event.toString());
	}

	@Override
	public void verificationLogEvent(LogEvent event) {
		//Use for log and textual user feedback
		System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "] " + event.toString());
	}
}
