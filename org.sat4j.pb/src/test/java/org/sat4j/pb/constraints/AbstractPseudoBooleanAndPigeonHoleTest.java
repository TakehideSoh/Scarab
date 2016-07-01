/*******************************************************************************
 * SAT4J: a SATisfiability library for Java Copyright (C) 2004, 2012 Artois University and CNRS
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU Lesser General Public License Version 2.1 or later (the
 * "LGPL"), in which case the provisions of the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of the LGPL, and not to allow others to use your version of
 * this file under the terms of the EPL, indicate your decision by deleting
 * the provisions above and replace them with the notice and other provisions
 * required by the LGPL. If you do not delete the provisions above, a recipient
 * may use your version of this file under the terms of the EPL or the LGPL.
 *
 * Based on the original MiniSat specification from:
 *
 * An extensible SAT solver. Niklas Een and Niklas Sorensson. Proceedings of the
 * Sixth International Conference on Theory and Applications of Satisfiability
 * Testing, LNCS 2919, pp 502-518, 2003.
 *
 * See www.minisat.se for the original solver in C++.
 *
 * Contributors:
 *   CRIL - initial API and implementation
 *******************************************************************************/

package org.sat4j.pb.constraints;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.sat4j.reader.ParseFormatException;

/**
 * @author leberre
 * 
 *         Those pseudo boolean problems were kindly provided by Niklas Een.
 * 
 */
public abstract class AbstractPseudoBooleanAndPigeonHoleTest extends
        AbstractEZPseudoBooleanAndPigeonHoleTest {

    protected static final String PREFIX = System.getProperty("test.pbprefix");

    public AbstractPseudoBooleanAndPigeonHoleTest(String arg) {
        super(arg);
    }

    public void testaloul1011() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX
                + "normalized-opb/submitted/aloul/FPGA_SAT05/normalized-chnl10_11_pb.cnf.cr.opb"));
    }

    public void testaloul1015() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX
                + "normalized-opb/submitted/aloul/FPGA_SAT05/normalized-chnl10_15_pb.cnf.cr.opb"));
    }

    public void testaloul1020() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX
                + "normalized-opb/submitted/aloul/FPGA_SAT05/normalized-chnl10_20_pb.cnf.cr.opb"));
    }

    public void testaloul1516() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX
                + "normalized-opb/submitted/aloul/FPGA_SAT05/normalized-chnl15_16_pb.cnf.cr.opb"));
    }

    public void testaloul1520() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX
                + "normalized-opb/submitted/aloul/FPGA_SAT05/normalized-chnl15_20_pb.cnf.cr.opb"));
    }

    public void testaloul1525() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX
                + "normalized-opb/submitted/aloul/FPGA_SAT05/normalized-chnl15_25_pb.cnf.cr.opb"));
    }

    public void testaloul2021() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX
                + "normalized-opb/submitted/aloul/FPGA_SAT05/normalized-chnl20_21_pb.cnf.cr.opb"));
    }

    public void testaloul2025() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX
                + "normalized-opb/submitted/aloul/FPGA_SAT05/normalized-chnl20_25_pb.cnf.cr.opb"));
    }

    public void testaloul2030() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX
                + "normalized-opb/submitted/aloul/FPGA_SAT05/normalized-chnl20_30_pb.cnf.cr.opb"));
    }

    public void testaloul3031() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX
                + "normalized-opb/submitted/aloul/FPGA_SAT05/normalized-chnl30_31_pb.cnf.cr.opb"));
    }

    public void testaloul3035() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX
                + "normalized-opb/submitted/aloul/FPGA_SAT05/normalized-chnl30_35_pb.cnf.cr.opb"));
    }

    public void testaloul3040() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX
                + "normalized-opb/submitted/aloul/FPGA_SAT05/normalized-chnl30_40_pb.cnf.cr.opb"));
    }

    public void testaloul3536() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX
                + "normalized-opb/submitted/aloul/FPGA_SAT05/normalized-chnl35_36_pb.cnf.cr.opb"));
    }

    public void testaloul3540() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX
                + "normalized-opb/submitted/aloul/FPGA_SAT05/normalized-chnl35_40_pb.cnf.cr.opb"));
    }

    public void testaloul3545() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX
                + "normalized-opb/submitted/aloul/FPGA_SAT05/normalized-chnl35_45_pb.cnf.cr.opb"));
    }

    public void testaloul4041() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX
                + "normalized-opb/submitted/aloul/FPGA_SAT05/normalized-chnl40_41_pb.cnf.cr.opb"));
    }

    public void testaloul4045() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX
                + "normalized-opb/submitted/aloul/FPGA_SAT05/normalized-chnl40_45_pb.cnf.cr.opb"));
    }

    public void testaloul4050() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX
                + "normalized-opb/submitted/aloul/FPGA_SAT05/normalized-chnl40_50_pb.cnf.cr.opb"));
    }

    public void testaloul5051() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX
                + "normalized-opb/submitted/aloul/FPGA_SAT05/normalized-chnl50_51_pb.cnf.cr.opb"));
    }

    public void testaloul5055() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX
                + "normalized-opb/submitted/aloul/FPGA_SAT05/normalized-chnl50_55_pb.cnf.cr.opb"));
    }

    public void testaloul5060() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX
                + "normalized-opb/submitted/aloul/FPGA_SAT05/normalized-chnl50_60_pb.cnf.cr.opb"));
    }

    public void testncirc103() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertTrue(solveInstance(PREFIX
                + "normalized-opb/submitted/manquinho/ttp/normalized-circ10_3.opb"));
    }

    public void testndata103() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertTrue(solveInstance(PREFIX
                + "normalized-opb/submitted/manquinho/ttp/normalized-data10_3.opb"));
    }

    public void testPN10() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX + "pigeons/PN-10-9.opb"));
    }
}
