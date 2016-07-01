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
package org.sat4j.pb;

import java.math.BigInteger;

import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.IteratorInt;
import org.sat4j.specs.TimeoutException;
import org.sat4j.specs.VarMapper;
import org.sat4j.tools.DimacsStringSolver;

/**
 * Solver used to display in a string the pb-instance in OPB format.
 * 
 * That solver is useful to produce OPB files to be used by third party solvers.
 * 
 * @author parrain
 * 
 */
public class OPBStringSolver extends DimacsStringSolver implements IPBSolver {

    private static final String FAKE_I_CONSTR_MSG = "Fake IConstr";

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private int indxConstrObj;

    private int nbOfConstraints;

    private ObjectiveFunction obj;

    private boolean inserted = false;

    private static final IConstr FAKE_CONSTR = new IConstr() {

        public int size() {
            throw new UnsupportedOperationException(FAKE_I_CONSTR_MSG);
        }

        public boolean learnt() {
            throw new UnsupportedOperationException(FAKE_I_CONSTR_MSG);
        }

        public double getActivity() {
            throw new UnsupportedOperationException(FAKE_I_CONSTR_MSG);
        }

        public int get(int i) {
            throw new UnsupportedOperationException(FAKE_I_CONSTR_MSG);
        }

        public boolean canBePropagatedMultipleTimes() {
            throw new UnsupportedOperationException(FAKE_I_CONSTR_MSG);
        }

        public String toString(VarMapper mapper) {
            return FAKE_I_CONSTR_MSG;
        }
    };

    /**
	 * 
	 */
    public OPBStringSolver() {
    }

    /**
     * @param initSize
     */
    public OPBStringSolver(int initSize) {
        super(initSize);
    }

    @Override
    public boolean isSatisfiable(IVecInt assumps) throws TimeoutException {
        for (IteratorInt it = assumps.iterator(); it.hasNext();) {
            int p = it.next();
            if (p > 0) {
                getOut().append("+1 x" + p + " >= 1 ;\n");
            } else {
                getOut().append("-1 x" + -p + " >= 0 ;\n");
            }
            this.nbOfConstraints++;
        }
        throw new TimeoutException();
    }

    public IConstr addPseudoBoolean(IVecInt lits, IVec<BigInteger> coeffs,
            boolean moreThan, BigInteger d) throws ContradictionException {
        if (moreThan) {
            return addAtLeast(lits, coeffs, d);
        }
        return addAtMost(lits, coeffs, d);
    }

    public void setObjectiveFunction(ObjectiveFunction obj) {
        this.obj = obj;
    }

    @Override
    public IConstr addAtLeast(IVecInt literals, int degree)
            throws ContradictionException {
        StringBuffer out = getOut();
        this.nbOfConstraints++;
        int negationweight = 0;
        int p;
        for (IteratorInt iterator = literals.iterator(); iterator.hasNext();) {
            p = iterator.next();
            assert p != 0;
            if (p > 0) {
                out.append("+1 x" + p + " ");
            } else {
                out.append("-1 x" + -p + " ");
                negationweight++;
            }
        }
        out.append(">= " + (degree - negationweight) + " ;\n");
        return FAKE_CONSTR;
    }

    @Override
    public IConstr addAtMost(IVecInt literals, int degree)
            throws ContradictionException {
        StringBuffer out = getOut();
        this.nbOfConstraints++;
        int negationweight = 0;
        int p;
        for (IteratorInt iterator = literals.iterator(); iterator.hasNext();) {
            p = iterator.next();
            assert p != 0;
            if (p > 0) {
                out.append("-1 x" + p + " ");
            } else {
                out.append("+1 x" + -p + " ");
                negationweight++;
            }
        }
        out.append(">= " + (-degree + negationweight) + " ;\n");
        return FAKE_CONSTR;
    }

    @Override
    public IConstr addClause(IVecInt literals) throws ContradictionException {
        StringBuffer out = getOut();
        this.nbOfConstraints++;
        int negationweight = 0;
        int lit;
        for (IteratorInt iterator = literals.iterator(); iterator.hasNext();) {
            lit = iterator.next();
            if (lit > 0) {
                out.append("+1 x" + lit + " ");
            } else {
                out.append("-1 x" + -lit + " ");
                negationweight++;
            }
        }
        out.append(">= " + (1 - negationweight) + " ;\n");
        return FAKE_CONSTR;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.sat4j.pb.IPBSolver#getExplanation()
     */
    public String getExplanation() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.sat4j.pb.IPBSolver#setListOfVariablesForExplanation(org.sat4j.specs
     * .IVecInt)
     */
    public void setListOfVariablesForExplanation(IVecInt listOfVariables) {
        // TODO Auto-generated method stub

    }

