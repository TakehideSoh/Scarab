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
import org.sat4j.specs.ISolver;

/**
 * @author leberre
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public abstract class AbstractRandomCardProblemsTest<T extends ISolver> extends
        AbstractPigeonHoleWithCardinalityTest<T> {

    /**
     * 
     */
    public AbstractRandomCardProblemsTest(String name) {
        super(name);
    }

    public void testRndDeg1() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX + "random-opb/rnddeg1.opb"));
    }

    public void testRndDeg2() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX + "random-opb/rnddeg2.opb"));
    }

    public void testRndDeg3() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX + "random-opb/rnddeg3.opb"));
    }

    public void testRndDeg4() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX + "random-opb/rnddeg4.opb"));
    }

    public void testRndDeg5() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX + "random-opb/rnddeg5.opb"));
    }

    public void testRndDeg6() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX + "random-opb/rnddeg6.opb"));
    }

    public void testRndDeg7() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX + "random-opb/rnddeg7.opb"));
    }

    public void testRndDeg8() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX + "random-opb/rnddeg8.opb"));
    }

    public void testRndDeg9() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX + "random-opb/rnddeg9.opb"));
    }

    public void testRndDeg10() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX + "random-opb/rnddeg10.opb"));
    }

    public void testRndDeg11() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX + "random-opb/rnddeg11.opb"));
    }

    public void testRndDeg12() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX + "random-opb/rnddeg12.opb"));
    }

    public void testRndDeg13() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX + "random-opb/rnddeg13.opb"));
    }

    public void testRndDeg14() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX + "random-opb/rnddeg14.opb"));
    }

    public void testRndDeg15() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX + "random-opb/rnddeg15.opb"));
    }

    public void testRndDeg16() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX + "random-opb/rnddeg16.opb"));
    }

    public void testRndDeg17() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX + "random-opb/rnddeg17.opb"));
    }

    public void testRndDeg18() throws FileNotFoundException, IOException,
            ParseFormatException {
        assertFalse(solveInstance(PREFIX + "random-opb/rnddeg18.opb"));
    }

}
