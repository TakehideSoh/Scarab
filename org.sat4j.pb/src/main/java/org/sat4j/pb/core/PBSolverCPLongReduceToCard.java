package org.sat4j.pb.core;

import org.sat4j.minisat.core.IOrder;
import org.sat4j.minisat.core.LearningStrategy;
import org.sat4j.minisat.core.RestartStrategy;
import org.sat4j.minisat.core.SearchParams;
import org.sat4j.pb.constraints.pb.ConflictMapReduceToCard;
import org.sat4j.pb.constraints.pb.IConflict;
import org.sat4j.pb.constraints.pb.PBConstr;

@SuppressWarnings("serial")
public class PBSolverCPLongReduceToCard extends PBSolverCP {

    public PBSolverCPLongReduceToCard(
            LearningStrategy<PBDataStructureFactory> learner,
            PBDataStructureFactory dsf, IOrder order) {
        super(learner, dsf, order);
        // TODO Auto-generated constructor stub
    }

    public PBSolverCPLongReduceToCard(
            LearningStrategy<PBDataStructureFactory> learner,
            PBDataStructureFactory dsf, SearchParams params, IOrder order,
            RestartStrategy restarter) {
        super(learner, dsf, params, order, restarter);
        // TODO Auto-generated constructor stub
    }

    public PBSolverCPLongReduceToCard(
            LearningStrategy<PBDataStructureFactory> learner,
            PBDataStructureFactory dsf, SearchParams params, IOrder order) {
        super(learner, dsf, params, order);
        // TODO Auto-generated constructor stub
    }

    public PBSolverCPLongReduceToCard(
            LearningStrategy<PBDataStructureFactory> learner,
            PBDataStructureFactory dsf, IOrder order, boolean noRemove) {
        super(learner, dsf, order, noRemove);
        // TODO Auto-generated constructor stub
    }

    public PBSolverCPLongReduceToCard(
            LearningStrategy<PBDataStructureFactory> learner,
            PBDataStructureFactory dsf, SearchParams params, IOrder order,
            RestartStrategy restarter, boolean noRemove) {
        super(learner, dsf, params, order, restarter, noRemove);
        // TODO Auto-generated constructor stub
    }

    public PBSolverCPLongReduceToCard(
            LearningStrategy<PBDataStructureFactory> learner,
            PBDataStructureFactory dsf, SearchParams params, IOrder order,
            boolean noRemove) {
        super(learner, dsf, params, order, noRemove);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected IConflict chooseConflict(PBConstr myconfl, int level) {
        return ConflictMapReduceToCard.createConflict(myconfl, level, noRemove);
    }

    @Override
    public String toString(String prefix) {
        return super.toString(prefix)
                + "\n"
                + prefix
                + "Reduce coefficients of asserted PB constraints from BigInteger to Long and reduce to cardinality constraint during conflict analysis if necessary";
    }

}
