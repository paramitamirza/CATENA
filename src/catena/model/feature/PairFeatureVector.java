package catena.model.feature;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import catena.model.feature.FeatureEnum.*;
import catena.parser.entities.*;
import edu.stanford.nlp.util.StringUtils;

public class PairFeatureVector {
	
	protected Doc doc;
	private ArrayList<String> vectors;
	private double[] features;
	protected Entity e1;
	protected Entity e2;
	protected String label;
	protected String predLabel;
	private PairType pairType;
	private TemporalSignalList tempSignalList;
	private CausalSignalList causalSignalList;
	
	private String[] pos = {"AJ0", "AJC", "AJS", "AT0", "AV0", "AVP", "AVQ", "CJC", "CJS", "CJT", "CRD", 
			"DPS", "DT0", "DTQ", "EX0", "ITJ", "NN0", "NN1", "NN2", "NP0", "ORD", "PNI", "PNP", "PNQ", 
			"PNX", "POS", "PRF", "PRP", "PUL", "PUN", "PUQ", "PUR", "TO0", "UNC", "VBB", "VBD", "VBG", 
			"VBI", "VBN", "VBZ", "VDB", "VDD", "VDG", "VDI", "VDN", "VDZ", "VHB", "VHD", "VHG", "VHI", 
			"VHN", "VHZ", "VM0", "VVB", "VVD", "VVG", "VVI", "VVN", "VVZ", "XX0", "ZZ0"};
	private String[] main_pos = {"v", "n", "art", "det", "adj", "adv", "conj", "crd", "ord", "pron",
			"prep", "to", "pos", "punc", "neg"};
	private String[] chunk = {"B-VP", "I-VP", "B-NP", "I-NP", "B-ADJP", "I-ADJP", "B-ADVP", "I-ADVP", 
			"B-PP", "I-PP", "B-SBAR", "I-SBAR"};
	private String[] ent_order = {"BEFORE", "AFTER"};
	private String[] ev_class = {"REPORTING", "PERCEPTION", "ASPECTUAL", "I_ACTION", "I_STATE", "STATE", "OCCURRENCE"};
	private String[] ev_tense = {"PAST", "PRESENT", "FUTURE", "NONE", "INFINITIVE", "PRESPART", "PASTPART"};
	private String[] ev_aspect = {"PROGRESSIVE", "PERFECTIVE", "PERFECTIVE_PROGRESSIVE", "NONE"};
	private String[] tmx_type = {"DATE", "TIME", "DURATION", "SET"};
	private String[] marker_position = {"BETWEEN", "BEFORE", "AFTER", "BEGIN", "BEGIN-BETWEEN", "BEGIN-BEFORE"};
	private String[] timex_rule = {"TMX-BEGIN", "TMX-END", "TMX-SIM", "TMX-IN"};
	private String[] temp_signal_event = {"as soon as", "as long as", "at the same time", "followed by", 
			"prior to", "still", "during", "while", "when", "immediately", "after", 
			"until", "if", "eventually", "then", "finally", "afterwards", "initially", "next", "once", 
			"since", "simultaneously", "formerly", "former", "meanwhile", "later", "into", "follow", 
			"earlier", "previously", "before", "as", "already"};
	private String[] temp_signal_timex = {"at", "by", "in", "on", "for", "from", "to", "during", 
			"between", "after", "before", "up to", "within", "until", "since", "still", "recently", 
			"formerly", "former", "early", "over", "next", "later", "lately", "immediately", 
			"earlier", "ago"};
	private String[] caus_signal = {"so that", "because of", "due to", "in consequence of", "in response to",
			"in exchange for", "in response", "in order to", "as a result of", "as a result", "for reason",
			"is why", "therefore", "because", "since", "as", "so", "by", "from"};
	private String[] caus_verb = {"CAUSE", "CAUSE-AMBIGUOUS", "ENABLE", "PREVENT", "PREVENT-AMBIGUOUS", "AFFECT", "LINK"};
	
	protected String[] dep_event_path = {"COORD-CONJ", "TMP-SUB", "OPRD", "OPRD-IM", "OBJ-SUB",
			"ADV", "OBJ", "SBJ", "ADV-SUB", "VC", "LGS-PMOD", "ADV-PMOD", "LOC-PMOD",
			"CONJ-COORD", "SUB-TMP", "IM-OPRD", "SUB-OBJ", "SUB-ADV", "PMOD-LGS", "PMOD-ADV", "PMOD-LOC"};
	private String[] dep_signal_path = {"SBJ", "OBJ", "OPRD", "IM", "ADV", "PRP",
			"SUB", "PRD", "TMP", "PMOD", "LGS", "DEP", "LOC", "APPO"};
	private String[] dep_signal_path2 = {"SBJ|OBJ", "SBJ|OPRD-IM", "SBJ|OPRD", "ADV|OBJ", "ADV|OPRD-IM", "ADV|OPRD", 
			"PRP|SUB", "TMP|SUB", "ADV|SUB", "PRD|PMOD", "PRP|PMOD", "LGS|PMOD", "ADV|PMOD", "PRP|IM"};
	
	private String[] temp_rel_type = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
			"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
	private List<String> temp_rel_type_list = Arrays.asList(temp_rel_type);
	
	private String[] caus_rel_type = {"CLINK", "CLINK-R", "NONE"};
	private List<String> caus_rel_type_list = Arrays.asList(caus_rel_type);
	
	private String[] temp_dense_rel_type = {"BEFORE", "AFTER", "SIMULTANEOUS", "INCLUDES", "IS_INCLUDED", "VAGUE"};
	private List<String> temp_dense_rel_type_list = Arrays.asList(temp_dense_rel_type);
	
	private String[] modals = {"will", "would", "can", "could", "may", "might", "shall", "should", "must"};
	private List<String> modalVerbs = Arrays.asList(modals);
	
	public PairFeatureVector() {
		this.setVectors(new ArrayList<String>());
	}
	
	public PairFeatureVector(Doc doc, Entity e1, Entity e2, String label, TemporalSignalList tempSignalList, CausalSignalList causalSignalList) {
		this.setDoc(doc);
		this.setE1(e1);
		this.setE2(e2);
		this.setLabel(label);
		this.setVectors(new ArrayList<String>());
		if (e1 instanceof Event && e2 instanceof Event) {
			this.setPairType(PairType.event_event);
		} else if ((e1 instanceof Timex && e2 instanceof Event) || 
				(e1 instanceof Event && e2 instanceof Timex)) {
			this.setPairType(PairType.event_timex);
		} else if (e1 instanceof Timex && e2 instanceof Timex) {
			this.setPairType(PairType.timex_timex);
		}
		this.setTempSignalList(tempSignalList);
		this.setCausalSignalList(causalSignalList);
	}
	
	public PairFeatureVector(Doc doc, Entity e1, Entity e2, ArrayList<String> vectors, String label, TemporalSignalList tempSignalList, CausalSignalList causalSignalList) {
		this.setDoc(doc);
		this.setE1(e1);
		this.setE2(e2);
		this.setVectors(vectors);
		this.setLabel(label);
		if (e1 instanceof Event && e2 instanceof Event) {
			this.setPairType(PairType.event_event);
		} else if ((e1 instanceof Timex && e2 instanceof Event) || 
				(e1 instanceof Event && e2 instanceof Timex)) {
			this.setPairType(PairType.event_timex);
		} else if (e1 instanceof Timex && e2 instanceof Timex) {
			this.setPairType(PairType.timex_timex);
		}
		this.setTempSignalList(tempSignalList);
		this.setCausalSignalList(causalSignalList);
	}
	
	public PairFeatureVector(Doc doc, Entity e1, Entity e2, String label, int numFeatures,
			TemporalSignalList tempSignalList, CausalSignalList causalSignalList) {
		this.setDoc(doc);
		this.setE1(e1);
		this.setE2(e2);
		this.setLabel(label);
		this.setVectors(new ArrayList<String>());
		this.setFeatures(new double[numFeatures]);
		if (e1 instanceof Event && e2 instanceof Event) {
			this.setPairType(PairType.event_event);
		} else if ((e1 instanceof Timex && e2 instanceof Event) || 
				(e1 instanceof Event && e2 instanceof Timex)) {
			this.setPairType(PairType.event_timex);
		} else if (e1 instanceof Timex && e2 instanceof Timex) {
			this.setPairType(PairType.timex_timex);
		}
		this.setTempSignalList(tempSignalList);
		this.setCausalSignalList(causalSignalList);
	}

	public Doc getDoc() {
		return doc;
	}

	public void setDoc(Doc doc) {
		this.doc = doc;
	}

	public ArrayList<String> getVectors() {
		return vectors;
	}

	public void setVectors(ArrayList<String> vectors) {
		this.vectors = vectors;
	}

	public Entity getE1() {
		return e1;
	}

	public void setE1(Entity e1) {
		this.e1 = e1;
	}

	public Entity getE2() {
		return e2;
	}

	public void setE2(Entity e2) {
		this.e2 = e2;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}
	
	public String getPredLabel() {
		return predLabel;
	}

	public void setPredLabel(String label) {
		this.predLabel = label;
	}
	
	public String printVectors() {
		return String.join("\t", this.vectors);
	}
	
	public String printCSVVectors() {
		String csv = "";
		for (String col : vectors) {
			String s = col.replace(",", "COMMA");
			s = s.replace("'", "QUOT");
			s = s.replace("`", "QUOT");
			csv += s + ",";
		}
		return csv.substring(0, csv.length()-1);
	}
	
	public String printLibSVMVectors() {
		int idx = 1;
		String svm = "";
		svm += this.vectors.get(this.vectors.size()-1);
		for (int i=0; i<this.vectors.size()-1; i++) {
			double val = Double.valueOf(this.vectors.get(i));
			if (val > 0) {
				svm += " " + idx + ":" + String.valueOf(val); 
			}
			idx += 1;
		}
		return svm;
	}

	public PairType getPairType() {
		return pairType;
	}

	public void setPairType(PairType pairType) {
		this.pairType = pairType;
	}

	public TemporalSignalList getTempSignalList() {
		return tempSignalList;
	}

	public void setTempSignalList(TemporalSignalList tempSignalList) {
		this.tempSignalList = tempSignalList;
	}

	public CausalSignalList getCausalSignalList() {
		return causalSignalList;
	}

	public void setCausalSignalList(CausalSignalList causalSignalList) {
		this.causalSignalList = causalSignalList;
	}
	
	protected ArrayList<String> getTokenIDArr(String startTokID, String endTokID) {
		ArrayList<String> tokIDs = new ArrayList<String>();
		int startTokIdx = doc.getTokens().get(startTokID).getIndex();
		int endTokIdx = doc.getTokens().get(endTokID).getIndex();
		for (int i=startTokIdx; i<endTokIdx+1; i++) {
			tokIDs.add(doc.getTokenArr().get(i));
		}
		return tokIDs;
	}
	
	public String getTokenAttribute(Entity e, FeatureName feature) {
		ArrayList<String> attrList = new ArrayList<String>();
		String currAttr;
		if (e instanceof Timex && (((Timex)e).isDct() || ((Timex)e).isEmptyTag())) {
			return "O";
		} else {	
			for (String tokID : getTokenIDArr(e.getStartTokID(), e.getEndTokID())) {
				currAttr = doc.getTokens().get(tokID).getTokenAttribute(feature);
				if (attrList.isEmpty()){
					attrList.add(currAttr);
				} else {
					if (!currAttr.equals(attrList.get(attrList.size()-1))) {
						attrList.add(currAttr);
					}
				}
			}
			if (feature.equals(FeatureName.token) || feature.equals(FeatureName.lemma))
				return String.join(" ", attrList);
			else
				return String.join("_", attrList);
		}
	}
	
	protected String getWholeChunkToken(Entity e) {
		if (e instanceof Timex) {
			if (((Timex)e).isDct() || ((Timex)e).isEmptyTag()) {
				return "O";
			} else {
				return getTokenAttribute(e, FeatureName.token);
			}
		} else {
			List<String> chunkText = new ArrayList<String>();
			
			Token currToken = doc.getTokens().get(e.getStartTokID());
			String currChunk = currToken.getTokenAttribute(FeatureName.chunk);
			if (currChunk.contains("B-") || currChunk.contains("I-")) {
				String currPhrase = currChunk.substring(2);
				if (currChunk.contains("B-")) {
					chunkText.add(currToken.getTokenAttribute(FeatureName.token));
					Token nextToken = doc.getTokens().get(doc.getTokenArr().get(currToken.getIndex()+1));
					String nextChunk = nextToken.getTokenAttribute(FeatureName.chunk);
					String nextPhrase;
					while (nextChunk.contains("I-")) {
						nextPhrase = nextChunk.substring(2);
						if (nextPhrase.equals(currPhrase))
							chunkText.add(nextToken.getTokenAttribute(FeatureName.token));
						nextToken = doc.getTokens().get(doc.getTokenArr().get(nextToken.getIndex()+1));
						nextChunk = nextToken.getTokenAttribute(FeatureName.chunk);
					}
				} else if (currChunk.contains("I-")) {
					chunkText.add(currToken.getTokenAttribute(FeatureName.token));
					Token nextToken = doc.getTokens().get(doc.getTokenArr().get(currToken.getIndex()+1));
					String nextChunk = nextToken.getTokenAttribute(FeatureName.chunk);
					String nextPhrase;
					while (nextChunk.contains("I-")) {
						nextPhrase = nextChunk.substring(2);
						if (nextPhrase.equals(currPhrase))
							chunkText.add(nextToken.getTokenAttribute(FeatureName.token));
						nextToken = doc.getTokens().get(doc.getTokenArr().get(nextToken.getIndex()+1));
						nextChunk = nextToken.getTokenAttribute(FeatureName.chunk);
					}
					Token beforeToken = doc.getTokens().get(doc.getTokenArr().get(currToken.getIndex()-1));
					String beforeChunk = beforeToken.getTokenAttribute(FeatureName.chunk);
					String beforePhrase;
					while (beforeChunk.contains("I-")) {
						beforePhrase = beforeChunk.substring(2);
						if (beforePhrase.equals(currPhrase))
							chunkText.add(0, beforeToken.getTokenAttribute(FeatureName.token));
						beforeToken = doc.getTokens().get(doc.getTokenArr().get(beforeToken.getIndex()-1));
						beforeChunk = beforeToken.getTokenAttribute(FeatureName.chunk);
					}
					if (beforeChunk.contains("B-")) {
						beforePhrase = beforeChunk.substring(2);
						if (beforePhrase.equals(currPhrase))
							chunkText.add(0, beforeToken.getTokenAttribute(FeatureName.token));
					}
				}
			} else {
				chunkText.add(currToken.getTokenAttribute(FeatureName.token));
			}
			
			return String.join(" ", chunkText);
		}
	}
	
	public ArrayList<String> getTokenAttribute(FeatureName feature) {
		ArrayList<String> texts = new ArrayList<String>();
		texts.add(getTokenAttribute(e1, feature));
		texts.add(getTokenAttribute(e2, feature));
		return texts;
	}
	
	public String getCombinedTokenAttribute(FeatureName feature) {
		return getTokenAttribute(e1, feature) + "|" + getTokenAttribute(e2, feature);
	}
	
	public Boolean isSameTokenAttribute(FeatureName feature) {
		String eAttr1 = getTokenAttribute(e1, feature);
		String eAttr2 = getTokenAttribute(e2, feature);
		return (eAttr1.equals(eAttr2));
	}
	
	public Integer getEntityDistance() {
		if ((e1 instanceof Timex && ((Timex)e1).isDct()) || (e2 instanceof Timex && ((Timex)e2).isDct()) ||
			(e1 instanceof Timex && ((Timex)e1).isEmptyTag()) || (e2 instanceof Timex && ((Timex)e2).isEmptyTag())) {
			return -1;
		} else {
			Sentence s1 = doc.getSentences().get(e1.getSentID());
			Sentence s2 = doc.getSentences().get(e2.getSentID());
			if (s1.getID().equals(s2.getID())) {	//same sentence
				int eidx1 = e1.getIndex();
				int eidx2 = e2.getIndex();
				return Math.abs(eidx1 - eidx2)-1; 
			} else {
				return -1;
			}
		}
	}
	
	public Boolean isTimexInBetween() {
		Sentence s1 = doc.getSentences().get(e1.getSentID());
		Sentence s2 = doc.getSentences().get(e2.getSentID());
		if (s1.getID().equals(s2.getID())) {	//same sentence
			int eidx1 = s1.getEntityArr().indexOf(e1.getID());
			int eidx2 = s1.getEntityArr().indexOf(e2.getID());
			for (int i=eidx1+1; i<eidx2; i++) {
				if (doc.getEntities().get(s1.getEntityArr().get(i)) instanceof Timex) {
					return true;
				}
			}
		}
		return false;
	}
	
	public Integer getSentenceDistance() {
		if ((e1 instanceof Timex && ((Timex)e1).isDct()) || (e2 instanceof Timex && ((Timex)e2).isDct()) ||
			(e1 instanceof Timex && ((Timex)e1).isEmptyTag()) || (e2 instanceof Timex && ((Timex)e2).isEmptyTag())) {
			return -1;
		} else {
			int sidx1 = doc.getSentences().get(e1.getSentID()).getIndex();
			int sidx2 = doc.getSentences().get(e2.getSentID()).getIndex();
			return Math.abs(sidx1 - sidx2);
		}
	}
	
	public Boolean isSameSentence() {
		if ((e1 instanceof Timex && ((Timex)e1).isDct()) || (e2 instanceof Timex && ((Timex)e2).isDct()) ||
			(e1 instanceof Timex && ((Timex)e1).isEmptyTag()) || (e2 instanceof Timex && ((Timex)e2).isEmptyTag())) {
			return false;
		} else {
			int sidx1 = doc.getSentences().get(e1.getSentID()).getIndex();
			int sidx2 = doc.getSentences().get(e2.getSentID()).getIndex();
			return (sidx1 == sidx2);
		}
	}
	
	public String getOrder() {
		if ((e1 instanceof Timex && ((Timex)e1).isDct()) || (e2 instanceof Timex && ((Timex)e2).isDct()) ||
			(e1 instanceof Timex && ((Timex)e1).isEmptyTag()) || (e2 instanceof Timex && ((Timex)e2).isEmptyTag())) {
			return "O";
		} else {
			int eidx1 = e1.getIndex();
			int eidx2 = e2.getIndex();
			if (eidx1 - eidx2 < 0) {
				return "BEFORE";
			} else if (eidx1 - eidx2 > 0) {
				return "AFTER";
			}
		}
		return null;
	}
	
	protected String getEntityAttribute(Entity e, FeatureName feature) {
		if (e instanceof Event) {
			if (((Event)e).getAttribute(feature).equals("O")) {
				String relatedTid = null;
				if (doc.getTokens().get(e.getStartTokID()).getMainPos().equals("n")) {
					relatedTid = getMateVerbFromSbjNoun(e.getStartTokID());
					if (relatedTid == null) relatedTid = getMateVerbFromObjNoun(e.getStartTokID());
				} else if (doc.getTokens().get(e.getStartTokID()).getMainPos().equals("adj")) {
					relatedTid = getMateVerbFromAdj(e.getStartTokID());
				}
				if (feature == FeatureName.tense) {
					String tense = getTense(e.getStartTokID());
					if (tense.equals("NONE")) {
						if (relatedTid != null) return getTense(relatedTid);
						else return "NONE";
					} else {
						return tense;
					}
				} else if (feature == FeatureName.aspect) {
					String aspect = getAspect(e.getStartTokID());
					if (aspect.equals("NONE")) {
						if (relatedTid != null) return getAspect(relatedTid);
						else return "NONE";
					} else {
						return aspect;
					}
				} else if (feature == FeatureName.polarity) {
					return getPolarity(e.getStartTokID());
				}
				
			} else {
				return ((Event)e).getAttribute(feature);
			}
		} else if (e instanceof Timex){
			return ((Timex)e).getAttribute(feature);
		}
		return null;
	}
	
