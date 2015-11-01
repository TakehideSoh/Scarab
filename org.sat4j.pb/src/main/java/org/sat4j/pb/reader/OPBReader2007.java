/*******************************************************************************
 * SAT4J: a SATisfiability library for Java Copyright (C) 2004-2008 Daniel Le Berre
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 * Based on the pseudo boolean algorithms described in:
 * A fast pseudo-Boolean constraint solver Chai, D.; Kuehlmann, A.
 * Computer-Aided Design of Integrated Circuits and Systems, IEEE Transactions on
 * Volume 24, Issue 3, March 2005 Page(s): 305 - 317
 * 
 * and 
 * Heidi E. Dixon, 2004. Automating Pseudo-Boolean Inference within a DPLL 
 * Framework. Ph.D. Dissertation, University of Oregon.
 *******************************************************************************/
/*=============================================================================
 * parser for CSP instances represented in XML format
 * 
 * Copyright (c) 2006 Olivier ROUSSEL (olivier.roussel <at> cril.univ-artois.fr)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *=============================================================================
 */
package org.sat4j.pb.reader;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.pb.IPBSolver;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

/**
 * Reader complying with the PB07 input format.
 * 
 * Non-linear to linear translation adapted from the PB07 readers provided by
 * Olivier Roussel and Vasco Manquinho (was available in C++, not in Java)
 * 
 * http://www.cril.univ-artois.fr/PB07/parser/SimpleParser.java
 * http://www.cril.univ-artois.fr/PB07/parser/SimpleParser.cc
 * 
 * @author parrain
 * @author daniel
 * 
 */
public class OPBReader2007 extends OPBReader2006 {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * @param solver
     */
    public OPBReader2007(IPBSolver solver) {
        super(solver);
    }

    @Override
    protected boolean isGoodFirstCharacter(char c) {
        return Character.isLetter(c) || c == '_' || c == '~';
    }

    @Override
    protected void checkId(StringBuffer s) throws ParseFormatException {
        // Small check on the coefficient ID to make sure everything is ok
        int cpt = 1;
        if (s.charAt(0) == '~') {
            cpt = 2;
        }
        if (s.charAt(cpt - 1) != 'x') {
            throw new ParseFormatException(
                    "Only identifiers of the form 'x'YYY are allowed!");
        }
        int varID = Integer.parseInt(s.substring(cpt));
        if (varID > this.nbVars) {
            throw new ParseFormatException(
                    "Variable identifier larger than #variables in metadata.");
        }
    }

    /**
     * contains the number of new symbols generated to linearize products
     */
    protected int nbNewSymbols;

    @Override
    protected void readTerm(StringBuffer coeff, StringBuffer var)
            throws IOException, ParseFormatException {
        readInteger(coeff);

        skipSpaces();

        var.setLength(0);
        IVec<String> tmpLit = new Vec<String>();
        StringBuffer tmpVar = new StringBuffer();
        while (readIdentifier(tmpVar)) {
            tmpLit = tmpLit.push(tmpVar.toString());
            skipSpaces();
        }
        if (tmpLit.size() == 0) {
            throw new ParseFormatException("identifier expected");
        }
        if (tmpLit.size() == 1) {
            // it is a "normal" term
            var.append(tmpLit.last());
            tmpLit.pop();
        } else {
            // it is a product term
            try {
                var.append(linearizeProduct(tmpLit));
            } catch (ContradictionException e) {
                throw new ParseFormatException(e);
            }
        }
    }

    /**
     * callback called when we read a term of a constraint
     * 
     * @param var
     *            the identifier of the variable
     * @param lits
     *            a set of literals in DIMACS format in which var once
     *            translated will be added.
     * @throws ParseFormatException
     */
    protected void literalInAProduct(String var, IVecInt lits)
            throws ParseFormatException {
        int beginning = var.charAt(0) == '~' ? 2 : 1;
        int id = Integer.parseInt(var.substring(beginning));
        int lid = (var.charAt(0) == '~' ? -1 : 1) * id;
        if (lid == 0 || Math.abs(lid) >= this.nbNewSymbols) {
            throw new ParseFormatException("Wrong variable id");
        }
        lits.push(lid);
    }

