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

import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.ObjectiveFunction;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

public class SteppedTimeoutLexicoDecoratorPB extends LexicoDecoratorPB {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    public SteppedTimeoutLexicoDecoratorPB(IPBSolver solver) {
        super(solver);
    }

    @Override
    public boolean admitABetterSolution(IVecInt assumps)
            throws TimeoutException {
        decorated().setObjectiveFunction(this.objs.get(this.currentCriterion));
        this.isSolutionOptimal = false;
        try {
            if (decorated().isSatisfiable(assumps, true)) {
                this.prevboolmodel = new boolean[nVars()];
                for (int i = 0; i < nVars(); i++) {
                    this.prevboolmodel[i] = decorated().model(i + 1);
                }
                this.prevfullmodel = decorated().model();
                this.prevmodelwithinternalvars = decorated()
                        .modelWithInternalVariables();
                calculateObjective();
                return true;
            }
            decorated().expireTimeout();
            return manageUnsatCase();
        } catch (TimeoutException te) {
            if (this.currentCriterion == numberOfCriteria() - 1) {
                throw te;
            }
            mergeCurrentandNextCriteria();
            if (this.prevConstr != null) {
                super.removeConstr(this.prevConstr);
                this.prevConstr = null;
            }
            try {
                fixCriterionValue();
            } catch (ContradictionException ce) {
                throw new IllegalStateException(ce);
            }
            if (isVerbose()) {
                System.out.println(getLogPrefix()
                        + "Partial timeout criterion number "
                        + (this.currentCriterion + 1));
            }
            this.currentCriterion++;
            calculateObjective();
            decorated().expireTimeout();
            return true;
        }
    }

    private void mergeCurrentandNextCriteria() {
        ObjectiveFunction currentObj = this.objs.get(this.currentCriterion);
        int currentObjSize = currentObj.getVars().size();
        ObjectiveFunction nextObj = this.objs.get(this.currentCriterion + 1);
        int nextObjSize = nextObj.getVars().size();
        IVecInt newLits = new VecInt(currentObjSize + nextObjSize);
        currentObj.getVars().copyTo(newLits);
        nextObj.getVars().copyTo(newLits);
        IVec<BigInteger> newCoeffs = new Vec<BigInteger>(currentObjSize
                + nextObjSize);
        BigInteger coeffFactor = BigInteger.valueOf(nextObjSize).add(
                BigInteger.ONE);
        for (Iterator<BigInteger> it = currentObj.getCoeffs().iterator(); it
                .hasNext();) {
            newCoeffs.push(it.next().multiply(coeffFactor));
        }
        nextObj.getCoeffs().copyTo(newCoeffs);
        this.objs.set(this.currentCriterion + 1, new ObjectiveFunction(newLits,
                newCoeffs));
    }

}