	protected String getMateModalVerb(String tokID) {
		String modal = "O";
		Sentence s = doc.getSentences().get(doc.getTokens().get(tokID).getSentID());
		ArrayList<String> tokenArr = getTokenIDArr(s.getStartTokID(), s.getEndTokID());
		for (String tok : tokenArr) {
			if (!tokID.equals(tok) && doc.getTokens().get(tok).getDependencyRel() != null) {
				if (doc.getTokens().get(tok).getDependencyRel().keySet().contains(tokID)
						&& doc.getTokens().get(tok).getDependencyRel().get(tokID).equals("VC") 
						&& tokenArr.indexOf(tok) < tokenArr.indexOf(tokID)) {
					if (modalVerbs.contains(doc.getTokens().get(tok).getLemma())
							|| doc.getTokens().get(tok).getPos().equals("VM0")
							) {
						return doc.getTokens().get(tok).getLemma();
					} else {
						return getMateModalVerb(tok);
					}
				} 
			}
		}
		return modal;
	}
	
	protected String getTense(String tokID) {
		String headTokID = getMateHeadVerb(tokID);
		String posHeadTokID = doc.getTokens().get(headTokID).getPos();
		String lemmaHeadTokID = doc.getTokens().get(getMateHeadVerb(tokID)).getLemma();
		if (lemmaHeadTokID.equals("to") && posHeadTokID.startsWith("TO")) {
			return "INFINITIVE";
		} else if (posHeadTokID.equals("VB")
				|| posHeadTokID.equals("VB")
				|| posHeadTokID.equals("VBI")
				|| posHeadTokID.equals("VDI")
				|| posHeadTokID.equals("VHI")
				|| posHeadTokID.equals("VVI")
				) {
			return "INFINITIVE";
		} else if (posHeadTokID.equals("VBZ") || posHeadTokID.equals("VBP")
				|| posHeadTokID.equals("VBB")
				|| posHeadTokID.equals("VDZ") || posHeadTokID.equals("VDB")
				|| posHeadTokID.equals("VHZ") || posHeadTokID.equals("VHB")
				|| posHeadTokID.equals("VVZ") || posHeadTokID.equals("VVB")
				) {
			if (lemmaHeadTokID.equals("be")) {
				ArrayList<String> tokenArr = getTokenIDArr(headTokID, tokID);
				for (String tok : tokenArr) {
					if (doc.getTokens().get(tok).getLemma().equals("go") &&
							(doc.getTokens().get(tok).getPos().equals("VBG") 
									|| doc.getTokens().get(tok).getPos().equals("VVG"))
							) {
						return "FUTURE";
					}
				}
			}
			return "PRESENT";
		} else if (posHeadTokID.equals("VBD")
				|| posHeadTokID.equals("VDD")
				|| posHeadTokID.equals("VHD")
				|| posHeadTokID.equals("VVD")
				) {
			return "PAST";
		} else if ((posHeadTokID.equals("MD") || posHeadTokID.equals("VM0"))
				&& (lemmaHeadTokID.equals("will") || lemmaHeadTokID.equals("shall"))) {
			return "FUTURE";
		} else if (posHeadTokID.equals("VBG")
				|| posHeadTokID.equals("VDG")
				|| posHeadTokID.equals("VHG")
				|| posHeadTokID.equals("VVG")
				) {
			return "PRESPART";
		} else if (posHeadTokID.equals("VBN")
				|| posHeadTokID.equals("VDN")
				|| posHeadTokID.equals("VHN")
				|| posHeadTokID.equals("VVN")
				) {
			return "PASTPART";
		} 
		return "NONE";
	}
	
	protected String getAspect(String tokID) {		
		String headTokID = getMateHeadVerb(tokID);
		ArrayList<String> tokenArr = getTokenIDArr(headTokID, tokID);
		String aspect = "";
		if (getTense(tokID).equals("PRESPART")
				&& !doc.getTokens().get(headTokID).getLemma().equals("have")) {
			return "NONE";
		} else {
			for (String tok : tokenArr) {
				if (!tok.equals(tokID)) {
					if (doc.getTokens().get(tok).getLemma().equals("have") || 
							doc.getTokens().get(tok).getPos().startsWith("VH")
							) {
						aspect = "PERFECTIVE";
					} else if (doc.getTokens().get(tok).getLemma().equals("be")
							&& doc.getTokens().get(tok).getPos().equals("VBG")
							) {
						if (aspect.equals("PERFECTIVE")) aspect += "_PROGRESSIVE";
						else aspect = "PROGRESSIVE";
					} 
				} else {
					if (doc.getTokens().get(tok).getPos().equals("VBG")
							|| doc.getTokens().get(tok).getPos().equals("VVG")) {
						if (aspect.equals("PERFECTIVE")) aspect += "_PROGRESSIVE";
						else aspect = "PROGRESSIVE";
					}
					if (aspect.equals("")) aspect = "NONE";
					return aspect;
				} 
			}
		}
		return "NONE";
	}
	
	protected String getPolarity(String tokID) {		
		String headTokID = getMateHeadVerb(tokID);
		ArrayList<String> tokenArr = getTokenIDArr(headTokID, tokID);
		String polarity = "POS";
		for (String tok : tokenArr) {
			if (!tok.equals(tokID)) {
				if (doc.getTokens().get(tok).getPos().equals("XX0")
						|| doc.getTokens().get(tok).getLemma().equals("not")) {
					return "NEG";
				}
			} 
		}
		return polarity;
	}
	
	protected String getMateHeadVerb(String tokID) {
		Sentence s = doc.getSentences().get(doc.getTokens().get(tokID).getSentID());
		ArrayList<String> tokenArr = getTokenIDArr(s.getStartTokID(), s.getEndTokID());
		for (String tok : tokenArr) {
			if (!tokID.equals(tok) && doc.getTokens().get(tok).getDependencyRel() != null) {
				if (doc.getTokens().get(tok).getDependencyRel().keySet().contains(tokID)
						&& (doc.getTokens().get(tok).getDependencyRel().get(tokID).equals("VC")
								|| doc.getTokens().get(tok).getDependencyRel().get(tokID).equals("IM") 
								)
						&& tokenArr.indexOf(tok) < tokenArr.indexOf(tokID)) {
					return getMateHeadVerb(tok);
				} 
			}
		}
		return tokID;
	}
	
	protected boolean isMatePassiveVerb(String tokID) {
		Sentence s = doc.getSentences().get(doc.getTokens().get(tokID).getSentID());
		ArrayList<String> tokenArr = getTokenIDArr(s.getStartTokID(), s.getEndTokID());
		for (String tok : tokenArr) {
			if (!tokID.equals(tok) && doc.getTokens().get(tok).getDependencyRel() != null) {
				if (doc.getTokens().get(tok).getDependencyRel().keySet().contains(tokID) 
						&& doc.getTokens().get(tok).getDependencyRel().get(tokID).equals("VC")
						&& doc.getTokens().get(tok).getLemma().equals("be")) {
					return true;
				} 
			}
		}
		return false;
	}
	
	protected String getMateVerbFromSbjNoun(String tokID) {
		Sentence s = doc.getSentences().get(doc.getTokens().get(tokID).getSentID());
		ArrayList<String> tokenArr = getTokenIDArr(s.getStartTokID(), s.getEndTokID());
		for (String tok : tokenArr) {
			if (!tokID.equals(tok) && doc.getTokens().get(tok).getDependencyRel() != null) {
				if (doc.getTokens().get(tok).getDependencyRel().keySet().contains(tokID) &&
					doc.getTokens().get(tok).getDependencyRel().get(tokID).equals("SBJ")) {
					return tok;
				}
			}
		}
		return null;
	}
	
	protected String getMateVerbFromObjNoun(String tokID) {
		Sentence s = doc.getSentences().get(doc.getTokens().get(tokID).getSentID());
		ArrayList<String> tokenArr = getTokenIDArr(s.getStartTokID(), s.getEndTokID());
		for (String tok : tokenArr) {
			if (!tokID.equals(tok) && doc.getTokens().get(tok).getDependencyRel() != null) {
				if (doc.getTokens().get(tok).getDependencyRel().keySet().contains(tokID) &&
					doc.getTokens().get(tok).getDependencyRel().get(tokID).equals("OBJ")) {
					return tok;
				}
			}
		}
		return null;
	}
	
	protected String getMateVerbFromAdj(String tokID) {
		Sentence s = doc.getSentences().get(doc.getTokens().get(tokID).getSentID());
		ArrayList<String> tokenArr = getTokenIDArr(s.getStartTokID(), s.getEndTokID());
		for (String tok : tokenArr) {
			if (!tokID.equals(tok) && doc.getTokens().get(tok).getDependencyRel() != null) {
				if (doc.getTokens().get(tok).getDependencyRel().keySet().contains(tokID) &&
					doc.getTokens().get(tok).getDependencyRel().get(tokID).equals("PRD")) {
					return tok;
				}
			}
		}
		return null;
	}
	
	protected String getMateCoordVerb(String tokID) {
		Sentence s = doc.getSentences().get(doc.getTokens().get(tokID).getSentID());
		ArrayList<String> tokenArr = getTokenIDArr(s.getStartTokID(), s.getEndTokID());
		String headID = getMateHeadVerb(tokID);
		for (String tok : tokenArr) {
			if (!headID.equals(tok) && 
				doc.getTokens().get(tok).getDependencyRel() != null) {
				if (doc.getTokens().get(tok).getDependencyRel().keySet().contains(headID) &&
					doc.getTokens().get(tok).getDependencyRel().get(headID).equals("COORD")) {
					return tok;
				} else if (doc.getTokens().get(tok).getDependencyRel().keySet().contains(headID) &&
					doc.getTokens().get(tok).getDependencyRel().get(headID).equals("CONJ")) {
					return getMateCoordVerb(tok);
				} 
			}
		}
		return null;
	}
	
	public String getMateSubjectFromVerb(String tokID) {
		Sentence s = doc.getSentences().get(doc.getTokens().get(tokID).getSentID());
		ArrayList<String> tokenArr = getTokenIDArr(s.getStartTokID(), s.getEndTokID());
		String headID = getMateHeadVerb(tokID);
		if (doc.getTokens().get(headID).getDependencyRel() != null) {
			for (String tok : doc.getTokens().get(headID).getDependencyRel().keySet()) {
				if ((doc.getTokens().get(headID).getDependencyRel().get(tok).equals("SBJ"))
						&& !doc.getTokens().get(tok).getLemma().equals("that")
						&& !doc.getTokens().get(tok).getLemma().equals("which")
						&& !doc.getTokens().get(tok).getLemma().equals("``")
						&& !doc.getTokens().get(tok).getLemma().equals("`")
						&& !doc.getTokens().get(tok).getLemma().equals("''")
						&& !doc.getTokens().get(tok).getLemma().equals("'")
						)
					return tok;
			}
		}
		for (String tok : tokenArr) {
			if (!headID.equals(tok) && doc.getTokens().get(tok).getDependencyRel() != null) {
				if (doc.getTokens().get(tok).getDependencyRel().keySet().contains(tokID) &&
					(doc.getTokens().get(tok).getDependencyRel().get(headID).equals("NMOD")
						|| doc.getTokens().get(tok).getDependencyRel().get(headID).equals("ADV"))) {
					return tok;
				}
			}
		}
		if (getMateCoordVerb(headID) != null)
			return getMateSubjectFromVerb(getMateCoordVerb(headID));
		else
			return null;
	}
	
	public String getMateObjectFromVerb(String tokID) {
		Sentence s = doc.getSentences().get(doc.getTokens().get(tokID).getSentID());
		ArrayList<String> tokenArr = getTokenIDArr(s.getStartTokID(), s.getEndTokID());
		if (doc.getTokens().get(tokID).getDependencyRel() != null) {
			for (String tok : doc.getTokens().get(tokID).getDependencyRel().keySet()) {
				if (doc.getTokens().get(tokID).getDependencyRel().get(tok).equals("OBJ")
						|| (doc.getTokens().get(tokID).getDependencyRel().get(tok).equals("VC")
								&& tokenArr.indexOf(tok) > tokenArr.indexOf(tokID)))
					return tok;
			}
		}
		return null;
	}
	
	protected void generateDependencyPath(String govID, ArrayList<String> depArr, List<String> paths, String pathSoFar, List<String> visited) {
		if (doc.getTokens().get(govID).getDependencyRel() != null && !visited.contains(govID)) {
			for (String key : doc.getTokens().get(govID).getDependencyRel().keySet()) {
				if (depArr.contains(key)) {
					paths.add(pathSoFar + "-" + doc.getTokens().get(govID).getDependencyRel().get(key));
				} else {
					generateDependencyPath(key, depArr, paths, pathSoFar + "-" + doc.getTokens().get(govID).getDependencyRel().get(key), visited);
				}
			}
		}
	}
	
	protected void generateDependencyPath(String govID, String depID, List<String> paths, String pathSoFar, List<String> visited) {
		if (doc.getTokens().get(govID).getDependencyRel() != null && !visited.contains(govID)) {
			for (String key : doc.getTokens().get(govID).getDependencyRel().keySet()) {
				if (depID.equals(key)) {
					paths.add(pathSoFar + "-" + doc.getTokens().get(govID).getDependencyRel().get(key));
				} else {
					generateDependencyPath(key, depID, paths, pathSoFar + "-" + doc.getTokens().get(govID).getDependencyRel().get(key), visited);
				}
			}
		}
	}
	
	public String getMateMainVerb(Entity e) {
		if (getTokenAttribute(e, FeatureName.mainpos).equals("v")) {
			return (doc.getTokens().get(getMateHeadVerb(e.getStartTokID())).isMainVerb() ? "MAIN" : "O");
		}
		return "O";
	}
	
	protected String getString(String startTokID, String endTokID) {
		ArrayList<String> tokIDs = getTokenIDArr(startTokID, endTokID);
		ArrayList<String> context = new ArrayList<String>();
		for (String tokID : tokIDs) {
			context.add(doc.getTokens().get(tokID).getTokenAttribute(FeatureName.token).toLowerCase());
		}
		return String.join(" ", context);
	}
	
	private String simplifiedDependencyPath(String depPath) {
		String path = depPath;
		if (!path.equals("-VC-OBJ"))
			path = path.replaceAll("-VC", "");
		path = path.replaceAll("-COORD", "");
		path = path.replaceAll("-CONJ", "");
		if (!path.endsWith("-NMOD") && !path.startsWith("-NMOD-"))
			path = path.replaceAll("-NMOD", "");
		if (!path.endsWith("AMOD"))
			path = path.replaceAll("-AMOD", "");
		if (!path.startsWith("-APPO-"))
			path = path.replaceAll("-APPO-", "-");
		path = path.replaceAll("-EXTR-", "-");
		path = path.replaceAll("-PMOD-PMOD", "-PMOD");
		return path;
	}
	
	public String getSignalMateDependencyPath(Entity e, ArrayList<String> entArr, ArrayList<String> signalArr) {
		String path = "O";
		for (String tokID : entArr) {
			List<String> paths = new ArrayList<String>();
			List<String> visited = new ArrayList<String>();
			String govID = tokID;
			if (getTokenAttribute(e, FeatureName.mainpos).equals("v")) {
				govID = getMateHeadVerb(tokID);					
			} else if (getTokenAttribute(e, FeatureName.mainpos).equals("adj") &&
				getMateVerbFromAdj(tokID) != null) {
				govID = getMateVerbFromAdj(tokID);
			}
			
			generateDependencyPath(govID, signalArr, paths, "", visited);
			if (!paths.isEmpty()) {
				path = simplifiedDependencyPath(paths.get(0));				
				if (!path.equals("")) return path.substring(1);
			}
			
			if (getMateCoordVerb(govID) != null) {
				generateDependencyPath(getMateCoordVerb(govID), signalArr, paths, "", visited);
				if (!paths.isEmpty()) {
					path = simplifiedDependencyPath(paths.get(0));				
					if (!path.equals("")) return path.substring(1);
				}
			}
			
			if (getTokenAttribute(e, FeatureName.mainpos).equals("n")) {
				if (getMateVerbFromSbjNoun(tokID) != null) {
					generateDependencyPath(getMateVerbFromSbjNoun(tokID), signalArr, paths, "", visited);
					if (!paths.isEmpty()) {
						path = simplifiedDependencyPath(paths.get(0));				
						if (!path.equals("")) return path.substring(1);
					}
				}
			}
		}
		
		for (String tokID : signalArr) {
			for (String tokkID : entArr) {
				List<String> paths = new ArrayList<String>();
				List<String> visited = new ArrayList<String>();
				String govID, depID;
				
				if (doc.getTokens().get(tokID).getMainPos().equals("v")) {
					govID = getMateHeadVerb(tokID);
					depID = tokkID;
					if (getMateSubjectFromVerb(govID) != null
							&& getMateSubjectFromVerb(govID).equals(depID)) {
						return "SBJ";
					} else if (getMateObjectFromVerb(tokID) != null
							&& getMateObjectFromVerb(tokID).equals(depID)) {
						return "OBJ";
					}
				}
				
				govID = tokID;
				depID = tokkID;
				if (getTokenAttribute(e, FeatureName.mainpos).equals("v")) {
					depID = getMateHeadVerb(tokkID);
				} else if (getTokenAttribute(e, FeatureName.mainpos).equals("adj") &&
					getMateVerbFromAdj(tokkID) != null) {
					depID = getMateVerbFromAdj(tokkID);
				}
				
				generateDependencyPath(govID, depID, paths, "", visited);			
				if (!paths.isEmpty()) {
					path = simplifiedDependencyPath(paths.get(0));				
					if (!path.equals("")) return path.substring(1);
				}
				
				if (getMateCoordVerb(depID) != null) {
					generateDependencyPath(govID, getMateCoordVerb(depID), paths, "", visited);
					if (!paths.isEmpty()) {
						path = simplifiedDependencyPath(paths.get(0));				
						if (!path.equals("")) return path.substring(1);
					}
				}
				
				if (getTokenAttribute(e, FeatureName.mainpos).equals("n")) {
					if (getMateVerbFromSbjNoun(depID) != null) {
						generateDependencyPath(govID, getMateVerbFromSbjNoun(depID), paths, "", visited);
						if (!paths.isEmpty()) {
							path = simplifiedDependencyPath(paths.get(0));				
							if (!path.equals("")) return path.substring(1);
						}
					}
				}
			}
			
		}
		return "O";
	}
	
	private String simplifiedDependencyPath2(String depPath) {
		String path = depPath;
		if (!path.equals("-VC-OBJ"))
			path = path.replaceAll("-VC", "");
		path = path.replaceAll("-COORD", "");
		path = path.replaceAll("-CONJ", "");
		if (!path.endsWith("-NMOD") && !path.startsWith("-NMOD-"))
			path = path.replaceAll("-NMOD", "");
		path = path.replaceAll("-EXTR-", "-");
		path = path.replaceAll("-PMOD-PMOD", "-PMOD");
		return path;
	}
	
