package catena.model.feature;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import catena.model.feature.FeatureEnum.FeatureName;
import catena.parser.entities.Doc;
import catena.parser.entities.Entity;
import catena.parser.entities.Event;
import catena.parser.entities.Sentence;
import catena.parser.entities.TemporalRelation;
import catena.parser.entities.Timex;

public class EventTimexFeatureVector extends PairFeatureVector{
	
	public static List<String> fields;

	public EventTimexFeatureVector(Doc doc, Entity e1, Entity e2, String label, TemporalSignalList tempSignalList, CausalSignalList causalSignalList) {
		super(doc, e1, e2, label, tempSignalList, causalSignalList);
		orderPair();
		//fields = Arrays.asList(new String[50]);
		fields = new ArrayList<String>();
	}
	
	public EventTimexFeatureVector(PairFeatureVector fv) {
		super(fv.getDoc(), fv.getE1(), fv.getE2(), fv.getVectors(), fv.getLabel(), fv.getTempSignalList(), fv.getCausalSignalList());
		orderPair();
		//fields = Arrays.asList(new String[50]);
		fields = new ArrayList<String>();
	}
	
	public void orderPair() {
		//if in timex-event order, switch!
		if (e1 instanceof Timex && e2 instanceof Event) {
			Entity temp = e1;
			this.setE1(e2);
			this.setE2(temp);
			this.setLabel(TemporalRelation.getInverseRelation(label));
		}
	}
	
	public ArrayList<String> getEntityAttributes() {
		ArrayList<String> entityAttrs = new ArrayList<String>();
		
		entityAttrs.add(getEntityAttribute(e1, FeatureName.eventClass));
		entityAttrs.add(getEntityAttribute(e1, FeatureName.tense) + "|" + getEntityAttribute(e1, FeatureName.aspect));
		entityAttrs.add(getEntityAttribute(e1, FeatureName.polarity));
		
		entityAttrs.add(getEntityAttribute(e2, FeatureName.timexType) + "|" + 
				getEntityAttribute(e2, FeatureName.timexValueTemplate));
		//entityAttrs.add(getEntityAttribute(e2, Feature.timexValue));
		//entityAttrs.add(getEntityAttribute(e2, Feature.timexValueTemplate));
		entityAttrs.add(getEntityAttribute(e2, FeatureName.dct));
		
		return entityAttrs;
	}
	
	public String getMateDependencyPath() {
		//Assuming that the pair is already in event-timex order
		if ((e2 instanceof Timex && ((Timex)e2).isDct()) || (e2 instanceof Timex && ((Timex)e2).isEmptyTag())) {
			return "O";
		} else if (isSameSentence()){
			ArrayList<String> tokenArr1 = getTokenIDArr(e1.getStartTokID(), e1.getEndTokID());
			ArrayList<String> tokenArr2 = getTokenIDArr(e2.getStartTokID(), e2.getEndTokID());
			
			for (String govID : tokenArr1) {
				List<String> paths = new ArrayList<String>();
				List<String> visited = new ArrayList<String>();
				
				if (getTokenAttribute(e1, FeatureName.mainpos).equals("v")) {
					govID = getMateHeadVerb(govID);
				} else if (getTokenAttribute(e1, FeatureName.mainpos).equals("adj") &&
					getMateVerbFromAdj(govID) != null) {
					govID = getMateVerbFromAdj(govID);
				}
				generateDependencyPath(govID, tokenArr2, paths, "", visited);
				if (!paths.isEmpty()) {
					return paths.get(0).substring(1);
				}
				if (getMateCoordVerb(govID) != null) {
					generateDependencyPath(getMateCoordVerb(govID), tokenArr2, paths, "", visited);
					if (!paths.isEmpty()) {
						return paths.get(0).substring(1);
					}
				}
			}
		} 
		return "O";
	}
	
	public String getMateMainVerb() {
		return super.getMateMainVerb(e1);
	}
	
	public ArrayList<String> getTemporalMarker() throws IOException {
		ArrayList<String> tMarkers = new ArrayList<String>();
		
		//Assuming that the pair is already in event-timex order
		if ((e2 instanceof Timex && ((Timex)e2).isDct()) || (e2 instanceof Timex && ((Timex)e2).isEmptyTag()) ||
			!isSameSentence()) {
			tMarkers.add("O|O");
			tMarkers.add("O");
		} else {	
			Marker m = super.getTemporalConnective();
			if (m.getText().equals("O")) m = super.getTemporalSignal();
			tMarkers.add(m.getCluster().replace(" ", "_") + "|" + m.getPosition());
			tMarkers.add(m.getDepRelE1() + "|" + m.getDepRelE2());
			return tMarkers;
		}
		return tMarkers;
	}
	
