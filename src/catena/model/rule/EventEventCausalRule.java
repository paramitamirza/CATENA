package catena.model.rule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import catena.model.CandidateLinks;
import catena.model.feature.CausalSignalList;
import catena.model.feature.EventEventFeatureVector;
import catena.model.feature.Marker;
import catena.model.feature.PairFeatureVector;
import catena.model.feature.TemporalSignalList;
import catena.model.feature.FeatureEnum.PairType;
import catena.parser.entities.CausalRelation;
import catena.parser.entities.Doc;
import catena.parser.entities.Entity;
import catena.parser.entities.EntityEnum;
import catena.parser.entities.Event;
import catena.parser.entities.TemporalRelation;

public class EventEventCausalRule {
	
	private String relType;
	
	public EventEventCausalRule(PairFeatureVector fv) {
		
	}
	
	public EventEventCausalRule(EventEventFeatureVector eefv) throws Exception {		
		this.setRelType("O");
		String eventRule = getEventCausalityRule(eefv); 
		if (!eventRule.equals("O") && !eventRule.equals("NONE")) {
			if (eventRule.contains("-R")) this.setRelType("CLINK-R");
			else this.setRelType("CLINK");
		} else {
			this.setRelType("NONE");
		}
	}
	
	public EventEventCausalRule(EventEventFeatureVector eefv, boolean CVerb) throws Exception {		
		this.setRelType("O");
		String eventRule = getEventCausalityRule(eefv); 
		if (!eventRule.equals("O") && !eventRule.equals("NONE")) {
			if (CVerb) {
				this.setRelType(eventRule);
			} else {
				if (eventRule.contains("-R")) this.setRelType("CLINK-R");
				else this.setRelType("CLINK");
			}
		} else {
			this.setRelType("NONE");
		}
	}
	
	public static List<String> getEventEventClinksPerFile(Doc doc, boolean CVerb) throws Exception {
		List<String> ee = new ArrayList<String>();
		
		TemporalSignalList tsignalList = new TemporalSignalList(EntityEnum.Language.EN);
		CausalSignalList csignalList = new CausalSignalList(EntityEnum.Language.EN);
	    
		List<CausalRelation> candidateClinks = doc.getCandidateClinks();	//candidate pairs
		
		for (CausalRelation clink : candidateClinks) {
			
			if (!clink.getSourceID().equals(clink.getTargetID())
					&& doc.getEntities().containsKey(clink.getSourceID())
					&& doc.getEntities().containsKey(clink.getTargetID())
					) {
				
				Entity e1 = doc.getEntities().get(clink.getSourceID());
				Entity e2 = doc.getEntities().get(clink.getTargetID());
				PairFeatureVector fv = new PairFeatureVector(doc, e1, e2, 
						CandidateLinks.getClinkType(e1.getID(), e2.getID(), doc), tsignalList, csignalList);	
				
				if (fv.getPairType().equals(PairType.event_event)) {
					EventEventFeatureVector eefv = new EventEventFeatureVector(fv);
					EventEventCausalRule eeRule = new EventEventCausalRule(eefv, CVerb);
					
					if (!eeRule.getRelType().equals("NONE")) {
						ee.add(eefv.getE1().getID() + "\t" + eefv.getE2().getID() + "\t" + 
								eefv.getLabel() + "\t" + eeRule.getRelType());
//					} else {
//						ee.add(eefv.getE1().getID() + "\t" + eefv.getE2().getID() + "\t" + 
//								eefv.getLabel() + "\tNONE");
					}
				}
			}
		}
		return ee;
	}
	
	public static Map<Integer, Set<String>> getEventEventClinksPerFileCombined(Doc doc, boolean CVerb) throws Exception {
		Map<Integer, Set<String>> ee = new TreeMap<Integer, Set<String>>();
		
		TemporalSignalList tsignalList = new TemporalSignalList(EntityEnum.Language.EN);
		CausalSignalList csignalList = new CausalSignalList(EntityEnum.Language.EN);
	    
		List<CausalRelation> candidateClinks = doc.getCandidateClinks();	//candidate pairs
		
		for (CausalRelation clink : candidateClinks) {
			
			if (!clink.getSourceID().equals(clink.getTargetID())
					&& doc.getEntities().containsKey(clink.getSourceID())
					&& doc.getEntities().containsKey(clink.getTargetID())
					) {
				
				Entity e1 = doc.getEntities().get(clink.getSourceID());
				Entity e2 = doc.getEntities().get(clink.getTargetID());
				PairFeatureVector fv = new PairFeatureVector(doc, e1, e2, 
						CandidateLinks.getClinkType(e1.getID(), e2.getID(), doc), tsignalList, csignalList);	
				
				if (fv.getPairType().equals(PairType.event_event)) {
					EventEventFeatureVector eefv = new EventEventFeatureVector(fv);
					EventEventCausalRule eeRule = new EventEventCausalRule(eefv, CVerb);
					
					if (!eeRule.getRelType().equals("NONE")) {
						String sentID = eefv.getE1().getSentID();
						if (eeRule.getRelType().endsWith("-R")) sentID = eefv.getE2().getSentID(); 
								
						if (!ee.containsKey(Integer.parseInt(sentID))) ee.put(Integer.parseInt(sentID), new HashSet<String>());
						
						if (eeRule.getRelType().endsWith("-R")) {
							ee.get(Integer.parseInt(sentID)).add(
									eefv.getE2().getID() 
									+ " " + CausalRelation.getInverseRelation(eeRule.getRelType())
									+ " " + eefv.getE1().getID()
									);
						} else {
							ee.get(Integer.parseInt(sentID)).add(
									eefv.getE1().getID() 
									+ " " + eeRule.getRelType()
									+ " " + eefv.getE2().getID()
									);
						}
//					} else {
//						ee.add(eefv.getE1().getID() + "\t" + eefv.getE2().getID() + "\t" + 
//								eefv.getLabel() + "\tNONE");
					}
				}
			}
		}
		return ee;
	}
	
