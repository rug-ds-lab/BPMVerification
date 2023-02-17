package nl.rug.ds.bpm.test;

import nl.rug.ds.bpm.eventstructure.PESPrefixUnfolding;
import nl.rug.ds.bpm.variability.SpecificationToXML;
import nl.rug.ds.bpm.variability.VariabilitySpecification;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class CommandlineSpecGen {

    public CommandlineSpecGen(String[] args) {
        specGen(args);
    }

    public static void main(String[] args) {
        CommandlineSpecGen specGen = new CommandlineSpecGen(args);
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

                printPESPrefixUnfoldingsS(vs);
            } else {
                System.out.println("CommandlineSpecGen output input_1 input_2 ...");
            }
        } catch (Exception e) {
            System.out.println("Generation failure");
        }
    }

    private void printPESPrefixUnfoldingsS(VariabilitySpecification vs) {
        for (PESPrefixUnfolding pes : vs.getCES().getSourcePesPrefixUnfoldings())
            System.out.println(pes.toString().replaceAll("silent", "s") + "\n\n");
    }
}
