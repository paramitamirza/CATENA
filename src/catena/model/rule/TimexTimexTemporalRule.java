package catena.model.rule;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import catena.model.feature.CausalSignalList;
import catena.model.feature.PairFeatureVector;
import catena.model.feature.TemporalSignalList;
import catena.model.feature.FeatureEnum.PairType;
import catena.parser.entities.Doc;
import catena.parser.entities.Entity;
import catena.parser.entities.EntityEnum;
import catena.parser.entities.TemporalRelation;
import catena.parser.entities.Timex;

public class TimexTimexTemporalRule {
	
	private String relType;
	private Boolean identityRel=true;
	
	public static Map<String,String> getTimexTimexRuleRelation(Doc doc) {
		Object[] entArr = doc.getEntities().keySet().toArray();
		Map<String,String> ttlinks = new HashMap<String,String>();
		for (int i = 0; i < entArr.length; i++) {
			for (int j = i; j < entArr.length; j++) {
				if (!entArr[i].equals(entArr[j]) && doc.getEntities().get(entArr[i]) instanceof Timex && 
						doc.getEntities().get(entArr[j]) instanceof Timex) {
					TimexTimexTemporalRule timextimex = 
							new TimexTimexTemporalRule(((Timex)doc.getEntities().get(entArr[i])), 
							((Timex)doc.getEntities().get(entArr[j])), doc.getDct(), false);
					if (!timextimex.getRelType().equals("O")) {
						ttlinks.put(((String) entArr[i]) + "," + ((String) entArr[j]), timextimex.getRelType());
						ttlinks.put(((String) entArr[j]) + "," + ((String) entArr[i]), TemporalRelation.getInverseRelation(timextimex.getRelType()));
					}
				}
			}
		}
		return ttlinks;
	}
	
	public static List<String> getTimexTimexTlinksPerFile(Doc doc, boolean goldCandidate) throws Exception {
		List<String> tt = new ArrayList<String>();
		
		TemporalSignalList tsignalList = new TemporalSignalList(EntityEnum.Language.EN);
		CausalSignalList csignalList = new CausalSignalList(EntityEnum.Language.EN);
	    
		//Determine the relation type of every timex-timex pair in the document via rules 
		Map<String,String> ttlinks = getTimexTimexRuleRelation(doc);
		
		List<TemporalRelation> candidateTlinks = new ArrayList<TemporalRelation> ();
		if (goldCandidate) candidateTlinks = doc.getTlinks();	//gold annotated pairs
		else candidateTlinks = doc.getCandidateTlinks();		//candidate pairs
		
		for (TemporalRelation tlink : candidateTlinks) {
			if (!tlink.getSourceID().equals(tlink.getTargetID())
					&& doc.getEntities().containsKey(tlink.getSourceID())
					&& doc.getEntities().containsKey(tlink.getTargetID())
					) {
				
				Entity e1 = doc.getEntities().get(tlink.getSourceID());
				Entity e2 = doc.getEntities().get(tlink.getTargetID());
				PairFeatureVector fv = new PairFeatureVector(doc, e1, e2, tlink.getRelType(), tsignalList, csignalList);	
				
				if (fv.getPairType().equals(PairType.timex_timex)) {
					String st = tlink.getSourceID() + "," + tlink.getTargetID();
					String ts = tlink.getTargetID() + "," + tlink.getSourceID();
					if (ttlinks.containsKey(st)) {
						tt.add(tlink.getSourceID() + "\t" + tlink.getTargetID() + "\t" + 
								tlink.getRelType() + "\t" + ttlinks.get(st));
					} else if (ttlinks.containsKey(ts)) {
						tt.add(tlink.getSourceID() + "\t" + tlink.getTargetID() + "\t" + 
								tlink.getRelType() + "\t" + TemporalRelation.getInverseRelation(ttlinks.get(ts)));
//					} else {
//						tt.add(tlink.getSourceID() + "\t" + tlink.getTargetID() + "\t" + 
//								tlink.getRelType() + "\tNONE");
					}
				}
			}
		}
		return tt;
	}
	
