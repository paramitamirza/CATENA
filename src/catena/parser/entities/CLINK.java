package catena.parser.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CLINK {
	
	private List<String> ee;
	private Map<String, String> eeLinks;
	
	public CLINK() {
		setEE(new ArrayList<String>());
		setEELinks(new HashMap<String, String>());
	}

	public List<String> getEE() {
		return ee;
	}

	public void setEE(List<String> ee) {
		this.ee = ee;
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
	
	public String EELinksToString() {
		StringBuilder str = new StringBuilder();
		for (String key : this.getEELinks().keySet()) {
			str.append(key.replace(",", "\t") 
					+ "\t" + this.getEELinks().get(key) + "\n");
		}
		
		return str.toString();
	}
}
