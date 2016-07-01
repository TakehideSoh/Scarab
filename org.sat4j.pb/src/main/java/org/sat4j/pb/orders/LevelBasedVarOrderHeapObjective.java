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
package org.sat4j.pb.orders;

import static org.sat4j.core.LiteralsUtils.neg;
import static org.sat4j.core.LiteralsUtils.var;

import java.math.BigInteger;

import org.sat4j.minisat.core.IPhaseSelectionStrategy;
import org.sat4j.minisat.orders.LevelBasedVarOrderHeap;
import org.sat4j.minisat.orders.PhaseInLastLearnedClauseSelectionStrategy;
import org.sat4j.pb.ObjectiveFunction;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

public class LevelBasedVarOrderHeapObjective extends LevelBasedVarOrderHeap
        implements IOrderObjective {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private ObjectiveFunction obj;

    public LevelBasedVarOrderHeapObjective() {
        this(new PhaseInLastLearnedClauseSelectionStrategy());
    }

    public LevelBasedVarOrderHeapObjective(IPhaseSelectionStrategy strategy) {
        super(strategy);
    }

    public void setObjectiveFunction(ObjectiveFunction obj) {
        this.obj = obj;
    }

    @Override
    public void init() {
        super.init();
        if (this.obj != null) {
            IVecInt vars = this.obj.getVars();
            IVec<BigInteger> coefs = this.obj.getCoeffs();
            for (int i = 0; i < vars.size(); i++) {
                int dimacsLiteral = vars.get(i);
                if (this.lits.belongsToPool(Math.abs(dimacsLiteral))) {
                    int p = this.lits.getFromPool(dimacsLiteral);
                    BigInteger c = coefs.get(i);
                    if (c.signum() < 0) {
                        p = neg(p);
                    }
                    int var = var(p);
                    this.activity[var] = c.bitLength() < Long.SIZE ? c.abs()
                            .longValue() : Long.MAX_VALUE;
                    if (this.heap.inHeap(var)) {
                        this.heap.increase(var);
                    } else {
                        this.heap.insert(var);
                    }
                    this.phaseStrategy.init(var, neg(p));
                }
            }
        }
    }

    @Override
    public String toString() {
        return super.toString() + " taking into account the objective function";
    }

}
