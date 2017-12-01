package catena.parser.entities;

import java.util.HashMap;
import java.util.Map;

import catena.model.feature.FeatureEnum.FeatureName;

public class Token {
	
	private String ID;
	private String sentID;
	private Integer index;
	private String text;
	private String lemma;
	private String pos;
	private String mainPos;
	private String chunk;
	private String tense;
	private String aspect;
	private String polarity;
	private String namedEntity;
	private String wnSupersense;
	private String discourseConn;
	private Boolean mainVerb;
	private Map<String, String> dependencyRel;
	private String incomingDep;
	private String eventID;
	private String timexID;
	private String tSignalID;
	private String cSignalID;
	
	public Token(String id) {
		this.setID(id);
		dependencyRel = new HashMap<String, String>();
	}
	
	public Token(String id, String sentid, String text) {
		this.ID = id;
		this.sentID = sentid;
		this.text = text;
		dependencyRel = new HashMap<String, String>();
	}
	
	public void setLemmaPosChunk(String lemma, String pos, String chunk) {
		this.lemma = lemma;
		this.pos = pos;
		this.chunk = chunk;
	}
	
	public void setTimeEntities(String eventid, String timexid, String csignalid) {
		this.eventID = eventid;
		this.timexID = timexid;
		this.cSignalID = csignalid;
	}
	
	public void setDependencyInfo(Boolean main, Map<String, String> dependencyRel) {
		this.mainVerb = main;
		this.dependencyRel = dependencyRel;
	}
	
	public String getTokenText(FeatureName feature) {
		switch (feature) {
			case token: return this.getText(); 
			case lemma: return this.getLemma();
			default: return this.getText() + "\t" + this.getLemma();
		}
	}
	
	public String getTokenAttribute(FeatureName feature) {
		switch (feature) {
			case token: return this.getText(); 
			case lemma: return this.getLemma();
			case pos: return this.getPos(); 
			case mainpos: return this.getMainPos();
			case chunk: return this.getChunk();
			case ner: return this.getNamedEntity();
			case supersense: return this.getWnSupersense();
			default: return this.getText() + "\t" + this.getLemma() + "\t" +
				this.getPos() + "\t" + this.getMainPos() + "\t" + this.getChunk() + "\t" + 
				this.getNamedEntity() + "\t" + this.getWnSupersense();
		}
	}

	public String getID() {
		return ID;
	}

	public void setID(String iD) {
		ID = iD;
	}

	public String getSentID() {
		return sentID;
	}

	public void setSentID(String sentID) {
		this.sentID = sentID;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getLemma() {
		return lemma;
	}

	public void setLemma(String lemma) {
		this.lemma = lemma;
	}

	public String getPos() {
		return pos;
	}

	public void setPos(String pos) {
		this.pos = pos;
	}

	public String getChunk() {
		return chunk;
	}

	public void setChunk(String chunk) {
		this.chunk = chunk;
	}

	public String getNamedEntity() {
		return namedEntity;
	}

	public void setNamedEntity(String namedEntity) {
		this.namedEntity = namedEntity;
	}

	public String getWnSupersense() {
		return wnSupersense;
	}

	public void setWnSupersense(String wnSupersense) {
		this.wnSupersense = wnSupersense;
	}

	public String getDiscourseConn() {
		return discourseConn;
	}

	public void setDiscourseConn(String discourseConn) {
		this.discourseConn = discourseConn;
	}

	public Map<String, String> getDependencyRel() {
		return dependencyRel;
	}

	public void setDependencyRel(Map<String, String> dependencyRel) {
		this.dependencyRel = dependencyRel;
	}

	public String getEventID() {
		return eventID;
	}

	public void setEventID(String eventID) {
		this.eventID = eventID;
	}

	public String getTimexID() {
		return timexID;
	}

	public void setTimexID(String timexID) {
		this.timexID = timexID;
	}

	public String getcSignalID() {
		return cSignalID;
	}

	public void setcSignalID(String cSignalID) {
		this.cSignalID = cSignalID;
	}

	public boolean isMainVerb() {
		return mainVerb;
	}

	public void setMainVerb(boolean mainVerb) {
		this.mainVerb = mainVerb;
	}

	public String getMainPos() {
		return mainPos;
	}

	public void setMainPos(String mainPos) {
		this.mainPos = mainPos;
	}

	public String gettSignalID() {
		return tSignalID;
	}

	public void settSignalID(String tSignalID) {
		this.tSignalID = tSignalID;
	}

	public Integer getIndex() {
		return index;
	}

	public void setIndex(Integer index) {
		this.index = index;
	}

	public String getTense() {
		return tense;
	}

	public void setTense(String tense) {
		this.tense = tense;
	}

	public String getAspect() {
		return aspect;
	}

	public void setAspect(String aspect) {
		this.aspect = aspect;
	}

	public String getPolarity() {
		return polarity;
	}

	public void setPolarity(String polarity) {
		this.polarity = polarity;
	}

	public String getIncomingDep() {
		return incomingDep;
	}

	public void setIncomingDep(String incomingDep) {
		this.incomingDep = incomingDep;
	}
}