	public String getSignalMateDependencyPath2(Entity e, ArrayList<String> entArr, ArrayList<String> signalArr) {
		String path = "O";
		for (String tokID : entArr) {
			List<String> paths = new ArrayList<String>();
			List<String> visited = new ArrayList<String>();
			String govID = tokID;
			if (getTokenAttribute(e, FeatureName.mainpos).equals("v")) {
				govID = getMateHeadVerb(tokID);					
			} else if (getTokenAttribute(e, FeatureName.mainpos).equals("adj") &&
				getMateVerbFromAdj(tokID) != null) {
				govID = getMateVerbFromAdj(tokID);
			}
			
			generateDependencyPath(govID, signalArr, paths, "", visited);
			if (!paths.isEmpty()) {
				path = simplifiedDependencyPath2(paths.get(0));				
				if (!path.equals("")) return path.substring(1);
			}
			
			if (getMateCoordVerb(govID) != null) {
				generateDependencyPath(getMateCoordVerb(govID), signalArr, paths, "", visited);
				if (!paths.isEmpty()) {
					path = simplifiedDependencyPath2(paths.get(0));				
					if (!path.equals("")) return path.substring(1);
				}
			}
			
			if (getTokenAttribute(e, FeatureName.mainpos).equals("n")) {
				if (getMateVerbFromSbjNoun(tokID) != null) {
					generateDependencyPath(getMateVerbFromSbjNoun(tokID), signalArr, paths, "", visited);
					if (!paths.isEmpty()) {
						path = simplifiedDependencyPath2(paths.get(0));				
						if (!path.equals("")) return path.substring(1);
					}
				}
			}
		}
		
		for (String tokID : signalArr) {
			for (String tokkID : entArr) {
				List<String> paths = new ArrayList<String>();
				List<String> visited = new ArrayList<String>();
				String govID, depID;
				
				if (doc.getTokens().get(tokID).getMainPos().equals("v")) {
					govID = getMateHeadVerb(tokID);
					depID = tokkID;
					if (getMateSubjectFromVerb(govID) != null
							&& getMateSubjectFromVerb(govID).equals(depID)) {
						return "SBJ";
					} else if (getMateObjectFromVerb(tokID) != null
							&& getMateObjectFromVerb(tokID).equals(depID)) {
						return "OBJ";
					}
				}
				
				govID = tokID;
				depID = tokkID;
				if (getTokenAttribute(e, FeatureName.mainpos).equals("v")) {
					depID = getMateHeadVerb(tokkID);
				} else if (getTokenAttribute(e, FeatureName.mainpos).equals("adj") &&
					getMateVerbFromAdj(tokkID) != null) {
					depID = getMateVerbFromAdj(tokkID);
				}
				
				generateDependencyPath(govID, depID, paths, "", visited);			
				if (!paths.isEmpty()) {
					path = simplifiedDependencyPath2(paths.get(0));				
					if (!path.equals("")) return path.substring(1);
				}
				
				if (getMateCoordVerb(depID) != null) {
					generateDependencyPath(govID, getMateCoordVerb(depID), paths, "", visited);
					if (!paths.isEmpty()) {
						path = simplifiedDependencyPath2(paths.get(0));				
						if (!path.equals("")) return path.substring(1);
					}
				}
				
				if (getTokenAttribute(e, FeatureName.mainpos).equals("n")) {
					if (getMateVerbFromSbjNoun(depID) != null) {
						generateDependencyPath(govID, getMateVerbFromSbjNoun(depID), paths, "", visited);
						if (!paths.isEmpty()) {
							path = simplifiedDependencyPath2(paths.get(0));				
							if (!path.equals("")) return path.substring(1);
						}
					}
				}
			}
			
		}
		return "O";
	}
	
	private ArrayList<String> getSignalTidArr(String signal, String context, String tidStartContext, String position) {
		ArrayList<String> signalTidArr = new ArrayList<String>();
		
		String resContext = null;
		if (position.equals("BEFORE") || position.equals("BETWEEN")) {
			resContext = context.trim().substring(0, context.lastIndexOf(signal));
		} else {
			resContext = context.trim().substring(0, context.indexOf(signal));
		}
		int start = resContext.length() - resContext.replace(" ", "").length(); //count the number of spaces
		
		int tidxStartContext = doc.getTokenArr().indexOf(tidStartContext);
		int tidxStartSignal = tidxStartContext + start;
		int signalLength = signal.trim().split(" ").length;
		for (int i = tidxStartSignal; i < tidxStartSignal + signalLength; i++) {
			signalTidArr.add(doc.getTokenArr().get(i));
		}
		
		return signalTidArr;
	}
	
	private Integer getSignalEntityDistance(String signal, String context, String position) {
		List<String> wordList = Arrays.asList(context.split(" "));
		Collections.reverse(wordList);
		String reversedContext = String.join(" ", wordList);
		
		wordList = Arrays.asList(signal.split(" "));
		Collections.reverse(wordList);
		String reversedSignal = String.join(" ", wordList);
		
		if (position.equals("BEFORE") || position.equals("BETWEEN")) {
			String resContext = reversedContext.trim().substring(0, reversedContext.indexOf(reversedSignal));
			return resContext.length() - resContext.replace(" ", "").length(); //count the number of spaces
		} else {
			String resContext = context.trim().substring(0, context.indexOf(signal));
			return resContext.length() - resContext.replace(" ", "").length(); //count the number of spaces
		}
	}
	
	protected ArrayList<String> getTidEntityBeforeAfter(Entity e) {
		ArrayList<String> tids = new ArrayList<String>();
		Sentence s = doc.getSentences().get(e.getSentID());
		ArrayList<String> entArr = s.getEntityArr();
		int eidx = entArr.indexOf(e.getID());
		
		if (eidx == 0) { //first entity
			tids.add(s.getStartTokID());
		} else {
			Entity eBefore = doc.getEntities().get(entArr.get(eidx - 1)); 
			tids.add(doc.getTokenArr().get(doc.getTokenArr().indexOf(eBefore.getEndTokID()) + 1));
		}
		if (eidx == entArr.size()-1) { //last entity
			tids.add(s.getEndTokID());
		} else { 
			Entity eAfter = doc.getEntities().get(entArr.get(eidx + 1));
			tids.add(doc.getTokenArr().get(doc.getTokenArr().indexOf(eAfter.getStartTokID()) - 1));
		}
		
		return tids;
	}
	
	protected ArrayList<String> getTidBeforeAfter(Entity e) {
		ArrayList<String> tids = new ArrayList<String>();
		Sentence s = doc.getSentences().get(e.getSentID());
		
		if (e.getStartTokID().equals(s.getStartTokID())) { //first entity
			tids.add(s.getStartTokID());
		} else {
			tids.add(doc.getTokenArr().get(doc.getTokenArr().indexOf(e.getStartTokID()) - 1));
		}
		if (e.getEndTokID().equals(s.getEndTokID())) { //last entity
			tids.add(s.getEndTokID());
		} else {
			tids.add(doc.getTokenArr().get(doc.getTokenArr().indexOf(e.getEndTokID()) + 1));
		}
		
		return tids;
	}
	
	public ArrayList<String> getConnectiveTidArr(String conn, String startTidContext, String endTidContext, String position) {
		ArrayList<String> connTidArr = new ArrayList<String>();
		ArrayList<String> tidArr = getTokenIDArr(startTidContext,endTidContext);
		if (position.equals("BEFORE") || position.equals("BETWEEN")) {
			Collections.reverse(tidArr);
		}
		Boolean start = false;
		for (String tid : tidArr) {
			if (doc.getTokens().get(tid).getDiscourseConn().equals(conn)) {
				connTidArr.add(tid);
				start = true;
			} else {
				if (start) {
					start = false;
					break;
				}
			}
		}
		if (position.equals("BEFORE") || position.equals("BETWEEN")) {
			Collections.reverse(connTidArr);
		}
		return connTidArr;
	}
	
	private Integer getConnectiveEntityDistance(Entity e, ArrayList<String> tidConn, String position) {
		if (position.equals("BEFORE") || position.equals("BETWEEN")) {
			return Math.abs(doc.getTokenArr().indexOf(e.getStartTokID()) 
				- doc.getTokenArr().indexOf(tidConn.get(tidConn.size()-1)));
		} else {
			return Math.abs(doc.getTokenArr().indexOf(e.getEndTokID()) 
				- doc.getTokenArr().indexOf(tidConn.get(0)));
		}
	}

	public Marker getSignalMarker(Map<String, String> signalList, String text, String position, String context, String contextStartTid) {
		Marker m = new Marker();
		m.setText(text);
		m.setCluster(signalList.get(text));
		m.setPosition(position);
		
//		String dep1 = getSignalMateDependencyPath(e1, getTokenIDArr(e1.getStartTokID(), e1.getEndTokID()), 
//				getSignalTidArr(text, context, contextStartTid, position));
//		String dep2 = getSignalMateDependencyPath(e2, getTokenIDArr(e2.getStartTokID(), e2.getEndTokID()), 
//				getSignalTidArr(text, context, contextStartTid, position));
		
		ArrayList<String> signalTidArr = getSignalTidArr(text, context, contextStartTid, position);
		String dep1 = "O"; String dep2 = "O";
		if (position.equals("BETWEEN")
				|| position.equals("INSIDE")) {
			dep1 = getSignalMateDependencyPath(e1, getTokenIDArr(e1.getStartTokID(), e1.getEndTokID()), 
					signalTidArr);
			dep2 = getSignalMateDependencyPath(e2, getTokenIDArr(e2.getStartTokID(), e2.getEndTokID()), 
					signalTidArr);
		} else if (position.equals("BEFORE")
				|| position.equals("BEGIN")) {
			dep1 = getSignalMateDependencyPath(e2, getTokenIDArr(e2.getStartTokID(), e2.getEndTokID()), 
					signalTidArr);
			dep2 = getSignalMateDependencyPath(e1, getTokenIDArr(e1.getStartTokID(), e1.getEndTokID()), 
					signalTidArr);
		} else if (position.equals("BEGIN-BEFORE")) {
			dep1 = getSignalMateDependencyPath(e1, getTokenIDArr(e1.getStartTokID(), e1.getEndTokID()), 
					signalTidArr);
		} else if (position.equals("BEGIN-BETWEEN")) {
			dep2 = getSignalMateDependencyPath(e2, getTokenIDArr(e2.getStartTokID(), e2.getEndTokID()), 
					signalTidArr);
		}
		
		m.setDepRelE1(dep1);
		m.setDepRelE2(dep2);
		return m;
	}
	
	public Marker getSignalMarker(Map<String, String> signalList, String key, String text, String position, String context, String contextStartTid) {
		Marker m = new Marker();
		m.setText(text);
		m.setCluster(signalList.get(key));
		m.setPosition(position);
		
//		String dep1 = getSignalMateDependencyPath(e1, getTokenIDArr(e1.getStartTokID(), e1.getEndTokID()), 
//				getSignalTidArr(text, context, contextStartTid, position));
//		String dep2 = getSignalMateDependencyPath(e2, getTokenIDArr(e2.getStartTokID(), e2.getEndTokID()), 
//				getSignalTidArr(text, context, contextStartTid, position));
		
		ArrayList<String> signalTidArr = getSignalTidArr(text, context, contextStartTid, position);
		String dep1 = "O"; String dep2 = "O";
//		if (position.equals("BETWEEN")
//				|| position.equals("INSIDE")) {
//			dep1 = getSignalMateDependencyPath(e1, getTokenIDArr(e1.getStartTokID(), e1.getEndTokID()), 
//					signalTidArr);
//			dep2 = getSignalMateDependencyPath(e2, getTokenIDArr(e2.getStartTokID(), e2.getEndTokID()), 
//					signalTidArr);
//		} else if (position.equals("BEFORE")
//				|| position.equals("BEGIN")) {
//			dep1 = getSignalMateDependencyPath(e2, getTokenIDArr(e2.getStartTokID(), e2.getEndTokID()), 
//					signalTidArr);
//			dep2 = getSignalMateDependencyPath(e1, getTokenIDArr(e1.getStartTokID(), e1.getEndTokID()), 
//					signalTidArr);
//		} else if (position.equals("BEGIN-BEFORE")) {
//			dep1 = getSignalMateDependencyPath(e1, getTokenIDArr(e1.getStartTokID(), e1.getEndTokID()), 
//					signalTidArr);
//		} else if (position.equals("BEGIN-BETWEEN")) {
//			dep2 = getSignalMateDependencyPath(e2, getTokenIDArr(e2.getStartTokID(), e2.getEndTokID()), 
//					signalTidArr);
//		}
		
		dep1 = getSignalMateDependencyPath(e1, getTokenIDArr(e1.getStartTokID(), e1.getEndTokID()), 
				signalTidArr);
		dep2 = getSignalMateDependencyPath(e2, getTokenIDArr(e2.getStartTokID(), e2.getEndTokID()), 
				signalTidArr);
		
		if (dep1.contains(dep2)) {
			dep1 = dep1.replace(dep2, "O");
			if (dep1.equals("")) dep1 = "O";
			dep2 = "O";
		} else if (dep2.contains(dep1)) {
			dep2 = dep2.replace(dep1, "O");
			if (dep2.equals("")) dep2 = "O";
			dep1 = "O";
		}
		
		m.setDepRelE1(dep1);
		m.setDepRelE2(dep2);
		return m;
	}
	
	public Marker getSignalMarkerPerEntity(Entity ent, Map<String, String> signalList, String text, String position, String context, String contextStartTid) {
		Marker m = new Marker();
		m.setText(text);
		m.setCluster(signalList.get(text));
		m.setPosition(position);
		String dep1 = getSignalMateDependencyPath(ent, getTokenIDArr(ent.getStartTokID(), ent.getEndTokID()), 
				getSignalTidArr(text, context, contextStartTid, position));
		String dep2 = "";
		m.setDepRelE1(dep1);
		m.setDepRelE2(dep2);
		return m;
	}
	
	public Marker getConnectiveMarker(String text, String position, ArrayList<String> conn) {
		Marker m = new Marker();
		m.setText(text);
		m.setCluster(text);
		m.setPosition(position);
		
//		String dep1 = getSignalMateDependencyPath(e1, getTokenIDArr(e1.getStartTokID(), e1.getEndTokID()), conn);
//		String dep2 = getSignalMateDependencyPath(e2, getTokenIDArr(e2.getStartTokID(), e2.getEndTokID()), conn);
		
		String dep1 = "O"; String dep2 = "O";
		if (position.equals("BETWEEN")
				|| position.equals("INSIDE")) {
			dep1 = getSignalMateDependencyPath(e1, getTokenIDArr(e1.getStartTokID(), e1.getEndTokID()), 
					conn);
			dep2 = getSignalMateDependencyPath(e2, getTokenIDArr(e2.getStartTokID(), e2.getEndTokID()), 
					conn);
		} else if (position.equals("BEFORE")
				|| position.equals("BEGIN")) {
			dep1 = getSignalMateDependencyPath(e2, getTokenIDArr(e2.getStartTokID(), e2.getEndTokID()), 
					conn);
			dep2 = getSignalMateDependencyPath(e1, getTokenIDArr(e1.getStartTokID(), e1.getEndTokID()), 
					conn);
		} else if (position.equals("BEGIN-BEFORE")) {
			dep1 = getSignalMateDependencyPath(e1, getTokenIDArr(e1.getStartTokID(), e1.getEndTokID()), 
					conn);
		} else if (position.equals("BEGIN-BETWEEN")) {
			dep2 = getSignalMateDependencyPath(e2, getTokenIDArr(e2.getStartTokID(), e2.getEndTokID()), 
					conn);
		}
		
		m.setDepRelE1(dep1);
		m.setDepRelE2(dep2);
		return m;
	}
	
	public Marker getVerbMarker(Map<String, String> verbList, String text, String position, String verbTid) {
		Marker m = new Marker();
		m.setText(text);
		m.setCluster(verbList.get(text));
		m.setPosition(position);
		ArrayList<String> verbTidArr = new ArrayList<String>(); verbTidArr.add(verbTid);
		String dep1 = getSignalMateDependencyPath2(e1, getTokenIDArr(e1.getStartTokID(), e1.getEndTokID()), verbTidArr);
		String dep2 = getSignalMateDependencyPath2(e2, getTokenIDArr(e2.getStartTokID(), e2.getEndTokID()), verbTidArr);
//		System.out.println(text + "|" + m.getCluster() + "|" + dep1 + "|" + dep2);
		m.setDepRelE1(dep1);
		m.setDepRelE2(dep2);
		return m;
	}
	
	public Marker getTemporalSignal() throws IOException {
		Map<String, String> tsignalListEvent = this.getTempSignalList().getEventList();
		Map<String, String> tsignalListTimex = this.getTempSignalList().getTimexList();
		
		if (tsignalListEvent != null && tsignalListTimex != null) {		
			Map<Integer, Marker> candidates = new HashMap<Integer, Marker>();
			
			Map<String, String> signalList;
			if (e2 instanceof Timex) signalList = tsignalListTimex;
			else signalList = tsignalListEvent;
			
			Object[] sigKeys = signalList.keySet().toArray();
			Arrays.sort(sigKeys, Collections.reverseOrder());
			
			Map<String, String> evSignalList = tsignalListEvent;
			Object[] evSigKeys = evSignalList.keySet().toArray();
			Arrays.sort(evSigKeys, Collections.reverseOrder());
			
			if (isSameSentence()) {
				Sentence s = doc.getSentences().get(e1.getSentID());
				
				String tidBefore1 = getTidEntityBeforeAfter(e1).get(0);
				//String tidAfter1 = getTidEntityBeforeAfter(e1).get(1);
				String tidStart1 = getTidBeforeAfter(e1).get(0);
				//String tidEnd1 = getTidBeforeAfter(e1).get(1);
				
				String tidBefore2 = getTidEntityBeforeAfter(e1).get(0);
				String tidAfter2 = getTidEntityBeforeAfter(e2).get(1);
				String tidStart2 = getTidBeforeAfter(e2).get(0);
				String tidEnd2 = getTidBeforeAfter(e2).get(1);
				
				String tidBegin = doc.getTokenArr().get(doc.getTokenArr().indexOf(s.getStartTokID()) + 4);
				
				String contextBefore = getString(tidBefore1, tidStart1);
				String contextBetween = getString(tidBefore2, tidStart2);	//#only consider the context before e2 (after other entity)
				String contextAfter = getString(tidEnd2, tidAfter2);
				String contextBegin = getString(s.getStartTokID(), tidBegin);
				String contextEntity = getString(e2.getStartTokID(), e2.getEndTokID());	//in event-timex pair, sometimes signal is within timex
				
				//for (String key : signalList.keySet()) {
				for (Object key : sigKeys) {
					if (contextEntity.contains(" " + ((String)key) + " ")) {
						Marker m = getSignalMarker(signalList, ((String)key), "INSIDE", contextEntity, e2.getStartTokID());
						candidates.put(getSignalEntityDistance(((String)key), contextEntity, "INSIDE"), m);
					} else if (contextBetween.contains(" " + ((String)key) + " ")) {
						Marker m = getSignalMarker(signalList, ((String)key), "BETWEEN", contextBetween, tidBefore2);
						candidates.put(getSignalEntityDistance(((String)key), contextBetween, "BETWEEN") + 100, m);
//					} else if (contextAfter.contains(" " + ((String)key) + " ")) {
//						Marker m = getSignalMarker(signalList, ((String)key), "AFTER", contextAfter, tidEnd2);
//						candidates.put(getSignalEntityDistance(((String)key), contextAfter, "AFTER") + 300, m);
					}
				}
				
				//for (String key : tsignalListEvent.keySet()) {
				for (Object key : evSigKeys) {
					if (contextBefore.contains(" " + ((String)key) + " ")) {
						Marker m = getSignalMarker(tsignalListEvent, ((String)key), "BEFORE", contextBefore, tidBefore1);
						candidates.put(getSignalEntityDistance(((String)key), contextBefore, "BEFORE") + 200, m);
					} else if (contextBegin.contains(" " + ((String)key) + " ")) {
						Marker m = getSignalMarker(tsignalListEvent, ((String)key), "BEGIN", contextBegin, s.getStartTokID());
						candidates.put(getSignalEntityDistance(((String)key), contextBegin, "BEGIN") + 400, m);
					} 
				}
			} else if (!(e2 instanceof Timex)) { //different sentences
				Sentence s1 = doc.getSentences().get(e1.getSentID());
				Sentence s2 = doc.getSentences().get(e2.getSentID());
				String tidBegin1 = doc.getTokenArr().get(doc.getTokenArr().indexOf(s1.getStartTokID()) + 4);
				String tidBegin2 = doc.getTokenArr().get(doc.getTokenArr().indexOf(s2.getStartTokID()) + 4);
				String contextBegin1 = getString(s1.getStartTokID(), tidBegin1);
				String contextBegin2 = getString(s2.getStartTokID(), tidBegin2);
				
				//for (String key : tsignalListEvent.keySet()) {
				for (Object key : evSigKeys) {
					if (contextBegin2.contains(" " + ((String)key) + " ")) {
						Marker m = getSignalMarker(tsignalListEvent, ((String)key), "BEGIN-BETWEEN", contextBegin2, s2.getStartTokID());
						candidates.put(getSignalEntityDistance(((String)key), contextBegin2, "BEGIN-BETWEEN"), m);
//					} else if (contextBegin1.contains(" " + ((String)key) + " ")) {
//						Marker m = getSignalMarker(tsignalListEvent, ((String)key), "BEGIN-BEFORE", contextBegin1, s1.getStartTokID());
//						candidates.put(getSignalEntityDistance(((String)key), contextBegin1, "BEGIN-BEFORE") + 100, m);
					}
				}
			}
			
			if (!candidates.isEmpty()) {
				Object[] keys = candidates.keySet().toArray();
				Arrays.sort(keys);
				return candidates.get(keys[0]);
			} else {
				return new Marker("O", "O", "O", "O", "O");
			}
		}
		return null;
	}
	
