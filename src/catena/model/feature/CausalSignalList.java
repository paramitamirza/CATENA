package catena.model.feature;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import catena.parser.entities.EntityEnum.Language;

public class CausalSignalList extends SignalList{
	
	private Map<String, String> list;
	private Map<String, String> patternlist;
	private Map<String, String> verbList;
	
	public CausalSignalList(Language lang) throws IOException {
		super(lang);
		list = new HashMap<String, String>();
		patternlist = new HashMap<String, String>();
		verbList = new HashMap<String, String>();
		readSignalFile();
	}
	
	public void readSignalFile() throws IOException {
		BufferedReader reader = null, verbReader = null;
		if (language.equals(Language.EN)) {
			reader = new BufferedReader(new FileReader("./resource/causal_signal.list"));
			verbReader = new BufferedReader(new FileReader("./resource/causal_verb.list"));
		} else if (language.equals(Language.IT)) {
			
		}
		if (reader != null) {
			String line;
			while ((line = reader.readLine()) != null) { 
				String[] cols = line.split("\\|\\|\\|");
				patternlist.put(cols[0].trim(), cols[1].trim());
				list.put(cols[0].trim(), cols[2].trim());
			}
			//for (String key : list.keySet()) System.out.println(key + "\t" + list.get(key));
		}
		if (verbReader != null) {
			String line;
			while ((line = verbReader.readLine()) != null) { 
				String[] cols = line.split("\\|\\|\\|");
				verbList.put(cols[0].trim(), cols[1].trim());
			}
			//for (String key : list.keySet()) System.out.println(key + "\t" + list.get(key));
		}
	}

	public Map<String, String> getList() {
		return list;
	}

	public void setList(Map<String, String> list) {
		this.list = list;
	}
	
	public Map<String, String> getPatternList() {
		return patternlist;
	}

	public void setPatternList(Map<String, String> plist) {
		this.patternlist = plist;
	}

	public Map<String, String> getVerbList() {
		return verbList;
	}

	public void setVerbList(Map<String, String> verbList) {
		this.verbList = verbList;
	}

}
