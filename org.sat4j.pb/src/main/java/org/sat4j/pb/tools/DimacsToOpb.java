package org.sat4j.pb.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.OPBStringSolver;
import org.sat4j.reader.DimacsReader;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.specs.ContradictionException;

/**
 * Read an OPB file and output a CNF file.
 * 
 * Uses binomial encoding for PB constraints.
 * 
 * @author leberre
 *
 */
public class DimacsToOpb {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage : dimacs2opb filename.cnf");
            return;
        }
        String cnfFileName = args[0];
        assert cnfFileName.endsWith("cnf");
        String opbFileName = cnfFileName.replace("cnf", "opb");
        IPBSolver solver = new OPBStringSolver();
        DimacsReader reader = new DimacsReader(solver);
        try {
            reader.parseInstance(cnfFileName);
            PrintWriter out = new PrintWriter(new FileWriter(opbFileName));
            out.println(solver.toString());
            out.close();

        } catch (ParseFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ContradictionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
