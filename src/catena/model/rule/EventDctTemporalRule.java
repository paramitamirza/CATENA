package catena.model.rule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import catena.model.feature.CausalSignalList;
import catena.model.feature.EventTimexFeatureVector;
import catena.model.feature.PairFeatureVector;
import catena.model.feature.TemporalSignalList;
import catena.model.feature.FeatureEnum.FeatureName;
import catena.model.feature.FeatureEnum.PairType;
import catena.parser.entities.Doc;
import catena.parser.entities.Entity;
import catena.parser.entities.EntityEnum;
import catena.parser.entities.Event;
import catena.parser.entities.TemporalRelation;
import catena.parser.entities.Timex;

public class EventDctTemporalRule extends EventTimexTemporalRule {
	
	public EventDctTemporalRule(Event e1, Timex t2, Doc doc, String depPath,
			Boolean measureRel) {
		super(e1, t2, doc, depPath, measureRel);
	}
	
	public EventDctTemporalRule(Event ev, Timex tmx, Doc doc, String depPath) {		
		super(ev, tmx, doc, depPath);
	}
	
	public static Map<String,String> getEventDctRuleRelation(Doc doc) throws Exception {
		return getEventDctRuleRelation(doc, false);
	}
	
	public static Map<String,String> getEventDctRuleRelation(Doc doc, boolean vague) throws Exception {
		List<String> et = getEventDctTlinksPerFile(doc, true, vague);
		Map<String,String> etlinks = new HashMap<String,String>();
		
		for (String etPair : et) {
			String source = etPair.split("\t")[0];
			String target = etPair.split("\t")[1];
			String relType = etPair.split("\t")[3];
			etlinks.put(source + "," + target, relType);
			etlinks.put(target + "," + source, TemporalRelation.getInverseRelation(relType));
		}
		return etlinks;
	}
	
	public static List<String> getEventDctTlinksPerFile(Doc doc, boolean goldCandidate) throws Exception {
		return getEventDctTlinksPerFile(doc, goldCandidate, false);
	}
	
	public static List<String> getEventDctTlinksPerFile(Doc doc, boolean goldCandidate, boolean vague) throws Exception {
		List<String> et = new ArrayList<String>();
		
		TemporalSignalList tsignalList = new TemporalSignalList(EntityEnum.Language.EN);
		CausalSignalList csignalList = new CausalSignalList(EntityEnum.Language.EN);
		
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
				
				if (fv.getPairType().equals(PairType.event_timex)) {
					EventTimexFeatureVector etfv = new EventTimexFeatureVector(fv);
					
					if (((Timex) etfv.getE2()).isDct()) {
						EventDctTemporalRule etRule = new EventDctTemporalRule((Event) etfv.getE1(), (Timex) etfv.getE2(), 
								doc, etfv.getMateDependencyPath());
						if (vague) {
							if (etfv.getTokenAttribute(etfv.getE1(), FeatureName.mainpos).equals("adj")) {
								et.add(etfv.getE1().getID() + "\t" + etfv.getE2().getID() + "\t" + 
										etfv.getLabel() + "\t" + "VAGUE");
							} else {
								if (!etRule.getRelType().equals("O")) {
									et.add(etfv.getE1().getID() + "\t" + etfv.getE2().getID() + "\t" + 
											etfv.getLabel() + "\t" + etRule.getRelType());
		//						} else {
		//							et.add(etfv.getE1().getID() + "\t" + etfv.getE2().getID() + "\t" + 
		//									etfv.getLabel() + "\tNONE");
								}
							}
						} else {
							if (!etRule.getRelType().equals("O")) {
								et.add(etfv.getE1().getID() + "\t" + etfv.getE2().getID() + "\t" + 
										etfv.getLabel() + "\t" + etRule.getRelType());
	//						} else {
	//							et.add(etfv.getE1().getID() + "\t" + etfv.getE2().getID() + "\t" + 
	//									etfv.getLabel() + "\tNONE");
							}
						}
						
					}
				}
			}
		}
		return et;
	}

}
