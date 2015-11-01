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
package org.sat4j.pb.reader;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Iterator;

import org.sat4j.pb.IPBSolver;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;

/**
 * @since 2.2
 */
public class OPBReader2010 extends OPBReader2007 {

    public static final BigInteger SAT4J_MAX_BIG_INTEGER = new BigInteger(
            "100000000000000000000000000000000000000000");

    private boolean isWbo = false;

    private BigInteger softLimit = SAT4J_MAX_BIG_INTEGER;

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    public OPBReader2010(IPBSolver solver) {
        super(solver);
    }

    /**
     * read the first comment line to get the number of variables and the number
     * of constraints in the file calls metaData with the data that was read
     * 
     * @throws IOException
     * @throws ParseFormatException
     */
    @Override
    protected void readMetaData() throws IOException, ParseFormatException {
        char c;
        String s;

        // get the number of variables and constraints
        c = get();
        if (c != '*') {
            throw new ParseFormatException(
                    "First line of input file should be a comment");
        }
        s = readWord();
        if (eof() || !"#variable=".equals(s)) {
            throw new ParseFormatException(
                    "First line should contain #variable= as first keyword");
        }

        this.nbVars = Integer.parseInt(readWord());
        this.nbNewSymbols = this.nbVars + 1;

        s = readWord();
        if (eof() || !"#constraint=".equals(s)) {
            throw new ParseFormatException(
                    "First line should contain #constraint= as second keyword");
        }

        this.nbConstr = Integer.parseInt(readWord());
        this.charAvailable = false;
        if (!eol()) {
            String rest = this.in.readLine();

            if (rest != null && rest.contains("#soft")) {
                this.isWbo = true;
                this.hasObjFunc = true;
            }
            if (rest != null && rest.indexOf("#product=") != -1) {
                String[] splitted = rest.trim().split(" ");
                if (splitted[0].equals("#product=")) {
                    Integer.parseInt(splitted[1]);
                }

                // if (splitted[2].equals("sizeproduct="))
                // readWord();

            }
        }
        // callback to transmit the data
        metaData(this.nbVars, this.nbConstr);
    }

    @Override
    protected void readObjective() throws IOException, ParseFormatException {
        if (this.isWbo) {
            readSoftLine();
        } else {
            super.readObjective();
        }
    }

    private void readSoftLine() throws IOException, ParseFormatException {
        String s = readWord();
        if (s == null || !"soft:".equals(s)) {
            throw new ParseFormatException("Did not find expected soft: line");
        }
        s = readWord().trim();
        if (s != null && !";".equals(s)) {
            this.softLimit = new BigInteger(s);
        }
        skipSpaces();
        if (get() != ';') {
            throw new ParseFormatException(
                    "soft: line should end with a semicolon");
        }
    }

    private boolean softConstraint;

    @Override
    protected void beginConstraint() {
        super.beginConstraint();
        this.softConstraint = false;
        try {
            if (this.isWbo) {
                skipSpaces();
                char c = get();
                putback(c);
                if (c == '[') {
                    this.softConstraint = true;
                    String s = readWord();
                    if (!s.endsWith("]")) {
                        throw new ParseFormatException(
                                "Expecting end of weight ");
                    }
                    BigInteger coeff = new BigInteger(s.substring(1,
                            s.length() - 1));
                    getCoeffs().push(coeff);
                    int varId = this.nbNewSymbols++;
                    getVars().push(varId);
                }

            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void endConstraint() throws ContradictionException {
        if (this.softConstraint) {
            int varId = getVars().last();
            BigInteger constrWeight = this.d;
            for (Iterator<BigInteger> it = this.coeffs.iterator(); it.hasNext();) {
                constrWeight = constrWeight.add(it.next().abs());
            }
            if ("<=".equals(this.operator)) {
                constrWeight = constrWeight.negate();
            }
            this.coeffs.push(constrWeight);
            this.lits.push(varId);
        }
        super.endConstraint();
    }

    @Override
    public IProblem parseInstance(final java.io.Reader input)
            throws ParseFormatException, ContradictionException {
        super.parseInstance(input);
        if (this.isWbo && this.softLimit != SAT4J_MAX_BIG_INTEGER) {
            this.solver.addPseudoBoolean(getVars(), getCoeffs(), false,
                    this.softLimit.subtract(BigInteger.ONE));
        }
        return this.solver;
    }
}
