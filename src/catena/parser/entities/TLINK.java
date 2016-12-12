package catena.parser.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TLINK {
	
	private List<String> tt;
	private List<String> ed;
	private List<String> et;
	private List<String> ee;
	
	private Map<String, String> ttLinks;
	private Map<String, String> edLinks;
	private Map<String, String> etLinks;
	private Map<String, String> eeLinks;
	
	public TLINK() {
		setTT(new ArrayList<String>());
		setED(new ArrayList<String>());
		setET(new ArrayList<String>());
		setEE(new ArrayList<String>());
		
		setTTLinks(new HashMap<String, String>());
		setEDLinks(new HashMap<String, String>());
		setETLinks(new HashMap<String, String>());
		setEELinks(new HashMap<String, String>());
	}

	public List<String> getTT() {
		return tt;
	}

	public void setTT(List<String> tt) {
		this.tt = tt;
	}

	public List<String> getED() {
		return ed;
	}

	public void setED(List<String> ed) {
		this.ed = ed;
	}

	public List<String> getET() {
		return et;
	}

	public void setET(List<String> et) {
		this.et = et;
	}

	public List<String> getEE() {
		return ee;
	}

	public void setEE(List<String> ee) {
		this.ee = ee;
	}

	public Map<String, String> getTTLinks() {
		Map<String, String> ttPairs = new HashMap<String, String>();
		for (String link : this.getTT()) {
			String[] cols = link.split("\t");
			ttPairs.put(cols[0]+","+cols[1]+","+cols[2], cols[4]);
		}
		return ttPairs;
	}

	public void setTTLinks(Map<String, String> ttLinks) {
		this.ttLinks = ttLinks;
	}

	public Map<String, String> getEDLinks() {
		Map<String, String> edPairs = new HashMap<String, String>();
		for (String link : this.getED()) {
			String[] cols = link.split("\t");
			edPairs.put(cols[0]+","+cols[1]+","+cols[2], cols[4]);
		}
		return edPairs;
	}

	public void setEDLinks(Map<String, String> edLinks) {
		this.edLinks = edLinks;
	}

	public Map<String, String> getETLinks() {
		Map<String, String> etPairs = new HashMap<String, String>();
		for (String link : this.getET()) {
			String[] cols = link.split("\t");
			etPairs.put(cols[0]+","+cols[1]+","+cols[2], cols[4]);
		}
		return etPairs;
	}

	public void setETLinks(Map<String, String> etLinks) {
		this.etLinks = etLinks;
	}

	public Map<String, String> getEELinks() {
		Map<String, String> eePairs = new HashMap<String, String>();
		for (String link : this.getEE()) {
			String[] cols = link.split("\t");
			eePairs.put(cols[0]+","+cols[1]+","+cols[2], cols[4]);
		}
		return eePairs;
	}

	public void setEELinks(Map<String, String> eeLinks) {
		this.eeLinks = eeLinks;
	}
	
	public String TTLinksToString() {
		StringBuilder str = new StringBuilder();
		for (String key : this.getTTLinks().keySet()) {
			str.append(key.replace(",", "\t") 
					+ "\t" + this.getTTLinks().get(key) + "\n");
		}
		
		return str.toString();
	}
	
	public String EDLinksToString() {
		StringBuilder str = new StringBuilder();
		for (String key : this.getEDLinks().keySet()) {
			str.append(key.replace(",", "\t") 
					+ "\t" + this.getEDLinks().get(key) + "\n");
		}
		
		return str.toString();
	}
	
	public String ETLinksToString() {
		StringBuilder str = new StringBuilder();
		for (String key : this.getETLinks().keySet()) {
			str.append(key.replace(",", "\t") 
					+ "\t" + this.getETLinks().get(key) + "\n");
		}
		
		return str.toString();
	}
	
	public String EELinksToString() {
		StringBuilder str = new StringBuilder();
		for (String key : this.getEELinks().keySet()) {
			str.append(key.replace(",", "\t") 
					+ "\t" + this.getEELinks().get(key) + "\n");
		}
		
		return str.toString();
	}
}
