package nl.rug.ds.bpm;

import nl.rug.ds.bpm.petrinet.interfaces.net.VerifiableNet;
import nl.rug.ds.bpm.specification.jaxb.BPMSpecification;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.verification.checker.CheckerFactory;
import nl.rug.ds.bpm.verification.event.PerformanceEvent;
import nl.rug.ds.bpm.verification.event.VerificationEvent;
import org.apache.commons.cli.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CommandLineEvaluation extends CommandlineVerifier {
    private final Map<String, Set<Map<String, Number>>> metrics;

    /**
     * Creates a CommandLineEvaluation.
     */
    public CommandLineEvaluation() {
        super();
        metrics = new LinkedHashMap<>();
    }

    /**
     * Creates a CommandLineEvaluation.
     *
     * @param args the command line arguments used.
     */
    public CommandLineEvaluation(String[] args) {
        this();

        Options options = createOptions();
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
            List<VerifiableNet> nets = loadNets(pnmlFilePath, netType);
            BPMSpecification specification = loadSpecification(specFilePath);
            CheckerFactory checkerFactory = loadModelChecker(checkerBinPath, outputPath);

            // Verify
            for (int i = 0; i < 10; i++) {
                for (VerifiableNet net : nets) {
                    Logger.log("Verifying net " + net.getName(), LogEvent.WARNING);
                    verify(net, specification, checkerFactory, verifierType);
                }
            }

            writeToXlsx(toExcel(), outputPath);

        } catch (ParseException e) {
            Logger.log(e.getMessage(), LogEvent.ERROR);
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("CommandlineVerifier", options);

            System.exit(1);
        }
    }

    public static void main(String[] args) {
        CommandLineEvaluation commandLineEvaluation = new CommandLineEvaluation(args);
    }

    /**
     * Returns a list of VerifiableNets found at the given location.
     *
     * @param location the directory containing pnml files.
     * @param type     the type of net to return.
     * @return A list of VerifiableNet.
     */
    public List<VerifiableNet> loadNets(String location, String type) {
        List<VerifiableNet> nets = new ArrayList<>();
        File dir = new File(location);

        if (dir.exists() && dir.isDirectory()) {
            FilenameFilter pnmlFilter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".pnml");
                }
            };

            List<File> pnmlFiles = new ArrayList<>();
            File[] files = dir.listFiles(pnmlFilter);
            if (files != null) {
                pnmlFiles.addAll(List.of(files));
                pnmlFiles.sort(new Comparator<File>() {
                    @Override
                    public int compare(File o1, File o2) {
                        return o1.toString().compareTo(o2.toString());
                    }

                    @Override
                    public boolean equals(Object obj) {
                        return false;
                    }
                });

                for (File pnml : pnmlFiles) {
                    nets.add(loadNet(pnml, type));
                }
            } else {
                Logger.log("No files found at " + location, LogEvent.CRITICAL);
            }
        } else {
            Logger.log("No such directory", LogEvent.CRITICAL);
        }

        return nets;
    }


    /**
     * Listener for verification results.
     *
     * @param event the result of a specification event (a single rule within the loaded specification file).
     */
    @Override
    public void verificationEvent(VerificationEvent event) {
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
        if (!metrics.containsKey(event.getNet().getName()))
            metrics.put(event.getNet().getName(), new HashSet<>());

        metrics.get(event.getNet().getName()).add(event.getMetrics());
    }

    private void writeToXlsx(Workbook workbook, String outputPath) {
        File dir = new File(outputPath);
        String path = dir.getAbsolutePath();
        String fileLocation = path + "/evaluation.xlsx";

        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(fileLocation);
            workbook.write(outputStream);
            workbook.close();
        } catch (IOException e) {
            Logger.log("Could not write to Excel file.", LogEvent.CRITICAL);
        }
    }

    private Workbook toExcel() {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Evaluation");

        Row header = sheet.createRow(0);

        Map<String, Integer> columns = getColumns();

        for (String col : columns.keySet()) {
            Cell headerCell = header.createCell(columns.get(col));
            headerCell.setCellValue(col);
        }

        int i = 1;
        for (String net : metrics.keySet()) {
            Row row = sheet.createRow(i++);
            Cell n = row.createCell(columns.get("Net"));
            n.setCellValue(net);

            Set<Map<String, Number>> runs = metrics.get(net);

            if (!runs.isEmpty()) {
                for (String metric : runs.iterator().next().keySet()) {
                    Cell cell = row.createCell(columns.get(metric));

                    double average = 0;
                    double lowest = Double.MAX_VALUE;
                    double highest = 0;

                    for (Map<String, Number> run : runs) {
                        double value = run.get(metric).doubleValue();
                        average = average + value;

                        if (value < lowest)
                            lowest = value;

                        if (value > highest)
                            highest = value;
                    }
                    average = average - lowest;
                    average = average - highest;
                    average = average / (runs.size() - 2);

                    cell.setCellValue(average);
                }
            }
        }

        return workbook;
    }

    private Map<String, Integer> getColumns() {
        Map<String, Integer> columns = new LinkedHashMap<>();
        int col = 0;
        columns.put("Net", col++);

        for (String net : metrics.keySet()) {
            Set<Map<String, Number>> runs = metrics.get(net);
            if (!runs.isEmpty()) {
                for (String metric : runs.iterator().next().keySet()) {
                    if (!columns.containsKey(metric)) {
                        columns.put(metric, col++);
                    }
                }
            }
        }

        return columns;
    }
}
