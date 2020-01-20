package org.cpsolver.itc2019.coursett;

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.TimetableLoader;
import org.cpsolver.coursett.constraint.GroupConstraint;
import org.cpsolver.coursett.constraint.JenrlConstraint;
import org.cpsolver.coursett.constraint.RoomConstraint;
import org.cpsolver.coursett.model.Configuration;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.RoomLocation;
import org.cpsolver.coursett.model.Student;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.Progress;
import org.cpsolver.ifs.util.ToolBox;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 * This class loads the ITC 2019 competition problem as the Course Timetabling problem.
 * 
 * @author Tomas Muller
 */
public class ItcCompetitionLoader extends TimetableLoader {
    private File iInputFile;
    private Progress iProgress = null;
    
	public ItcCompetitionLoader(TimetableModel model, Assignment<Lecture, Placement> assignment) {
		super(model, assignment);
		getModel().getProperties().setProperty("FlexibleConstraint.CheckWeeks", "true");
        iProgress = Progress.getInstance(getModel());
        iInputFile = new File(getModel().getProperties().getProperty("General.Input", "." + File.separator + "solution.xml"));
	}
	
	static {
    	Constants.sPreferenceLevelProhibited = 1000;
    	Constants.sPreferenceLevelRequired = -1000;
	}

    private Solver<Lecture, Placement> iSolver = null;

    public void setSolver(Solver<Lecture, Placement> solver) {
        iSolver = solver;
    }

    public Solver<Lecture, Placement> getSolver() {
        return iSolver;
    }
    
    public void setInputFile(File inputFile) {
        iInputFile = inputFile;
    }

    @Override
    public void load() throws Exception {
    	load(new SAXReader().read(iInputFile));
    	
    }
    
    protected static BitSet toWeekCode(String weeks) {
    	BitSet ret = new BitSet();
    	for (int i = 0; i < weeks.length(); i++)
    		if (weeks.charAt(i) == '1')
    			for (int d = 0; d < 7; d++)
    				ret.set(7 * i + d);
    	return ret;
    }
    
    protected static String toPreference(boolean required, int penalty, boolean positive) {
    	if (positive) {
    		return (required ? "R" : penalty == 0 ? "0" : penalty <= 2 ? "-1" : "-2");
    	} else {
    		return (required ? "P" : penalty == 0 ? "0" : penalty <= 2 ? "1" : "2");
    	}
    }
    