    @Override
    public String toString() {
        StringBuffer out = getOut();
        if (!this.inserted) {
            StringBuffer tmp = new StringBuffer();
            tmp.append("* #variable= ");
            tmp.append(nVars());
            tmp.append(" #constraint= ");
            tmp.append(this.nbOfConstraints);
            if (this.obj != null) {
                tmp.append("\n");
                tmp.append("min: ");
                tmp.append(this.obj);
                tmp.append(";");
            }
            out.insert(this.indxConstrObj, tmp.toString());
            this.inserted = true;
        }
        return out.toString();
    }

    @Override
    public String toString(String prefix) {
        return "OPB output solver";
    }

    @Override
    public int newVar(int howmany) {
        StringBuffer out = getOut();
        setNbVars(howmany);
        // to add later the number of constraints
        this.indxConstrObj = out.length();
        out.append("\n");
        return howmany;
    }

    @Override
    public void setExpectedNumberOfClauses(int nb) {
    }

    public ObjectiveFunction getObjectiveFunction() {
        return this.obj;
    }

    @Override
    public int nConstraints() {
        return this.nbOfConstraints;
    }

    public IConstr addAtMost(IVecInt literals, IVecInt coeffs, int degree)
            throws ContradictionException {
        StringBuffer out = getOut();
        assert literals.size() == coeffs.size();
        this.nbOfConstraints++;
        for (int i = 0; i < literals.size(); i++) {
            out.append(-coeffs.get(i));
            out.append(" x");
            out.append(literals.get(i));
            out.append(" ");
        }
        out.append(">= ");
        out.append(-degree);
        out.append(" ;\n");
        return FAKE_CONSTR;
    }

    public IConstr addAtMost(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger degree) throws ContradictionException {
        StringBuffer out = getOut();
        assert literals.size() == coeffs.size();
        this.nbOfConstraints++;
        for (int i = 0; i < literals.size(); i++) {
            out.append(coeffs.get(i).negate());
            out.append(" x");
            out.append(literals.get(i));
            out.append(" ");
        }
        out.append(">= ");
        out.append(degree.negate());
        out.append(" ;\n");
        return FAKE_CONSTR;
    }

    public IConstr addAtLeast(IVecInt literals, IVecInt coeffs, int degree)
            throws ContradictionException {
        StringBuffer out = getOut();
        assert literals.size() == coeffs.size();
        this.nbOfConstraints++;
        for (int i = 0; i < literals.size(); i++) {
            out.append(coeffs.get(i));
            out.append(" x");
            out.append(literals.get(i));
            out.append(" ");
        }
        out.append(">= ");
        out.append(degree);
        out.append(" ;\n");
        return FAKE_CONSTR;
    }

    public IConstr addAtLeast(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger degree) throws ContradictionException {
        StringBuffer out = getOut();
        assert literals.size() == coeffs.size();
        this.nbOfConstraints++;
        for (int i = 0; i < literals.size(); i++) {
            out.append(coeffs.get(i));
            out.append(" x");
            out.append(literals.get(i));
            out.append(" ");
        }
        out.append(">= ");
        out.append(degree);
        out.append(" ;\n");
        return FAKE_CONSTR;

    }

    @Override
    public IConstr addExactly(IVecInt literals, int weight)
            throws ContradictionException {
        StringBuffer out = getOut();
        this.nbOfConstraints++;
        int negationweight = 0;
        int p;
        for (IteratorInt iterator = literals.iterator(); iterator.hasNext();) {
            p = iterator.next();
            assert p != 0;
            if (p > 0) {
                out.append("+1 x" + p + " ");
            } else {
                out.append("-1 x" + -p + " ");
                negationweight++;
            }
        }
        out.append("= " + (weight - negationweight) + " ;\n");
        return FAKE_CONSTR;
    }

    public IConstr addExactly(IVecInt literals, IVecInt coeffs, int weight)
            throws ContradictionException {
        StringBuffer out = getOut();
        assert literals.size() == coeffs.size();
        this.nbOfConstraints++;
        for (int i = 0; i < literals.size(); i++) {
            out.append(coeffs.get(i));
            out.append(" x");
            out.append(literals.get(i));
            out.append(" ");
        }
        out.append("= ");
        out.append(weight);
        out.append(" ;\n");
        return FAKE_CONSTR;
    }

    public IConstr addExactly(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger weight) throws ContradictionException {
        StringBuffer out = getOut();
        assert literals.size() == coeffs.size();
        this.nbOfConstraints++;
        for (int i = 0; i < literals.size(); i++) {
            out.append(coeffs.get(i));
            out.append(" x");
            out.append(literals.get(i));
            out.append(" ");
        }
        out.append("= ");
        out.append(weight);
        out.append(" ;\n");
        return FAKE_CONSTR;
    }

}
