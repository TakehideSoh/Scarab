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

package org.sat4j.pb.tools;

import java.math.BigInteger;
import java.util.Iterator;

import org.sat4j.core.ConstrGroup;
import org.sat4j.core.VecInt;
import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.ObjectiveFunction;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.IteratorInt;
import org.sat4j.tools.xplain.Xplain;

public class XplainPB extends Xplain<IPBSolver> implements IPBSolver {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    public XplainPB(IPBSolver solver) {
        super(solver);
    }

    @Override
    public IConstr addAtLeast(IVecInt literals, int degree)
            throws ContradictionException {
        IVecInt coeffs = new VecInt(literals.size(), 1);
        int newvar = createNewVar(literals);
        literals.push(newvar);
        coeffs.push(degree);
        IConstr constr = decorated().addAtLeast(literals, coeffs, degree);
        if (constr == null) {
            // constraint trivially satisfied
            discardLastestVar();
        } else {
            getConstrs().put(newvar, constr);
        }
        return constr;
    }

    @Override
    public IConstr addAtMost(IVecInt literals, int degree)
            throws ContradictionException {
        IVecInt coeffs = new VecInt(literals.size(), 1);
        int newvar = createNewVar(literals);
        literals.push(newvar);
        coeffs.push(degree - coeffs.size());
        IConstr constr = decorated().addAtMost(literals, coeffs, degree);
        if (constr == null) {
            // constraint trivially satisfied
            discardLastestVar();
        } else {
            getConstrs().put(newvar, constr);
        }
        return constr;
    }

    @Override
    public IConstr addExactly(IVecInt literals, int n)
            throws ContradictionException {
        int newvar = createNewVar(literals);

        // at most
        IVecInt coeffs = new VecInt(literals.size(), 1);
        literals.push(newvar);
        coeffs.push(n - coeffs.size());
        IConstr constr1 = decorated().addAtMost(literals, coeffs, n);
        // at least
        coeffs.pop();
        coeffs.push(n);
        IConstr constr2 = decorated().addAtLeast(literals, coeffs, n);
        if (constr1 == null && constr2 == null) {
            discardLastestVar();
            return null;
        }
        ConstrGroup group = new ConstrGroup();
        group.add(constr1);
        group.add(constr2);
        getConstrs().put(newvar, group);
        return group;
    }

    public IConstr addPseudoBoolean(IVecInt lits, IVec<BigInteger> coeffs,
            boolean moreThan, BigInteger d) throws ContradictionException {
        int newvar = createNewVar(lits);
        lits.push(newvar);
        if (moreThan && d.signum() >= 0) {
            coeffs.push(d);
        } else {
            BigInteger sum = BigInteger.ZERO;
            for (Iterator<BigInteger> ite = coeffs.iterator(); ite.hasNext();) {
                sum = sum.add(ite.next());
            }
            sum = sum.subtract(d);
            coeffs.push(sum.negate());
        }
        IConstr constr = decorated()
                .addPseudoBoolean(lits, coeffs, moreThan, d);
        if (constr == null) {
            // constraint trivially satisfied
            discardLastestVar();
            // System.err.println(lits.toString()+"/"+coeffs+"/"+(moreThan?">=":"<=")+d);
        } else {
            getConstrs().put(newvar, constr);
        }
        return constr;
    }

    private IConstr addPseudoBoolean(IVecInt lits, IVecInt coeffs,
            boolean moreThan, int d) throws ContradictionException {
        int newvar = createNewVar(lits);
        lits.push(newvar);
        if (moreThan && d >= 0) {
            coeffs.push(d);
        } else {
            int sum = 0;
            for (IteratorInt ite = coeffs.iterator(); ite.hasNext();) {
                sum += ite.next();
            }
            sum = sum - d;
            coeffs.push(-sum);
        }
        IConstr constr = moreThan ? decorated().addAtLeast(lits, coeffs, d)
                : decorated().addAtMost(lits, coeffs, d);
        if (constr == null) {
            // constraint trivially satisfied
            discardLastestVar();
            // System.err.println(lits.toString()+"/"+coeffs+"/"+(moreThan?">=":"<=")+d);
        } else {
            getConstrs().put(newvar, constr);
        }
        return constr;
    }

    public void setObjectiveFunction(ObjectiveFunction obj) {
        decorated().setObjectiveFunction(obj);
    }

    public ObjectiveFunction getObjectiveFunction() {
        return decorated().getObjectiveFunction();
    }

    public IConstr addAtMost(IVecInt literals, IVecInt coeffs, int degree)
            throws ContradictionException {
        return addPseudoBoolean(literals, coeffs, false, degree);
    }

    public IConstr addAtMost(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger degree) throws ContradictionException {
        return addPseudoBoolean(literals, coeffs, false, degree);
    }

    public IConstr addAtLeast(IVecInt literals, IVecInt coeffs, int degree)
            throws ContradictionException {
        return addPseudoBoolean(literals, coeffs, true, degree);
    }

    public IConstr addAtLeast(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger degree) throws ContradictionException {
        return addPseudoBoolean(literals, coeffs, true, degree);
    }

    public IConstr addExactly(IVecInt literals, IVecInt coeffs, int weight)
            throws ContradictionException {
        throw new UnsupportedOperationException();
    }

    public IConstr addExactly(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger weight) throws ContradictionException {
        throw new UnsupportedOperationException();
    }
}
