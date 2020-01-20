package org.cpsolver.coursett.itc2019;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.constraint.MaxBreaksFlexibleConstraint;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.assignment.Assignment;

/**
 * ITC 2019 penalization for the {@link MaxBreaksFlexibleConstraint}
 * 
 * @author Tomas Muller
 */
public class ItcMaxBreaksConstraint extends MaxBreaksFlexibleConstraint {

	public ItcMaxBreaksConstraint(Long id, String owner, String preference, String reference) {
		super(id, owner, preference, reference);
	}
	
	@Override
    public double getNrViolations(Assignment<Lecture, Placement> assignment, Set<Placement> conflicts, HashMap<Lecture, Placement> assignments) {
        int penalty = 0;
        // constraint is checked for every day in week
        for (int dayCode : Constants.DAY_CODES) {
            // constraint is checked for every week in semester (or for the whole semester)
            for (BitSet week : getWeeks()) {
                // each blocks contains placements which are BTB
                List<Block> blocks = getBlocks(assignment, dayCode, null, null, assignments, week);
                // too many blocks -> increase penalty
                if (blocks.size() > iMaxBlocksOnADay)
                	penalty += (blocks.size() - iMaxBlocksOnADay);
            }
        }
        return penalty;
    }
	
	public double getCurrentPreference(Assignment<Lecture, Placement> assignment, Set<Placement> conflicts, HashMap<Lecture, Placement> assignments){
        if (isHard()) return 0;
        int violations = (int) getNrViolations(assignment, conflicts, assignments);
        if (violations == 0) return 0;
        return Math.abs(iPreference) * violations / ((TimetableModel)getModel()).getWeeks().size();
    }

}
