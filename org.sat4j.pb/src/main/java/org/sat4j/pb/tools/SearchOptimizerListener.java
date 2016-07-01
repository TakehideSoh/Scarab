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

import org.sat4j.ILauncherMode;
import org.sat4j.pb.IPBSolverService;
import org.sat4j.pb.ObjectiveFunction;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.Lbool;
import org.sat4j.specs.RandomAccessModel;
import org.sat4j.tools.SearchListenerAdapter;
import org.sat4j.tools.SolutionFoundListener;

public final class SearchOptimizerListener extends
        SearchListenerAdapter<IPBSolverService> {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private IPBSolverService solverService;

    private ObjectiveFunction obj;

    private final SolutionFoundListener sfl;

    private BigInteger currentValue;

    private IConstr prevConstr = null;

    public SearchOptimizerListener(SolutionFoundListener sfl) {
        this.sfl = sfl;
    }

    @Override
    public void init(IPBSolverService solverService) {
        this.obj = solverService.getObjectiveFunction();
        this.solverService = solverService;
        this.currentValue = null;
        this.prevConstr = null;
    }

    @Override
    public void solutionFound(int[] model, RandomAccessModel lazyModel) {
        if (obj != null) {
            this.currentValue = obj.calculateDegree(lazyModel);
            System.out.println(ILauncherMode.CURRENT_OPTIMUM_VALUE_PREFIX
                    + this.currentValue.add(obj.getCorrection()));
            if (this.prevConstr != null) {
                this.solverService.removeSubsumedConstr(prevConstr);
            }
            this.prevConstr = this.solverService.addAtMostOnTheFly(
                    obj.getVars(), obj.getCoeffs(),
                    this.currentValue.subtract(BigInteger.ONE));
        }
        sfl.onSolutionFound(model);
    }

    @Override
    public void end(Lbool result) {
        if (result == Lbool.FALSE) {
            sfl.onUnsatTermination();
            System.out.println(solverService.getLogPrefix()
                    + "objective function=" + currentValue);
        }
    }

    @Override
    public String toString() {
        return "Internal optimizer search listener";
    }
}
