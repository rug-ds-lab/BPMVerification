package nl.rug.ds.bpm;

import nl.rug.ds.bpm.petrinet.ddnet.DataDrivenNet;
import nl.rug.ds.bpm.petrinet.interfaces.net.VerifiableNet;
import nl.rug.ds.bpm.petrinet.ptnet.PlaceTransitionNet;
import nl.rug.ds.bpm.pnml.ptnet.jaxb.ptnet.Net;
import nl.rug.ds.bpm.pnml.ptnet.marshaller.PTNetUnmarshaller;
import nl.rug.ds.bpm.specification.jaxb.BPMSpecification;
import nl.rug.ds.bpm.specification.jaxb.Condition;
import nl.rug.ds.bpm.specification.jaxb.Specification;
import nl.rug.ds.bpm.util.exception.MalformedNetException;
import nl.rug.ds.bpm.util.exception.SpecificationException;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.util.log.listener.VerificationLogListener;
import nl.rug.ds.bpm.verification.VerificationFactory;
import nl.rug.ds.bpm.verification.checker.Checker;
import nl.rug.ds.bpm.verification.checker.CheckerFactory;
import nl.rug.ds.bpm.verification.checker.nusmv2.NuSMVFactory;
import nl.rug.ds.bpm.verification.event.PerformanceEvent;
import nl.rug.ds.bpm.verification.event.VerificationEvent;
import nl.rug.ds.bpm.verification.event.listener.PerformanceEventListener;
import nl.rug.ds.bpm.verification.event.listener.VerificationEventListener;
import nl.rug.ds.bpm.verification.verifier.Verifier;
import org.apache.commons.cli.*;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class that enables verification from the command line.
 */
public class CommandlineVerifier implements VerificationEventListener, VerificationLogListener, PerformanceEventListener {

	/**
	 * Creates a CommandlineVerifier.
	 *
	 * @param args the command line arguments used.
	 */
	public CommandlineVerifier(String[] args) {
		// Add the log listener
		Logger.addLogListener(this);

		// Apache Commons CLI options
		Options options = new Options();

		Option pnmlOption = new Option("p", "pnml", true, "pnml file path");
		pnmlOption.setRequired(true);
		options.addOption(pnmlOption);

		Option netOption = new Option("n", "net", true, "type of Petri net to use, either ptnet (default) or ddnet");
		netOption.setRequired(false);
		options.addOption(netOption);

		Option specOption = new Option("s", "spec", true, "specification file path");
		specOption.setRequired(true);
		options.addOption(specOption);

		Option checkerOption = new Option("c", "checker", true, "model checker binary location");
		checkerOption.setRequired(true);
		options.addOption(checkerOption);

		Option verifierOption = new Option("v", "verifier", true, "type of verifier to use, either kripke, stutter, or multi (default)]");
		verifierOption.setRequired(false);
		options.addOption(verifierOption);

		Option outputOption = new Option("o", "output", true, "output directory path");
		outputOption.setRequired(false);
		options.addOption(outputOption);

		Option logOption = new Option("l", "log", true, "the log level, either critical, error, warning, info (default), verbose, or debug");
		logOption.setRequired(false);
		options.addOption(logOption);

		CommandLineParser parser = new DefaultParser();

		try {
			CommandLine cmd = parser.parse(options, args);

			String pnmlFilePath = cmd.getOptionValue("pnml");
			String specFilePath = cmd.getOptionValue("spec");
			String checkerBinPath = cmd.getOptionValue("checker");
			String netType = cmd.getOptionValue("net");
			String verifierType = cmd.getOptionValue("verifier");
			String outputPath = cmd.getOptionValue("output");
			String logLevel = cmd.getOptionValue("log");

			// Set the log level
			setLogLevel(logLevel);

			// Load the pnml, specification, and create a model checker factory
			VerifiableNet net = loadNet(pnmlFilePath, netType);
			BPMSpecification specification = loadSpecification(specFilePath);
			CheckerFactory checkerFactory = loadModelChecker(checkerBinPath, outputPath);

			// Verify
			verify(net, specification, checkerFactory, verifierType);

		} catch (ParseException e) {
			Logger.log(e.getMessage(), LogEvent.ERROR);
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("CommandlineVerifier", options);

			System.exit(1);
		}
	}

	public static void main(String[] args) {
		CommandlineVerifier pnmlVerifier = new CommandlineVerifier(args);
	}

	/**
	 * Loads the pnml file at the given location.
	 *
	 * @param pnml the location of the pnml file.
	 * @return a PlaceTransitionNet.
	 */
	public PlaceTransitionNet loadNet(String pnml) {
		return (PlaceTransitionNet) loadNet(pnml, "ptnt");
	}

	/**
	 * Loads the pnml file at the given location and returns a net of the given type.
	 *
	 * @param pnml the location of the pnml file.
	 * @param type the type of net to return.
	 * @return a VerifiableNet of the given type.
	 */
	public VerifiableNet loadNet(String pnml, String type) {
		VerifiableNet net = null;

		File file = new File(pnml);
		if (!file.exists() || !file.isFile())
			Logger.log("No such pnml file", LogEvent.CRITICAL);

		//Load net(s) from pnml file
		try {
			PTNetUnmarshaller pnu = new PTNetUnmarshaller(file);
			Set<Net> pnset = pnu.getNets();

			Iterator<Net> nets = pnset.iterator();
			if (!nets.hasNext())
				Logger.log("Failed to load pnml file", LogEvent.CRITICAL);

			//Create Petri net object from the first pnml net
			net = (type != null && type.equalsIgnoreCase("ddnet") ? new DataDrivenNet(nets.next()) : new PlaceTransitionNet(nets.next()));

		} catch (MalformedNetException e) {
			Logger.log(e.getMessage(), LogEvent.CRITICAL);
		}

		return net;
	}