	public Marker getTemporalSignalPerEntity(Entity ent) throws IOException {
		Map<String, String> tsignalListEvent = this.getTempSignalList().getEventList();
		Map<String, String> tsignalListTimex = this.getTempSignalList().getTimexList();
		
		if (tsignalListEvent != null && tsignalListTimex != null) {		
			Map<Integer, Marker> candidates = new HashMap<Integer, Marker>();
			
			Map<String, String> signalList;
			if (ent instanceof Timex) signalList = tsignalListTimex;
			else signalList = tsignalListEvent;
			
			//sort the signals, reversed-alphabetically, so that "because of" < "because"
			Object[] sigKeys = signalList.keySet().toArray();
			Arrays.sort(sigKeys, Collections.reverseOrder());
			
			Sentence s = doc.getSentences().get(ent.getSentID());
			
			String tidBefore1 = getTidEntityBeforeAfter(ent).get(0);
			String tidStart1 = getTidBeforeAfter(ent).get(0);
			String tidAfter2 = getTidEntityBeforeAfter(ent).get(1);
			String tidEnd2 = getTidBeforeAfter(ent).get(1);
			
			String tidBegin = doc.getTokenArr().get(doc.getTokenArr().indexOf(s.getStartTokID()) + 4);
			
			String contextBefore = getString(tidBefore1, tidStart1);
			String contextAfter = getString(tidEnd2, tidAfter2);
			String contextBegin = getString(s.getStartTokID(), tidBegin);
			String contextEntity = getString(ent.getStartTokID(), ent.getEndTokID());	//in event-timex pair, sometimes signal is within timex
			
			Marker m;
			//for (String key : signalList.keySet()) {
			for (Object key : sigKeys) {
				if (contextEntity.contains(" " + ((String)key) + " ")) {
					m = getSignalMarkerPerEntity(ent, signalList, ((String)key), "INSIDE", contextEntity, ent.getStartTokID());
					candidates.put(getSignalEntityDistance(((String)key), contextEntity, "INSIDE"), m);
				} else if (contextBefore.contains(" " + ((String)key) + " ")) {
					m = getSignalMarkerPerEntity(ent, signalList, ((String)key), "BEFORE", contextBefore, tidBefore1);
					candidates.put(getSignalEntityDistance(((String)key), contextBefore, "BEFORE") + 100, m);	
				} else if (contextAfter.contains(" " + ((String)key) + " ")) {
					m = getSignalMarkerPerEntity(ent, signalList, ((String)key), "AFTER", contextAfter, tidEnd2);
					candidates.put(getSignalEntityDistance(((String)key), contextAfter, "AFTER") + 200, m);
				} else if (contextBegin.contains(" " + ((String)key) + " ")) {
					m = getSignalMarkerPerEntity(ent, signalList, ((String)key), "BEGIN", contextBegin, s.getStartTokID());
					candidates.put(getSignalEntityDistance(((String)key), contextBegin, "BETWEEN") + 300, m);
				} 
			}
			
			if (!candidates.isEmpty()) {
				Object[] keys = candidates.keySet().toArray();
				Arrays.sort(keys);
				return candidates.get(keys[0]);
			} else {
				return new Marker("O", "O", "O", "O", "O");
			}
		}
		return null;
	}
			
	public Marker getTemporalConnective() {
		Map<Integer, Marker> candidates = new HashMap<Integer, Marker>();
		
		if (isSameSentence()) {
			Sentence s = doc.getSentences().get(e1.getSentID());
			
			String tidBefore1 = getTidEntityBeforeAfter(e1).get(0);
			//String tidAfter1 = getTidEntityBeforeAfter(e1).get(1);
			String tidStart1 = getTidBeforeAfter(e1).get(0);
			//String tidEnd1 = getTidBeforeAfter(e1).get(1);
			
			String tidBefore2 = getTidEntityBeforeAfter(e1).get(0);
			String tidAfter2 = getTidEntityBeforeAfter(e2).get(1);
			String tidStart2 = getTidBeforeAfter(e2).get(0);
			String tidEnd2 = getTidBeforeAfter(e2).get(1);
			
			String tidBegin = doc.getTokenArr().get(doc.getTokenArr().indexOf(s.getStartTokID()) + 4);
			
			ArrayList<String> tidConnBefore = getConnectiveTidArr("Temporal", tidBefore1, tidStart1, "BEFORE");
			ArrayList<String> tidConnBetween = getConnectiveTidArr("Temporal", tidBefore2, tidStart2, "BETWEEN");
			ArrayList<String> tidConnAfter = getConnectiveTidArr("Temporal", tidEnd2, tidAfter2, "AFTER");
			ArrayList<String> tidConnBegin = getConnectiveTidArr("Temporal", s.getStartTokID(), tidBegin, "BEGIN");
			ArrayList<String> tidConnEntity = getConnectiveTidArr("Temporal", e2.getStartTokID(), e2.getEndTokID(), "INSIDE");
			
			if (!tidConnBetween.isEmpty()) {
				String text = getString(tidConnBetween.get(0), tidConnBetween.get(tidConnBetween.size()-1));
				Marker m = getConnectiveMarker(text, "BETWEEN", tidConnBetween);
				candidates.put(getConnectiveEntityDistance(e2, tidConnBetween, "BETWEEN"), m);
			} else if (!tidConnBefore.isEmpty()) {
				String text = getString(tidConnBefore.get(0), tidConnBefore.get(tidConnBefore.size()-1));
				Marker m = getConnectiveMarker(text, "BEFORE", tidConnBefore);
				candidates.put(getConnectiveEntityDistance(e1, tidConnBefore, "BEFORE") + 100, m);
			} else if (!tidConnAfter.isEmpty()) {
				String text = getString(tidConnAfter.get(0), tidConnAfter.get(tidConnAfter.size()-1));
				Marker m = getConnectiveMarker(text, "AFTER", tidConnAfter);
				candidates.put(getConnectiveEntityDistance(e2, tidConnAfter, "AFTER") + 200, m);
			} else if (!tidConnEntity.isEmpty()) {
				String text = getString(tidConnEntity.get(0), tidConnEntity.get(tidConnEntity.size()-1));
				Marker m = getConnectiveMarker(text, "INSIDE", tidConnBegin);
				int distance = Math.abs(doc.getTokenArr().indexOf(e2.getStartTokID()) 
						- doc.getTokenArr().indexOf(tidConnEntity.get(0)));
				candidates.put(distance + 300, m);
			} else if (!tidConnBegin.isEmpty()) {
				String text = getString(tidConnBegin.get(0), tidConnBegin.get(tidConnBegin.size()-1));
				Marker m = getConnectiveMarker(text, "BEGIN", tidConnBegin);
				int distance = Math.abs(doc.getTokenArr().indexOf(s.getStartTokID()) 
						- doc.getTokenArr().indexOf(tidConnBegin.get(0)));
				candidates.put(distance + 400, m);
			}
		} else { //different sentences
			Sentence s1 = doc.getSentences().get(e1.getSentID());
			Sentence s2 = doc.getSentences().get(e2.getSentID());
			String tidBegin1 = doc.getTokenArr().get(doc.getTokenArr().indexOf(s1.getStartTokID()) + 4);
			String tidBegin2 = doc.getTokenArr().get(doc.getTokenArr().indexOf(s2.getStartTokID()) + 4);
			ArrayList<String> tidConnBegin1 = getConnectiveTidArr("Temporal", s1.getStartTokID(), tidBegin1, "BEGIN");
			ArrayList<String> tidConnBegin2 = getConnectiveTidArr("Temporal", s1.getStartTokID(), tidBegin2, "BEGIN");
			
			if (!tidConnBegin2.isEmpty()) {
				String text = getString(tidConnBegin2.get(0), tidConnBegin2.get(tidConnBegin2.size()-1));
				Marker m = getConnectiveMarker(text, "BEGIN-BETWEEN", tidConnBegin2);
				int distance = Math.abs(doc.getTokenArr().indexOf(s2.getStartTokID()) 
						- doc.getTokenArr().indexOf(tidConnBegin2.get(0)));
				candidates.put(distance, m);
			} else if (!tidConnBegin1.isEmpty()) {
				String text = getString(tidConnBegin1.get(0), tidConnBegin1.get(tidConnBegin1.size()-1));
				Marker m = getConnectiveMarker(text, "BEGIN-BEFORE", tidConnBegin1);
				int distance = Math.abs(doc.getTokenArr().indexOf(s1.getStartTokID()) 
						- doc.getTokenArr().indexOf(tidConnBegin1.get(0)));
				candidates.put(distance + 100, m);
			}
		}
		
		if (!candidates.isEmpty()) {
			Object[] keys = candidates.keySet().toArray();
			Arrays.sort(keys);
			return candidates.get(keys[0]);
		} else {
			return new Marker("O", "O", "O", "O", "O");
		}
	}
	
	public Marker getCausalSignal() throws IOException {
		Map<String, String> signalList = null;
		Map<String, String> patternList = null;
		signalList = ((CausalSignalList) this.getCausalSignalList()).getList();
		patternList = ((CausalSignalList) this.getCausalSignalList()).getPatternList();
		
		Object[] sigKeys = signalList.keySet().toArray();
//		Arrays.sort(sigKeys, Collections.reverseOrder());
		
		if (signalList != null & patternList != null) {	
			Map<Integer, Marker> candidates = new TreeMap<Integer, Marker>();
			Integer distance;
			
			if (isSameSentence()) {
				Sentence s = doc.getSentences().get(e1.getSentID());
				
				String tidBefore1 = getTidEntityBeforeAfter(e1).get(0);
				//String tidAfter1 = getTidEntityBeforeAfter(e1).get(1);
				String tidStart1 = getTidBeforeAfter(e1).get(0);
				//String tidEnd1 = getTidBeforeAfter(e1).get(1);
				
				String tidBefore2 = getTidEntityBeforeAfter(e1).get(0);
				String tidAfter2 = getTidEntityBeforeAfter(e2).get(1);
				String tidStart2 = getTidBeforeAfter(e2).get(0);
				String tidEnd2 = getTidBeforeAfter(e2).get(1);
				
				String tidBegin = doc.getTokenArr().get(doc.getTokenArr().indexOf(s.getStartTokID()) + 4);
				
				String contextBefore = " " + getString(tidBefore1, tidStart1) + " ";
				String contextBetween = " " + getString(tidBefore2, tidStart2) + " ";	//#only consider the context before e2 (after other entity)
				String contextAfter = " " + getString(tidEnd2, tidAfter2) + " ";
				String contextBegin = " " + getString(s.getStartTokID(), tidBegin) + " ";
				
				int i = 0;
				for (String key : signalList.keySet()) {
//				for (Object key : sigKeys) {
					Pattern pattern = Pattern.compile(" " + patternList.get(key) + " ");
					Matcher matcher = pattern.matcher(contextBetween);
					if (matcher.find()) {
						Marker m = getSignalMarker(signalList, key, matcher.group().trim(), "BETWEEN", contextBetween, tidBefore2);
						distance = getSignalEntityDistance(matcher.group(), contextBetween, "BETWEEN") + i;
						if (!candidates.containsKey(distance)) candidates.put(distance, m);
						else {
							while (candidates.containsKey(distance)) {
								distance ++; i ++;
							}
							candidates.put(distance, m);
						}
					}
					matcher = pattern.matcher(contextBefore);
					if (matcher.find()) {
						Marker m = getSignalMarker(signalList, key, matcher.group().trim(), "BEFORE", contextBefore, tidBefore1);
						distance = getSignalEntityDistance(matcher.group(), contextBefore, "BEFORE") + i;
						if (!candidates.containsKey(distance)) candidates.put(distance, m);
						else {
							while (candidates.containsKey(distance)) {
								distance ++; i ++;
							}
							candidates.put(distance, m);
						}
					} 
					matcher = pattern.matcher(contextAfter);
					if (matcher.find()) {
						Marker m = getSignalMarker(signalList, key, matcher.group().trim(), "AFTER", contextAfter, tidEnd2);
						distance = getSignalEntityDistance(matcher.group(), contextAfter, "AFTER") + i;
						if (!candidates.containsKey(distance)) candidates.put(distance, m);
						else {
							while (candidates.containsKey(distance)) {
								distance ++; i ++;
							}
							candidates.put(distance, m);
						}
					}
//					matcher = pattern.matcher(contextBegin);
//					if (matcher.find()) {
//						Marker m = getSignalMarker(signalList, key, matcher.group().trim(), "BEGIN", contextBegin, s.getStartTokID());
//						distance = getSignalEntityDistance(matcher.group(), contextBegin, "BEGIN");
//						if (!candidates.containsKey(distance + 100)) candidates.put(distance + 100, m);
//						else {
//							while (candidates.containsKey(distance)) {
//								distance ++; i ++;
//							}
//							candidates.put(distance, m);
//						}
//					} 
					i ++;
				}
			} else { //consecutive sentences
				Sentence s1 = doc.getSentences().get(e1.getSentID());
				Sentence s2 = doc.getSentences().get(e2.getSentID());
				String tidBegin1 = doc.getTokenArr().get(doc.getTokenArr().indexOf(s1.getStartTokID()) + 4);
				String tidBegin2 = doc.getTokenArr().get(doc.getTokenArr().indexOf(s2.getStartTokID()) + 4);
				String contextBegin1 = " " + getString(s1.getStartTokID(), tidBegin1) + " ";
				String contextBegin2 = " " + getString(s2.getStartTokID(), tidBegin2) + " ";
				
				int i = 0;
				for (String key : signalList.keySet()) {
//				for (Object key : sigKeys) {
					Pattern pattern = Pattern.compile(" " + patternList.get(key) + " ");
					Matcher matcher = pattern.matcher(contextBegin2);
					if (matcher.find()) {
						Marker m = getSignalMarker(signalList, key, matcher.group().trim(), "BEGIN-BETWEEN", contextBegin2, s2.getStartTokID());
						distance = getSignalEntityDistance(matcher.group(), contextBegin2, "BEGIN-BETWEEN") + i;
						if (!candidates.containsKey(distance)) candidates.put(distance, m);
						else {
							while (candidates.containsKey(distance)) {
								distance ++; i ++;
							}
							candidates.put(distance, m);
						}
					}
//					matcher = pattern.matcher(contextBegin1);
//					if (matcher.find()) {
//						Marker m = getSignalMarker(signalList, key, matcher.group().trim(), "BEGIN-BEFORE", contextBegin1, s1.getStartTokID());
//						distance = getSignalEntityDistance(matcher.group(), contextBegin1, "BEGIN-BEFORE");
//						if (!candidates.containsKey(distance + 100)) candidates.put(distance + 100, m);
//						else {
//							while (candidates.containsKey(distance)) {
//								distance ++; i ++;
//							}
//							candidates.put(distance, m);
//						}
//					}
					i ++;
				}
			}
			
			if (!candidates.isEmpty()) {
				Object[] keys = candidates.keySet().toArray();
				Arrays.sort(keys);
				return candidates.get(keys[0]);
			} else {
				return new Marker("O", "O", "O", "O", "O");
			}
		}
		return null;
	}
	
	public Marker getCausalSignalPerEntity(Entity ent) throws IOException {
		Map<String, String> csignalList = this.getCausalSignalList().getList();
		
		if (csignalList != null) {		
			Map<Integer, Marker> candidates = new HashMap<Integer, Marker>();
			
			//sort the signals, reversed-alphabetically, so that "because of" < "because"
			Object[] sigKeys = csignalList.keySet().toArray();
			Arrays.sort(sigKeys, Collections.reverseOrder());
			
			Sentence s = doc.getSentences().get(ent.getSentID());
			
			String tidBefore1 = getTidEntityBeforeAfter(ent).get(0);
			String tidStart1 = getTidBeforeAfter(ent).get(0);
			String tidAfter2 = getTidEntityBeforeAfter(ent).get(1);
			String tidEnd2 = getTidBeforeAfter(ent).get(1);
			
			String tidBegin = doc.getTokenArr().get(doc.getTokenArr().indexOf(s.getStartTokID()) + 4);
			
			String contextBefore = getString(tidBefore1, tidStart1);
			String contextAfter = getString(tidEnd2, tidAfter2);
			String contextBegin = getString(s.getStartTokID(), tidBegin);
			String contextEntity = getString(ent.getStartTokID(), ent.getEndTokID());	//in event-timex pair, sometimes signal is within timex
			
			Marker m;
			//for (String key : signalList.keySet()) {
			for (Object key : sigKeys) {
				if (contextEntity.contains(" " + ((String)key) + " ")) {
					m = getSignalMarkerPerEntity(ent, csignalList, ((String)key), "INSIDE", contextEntity, ent.getStartTokID());
					candidates.put(getSignalEntityDistance(((String)key), contextEntity, "INSIDE"), m);
				} else if (contextBefore.contains(" " + ((String)key) + " ")) {
					m = getSignalMarkerPerEntity(ent, csignalList, ((String)key), "BEFORE", contextBefore, tidBefore1);
					candidates.put(getSignalEntityDistance(((String)key), contextBefore, "BEFORE") + 100, m);	
				} else if (contextAfter.contains(" " + ((String)key) + " ")) {
					m = getSignalMarkerPerEntity(ent, csignalList, ((String)key), "AFTER", contextAfter, tidEnd2);
					candidates.put(getSignalEntityDistance(((String)key), contextAfter, "AFTER") + 200, m);
				} else if (contextBegin.contains(" " + ((String)key) + " ")) {
					m = getSignalMarkerPerEntity(ent, csignalList, ((String)key), "BEGIN", contextBegin, s.getStartTokID());
					candidates.put(getSignalEntityDistance(((String)key), contextBegin, "BETWEEN") + 300, m);
				} 
			}
			
			if (!candidates.isEmpty()) {
				Object[] keys = candidates.keySet().toArray();
				Arrays.sort(keys);
				return candidates.get(keys[0]);
			} else {
				return new Marker("O", "O", "O", "O", "O");
			}
		}
		return null;
	}
	