	public TimexTimexTemporalRule(Timex t1, Timex t2, Timex dct, Boolean identityRel) {
		
		this.setRelType("O");	
		this.setIdentityRel(identityRel);
		
		//Remove time in DCT and change it into DATE type
		if (t1.isDct()) {
			if (t1.getValue().contains("T")) t1.setValue(t1.getValue().split("T")[0]);
			t1.setType("DATE");
		}
		if (t2.isDct()) {
			if (t2.getValue().contains("T")) t2.setValue(t2.getValue().split("T")[0]);
			t2.setType("DATE");
		}
		
		if (!t1.getID().equals(t2.getID())) {
			if (t1.getType().equals("DATE") && t2.getType().equals("TIME")) {
				if (t1.getValue().equals(t2.getValue())) {
					this.setRelType("IDENTITY");
				} else if (t2.getValue().contains(t1.getValue())) {
					this.setRelType("INCLUDES");
				} else {
					this.setRelType(getTmxDateRelation(t1.getValue(), t2.getValue(), dct.getValue()));
				}
			} else if (t1.getType().equals("TIME") && t2.getType().equals("DATE")) {
				if (t1.getValue().equals(t2.getValue())) {
					this.setRelType("IDENTITY");
				} else if (t1.getValue().contains(t2.getValue())) {
					this.setRelType("IS_INCLUDED");
				} else {
					this.setRelType(getTmxDateRelation(t1.getValue(), t2.getValue(), dct.getValue()));
				}
			} else if (t1.getType().equals("DATE") && t2.getType().equals("DATE")) {
				if (t1.getValue().equals(t2.getValue())) {
					this.setRelType("IDENTITY");
				} else if (t2.getValue().contains(t1.getValue())) {
					this.setRelType("INCLUDES");
				} else if (t1.getValue().contains(t2.getValue())) {
					this.setRelType("IS_INCLUDED");
				} else {
					if ((t1.getValue().contains("W") || t2.getValue().contains("W")) 
							&& (!t1.getValue().contains("WI") && !t2.getValue().contains("WI"))
							&& (!t1.getValue().contains("P") && !t2.getValue().contains("P"))) {
						this.setRelType(this.getTmxWeekDateRelation(t1.getValue(), t2.getValue()));
					} else {
						this.setRelType(getTmxDateRelation(t1.getValue(), t2.getValue(), dct.getValue()));
					}
				}
			} else if (t1.getType().equals("TIME") && t2.getType().equals("TIME")) {
				String[] dateTime1 = {t1.getValue(), ""};
				if (t1.getValue().split("T").length > 1) {
					dateTime1 = t1.getValue().split("T");
				} 
				String[] dateTime2 = {t2.getValue(), ""};
				if (t2.getValue().split("T").length > 1) {
					dateTime2 = t2.getValue().split("T");
				} 
				if (dateTime1[0].equals(dateTime2[0])) {
					if (!dateTime1[1].equals("") && !dateTime2[1].equals("")) {
						this.setRelType(getTimeRelation(getTimeComponents(dateTime1[1]), 
								getTimeComponents(dateTime2[1])));
					}
				} else {
					this.setRelType(getTmxDateRelation(dateTime1[0], dateTime2[0], dct.getValue()));
				}
			}
		}
		
		if (!this.isIdentityRel()) {
			if (this.getRelType().equals("IDENTITY")) {
				this.setRelType("SIMULTANEOUS");
			}
		}
	}
	
