package catena.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import catena.model.feature.CausalSignalList;
import catena.model.feature.EventTimexFeatureVector;
import catena.model.feature.PairFeatureVector;
import catena.model.feature.TemporalSignalList;
import catena.model.feature.FeatureEnum.PairType;
import catena.parser.entities.CausalRelation;
import catena.parser.entities.Doc;
import catena.parser.entities.Entity;
import catena.parser.entities.EntityEnum;
import catena.parser.entities.Sentence;
import catena.parser.entities.TemporalRelation;
import catena.parser.entities.Timex;
import catena.parser.entities.Token;
import catena.parser.entities.Event;

public class CandidateLinks {
	
	public static String[] label = {"TLINK", "NONE"};	
	
	private static String getTlinkType(String sourceId, String targetId, Doc doc) {
		if (doc.getTlinkTypes().containsKey(sourceId + "," + targetId)) {
			return doc.getTlinkTypes().get(sourceId + "," + targetId);
		} 
		return "NONE";
	}
	
	public static void setCandidateTlinks(Doc doc) {
		doc.setCandidateTlinks(new ArrayList<TemporalRelation>());
		ArrayList<TemporalRelation> tlinkArr = doc.getCandidateTlinks();
		
		Timex dct = doc.getDct();
		Map<String, String> mainEvents = new LinkedHashMap<String, String>();
		
		String mainEvId = "", mainEvType = "";
		for (String sid : doc.getSentenceArr()) {
			Sentence sent = doc.getSentences().get(sid);
			
			if (!mainEvId.equals("") && !mainEvType.equals("")) {
				mainEvents.put(mainEvId, mainEvType);
			}
			
			for (int i = 0; i < sent.getEntityArr().size(); i ++) {
				String eid = sent.getEntityArr().get(i);
				Entity e = doc.getEntities().get(eid);
				String eType = "Event";
				if (e instanceof Timex) eType = "Timex";
				
				//TLINKs between entities in the same sentence
				for (int j = i+1; j < sent.getEntityArr().size(); j ++) {
					String eeid = sent.getEntityArr().get(j);
					Entity ee = doc.getEntities().get(eid);
					String eeType = "Event";
					if (ee instanceof Timex) eeType = "Timex";
					
					TemporalRelation tll = new TemporalRelation(eid, eeid);
					tll.setSourceType(eType); tll.setTargetType(eeType);
					tll.setRelType(getTlinkType(eid, eeid, doc));
					tlinkArr.add(tll);
				}
						
				// TLINK: (timex, DCT)
				if (eType.equals("Timex")) {
					TemporalRelation tl = new TemporalRelation(eid, dct.getID());
					tl.setSourceType(eType); tl.setTargetType("Timex");
					tl.setRelType(getTlinkType(eid, dct.getID(), doc));
					tlinkArr.add(tl);
				}
				
				if (eType.equals("Event")) {
					Token tok = doc.getTokens().get(e.getStartTokID());
					
					//TLINK with main event in previous sentence, only if main verb
					if (tok.isMainVerb()) {
						if (!mainEvents.isEmpty()) {
							Object[] mainEids = mainEvents.keySet().toArray();
							int lastIdx = mainEids.length-1;
							
							if (lastIdx >= 0) {
								String mainEid = (String) mainEids[lastIdx];
								TemporalRelation tll = new TemporalRelation(eid, mainEid);
								tll.setSourceType(eType); tll.setTargetType(mainEvents.get(mainEid));
								tll.setRelType(getTlinkType(eid, mainEid, doc));
								tlinkArr.add(tll);
							}
						}
					}
					
					// TLINK: (event, DCT) only if main verb
					if (tok.isMainVerb()) {
						TemporalRelation tl = new TemporalRelation(eid, dct.getID());
						tl.setSourceType(eType); tl.setTargetType("Timex");
						tl.setRelType(getTlinkType(eid, dct.getID(), doc));
						tlinkArr.add(tl);
						
						mainEvId = eid;
						mainEvType = eType;
					}
				}
			}
		}
	}
	
	public static String getClinkType(String sourceId, String targetId, Doc doc) {
		if (doc.getClinkTypes().containsKey(sourceId + "," + targetId)) {
			return doc.getClinkTypes().get(sourceId + "," + targetId);
		} 
		return "NONE";
	}
	