	public Marker getCausalConnective() {
		Map<Integer, Marker> candidates = new HashMap<Integer, Marker>();
		
		if (isSameSentence()) {
			Sentence s = doc.getSentences().get(e1.getSentID());
			
			String tidBefore1 = getTidEntityBeforeAfter(e1).get(0);
			//String tidAfter1 = getTidEntityBeforeAfter(e1).get(1);
			String tidStart1 = getTidBeforeAfter(e1).get(0);
			//String tidEnd1 = getTidBeforeAfter(e1).get(1);
			
			String tidBefore2 = getTidEntityBeforeAfter(e1).get(0);
			String tidAfter2 = getTidEntityBeforeAfter(e2).get(1);
			String tidStart2 = getTidBeforeAfter(e2).get(0);
			String tidEnd2 = getTidBeforeAfter(e2).get(1);
			
			String tidBegin = doc.getTokenArr().get(doc.getTokenArr().indexOf(s.getStartTokID()) + 4);
			
			ArrayList<String> tidConnBefore = getConnectiveTidArr("Contingency", tidBefore1, tidStart1, "BEFORE");
			ArrayList<String> tidConnBetween = getConnectiveTidArr("Contingency", tidBefore2, tidStart2, "BETWEEN");
			ArrayList<String> tidConnAfter = getConnectiveTidArr("Contingency", tidEnd2, tidAfter2, "AFTER");
			ArrayList<String> tidConnBegin = getConnectiveTidArr("Contingency", s.getStartTokID(), tidBegin, "BEGIN");
			
			if (!tidConnBetween.isEmpty()) {
				String text = getString(tidConnBetween.get(0), tidConnBetween.get(tidConnBetween.size()-1));
				Marker m = getConnectiveMarker(text, "BETWEEN", tidConnBetween);
				candidates.put(getConnectiveEntityDistance(e2, tidConnBetween, "BETWEEN"), m);
			} else if (!tidConnBefore.isEmpty()) {
				String text = getString(tidConnBefore.get(0), tidConnBefore.get(tidConnBefore.size()-1));
				Marker m = getConnectiveMarker(text, "BEFORE", tidConnBefore);
				candidates.put(getConnectiveEntityDistance(e1, tidConnBefore, "BEFORE") + 100, m);
			} else if (!tidConnAfter.isEmpty()) {
				String text = getString(tidConnAfter.get(0), tidConnAfter.get(tidConnAfter.size()-1));
				Marker m = getConnectiveMarker(text, "AFTER", tidConnAfter);
				candidates.put(getConnectiveEntityDistance(e2, tidConnAfter, "AFTER") + 200, m);
			} else if (!tidConnBegin.isEmpty()) {
				String text = getString(tidConnBegin.get(0), tidConnBegin.get(tidConnBegin.size()-1));
				Marker m = getConnectiveMarker(text, "BEGIN", tidConnBegin);
				int distance = Math.abs(doc.getTokenArr().indexOf(s.getStartTokID()) 
						- doc.getTokenArr().indexOf(tidConnBegin.get(0)));
				candidates.put(distance + 300, m);
			}
		} else { //consecutive sentences
			Sentence s1 = doc.getSentences().get(e1.getSentID());
			Sentence s2 = doc.getSentences().get(e2.getSentID());
			String tidBegin1 = doc.getTokenArr().get(doc.getTokenArr().indexOf(s1.getStartTokID()) + 4);
			String tidBegin2 = doc.getTokenArr().get(doc.getTokenArr().indexOf(s2.getStartTokID()) + 4);
			ArrayList<String> tidConnBegin1 = getConnectiveTidArr("Contingency", s1.getStartTokID(), tidBegin1, "BEGIN");
			ArrayList<String> tidConnBegin2 = getConnectiveTidArr("Contingency", s1.getStartTokID(), tidBegin2, "BEGIN");
			
			if (!tidConnBegin2.isEmpty()) {
				String text = getString(tidConnBegin2.get(0), tidConnBegin2.get(tidConnBegin2.size()-1));
				Marker m = getConnectiveMarker(text, "BEGIN-BETWEEN", tidConnBegin2);
				int distance = Math.abs(doc.getTokenArr().indexOf(s2.getStartTokID()) 
						- doc.getTokenArr().indexOf(tidConnBegin2.get(0)));
				candidates.put(distance, m);
			} else if (!tidConnBegin1.isEmpty()) {
				String text = getString(tidConnBegin1.get(0), tidConnBegin1.get(tidConnBegin1.size()-1));
				Marker m = getConnectiveMarker(text, "BEGIN-BEFORE", tidConnBegin1);
				int distance = Math.abs(doc.getTokenArr().indexOf(s1.getStartTokID()) 
						- doc.getTokenArr().indexOf(tidConnBegin1.get(0)));
				candidates.put(distance + 100, m);
			}
		}
		if (!candidates.isEmpty()) {
			Object[] keys = candidates.keySet().toArray();
			Arrays.sort(keys);
			return candidates.get(keys[0]);
		} else {
			return new Marker("O", "O", "O", "O", "O");
		}
	}
	
	public Marker getCausalVerb() {
		Map<String, String> verbList = null;
		verbList = ((CausalSignalList) this.getCausalSignalList()).getVerbList();
		
		if (verbList != null) {	
			Map<Integer, Marker> candidates = new HashMap<Integer, Marker>();
			
			if (isSameSentence()) {
				
				String lemma1 = doc.getTokens().get(e1.getStartTokID()).getLemma();
				String lemma2 = doc.getTokens().get(e2.getStartTokID()).getLemma();
				
				if (verbList.keySet().contains(lemma1)
						&& verbList.get(lemma1).equals("ENABLE")
						&& !isMatePassiveVerb(e1.getStartTokID())) {
					Marker m = getVerbMarker(verbList, lemma1, "BETWEEN", e1.getStartTokID());
					int distance = Math.abs(doc.getTokenArr().indexOf(e1.getStartTokID()) 
							- doc.getTokenArr().indexOf(e2.getStartTokID()));
					candidates.put(distance, m);
					
				} else {
				
					//String tidStart1 = getTidBeforeAfter(e1).get(0);
					String tidEnd1 = getTidBeforeAfter(e1).get(1);
					String tidStart2 = getTidBeforeAfter(e2).get(0);
					//String tidEnd2 = getTidBeforeAfter(e2).get(1);
					
					ArrayList<String> tidBetweenArr = getTokenIDArr(tidEnd1, tidStart2); //all words between e1 and e2
					
					for (String tid : tidBetweenArr) {

						if (doc.getTokens().get(tid).getChunk().contains("VP")) {
							
							String lemma = doc.getTokens().get(tid).getLemma();
							
							Map<String, List<String>> linkVerbs = new HashMap<String, List<String>>();
							String[] link = {"to", "with"}; 
							String[] lead = {"to"};
							String[] depend = {"on"}; 
							String[] result = {"in", "from"};
							String[] rely = {"on"}; 
							String[] stem = {"from"};
							String[] relate = {"to"};
							String[] connect = {"with"};
							String[] associate = {"with"};
							
							linkVerbs.put("link", Arrays.asList(link)); 
							linkVerbs.put("lead", Arrays.asList(lead));
							linkVerbs.put("depend", Arrays.asList(depend)); 
							linkVerbs.put("result", Arrays.asList(result));
							linkVerbs.put("rely", Arrays.asList(rely));
							linkVerbs.put("stem", Arrays.asList(stem));
							linkVerbs.put("relate", Arrays.asList(relate));
							linkVerbs.put("connect", Arrays.asList(connect));
							linkVerbs.put("associate", Arrays.asList(associate));
							
//							if (lemma.equals("reflect")) {
//								Marker m = getVerbMarker(verbList, lemma, "BETWEEN", tid);
//								int distance = Math.abs(doc.getTokenArr().indexOf(tid) 
//										- doc.getTokenArr().indexOf(e2.getStartTokID()));
//								candidates.put(distance, m);
//								
//							} else 
							if (lemma.equals("link") 
									|| lemma.equals("lead") 
									|| lemma.equals("depend") 
									|| lemma.equals("result")
									|| lemma.equals("rely")
									|| lemma.equals("stem")
									|| lemma.equals("relate")
									|| lemma.equals("connect")
									|| lemma.equals("associate")
								) {
								int tidx = doc.getTokens().get(tid).getIndex();
								String lemmaNext = doc.getTokens().get(doc.getTokenArr().get(tidx+1)).getLemma();
								if (linkVerbs.get(lemma).contains(lemmaNext)) {
									Marker m = getVerbMarker(verbList, lemma+"-"+lemmaNext, "BETWEEN", tid);
									int distance = Math.abs(doc.getTokenArr().indexOf(tid) 
											- doc.getTokenArr().indexOf(e2.getStartTokID()));
									candidates.put(distance, m);
								}
							} else if (lemma.equals("have")) {
								Boolean vc = false;
								Map<String, String> deps = doc.getTokens().get(tid).getDependencyRel();
								if (deps != null) {
									for (String depid : deps.keySet()) {
										if (deps.get(depid).equals("VC")) {
											vc = true; 
											break;
										}
									}
								}
								if (!vc) {
									Marker m = getVerbMarker(verbList, lemma, "BETWEEN", tid);
									int distance = Math.abs(doc.getTokenArr().indexOf(tid) 
											- doc.getTokenArr().indexOf(e2.getStartTokID()));
									candidates.put(distance, m);
								}
						    } else if (verbList.keySet().contains(lemma)) {
						    	if (!isMatePassiveVerb(tid)) {
//						    		System.out.println(lemma + "|" + lemma1 + "|" + lemma2);
							    	Marker m = getVerbMarker(verbList, lemma, "BETWEEN", tid);
									int distance = Math.abs(doc.getTokenArr().indexOf(tid) 
											- doc.getTokenArr().indexOf(e2.getStartTokID()));
									candidates.put(distance, m);
								}
							}
						}
					}
				}				
			}
			if (!candidates.isEmpty()) {
				Object[] keys = candidates.keySet().toArray();
				Arrays.sort(keys);
				return candidates.get(keys[0]);
			} else {
				return new Marker("O", "O", "O", "O", "O");
			}
		}
		return null;
	}
	
	public Marker getTemporalMarkerFeature() throws IOException {
		Marker m = null;
		if (this instanceof EventTimexFeatureVector) {
			//Assuming that the pair is already in event-timex order
			if ((e2 instanceof Timex && ((Timex)e2).isDct()) || (e2 instanceof Timex && ((Timex)e2).isEmptyTag()) ||
				!isSameSentence()) {
				m = new Marker("O", "O", "O", "O", "O");
			} else {	
				m = getTemporalConnective();
				if (m.getText().equals("O")) m = getTemporalSignal();
			}
		} else if (this instanceof EventEventFeatureVector) {
			m = getTemporalConnective();
			if (m.getText().equals("O")) m = getTemporalSignal();
		}
		return m;
	}
	
	public Marker getCausalMarkerFeature() throws IOException {
		Marker m = null;
//		m = getCausalConnective();
//		if (m.getText().equals("O")) 
		m = getCausalSignal();
		if (m.getText().equals("O")) m = getCausalVerb();
		return m;
	}
	
	public String getSimplifiedSignalDependencyPath(Marker m) {
		String depE1E2 = m.getDepRelE1() + "|" + m.getDepRelE2();
//		for (String s : this.dep_signal_path2) {
//			if (depE1E2.contains(s)) {
//				return s;
//			}
//		}
//		return "O|O";
		return depE1E2;
	}
	
	public void addToVector(String featureName, String featureValue) {
		List<String> fields = null;
		if (this instanceof EventEventFeatureVector) {
			fields = EventEventFeatureVector.fields;
		} else if (this instanceof EventTimexFeatureVector) {
			fields = EventTimexFeatureVector.fields;
		}
		getVectors().add(featureValue);
		fields.add(featureName);
	}
	
	public void addBinaryFeatureToVector(String featureName, String featureValue, List<String> featureValList) {
		List<String> fields = null;
		if (this instanceof EventEventFeatureVector) {
			fields = EventEventFeatureVector.fields;
		} else if (this instanceof EventTimexFeatureVector) {
			fields = EventTimexFeatureVector.fields;
		}
		for (String val : featureValList) {
			if (val.equals(featureValue)) {
				getVectors().add("1");
			} else {
				getVectors().add("0");
			}
			fields.add(featureName + "_" + val);
		}
	}
	
