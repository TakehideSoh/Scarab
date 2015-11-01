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

import org.sat4j.pb.GoodOPBReader;
import org.sat4j.pb.IPBSolver;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.reader.Reader;

/**
 * @author leberre
 * 
 *         Those pseudo boolean problems were kindly provided by Niklas Een.
 * 
 */
public abstract class AbstractEZPseudoBooleanAndPigeonHoleTest extends
        AbstractPigeonHoleWithCardinalityTest<IPBSolver> {

    /**
     * Cr?ation d'un test
     * 
     * @param arg
     *            argument ?ventuel
     */
    public AbstractEZPseudoBooleanAndPigeonHoleTest(String arg) {
        super(arg);
    }

    @Override
    protected Reader createInstanceReader(IPBSolver solver) {
        return new GoodOPBReader(solver);
    }

    @Override
    protected void tearDown() {
        super.tearDown();
    }

    // VASCO: traveling tournament problem
    public void testncirc43() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertTrue(solveInstance(PREFIX
                + "normalized-opb/submitted/manquinho/ttp/normalized-circ4_3.opb"));
    }

    public void testncirc63() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertTrue(solveInstance(PREFIX
                + "normalized-opb/submitted/manquinho/ttp/normalized-circ6_3.opb"));
    }

    public void testncirc83() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertTrue(solveInstance(PREFIX
                + "normalized-opb/submitted/manquinho/ttp/normalized-circ8_3.opb"));
    }

    public void testndata43() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertTrue(solveInstance(PREFIX
                + "normalized-opb/submitted/manquinho/ttp/normalized-data4_3.opb"));
    }

    public void testndata63() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertTrue(solveInstance(PREFIX
                + "normalized-opb/submitted/manquinho/ttp/normalized-data6_3.opb"));
    }

    public void testndata83() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertTrue(solveInstance(PREFIX
                + "normalized-opb/submitted/manquinho/ttp/normalized-data8_3.opb"));
    }

    public void testn9symml() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertTrue(solveInstance(PREFIX
                + "normalized-opb/submitted/manquinho/synthesis-ptl-cmos-circuits/normalized-9symml.opb"));
    }

    public void testnC17() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertTrue(solveInstance(PREFIX
                + "normalized-opb/submitted/manquinho/synthesis-ptl-cmos-circuits/normalized-C17.opb"));
    }

    public void testnC432() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertTrue(solveInstance(PREFIX
                + "normalized-opb/submitted/manquinho/synthesis-ptl-cmos-circuits/normalized-C432.opb"));
    }

    public void testnb1() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertTrue(solveInstance(PREFIX
                + "normalized-opb/submitted/manquinho/synthesis-ptl-cmos-circuits/normalized-b1.opb"));
    }

    public void testnc8() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertTrue(solveInstance(PREFIX
                + "normalized-opb/submitted/manquinho/synthesis-ptl-cmos-circuits/normalized-c8.opb"));
    }

    public void testncc() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertTrue(solveInstance(PREFIX
                + "normalized-opb/submitted/manquinho/synthesis-ptl-cmos-circuits/normalized-cc.opb"));
    }

    public void testncm42a() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertTrue(solveInstance(PREFIX
                + "normalized-opb/submitted/manquinho/synthesis-ptl-cmos-circuits/normalized-cm42a.opb"));
    }

    public void testncmb() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertTrue(solveInstance(PREFIX
                + "normalized-opb/submitted/manquinho/synthesis-ptl-cmos-circuits/normalized-cmb.opb"));
    }

    public void testnmux() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertTrue(solveInstance(PREFIX
                + "normalized-opb/submitted/manquinho/synthesis-ptl-cmos-circuits/normalized-mux.opb"));
    }

    public void testnmyadder() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertTrue(solveInstance(PREFIX
                + "normalized-opb/submitted/manquinho/synthesis-ptl-cmos-circuits/normalized-my_adder.opb"));
    }
}