	public static void setCandidateClinks(Doc doc) {
		doc.setCandidateClinks(new ArrayList<CausalRelation>());
		ArrayList<CausalRelation> clinkArr = doc.getCandidateClinks();
		
		for (int s=0; s<doc.getSentenceArr().size(); s++) {
			Sentence s1 = doc.getSentences().get(doc.getSentenceArr().get(s));
			
			Entity e1, e2;
			String pair = null;
			for (int i = 0; i < s1.getEntityArr().size(); i++) {
				e1 = doc.getEntities().get(s1.getEntityArr().get(i));
				
				//candidate pairs within the same sentence
//				if (isContainCausalSignal(s1, doc) || isContainCausalVerb(s1, doc)) {
					if (i < s1.getEntityArr().size()-1) {
						for (int j = i+1; j < s1.getEntityArr().size(); j++) {
							e2 = doc.getEntities().get(s1.getEntityArr().get(j));
							if (e1 instanceof Event && e2 instanceof Event) {
								CausalRelation cl = new CausalRelation(e1.getID(), e2.getID());
								cl.setSourceType("Event"); cl.setTargetType("Event");
								clinkArr.add(cl);
							}
						}
					}
//				}
				
				//candidate pairs in consecutive sentences
				if (s < doc.getSentenceArr().size()-1) {
//					if (doc.getTokens().get(e1.getStartTokID()).isMainVerb()) {
						Sentence s2 = doc.getSentences().get(doc.getSentenceArr().get(s+1));
//						if (isContainCausalSignal(s2, doc)) {
						
							for (int j = 0; j < s2.getEntityArr().size(); j++) {
								e2 = doc.getEntities().get(s2.getEntityArr().get(j));
								if (e1 instanceof Event && e2 instanceof Event) {
									CausalRelation cl = new CausalRelation(e1.getID(), e2.getID());
									cl.setSourceType("Event"); cl.setTargetType("Event");
									clinkArr.add(cl);
								}
							}
//						}
//					}
				}
			}
		}
	}
	
	public static List<String> getTimexTimexTlinks(Doc doc) throws IOException {
		List<String> tt = new ArrayList<String>();
		List<TemporalRelation> candidateTlinks = doc.getCandidateTlinks();
		
		TemporalSignalList tsignalList = new TemporalSignalList(EntityEnum.Language.EN);
		CausalSignalList csignalList = new CausalSignalList(EntityEnum.Language.EN);
		
		Map<String, String> candidates = new HashMap<String, String>();
		
		for (TemporalRelation tlink : candidateTlinks) {
			if (!tlink.getSourceID().equals(tlink.getTargetID())
					&& doc.getEntities().containsKey(tlink.getSourceID())
					&& doc.getEntities().containsKey(tlink.getTargetID())
					) {
				
				Entity e1 = doc.getEntities().get(tlink.getSourceID());
				Entity e2 = doc.getEntities().get(tlink.getTargetID());
				PairFeatureVector fv = new PairFeatureVector(doc, e1, e2, tlink.getRelType(), tsignalList, csignalList);	
				
				if (fv.getPairType().equals(PairType.timex_timex)) {
					if (doc.getTlinkTypes().containsKey(tlink.getSourceID() + "," + tlink.getTargetID())) {
						tt.add(tlink.getSourceID() + "\t" + tlink.getTargetID() + "\t"
								+ "TLINK\t"
								+ "TLINK");
					} else {
						tt.add(tlink.getSourceID() + "\t" + tlink.getTargetID() + "\t"
								+ "NONE\t"
								+ "TLINK");
					}
					candidates.put(tlink.getSourceID() + "," + tlink.getTargetID(), "TLINK");
				}
			}
		}
		
		//Gold annotated TLINKs
		for (TemporalRelation tlink : doc.getTlinks()) {
			Entity e1 = doc.getEntities().get(tlink.getSourceID());
			Entity e2 = doc.getEntities().get(tlink.getTargetID());
			PairFeatureVector fv = new PairFeatureVector(doc, e1, e2, tlink.getRelType(), tsignalList, csignalList);	
			
			if (fv.getPairType().equals(PairType.timex_timex)) {
				if (!candidates.containsKey(tlink.getSourceID() + "," + tlink.getTargetID())
						&& !candidates.containsKey(tlink.getTargetID() + "," + tlink.getSourceID())) {
					tt.add(tlink.getSourceID() + "\t" + tlink.getTargetID() + "\t"
							+ "TLINK\t"
							+ "NONE");
				}
			}
		}
		
		return tt;
	}
	
