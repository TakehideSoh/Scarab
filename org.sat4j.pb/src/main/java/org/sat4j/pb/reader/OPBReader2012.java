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
import java.io.LineNumberReader;

import org.sat4j.pb.PBSolverHandle;
import org.sat4j.pb.tools.LexicoDecoratorPB;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;

/**
 * OPBReader allowing to read several objective functions in an OPB file.
 * 
 * @since 2.3.3
 */
public class OPBReader2012 extends OPBReader2010 {

    private LexicoDecoratorPB lexico = null;

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    public OPBReader2012(PBSolverHandle solver) {
        super(solver);
    }

    @Override
    protected void readMetaData() throws IOException, ParseFormatException {
        super.readMetaData();
        skipSpaces();
        char c = get();
        if (c != '*') {
            // no aggregation line
            putback(c);
            return;
        }
        skipSpaces();
        String s = readWord();
        if ("#aggregation=".equals(s)) {
            s = readWord();
            assert "lexico".equals(s);
            PBSolverHandle handle = (PBSolverHandle) solver;
            lexico = new LexicoDecoratorPB(handle.decorated());
            handle.changeDecorated(lexico);
        }
        if ("beginMapping".equals(s)) {
            startsMapping();
            get();
        } else {
            if (savedChar != '\n') {
                this.in.readLine();
            }
            get(); // remove trailing \n
        }
    }

    @Override
    protected void readObjective() throws IOException, ParseFormatException {
        super.readObjective();
        if (lexico != null && !lits.isEmpty()) {
            lexico.addCriterion(lits.clone(), coeffs.clone());
            lits.clear();
            coeffs.clear();
            readObjective();
        }
    }

    @Override
    public IProblem parseInstance(final java.io.Reader input)
            throws ParseFormatException, ContradictionException {
        IProblem problem = parseInstance(new LineNumberReader(input));
        if (lexico == null) {
            this.solver.setObjectiveFunction(getObjectiveFunction());
        }
        return ((PBSolverHandle) solver).decorated();
    }
}
