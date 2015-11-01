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

import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.PBSolverHandle;
import org.sat4j.pb.PseudoOptDecorator;
import org.sat4j.reader.InstanceReader;
import org.sat4j.reader.Reader;

/**
 * An reader having the responsibility to choose the right reader according to
 * the input.
 * 
 * @author leberre
 */
public class PBInstanceReader extends InstanceReader {

    private OPBReader2012 opb;

    private final IPBSolver solver;

    public PBInstanceReader(IPBSolver solver) {
        super(solver);
        this.solver = solver;
    }

    private Reader getDefaultOPBReader() {
        if (this.opb == null) {
            this.opb = new OPBReader2012(new PBSolverHandle(
                    new PseudoOptDecorator(solver)));
        }
        return this.opb;
    }

    public boolean hasObjectiveFunction() {
        return this.opb.hasObjFunc;
    }

    @Override
    protected Reader handleFileName(String fname, String prefix) {
        if (fname.endsWith(".opb") || "PB".equals(prefix)) {
            return getDefaultOPBReader();
        }
        return super.handleFileName(fname, prefix);
    }
}