	public static List<String> getEventTimexTlinks(Doc doc) throws IOException {
		List<String> et = new ArrayList<String>();
		List<TemporalRelation> candidateTlinks = doc.getCandidateTlinks();
		
		TemporalSignalList tsignalList = new TemporalSignalList(EntityEnum.Language.EN);
		CausalSignalList csignalList = new CausalSignalList(EntityEnum.Language.EN);
	    
		Map<String, String> candidates = new HashMap<String, String>();
		
		for (TemporalRelation tlink : candidateTlinks) {
			if (!tlink.getSourceID().equals(tlink.getTargetID())
					&& doc.getEntities().containsKey(tlink.getSourceID())
					&& doc.getEntities().containsKey(tlink.getTargetID())
					) {
				
				Entity e1 = doc.getEntities().get(tlink.getSourceID());
				Entity e2 = doc.getEntities().get(tlink.getTargetID());
				PairFeatureVector fv = new PairFeatureVector(doc, e1, e2, tlink.getRelType(), tsignalList, csignalList);	
				
				if (fv.getPairType().equals(PairType.event_timex)) {
					EventTimexFeatureVector etfv = new EventTimexFeatureVector(fv);
					
					if (!((Timex) etfv.getE2()).isDct()) {
						if (doc.getTlinkTypes().containsKey(tlink.getSourceID() + "," + tlink.getTargetID())) {
							et.add(tlink.getSourceID() + "\t" + tlink.getTargetID() + "\t"
									+ "TLINK\t"
									+ "TLINK");
						} else {
							et.add(tlink.getSourceID() + "\t" + tlink.getTargetID() + "\t"
									+ "NONE\t"
									+ "TLINK");
						}
						candidates.put(tlink.getSourceID() + "," + tlink.getTargetID(), "TLINK");
					}
				}
			}
		}	
		
		//Gold annotated TLINKs
		for (TemporalRelation tlink : doc.getTlinks()) {
			Entity e1 = doc.getEntities().get(tlink.getSourceID());
			Entity e2 = doc.getEntities().get(tlink.getTargetID());
			PairFeatureVector fv = new PairFeatureVector(doc, e1, e2, tlink.getRelType(), tsignalList, csignalList);	
			
			if (fv.getPairType().equals(PairType.event_timex)) {
				EventTimexFeatureVector etfv = new EventTimexFeatureVector(fv);
				
				if (!((Timex) etfv.getE2()).isDct()) {
					if (!candidates.containsKey(tlink.getSourceID() + "," + tlink.getTargetID())
							&& !candidates.containsKey(tlink.getTargetID() + "," + tlink.getSourceID())) {
						et.add(tlink.getSourceID() + "\t" + tlink.getTargetID() + "\t"
								+ "TLINK\t"
								+ "NONE");
					}
				}
			}
		}
				
		return et;
	}
	