	/**
	 * Feature for timespan timexes
	 * e.g. "between" tmx1 "and" tmx2, "from" tmx1 "to" tmx 2, tmx1 "-" tmx2, tmx1 "until" tmx2
	 *      we said that timex is tmx1 is "TMX-BEGIN" and tmx2 is "TMX-END"
	 * @return String timexRule
	 */
	public String getTimexRule() {
		//Assuming that the pair is already in event-timex order
		if ((e2 instanceof Timex && ((Timex)e2).isDct()) || (e2 instanceof Timex && ((Timex)e2).isEmptyTag())) {
			return "O";
		} else {
			Sentence s = doc.getSentences().get(e2.getSentID());
			ArrayList<String> entArr = s.getEntityArr();
			int eidx = entArr.indexOf(e2.getID());
			
			int tidxStart = doc.getTokens().get(e2.getStartTokID()).getIndex();
			int tidxStartSent = doc.getTokens().get(s.getStartTokID()).getIndex();
			
			if (tidxStart > tidxStartSent) {
				
				String depPath = getMateDependencyPath();
				if (depPath.contains("TMP-PMOD") 
						&& (!depPath.contains("OBJ")
								&& !depPath.contains("SUB")
								&& !depPath.contains("NMOD"))) {
					String beforeTmx = doc.getTokens().get(doc.getTokenArr().get(tidxStart-1)).getTokenAttribute(FeatureName.lemma);
					if ((beforeTmx.equals("for")
							|| beforeTmx.equals("during"))
							&& ((Timex)e2).getType().equals("DURATION")) {
						return "TMX-SIM";
					} else if ((beforeTmx.equals("in"))
							&& ((Timex)e2).getType().equals("DURATION")) {
						return "TMX-IN";
					} else if ((beforeTmx.equals("in") || beforeTmx.equals("at") || beforeTmx.equals("on"))
							&& (((Timex)e2).getType().equals("DATE") || ((Timex)e2).getType().equals("TIME"))) {
						return "TMX-IN";
					}
				}
				
				if (eidx < entArr.size()-1 && doc.getEntities().get(entArr.get(eidx+1)) instanceof Timex) {
					Entity tmx2 = doc.getEntities().get(entArr.get(eidx+1));
					int tmx2Idx = doc.getTokens().get(tmx2.getStartTokID()).getIndex();
					String beforeTmx1 = doc.getTokens().get(doc.getTokenArr().get(tidxStart-1)).getTokenAttribute(FeatureName.lemma);
					String beforeTmx2 = doc.getTokens().get(doc.getTokenArr().get(tmx2Idx-1)).getTokenAttribute(FeatureName.lemma);
					
					if (beforeTmx1.equals("between") && beforeTmx2.equals("and")) {
						return "TMX-BEGIN";
					} else if (beforeTmx1.equals("from") && 
							(beforeTmx2.equals("to") || beforeTmx2.equals("until") || beforeTmx2.equals("till"))) {
						return "TMX-BEGIN";
					} else if (beforeTmx2.equals("-")) {
						return "TMX-BEGIN";
					} else if (beforeTmx2.equals("until") || beforeTmx2.equals("until")) {
						return "TMX-BEGIN";
					}
				} else if (eidx > 0 && doc.getEntities().get(entArr.get(eidx-1)) instanceof Timex) {
					Entity tmx1 = doc.getEntities().get(entArr.get(eidx-1));
					int tmx1Idx = doc.getTokens().get(tmx1.getStartTokID()).getIndex();
					String beforeTmx1 = doc.getTokens().get(doc.getTokenArr().get(tmx1Idx-1)).getTokenAttribute(FeatureName.lemma);
					String beforeTmx2 = doc.getTokens().get(doc.getTokenArr().get(tidxStart-1)).getTokenAttribute(FeatureName.lemma);
					
					if (beforeTmx1.equals("between") && beforeTmx2.equals("and")) {
						return "TMX-END";
					} else if (beforeTmx1.equals("from") && 
							(beforeTmx2.equals("to") || beforeTmx2.equals("until") || beforeTmx2.equals("till"))) {
						return "TMX-END";
					} else if (beforeTmx2.equals("-")) {
						return "TMX-END";
					} else if (beforeTmx2.equals("until") || beforeTmx2.equals("until")) {
						return "TMX-END";
					}
				}
			}
			return "O";
		}
	}

}