	private String getTmxWeekDateRelation(String date1, String date2) {
		if (date1.contains("T")) date1 = date1.split("T")[0];
		if (date2.contains("T")) date2 = date2.split("T")[0];
		
		if (date1.contains("W") && date2.contains("W")
				&& !date1.contains("X") && !date2.contains("X")) {
			int y1 = Integer.valueOf(date1.split("-W")[0]);
			int y2 = Integer.valueOf(date2.split("-W")[0]);
			int w1 = Integer.valueOf(date1.split("-W")[1]);
			int w2 = Integer.valueOf(date2.split("-W")[1]);
			if (y1 < y2) {
				return "BEFORE";
			} else if (y1 > y2) {
				return "AFTER";
			} else {
				if (w1 < w2) {
					return "BEFORE";
				} else if (w1 > w2) {
					return "AFTER";
				} else {
					return "IDENTITY";
				}
			}
		} else if (date1.contains("W") && !date2.contains("W")
				&& !date1.contains("X")) {
			ArrayList<Date> dd1 = new ArrayList<Date>();
			int y1 = Integer.valueOf(date1.split("-W")[0]);
			int w1 = Integer.valueOf(date1.split("-W")[1]);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Calendar cal = Calendar.getInstance();
			cal.setMinimalDaysInFirstWeek(4);
			cal.set(Calendar.YEAR, y1);
			cal.set(Calendar.WEEK_OF_YEAR, w1);
			cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
			for (int i=0; i<7; i++) {			
				dd1.add(getDateComponents(sdf.format(cal.getTime())));
				cal.add(Calendar.DATE, 1);
			}
			Date d2 = getDateComponents(date2);
			
			if (dd1.contains(d2)) {
				return "INCLUDES";
			} else {
				if (getDateRelation(dd1.get(6), d2).equals("BEFORE")) {
					return "BEFORE";
				} else if (getDateRelation(dd1.get(0), d2).equals("AFTER")) {
					return "AFTER";
				} else {
					return "O";
				}
			}
				
		} else if (!date1.contains("W") && date2.contains("W")
				&& !date2.contains("X")) {
			ArrayList<Date> dd2 = new ArrayList<Date>();
			int y2 = Integer.valueOf(date2.split("-W")[0]);
			int w2 = Integer.valueOf(date2.split("-W")[1]);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Calendar cal = Calendar.getInstance();
			cal.setMinimalDaysInFirstWeek(4);
			cal.set(Calendar.YEAR, y2);
			cal.set(Calendar.WEEK_OF_YEAR, w2);
			cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
			for (int i=0; i<7; i++) {			
				dd2.add(getDateComponents(sdf.format(cal.getTime())));
				cal.add(Calendar.DATE, 1);
			}
			Date d1 = getDateComponents(date1);
			
			if (dd2.contains(d1)) {
				return "IS_INCLUDED";
			} else {
				if (getDateRelation(d1, dd2.get(0)).equals("BEFORE")) {
					return "BEFORE";
				} else if (getDateRelation(d1, dd2.get(6)).equals("AFTER")) {
					return "AFTER";
				} else {
					return "O";
				}
			}
		} else {
			return "O";
		}		
	}
	
	private String getTmxDateRelation(String date1, String date2, String dctStr) {
		String[] eraArr = {"PAST_REF", "PRESENT_REF", "FUTURE_REF"};
		List<String> eraList = Arrays.asList(eraArr);
		
		if (!eraList.contains(date1) && date1.contains("T")) {
			date1 = date1.split("T")[0];
		}
		if (!eraList.contains(date2) && date2.contains("T")) {
			date2 = date2.split("T")[0];
		}
		
		Date d1 = getDateComponents(date1);
		Date d2 = getDateComponents(date2);
		Date dct = getDateComponents(dctStr.split("T")[0]);
		
		if (!d1.getEra().equals("") && !d2.getEra().equals("") &&
				eraList.contains(d1.getEra()) && eraList.contains(d2.getEra())) {
			if (eraList.indexOf(d1.getEra()) < eraList.indexOf(d2.getEra())) {
				return "BEFORE";
			} else if (eraList.indexOf(d1.getEra()) > eraList.indexOf(d2.getEra())) {
				return "AFTER";
			} else {
				return "IDENTITY";
			}
		} else if (!d1.getEra().equals("") && d2.getEra().equals("") &&
				eraList.contains(d1.getEra())) {
			if (d1.getEra().equals("PAST_REF")) {
				if (getDateRelation(d2, dct).equals("BEFORE")) {
					return "INCLUDES";
				} else {
					return "BEFORE";
				}
			} else if (d1.getEra().equals("PRESENT_REF")) {
				if (getDateRelation(d2, dct).equals("IDENTITY")) {
					return "INCLUDES";
				} else {
					return getDateRelation(d2, dct);
				}
			} else if (d1.getEra().equals("FUTURE_REF")) {
				if (getDateRelation(d2, dct).equals("AFTER")) {
					return "INCLUDES";
				} else {
					return "AFTER";
				}
			}
		} else if (d1.getEra().equals("") && !d2.getEra().equals("") &&
				eraList.contains(d2.getEra())) {
			if (d2.getEra().equals("PAST_REF")) {
				if (getDateRelation(d1, dct).equals("BEFORE")) {
					return "IS_INCLUDED";
				} else {
					return "AFTER";
				}
			} else if (d2.getEra().equals("PRESENT_REF")) {
				if (getDateRelation(d1, dct).equals("IDENTITY")) {
					return "IS_INCLUDED";
				} else {
					return getDateRelation(d1, dct);
				}
			} else if (d2.getEra().equals("FUTURE_REF")) {
				if (getDateRelation(d1, dct).equals("AFTER")) {
					return "IS_INCLUDED";
				} else {
					return "BEFORE";
				}
			}
		} else {
			return getDateRelation(d1, d2);
		}
		return null;
	}
	
