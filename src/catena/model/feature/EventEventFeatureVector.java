package catena.model.feature;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import catena.model.feature.FeatureEnum.FeatureName;
import catena.parser.entities.Doc;
import catena.parser.entities.Entity;
import catena.parser.entities.Event;
import catena.parser.entities.TemporalRelation;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.Lin;

public class EventEventFeatureVector extends PairFeatureVector{
	
	public static List<String> fields;

	public EventEventFeatureVector(Doc doc, Entity e1, Entity e2, String label, TemporalSignalList tempSignalList, CausalSignalList causalSignalList) {
		super(doc, e1, e2, label, tempSignalList, causalSignalList);
		orderPair();
		//fields = Arrays.asList(new String[50]);
		fields = new ArrayList<String>();
	}
	
	public EventEventFeatureVector(PairFeatureVector fv) {
		super(fv.getDoc(), fv.getE1(), fv.getE2(), fv.getVectors(), fv.getLabel(), fv.getTempSignalList(), fv.getCausalSignalList());
		orderPair();
		//fields = Arrays.asList(new String[50]);
		fields = new ArrayList<String>();
	}
	
	public void orderPair() {
		//if in timex-event order, switch!
		if (getOrder().equals("AFTER")) {
			Entity temp = e1;
			this.setE1(e2);
			this.setE2(temp);
			this.setLabel(TemporalRelation.getInverseRelation(label));
		}
	}
	
	public Double getWordSimilarity() {
		ILexicalDatabase db = new NictWordNet();
		RelatednessCalculator rc = new Lin(db);
		return rc.calcRelatednessOfWords(getTokenAttribute(e1, FeatureName.lemma), getTokenAttribute(e2, FeatureName.lemma));
	}
	
	public String getDiscreteWordSimilarity() {
		if (getWordSimilarity() > 1) return "SIM1";
		else if (getWordSimilarity() <=1 && getWordSimilarity() > 0.5) return "SIMHIGH";
		else if (getWordSimilarity() <=0.5 && getWordSimilarity() > 0.0) return "SIMLOW";
		else if (getWordSimilarity() <= 0.0) return "SIM0";
		else return "SIM";
	}
	
	public Double getDiscreteDoubleWordSimilarity() {
		if (getWordSimilarity() > 1) return 1.0;
		else if (getWordSimilarity() <=1 && getWordSimilarity() > 0.5) return 0.75;
		else if (getWordSimilarity() <=0.5 && getWordSimilarity() > 0.0) return 0.25;
		else if (getWordSimilarity() <= 0.0) return 0.0;
		else return 0.0;
	}
	
	public ArrayList<String> getEntityAttributes() {
		ArrayList<String> entityAttrs = new ArrayList<String>();
		entityAttrs.add(getEntityAttribute(e1, FeatureName.eventClass));
		entityAttrs.add(getEntityAttribute(e2, FeatureName.eventClass));
		entityAttrs.add(getEntityAttribute(e1, FeatureName.tense));
		entityAttrs.add(getEntityAttribute(e2, FeatureName.tense));
		entityAttrs.add(getEntityAttribute(e1, FeatureName.aspect));
		entityAttrs.add(getEntityAttribute(e2, FeatureName.aspect));
		entityAttrs.add(getEntityAttribute(e1, FeatureName.polarity));
		entityAttrs.add(getEntityAttribute(e2, FeatureName.polarity));
		return entityAttrs;
	}
	
	public ArrayList<String> getCombinedEntityAttributes() {
		ArrayList<String> entityAttrs = new ArrayList<String>();
		entityAttrs.add(getEntityAttribute(e1, FeatureName.eventClass) + "|" + getEntityAttribute(e2, FeatureName.eventClass));
		entityAttrs.add(getEntityAttribute(e1, FeatureName.tense) + "-" + getEntityAttribute(e1, FeatureName.aspect) + "|" + 
				getEntityAttribute(e2, FeatureName.tense) + "-" + getEntityAttribute(e2, FeatureName.aspect));
		entityAttrs.add(getEntityAttribute(e1, FeatureName.polarity) + "|" + getEntityAttribute(e2, FeatureName.polarity));
		return entityAttrs;
	}
	
	public ArrayList<String> getSameEntityAttributes() {
		ArrayList<String> entityAttrs = new ArrayList<String>();
		entityAttrs.add(getEntityAttribute(e1, FeatureName.eventClass).equals(getEntityAttribute(e2, FeatureName.eventClass)) ? "TRUE" : "FALSE");
		entityAttrs.add((getEntityAttribute(e1, FeatureName.tense) + "-" + getEntityAttribute(e1, FeatureName.aspect)).equals(getEntityAttribute(e2, FeatureName.tense) + "-" + getEntityAttribute(e2, FeatureName.aspect)) ? "TRUE" : "FALSE");
		entityAttrs.add(getEntityAttribute(e1, FeatureName.polarity).equals(getEntityAttribute(e2, FeatureName.polarity)) ? "TRUE" : "FALSE");
		return entityAttrs;
	}
	
