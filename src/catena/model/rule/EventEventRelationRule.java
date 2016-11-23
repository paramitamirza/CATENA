package catena.model.rule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import catena.model.feature.CausalSignalList;
import catena.model.feature.EventEventFeatureVector;
import catena.model.feature.Marker;
import catena.model.feature.PairFeatureVector;
import catena.model.feature.TemporalSignalList;
import catena.model.feature.FeatureEnum.FeatureName;
import catena.model.feature.FeatureEnum.PairType;
import catena.parser.entities.Doc;
import catena.parser.entities.Entity;
import catena.parser.entities.EntityEnum;
import catena.parser.entities.Event;
import catena.parser.entities.Sentence;
import catena.parser.entities.TemporalRelation;
import catena.parser.entities.Timex;

public class EventEventRelationRule {
	
	private String relType;
	private Boolean identityRel=true;
	public static Integer numReason=0;
	
	public EventEventRelationRule(PairFeatureVector fv) {
		
	}
	
	public EventEventRelationRule(Event e1, Event e2, Doc doc, String depPath,
			Boolean identity) {
		this(e1, e2, doc, depPath);
		this.setIdentityRel(identity);
	}
	
	public EventEventRelationRule(Event e1, Event e2, Doc doc, String depPath,
			Boolean identity, Map<String, String> etanchor, 
			Map<String, String> etbefore, Map<String, String> etafter, 
			Map<String, String> ttlinks) {
		this(e1, e2, doc, depPath, etanchor, etbefore, etafter, ttlinks);
		this.setIdentityRel(identity);
	}
	
	public EventEventRelationRule(Event e1, Event e2, Doc doc, String depPath) {
		
		this.setRelType("O");
		
		String eventRule = getEventRule(e1, e2, doc, depPath); 
		if (!eventRule.equals("O")) {
			this.setRelType(eventRule);
		}
		
		if (!identityRel && this.getRelType().equals("IDENTITY")) {
			this.setRelType("SIMULTANEOUS");
		}
		/***** TempEval3 *****/
		if (this.getRelType().equals("DURING") || this.getRelType().equals("DURING_INV")) {
			this.setRelType("SIMULTANEOUS");
		}
	}
	
	public EventEventRelationRule(Event e1, Event e2, Doc doc, String depPath,
			Map<String, String> etanchor,
			Map<String, String> etbefore, Map<String, String> etafter,
			Map<String, String> ttlinks) {
		
		this.setRelType("O");
		
		String eventRule = getEventRule(e1, e2, doc, depPath); 
		if (!eventRule.equals("O")) {
			this.setRelType(eventRule);
		} else {
			eventRule = getEventRule(e1, e2, doc, etanchor, 
					etbefore, etafter, ttlinks);
			if (!eventRule.equals("O")) {
				this.setRelType(eventRule);
			}
		}
		
		if (!identityRel && this.getRelType().equals("IDENTITY")) {
			this.setRelType("SIMULTANEOUS");
		}
		/***** TempEval3 *****/
		if (this.getRelType().equals("DURING") || this.getRelType().equals("DURING_INV")) {
			this.setRelType("SIMULTANEOUS");
		}
	}
	
	public static List<String> getEventEventTlinksPerFile(Doc doc, boolean goldCandidate) throws Exception {
		List<String> ee = new ArrayList<String>();
		
		TemporalSignalList tsignalList = new TemporalSignalList(EntityEnum.Language.EN);
		CausalSignalList csignalList = new CausalSignalList(EntityEnum.Language.EN);
	    
		List<TemporalRelation> candidateTlinks = new ArrayList<TemporalRelation> ();
		if (goldCandidate) candidateTlinks = doc.getTlinks();	//gold annotated pairs
		else candidateTlinks = doc.getCandidateTlinks();		//candidate pairs
	    
		for (TemporalRelation tlink : candidateTlinks) {
			if (!tlink.getSourceID().equals(tlink.getTargetID())
					&& doc.getEntities().containsKey(tlink.getSourceID())
					&& doc.getEntities().containsKey(tlink.getTargetID())
					&& !tlink.getRelType().equals("NONE")
					) {	//classifying the relation task
				
				Entity e1 = doc.getEntities().get(tlink.getSourceID());
				Entity e2 = doc.getEntities().get(tlink.getTargetID());
				PairFeatureVector fv = new PairFeatureVector(doc, e1, e2, tlink.getRelType(), tsignalList, csignalList);	
				
				if (fv.getPairType().equals(PairType.event_event)) {
					EventEventFeatureVector eefv = new EventEventFeatureVector(fv);
					EventEventRelationRule eeRule = new EventEventRelationRule((Event) eefv.getE1(), (Event) eefv.getE2(), 
							doc, eefv.getMateDependencyPath());
					if (!eeRule.getRelType().equals("O")) {
						ee.add(eefv.getE1().getID() + "\t" + eefv.getE2().getID() + "\t" + 
								eefv.getLabel() + "\t" + eeRule.getRelType());
					} else {
						ee.add(eefv.getE1().getID() + "\t" + eefv.getE2().getID() + "\t" + 
								eefv.getLabel() + "\tNONE");
					}
				}
			}
		}
		return ee;
	}
	