	private String getDateRelation(Date d1, Date d2) {
		Integer yy;
		
		if (d1.getYear() == 0 && d2.getYear() == 0) {
			//if (d1.getDecade())
			
		} else if (d1.getYear() == 0 && d2.getYear() != 0) {
			if (d1.getDecade() != 0) {
				yy = Integer.valueOf(d2.getYear().toString().substring(0, 3));
				if (d1.getDecade() < yy) {
					return "BEFORE";
				} else if (d1.getDecade() > yy) {
					return "AFTER";
				} else {
					return "INCLUDES";
				}
			} else if (d1.getCentury() != 0) {
				yy = Integer.valueOf(d2.getYear().toString().substring(0, 2));
				if (d1.getCentury() < yy) {
					return "BEFORE";
				} else if (d1.getCentury() > yy) {
					return "AFTER";
				} else {
					return "INCLUDES";
				}
			}
			
		} else if (d1.getYear() != 0 && d2.getYear() == 0) {
			if (d2.getDecade() != 0) {
				yy = Integer.valueOf(d1.getYear().toString().substring(0, 3));
				if (yy < d2.getDecade()) {
					return "BEFORE";
				} else if (yy > d2.getDecade()) {
					return "AFTER";
				} else {
					return "IS_INCLUDED";
				}
			} else if (d2.getCentury() != 0) {
				yy = Integer.valueOf(d1.getYear().toString().substring(0, 2));
				if (yy < d2.getCentury()) {
					return "BEFORE";
				} else if (yy > d2.getCentury()) {
					return "AFTER";
				} else {
					return "IS_INCLUDED";
				}
			}
			
		} else {
			if (d1.getYear() < d2.getYear()) {
				return "BEFORE";
			} else if (d1.getYear() > d2.getYear()) {
				return "AFTER";
			} else {
				if (!d1.getMonthArr().isEmpty() && d2.getMonthArr().isEmpty()) {
					if (d2.getMonth() == 0) {
						return "IS_INCLUDED";
					} else {
						if (d1.getMonthArr().contains(d2.getMonth())) { 
							return "INCLUDES";						
						} else if (d2.getMonth() < d1.getMonthArr().get(0)) { 
							return "AFTER";
						} else if (d2.getMonth() > d1.getMonthArr().get(d1.getMonthArr().size()-1)) { 
							return "BEFORE";
						}
					}
				} else if (d1.getMonthArr().isEmpty() && !d2.getMonthArr().isEmpty()) {
					if (d1.getMonth() == 0) {
						return "INCLUDES";
					} else {
						if (d2.getMonthArr().contains(d1.getMonth())) { 
							return "IS_INCLUDED";						
						} else if (d1.getMonth() < d2.getMonthArr().get(0)) { 
							return "BEFORE";
						} else if (d1.getMonth() > d2.getMonthArr().get(d2.getMonthArr().size()-1)) { 
							return "AFTER";
						}
					} 
				} else if (!d1.getMonthArr().isEmpty() && !d2.getMonthArr().isEmpty()) {
					if (d1.getMonthArr().get(0) == d2.getMonthArr().get(0)) {
						return "IDENTITY";
					} else if (d1.getMonthArr().get(d1.getMonthArr().size()-1) < d2.getMonthArr().get(0)) {
						return "BEFORE";
					} else if (d2.getMonthArr().get(d2.getMonthArr().size()-1) < d1.getMonthArr().get(0)) {
						return "AFTER";
					}
				} else {
					if (d1.getMonth() < d2.getMonth()) {
						return "BEFORE";
					} else if (d1.getMonth() > d2.getMonth()) {
						return "AFTER";
					} else {
						if (d1.getDay() != 0 && d2.getDay() != 0) {
							if (d1.getDay() < d2.getDay()) {
								return "BEFORE";
							} else if (d1.getDay() > d2.getDay()) {
								return "AFTER";
							} else {
								return "IDENTITY"; //the same date
							}
						} else if (d1.getDay() == 0 && d2.getDay() != 0) {
							return "INCLUDES";
						} else if (d1.getDay() != 0 && d2.getDay() == 0) {
							return "IS_INCLUDED";
						} else {
							return "IDENTITY"; //the same date
						}
					}
				}
			}
		}
		return "O";
	}
	
