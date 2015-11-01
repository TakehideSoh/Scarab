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
 * parser for pseudo-Boolean instances
 * 
 * Copyright (c) 2005-2007 Olivier ROUSSEL and Vasco MANQUINHO
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.ObjectiveFunction;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.reader.Reader;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

/**
 * Based on the "Official" reader for the Pseudo Boolean evaluation 2005.
 * http://www.cril.univ-artois.fr/PB05/parser/SimpleParser.java provided by
 * Olivier Roussel and Vasco Manquinho.
 * 
 * Modified to comply with SAT4J architecture by Mederic Baron
 * 
 * Updated since then by Daniel Le Berre
 * 
 * @author or
 * @author vm
 * @author mederic baron
 * @author leberre
 */
public class OPBReader2005 extends Reader implements Serializable {

    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 1L;

    protected final IPBSolver solver;

    protected final IVecInt lits;

    protected final IVec<BigInteger> coeffs;

    protected BigInteger d;

    protected String operator;

    private final IVecInt objectiveVars = new VecInt();

    private final IVec<BigInteger> objectiveCoeffs = new Vec<BigInteger>();

    // does the instance have an objective function?
    protected boolean hasObjFunc = false;

    // does the instance need variables explanation?
    protected boolean hasVariablesExplanation = false;

    protected int nbVars, nbConstr; // MetaData: #Variables and #Constraints in

    // file.
    protected int nbConstraintsRead;

    /**
     * callback called when we get the number of variables and the expected
     * number of constraints
     * 
     * @param nbvar
     *            the number of variables
     * @param nbconstr
     *            the number of contraints
     */
    protected void metaData(int nbvar, int nbconstr) {
        this.solver.newVar(nbvar);
    }

    /**
     * callback called before we read the objective function
     */
    protected void beginObjective() {
    }

    /**
     * callback called after we've read the objective function
     */
    protected void endObjective() {
        assert this.lits.size() == this.coeffs.size();
        assert this.lits.size() == this.coeffs.size();
        for (int i = 0; i < this.lits.size(); i++) {
            this.objectiveVars.push(this.lits.get(i));
            this.objectiveCoeffs.push(this.coeffs.get(i));
        }
    }

    /**
     * callback called before we read a constraint
     */
    protected void beginConstraint() {
        this.lits.clear();
        this.coeffs.clear();
        assert this.lits.size() == 0;
        assert this.coeffs.size() == 0;
    }

    /**
     * callback called after we've read a constraint
     */
    /**
     * @throws ContradictionException
     */
    protected void endConstraint() throws ContradictionException {

        assert !(this.lits.size() == 0);
        assert !(this.coeffs.size() == 0);
        assert this.lits.size() == this.coeffs.size();

        if ("=".equals(this.operator)) {
            this.solver.addExactly(this.lits, this.coeffs, this.d);
        } else if ("<=".equals(this.operator)) {
            this.solver.addAtMost(this.lits, this.coeffs, this.d);
        } else {
            assert ">=".equals(this.operator);
            this.solver.addAtLeast(this.lits, this.coeffs, this.d);
        }
        this.nbConstraintsRead++;
    }

    /**
     * callback called when we read a term of a constraint
     * 
     * @param coeff
     *            the coefficient of the term
     * @param var
     *            the identifier of the variable
     * @throws ParseFormatException
     */
    private void constraintTerm(BigInteger coeff, String var)
            throws ParseFormatException {
        this.coeffs.push(coeff);
        this.lits.push(translateVarToId(var));
    }

    protected int translateVarToId(String var) throws ParseFormatException {
        int id = Integer.parseInt(var.substring(1));
        return (this.savedChar == '-' ? -1 : 1) * id;
    }

    /**
     * callback called when we read the relational operator of a constraint
     * 
     * @param relop
     *            the relational oerator (>= or =)
     */
    protected void constraintRelOp(String relop) {
        this.operator = relop;
    }

    /**
     * callback called when we read the right term of a constraint (also known
     * as the degree)
     * 
     * @param val
     *            the degree of the constraint
     */
    protected void constraintRightTerm(BigInteger val) {
        this.d = val;
    }

    transient BufferedReader in; // the stream we're reading from

    char savedChar; // a character read from the file but not yet consumed