	public static List<String> getEventDctTlinks(Doc doc) throws IOException {
		List<String> et = new ArrayList<String>();
		List<TemporalRelation> candidateTlinks = doc.getCandidateTlinks();
		
		TemporalSignalList tsignalList = new TemporalSignalList(EntityEnum.Language.EN);
		CausalSignalList csignalList = new CausalSignalList(EntityEnum.Language.EN);
	    
		Map<String, String> candidates = new HashMap<String, String>();
		
		for (TemporalRelation tlink : candidateTlinks) {
			if (!tlink.getSourceID().equals(tlink.getTargetID())
					&& doc.getEntities().containsKey(tlink.getSourceID())
					&& doc.getEntities().containsKey(tlink.getTargetID())
					) {
				
				Entity e1 = doc.getEntities().get(tlink.getSourceID());
				Entity e2 = doc.getEntities().get(tlink.getTargetID());
				PairFeatureVector fv = new PairFeatureVector(doc, e1, e2, tlink.getRelType(), tsignalList, csignalList);	
				
				if (fv.getPairType().equals(PairType.event_timex)) {
					EventTimexFeatureVector etfv = new EventTimexFeatureVector(fv);
					
					if (((Timex) etfv.getE2()).isDct()) {
						if (doc.getTlinkTypes().containsKey(tlink.getSourceID() + "," + tlink.getTargetID())) {
							et.add(tlink.getSourceID() + "\t" + tlink.getTargetID() + "\t"
									+ "TLINK\t"
									+ "TLINK");
						} else {
							et.add(tlink.getSourceID() + "\t" + tlink.getTargetID() + "\t"
									+ "NONE\t"
									+ "TLINK");
						}
					}
					candidates.put(tlink.getSourceID() + "," + tlink.getTargetID(), "TLINK");
				}
			}
		}		
		
		//Gold annotated TLINKs
		for (TemporalRelation tlink : doc.getTlinks()) {
			Entity e1 = doc.getEntities().get(tlink.getSourceID());
			Entity e2 = doc.getEntities().get(tlink.getTargetID());
			PairFeatureVector fv = new PairFeatureVector(doc, e1, e2, tlink.getRelType(), tsignalList, csignalList);	
			
			if (fv.getPairType().equals(PairType.event_timex)) {
				EventTimexFeatureVector etfv = new EventTimexFeatureVector(fv);
				
				if (((Timex) etfv.getE2()).isDct()) {
					if (!candidates.containsKey(tlink.getSourceID() + "," + tlink.getTargetID())
							&& !candidates.containsKey(tlink.getTargetID() + "," + tlink.getSourceID())) {
						et.add(tlink.getSourceID() + "\t" + tlink.getTargetID() + "\t"
								+ "TLINK\t"
								+ "NONE");
					}
				}
			}
		}
				
		return et;
	}
	
	public static List<String> getEventEventTlinks(Doc doc) throws IOException {
		List<String> ee = new ArrayList<String>();
		List<TemporalRelation> candidateTlinks = doc.getCandidateTlinks();
		
		TemporalSignalList tsignalList = new TemporalSignalList(EntityEnum.Language.EN);
		CausalSignalList csignalList = new CausalSignalList(EntityEnum.Language.EN);
	    
		Map<String, String> candidates = new HashMap<String, String>();
		
		for (TemporalRelation tlink : candidateTlinks) {
			if (!tlink.getSourceID().equals(tlink.getTargetID())
					&& doc.getEntities().containsKey(tlink.getSourceID())
					&& doc.getEntities().containsKey(tlink.getTargetID())
					) {
				
				Entity e1 = doc.getEntities().get(tlink.getSourceID());
				Entity e2 = doc.getEntities().get(tlink.getTargetID());
				PairFeatureVector fv = new PairFeatureVector(doc, e1, e2, tlink.getRelType(), tsignalList, csignalList);	
				
				if (fv.getPairType().equals(PairType.event_event)) {
					if (doc.getTlinkTypes().containsKey(tlink.getSourceID() + "," + tlink.getTargetID())) {
						ee.add(tlink.getSourceID() + "\t" + tlink.getTargetID() + "\t"
								+ "TLINK\t"
								+ "TLINK");
					} else {
						ee.add(tlink.getSourceID() + "\t" + tlink.getTargetID() + "\t"
								+ "NONE\t"
								+ "TLINK");
					}
					candidates.put(tlink.getSourceID() + "," + tlink.getTargetID(), "TLINK");
				}
			}
		}	
		
		//Gold annotated TLINKs
		for (TemporalRelation tlink : doc.getTlinks()) {
			Entity e1 = doc.getEntities().get(tlink.getSourceID());
			Entity e2 = doc.getEntities().get(tlink.getTargetID());
			PairFeatureVector fv = new PairFeatureVector(doc, e1, e2, tlink.getRelType(), tsignalList, csignalList);	
			
			if (fv.getPairType().equals(PairType.event_event)) {
				if (!candidates.containsKey(tlink.getSourceID() + "," + tlink.getTargetID())
						&& !candidates.containsKey(tlink.getTargetID() + "," + tlink.getSourceID())) {
					ee.add(tlink.getSourceID() + "\t" + tlink.getTargetID() + "\t"
							+ "TLINK\t"
							+ "NONE");
				}
			}
		}
		
		return ee;
	}
	
	public static void main(String[] args) {
		
	}
}