	private String getTimeRelation(Time t1, Time t2) {
		String[] partDayArr = {"MO", "AF", "EV", "NI"};
		List<String> partDayList = Arrays.asList(partDayArr);
		
		if (!t1.getRange().isEmpty() && t2.getRange().isEmpty()) {
			if (t1.getRange().contains(t2.getHour())) { 
				return "INCLUDES";						
			} else if (t2.getHour() < t1.getRange().get(0)) { 
				return "AFTER";
			} else if (t2.getHour() > t1.getRange().get(t1.getRange().size()-1)) { 
				return "BEFORE";
			}
		} else if (t1.getRange().isEmpty() && !t2.getRange().isEmpty()) {
			if (t2.getRange().contains(t1.getHour())) { 
				return "IS_INCLUDED";						
			} else if (t1.getHour() < t2.getRange().get(0)) { 
				return "BEFORE";
			} else if (t1.getHour() > t2.getRange().get(t2.getRange().size()-1)) { 
				return "AFTER";
			} 
		} else if (!t1.getRange().isEmpty() && !t2.getRange().isEmpty()) {
			if (partDayList.contains(t1.getPartDay()) && partDayList.contains(t2.getPartDay())) {
				if (partDayList.indexOf(t1.getPartDay()) < partDayList.indexOf(t2.getPartDay())) {
					return "BEFORE";
				} else if (partDayList.indexOf(t1.getPartDay()) > partDayList.indexOf(t2.getPartDay())) {
					return "AFTER";
				} else {
					return "IDENTITY";
				}
			}
		} else {
			if (t1.getHour() < t2.getHour()) {
				return "BEFORE";
			} else if (t1.getHour() > t2.getHour()) {
				return "AFTER";
			} else {
				if (t1.getMinute() < t2.getMinute()) {
					return "BEFORE";
				} else if (t1.getMinute() > t2.getMinute()) {
					return "AFTER";
				} else {
					if (t1.getSecond() < t2.getSecond()) {
						return "BEFORE";
					} else if (t1.getSecond() > t2.getSecond()) {
						return "AFTER";
					} else {
						return "IDENTITY";
					}
				}
			}
		}
		return null;
	}
	
	private Date getDateComponents(String date) {
		Date d = new Date();
		
		String[] cols = date.split("-");
		if (cols.length == 1) {
			if (cols[0].matches("\\d+")) {
				d.setYear(Integer.valueOf(cols[0]));
			} else if (cols[0].length() == 4 && cols[0].contains("X")) {
				String y = cols[0].replace("X", "");
				if (y.length() == 3) {	//decade
					d.setDecade(Integer.valueOf(y));
				} else if (y.length() == 2) {	//century
					d.setCentury(Integer.valueOf(y));
				}
			} else {
				d.setEra(cols[0]);
			}
		} else if (cols.length == 2) {
			if (cols[0].matches("\\d+") && cols[1].matches("\\d+")) {
				d.setYear(Integer.valueOf(cols[0])); 
				d.setMonth(Integer.valueOf(cols[1]));
			} else if (cols[1].startsWith("Q")) {
				d.setYear(Integer.valueOf(cols[0]));
				if (cols[1].endsWith("1")) {
					d.getMonthArr().add(1);
					d.getMonthArr().add(2);
					d.getMonthArr().add(3);
				} else if (cols[1].endsWith("2")) {
					d.getMonthArr().add(4);
					d.getMonthArr().add(5);
					d.getMonthArr().add(6);
				} else if (cols[1].endsWith("3")) {
					d.getMonthArr().add(7);
					d.getMonthArr().add(8);
					d.getMonthArr().add(9);
				} else if (cols[1].endsWith("4")) {
					d.getMonthArr().add(10);
					d.getMonthArr().add(11);
					d.getMonthArr().add(12);
				}
			} 
		} else if (cols.length == 3 && cols[0].matches("\\d+") && 
				cols[1].matches("\\d+") && cols[2].matches("\\d+")) {
			d.setYear(Integer.valueOf(cols[0])); 
			d.setMonth(Integer.valueOf(cols[1]));
			d.setDay(Integer.valueOf(cols[2]));
		}
		
		return d;
	}
	
