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

public class EventEventCausalRule {
	
	private String relType;
	private Boolean identityRel=true;
	public static Integer numReason=0;
	
	private static String[] ruleTlinks = {"BEFORE", "AFTER", "SIMULTANEOUS", "INCLUDES", "IS_INCLUDED"};
	public static List<String> ruleTlinkTypes = Arrays.asList(ruleTlinks);
	
	public EventEventCausalRule(PairFeatureVector fv) {
		
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