    boolean charAvailable = false; // true iff savedChar contains a character

    boolean eofReached = false; // true iff we've reached EOF

    private boolean eolReached = false;

    /**
     * get the next character from the stream
     * 
     * @throws IOException
     */
    protected char get() throws IOException {
        int c;

        if (this.charAvailable) {
            this.charAvailable = false;
            return this.savedChar;
        }

        c = this.in.read();
        if (c == -1) {
            this.eofReached = true;
        }
        if (c == '\n' || c == '\r') {
            this.eolReached = true;
        } else {
            this.eolReached = false;
        }
        return (char) c;
    }

    public IVecInt getVars() {
        return this.objectiveVars;
    }

    public IVec<BigInteger> getCoeffs() {
        return this.objectiveCoeffs;
    }

    /**
     * put back a character into the stream (only one chr can be put back)
     */
    protected void putback(char c) {
        this.savedChar = c;
        this.charAvailable = true;
    }

    /**
     * return true iff we've reached EOF
     */
    protected boolean eof() {
        return this.eofReached;
    }

    protected boolean eol() {
        return this.eolReached;
    }

    /**
     * skip white spaces
     * 
     * @throws IOException
     */
    protected void skipSpaces() throws IOException {
        char c;

        do {
            c = get();
        } while (Character.isWhitespace(c));

        putback(c);
    }

    /**
     * read a word from file
     * 
     * @return the word we read
     * @throws IOException
     */
    public String readWord() throws IOException {
        StringBuffer s = new StringBuffer();
        char c;

        skipSpaces();

        while (!Character.isWhitespace(c = get()) && !eol() && !eof()) {
            s.append(c);
        }

        putback(c);
        return s.toString();
    }

    /**
     * read a integer from file
     * 
     * @param s
     *            a StringBuffer to store the integer that was read
     * @throws IOException
     */
    public void readInteger(StringBuffer s) throws IOException {
        char c;

        skipSpaces();
        s.setLength(0);

        c = get();
        if (c == '-' || Character.isDigit(c)) {
            s.append(c);
            // note: BigInteger don't like a '+' before the number, we just skip
            // it
        }

        while (Character.isDigit(c = get()) && !eol() && !eof()) {
            s.append(c);
        }

        putback(c);
    }

    /**
     * read an identifier from stream and store it in s
     * 
     * @return the identifier we read or null
     * @throws IOException
     * @throws ParseFormatException
     */
    protected boolean readIdentifier(StringBuffer s) throws IOException,
            ParseFormatException {
        char c;

        s.setLength(0);

        skipSpaces();

        // first char (must be a letter or underscore)
        c = get();
        if (eof()) {
            return false;
        }

        if (!isGoodFirstCharacter(c)) {
            putback(c);
            return false;
        }

        s.append(c);

        // next chars (must be a letter, a digit or an underscore)
        while (true) {
            c = get();
            if (eof()) {
                break;
            }

            if (isGoodFollowingCharacter(c)) {
                s.append(c);
            } else {
                putback(c);
                break;
            }
        }
        checkId(s);
        return true;
    }

    protected boolean isGoodFirstCharacter(char c) {
        return Character.isLetter(c) || c == '_';
    }

    protected boolean isGoodFollowingCharacter(char c) {
        return Character.isLetter(c) || Character.isDigit(c) || c == '_';
    }

    protected void checkId(StringBuffer s) throws ParseFormatException {
        // Small check on the coefficient ID to make sure everything is ok
        int varID = Integer.parseInt(s.substring(1));
        if (varID > this.nbVars) {
            throw new ParseFormatException(
                    "Variable identifier larger than #variables in metadata.");
        }
    }

    /**
     * read a relational operator from stream and store it in s
     * 
     * @return the relational operator we read or null
     * @throws IOException
     */
    private String readRelOp() throws IOException {
        char c;

        skipSpaces();

        c = get();
        if (eof()) {
            return null;
        }

        if (c == '=') {
            return "=";
        }

        char following = get();
        if (c == '>' && following == '=') {
            return ">=";
        }
        if (c == '<' && following == '=') {
            return "<=";
        }

        return null;
    }