	public void addToVector(FeatureName feature) throws Exception {
		List<String> fields = null;
		if (this instanceof EventEventFeatureVector) {
			fields = EventEventFeatureVector.fields;
		} else if (this instanceof EventTimexFeatureVector) {
			fields = EventTimexFeatureVector.fields;
		}
		Marker m = null;
		if (fields != null) {
			switch(feature) {
				case id: 	
					getVectors().add(e1.getID());
					fields.add("id1");
					getVectors().add(e2.getID());
					fields.add("id2");
					break;
				case token: 	
					getVectors().add(getTokenAttribute(e1, FeatureName.token).replace(" ", "_"));
					fields.add("token1");
					getVectors().add(getTokenAttribute(e2, FeatureName.token).replace(" ", "_"));
					fields.add("token2");
					break;
				case lemma: 	
					getVectors().add(getTokenAttribute(e1, FeatureName.lemma).replace(" ", "_"));
					fields.add("lemma1");
					getVectors().add(getTokenAttribute(e2, FeatureName.lemma).replace(" ", "_"));
					fields.add("lemma2");
					break;
				case tokenSpace: 	
					getVectors().add(getTokenAttribute(e1, FeatureName.token));
					fields.add("token1");
					getVectors().add(getTokenAttribute(e2, FeatureName.token));
					fields.add("token2");
					break;
				case lemmaSpace: 	
					getVectors().add(getTokenAttribute(e1, FeatureName.lemma));
					fields.add("lemma1");
					getVectors().add(getTokenAttribute(e2, FeatureName.lemma));
					fields.add("lemma2");
					break;
				case tokenChunk: 	
					getVectors().add(getWholeChunkToken(e1));
					fields.add("tokenchunk1");
					getVectors().add(getWholeChunkToken(e2));
					fields.add("tokenchunk2");
					break;
				case pos:
					getVectors().add(getTokenAttribute(e1, FeatureName.pos));
					fields.add("pos1");
					getVectors().add(getTokenAttribute(e2, FeatureName.pos));
					fields.add("pos2");
					break;
				case mainpos:
					getVectors().add(getTokenAttribute(e1, FeatureName.mainpos));
					fields.add("mainpos1");
					getVectors().add(getTokenAttribute(e2, FeatureName.mainpos));
					fields.add("mainpos2");
					break;
				case chunk:
					getVectors().add(getTokenAttribute(e1, FeatureName.chunk));
					fields.add("chunk1");
					getVectors().add(getTokenAttribute(e2, FeatureName.chunk));
					fields.add("chunk2");
					break;
				case ner:
					getVectors().add(getTokenAttribute(e1, FeatureName.ner));
					fields.add("ner1");
					getVectors().add(getTokenAttribute(e2, FeatureName.ner));
					fields.add("ner2");
					break;
				case supersense:
					getVectors().add(getTokenAttribute(e1, FeatureName.supersense));
					fields.add("supersense1");
					getVectors().add(getTokenAttribute(e2, FeatureName.supersense));
					fields.add("supersense2");
					break;
				case posCombined:
					getVectors().add(getCombinedTokenAttribute(FeatureName.pos));
					fields.add("pos");
					break;
				case mainposCombined:
					getVectors().add(getCombinedTokenAttribute(FeatureName.mainpos));
					fields.add("mainpos");
					break;
				case chunkCombined:
					getVectors().add(getCombinedTokenAttribute(FeatureName.chunk));
					fields.add("chunk");
					break;
				case nerCombined:
					getVectors().add(getCombinedTokenAttribute(FeatureName.ner));
					fields.add("ner");
					break;
				case supersenseCombined:
					getVectors().add(getCombinedTokenAttribute(FeatureName.supersense));
					fields.add("supersense");
					break;
				case samePos:
					getVectors().add(isSameTokenAttribute(FeatureName.pos) ? "TRUE" : "FALSE");
					fields.add("samePos");
					break;
				case sameMainPos:
					getVectors().add(isSameTokenAttribute(FeatureName.mainpos) ? "TRUE" : "FALSE");
					fields.add("sameMainPos");
					break;
				case entDistance:
					getVectors().add(getEntityDistance().toString());
					fields.add("entDistance");
					break;
				case sentDistance:
					getVectors().add(getSentenceDistance().toString());
					fields.add("sentDistance");
					break;
				case entOrder:
					getVectors().add(getOrder());
					fields.add("entOrder");
					break;					
				case eventClass:
					if (this instanceof EventEventFeatureVector) {
						getVectors().add(getEntityAttribute(e1, FeatureName.eventClass));
						fields.add("eventClass1");
						getVectors().add(getEntityAttribute(e2, FeatureName.eventClass));
						fields.add("eventClass2");
					} else if (this instanceof EventTimexFeatureVector) {
						this.getVectors().add(getEntityAttribute(e1, FeatureName.eventClass));
						fields.add("eventClass");
					}
					break;
				case tense:
					if (this instanceof EventEventFeatureVector) {
						getVectors().add(getEntityAttribute(e1, FeatureName.tense));
						fields.add("tense1");
						getVectors().add(getEntityAttribute(e2, FeatureName.tense));
						fields.add("tense2");
					} else if (this instanceof EventTimexFeatureVector) {
						getVectors().add(getEntityAttribute(e1, FeatureName.tense));
						fields.add("tense");
					}
					break;
				case aspect:
					if (this instanceof EventEventFeatureVector) {
						getVectors().add(getEntityAttribute(e1, FeatureName.aspect));
						fields.add("aspect1");
						getVectors().add(getEntityAttribute(e2, FeatureName.aspect));
						fields.add("aspect2");
					} else if (this instanceof EventTimexFeatureVector) {
						getVectors().add(getEntityAttribute(e1, FeatureName.aspect));
						fields.add("aspect");
					}
					break;
				case tenseAspect:
					if (this instanceof EventEventFeatureVector) {
						getVectors().add(getEntityAttribute(e1, FeatureName.tense) + "-" + getEntityAttribute(e1, FeatureName.aspect));
						fields.add("tense-aspect1");
						getVectors().add(getEntityAttribute(e2, FeatureName.tense) + "-" + getEntityAttribute(e2, FeatureName.aspect));
						fields.add("tense-aspect2");
					} else if (this instanceof EventTimexFeatureVector) {
						getVectors().add(getEntityAttribute(e1, FeatureName.tense) + "-" + getEntityAttribute(e1, FeatureName.aspect));
						fields.add("tense-aspect");
					}
					break;
				case polarity:
					if (this instanceof EventEventFeatureVector) {
						getVectors().add(getEntityAttribute(e1, FeatureName.polarity));
						fields.add("polarity1");
						getVectors().add(getEntityAttribute(e2, FeatureName.polarity));
						fields.add("polarity2");
					} else if (this instanceof EventTimexFeatureVector) {
						getVectors().add(getEntityAttribute(e1, FeatureName.polarity));
						fields.add("polarity");
					}
					break;
				case eventClassCombined:
					if (this instanceof EventEventFeatureVector) {
						getVectors().add(getEntityAttribute(e1, FeatureName.eventClass) + "|" + 
								getEntityAttribute(e2, FeatureName.eventClass));
						fields.add("eventClass1|eventClass2");
					} else if (this instanceof EventTimexFeatureVector) {
						this.getVectors().add(getEntityAttribute(e1, FeatureName.eventClass));
						fields.add("eventClass");
					}
					break;
				case tenseCombined:
					if (this instanceof EventEventFeatureVector) {
						getVectors().add(getEntityAttribute(e1, FeatureName.tense) + "|" + 
								getEntityAttribute(e2, FeatureName.tense));
						fields.add("tense1|tense2");
					} else if (this instanceof EventTimexFeatureVector) {
						getVectors().add(getEntityAttribute(e1, FeatureName.tense));
						fields.add("tense");
					}
					break;
				case aspectCombined:
					if (this instanceof EventEventFeatureVector) {
						getVectors().add(getEntityAttribute(e1, FeatureName.aspect) + "|" + 
								getEntityAttribute(e2, FeatureName.aspect));
						fields.add("aspect1|aspect2");
					} else if (this instanceof EventTimexFeatureVector) {
						getVectors().add(getEntityAttribute(e1, FeatureName.aspect));
						fields.add("aspect");
					}
					break;
				case tenseAspectCombined:
					if (this instanceof EventEventFeatureVector) {
						getVectors().add(getEntityAttribute(e1, FeatureName.tense) + "-" + getEntityAttribute(e1, FeatureName.aspect) + "|" + 
								getEntityAttribute(e2, FeatureName.tense) + "-" + getEntityAttribute(e2, FeatureName.aspect));
						fields.add("tense-aspect1|tense-aspect2");
					} else if (this instanceof EventTimexFeatureVector) {
						getVectors().add(getEntityAttribute(e1, FeatureName.tense) + "-" + getEntityAttribute(e1, FeatureName.aspect));
						fields.add("tense-aspect");
					}
					break;
				case polarityCombined:
					if (this instanceof EventEventFeatureVector) {
						getVectors().add(getEntityAttribute(e1, FeatureName.polarity) + "|" + 
								getEntityAttribute(e2, FeatureName.polarity));
						fields.add("polarity1|polarity2");
					} else if (this instanceof EventTimexFeatureVector) {
						getVectors().add(getEntityAttribute(e1, FeatureName.polarity));
						fields.add("polarity");
					}
					break;
				case sameEventClass:
					getVectors().add(getEntityAttribute(e1, FeatureName.eventClass).equals(getEntityAttribute(e2, FeatureName.eventClass)) ? "TRUE" : "FALSE");
					fields.add("sameEventClass");
					break;
				case sameTense:
					getVectors().add(getEntityAttribute(e1, FeatureName.tense).equals(getEntityAttribute(e2, FeatureName.tense)) ? "TRUE" : "FALSE");
					fields.add("sameTense");
					break;
				case sameAspect:
					getVectors().add(getEntityAttribute(e1, FeatureName.aspect).equals(getEntityAttribute(e2, FeatureName.aspect)) ? "TRUE" : "FALSE");
					fields.add("sameAspect");
					break;
				case sameTenseAspect:
					getVectors().add((getEntityAttribute(e1, FeatureName.tense).equals(getEntityAttribute(e2, FeatureName.tense)) &&
							getEntityAttribute(e1, FeatureName.aspect).equals(getEntityAttribute(e2, FeatureName.aspect))) ? "TRUE" : "FALSE");
					fields.add("sameTenseAspect");
					break;
				case samePolarity:
					getVectors().add(getEntityAttribute(e1, FeatureName.polarity).equals(getEntityAttribute(e2, FeatureName.polarity)) ? "TRUE" : "FALSE");
					fields.add("samePolarity");
					break;
				case timexType:
					getVectors().add(getEntityAttribute(e2, FeatureName.timexType));
					fields.add("timexType");
					break;
				case timexValue:
					getVectors().add(getEntityAttribute(e2, FeatureName.timexValue));
					fields.add("timexValue");
					break;
				case timexValueTemplate:
					getVectors().add(getEntityAttribute(e2, FeatureName.timexValueTemplate));
					fields.add("timexValueTemplate");
					break;
				case timexTypeValueTemplate:
					getVectors().add(getEntityAttribute(e2, FeatureName.timexType) + "|" + 
							getEntityAttribute(e2, FeatureName.timexValueTemplate));
					fields.add("timexValueTemplate");
					break;
				case dct:
					getVectors().add(getEntityAttribute(e2, FeatureName.dct));
					fields.add("dct");
					break;
				case mainVerb:
					if (this instanceof EventEventFeatureVector) {
						getVectors().add(((EventEventFeatureVector) this).getMateMainVerb());
						fields.add("mainVerb1|mainVerb2");
					} else if (this instanceof EventTimexFeatureVector) {
						getVectors().add(((EventTimexFeatureVector) this).getMateMainVerb());
						fields.add("mainVerb");
					}
					break;
				case depPath:
					if (this instanceof EventEventFeatureVector) {
						getVectors().add(((EventEventFeatureVector) this).getMateDependencyPath());
						fields.add("depPath");
					} else if (this instanceof EventTimexFeatureVector) {
						getVectors().add(((EventTimexFeatureVector) this).getMateDependencyPath());
						fields.add("depPath");
					}
					break;
					
				case depTmxPath:
					String depTmxPathStr = ((EventTimexFeatureVector) this).getMateDependencyPath();
					if (depTmxPathStr.equals("TMP-PMOD")) {
						getVectors().add("TMP-PMOD"); 
					} else if (!depTmxPathStr.equals("TMP-PMOD")
							&& depTmxPathStr.contains("TMP-PMOD")) {
						getVectors().add("X-TMP-PMOD"); 
					} else if (!depTmxPathStr.contains("TMP-PMOD")) {
						getVectors().add("O"); 
					} else {
						getVectors().add("O");
					}
					fields.add("depTmxPath");
					break;
					
				case depEvPath:
					String depEvPathStr = ((EventEventFeatureVector) this).getMateDependencyPath();
					
					String depEvPath = "O";
					for (String s : this.dep_event_path) {
						if (depEvPathStr.equals(s)
								|| depEvPathStr.contains(s)
							) {
							depEvPath = s;
							break;
						}
					}
					getVectors().add(depEvPath);
					fields.add("depEvPath");
					break;
					
				case modalVerb:
					if (this instanceof EventEventFeatureVector) {
						getVectors().add(getMateModalVerb(e1.getStartTokID()) + "|" + getMateModalVerb(e2.getStartTokID()));
						fields.add("modalVerb1|modalVerb2");
					} else if (this instanceof EventTimexFeatureVector) {
						getVectors().add(getMateModalVerb(e1.getStartTokID()));
						fields.add("mainVerb");
					}
					break;
					
				case tempMarker:
					m = getTemporalMarkerFeature();
					getVectors().add(m.getCluster().replace(" ", "_") + "|" + m.getPosition());
					fields.add("tempMarkerClusText-Position");
					getVectors().add(m.getDepRelE1() + "|" + m.getDepRelE2());
					fields.add("tempMarkerDep1-Dep2");
					break;
				case tempMarkerText:
					m = getTemporalMarkerFeature();
					getVectors().add(m.getText().replace(" ", "_"));
					fields.add("tempMarkerText");
					break;
				case tempMarkerClusText:
					m = getTemporalMarkerFeature();
					getVectors().add(m.getCluster().replace(" ", "_"));
					fields.add("tempMarkerClusText");
					break;
				case tempMarkerClusTextPos:
					m = getTemporalMarkerFeature();
					getVectors().add(m.getCluster().replace(" ", "_") + "|" + m.getPosition());
					fields.add("tempMarkerClusTextPos");
					break;
				case tempMarkerTextSpace:
					m = getTemporalMarkerFeature();
					getVectors().add(m.getText());
					fields.add("tempMarkerTextSpace");
					break;
				case tempMarkerClusTextSpace:
					m = getTemporalMarkerFeature();
					getVectors().add(m.getCluster());
					fields.add("tempMarkerClusTextSpace");
					break;
				case tempMarkerPos:
					m = getTemporalMarkerFeature();
					getVectors().add(m.getPosition());
					fields.add("tempMarkerPos");
					break;
				case tempMarkerDep1:
					m = getTemporalMarkerFeature();
					getVectors().add(m.getDepRelE1());
					fields.add("tempMarkerDep1");
					break;
				case tempMarkerDep2:
					m = getTemporalMarkerFeature();
					getVectors().add(m.getDepRelE2());
					fields.add("tempMarkerDep2");
					break;
				case tempMarkerDep1Dep2:
					m = getTemporalMarkerFeature();
					getVectors().add(m.getDepRelE1() + "|" + m.getDepRelE2());
					fields.add("tempMarkerDep1-Dep2");
					break;
					
				case tempSignalText:
					if (this instanceof EventTimexFeatureVector) {
						//Assuming that the pair is already in event-timex order
						if ((e2 instanceof Timex && ((Timex)e2).isDct()) || (e2 instanceof Timex && ((Timex)e2).isEmptyTag()) ||
							!isSameSentence()) {
							getVectors().add("O");
							fields.add("tempSignalText");
						} else {	
							m = getTemporalSignal();
							getVectors().add(m.getText().replace(" ", "_"));
							fields.add("tempSignalText");
						}
					} else if (this instanceof EventEventFeatureVector) {
						m = getTemporalSignal();
						getVectors().add(m.getText().replace(" ", "_"));
						fields.add("tempSignalText");
					}
					break;
					
				case tempSignalClusText:
					if (this instanceof EventTimexFeatureVector) {
						//Assuming that the pair is already in event-timex order
						if ((e2 instanceof Timex && ((Timex)e2).isDct()) || (e2 instanceof Timex && ((Timex)e2).isEmptyTag()) ||
							!isSameSentence()) {
							getVectors().add("O");
							fields.add("tempSignalClusText");
						} else {	
							m = getTemporalSignal();
							getVectors().add(m.getCluster().replace(" ", "_"));
							fields.add("tempSignalClusText");
						}
					} else if (this instanceof EventEventFeatureVector) {
						m = getTemporalSignal();
						getVectors().add(m.getCluster().replace(" ", "_"));
						fields.add("tempSignalClusText");
					}
					break;
					
				case tempSignalPos:
					if (this instanceof EventTimexFeatureVector) {
						//Assuming that the pair is already in event-timex order
						if ((e2 instanceof Timex && ((Timex)e2).isDct()) || (e2 instanceof Timex && ((Timex)e2).isEmptyTag()) ||
							!isSameSentence()) {
							getVectors().add("O");
							fields.add("tempSignalPos");
						} else {	
							m = getTemporalSignal();
							getVectors().add(m.getPosition());
							fields.add("tempSignalPos");
						}
					} else if (this instanceof EventEventFeatureVector) {
						m = getTemporalSignal();
						getVectors().add(m.getPosition());
						fields.add("tempSignalPos");
					}
					break;
					
				case tempConnText:
					m = getTemporalConnective();
					getVectors().add(m.getText().replace(" ", "_"));
					fields.add("tempConnText");
					break;
					
				case tempConnPos:
					m = getTemporalConnective();
					getVectors().add(m.getPosition());
					fields.add("tempConnPos");
					break;
					
				case tempSignal1ClusText:
					m = getTemporalSignalPerEntity(e1);
					getVectors().add(m.getCluster().replace(" ", "_"));
					fields.add("tempSignal1ClusText");
					break;
				case tempSignal1Text:
					m = getTemporalSignalPerEntity(e1);
					getVectors().add(m.getText().replace(" ", "_"));
					fields.add("tempSignal1Text");
					break;
				case tempSignal1Pos:
					m = getTemporalSignalPerEntity(e1);
					getVectors().add(m.getPosition());
					fields.add("tempSignal1Pos");
					break;
				case tempSignal2ClusText:
					if (this instanceof EventTimexFeatureVector) {
						//Assuming that the pair is already in event-timex order
						if ((e2 instanceof Timex && ((Timex)e2).isDct()) || (e2 instanceof Timex && ((Timex)e2).isEmptyTag()) ||
							!isSameSentence()) {
							getVectors().add("O");
							fields.add("tempSignal2ClusText");
						} else {	
							m = getTemporalSignalPerEntity(e2);
							getVectors().add(m.getCluster().replace(" ", "_"));
							fields.add("tempSignal2ClusText");
						}
					} else if (this instanceof EventEventFeatureVector) {
						m = getTemporalSignalPerEntity(e2);
						getVectors().add(m.getCluster().replace(" ", "_"));
						fields.add("tempSignal2ClusText");
					}
					break;
				case tempSignal2Text:
					if (this instanceof EventTimexFeatureVector) {
						//Assuming that the pair is already in event-timex order
						if ((e2 instanceof Timex && ((Timex)e2).isDct()) || (e2 instanceof Timex && ((Timex)e2).isEmptyTag()) ||
							!isSameSentence()) {
							getVectors().add("O");
							fields.add("tempSignal2Text");
						} else {	
							m = getTemporalSignalPerEntity(e2);
							getVectors().add(m.getText().replace(" ", "_"));
							fields.add("tempSignal2Text");
						}
					} else if (this instanceof EventEventFeatureVector) {
						m = getTemporalSignalPerEntity(e2);
						getVectors().add(m.getText().replace(" ", "_"));
						fields.add("tempSignal2Text");
					}
					break;
				case tempSignal2Pos:
					if (this instanceof EventTimexFeatureVector) {
						//Assuming that the pair is already in event-timex order
						if ((e2 instanceof Timex && ((Timex)e2).isDct()) || (e2 instanceof Timex && ((Timex)e2).isEmptyTag()) ||
							!isSameSentence()) {
							getVectors().add("O");
							fields.add("tempSignal2Pos");
						} else {	
							m = getTemporalSignalPerEntity(e2);
							getVectors().add(m.getPosition());
							fields.add("tempSignal2Pos");
						}
					} else if (this instanceof EventEventFeatureVector) {
						m = getTemporalSignalPerEntity(e2);
						getVectors().add(m.getPosition());
						fields.add("tempSignal2Pos");
					}
					break;
					
				case causMarker:
					m = getCausalMarkerFeature();
					getVectors().add(m.getCluster().replace(" ", "_") + "|" + m.getPosition());
					fields.add("causMarkerClusText-Position");
					getVectors().add(m.getDepRelE1() + "|" + m.getDepRelE2());
					fields.add("causMarkerDep1-Dep2");
					break;
				case causMarkerText:
					m = getCausalMarkerFeature();
					getVectors().add(m.getText().replace(" ", "_"));
					fields.add("causMarkerText");
					break;
				case causMarkerClusText:
					m = getCausalMarkerFeature();
					getVectors().add(m.getCluster().replace(" ", "_"));
					fields.add("causMarkerClusText");
					break;
				case causMarkerClusTextPos:
					m = getCausalMarkerFeature();
					getVectors().add(m.getCluster().replace(" ", "_") + "|" + m.getPosition());
					fields.add("causMarkerClusTextPos");
					break;
				case causMarkerTextSpace:
					m = getCausalMarkerFeature();
					getVectors().add(m.getText());
					fields.add("causMarkerText");
					break;
				case causMarkerClusTextSpace:
					m = getCausalMarkerFeature();
					getVectors().add(m.getCluster());
					fields.add("causMarkerClusText");
					break;
				case causSignalClusText:
					m = getCausalSignal();
					getVectors().add(m.getCluster().replace(" ", "_"));
					fields.add("causSignalClusText");
					break;
				case causMarkerPos:
					m = getCausalMarkerFeature();
					getVectors().add(m.getPosition());
					fields.add("causMarkerPos");
					break;
				case causSignalPos:
					m = getCausalSignal();
					getVectors().add(m.getPosition());
					fields.add("causSignalPos");
					break;
				case causMarkerDep1:
					m = getCausalMarkerFeature();
					getVectors().add(m.getDepRelE1());
					fields.add("causMarkerDep1");
					break;
				case causMarkerDep2:
					m = getCausalMarkerFeature();
					getVectors().add(m.getDepRelE2());
					fields.add("causMarkerDep2");
					break;
				case causMarkerDep1Dep2:
					m = getCausalMarkerFeature();
					String depE1E2 = getSimplifiedSignalDependencyPath(m);
					getVectors().add(depE1E2);
					fields.add("causMarkerDep1-Dep2");
					break;
				case causSignalDep1Dep2:
					m = getCausalSignal();
					String depSE1E2 = getSimplifiedSignalDependencyPath(m);
					getVectors().add(depSE1E2);
					fields.add("causSignalDep1-Dep2");
					break;
				case causSignal1ClusText:
					m = getCausalSignalPerEntity(e1);
					getVectors().add(m.getCluster().replace(" ", "_"));
					fields.add("causSignal1ClusText");
					break;
				case causSignal1Text:
					m = getCausalSignalPerEntity(e1);
					getVectors().add(m.getText().replace(" ", "_"));
					fields.add("causSignal1Text");
					break;
				case causSignal1Pos:
					m = getCausalSignalPerEntity(e1);
					getVectors().add(m.getPosition());
					fields.add("causSignal1Pos");
					break;
				case causSignal2ClusText:
					m = getCausalSignalPerEntity(e2);
					getVectors().add(m.getCluster().replace(" ", "_"));
					fields.add("causSignal1ClusText");
					break;
				case causSignal2Text:
					m = getCausalSignalPerEntity(e2);
					getVectors().add(m.getText().replace(" ", "_"));
					fields.add("causSignal1Text");
					break;
				case causSignal2Pos:
					m = getCausalSignalPerEntity(e2);
					getVectors().add(m.getPosition());
					fields.add("causSignal1Pos");
					break;
				case causVerbClusText:
					m = getCausalVerb();
					getVectors().add(m.getCluster().replace(" ", "_"));
					fields.add("causVerbClusText");
					break;
				case causVerbPos:
					m = getCausalVerb();
					getVectors().add(m.getPosition());
					fields.add("causVerbPos");
					break;
				case coref:
					getVectors().add(((EventEventFeatureVector) this).isCoreference() ? "COREF" : "NOCOREF");
					fields.add("coref");
					break;
				case wnSim:
					//getVectors().add(((EventEventFeatureVector) this).getWordSimilarity().toString());
					getVectors().add(((EventEventFeatureVector) this).getDiscreteWordSimilarity());
					fields.add("wnSim");
					break;
				case timexRule:
					getVectors().add(((EventTimexFeatureVector) this).getTimexRule());
					fields.add("timexRule");
					break;
				case label:
					String lbl = getLabel();
					if (lbl.equals("END")) lbl = "ENDS";
					getVectors().add(lbl);
					fields.add("label");
					break;
				case labelBinary:
					String lblB = getLabel();
					if (!lblB.equals("NONE")) lblB = "TLINK";
					else lblB = "NONE";
					getVectors().add(lblB);
					fields.add("labelBinary");
					break;
				case labelCollapsed:
					String lblC = getLabel();
					if (lblC.equals("END")) lblC = "ENDS";
					else if (lblC.equals("DURING")) lblC = "SIMULTANEOUS";
					else if (lblC.equals("DURING_INV")) lblC = "SIMULTANEOUS";
					else if (lblC.equals("IBEFORE")) lblC = "BEFORE";
					else if (lblC.equals("IAFTER")) lblC = "AFTER";
					getVectors().add(lblC);
					fields.add("labelCollapsed");
					break;
				case labelCollapsed1:
					String lblC1 = getLabel();
					if (lblC1.equals("END")) lbl = "ENDS";
					else if (lblC1.equals("IDENTITY")) lblC1 = "SIMULTANEOUS";
					else if (lblC1.equals("DURING")) lblC1 = "SIMULTANEOUS";
					else if (lblC1.equals("DURING_INV")) lblC1 = "SIMULTANEOUS";
					else if (lblC1.equals("IBEFORE")) lblC1 = "BEFORE";
					else if (lblC1.equals("IAFTER")) lblC1 = "AFTER";
					getVectors().add(lblC1);
					fields.add("labelCollapsed1");
					break;
				case labelCollapsed2:
					String lblC2 = getLabel();
					if (lblC2.equals("END")) lbl = "ENDS";
					else if (lblC2.equals("IDENTITY")) lblC2 = "SIMULTANEOUS";
					else if (lblC2.equals("DURING")) lblC2 = "SIMULTANEOUS";
					else if (lblC2.equals("DURING_INV")) lblC2 = "SIMULTANEOUS";
					else if (lblC2.equals("IBEFORE")) lblC2 = "BEFORE";
					else if (lblC2.equals("IAFTER")) lblC2 = "AFTER";
					else if (lblC2.equals("BEGINS")) lblC2 = "BEFORE";
					else if (lblC2.equals("BEGUN_BY")) lblC2 = "AFTER";
					else if (lblC2.equals("ENDS")) lblC2 = "AFTER";
					else if (lblC2.equals("ENDED_BY")) lblC2 = "BEFORE";
					getVectors().add(lblC2);
					fields.add("labelCollapsed2");
					break;
				case labelCollapsed3:
					String lblC3 = getLabel();
					if (lblC3.equals("END")) lbl = "ENDS";
					else if (lblC3.equals("IDENTITY")) lblC3 = "SIMULTANEOUS";
					else if (lblC3.equals("DURING")) lblC3 = "IS_INCLUDED";
					else if (lblC3.equals("DURING_INV")) lblC3 = "INCLUDES";
					else if (lblC3.equals("IBEFORE")) lblC3 = "BEFORE";
					else if (lblC3.equals("IAFTER")) lblC3 = "AFTER";
					else if (lblC3.equals("BEGINS")) lblC3 = "BEFORE";
					else if (lblC3.equals("BEGUN_BY")) lblC3 = "AFTER";
					else if (lblC3.equals("ENDS")) lblC3 = "AFTER";
					else if (lblC3.equals("ENDED_BY")) lblC3 = "BEFORE";
					getVectors().add(lblC3);
					fields.add("labelCollapsed3");
					break;
				case labelCollapsed4:
					String lblC4 = getLabel();
					if (lblC4.equals("END")) lblC4 = "ENDS";
					else if (lblC4.equals("IDENTITY")) lblC4 = "SIMULTANEOUS";
					else if (lblC4.equals("DURING")) lblC4 = "DURING";
					else if (lblC4.equals("DURING_INV")) lblC4 = "DURING";
					else if (lblC4.equals("IBEFORE")) lblC4 = "BEFORE";
					else if (lblC4.equals("IAFTER")) lblC4 = "AFTER";
					else if (lblC4.equals("BEGINS")) lblC4 = "DURING";
					else if (lblC4.equals("BEGUN_BY")) lblC4 = "DURING";
					else if (lblC4.equals("ENDS")) lblC4 = "DURING";
					else if (lblC4.equals("ENDED_BY")) lblC4 = "DURING";
					getVectors().add(lblC4);
					fields.add("labelCollapsed4");
					break;
				case labelCollapsed5:
					String lblC5 = getLabel();
					if (lblC5.equals("END")) lblC5 = "ENDS";
					else if (lblC5.equals("IDENTITY")) lblC5 = "SIMULTANEOUS";
					else if (lblC5.equals("DURING")) lblC5 = "DURING";
					else if (lblC5.equals("DURING_INV")) lblC5 = "DURING";
					else if (lblC5.equals("IBEFORE")) lblC5 = "BEFORE";
					else if (lblC5.equals("IAFTER")) lblC5 = "AFTER";
					else if (lblC5.equals("BEGINS")) lblC5 = "IS_INCLUDED";
					else if (lblC5.equals("BEGUN_BY")) lblC5 = "INCLUDES";
					else if (lblC5.equals("ENDS")) lblC5 = "IS_INCLUDED";
					else if (lblC5.equals("ENDED_BY")) lblC5 = "INCLUDES";
					getVectors().add(lblC5);
					fields.add("labelCollapsed5");
					break;
				case labelCollapsed6:
					String lblC6 = getLabel();
					if (lblC6.equals("END")) lblC6 = "ENDS";
					else if (lblC6.equals("IDENTITY")) lblC6 = "SIMULTANEOUS";
					else if (lblC6.equals("DURING")) lblC6 = "NONE";
					else if (lblC6.equals("DURING_INV")) lblC6 = "NONE";
					else if (lblC6.equals("IBEFORE")) lblC6 = "BEFORE";
					else if (lblC6.equals("IAFTER")) lblC6 = "AFTER";
					else if (lblC6.equals("BEGINS")) lblC6 = "NONE";
					else if (lblC6.equals("BEGUN_BY")) lblC6 = "NONE";
					else if (lblC6.equals("ENDS")) lblC6 = "NONE";
					else if (lblC6.equals("ENDED_BY")) lblC6 = "NONE";
					getVectors().add(lblC6);
					fields.add("labelCollapsed6");
					break;
				case labelCollapsed01:
					String lblC01 = getLabel();
					if (lblC01.equals("IDENTITY")) lblC01 = "SIMULTANEOUS";
					else if (lblC01.equals("IS_INCLUDED")) lblC01 = "INCLUDES";
					else if (lblC01.equals("IBEFORE")) lblC01 = "BEFORE";
					else if (lblC01.equals("IAFTER")) lblC01 = "AFTER";
					else if (lblC01.equals("BEGUN_BY")) lblC01 = "BEGINS";
					else if (lblC01.equals("ENDED_BY")) lblC01 = "ENDS";
					else if (lblC01.equals("DURING_INV")) lblC01 = "DURING";
					getVectors().add(lblC01);
					fields.add("labelCollapsed01");
					break;
				case labelCollapsed02:
					String lblC02 = getLabel();
					if (lblC02.equals("IDENTITY")) lblC02 = "SIMULTANEOUS";
					else if (lblC02.equals("IS_INCLUDED")) lblC02 = "INCLUDES";
					else if (lblC02.equals("IBEFORE")) lblC02 = "BEFORE";
					else if (lblC02.equals("IAFTER")) lblC02 = "AFTER";
					else if (lblC02.equals("BEGINS")) lblC02 = "DURING";
					else if (lblC02.equals("ENDS")) lblC02 = "DURING";
					else if (lblC02.equals("DURING")) lblC02 = "DURING";
					else if (lblC02.equals("BEGUN_BY")) lblC02 = "DURING_INV";
					else if (lblC02.equals("ENDED_BY")) lblC02 = "DURING_INV";
					else if (lblC02.equals("DURING_INV")) lblC02 = "DURING_INV";
					getVectors().add(lblC02);
					fields.add("labelCollapsed02");
					break;
				case labelCollapsed03:
					String lblC03 = getLabel();
					if (lblC03.equals("IDENTITY")) lblC03 = "SIMULTANEOUS";
					else if (lblC03.equals("IBEFORE")) lblC03 = "BEFORE";
					else if (lblC03.equals("IAFTER")) lblC03 = "AFTER";
					else if (lblC03.equals("BEGINS")) lblC03 = "DURING";
					else if (lblC03.equals("ENDS")) lblC03 = "DURING";
					else if (lblC03.equals("DURING")) lblC03 = "DURING";
					else if (lblC03.equals("BEGUN_BY")) lblC03 = "DURING_INV";
					else if (lblC03.equals("ENDED_BY")) lblC03 = "DURING_INV";
					else if (lblC03.equals("DURING_INV")) lblC03 = "DURING_INV";
					getVectors().add(lblC03);
					fields.add("labelCollapsed03");
					break;
			}
		}
	}
	
