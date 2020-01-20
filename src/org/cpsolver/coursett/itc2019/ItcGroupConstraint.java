package org.cpsolver.coursett.itc2019;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Set;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.constraint.GroupConstraint;
import org.cpsolver.coursett.criteria.DistributionPreferences;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.assignment.Assignment;

/**
 * ITC 2019 penalization for the {@link GroupConstraint}
 * 
 * @author Tomas Muller
 */
public class ItcGroupConstraint extends GroupConstraint {
	
	public ItcGroupConstraint(Long id, ConstraintTypeInterface type, String preference) {
		super(id, type, preference);
	}
	
	@Override
	public int getCurrentPreference(Assignment<Lecture, Placement> assignment) {
        if (isHard()) return 0; // no preference
        if (countAssignedVariables(assignment) < 2) return 0; // not enough variable
        if (getType().is(Flag.MAX_HRS_DAY)) { // max hours a day
            int over = 0;
            for (int dayCode: Constants.DAY_CODES) {
                for (BitSet week: ((TimetableModel)getModel()).getWeeks())
                    over += Math.max(0, nrSlotsADay(assignment, dayCode, week, null, null) - getType().getMax());
            }
            return (over > 0 ? Math.abs(getPreference()) * over / ((TimetableModel)getModel()).getWeeks().size() : 0);
        }
        int nrViolatedPairs = 0;
        for (Lecture v1 : variables()) {
            Placement p1 = assignment.getValue(v1);
            if (p1 == null) continue;
            for (Lecture v2 : variables()) {
                Placement p2 = assignment.getValue(v2);
                if (p2 == null || v1.getId() >= v2.getId()) continue;
                if (!isSatisfiedPair(assignment, p1, p2)) nrViolatedPairs++;
            }
        }
        if (getType().is(Flag.BACK_TO_BACK)) {
        	// No back-to-backs in the competition instance >> do not care
        	return super.getCurrentPreference(assignment);
        }
        return (nrViolatedPairs > 0 ? Math.abs(getPreference()) * nrViolatedPairs : 0);
    }
	
	@Override
	public int getCurrentPreference(Assignment<Lecture, Placement> assignment, Placement placement) {
        if (isHard()) return 0; // no preference
        if (countAssignedVariables(assignment) + (assignment.getValue(placement.variable()) == null ? 1 : 0) < 2) return 0; // not enough variable
        if (getType().is(Flag.MAX_HRS_DAY)) {
            HashMap<Lecture, Placement> assignments = new HashMap<Lecture, Placement>();
            assignments.put(placement.variable(), placement);
            HashMap<Lecture, Placement> unassignments = new HashMap<Lecture, Placement>();
            unassignments.put(placement.variable(), null);
            int after = 0;
            int before = 0;
            for (int dayCode: Constants.DAY_CODES) {
                for (BitSet week: ((TimetableModel)getModel()).getWeeks()) {
                    after += Math.max(0, nrSlotsADay(assignment, dayCode, week, assignments, null) - getType().getMax());
                    before += Math.max(0, nrSlotsADay(assignment, dayCode, week, unassignments, null) - getType().getMax());
                }
            }
            int weeks = ((TimetableModel)getModel()).getWeeks().size();
            return (after > 0 ? Math.abs(getPreference()) * after / weeks : - Math.abs(getPreference())) - (before > 0 ? Math.abs(getPreference()) * before / weeks : - Math.abs(getPreference()));
        }
        
        int nrViolatedPairsAfter = 0;
        int nrViolatedPairsBefore = 0;
        for (Lecture v1 : variables()) {
            for (Lecture v2 : variables()) {
                if (v1.getId() >= v2.getId()) continue;
                Placement p1 = (v1.equals(placement.variable()) ? null : assignment.getValue(v1));
                Placement p2 = (v2.equals(placement.variable()) ? null : assignment.getValue(v2));
                if (p1 != null && p2 != null && !isSatisfiedPair(assignment, p1, p2))
                    nrViolatedPairsBefore ++;
                if (v1.equals(placement.variable())) p1 = placement;
                if (v2.equals(placement.variable())) p2 = placement;
                if (p1 != null && p2 != null && !isSatisfiedPair(assignment, p1, p2))
                    nrViolatedPairsAfter ++;
            }
        }
        
        if (getType().is(Flag.BACK_TO_BACK)) {
        	// No back-to-backs in the competition instance >> do not care
        	return super.getCurrentPreference(assignment, placement);
        }
        
        return (nrViolatedPairsAfter > 0 ? Math.abs(getPreference()) * nrViolatedPairsAfter : 0) -
                (nrViolatedPairsBefore > 0 ? Math.abs(getPreference()) * nrViolatedPairsBefore : 0);
    }
	
    @Override
    public GroupConstraintContext createAssignmentContext(Assignment<Lecture, Placement> assignment) {
        return new ItcGroupConstraintContext(assignment);
    }
	
	public class ItcGroupConstraintContext extends GroupConstraint.GroupConstraintContext {
        public ItcGroupConstraintContext(Assignment<Lecture, Placement> assignment) {
            super(assignment);
        }

        @Override
        protected void updateCriterion(Assignment<Lecture, Placement> assignment) {
            if (!isHard()) {
                getModel().getCriterion(DistributionPreferences.class).inc(assignment, -iLastPreference);
                iLastPreference = getCurrentPreference(assignment);
                getModel().getCriterion(DistributionPreferences.class).inc(assignment, iLastPreference);
            }
        }
    }
	
	@Override
	protected int nrSlotsADay(Assignment<Lecture, Placement> assignment, int dayCode, BitSet week, HashMap<Lecture, Placement> assignments, Set<Placement> conflicts) {
        int slots = 0;
        for (Lecture lecture: variables()) {
            Placement placement = null;
            if (assignments != null && assignments.containsKey(lecture))
                placement = assignments.get(lecture);
            else if (assignment != null)
                placement = assignment.getValue(lecture);
            if (placement == null || placement.getTimeLocation() == null) continue;
            if (conflicts != null && conflicts.contains(placement)) continue;
            TimeLocation t = placement.getTimeLocation();
            if (t == null || (t.getDayCode() & dayCode) == 0 || (week != null && !t.shareWeeks(week))) continue;
            slots += t.getLength();
        }
        return slots;
    }
}
