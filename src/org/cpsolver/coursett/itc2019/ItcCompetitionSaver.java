package org.cpsolver.coursett.itc2019;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.cpsolver.coursett.TimetableSaver;
import org.cpsolver.coursett.constraint.FlexibleConstraint;
import org.cpsolver.coursett.constraint.GroupConstraint;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.Student;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.solver.Solver;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

/**
 * This class saves the solution of the Course Timetabling problem into the ITC 2019 solution format.
 * 
 * @author Tomas Muller
 */
public class ItcCompetitionSaver extends TimetableSaver {
	private static Logger sLog = Logger.getLogger(ItcCompetitionSaver.class);
    private File iOutputFolder = null;

    public ItcCompetitionSaver(Solver<Lecture, Placement> solver) {
        super(solver);
        iOutputFolder = new File(getModel().getProperties().getProperty("General.Output", "." + File.separator + "output"));

    }

    @Override
    public void save() throws Exception {
        save(null);
    }
    
    public Document saveDocument() {
        Document document = DocumentHelper.createDocument();

        Element root = document.addElement("solution");

        doSave(root);

        return document;
    }

    public void save(File outFile) throws Exception {
        if (outFile == null)
            outFile = new File(iOutputFolder, "solution.xml");
        outFile.getParentFile().mkdirs();

        Document document = DocumentHelper.createDocument();
        
        Element root = document.addElement("solution");

        doSave(root);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outFile);
            (new XMLWriter(fos, OutputFormat.createPrettyPrint())).write(document);
            fos.flush();
            fos.close();
            fos = null;
        } finally {
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
            }
        }
    }
    
    private String getDaysStr(int days) {
		StringBuffer buffer = new StringBuffer(Integer.toBinaryString(days));
		while (buffer.length() < 7) buffer.insert(0, "0");
		return buffer.toString();
	}
    
    protected void doSave(Element root) {
        root.addAttribute("name", getModel().getProperties().getProperty("Problem.Name"));
        root.addAttribute("runtime", String.valueOf(Math.round(getSolution().getBestTime())));
        root.addAttribute("cores", String.valueOf(getModel().getProperties().getPropertyInt("Parallel.NrSolvers", 1)));
        root.addAttribute("technique", "UniTime/Local Search");
        root.addAttribute("author", "UniTime Solver");
        root.addAttribute("institution", "UniTime");
        root.addAttribute("country", "Czechia");
        
        for (Lecture lecture: getModel().variables()) {
        	Element classEl = root.addElement("class");
        	classEl.addAttribute("id", String.valueOf(lecture.getClassId()));
        	Placement placement = getAssignment().getValue(lecture);
        	if (placement != null) {
        		classEl.addAttribute("days", getDaysStr(placement.getTimeLocation().getDayCode()));
        		classEl.addAttribute("start", String.valueOf(placement.getTimeLocation().getStartSlot()));
        		classEl.addAttribute("weeks", placement.getTimeLocation().getDatePatternName());
        		if (placement.getRoomLocation() != null)
        			classEl.addAttribute("room", String.valueOf(placement.getRoomLocation().getId()));
            	classEl.addComment(placement.getLongName(false));
        	}
        	for (Student student: lecture.students()) {
        		classEl.addElement("student").addAttribute("id", String.valueOf(student.getId()));
        	}
        }
        
        for (Constraint<Lecture, Placement> c: getModel().constraints()) {
        	if (c.isHard()) continue;
        	if (c instanceof GroupConstraint) {
        		GroupConstraint gc = (GroupConstraint)c;
        		int pref = gc.getCurrentPreference(getAssignment());
        		if (pref != 0)
        			sLog.info(gc + " has penalty " + pref);
        	} else if (c instanceof FlexibleConstraint) {
        		FlexibleConstraint fc = (FlexibleConstraint)c;
        		double viol = fc.getNrViolations(getAssignment(), null, null);
        		double pref = fc.getCurrentPreference(getAssignment(), null, null);
        		if (viol != 0 || pref != 0)
        			sLog.info(toString(fc) + " has penalty " + pref + " (violations " + viol + ")");
        	}
        }
    }
    
    public String toString(FlexibleConstraint fc) {
        StringBuffer sb = new StringBuffer();
        sb.append(fc.getReference());
        sb.append(" between ");
        for (Iterator<Lecture> e = fc.variables().iterator(); e.hasNext();) {
            Lecture v = e.next();
            sb.append(v.getName());
            if (e.hasNext())
                sb.append(", ");
        }
        return sb.toString();
    }
}