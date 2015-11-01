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

import java.io.IOException;

import org.sat4j.AbstractLauncher;
import org.sat4j.ILauncherMode;
import org.sat4j.core.ASolverFactory;
import org.sat4j.pb.core.ObjectiveReducerPBSolverDecorator;
import org.sat4j.pb.reader.OPBReader2006;
import org.sat4j.pb.tools.SearchOptimizerListener;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.reader.Reader;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ILogAble;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;
import org.sat4j.tools.SolverDecorator;

/**
 * Launcher especially dedicated to the pseudo boolean 05 evaluation (@link
 * http://www.cril.univ-artois.fr/PB05/).
 * 
 * @author mederic
 */
public class LanceurPseudo2005 extends AbstractLauncher implements ILogAble {

    ASolverFactory<IPBSolver> factory;

    public LanceurPseudo2005() {
        this(SolverFactory.instance());
    }

    LanceurPseudo2005(ASolverFactory<IPBSolver> factory) {
        this.factory = factory;
        setLauncherMode(ILauncherMode.OPTIMIZATION);
    }

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    /**
     * Lance le prouveur sur un fichier Dimacs
     * 
     * @param args
     *            doit contenir le nom d'un fichier Dimacs, eventuellement
     *            compress?.
     */
    public static void main(final String[] args) {
        final AbstractLauncher lanceur = new LanceurPseudo2005();
        lanceur.run(args);
        System.exit(lanceur.getExitCode().value());
    }

    protected ObjectiveFunction obfct;

    /*
     * (non-Javadoc)
     * 
     * @see org.sat4j.Lanceur#createReader(org.sat4j.specs.ISolver)
     */
    @Override
    protected Reader createReader(ISolver theSolver, String problemname) {
        return new OPBReader2006((IPBSolver) theSolver);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.sat4j.Lanceur#configureSolver(java.lang.String[])
     */
    @Override
    protected ISolver configureSolver(String[] args) {
        IPBSolver theSolver;
        String solverName = args[0];
        boolean lower = false;
        if (solverName.startsWith("Lower")) {
            lower = true;
            solverName = solverName.substring("Lower".length());
        }
        if (args.length > 1) {
            theSolver = this.factory.createSolverByName(solverName);
        } else {
            theSolver = this.factory.defaultSolver();
        }
        if (System.getProperty("OBJREDUCER") != null) {
            if (lower) {
                theSolver = new ConstraintRelaxingPseudoOptDecorator(
                        new ObjectiveReducerPBSolverDecorator(theSolver));
            } else {
                theSolver = new PseudoOptDecorator(
                        new ObjectiveReducerPBSolverDecorator(theSolver));
            }
        } else if (System.getProperty("INTERNAL") != null) {
            theSolver.setSearchListener(new SearchOptimizerListener(
                    ILauncherMode.DECISION));
            setLauncherMode(ILauncherMode.DECISION);
        } else {
            if (lower) {
                theSolver = new ConstraintRelaxingPseudoOptDecorator(theSolver);
            } else {
                theSolver = new PseudoOptDecorator(theSolver);
            }
        }
        if (args.length == 3) {
            theSolver.setTimeout(Integer.valueOf(args[1]));
        }
        this.out.println(theSolver.toString(COMMENT_PREFIX));
        return theSolver;
    }

    @Override
    public void usage() {
        this.out.println("java -jar sat4j-pb.jar [solvername [timeout]] instancename.opb"); //$NON-NLS-1$
        showAvailableSolvers(SolverFactory.instance());
    }

    @Override
    protected String getInstanceName(String[] args) {
        assert args.length == 1 || args.length == 2 || args.length == 3;
        if (args.length == 0) {
            return null;
        }
        return args[args.length - 1];
    }

    @Override
    protected IProblem readProblem(String problemname)
            throws ParseFormatException, IOException, ContradictionException {
        IProblem problem = super.readProblem(problemname);
        ObjectiveFunction obj = null;
        if (super.feedWithDecorated) {
            SolverDecorator<IPBSolver> decorator = (SolverDecorator<IPBSolver>) problem;
            obj = (decorator.decorated()).getObjectiveFunction();
        } else {
            obj = ((IPBSolver) problem).getObjectiveFunction();
        }
        if (obj != null) {
            this.out.println(COMMENT_PREFIX + "objective function length is "
                    + obj.getVars().size() + " literals");
        }
        return problem;
    }
}