	public String getEventCausalityRule(EventEventFeatureVector eefv) throws Exception {
		String cVerb = "O", construction = "O";		
		if (eefv.getE1().getSentID().equals(eefv.getE2().getSentID())) {	//in the same sentence
			Marker m = eefv.getCausalVerb();
//			Marker mSig = eefv.getCausalSignal();
//			if (mSig.getText().equals("O") 
//					|| mSig.getText().equals("result")
//					|| mSig.getText().equals("by")) {
				if (!m.getCluster().equals("O")) {
					if (m.getCluster().equals("AFFECT")) {
						if ((m.getDepRelE1().equals("SBJ") 
								|| m.getDepRelE1().equals("ADV")
								|| m.getDepRelE1().equals("NMOD")
								|| m.getDepRelE1().equals("APPO")
								|| m.getDepRelE1().equals("PRD-IM")
								|| m.getDepRelE1().equals("PRP-IM"))) {
							if (m.getDepRelE2().equals("OBJ")) {
								cVerb = "AFFECT";
							} else {
								cVerb = "NONE";
							}
						} else {
							cVerb = "NONE";
						}
					} else if (m.getCluster().contains("LINK")) {
						if ((m.getDepRelE1().equals("SBJ") 
								|| m.getDepRelE1().equals("ADV")
								|| m.getDepRelE1().equals("NMOD")
								|| m.getDepRelE1().equals("APPO")
								|| m.getDepRelE1().equals("PRD-IM")
								|| m.getDepRelE1().equals("APPO-OPRD-IM")
								|| m.getDepRelE1().equals("NMOD-OPRD-IM")
								|| m.getDepRelE1().equals("PRP-IM"))) {
							if (m.getDepRelE2().equals("DIR-PMOD")
									|| m.getDepRelE2().equals("ADV-PMOD")
									|| m.getDepRelE2().equals("NMOD-PMOD")
									|| m.getDepRelE2().equals("AMOD-PMOD")) {
								cVerb = m.getCluster();
								
//							} else if ((m.getDepRelE2().equals("OBJ")
//									|| m.getDepRelE2().equals("OBJ-NMOD")
//									)
//									&& (m.getDepRelE1().equals("ADV")
//											|| m.getDepRelE1().equals("SBJ")
//											|| m.getDepRelE1().equals("APPO")
//											|| m.getDepRelE1().equals("OBJ-APPO")
//											|| m.getDepRelE1().equals("PRP-IM"))
//									&& (m.getText().equals("reflect")
//	//								|| m.getText().equals("follow")
//								)) {
//								cVerb = m.getCluster();
	//						} else if (m.getDepRelE2().equals("LGS-PMOD")
	//								&& m.getText().equals("follow")) {
	//							cVerb = "LINK";
							} else {
								cVerb = "NONE";
							}
						} else {
							cVerb = "NONE";
						}
					} else if (m.getCluster().equals("CAUSE")) {
						if ((m.getDepRelE1().equals("SBJ") 
								|| m.getDepRelE1().equals("ADV")
								|| m.getDepRelE1().equals("NMOD")
								|| m.getDepRelE1().equals("APPO")
								|| m.getDepRelE1().equals("PRD-IM")
								|| m.getDepRelE1().equals("PRP-IM"))) {
							if (m.getDepRelE2().equals("OBJ")) {
								cVerb = "CAUSE";
								construction = "BASIC";
							} else if (m.getDepRelE2().equals("OPRD-IM")
									|| m.getDepRelE2().equals("OPRD")) {
								cVerb = "CAUSE";
								construction = "PERIPHRASTIC";
							} else if (m.getDepRelE2().equals("LGS-PMOD")) {
								cVerb = "CAUSE-R";
								construction = "PASS";
							} else {
								cVerb = "NONE";
							}
						} else {
							cVerb = "NONE";
						}
					} else if (m.getCluster().equals("CAUSE-AMBIGUOUS")) {
						if ((m.getDepRelE1().equals("SBJ") 
								|| m.getDepRelE1().equals("ADV")
								|| m.getDepRelE1().equals("NMOD")
								|| m.getDepRelE1().equals("PRD-IM")
								|| m.getDepRelE1().equals("PRP-IM")
								|| m.getDepRelE1().equals("SBJ-PMOD")
								|| m.getDepRelE1().equals("SBJ-ADV-PMOD"))) {
							if (m.getDepRelE2().equals("OPRD-IM")) {
								cVerb = "CAUSE";
								construction = "PERIPHRASTIC";
							} else if (m.getText().equals("make")
									&& m.getDepRelE2().equals("OPRD-SUB-IM")) {
								cVerb = "CAUSE";
								construction = "PERIPHRASTIC";
							} else {
								cVerb = "NONE";
							}
						} else {
							cVerb = "NONE";
						}
					} else if (m.getCluster().equals("PREVENT")) {
						if ((m.getDepRelE1().equals("SBJ") 
								|| m.getDepRelE1().equals("ADV")
								|| m.getDepRelE1().equals("NMOD")
								|| m.getDepRelE1().equals("APPO")
								|| m.getDepRelE1().equals("PRD-IM")
								|| m.getDepRelE1().equals("PRP-IM")
								|| m.getDepRelE1().equals("OBJ-IM"))) {
							if (m.getDepRelE2().equals("OBJ")) {
								cVerb = "PREVENT";
								construction = "BASIC";
							} else if (m.getDepRelE2().equals("OPRD-IM")
									|| m.getDepRelE2().equals("OPRD")
									|| m.getDepRelE2().equals("ADV-PMOD")) {
								cVerb = "PREVENT";
								construction = "PERIPHRASTIC";
							} else if (m.getDepRelE2().equals("LGS-PMOD")) {
								cVerb = "PREVENT-R";
								construction = "PERIPHRASTIC";
							} else {
								cVerb = "NONE";
							}
						} else {
							cVerb = "NONE";
						}
					} else if (m.getCluster().equals("PREVENT-AMBIGUOUS")) {
						if ((m.getDepRelE1().equals("SBJ") 
								|| m.getDepRelE1().equals("ADV")
								|| m.getDepRelE1().equals("NMOD")
								|| m.getDepRelE1().equals("PRD-IM")
								|| m.getDepRelE1().equals("PRP-IM")
								|| m.getDepRelE1().equals("ADV-PMOD-IM"))) {
							if (m.getDepRelE2().equals("ADV-PMOD")
									|| m.getDepRelE2().equals("OPRD")) {
								cVerb = "PREVENT";
								construction = "PERIPHRASTIC";
							} else {
								cVerb = "NONE";
							}
						} else {
							cVerb = "NONE";
						}
					} else if (m.getCluster().equals("ENABLE")) {
						if ((m.getDepRelE1().equals("SBJ") 
								|| m.getDepRelE1().equals("ADV")
								|| m.getDepRelE1().equals("NMOD")
								|| m.getDepRelE1().equals("APPO")
								|| m.getDepRelE1().equals("PRD-IM")
								|| m.getDepRelE1().equals("PRP-IM")
								|| m.getDepRelE1().equals("OBJ-NMOD")
								|| m.getDepRelE1().equals("IM")
								|| m.getDepRelE1().equals("NMOD-IM"))) {
							if (m.getDepRelE2().equals("OBJ")
									&& (m.getText().equals("ensure")
									|| m.getText().equals("guarantee"))) {
								cVerb = "ENABLE";
								construction = "BASIC";
							} else if (m.getDepRelE2().equals("OPRD-IM")
									|| m.getDepRelE2().equals("OPRD")
									|| m.getDepRelE2().equals("OBJ-IM")) {
								cVerb = "ENABLE";
								construction = "PERIPHRASTIC";
							} else {
								cVerb = "NONE";
							}
						} else {
							cVerb = "NONE";
						}
					}
					
					String link = "O";
					if (!cVerb.equals("O") && !cVerb.equals("NONE")) {
						if (cVerb.contains("-R")) link = "CLINK-R";
						else link = "CLINK";
					}
//					System.err.println(eefv.getDoc().getFilename()+"\t"
//							+eefv.getLabel()+"\t"+link+"\t"
//							+cVerb+ "\t"+m.getCluster()+"\t"
//							+construction+"\t"
//							+eefv.getE1().getID()+"\t"+eefv.getE2().getID()+"\t"
//							+m.getDepRelE1()+"|"+m.getDepRelE2());
				}
//			}
		}
		return cVerb;
	}
	
	public String getRelType() {
		return relType;
	}

	public void setRelType(String relType) {
		this.relType = relType;
	}

}
