package org.sat4j.pb.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.PBSolverHandle;
import org.sat4j.pb.reader.OPBReader2012;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.specs.ContradictionException;
import org.sat4j.tools.DimacsStringSolver;
import org.sat4j.tools.encoding.EncodingStrategy;
import org.sat4j.tools.encoding.Policy;

/**
 * This util class allow to translate an OPB file into multiple CNF file, where
 * atMost constraints are translated through multiple encodings
 * 
 * @author lonca
 */
public class OpbToDimacsWriter {

    public static void main(String args[]) throws ParseFormatException,
            IOException, ContradictionException {
        new OpbToDimacsWriter(args);
    }

    public OpbToDimacsWriter(String[] args) throws ParseFormatException,
            IOException, ContradictionException {
        Set<EncodingStrategy> tabooEncodings = new HashSet<EncodingStrategy>();
        tabooEncodings.add(EncodingStrategy.NATIVE);
        for (EncodingStrategy strategy : EncodingStrategy.values()) {
            if (tabooEncodings.contains(strategy))
                continue;
            DimacsStringSolver dss = new DimacsStringSolver();
            IPBSolver solver = new ClausalConstraintsDecorator(new PBAdapter(
                    dss), Policy.getAdapterFromEncodingName(strategy));
            OPBReader2012 reader = new OPBReader2012(new PBSolverHandle(solver));
            solver.setVerbose(false);
            reader.parseInstance(args[0]);
            FileWriter fwriter = new FileWriter(args[0] + ".cardEncoding."
                    + strategy.name() + ".cnf");
            fwriter.write(dss.toString());
            fwriter.close();
        }
    }
}