	/**
	 * Sets the log level to the given level.
	 *
	 * @param level the given level.
	 */
	public void setLogLevel(String level) {
		if (level != null) {
			switch (level.toLowerCase()) {
				case "critical" -> Logger.setLogLevel(LogEvent.CRITICAL);
				case "error" -> Logger.setLogLevel(LogEvent.ERROR);
				case "warning" -> Logger.setLogLevel(LogEvent.WARNING);
				case "info" -> Logger.setLogLevel(LogEvent.INFO);
				case "verbose" -> Logger.setLogLevel(LogEvent.VERBOSE);
				case "debug" -> Logger.setLogLevel(LogEvent.DEBUG);
				default -> Logger.setLogLevel(LogEvent.INFO);
			}
		} else
			Logger.setLogLevel(LogEvent.INFO);
	}

	/**
	 * Obtain a checker factory.
	 *
	 * @param checker the path to the binary of the NuSMV2 or NuXMV model checker.
	 * @param output  the directory to write the model checker input file to.
	 * @return a checker factory.
	 */
	public CheckerFactory loadModelChecker(String checker, String output) {
		File nusmv2 = new File(checker);

		if (output != null) {
			File out = new File(output);
			if (out.exists() && out.isDirectory())
				Checker.setOutputPath(output);
		}

		if (!nusmv2.exists() || !nusmv2.isFile())
			Logger.log("No such model checker binary", LogEvent.CRITICAL);

		//Create the wanted model modelcheck factory
		return new NuSMVFactory(nusmv2);
	}

	/**
	 * Loads the specification file at the given location.
	 *
	 * @param spec the location of the specification example.
	 * @return a BPMSpecification.
	 */
	public BPMSpecification loadSpecification(String spec) {
		BPMSpecification bpmSpecification = null;

		File specification = new File(spec);

		if (!specification.exists() || !specification.isFile())
			Logger.log("No such specification file", LogEvent.CRITICAL);

		try {
			bpmSpecification = VerificationFactory.loadSpecification(specification);
		} catch (SpecificationException e) {
			Logger.log(e.getMessage(), LogEvent.CRITICAL);
		}

		return bpmSpecification;
	}

	/**
	 * Verifies the given net against the given specification using the given model checker factory using the specified type of verifier.
	 *
	 * @param net            the VerifiableNet to verify.
	 * @param specification  the specification to verify against.
	 * @param checkerFactory the model checker factory to use.
	 * @param type           the type of verifier to use.
	 */
	public void verify(VerifiableNet net, BPMSpecification specification, CheckerFactory checkerFactory, String type) {
		try {
			type = (type == null ? "" : type);

			//Make a verifier
			Verifier verifier = switch (type.toLowerCase()) {
				case "kripke" -> VerificationFactory.createKripkeVerifier(net, specification, checkerFactory);
				case "stutter" -> VerificationFactory.createStutterVerifier(net, specification, checkerFactory);
				case "multi" -> VerificationFactory.createMultiVerifier(net, specification, checkerFactory);
				default -> VerificationFactory.createMultiVerifier(net, specification, checkerFactory);
			};

			verifier.addVerificationEventListener(this);
			verifier.addPerformanceEventListener(this);

			//Start verification
			verifier.verify();
		} catch (Exception e) {
			Logger.log("Verification failure", LogEvent.CRITICAL);
		}
	}

	/**
	 * Listener for verification results.
	 *
	 * @param event the result of a specification event (a single rule within the loaded specification file).
	 */
	@Override
	public void verificationEvent(VerificationEvent event) {
		//Use for user feedback
		//Event returns: specification id, formula, type, result, and specification itself
		System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "] FEEDBACK: " + event.toString());
	}

	/**
	 * Listener for log events.
	 *
	 * @param event a logged event.
	 */
	@Override
	public void verificationLogEvent(LogEvent event) {
		//Use for log and textual user feedback
		System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "] " + event.toString());
	}

	/**
	 * listener for performance events.
	 *
	 * @param event the performance event.
	 */
	@Override
	public void performanceEvent(PerformanceEvent event) {
		//Use to obtain performance metrics, e.g., computation times, structure sizes, reduction.
		//Event returns the net, specification set, and a map with Name-Number pairs of metrics.
		StringBuilder sb = new StringBuilder();

		sb.append("Metrics for net: ").append(event.getNet().getId()).append(" - ").append(event.getNet().getId()).append("\n\r");
		if (event.getSpecificationSet() != null) {
			sb.append("Specification set: ").append("\n\r");
			sb.append("- Conditions: ").append(event.getSpecificationSet().getConditions().stream().map(Condition::getCondition).collect(Collectors.joining(", "))).append("\n\r");
			sb.append("- Specifications: ").append(event.getSpecificationSet().getSpecifications().stream().map(Specification::getId).collect(Collectors.joining(", "))).append("\n\r");
		}
		sb.append("\n\r");

		for (String name : event.getMetrics().keySet())
			sb.append(name).append("\t").append("=").append("\t").append(event.getMetrics().get(name)).append("\n\r");

		System.out.println(sb);
	}
}