    protected static String bitset2string(BitSet b) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < b.length(); i++)
            sb.append(b.get(i) ? "1" : "0");
        return sb.toString();
    }
    
    public void load(Document document) throws Exception {
        Element root = document.getRootElement();
    	getModel().getProperties().setProperty("Problem.Name", root.attributeValue("name"));
    	getModel().getProperties().setProperty("Problem.NrWeeks", root.attributeValue("nrWeeks", "13"));
		int nrWeeks = Integer.parseInt(root.attributeValue("nrWeeks", "13"));
		String allWeeks = "";
		for (int i = 0; i < nrWeeks; i++) allWeeks += "1";
		getModel().getProperties().setProperty("DatePattern.Default", bitset2string(toWeekCode(allWeeks)));
		
		Element optimizationEl = root.element("optimization");
		if (optimizationEl == null) optimizationEl = root.addElement("optimization");
		getModel().getProperties().setProperty("Comparator.TimePreferenceWeight", optimizationEl.attributeValue("time", "2"));
		getModel().getProperties().setProperty("Placement.DeltaTimePreferenceWeight1", optimizationEl.attributeValue("time", "2"));
		getModel().getProperties().setProperty("Placement.TimePreferenceWeight2", optimizationEl.attributeValue("time", "2"));
		getModel().getProperties().setProperty("Comparator.RoomPreferenceWeight", optimizationEl.attributeValue("room", "1"));
		getModel().getProperties().setProperty("Placement.RoomPreferenceWeight1", optimizationEl.attributeValue("room", "1"));
		getModel().getProperties().setProperty("Placement.RoomPreferenceWeight2", optimizationEl.attributeValue("room", "1"));
		getModel().getProperties().setProperty("Comparator.ContrPreferenceWeight", optimizationEl.attributeValue("distribution", "10"));
		getModel().getProperties().setProperty("Placement.ConstrPreferenceWeight1", optimizationEl.attributeValue("distribution", "10"));
		getModel().getProperties().setProperty("Placement.ConstrPreferenceWeight2", optimizationEl.attributeValue("distribution", "10"));
		getModel().getProperties().setProperty("Placement.FlexibleConstrPreferenceWeight1", optimizationEl.attributeValue("distribution", "10"));
		getModel().getProperties().setProperty("Placement.FlexibleConstrPreferenceWeight2", optimizationEl.attributeValue("distribution", "10"));
		getModel().getProperties().setProperty("FlexibleConstraint.Weight", optimizationEl.attributeValue("distribution", "10"));
		double hardConflictFraction = getModel().getProperties().getPropertyDouble("ITC2019.HardStudentConflicts", 0.8);
		double softConflictFraction = 1.0 - hardConflictFraction;
		getModel().getProperties().setProperty("Comparator.StudentConflictWeight", String.valueOf(softConflictFraction*Integer.parseInt(optimizationEl.attributeValue("student", "5"))));
		getModel().getProperties().setProperty("Comparator.DistStudentConflictWeight", String.valueOf(softConflictFraction*Integer.parseInt(optimizationEl.attributeValue("student", "5"))));
		getModel().getProperties().setProperty("Comparator.HardStudentConflictWeight", String.valueOf(hardConflictFraction*Integer.parseInt(optimizationEl.attributeValue("student", "5"))));
		getModel().getProperties().setProperty("Placement.NrHardStudConfsWeight1", String.valueOf(hardConflictFraction*Integer.parseInt(optimizationEl.attributeValue("student", "5"))));
		getModel().getProperties().setProperty("Placement.NrStudConfsWeight1", String.valueOf(softConflictFraction*Integer.parseInt(optimizationEl.attributeValue("student", "5"))));
		getModel().getProperties().setProperty("Placement.NrDistStudConfsWeight1", String.valueOf(softConflictFraction*Integer.parseInt(optimizationEl.attributeValue("student", "5"))));
		getModel().getProperties().setProperty("Placement.NrHardStudConfsWeight2", String.valueOf(hardConflictFraction*Integer.parseInt(optimizationEl.attributeValue("student", "5"))));
		getModel().getProperties().setProperty("Placement.NrStudConfsWeight2", String.valueOf(softConflictFraction*Integer.parseInt(optimizationEl.attributeValue("student", "5"))));
		getModel().getProperties().setProperty("Placement.NrDistStudConfsWeight2", String.valueOf(softConflictFraction*Integer.parseInt(optimizationEl.attributeValue("student", "5"))));
		for (Criterion<Lecture, Placement> c: getModel().getCriteria())
			c.configure(getModel().getProperties());
		
		Map<Long, RoomConstraint> roomConstraints = new HashMap<Long, RoomConstraint>();
		long unAvId = 0l;
		for (Iterator<?> i = root.element("rooms").elementIterator("room"); i.hasNext(); ) {
			Element roomEl = (Element)i.next();
			RoomConstraint room = new RoomConstraint(
					Long.valueOf(roomEl.attributeValue("id")), // room id
					"R" + roomEl.attributeValue("id"), // room name
					Long.valueOf(roomEl.attributeValue("id")), // building id
					Integer.valueOf(roomEl.attributeValue("capacity")), // size
					null, // sharing model
					0d, 0d, // coordinates
					false, // ignore too far
					true);
			
			for (Iterator<?> j = roomEl.elementIterator("travel"); j.hasNext(); ) {
				Element travelEl = (Element)j.next();
				getModel().getDistanceMetric().addTravelTime(room.getResourceId(),
                        Long.valueOf(travelEl.attributeValue("room")),
                        5 * Integer.valueOf(travelEl.attributeValue("value")));
			}
			
			for (Iterator<?> j = roomEl.elementIterator("unavailable"); j.hasNext(); ) {
				Element unavailableEl = (Element)j.next();
				TimeLocation time = new TimeLocation(
						Integer.parseInt(unavailableEl.attributeValue("days"), 2), // days
						Integer.valueOf(unavailableEl.attributeValue("start")), // start
						Integer.valueOf(unavailableEl.attributeValue("length")), // length
						0, 0f, 0, // preferences
						Long.valueOf(allWeeks,2), allWeeks, toWeekCode(unavailableEl.attributeValue("weeks",allWeeks)), 0);
				List<RoomLocation> rooms = new ArrayList<RoomLocation>();
				rooms.add(new RoomLocation(room.getResourceId(), room.getName(), room.getBuildingId(), 0, room.getCapacity(), room.getPosX(), room.getPosY(), room.getIgnoreTooFar(), room));
				List<TimeLocation> times = new ArrayList<TimeLocation>(); times.add(time);
				Lecture l = new Lecture(--unAvId, 0l, null, null, times, rooms, 1, null, 0, 0, 0d);
				Placement unavailability = new Placement(l, time, rooms);
				room.setNotAvailable(unavailability);
			}
			
			getModel().addConstraint(room);
			roomConstraints.put(room.getResourceId(), room);
		}
		
		Map<Long, Lecture> lectures = new HashMap<Long, Lecture>();
		HashMap<Lecture, Long> parents = new HashMap<Lecture, Long>();
        HashMap<Long, List<Configuration>> configurations = new HashMap<Long, List<Configuration>>();
        
		for (Iterator<?> i = root.element("courses").elementIterator("course"); i.hasNext(); ) {
			Element courseEl = (Element)i.next();
			Long courseId = Long.valueOf(courseEl.attributeValue("id"));
			List<Configuration> altConfigs = new ArrayList<Configuration>();
			configurations.put(courseId, altConfigs);
			
			for (Iterator<?> j = courseEl.elementIterator("config"); j.hasNext(); ) {
				Element configEl = (Element)j.next();
				Long configId = Long.valueOf(configEl.attributeValue("id"));
				int configLimit = -1;
				for (Iterator<?> k = configEl.elementIterator("subpart"); k.hasNext(); ) {
					Element subpartEl = (Element)k.next();
					int limit = 0;
					for (Iterator<?> l = subpartEl.elementIterator("class"); l.hasNext(); ) {
						Element classEl = (Element)l.next();
						limit += Integer.valueOf(classEl.attributeValue("limit"));
					}
					if (configLimit < 0 || configLimit > limit)
						configLimit = limit;
				}
				Configuration config = new Configuration(courseId, configId, configLimit);
				config.setAltConfigurations(altConfigs);
				altConfigs.add(config);
				
				for (Iterator<?> k = configEl.elementIterator("subpart"); k.hasNext(); ) {
					Element subpartEl = (Element)k.next();
					Long subpartId = Long.valueOf(subpartEl.attributeValue("id"));
					List<Lecture> sameSubpartLectures = new ArrayList<Lecture>();
					
					for (Iterator<?> l = subpartEl.elementIterator("class"); l.hasNext(); ) {
						Element classEl = (Element)l.next();
						List<TimeLocation> times = new ArrayList<TimeLocation>();
						for (Iterator<?> m = classEl.elementIterator("time"); m.hasNext(); ) {
							Element timeEl = (Element)m.next();
							TimeLocation time = new TimeLocation(
									Integer.parseInt(timeEl.attributeValue("days"), 2), //days
									Integer.valueOf(timeEl.attributeValue("start")), //start
									Integer.valueOf(timeEl.attributeValue("length")), // length
									Integer.valueOf(timeEl.attributeValue("penalty", "0")), // preference
									Double.valueOf(timeEl.attributeValue("penalty", "0")), // norm. preference
									0, // date pattern preference
									Long.valueOf(timeEl.attributeValue("weeks", allWeeks), 2), // dp id
									timeEl.attributeValue("weeks", allWeeks), // dp name
									toWeekCode(timeEl.attributeValue("weeks", allWeeks)), // date pattern
									0);
							times.add(time);
						}
						List<RoomLocation> rooms = new ArrayList<RoomLocation>();
						for (Iterator<?> m = classEl.elementIterator("room"); m.hasNext(); ) {
							Element roomEl = (Element)m.next();
							RoomConstraint rc = roomConstraints.get(Long.valueOf(roomEl.attributeValue("id")));
							if (rc == null) {
								iProgress.warn("Room " + roomEl.attributeValue("id") + " is not defined.");
								continue;
							}
							RoomLocation room = new RoomLocation(
									rc.getResourceId(), rc.getName(), rc.getBuildingId(),
									Integer.valueOf(roomEl.attributeValue("penalty", "0")),
									rc.getCapacity(), rc.getPosX(), rc.getPosY(), rc.getIgnoreTooFar(), rc);
							rooms.add(room);
						}
						Lecture lecture = new Lecture(
								Long.valueOf(classEl.attributeValue("id")), // class id 
								0l, // solver group id
								subpartId, // subpart id
								"C" + classEl.attributeValue("id"), // name
								times, rooms,
								("true".equalsIgnoreCase(classEl.attributeValue("room", classEl.attributeValue("rooms", "true"))) ? 1 : 0),
								null, Integer.valueOf(classEl.attributeValue("limit")), Integer.valueOf(classEl.attributeValue("limit")), 1.0);
						lecture.setSameSubpartLectures(sameSubpartLectures);
						sameSubpartLectures.add(lecture);
						lectures.put(lecture.getClassId(), lecture);
						
						getModel().addVariable(lecture);
						if (classEl.attributeValue("parent") != null) {
							parents.put(lecture, Long.valueOf(classEl.attributeValue("parent")));
						} else {
							config.addTopLecture(lecture);
							lecture.setConfiguration(config);
						}
						for (RoomLocation r: rooms)
							r.getRoomConstraint().addVariable(lecture);
					}
				}
			}
		}
		
		for (Map.Entry<Lecture, Long> entry : parents.entrySet()) {
            Lecture lecture = entry.getKey();
            Lecture parent = lectures.get(entry.getValue());
            if (parent == null) {
                iProgress.warn("Parent class " + entry.getValue() + " does not exists.");
            } else {
                lecture.setParent(parent);
            }
        }
		
		long distId = 0;
		for (Iterator<?> i = root.element("distributions").elementIterator("distribution"); i.hasNext(); ) {
			Element distributionEl = (Element)i.next();
			String reference = distributionEl.attributeValue("type");
			GroupConstraint.ConstraintTypeInterface type = null;
			boolean positive = true;
			boolean required = "true".equalsIgnoreCase(distributionEl.attributeValue("required", "false"));
			int penalty = Integer.parseInt(distributionEl.attributeValue("penalty", "0"));
			Constraint<Lecture, Placement> constraint = null;
			if ("SameDays".equalsIgnoreCase(reference)) {
				type = GroupConstraint.ConstraintType.SAME_DAYS; positive = true;
			} else if ("DifferentDays".equalsIgnoreCase(reference)) {
				type = GroupConstraint.ConstraintType.SAME_DAYS; positive = false;
			} else if ("SameStart".equalsIgnoreCase(reference)) {
				type = GroupConstraint.ConstraintType.SAME_START; positive = true;
			} else if ("SameRoom".equalsIgnoreCase(reference)) {
				type = GroupConstraint.ConstraintType.SAME_ROOM; positive = true;
			} else if ("DifferentRoom".equalsIgnoreCase(reference)) {
				type = GroupConstraint.ConstraintType.SAME_ROOM; positive = false;
			} else if ("SameAttendees".equalsIgnoreCase(reference)) {
				type = GroupConstraint.ConstraintType.SAME_STUDENTS; positive = true;
			} else if ("SameTime".equalsIgnoreCase(reference)) {
				type = GroupConstraint.ConstraintType.SAME_TIME; positive = true;
			} else if ("DifferentTime".equalsIgnoreCase(reference)) {
				type = GroupConstraint.ConstraintType.SAME_TIME; positive = false;
			} else if ("SameWeeks".equalsIgnoreCase(reference)) {
				type = GroupConstraint.ConstraintType.SAME_WEEKS; positive = true;
			} else if ("DifferentWeeks".equalsIgnoreCase(reference)) {
				type = GroupConstraint.ConstraintType.SAME_WEEKS; positive = false;
			} else if ("Overlap".equalsIgnoreCase(reference)) {	
				type = GroupConstraint.ConstraintType.DIFF_TIME; positive = false;
			} else if ("NotOverlap".equalsIgnoreCase(reference)) {
				type = GroupConstraint.ConstraintType.DIFF_TIME; positive = true;
			} else if ("Precedence".equalsIgnoreCase(reference)) {
				type = GroupConstraint.ConstraintType.PRECEDENCE; positive = true; break;
			} else if (reference.matches("WorkDay\\([0-9]+\\)")) {
				Matcher matcher = Pattern.compile("WorkDay\\(([0-9]+)\\)").matcher(reference);
		        if (matcher.find()) {
		        	int p = Integer.parseInt(matcher.group(1));
		        	type = new GroupConstraint.ParametrizedConstraintType<Integer>(GroupConstraint.ConstraintType.WORKDAY, p, "WORKDAY(" + (p / 12.0) + ")");
					positive = true;
		        }
			} else if (reference.matches("MinGap\\([0-9]+\\)")) {
				Matcher matcher = Pattern.compile("MinGap\\(([0-9]+)\\)").matcher(reference);
		        if (matcher.find()) {
		        	int p = Integer.parseInt(matcher.group(1));
		        	type = new GroupConstraint.ParametrizedConstraintType<Integer>(GroupConstraint.ConstraintType.MIN_GAP, p, "MIN_GAP(" + (p / 12.0) + ")");
		        	positive = true;
		        }
			}
			if (type != null) {
				constraint = new ItcGroupConstraint(distId++, type, toPreference(required, penalty, positive));
			} else if (reference.matches("MaxBlock\\(([0-9]+),([0-9]+)\\)")) {
				Matcher matcher = Pattern.compile("MaxBlock\\(([0-9]+),([0-9]+)\\)").matcher(reference);
		        if (matcher.find()) {
		        	int m = Integer.parseInt(matcher.group(1));
		        	int s = Integer.parseInt(matcher.group(2));
					constraint = new ItcMaxBlockConstraint(distId++, "?", toPreference(required, penalty, positive), "_MaxBlock:" + (5 * m) + ":" + (5 * s) + "_");
		        }
			} else if (reference.matches("MaxDays\\(([0-9]+)\\)")) {
				Matcher matcher = Pattern.compile("MaxDays\\(([0-9]+)\\)").matcher(reference);
		        if (matcher.find()) {
		        	int d = Integer.parseInt(matcher.group(1));
					constraint = new ItcMaxDaysConstraint(distId++, "?", toPreference(required, penalty, positive), "_MaxDays:" + d + "_");
		        }
			} else if (reference.matches("MaxDayLoad\\(([0-9]+)\\)")) {
				Matcher matcher = Pattern.compile("MaxDayLoad\\(([0-9]+)\\)").matcher(reference);
		        if (matcher.find()) {
		        	int s = Integer.parseInt(matcher.group(1));
		        	constraint = new ItcGroupConstraint(distId++,
		        			new GroupConstraint.ParametrizedConstraintType<Integer>(GroupConstraint.ConstraintType.MAX_HRS_DAY, s, "MAX_HRS_DAY(" + (s / 12.0) + ")")
                            .setMin(s).setMax(s),
		        			toPreference(required, penalty, positive));
		        }
			} else if (reference.matches("MaxBreaks\\(([0-9]+),([0-9]+)\\)")) {
				Matcher matcher = Pattern.compile("MaxBreaks\\(([0-9]+),([0-9]+)\\)").matcher(reference);
		        if (matcher.find()) {
		        	int r = Integer.parseInt(matcher.group(1));
		        	int s = Integer.parseInt(matcher.group(2));
					constraint = new ItcMaxBreaksConstraint(distId++, "?", toPreference(required, penalty, positive), "_MaxBreaks:" + r + ":" + (5 * s) + "_");
		        }
			} else {
				iProgress.warn("Distrubtion type " + distributionEl.attributeValue("type") + " not implemented.");
			}
			if (constraint != null) {
				for (Iterator<?> j = distributionEl.elementIterator("class"); j.hasNext(); ) {
					Element classEl = (Element)j.next();
					Lecture lecture = lectures.get(Long.valueOf(classEl.attributeValue("id")));
					if (lecture == null) {
						iProgress.warn("Class " + classEl.attributeValue("id") + " is not defined.");
						continue;
					}
					constraint.addVariable(lecture);
				}
				getModel().addConstraint(constraint);
			}
		}
		
		HashMap<Long, Set<Student>> offering2students = new HashMap<Long, Set<Student>>();
		Map<Long, Student> students = new HashMap<Long, Student>();
		for (Iterator<?> i = root.element("students").elementIterator("student"); i.hasNext(); ) {
			Element studentEl = (Element)i.next();
			
			Student student = new Student(Long.valueOf(studentEl.attributeValue("id")));
            getModel().addStudent(student);
            students.put(student.getId(), student);
            
            for (Iterator<?> j = studentEl.elementIterator("course"); j.hasNext(); ) {
				Element courseEl = (Element)j.next();
				Long courseId = Long.valueOf(courseEl.attributeValue("id"));
				student.addOffering(courseId, 1d, null);
				Set<Student> studentsThisOffering = offering2students.get(courseId);
                if (studentsThisOffering == null) {
                    studentsThisOffering = new HashSet<Student>();
                    offering2students.put(courseId, studentsThisOffering);
                }
                studentsThisOffering.add(student);
			}
		}
		
        Element solutionEl = root.element("solution");
        if (solutionEl != null) {
        	iProgress.info("Loading solution...");
    		for (Iterator<?> i = solutionEl.elementIterator("class"); i.hasNext(); ) {
    			Element classEl = (Element)i.next();
    			Lecture clazz = lectures.get(Long.valueOf(classEl.attributeValue("id")));
    			if (clazz == null) {
    				iProgress.warn("Class " + classEl.attributeValue("id") + " does not exist.");
    				continue;
    			}
    			TimeLocation time = null;
    			if (classEl.attributeValue("days") != null && classEl.attributeValue("start") != null) {
    				int days = Integer.parseInt(classEl.attributeValue("days"), 2);
    				int start = Integer.valueOf(classEl.attributeValue("start"));
    				String weeks = classEl.attributeValue("weeks", allWeeks);
    				for (TimeLocation x: clazz.timeLocations())
    					if (x.getDayCode() == days && x.getStartSlot() == start && x.getDatePatternName().equals(weeks)) {
    						time = x; break;
    					}
    				if (time == null) {
    					TimeLocation t = new TimeLocation(days, start, 12, 0, 0d, 0, Long.valueOf(weeks, 2), weeks, toWeekCode(weeks), 10);
    					iProgress.warn("Time " + t.getDayHeader() + " " + t.getStartTimeHeader(false) + " " + weeks + " is not in the domain of class " + clazz.getId());
    				}
    			}
    			RoomLocation room = null;
    			if (classEl.attributeValue("room") != null) {
    				int roomId = Integer.parseInt(classEl.attributeValue("room"));
    				for (RoomLocation r: clazz.roomLocations())
    					if (r.getId() == roomId) { room = r; break; }
    				if (room == null)
    					iProgress.warn("Room " + roomId + " is not in the domain of class " + clazz.getId());
    			}
    			Placement placement = null;
    			if (time != null) {
    				if (clazz.getNrRooms() != 0 && room == null) {
    					iProgress.warn("Clazz " + clazz.getId() + " need a room.");
    				} else if (room != null && !room.getRoomConstraint().isAvailable(clazz, time, 0l)) {
    					iProgress.warn("Room " + room.getId() + " is not available during " + time);
    				} else {
    					placement = new Placement(clazz, time, room);
    				}
    			}
    			if (placement != null) {
    				clazz.setInitialAssignment(placement);
    				Set<Placement> conf = getModel().conflictValues(getAssignment(), placement);
                    if (conf.isEmpty()) {
                    	getAssignment().assign(0, placement);
                    } else {
                        iProgress.error("Unable to assign clazz " + clazz.getId() + " to " + placement);
                        iProgress.error("Conflicts:" + ToolBox.dict2string(getModel().conflictConstraints(getAssignment(), placement), 2));
                    }
    			}
    			for (Iterator<?> j = classEl.elementIterator("student"); j.hasNext(); ) {
    				Element studentEl = (Element)j.next();
    				Long studentId = Long.valueOf(studentEl.attributeValue("id"));
    				Student student = students.get(studentId);
    				if (student == null) {
    					iProgress.warn("Student " + studentId + " does not exist");
    				} else if (!student.getOfferings().contains(clazz.getConfiguration().getOfferingId())) {
    					iProgress.warn("Student " + studentId + " did not request course " + clazz.getConfiguration().getOfferingId());
    				} else {
    					student.addLecture(clazz);
                        student.addConfiguration(clazz.getConfiguration());
                        clazz.addStudent(getAssignment(), student);
    				}
    			}
    		}
        } else {
    		for (Map.Entry<Long, Set<Student>> entry : offering2students.entrySet()) {
                Long offeringId = entry.getKey();
                Set<Student> studentsThisOffering = entry.getValue();
                List<Configuration> altConfigs = configurations.get(offeringId);
                getModel().getStudentSectioning().initialSectioning(getAssignment(), offeringId, String.valueOf(offeringId), studentsThisOffering, altConfigs);
            }
        }
    		
		HashMap<Lecture, HashMap<Lecture, JenrlConstraint>> jenrls = new HashMap<Lecture, HashMap<Lecture, JenrlConstraint>>();
        for (Iterator<Student> i1 = getModel().getAllStudents().iterator(); i1.hasNext();) {
            Student st = i1.next();
            for (Iterator<Lecture> i2 = st.getLectures().iterator(); i2.hasNext();) {
                Lecture l1 = i2.next();
                for (Iterator<Lecture> i3 = st.getLectures().iterator(); i3.hasNext();) {
                    Lecture l2 = i3.next();
                    if (l1.getId() >= l2.getId())
                        continue;
                    HashMap<Lecture, JenrlConstraint> x = jenrls.get(l1);
                    if (x == null) {
                        x = new HashMap<Lecture, JenrlConstraint>();
                        jenrls.put(l1, x);
                    }
                    JenrlConstraint jenrl = x.get(l2);
                    if (jenrl == null) {
                        jenrl = new JenrlConstraint();
                        jenrl.addVariable(l1);
                        jenrl.addVariable(l2);
                        getModel().addConstraint(jenrl);
                        x.put(l2, jenrl);
                    }
                    jenrl.incJenrl(getAssignment(), st);
                }
            }
        }
        
        if (getModel().getProperties().getPropertyBoolean("General.PurgeInvalidPlacements", true)) {
            for (Lecture lecture : getModel().variables()) {
                lecture.purgeInvalidValues(false);
            }
        }
	}
}