	public static String getEventCausalityRule(EventEventFeatureVector eefv) throws Exception {
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
	
	public String getEventRule(Event e1, Event e2, Doc doc, String depPath) {
		String[] aspectual_initiation = {"begin", "start", "initiate", "commence", "launch"};
		String[] aspectual_termination = {"stop", "finish", "terminate", "cease"};
		String[] aspectual_continuation = {"continue", "retain", "keep"};
		String[] aspectual_remain = {"remain"};
		
		List<String> asp_init_list = Arrays.asList(aspectual_initiation);
		List<String> asp_term_list = Arrays.asList(aspectual_termination);
		List<String> asp_cont_list = Arrays.asList(aspectual_continuation);
		List<String> asp_remain_list = Arrays.asList(aspectual_remain);
				
		if (e1.getSentID().equals(e2.getSentID())) {	//in the same sentence
			Sentence s = doc.getSentences().get(e1.getSentID());
			ArrayList<String> entArr = s.getEntityArr();
			int eidx1 = entArr.indexOf(e1.getID());
			int eidx2 = entArr.indexOf(e2.getID());
			
			if (eidx1 < eidx2 && eidx2-eidx1 == 1
					&& depPath.equals("LGS-PMOD")) {
				return "AFTER";
				
			} else if (eidx1 < eidx2 && eidx2-eidx1 == 1
					&& (depPath.equals("OPRD-IM") 
//							|| depPath.equals("OPRD")
						)
					) {
				if (asp_init_list.contains(doc.getTokens().get(e1.getStartTokID()).getLemma())) {
					return "BEGINS";
					
				} else if (asp_term_list.contains(doc.getTokens().get(e1.getStartTokID()).getLemma())) {
					return "ENDS";
					
				} else if (asp_cont_list.contains(doc.getTokens().get(e1.getStartTokID()).getLemma())) {
					return "INCLUDES";
					
				} else if (asp_remain_list.contains(doc.getTokens().get(e1.getStartTokID()).getLemma())) {
					return "DURING_INV";
					
				} else {
					if (e1.getAspect().equals("PERFECTIVE_PROGRESSIVE")) {
						return "SIMULTANEOUS";
					} else {
						return "BEFORE";
					}
				}
//			} else if (eidx1 < eidx2 && eidx2-eidx1 == 1
//					&& depPath.equals("OPRD")
//					) {
//				if (asp_term_list.contains(doc.getTokens().get(e1.getStartTokID()).getLemma())) {
//					return "ENDS";
//					
//				} 
			} else if (depPath.equals("OBJ-SUB")
					&& e1.getEventClass().equals("REPORTING")) {
				if (!reportingEventRules(e1, e2).equals("O")) 
					return reportingEventRules(e1, e2);
				
			} else if (depPath.equals("OBJ_REV")
					&& e2.getEventClass().equals("REPORTING")) {
				if (!reportingEventRules(e2, e1).equals("O")) 
					return reportingEventRules(e2, e1);
				
			} else if (depPath.equals("LOC-PMOD")) {
				return "IS_INCLUDED";
				
			} else if (depPath.equals("PMOD-LOC")) {
				return "INCLUDES";
				
			} else if (!reichenbachRules(e1, e2).equals("O")) {
				return reichenbachRules(e1, e2);
			}
		}
		return "O";
	}
	
	//Reporting Event with Dominated Event rules (Chambers et al., 2014)
	public String reportingEventRules(Event gov, Event dep) {
		if (gov.getTense().equals("PRESENT") 
				&& dep.getTense().equals("PAST")) {
			return "AFTER";
			
		} else if (gov.getTense().equals("PRESENT") 
				&& dep.getTense().equals("PRESENT")
				&& dep.getAspect().equals("PERFECTIVE")) {
			return "AFTER";
			
		} else if (gov.getTense().equals("PRESENT") 
				&& dep.getTense().equals("FUTURE")) {
			return "BEFORE";
			
		} else if (gov.getTense().equals("PAST") 
				&& dep.getTense().equals("PAST")
				&& dep.getAspect().equals("PERFECTIVE")) {
			return "AFTER";
			
		} else if (gov.getTense().equals("PAST") 
				&& dep.getTense().equals("PAST")
				&& dep.getAspect().equals("PROGRESSIVE")) {
			return "IS_INCLUDED";
		}
		
		return "O";
	}
	
	//Reichenbach Rules (Chambers et al., 2014)
	public String reichenbachRules(Event e1, Event e2) {
		if (e1.getTense().equals("PAST") 
				&& e1.getAspect().equals("NONE")
				&& e2.getTense().equals("PAST")
				&& e2.getAspect().equals("PERFECTIVE")) {
			return "AFTER";
			
		} else if (e1.getTense().equals("FUTURE") 
				&& e1.getAspect().equals("NONE")
				&& e2.getTense().equals("PRESENT")
				&& e2.getAspect().equals("PERFECTIVE")) {
			return "AFTER";
			
		} else if (e1.getTense().equals("PAST") 
				&& e1.getAspect().equals("NONE")
				&& e2.getTense().equals("FUTURE")
				&& e2.getAspect().equals("NONE")) {
			return "BEFORE";
			
		} 
		
		return "O";
	}
	
	public String getEventRule(Event e1, Event e2, Doc doc, 
			Map<String, String> etanchor, 
			Map<String, String> etbefore, Map<String, String> etafter, 
			Map<String, String> ttlinks) {
		String tt = "";
		if (etanchor.containsKey(e1.getID()) && etanchor.containsKey(e2.getID())) {
			tt = etanchor.get(e1.getID())+"\t"+etanchor.get(e2.getID());
			if (ttlinks.containsKey(tt)){
				numReason ++;
				return ttlinks.get(tt);
			}
		} else if (etbefore.containsKey(e1.getID()) && etanchor.containsKey(e2.getID())) {
			tt = etbefore.get(e1.getID())+"\t"+etanchor.get(e2.getID());
			if (ttlinks.containsKey(tt) && ttlinks.get(tt).equals("BEFORE")) {
				numReason ++;
				return ttlinks.get(tt);
			}
		} else if (etafter.containsKey(e1.getID()) && etanchor.containsKey(e2.getID())) {
			tt = etafter.get(e1.getID())+"\t"+etanchor.get(e2.getID());
			if (ttlinks.containsKey(tt) && ttlinks.get(tt).equals("AFTER")) {
				numReason ++;
				return ttlinks.get(tt);
			}
		} else if (etanchor.containsKey(e1.getID()) && etafter.containsKey(e2.getID())) {
			tt = etanchor.get(e1.getID())+"\t"+etafter.get(e2.getID());
			if (ttlinks.containsKey(tt) && ttlinks.get(tt).equals("BEFORE")) {
				numReason ++;
				return ttlinks.get(tt);
			}
		} else if (etanchor.containsKey(e1.getID()) && etbefore.containsKey(e2.getID())) {
			tt = etanchor.get(e1.getID())+"\t"+etbefore.get(e2.getID());
			if (ttlinks.containsKey(tt) && ttlinks.get(tt).equals("AFTER")) {
				numReason ++;
				return ttlinks.get(tt);
			}
		}
		return "O";
	}
	
	public String getRelType() {
		return relType;
	}

	public void setRelType(String relType) {
		this.relType = relType;
	}

	public Boolean getIdentityRel() {
		return identityRel;
	}

	public void setIdentityRel(Boolean identity) {
		this.identityRel = identity;
	}

}
