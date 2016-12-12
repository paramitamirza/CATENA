package catena.parser.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Doc {
	
	private String filename;
	private EntityEnum.Language lang;
	private Map<String, Token> tokens;
	private Map<String, Sentence> sentences;
	private Map<String, Entity> entities;
	private ArrayList<String> tokenArr;
	private ArrayList<String> sentenceArr;
	private Map<String, TemporalSignal> temporalSignals;
	private Map<String, CausalSignal> causalSignals;
	private ArrayList<TemporalRelation> tlinks;
	private ArrayList<CausalRelation> clinks;
	private Timex dct;
	private Map<String, String> instances;
	private Map<String, String> instancesInv;
	
	private Map<String, String> tlinkTypes;
	private Map<String, String> clinkTypes;
	
	private ArrayList<TemporalRelation> candidateTlinks;
	private ArrayList<CausalRelation> candidateClinks;
	
	private int tokIdx;
	private int sentIdx;
	private int entIdx;
	
	public Doc() {
		this.setLang(EntityEnum.Language.EN);
		setTokens(new HashMap<String, Token>());
		setSentences(new HashMap<String, Sentence>());
		setEntities(new HashMap<String, Entity>());
		setTokIdx(0);
		setSentIdx(0);
		setEntIdx(0);
		setTokenArr(new ArrayList<String>());
		setSentenceArr(new ArrayList<String>());
		setTemporalSignals(new HashMap<String, TemporalSignal>());
		setCausalSignals(new HashMap<String, CausalSignal>());
		setTlinks(new ArrayList<TemporalRelation>());
		setClinks(new ArrayList<CausalRelation>());
		setInstances(new HashMap<String, String>());
		setInstancesInv(new HashMap<String, String>());
		setTlinkTypes(new HashMap<String, String>());
		setClinkTypes(new HashMap<String, String>());
		setCandidateTlinks(new ArrayList<TemporalRelation>());
		setCandidateClinks(new ArrayList<CausalRelation>());
	}
	
	public Doc(EntityEnum.Language lang) {
		this();
		this.setLang(lang);
	}
	
	public Doc(EntityEnum.Language lang, String filename) {
		this();
		this.setLang(lang);
		this.setFilename(filename);
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public EntityEnum.Language getLang() {
		return lang;
	}

	public void setLang(EntityEnum.Language lang) {
		this.lang = lang;
	}

	public Map<String, Token> getTokens() {
		return tokens;
	}

	public void setTokens(Map<String, Token> tokens) {
		this.tokens = tokens;
	}

	public Map<String, Sentence> getSentences() {
		return sentences;
	}

	public void setSentences(Map<String, Sentence> sentences) {
		this.sentences = sentences;
	}

	public Map<String, Entity> getEntities() {
		return entities;
	}

	public void setEntities(Map<String, Entity> entities) {
		this.entities = entities;
	}

	public Map<String, TemporalSignal> getTemporalSignals() {
		return temporalSignals;
	}

	public void setTemporalSignals(Map<String, TemporalSignal> temporalSignals) {
		this.temporalSignals = temporalSignals;
	}

	public Map<String, CausalSignal> getCausalSignals() {
		return causalSignals;
	}

	public void setCausalSignals(Map<String, CausalSignal> causalSignals) {
		this.causalSignals = causalSignals;
	}

	public ArrayList<TemporalRelation> getTlinks() {
		return tlinks;
	}

	public void setTlinks(ArrayList<TemporalRelation> tlinks) {
		this.tlinks = tlinks;
	}

	public ArrayList<CausalRelation> getClinks() {
		return clinks;
	}

	public void setClinks(ArrayList<CausalRelation> clinks) {
		this.clinks = clinks;
	}

	public int getTokIdx() {
		return tokIdx;
	}

	public void setTokIdx(int tokIdx) {
		this.tokIdx = tokIdx;
	}

	public int getSentIdx() {
		return sentIdx;
	}

	public void setSentIdx(int sentIdx) {
		this.sentIdx = sentIdx;
	}

	public int getEntIdx() {
		return entIdx;
	}

	public void setEntIdx(int entIdx) {
		this.entIdx = entIdx;
	}

	public ArrayList<String> getTokenArr() {
		return tokenArr;
	}

	public void setTokenArr(ArrayList<String> tokenArr) {
		this.tokenArr = tokenArr;
	}

	public ArrayList<String> getSentenceArr() {
		return sentenceArr;
	}

	public void setSentenceArr(ArrayList<String> sentenceArr) {
		this.sentenceArr = sentenceArr;
	}

	public Timex getDct() {
		return dct;
	}

	public void setDct(Timex dct) {
		this.dct = dct;
	}

	public Map<String, String> getInstances() {
		return instances;
	}

	public void setInstances(Map<String, String> instances) {
		this.instances = instances;
	}

	public Map<String, String> getInstancesInv() {
		return instancesInv;
	}

	public void setInstancesInv(Map<String, String> instancesInv) {
		this.instancesInv = instancesInv;
	}

	public Map<String, String> getTlinkTypes() {
		return tlinkTypes;
	}
	
	public Map<String, String> getTlinkTypes(Map<String, String> relTypeMapping) {
		Map<String, String> newTlinkTypes = new HashMap<String, String>();
		for (String key : tlinkTypes.keySet()) {
			if (relTypeMapping.containsKey(tlinkTypes.get(key))) {
				newTlinkTypes.put(key, relTypeMapping.get(tlinkTypes.get(key)));
			} else {
				newTlinkTypes.put(key, tlinkTypes.get(key));
			}
		}
		return newTlinkTypes;
	}

	public void setTlinkTypes(Map<String, String> tlinkTypes) {
		this.tlinkTypes = tlinkTypes;
	}

	public Map<String, String> getClinkTypes() {
		return clinkTypes;
	}

	public void setClinkTypes(Map<String, String> clinkTypes) {
		this.clinkTypes = clinkTypes;
	}

	public ArrayList<TemporalRelation> getCandidateTlinks() {
		return candidateTlinks;
	}

	public void setCandidateTlinks(ArrayList<TemporalRelation> candidateTlinks) {
		this.candidateTlinks = candidateTlinks;
	}

	public ArrayList<CausalRelation> getCandidateClinks() {
		return candidateClinks;
	}

	public void setCandidateClinks(ArrayList<CausalRelation> candidateClinks) {
		this.candidateClinks = candidateClinks;
	}
}