	public Boolean isCoreference() {
		return ((Event)e1).getCorefList().contains(e2.getID());
	}
	
	private static String reversePath(String path) {
	    StringBuilder sb = new StringBuilder(path.length() + 1);
	    String[] words = path.split("-");
	    for (int i = words.length - 1; i >= 0; i--) {
	        sb.append(words[i]).append('-');
	    }
	    sb.setLength(sb.length() - 1);  // Strip trailing space
	    return sb.toString();
	}
	
	public String getSimplifiedMateDependencyPath() {
		String depEvPathStr = getMateDependencyPath();
		
		String depEvPath = "O";
		for (String s : this.dep_event_path) {
			if (depEvPathStr.equals(s)
					|| depEvPathStr.contains(s)
				) {
				depEvPath = s;
				break;
			}
		}
		return depEvPath;
	}
	
	public String getMateDependencyPath() {
		//Assuming that event is always single-token
		if (isSameSentence()) {
			ArrayList<String> tokenArr1 = new ArrayList<String>();
			ArrayList<String> tokenArr2 = new ArrayList<String>();
			String tokenID1 = e1.getStartTokID();
			String tokenID2 = e2.getStartTokID();
			
			tokenArr1.add(tokenID1);
			tokenArr2.add(tokenID2);
			
			for (String govID : tokenArr1) {
				List<String> paths = new ArrayList<String>();
				List<String> visited = new ArrayList<String>();
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
			for (String govID : tokenArr2) {
				List<String> paths = new ArrayList<String>();
				List<String> visited = new ArrayList<String>();
				generateDependencyPath(govID, tokenArr1, paths, "", visited);
				if (!paths.isEmpty()) {
					return reversePath(paths.get(0).substring(1));
				}
				if (getMateCoordVerb(govID) != null) {
					generateDependencyPath(getMateCoordVerb(govID), tokenArr1, paths, "", visited);
					if (!paths.isEmpty()) {
						return reversePath(paths.get(0).substring(1));
					}
				}
			}
			
			tokenArr1.clear();
			tokenArr2.clear();			
			
			if (getTokenAttribute(e1, FeatureName.mainpos).equals("v")) {
				tokenArr1.add(getMateHeadVerb(tokenID1));
			} else if (getTokenAttribute(e1, FeatureName.mainpos).equals("adj") &&
				getMateVerbFromAdj(tokenID1) != null) {
				tokenArr1.add(getMateVerbFromAdj(tokenID1));
			} else {
				tokenArr1.add(tokenID1);
			}
			if (getTokenAttribute(e2, FeatureName.mainpos).equals("v")) {
				tokenArr2.add(getMateHeadVerb(tokenID2));
			} else if (getTokenAttribute(e2, FeatureName.mainpos).equals("adj") &&
				getMateVerbFromAdj(tokenID2) != null) {
				tokenArr2.add(getMateVerbFromAdj(tokenID2));
			} else {
				tokenArr2.add(tokenID2);
			}
			
			for (String govID : tokenArr1) {
				List<String> paths = new ArrayList<String>();
				List<String> visited = new ArrayList<String>();
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
			for (String govID : tokenArr2) {
				List<String> paths = new ArrayList<String>();
				List<String> visited = new ArrayList<String>();
				generateDependencyPath(govID, tokenArr1, paths, "", visited);
				if (!paths.isEmpty()) {
					return reversePath(paths.get(0).substring(1));
				}
				if (getMateCoordVerb(govID) != null) {
					generateDependencyPath(getMateCoordVerb(govID), tokenArr1, paths, "", visited);
					if (!paths.isEmpty()) {
						return reversePath(paths.get(0).substring(1));
					}
				}
			}
		} 
		return "O";
	}
	
	public String getMateMainVerb() {
		return super.getMateMainVerb(e1) + "|" + super.getMateMainVerb(e2);
	}
	
	public ArrayList<String> getTemporalMarker() throws IOException {
		ArrayList<String> tMarkers = new ArrayList<String>();
		Marker m = super.getTemporalConnective();
		if (m.getText().equals("O")) m = super.getTemporalSignal();
		tMarkers.add(m.getCluster().replace(" ", "_") + "|" + m.getPosition());
		tMarkers.add(m.getDepRelE1() + "|" + m.getDepRelE2());
		return tMarkers;
	}
	
	public ArrayList<String> getCausalMarker() throws IOException {
		ArrayList<String> tMarkers = new ArrayList<String>();
		Marker m = super.getCausalConnective();
		if (m.getText().equals("O")) m = super.getCausalSignal();
		if (m.getText().equals("O")) m = super.getCausalVerb();
		tMarkers.add(m.getCluster().replace(" ", "_") + "|" + m.getPosition());
		tMarkers.add(m.getDepRelE1() + "|" + m.getDepRelE2());
		return tMarkers;
	}

}