	private Time getTimeComponents(String time) {
		Time t = new Time();
		
		String[] cols = time.split(":");
		if (cols.length == 1) {
			if (cols[0].matches("\\d+")) {
				t.setHour(Integer.valueOf(cols[0]));
			} else {
				if (cols[0].equals("MO")) {
					t.setPartDay(cols[0]);
					t.getRange().add(1); t.getRange().add(2);
					t.getRange().add(3); t.getRange().add(4);
					t.getRange().add(5); t.getRange().add(6);
					t.getRange().add(7); t.getRange().add(8);
					t.getRange().add(9); t.getRange().add(10);
					t.getRange().add(11);
				} else if (cols[0].equals("AF")) {
					t.setPartDay(cols[0]);
					t.getRange().add(13); t.getRange().add(14);
					t.getRange().add(15); t.getRange().add(16);
				} else if (cols[0].equals("EV")) {
					t.setPartDay(cols[0]);
					t.getRange().add(17); t.getRange().add(18);
					t.getRange().add(19); t.getRange().add(20);
				} else if (cols[0].equals("NI")) {
					t.setPartDay(cols[0]);
					t.getRange().add(21); t.getRange().add(22);
					t.getRange().add(23); t.getRange().add(24);
					t.getRange().add(0);
				}
			} 
		} else if (cols.length == 2 && 
				cols[0].matches("\\d+") && cols[1].matches("\\d+")) {
			t.setHour(Integer.valueOf(cols[0]));
			t.setMinute(Integer.valueOf(cols[1]));
		} else if (cols.length == 3 && cols[0].matches("\\d+") && 
				cols[1].matches("\\d+") && cols[2].matches("\\d+")) {
			t.setHour(Integer.valueOf(cols[0]));
			t.setMinute(Integer.valueOf(cols[1]));
			t.setSecond(Integer.valueOf(cols[2]));
		}
		
		return t;
	}
	
	public String getRelType() {
		return relType;
	}

	public void setRelType(String relType) {
		this.relType = relType;
	}
	
	public Boolean isIdentityRel() {
		return identityRel;
	}

	public void setIdentityRel(Boolean idRel) {
		this.identityRel = idRel;
	}

	private class Date {
		private Integer century;
		private Integer decade;
		private Integer year;
		private Integer month;
		private Integer day;
		private ArrayList<Integer> monthArr;
		private ArrayList<Integer> dayArr;
		private String era;
		public Date() {
			this.setCentury(0); this.setDecade(0);
			this.setYear(0); this.setMonth(0); this.setDay(0);
			this.setMonthArr(new ArrayList<Integer>());
			this.setDayArr(new ArrayList<Integer>());
			this.era = "";
		}
		public Integer getCentury() {
			return century;
		}
		public void setCentury(Integer cen) {
			this.century = cen;
		}
		public Integer getDecade() {
			return decade;
		}
		public void setDecade(Integer dec) {
			this.decade = dec;
		}
		public Integer getYear() {
			return year;
		}
		public void setYear(Integer year) {
			this.year = year;
		}
		public Integer getMonth() {
			return month;
		}
		public void setMonth(Integer month) {
			this.month = month;
		}
		public Integer getDay() {
			return day;
		}
		public void setDay(Integer day) {
			this.day = day;
		}
		public ArrayList<Integer> getMonthArr() {
			return monthArr;
		}
		public void setMonthArr(ArrayList<Integer> monthArr) {
			this.monthArr = monthArr;
		}
		public ArrayList<Integer> getDayArr() {
			return dayArr;
		}
		public void setDayArr(ArrayList<Integer> dayArr) {
			this.dayArr = dayArr;
		}
		public String getEra() {
			return era;
		}
		public void setEra(String era) {
			this.era = era;
		}
		@Override
	    public boolean equals(Object obj) {
			if(obj instanceof Date) {
				Date d = (Date) obj;
				return this.year.equals(d.year)
						&& this.month.equals(d.month)
						&& this.day.equals(d.day);
			}
			return false;
		}
		@Override
	    public int hashCode() {
	        return 1;
	    }
		@Override
		public String toString() {
			return this.year + "-" + this.month + "-" + this.day;
		}
	}
	
	private class Time {
		private Integer hour;
		private Integer minute;
		private Integer second;
		private String partDay;
		private ArrayList<Integer> range;
		public Time() {
			this.setHour(0); this.setMinute(0); this.setSecond(0);
			this.setRange(new ArrayList<Integer>());
		}
		public Integer getHour() {
			return hour;
		}
		public void setHour(Integer hour) {
			this.hour = hour;
		}
		public Integer getMinute() {
			return minute;
		}
		public void setMinute(Integer minute) {
			this.minute = minute;
		}
		public Integer getSecond() {
			return second;
		}
		public void setSecond(Integer second) {
			this.second = second;
		}
		public ArrayList<Integer> getRange() {
			return range;
		}
		public void setRange(ArrayList<Integer> range) {
			this.range = range;
		}
		public String getPartDay() {
			return partDay;
		}
		public void setPartDay(String partDay) {
			this.partDay = partDay;
		}
	}

}
