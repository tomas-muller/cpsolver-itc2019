package org.cpsolver.itc2019.coursett;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.constraint.MaxBlockFlexibleConstraint;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.assignment.Assignment;

/**
 * ITC 2019 penalization for the {@link MaxBlockFlexibleConstraint}
 * 
 * @author Tomas Muller
 */
public class ItcMaxBlockConstraint extends MaxBlockFlexibleConstraint {

	public ItcMaxBlockConstraint(Long id, String owner, String preference, String reference) {
		super(id, owner, preference, reference);
	}
	
	@Override
    public double getNrViolations(Assignment<Lecture, Placement> assignment, Set<Placement> conflicts, HashMap<Lecture, Placement> assignments) {
        List<BitSet> weeks = getWeeks();

        int penalty = 0;
        for (int dayCode : Constants.DAY_CODES) {
            for (BitSet week : weeks) {
                List<Block> blocks = getBlocks(assignment, dayCode, null, null, assignments, week);
                for (Block block : blocks) {
                    if (block.getNbrPlacements() == 1 || block.haveSameStartTime()) continue;
                    if (block.getLengthInSlots() > iMaxBlockSlotsBTB) {
                    	penalty ++;
                    }
                }
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
