package catena.parser.entities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

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
	
	public TimeMLDoc toTimeMLDoc(List<String> ttResult, List<String> edResult, 
			List<String> etResult, List<String> eeResult) throws ParserConfigurationException, SAXException, IOException {
		
		TimeMLDoc tml = toTimeMLDoc(false, false);
		
		int linkId = 1;
		TemporalRelation tlink = new TemporalRelation();
		
		for (String ttStr : ttResult) {
			if (!ttStr.isEmpty()) {
				String[] cols = ttStr.split("\t");
				if (!cols[3].equals("NONE")) {
					tlink.setSourceID(cols[0].replace("tmx", "t"));
					tlink.setTargetID(cols[1].replace("tmx", "t"));
					tlink.setRelType(cols[3]);
					tlink.setSourceType("Timex");
					tlink.setTargetType("Timex");
					tml.addLink(tlink.toTimeMLNode(tml.getDoc(), linkId));
					linkId += 1;
				}
			}
		}
		for (String etStr : edResult) {
			if (!etStr.isEmpty()) {
				String[] cols = etStr.split("\t");
				if (!cols[3].equals("NONE")) {
					tlink.setSourceID(this.getInstancesInv().get(cols[0]));
					tlink.setTargetID(cols[1].replace("tmx", "t"));
					tlink.setRelType(cols[3]);
					tlink.setSourceType("Event");
					tlink.setTargetType("Timex");
					tml.addLink(tlink.toTimeMLNode(tml.getDoc(), linkId));
					linkId += 1;
				}
			}
		}
		for (String etStr : etResult) {
			if (!etStr.isEmpty()) {
				String[] cols = etStr.split("\t");
				if (!cols[3].equals("NONE")) {
					tlink.setSourceID(this.getInstancesInv().get(cols[0]));
					tlink.setTargetID(cols[1].replace("tmx", "t"));
					tlink.setRelType(cols[3]);
					tlink.setSourceType("Event");
					tlink.setTargetType("Timex");
					tml.addLink(tlink.toTimeMLNode(tml.getDoc(), linkId));
					linkId += 1;
				}
			}
		}
		for (String eeStr : eeResult) {
			if (!eeStr.isEmpty()) {
				String[] cols = eeStr.split("\t");
				if (!cols[3].equals("NONE")) {
					tlink.setSourceID(this.getInstancesInv().get(cols[0]));
					tlink.setTargetID(this.getInstancesInv().get(cols[1]));
					tlink.setRelType(cols[3]);
					tlink.setSourceType("Event");
					tlink.setTargetType("Event");
					tml.addLink(tlink.toTimeMLNode(tml.getDoc(), linkId));
					linkId += 1;
				}
			}
		}
		
		return tml;
	}
	
	public TimeMLDoc toTimeMLDoc(List<String> eeCausalResult) throws ParserConfigurationException, SAXException, IOException {
		
		TimeMLDoc tml = toTimeMLDoc(false, false);
		
		int linkId = 1000;
		CausalRelation clink = new CausalRelation();
		
		for (String eeStr : eeCausalResult) {
			if (!eeStr.isEmpty()) {
				String[] cols = eeStr.split("\t");
				if (!cols[3].equals("NONE")) {
					if (cols[3].equals("CLINK")) {
						clink.setSourceID(this.getInstancesInv().get(cols[0]));
						clink.setTargetID(this.getInstancesInv().get(cols[1]));
						clink.setSourceType("Event");
						clink.setTargetType("Event");
						tml.addLink(clink.toTimeMLNode(tml.getDoc(), linkId));
						linkId += 1;
						
					} else if (cols[3].equals("CLINK-R")) {
						clink.setSourceID(this.getInstancesInv().get(cols[1]));
						clink.setTargetID(this.getInstancesInv().get(cols[0]));
						clink.setSourceType("Event");
						clink.setTargetType("Event");
						tml.addLink(clink.toTimeMLNode(tml.getDoc(), linkId));
						linkId += 1;
						
					}
				}
			}
		}
		
		return tml;
	}
	
	public TimeMLDoc toTimeMLDoc(boolean tlinks, boolean clinks) throws ParserConfigurationException, SAXException, IOException {
		String tmlStr = "";
		
		String text = "";
		boolean timexStarted = false;
		boolean eventStarted = false;
		boolean signaltStarted = false;
		boolean signalcStarted = false;		
		for (String sID : this.getSentenceArr()) {
			Sentence s = this.getSentences().get(sID);
			String sent = "";
			Token tok;
			int startTokIdx = this.getTokens().get(s.getStartTokID()).getIndex();
			int endTokIdx = this.getTokens().get(s.getEndTokID()).getIndex();
			for (int i=startTokIdx; i<endTokIdx+1; i++) {
				tok = this.getTokens().get(this.getTokenArr().get(i));
				if (tok.getTimexID() != null) {
					Timex tmx = (Timex)this.getEntities().get(tok.getTimexID());
					if (timexStarted) {
						sent += " " + tok.getText();
					} else {
						if (eventStarted) sent += "</EVENT>";
						else if (signaltStarted) sent += "</SIGNAL>";
						else if (signalcStarted) sent += "</C-SIGNAL>";
						sent += " " + "<TIMEX3 tid=\""+tmx.getID().replace("tmx", "t")+"\" type=\""+tmx.getType()+"\" value=\""+tmx.getValue()+"\">" + tok.getText();
						timexStarted = true;
						eventStarted = false;
						signaltStarted = false;
						signalcStarted = false;
					}
				} else if (tok.getEventID() != null) {
					Event ev = (Event)this.getEntities().get(tok.getEventID());
					if (eventStarted) {
						sent += " " + tok.getText();
					} else {
						if (timexStarted) sent += "</TIMEX3>";
						else if (signaltStarted) sent += "</SIGNAL>";
						else if (signalcStarted) sent += "</C-SIGNAL>";
						sent += " " + "<EVENT eid=\""+ev.getID()+"\" class=\""+ev.getEventClass()+"\">" + tok.getText();
						eventStarted = true;
						timexStarted = false;
						signaltStarted = false;
						signalcStarted = false;
					}
				} else if (tok.gettSignalID() != null) {
					TemporalSignal tsig = (TemporalSignal)this.getTemporalSignals().get(tok.gettSignalID());
					if (signaltStarted) {
						sent += " " + tok.getText();
					} else {
						if (eventStarted) sent += "</EVENT>";
						else if (timexStarted) sent += "</TIMEX3>";
						else if (signalcStarted) sent += "</C-SIGNAL>";
						sent += " " + "<SIGNAL sid=\""+tsig.getID()+"\">" + tok.getText();
						signaltStarted = true;
						eventStarted = false;
						timexStarted = false;
						signalcStarted = false;
					}
				} else if (tok.getcSignalID() != null) {
					CausalSignal csig = (CausalSignal)this.getCausalSignals().get(tok.getcSignalID());
					if (signalcStarted) {
						sent += " " + tok.getText();
					} else {
						if (eventStarted) sent += "</EVENT>";
						else if (timexStarted) sent += "</TIMEX3>";
						else if (signaltStarted) sent += "</SIGNAL>";
						sent += " " + "<C-SIGNAL cid=\""+csig.getID()+"\">" + tok.getText();
						signalcStarted = true;
						eventStarted = false;
						timexStarted = false;
						signaltStarted = false;
					}
				} else {
					if (timexStarted) {
						sent += "</TIMEX3>" + " " + tok.getText();
						timexStarted = false;
					} else if (eventStarted) {
						sent += "</EVENT>" + " " + tok.getText();
						eventStarted = false;
					} else if (signaltStarted) {
						sent += "</SIGNAL>" + " " + tok.getText();
						signaltStarted = false;
					} else if (signalcStarted) {
						sent += "</C-SIGNAL>" + " " + tok.getText();
						signalcStarted = false;
					} else {
						sent += " " + tok.getText();
					}
				}
			}
			if (timexStarted) sent += "</TIMEX3>";
			else if (eventStarted) sent += "</EVENT>";
			else if (signaltStarted) sent += "</SIGNAL>";
			else if (signalcStarted) sent += "</C-SIGNAL>";
			timexStarted = false;
			eventStarted = false;
			signaltStarted = false;
			signalcStarted = false;
			
			text += sent.substring(1);
			text += "\n";
		}
		
		text = text.replaceAll("& UR; ", "");
		text = text.replaceAll("&", "&amp;");
		
		text = text.replaceAll(" 's", "'s");
		text = text.replaceAll(" 're", "'re");
		text = text.replaceAll(" 'm", "'m");
		text = text.replaceAll(" 've", "'ve");
		text = text.replaceAll(" 'd", "'d");
		text = text.replaceAll(" n't", "n't");
		text = text.replaceAll(" \\.", "\\.");
		text = text.replaceAll(" ,", ",");
		text = text.replaceAll(" !", "!");
		text = text.replaceAll(" \\?", "\\?");
		text = text.replaceAll(" :", ":");
		text = text.replaceAll(" ;", ";");
		
		text = text.replaceAll("`` ", "\"");
        text = text.replaceAll(" ''", "\"");
        text = text.replaceAll("-LCB- ", "{");
        text = text.replaceAll(" -RCB-", "}");
        text = text.replaceAll("-LRB- ", "(");
        text = text.replaceAll(" -RRB-", ")");
        text = text.replaceAll("-LSB- ", "[");
        text = text.replaceAll(" -RSB-", "]");
        
        String instances = "";
        for (String eid : this.getEntities().keySet()) {
        	if (this.getEntities().get(eid).getID().startsWith("e")) {
        		Event ev = (Event)this.getEntities().get(eid);
        		instances += "<MAKEINSTANCE"
        				+ " eventID=\""+ev.getID()+"\""
        				+ " eiid=\""+ev.getID().replace("e", "ei")+"\""
        				+ " tense=\""+ev.getTense()+"\""
        				+ " aspect=\""+ev.getAspect()+"\""
        				+ " polarity=\""+ev.getPolarity()+"\" />";
        		instances += "\n";
        		this.getInstances().put(ev.getID().replace("e", "ei"), ev.getID());
        		this.getInstancesInv().put(ev.getID(), ev.getID().replace("e", "ei"));
        	}
        }
        
		tmlStr += "<?xml version=\"1.0\" ?>\n";
		tmlStr += "<TimeML xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://timeml.org/timeMLdocs/TimeML_1.2.1.xsd\">\n";
		tmlStr += "\n";
		tmlStr += "<DOCID>" + filename + "</DOCID>\n";
		tmlStr += "\n";
		tmlStr += "<DCT><TIMEX3 tid=\"" + dct.getID() 
				+ "\" type=\"" + dct.getType() 
				+ "\" value=\"" + dct.getValue() 
				+ "\" temporalFunction=\"false\" functionInDocument=\"CREATION_TIME\">"
				+ dct.getValue() + "</TIMEX3></DCT>\n";
		tmlStr += "\n";
		tmlStr += "<TEXT>\n";
		tmlStr += text;
		tmlStr += "</TEXT>\n";
		tmlStr += "\n";
		tmlStr += instances;
		tmlStr += "\n";
		tmlStr += "</TimeML>";
		
//		System.out.println(tmlStr);
		TimeMLDoc tml = new TimeMLDoc(tmlStr);
		
		int linkId = 1;
		if (tlinks) {
			for (TemporalRelation tlink : this.getTlinks()) {
				if (tlink.getSourceType().equals("Event")) tlink.setSourceID(this.getInstancesInv().get(tlink.getSourceID()));
				if (tlink.getTargetType().equals("Event")) tlink.setTargetID(this.getInstancesInv().get(tlink.getTargetID()));
				if (tlink.getSourceType().equals("Timex")) tlink.setSourceID(tlink.getSourceID().replace("tmx", "t"));
				if (tlink.getTargetType().equals("Timex")) tlink.setTargetID(tlink.getTargetID().replace("tmx", "t"));
				tml.addLink(tlink.toTimeMLNode(tml.getDoc(), linkId));
				linkId += 1;
			}
		}
		if (clinks) {
			for (CausalRelation clink : this.getClinks()) {
				if (clink.getSourceType().equals("Event")) clink.setSourceID(this.getInstancesInv().get(clink.getSourceID()));
				if (clink.getTargetType().equals("Event")) clink.setTargetID(this.getInstancesInv().get(clink.getTargetID()));
				tml.addLink(clink.toTimeMLNode(tml.getDoc(), linkId));
				linkId += 1;
			}
		}
		return tml;
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