    /**
     * callback called when we read a term of a constraint
     * 
     * @param var
     *            the identifier of the variable
     * @param lits
     *            a set of literals in DIMACS format in which var once
     *            translated will be added.
     */
    protected void negateLiteralInAProduct(String var, IVecInt lits) {
        int beginning = var.charAt(0) == '~' ? 2 : 1;
        int id = Integer.parseInt(var.substring(beginning));
        int lid = (var.charAt(0) == '~' ? 1 : -1) * id;
        lits.push(lid);
    }

    /**
     * read the first comment line to get the number of variables and the number
     * of constraints in the file calls metaData with the data that was read
     * 
     * @throws IOException
     * @throws ParseException
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
    protected int translateVarToId(String var) throws ParseFormatException {
        int beginning = var.charAt(0) == '~' ? 2 : 1;
        int id = Integer.parseInt(var.substring(beginning));
        if (id == 0 || id >= this.nbNewSymbols) {
            throw new ParseFormatException("Wrong variable id format: " + var);
        }
        return (var.charAt(0) == '~' ? -1 : 1) * id;
    }

    private String linearizeProduct(IVec<String> tmpLit)
            throws ContradictionException, ParseFormatException {
        tmpLit.sort(String.CASE_INSENSITIVE_ORDER);
        String newVar = getProductVariable(tmpLit);
        if (newVar == null) {
            // generate a new symbol
            newVar = "X" + this.nbNewSymbols++;
            // linearization proposed by O. Roussel (PB07)
            // generate the clause
            // product => newSymbol (this is a clause)
            // not x1 or not x2 ... or not xn or newSymbol
            if (tmpLit.size() == 2) {
                Map<String, String> map1 = this.binaryProductToVar.get(tmpLit
                        .get(0));
                if (map1 == null) {
                    map1 = new HashMap<String, String>();
                    this.binaryProductToVar.put(tmpLit.get(0), map1);
                }
                map1.put(tmpLit.get(1), newVar);
            }
            this.varToProduct.put(newVar, tmpLit);
            IVecInt newLits = new VecInt();
            for (Iterator<String> iterator = tmpLit.iterator(); iterator
                    .hasNext();) {
                negateLiteralInAProduct(iterator.next(), newLits);
            }
            literalInAProduct(newVar, newLits);
            this.solver.addClause(newLits);
            // generate the PB-constraint
            // newSymbol => product translated as
            // x1+x2+x3...+xn-n*newSymbol>=0
            newLits.clear();
            IVec<BigInteger> newCoefs = new Vec<BigInteger>();
            for (Iterator<String> iterator = tmpLit.iterator(); iterator
                    .hasNext();) {
                literalInAProduct(iterator.next(), newLits);
                newCoefs.push(BigInteger.ONE);
            }
            literalInAProduct(newVar, newLits);
            newCoefs.push(new BigInteger(String.valueOf(-tmpLit.size())));
            this.solver.addPseudoBoolean(newLits, newCoefs, true,
                    BigInteger.ZERO);
            // nbConstraintsRead += 2;
        }
        return newVar;
    }

    private final Map<String, IVec<String>> varToProduct = new HashMap<String, IVec<String>>();

    private final Map<String, Map<String, String>> binaryProductToVar = new HashMap<String, Map<String, String>>();

    private String getProductVariable(IVec<String> lits) {
        if (lits.size() == 2) {
            Map<String, String> map = this.binaryProductToVar.get(lits.get(0));
            if (map == null) {
                return null;
            }
            return map.get(lits.get(1));
        }
        for (Map.Entry<String, IVec<String>> c : this.varToProduct.entrySet()) {
            if (c.getValue().equals(lits)) {
                return c.getKey();
            }
        }
        return null;
    }

    @Override
    public String decode(int[] model) {
        StringBuffer stb = new StringBuffer();
        int p;
        for (int element : model) {
            p = element;
            if (Math.abs(p) <= this.nbVars) {
                if (p < 0) {
                    stb.append("-x");
                    stb.append(-p);
                } else {
                    stb.append("x");
                    stb.append(p);
                }
                stb.append(" ");
            }
        }
        return stb.toString();
    }

    @Override
    public void decode(int[] model, PrintWriter out) {
        int p;
        for (int element : model) {
            p = element;
            if (Math.abs(p) <= this.nbVars) {
                if (element < 0) {
                    out.print("-x");
                    out.print(-p);
                } else {
                    out.print("x");
                    out.print(p);
                }
                out.print(" ");
            }
        }
    }

}