    /**
     * read the first comment line to get the number of variables and the number
     * of constraints in the file calls metaData with the data that was read
     * 
     * @throws IOException
     * @throws ParseException
     */
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

        s = readWord();
        if (eof() || !"#constraint=".equals(s)) {
            throw new ParseFormatException(
                    "First line should contain #constraint= as second keyword");
        }

        this.nbConstr = Integer.parseInt(readWord());

        // skip the rest of the line
        this.in.readLine();

        // callback to transmit the data
        metaData(this.nbVars, this.nbConstr);
    }

    /**
     * skip the comments at the beginning of the file
     * 
     * @throws IOException
     */
    private void skipComments() throws IOException {
        char c = ' ';

        // skip further comments

        while (!eof() && (c = get()) == '*') {
            this.in.readLine();
        }

        putback(c);
    }

    /**
     * read a term into coeff and var
     * 
     * @param coeff
     *            the coefficient of the variable
     * @param var
     *            the identifier we read
     * @throws IOException
     * @throws ParseException
     */
    protected void readTerm(StringBuffer coeff, StringBuffer var)
            throws IOException, ParseFormatException {
        char c;

        readInteger(coeff);

        skipSpaces();
        c = get();
        if (c != '*') {
            throw new ParseFormatException(
                    "'*' expected between a coefficient and a variable");
        }

        if (!readIdentifier(var)) {
            throw new ParseFormatException("identifier expected");
        }
    }

    private Map<Integer, String> mapping;

    /**
     * @throws IOException
     * @throws ParseFormatException
     */
    protected void readVariablesExplanation() throws IOException,
            ParseFormatException {
        char c = get();
        String s;
        while (c == '*') {
            s = readWord();
            if ("beginMapping".equals(s)) {
                startsMapping();
                get();
                c = get();
                continue;
            }
            if ("endMapping".equals(s)) {
                // this.in.readLine();
                get();
                System.out.println(mapping);
                return;
            }
            String[] values = s.split("=");
            if (values.length == 2) {
                mapping.put(Integer.valueOf(values[0]), values[1]);
            }
            if (this.savedChar != '\n') {
                this.in.readLine();
            }
            get();// remove trailing \n
            c = get();
        }
        putback(c);
    }

    protected void startsMapping() {
        mapping = new HashMap<Integer, String>();
    }

    /**
     * read the objective line (if any) calls beginObjective, objectiveTerm and
     * endObjective
     * 
     * @throws IOException
     * @throws ParseException
     */
    protected void readObjective() throws IOException, ParseFormatException {
        char c;
        StringBuffer var = new StringBuffer();
        StringBuffer coeff = new StringBuffer();

        // read objective line (if any)

        skipSpaces();
        c = get();
        if (c != 'm') {
            // no objective line
            putback(c);
            if (solver.isVerbose()) {
                System.out.println(solver.getLogPrefix()
                        + " no objective function found");
            }
            return;
        }
        if (solver.isVerbose()) {
            System.out.println(solver.getLogPrefix()
                    + " objective function found");
        }
        this.hasObjFunc = true;
        if (get() == 'i' && get() == 'n' && get() == ':') {
            beginObjective(); // callback

            while (!eof()) {
                readTerm(coeff, var);
                constraintTerm(new BigInteger(coeff.toString()), var.toString()); // callback

                skipSpaces();
                c = get();
                if (c == ';') {
                    break; // end of objective
                } else if (c == '-' || c == '+' || Character.isDigit(c)) {
                    putback(c);
                } else {
                    throw new ParseFormatException(
                            "unexpected character in objective function");
                }
            }

            endObjective();
        } else {
            throw new ParseFormatException(
                    "input format error: 'min:' expected");
        }
    }

    /**
     * read a constraint calls beginConstraint, constraintTerm and endConstraint
     * 
     * @throws ParseException
     * @throws IOException
     * @throws ContradictionException
     */
    protected void readConstraint() throws IOException, ParseFormatException,
            ContradictionException {
        StringBuffer var = new StringBuffer();
        StringBuffer coeff = new StringBuffer();
        char c;

        beginConstraint();

        while (!eof()) {
            readTerm(coeff, var);
            constraintTerm(new BigInteger(coeff.toString()), var.toString());

            skipSpaces();
            c = get();
            if (c == '>' || c == '=' || c == '<') {
                // relational operator found
                putback(c);
                break;
            } else if (c == '-' || c == '+' || Character.isDigit(c)) {
                putback(c);
            } else {
                throw new ParseFormatException(
                        "unexpected character in constraint");
            }
        }

        if (eof()) {
            throw new ParseFormatException(
                    "unexpected EOF before end of constraint");
        }

        String relop;
        if ((relop = readRelOp()) == null) {
            throw new ParseFormatException(
                    "unexpected relational operator in constraint");

        }
        constraintRelOp(relop);
        readInteger(coeff);
        constraintRightTerm(new BigInteger(coeff.toString()));

        skipSpaces();
        c = get();
        if (eof() || c != ';') {
            throw new ParseFormatException(
                    "semicolon expected at end of constraint");
        }

        endConstraint();
    }

    public OPBReader2005(IPBSolver solver) {
        this.solver = solver;
        this.lits = new VecInt();
        this.coeffs = new Vec<BigInteger>();
    }

    /**
     * parses the file and uses the callbacks to send to send the data back to
     * the program
     * 
     * @throws IOException
     * @throws ParseException
     * @throws ContradictionException
     */
    public void parse() throws IOException, ParseFormatException,
            ContradictionException {
        mapping = null;

        readMetaData();

        skipComments();

        readVariablesExplanation();

        skipComments();

        readObjective();

        // read constraints
        this.nbConstraintsRead = 0;
        char c;
        while (!eof()) {
            skipSpaces();
            if (eof()) {
                break;
            }

            c = get();
            putback(c);
            if (c == '*') {
                skipComments();
            }

            if (eof()) {
                break;
            }

            readConstraint();
            // nbConstraintsRead++;
        }
        // Small check on the number of constraints
        if (this.nbConstraintsRead != this.nbConstr) {
            throw new ParseFormatException("Number of constraints read ("
                    + this.nbConstraintsRead + ") is different from metadata ("
                    + this.nbConstr + ")");
        }
    }

    @Override
    public IProblem parseInstance(final java.io.Reader input)
            throws ParseFormatException, ContradictionException {
        IProblem problem = parseInstance(new LineNumberReader(input));
        this.solver.setObjectiveFunction(getObjectiveFunction());
        return problem;
    }

    protected IProblem parseInstance(LineNumberReader input)
            throws ParseFormatException, ContradictionException {
        this.solver.reset();
        this.in = input;
        try {
            parse();
            return this.solver;
        } catch (ContradictionException ce) {
            throw ce;
        } catch (ParseFormatException pfe) {
            throw new ParseFormatException(" line "
                    + (input.getLineNumber() + 1)
                    + ", "
                    + pfe.getMessage().substring(
                            ParseFormatException.PARSING_ERROR.length()));
        } catch (Exception e) {
            throw new ParseFormatException(" line "
                    + (input.getLineNumber() + 1) + ", " + e.toString());

        }
    }

    @Override
    public String decode(int[] model) {
        StringBuffer stb = new StringBuffer();

        for (int i = 0; i < model.length; i++) {
            if (model[i] < 0) {
                stb.append("-x");
                stb.append(-model[i]);
            } else {
                stb.append("x");
                stb.append(model[i]);
            }
            stb.append(" ");
        }
        return stb.toString();
    }

    @Override
    public void decode(int[] model, PrintWriter out) {
        for (int i = 0; i < model.length; i++) {
            if (model[i] < 0) {
                out.print("-x");
                out.print(-model[i]);
            } else {
                out.print("x");
                out.print(model[i]);
            }
            out.print(" ");
        }
    }

    public ObjectiveFunction getObjectiveFunction() {
        if (this.hasObjFunc) {
            return new ObjectiveFunction(getVars(), getCoeffs());
        }
        return null;
    }

    public IVecInt getListOfVariables() {
        return null;
    }

    @Override
    public IProblem parseInstance(final InputStream in)
            throws ParseFormatException, ContradictionException, IOException {
        return parseInstance(new InputStreamReader(in));
    }

    @Override
    public boolean hasAMapping() {
        return mapping != null;
    }

    @Override
    public Map<Integer, String> getMapping() {
        return mapping;
    }
}