	public void addBinaryFeatureToVector(FeatureName feature) throws Exception {
		List<String> fields = null;
		if (this instanceof EventEventFeatureVector) {
			fields = EventEventFeatureVector.fields;
		} else if (this instanceof EventTimexFeatureVector) {
			fields = EventTimexFeatureVector.fields;
		}
		Marker m = null;
		String lblStr = null;
		if (fields != null) {
			switch(feature) {
				case id: 	
					getVectors().add(e1.getID());
					fields.add("id1");
					getVectors().add(e2.getID());
					fields.add("id2");
					break;
				case pos:
					String e1Pos = getTokenAttribute(e1, FeatureName.pos);
					String e2Pos = getTokenAttribute(e2, FeatureName.pos);
					for (String s : this.pos) {
						if (e1Pos.contains(s)) getVectors().add("1");
						else getVectors().add("0");
						fields.add("pos1_" + s);
					}
					for (String s : this.pos) {
						if (e2Pos.contains(s)) getVectors().add("1");
						else getVectors().add("0");
						fields.add("pos2_" + s);
					}
					break;
				case mainpos:
					String e1Mpos = getTokenAttribute(e1, FeatureName.mainpos);
					String e2Mpos = getTokenAttribute(e2, FeatureName.mainpos);
					for (String s : this.main_pos) {
						if (e1Mpos.contains(s)) getVectors().add("1");
						else getVectors().add("0");
						fields.add("mainpos1_" + s);
					}
					for (String s : this.main_pos) {
						if (e2Mpos.contains(s)) getVectors().add("1");
						else getVectors().add("0");
						fields.add("mainpos2_" + s);
					}
					break;
				case chunk:
					String e1Chunk = getTokenAttribute(e1, FeatureName.chunk);
					String e2Chunk = getTokenAttribute(e2, FeatureName.chunk);
					for (String s : this.chunk) {
						if (e1Chunk.contains(s)) getVectors().add("1");
						else getVectors().add("0");
						fields.add("chunk1_" + s);
					}
					for (String s : this.chunk) {
						if (e2Chunk.contains(s)) getVectors().add("1");
						else getVectors().add("0");
						fields.add("chunk2_" + s);
					}
					break;
				case samePos:
					getVectors().add(isSameTokenAttribute(FeatureName.pos) ? "1" : "0");
					fields.add("samePos");
					break;
				case sameMainPos:
					getVectors().add(isSameTokenAttribute(FeatureName.mainpos) ? "1" : "0");
					fields.add("sameMainPos");
					break;
				case entDistance:
					//getVectors().add(getEntityDistance().toString());
					if (getEntityDistance() > 0) {
						getVectors().add("1");
					} else if (getEntityDistance() < 0){
						getVectors().add("-1");
					} else {
						getVectors().add("0");
					}
					fields.add("entDistance");
					break;
				case sentDistance:
					//getVectors().add(getSentenceDistance().toString());
					if (getSentenceDistance() > 0) {
						getVectors().add("1");
					} else if (getSentenceDistance() < 0){
						getVectors().add("-1");
					} else {
						getVectors().add("0");
					}
					fields.add("sentDistance");
					break;
				case timexInBetween:
					if (isTimexInBetween()) {
						getVectors().add("1");
					} else {
						getVectors().add("0");
					}
					fields.add("timexInBetween");
					break;
				case entOrder:
					for (String s : this.ent_order) {
						if (s.equals(getOrder())) getVectors().add("1");
						else getVectors().add("0");
						fields.add("entOrder_" + s);
					}
					break;					
				case eventClass:
					if (this instanceof EventEventFeatureVector) {
						for (String s : this.ev_class) {
							if (s.equals(getEntityAttribute(e1, FeatureName.eventClass))) getVectors().add("1");
							else getVectors().add("0");
							fields.add("e1class_" + s);
						}
						for (String s : this.ev_class) {
							if (s.equals(getEntityAttribute(e2, FeatureName.eventClass))) getVectors().add("1");
							else getVectors().add("0");
							fields.add("e2class_" + s);
						}
					} else if (this instanceof EventTimexFeatureVector) {
						for (String s : this.ev_class) {
							if (s.equals(getEntityAttribute(e1, FeatureName.eventClass))) getVectors().add("1");
							else getVectors().add("0");
							fields.add("eclass_" + s);
						}
					}
					break;
				case tense:
					if (this instanceof EventEventFeatureVector) {
						for (String s : this.ev_tense) {
							if (s.equals(getEntityAttribute(e1, FeatureName.tense))) getVectors().add("1");
							else getVectors().add("0");
							fields.add("e1tense_" + s);
						}
						for (String s : this.ev_tense) {
							if (s.equals(getEntityAttribute(e2, FeatureName.tense))) getVectors().add("1");
							else getVectors().add("0");
							fields.add("e2tense_" + s);
						}
					} else if (this instanceof EventTimexFeatureVector) {
						for (String s : this.ev_tense) {
							if (s.equals(getEntityAttribute(e1, FeatureName.tense))) getVectors().add("1");
							else getVectors().add("0");
							fields.add("etense_" + s);
						}
					}
					break;
				case aspect:
					if (this instanceof EventEventFeatureVector) {
						for (String s : this.ev_aspect) {
							if (s.equals(getEntityAttribute(e1, FeatureName.aspect))) getVectors().add("1");
							else getVectors().add("0");
							fields.add("e1aspect_" + s);
						}
						for (String s : this.ev_aspect) {
							if (s.equals(getEntityAttribute(e2, FeatureName.aspect))) getVectors().add("1");
							else getVectors().add("0");
							fields.add("e2aspect_" + s);
						}
					} else if (this instanceof EventTimexFeatureVector) {
						for (String s : this.ev_aspect) {
							if (s.equals(getEntityAttribute(e1, FeatureName.aspect))) getVectors().add("1");
							else getVectors().add("0");
							fields.add("easpect_" + s);
						}
					}
					break;
				case polarity:
					if (this instanceof EventEventFeatureVector) {
						if (getEntityAttribute(e1, FeatureName.polarity).equals("neg")) getVectors().add("0");
						else getVectors().add("1");
						fields.add("e1polarity");
						if (getEntityAttribute(e2, FeatureName.polarity).equals("neg")) getVectors().add("0");
						else getVectors().add("1");
						fields.add("e2polarity");
					} else if (this instanceof EventTimexFeatureVector) {
						if (getEntityAttribute(e1, FeatureName.polarity).equals("neg")) getVectors().add("0");
						else getVectors().add("1");
						fields.add("epolarity");
					}
					break;
				case sameEventClass:
					getVectors().add(getEntityAttribute(e1, FeatureName.eventClass).equals(getEntityAttribute(e2, FeatureName.eventClass)) ? "1" : "0");
					fields.add("sameEventClass");
					break;
				case sameTense:
					getVectors().add(getEntityAttribute(e1, FeatureName.tense).equals(getEntityAttribute(e2, FeatureName.tense)) ? "1" : "0");
					fields.add("sameTense");
					break;
				case sameAspect:
					getVectors().add(getEntityAttribute(e1, FeatureName.aspect).equals(getEntityAttribute(e2, FeatureName.aspect)) ? "1" : "0");
					fields.add("sameAspect");
					break;
				case sameTenseAspect:
					getVectors().add((getEntityAttribute(e1, FeatureName.tense).equals(getEntityAttribute(e2, FeatureName.tense)) &&
							getEntityAttribute(e1, FeatureName.aspect).equals(getEntityAttribute(e2, FeatureName.aspect))) ? "1" : "0");
					fields.add("sameTenseAspect");
					break;
				case samePolarity:
					getVectors().add(getEntityAttribute(e1, FeatureName.polarity).equals(getEntityAttribute(e2, FeatureName.polarity)) ? "1" : "0");
					fields.add("samePolarity");
					break;
				case timexType:
					for (String s : this.tmx_type) {
						if (s.equals(getEntityAttribute(e2, FeatureName.timexType))) getVectors().add("1");
						else getVectors().add("0");
						fields.add("ttype_" + s);
					}
					break;
				case mainVerb:
					if (this instanceof EventEventFeatureVector) {
						if (getMateMainVerb(e1).equals("MAIN")) getVectors().add("1");
						else getVectors().add("0");
						fields.add("e1_mainverb");
						if (getMateMainVerb(e2).equals("MAIN")) getVectors().add("1");
						else getVectors().add("0");
						fields.add("e2_mainverb");
					} else if (this instanceof EventTimexFeatureVector) {
						if (getMateMainVerb(e1).equals("MAIN")) getVectors().add("1");
						else getVectors().add("0");
						fields.add("e_mainverb");
					}
					break;
					
				case depTmxPath:
					String depTmxPathStr = ((EventTimexFeatureVector) this).getMateDependencyPath();
					if (depTmxPathStr.equals("TMP-PMOD")) {
						getVectors().add("1"); 
					} else {
						getVectors().add("0");
					}
					if (!depTmxPathStr.equals("TMP-PMOD")
							&& depTmxPathStr.contains("TMP-PMOD")) {
						getVectors().add("1"); 
					} else {
						getVectors().add("0");
					}
					fields.add("depTmxPath1");
					fields.add("depTmxPath2");
					break;
					
				case depEvPath:
					String depEvPathStr = ((EventEventFeatureVector) this).getMateDependencyPath();
					
					for (String s : this.dep_event_path) {
						if (s.equals(depEvPathStr)) getVectors().add("1");
						else getVectors().add("0");
						fields.add("depevpath_" + s.replace(" ", "_"));
					}
					break;
					
				case modalVerb:
					if (this instanceof EventEventFeatureVector) {
						for (String modal : modalVerbs) {
							if (modal.equals(getMateModalVerb(e1.getStartTokID()))) getVectors().add("1");
							else getVectors().add("0");
							fields.add("modal1_"+modal);
						}
						for (String modal : modalVerbs) {
							if (modal.equals(getMateModalVerb(e2.getStartTokID()))) getVectors().add("1");
							else getVectors().add("0");
							fields.add("modal2_"+modal);
						}
					} else if (this instanceof EventTimexFeatureVector) {
						for (String modal : modalVerbs) {
							if (modal.equals(getMateModalVerb(e1.getStartTokID()))) getVectors().add("1");
							else getVectors().add("0");
							fields.add("modal_"+modal);
						}
					}
					break;
					
				case hasModal:
					if (this instanceof EventEventFeatureVector) {
						if (!getMateModalVerb(e1.getStartTokID()).equals("O")) getVectors().add("1");
						else getVectors().add("0");
						fields.add("modal_1");
						if (!getMateModalVerb(e2.getStartTokID()).equals("O")) getVectors().add("1");
						else getVectors().add("0");
						fields.add("modal_2");
					} else if (this instanceof EventTimexFeatureVector) {
						if (!getMateModalVerb(e1.getStartTokID()).equals("O")) getVectors().add("1");
						else getVectors().add("0");
						fields.add("modal");
					}
					break;	
					
				case tempSignalClusText:
					m = getTemporalSignal();
					for (String s : this.temp_signal_timex) {
						if (s.equals(m.getCluster())) getVectors().add("1");
						else getVectors().add("0");
						fields.add("tempsig_" + s.replace(" ", "_"));
					}
					for (String s : this.temp_signal_event) {
						if (s.equals(m.getCluster())) getVectors().add("1");
						else getVectors().add("0");
						fields.add("tempsig_" + s.replace(" ", "_"));
					}
					break;
				case tempSignalPos:
					m = getTemporalSignal();
					for (String s : this.marker_position) {
						if (s.equals(m.getCluster())) getVectors().add("1");
						else getVectors().add("0");
						fields.add("tempsigpos_" + s);
					}
					break;
				case tempSignalDep1Dep2:
					m = getTemporalSignal();
					if (m.getCluster().equals("O")) {
						for (int i=0; i<(this.dep_signal_path.length*2); i++) getVectors().add("0");
					} else {
						for (String s : this.dep_signal_path) {
							if (m.getDepRelE1().contains(s)) getVectors().add("1");
							else getVectors().add("0");
						}
						for (String s : this.dep_signal_path) {
							if (m.getDepRelE2().contains(s)) getVectors().add("1");
							else getVectors().add("0");
						}
					}
					for (String s : this.dep_signal_path) {
						fields.add("tempsigdep1_" + s);
					}
					for (String s : this.dep_signal_path) {
						fields.add("tempsigdep2_" + s);
					}
					break;
					
				case tempSignal1ClusText:
					m = getTemporalSignalPerEntity(e1);
					for (String s : this.temp_signal_event) {
						if (s.equals(m.getCluster())) getVectors().add("1");
						else getVectors().add("0");
						fields.add("tempsig1_" + s.replace(" ", "_"));
					}
					break;
				case tempSignal1Pos:
					m = getTemporalSignalPerEntity(e1);
					for (String s : this.marker_position) {
						if (s.equals(m.getCluster())) getVectors().add("1");
						else getVectors().add("0");
						fields.add("tempsigpos1_" + s);
					}
					break;
				case tempSignal1Dep:
					m = getTemporalSignalPerEntity(e1);
					for (String s : this.dep_signal_path) {
						if (m.getDepRelE1().contains(s)) getVectors().add("1");
						else getVectors().add("0");
						fields.add("tempsigdep1_" + s);
					}
					break;
				case tempSignal2ClusText:
					if (this instanceof EventTimexFeatureVector) {
						//Assuming that the pair is already in event-timex order
						if ((e2 instanceof Timex && ((Timex)e2).isDct()) || (e2 instanceof Timex && ((Timex)e2).isEmptyTag()) ||
							!isSameSentence()) {
							for (String s : this.temp_signal_timex) {
								getVectors().add("0");
								fields.add("tempsig2_" + s.replace(" ", "_"));
							}
						} else {	
							m = getTemporalSignalPerEntity(e2);
							for (String s : this.temp_signal_timex) {
								if (s.equals(m.getCluster())) getVectors().add("1");
								else getVectors().add("0");
								fields.add("tempsig2_" + s.replace(" ", "_"));
							}
						}
					} else if (this instanceof EventEventFeatureVector) {
						m = getTemporalSignalPerEntity(e2);
						for (String s : this.temp_signal_event) {
							if (s.equals(m.getCluster())) getVectors().add("1");
							else getVectors().add("0");
							fields.add("tempsig2_" + s.replace(" ", "_"));
						}
					}
					break;
				case tempSignal2Pos:
					if (this instanceof EventTimexFeatureVector) {
						//Assuming that the pair is already in event-timex order
						if ((e2 instanceof Timex && ((Timex)e2).isDct()) || (e2 instanceof Timex && ((Timex)e2).isEmptyTag()) ||
							!isSameSentence()) {
							for (String s : this.marker_position) {
								getVectors().add("0");
								fields.add("tempsigpos2_" + s);
							}
						} else {	
							m = getTemporalSignalPerEntity(e2);
							for (String s : this.marker_position) {
								if (s.equals(m.getCluster())) getVectors().add("1");
								else getVectors().add("0");
								fields.add("tempsigpos2_" + s);
							}
						}
					} else if (this instanceof EventEventFeatureVector) {
						m = getTemporalSignalPerEntity(e2);
						for (String s : this.marker_position) {
							if (s.equals(m.getCluster())) getVectors().add("1");
							else getVectors().add("0");
							fields.add("tempsigpos2_" + s);
						}
					}
					break;
					
				case tempSignal2Dep:
					if (this instanceof EventTimexFeatureVector) {
						//Assuming that the pair is already in event-timex order
						if ((e2 instanceof Timex && ((Timex)e2).isDct()) || (e2 instanceof Timex && ((Timex)e2).isEmptyTag()) ||
							!isSameSentence()) {
							for (String s : this.dep_signal_path) {
								getVectors().add("0");
								fields.add("tempsigdep2_" + s);
							}
						} else {	
							m = getTemporalSignalPerEntity(e2);
							for (String s : this.dep_signal_path) {
								if (m.getDepRelE1().contains(s)) getVectors().add("1");
								else getVectors().add("0");
								fields.add("tempsigdep2_" + s);
							}
						}
					} else if (this instanceof EventEventFeatureVector) {
						m = getTemporalSignalPerEntity(e2);
						for (String s : this.dep_signal_path) {
							if (m.getDepRelE1().contains(s)) getVectors().add("1");
							else getVectors().add("0");
							fields.add("tempsigdep2_" + s);
						}
					}
					break;
					
				case causMarkerClusText:
					m = getCausalSignal();
					if (m.getCluster().equals("O")) {
						for (int i=0; i<this.caus_signal.length; i++) getVectors().add("0");
						m = getCausalVerb();
						if (m.getCluster().equals("O")) {
							for (int i=0; i<this.caus_verb.length; i++) getVectors().add("0");
						} else {
							for (String s : this.caus_verb) {
								if (s.equals(m.getCluster())) getVectors().add("1");
								else getVectors().add("0");
							}
						}
					} else {
						for (String s : this.caus_signal) {
							if (s.equals(m.getCluster())) getVectors().add("1");
							else getVectors().add("0");
						}
						for (int i=0; i<this.caus_verb.length; i++) getVectors().add("0");
					}
					for (String s : this.caus_signal) {
						fields.add("caussig_" + s.replace(" ", "_"));
					}
					for (String s : this.caus_verb) {
						fields.add("causverb_" + s.replace(" ", "_"));
					}
					break;
				case causMarkerPos:
					m = getCausalSignal();
					if (m.getPosition().equals("O")) {
						m = getCausalVerb();
						if (m.getPosition().equals("O")) {
							for (int i=0; i<this.marker_position.length; i++) getVectors().add("0");
						} else {
							for (String s : this.marker_position) {
								if (s.equals(m.getPosition())) getVectors().add("1");
								else getVectors().add("0");
							}
						}
					} else {
						for (String s : this.marker_position) {
							if (s.equals(m.getPosition())) getVectors().add("1");
							else getVectors().add("0");
						}
					}
					for (String s : this.marker_position) {
						fields.add("causmarkpos_" + s);
					}
					break;
				case causMarkerDep1Dep2:
					m = getCausalSignal();
					if (m.getCluster().equals("O")) {
						m = getCausalVerb();
						if (m.getCluster().equals("O")) {
							for (int i=0; i<(this.dep_signal_path.length*2); i++) getVectors().add("0");
						} else {
							for (String s : this.dep_signal_path) {
								if (m.getDepRelE1().contains(s)) getVectors().add("1");
								else getVectors().add("0");
							}
							for (String s : this.dep_signal_path) {
								if (m.getDepRelE2().contains(s)) getVectors().add("1");
								else getVectors().add("0");
							}
						}
					} else {
						for (String s : this.dep_signal_path) {
							if (m.getDepRelE1().contains(s)) getVectors().add("1");
							else getVectors().add("0");
						}
						for (String s : this.dep_signal_path) {
							if (m.getDepRelE2().contains(s)) getVectors().add("1");
							else getVectors().add("0");
						}
					}
					for (String s : this.dep_signal_path) {
						fields.add("caussigdep_" + s);
					}
					for (String s : this.dep_signal_path) {
						fields.add("caussigdep_" + s);
					}
					break;
				case causSignalClusText:
					m = getCausalSignal();
					if (m.getCluster().equals("O")) {
						for (int i=0; i<this.caus_signal.length; i++) getVectors().add("0");
					} else {
						for (String s : this.caus_signal) {
							if (s.equals(m.getCluster())) getVectors().add("1");
							else getVectors().add("0");
						}
					}
					for (String s : this.caus_signal) {
						fields.add("caussig_" + s.replace(" ", "_"));
					}
					break;
				case causSignalPos:
					m = getCausalSignal();
					if (m.getPosition().equals("O")) {
						for (int i=0; i<this.marker_position.length; i++) getVectors().add("0");
					} else {
						for (String s : this.marker_position) {
							if (s.equals(m.getPosition())) getVectors().add("1");
							else getVectors().add("0");
						}
					}
					for (String s : this.marker_position) {
						fields.add("caussigpos_" + s);
					}
					break;
				case causSignalDep1Dep2:
					m = getCausalSignal();
					if (m.getCluster().equals("O")) {
						for (int i=0; i<(this.dep_signal_path.length*2); i++) getVectors().add("0");
					} else {
//						String dependency = getSimplifiedSignalDependencyPath(m);
						
						for (String s : this.dep_signal_path) {
							if (m.getDepRelE1().contains(s)) getVectors().add("1");
							else getVectors().add("0");
						}
						for (String s : this.dep_signal_path) {
							if (m.getDepRelE2().contains(s)) getVectors().add("1");
							else getVectors().add("0");
						}
						
//						for (String s : this.dep_signal_path) {
//							if (dependency.contains(s)) getVectors().add("1");
//							else getVectors().add("0");
//						}
					}
					for (String s : this.dep_signal_path) {
						fields.add("caussigdep1_" + s);
					}
					for (String s : this.dep_signal_path) {
						fields.add("caussigdep2_" + s);
					}
//					for (String s : this.dep_signal_path) {
//						fields.add("caussigdep_" + s);
//					}
					break;
				case coref:
					getVectors().add(((EventEventFeatureVector) this).isCoreference() ? "1" : "0");
					fields.add("coref");
					break;
				case wnSim:
					//getVectors().add(((EventEventFeatureVector) this).getWordSimilarity().toString());
					/*if (((EventEventFeatureVector) this).getWordSimilarity() > 0) {
						getVectors().add("1");
					} else {
						getVectors().add("0");
					}*/
					getVectors().add(((EventEventFeatureVector) this).getDiscreteDoubleWordSimilarity().toString());
					fields.add("wnSim");
					break;
				case timexRule:
					for (String s : this.timex_rule) {
						if (s.equals(((EventTimexFeatureVector) this).getTimexRule())) getVectors().add("1");
						else getVectors().add("0");
						fields.add("timexrule_" + s);
					}
					break;
				case label:
					String lbl = getLabel();
					if (lbl.equals("END")) lbl = "ENDS";
					getVectors().add(String.valueOf(temp_rel_type_list.indexOf(lbl)+1));
					fields.add("label");
					break;
				case labelCaus:
					String lblCaus = getLabel();
					getVectors().add(String.valueOf(caus_rel_type_list.indexOf(lblCaus)+1));
					fields.add("label");
					break;
				case labelCollapsed:
					String lblC = getLabel();
					if (lblC.equals("END")) lblC = "ENDS";
					else if (lblC.equals("DURING")) lblC = "SIMULTANEOUS";
					else if (lblC.equals("DURING_INV")) lblC = "SIMULTANEOUS";
					else if (lblC.equals("IBEFORE")) lblC = "BEFORE";
					else if (lblC.equals("IAFTER")) lblC = "AFTER";
					getVectors().add(String.valueOf(temp_rel_type_list.indexOf(lblC)+1));
					fields.add("labelCollapsed");
					break;
				case labelBinary:
					String lblB = getLabel();
					if (!lblB.equals("NONE")) getVectors().add("1");
					else getVectors().add("0");
					fields.add("labelBinary");
					break;
				case labelDense:
					String lblDense = getLabel();
					getVectors().add(String.valueOf(temp_dense_rel_type_list.indexOf(lblDense)+1));
					fields.add("labelDense");
					break;	
				case labelCollapsed1:
					String lblC1 = getLabel();
					if (lblC1.equals("END")) lblC1 = "ENDS";
					else if (lblC1.equals("IDENTITY")) lblC1 = "SIMULTANEOUS";
					else if (lblC1.equals("DURING")) lblC1 = "IS_INCLUDED";
					else if (lblC1.equals("DURING_INV")) lblC1 = "INCLUDES";
					else if (lblC1.equals("IBEFORE")) lblC1 = "BEFORE";
					else if (lblC1.equals("IAFTER")) lblC1 = "AFTER";
					else if (lblC1.equals("BEGINS")) lblC1 = "BEFORE";
					else if (lblC1.equals("BEGUN_BY")) lblC1 = "AFTER";
					else if (lblC1.equals("ENDS")) lblC1 = "AFTER";
					else if (lblC1.equals("ENDED_BY")) lblC1 = "BEFORE";
					getVectors().add(String.valueOf(temp_rel_type_list.indexOf(lblC1)+1));
					fields.add("labelCollapsed1");
					break;
				case labelCollapsed2:
					String lblC2 = getLabel();
					if (lblC2.equals("END")) lblC2 = "ENDS";
					else if (lblC2.equals("IDENTITY")) lblC2 = "SIMULTANEOUS";
					else if (lblC2.equals("DURING")) lblC2 = "IS_INCLUDED";
					else if (lblC2.equals("DURING_INV")) lblC2 = "INCLUDES";
					else if (lblC2.equals("IBEFORE")) lblC2 = "BEFORE";
					else if (lblC2.equals("IAFTER")) lblC2 = "AFTER";
					else if (lblC2.equals("BEGINS")) lblC2 = "IS_INCLUDED";
					else if (lblC2.equals("BEGUN_BY")) lblC2 = "INCLUDES";
					else if (lblC2.equals("ENDS")) lblC2 = "IS_INCLUDED";
					else if (lblC2.equals("ENDED_BY")) lblC2 = "INCLUDES";
					getVectors().add(String.valueOf(temp_rel_type_list.indexOf(lblC2)+1));
					fields.add("labelCollapsed2");
					break;
				case labelCollapsed3:
					String lblC3 = getLabel();
					if (lblC3.equals("END")) lblC3 = "ENDS";
					else if (lblC3.equals("IDENTITY")) lblC3 = "SIMULTANEOUS";
					else if (lblC3.equals("IBEFORE")) lblC3 = "BEFORE";
					else if (lblC3.equals("IAFTER")) lblC3 = "AFTER";
					else if (lblC3.equals("BEGINS")) lblC3 = "DURING";
					else if (lblC3.equals("BEGUN_BY")) lblC3 = "DURING_INV";
					else if (lblC3.equals("ENDS")) lblC3 = "DURING";
					else if (lblC3.equals("ENDED_BY")) lblC3 = "DURING_INV";
					getVectors().add(String.valueOf(temp_rel_type_list.indexOf(lblC3)+1));
					fields.add("labelCollapsed3");
					break;
				case labelCollapsed4:
					String lblC4 = getLabel();
					if (lblC4.equals("END")) lblC4 = "ENDS";
					else if (lblC4.equals("IDENTITY")) lblC4 = "SIMULTANEOUS";
					else if (lblC4.equals("IS_INCLUDED")) lblC4 = "INCLUDES";
					else if (lblC4.equals("DURING")) lblC4 = "DURING";
					else if (lblC4.equals("DURING_INV")) lblC4 = "DURING";
					else if (lblC4.equals("IBEFORE")) lblC4 = "BEFORE";
					else if (lblC4.equals("IAFTER")) lblC4 = "AFTER";
					else if (lblC4.equals("BEGINS")) lblC4 = "DURING";
					else if (lblC4.equals("BEGUN_BY")) lblC4 = "DURING";
					else if (lblC4.equals("ENDS")) lblC4 = "DURING";
					else if (lblC4.equals("ENDED_BY")) lblC4 = "DURING";
					getVectors().add(String.valueOf(temp_rel_type_list.indexOf(lblC4)+1));
					fields.add("labelCollapsed4");
					break;
				case labelCollapsed5:
					String lblC5 = getLabel();
					if (lblC5.equals("END")) lblC5 = "ENDS";
					else if (lblC5.equals("IDENTITY")) lblC5 = "SIMULTANEOUS";
					else if (lblC5.equals("DURING")) lblC5 = "DURING";
					else if (lblC5.equals("DURING_INV")) lblC5 = "DURING";
					else if (lblC5.equals("IBEFORE")) lblC5 = "BEFORE";
					else if (lblC5.equals("IAFTER")) lblC5 = "AFTER";
					else if (lblC5.equals("BEGINS")) lblC5 = "IS_INCLUDED";
					else if (lblC5.equals("BEGUN_BY")) lblC5 = "INCLUDES";
					else if (lblC5.equals("ENDS")) lblC5 = "IS_INCLUDED";
					else if (lblC5.equals("ENDED_BY")) lblC5 = "INCLUDES";
					getVectors().add(String.valueOf(temp_rel_type_list.indexOf(lblC5)+1));
					fields.add("labelCollapsed5");
					break;
				case labelCollapsed6:
					String lblC6 = getLabel();
					if (lblC6.equals("END")) lblC6 = "ENDS";
					else if (lblC6.equals("IDENTITY")) lblC6 = "SIMULTANEOUS";
					else if (lblC6.equals("DURING")) lblC6 = "NONE";
					else if (lblC6.equals("DURING_INV")) lblC6 = "NONE";
					else if (lblC6.equals("IBEFORE")) lblC6 = "BEFORE";
					else if (lblC6.equals("IAFTER")) lblC6 = "AFTER";
					else if (lblC6.equals("BEGINS")) lblC6 = "NONE";
					else if (lblC6.equals("BEGUN_BY")) lblC6 = "NONE";
					else if (lblC6.equals("ENDS")) lblC6 = "NONE";
					else if (lblC6.equals("ENDED_BY")) lblC6 = "NONE";
					getVectors().add(String.valueOf(temp_rel_type_list.indexOf(lblC6)+1));
					fields.add("labelCollapsed6");
					break;
				case labelCollapsed01:
					String lblC01 = getLabel();
					if (lblC01.equals("IDENTITY")) lblC01 = "SIMULTANEOUS";
					else if (lblC01.equals("IS_INCLUDED")) lblC01 = "INCLUDES";
					else if (lblC01.equals("IBEFORE")) lblC01 = "BEFORE";
					else if (lblC01.equals("IAFTER")) lblC01 = "AFTER";
					else if (lblC01.equals("BEGUN_BY")) lblC01 = "BEGINS";
					else if (lblC01.equals("ENDED_BY")) lblC01 = "ENDS";
					else if (lblC01.equals("DURING_INV")) lblC01 = "DURING";
					getVectors().add(String.valueOf(temp_rel_type_list.indexOf(lblC01)+1));
					fields.add("labelCollapsed01");
					break;
				case labelCollapsed02:
					String lblC02 = getLabel();
					if (lblC02.equals("IDENTITY")) lblC02 = "SIMULTANEOUS";
					else if (lblC02.equals("IS_INCLUDED")) lblC02 = "INCLUDES";
					else if (lblC02.equals("IBEFORE")) lblC02 = "BEFORE";
					else if (lblC02.equals("IAFTER")) lblC02 = "AFTER";
					else if (lblC02.equals("BEGINS")) lblC02 = "DURING";
					else if (lblC02.equals("ENDS")) lblC02 = "DURING";
					else if (lblC02.equals("DURING")) lblC02 = "DURING";
					else if (lblC02.equals("BEGUN_BY")) lblC02 = "DURING_INV";
					else if (lblC02.equals("ENDED_BY")) lblC02 = "DURING_INV";
					else if (lblC02.equals("DURING_INV")) lblC02 = "DURING_INV";
					getVectors().add(String.valueOf(temp_rel_type_list.indexOf(lblC02)+1));
					fields.add("labelCollapsed02");
					break;
				case labelCollapsed03:
					String lblC03 = getLabel();
					if (lblC03.equals("IDENTITY")) lblC03 = "SIMULTANEOUS";
					else if (lblC03.equals("IBEFORE")) lblC03 = "BEFORE";
					else if (lblC03.equals("IAFTER")) lblC03 = "AFTER";
					else if (lblC03.equals("BEGINS")) lblC03 = "DURING";
					else if (lblC03.equals("ENDS")) lblC03 = "DURING";
					else if (lblC03.equals("DURING")) lblC03 = "DURING";
					else if (lblC03.equals("BEGUN_BY")) lblC03 = "DURING_INV";
					else if (lblC03.equals("ENDED_BY")) lblC03 = "DURING_INV";
					else if (lblC03.equals("DURING_INV")) lblC03 = "DURING_INV";
					getVectors().add(String.valueOf(temp_rel_type_list.indexOf(lblC03)+1));
					fields.add("labelCollapsed03");
					break;
				
			}
		}
	}
	
	public String getLabelFromNum(String num) {
		return temp_rel_type_list.get(Integer.valueOf(num)-1);
	}
	
	public void addPhraseFeatureToVector(FeatureName feature) throws Exception {
		List<String> fields = null;
		if (this instanceof EventEventFeatureVector) {
			fields = EventEventFeatureVector.fields;
		} else if (this instanceof EventTimexFeatureVector) {
			fields = EventTimexFeatureVector.fields;
		}
		Marker m = null;
		String[] embed = null;
		int i;
		if (fields != null) {
			switch(feature) {
				case tempMarkerTextPhrase:
					m = getTemporalMarkerFeature();
					embed = getPhraseEmbedding("http://137.132.82.174:8080/", m.getText());
					if (!m.getText().equals("O")) {
						i = 0;
						for (String s : embed) {
							getVectors().add(s);
							fields.add("tempmark_embed_" + i);
							i += 1;
						}
					} else {
						i = 0;
						for (String s : embed) {
							getVectors().add("0");
							fields.add("tempmark_embed_" + i);
							i += 1;
						}
					}
					break;
				case causMarkerTextPhrase:
					m = getCausalMarkerFeature();
					embed = getPhraseEmbedding("http://137.132.82.174:8080/", m.getText());
					if (!m.getText().equals("O")) {
						i = 0;
						for (String s : embed) {
							getVectors().add(s);
							fields.add("causmark_embed_" + i);
							i += 1;
						}
					} else {
						i = 0;
						for (String s : embed) {
							getVectors().add("0");
							fields.add("causmark_embed_" + i);
							i += 1;
						}
					}
					break;
				case tokenChunk:
					embed = getPhraseEmbedding("http://137.132.82.174:8080/", getWholeChunkToken(e1));
					i = 0;
					for (String s : embed) {
						getVectors().add(s);
						fields.add("tokenchunk_embed_" + i);
						i += 1;
					}
					embed = getPhraseEmbedding("http://137.132.82.174:8080/", getWholeChunkToken(e2));
					for (String s : embed) {
						getVectors().add(s);
						fields.add("tokenchunk_embed_" + i);
						i += 1;
					}
					break;
			}
		}
	}
	
	public static String[] getPhraseEmbedding(String urlStr, String phrase) throws Exception {
		URL url = new URL(urlStr);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setUseCaches(false);
		conn.setAllowUserInteraction(false);
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

		// Create the form content
		OutputStream out = conn.getOutputStream();
		Writer writer = new OutputStreamWriter(out, "UTF-8");
		writer.write("sentence=");
		writer.write(URLEncoder.encode(phrase, "UTF-8"));
		writer.close();
		out.close();

		if (conn.getResponseCode() != 200) {
			throw new IOException(conn.getResponseMessage());
		}

		// Buffer the result into a string
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = rd.readLine()) != null) {
			sb.append(line);
		}
		rd.close();

		conn.disconnect();
		return sb.toString().split(",");
	}

	public double[] getFeatures() {
		return features;
	}

	public void setFeatures(double[] features) {
		this.features = features;
	}
	
	public String toString() {
		return StringUtils.join(getVectors(), ",");
	}
	
	public String toFeatureString() {
		return StringUtils.join(Arrays.asList(getFeatures()), ",");
	}
	
	public String toCSVString() {
		String res = "";
		for (int i=0; i<features.length-1; i++) {
			res += features[i] + ",";
		}
		return res.substring(0, res.length()-1);
	}
	
	public String vecToCSVString() {
		String res = "";
		for (int i=0; i<vectors.size()-1; i++) {
			res += vectors.get(i) + ",";
		}
		return res.substring(0, res.length()-1);
	}
}
