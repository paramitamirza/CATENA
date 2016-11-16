package catena.model.feature;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import catena.parser.entities.EntityEnum.Language;

public class TemporalSignalList extends SignalList{
	
	private Map<String, String> eventList;
	private Map<String, String> timexList;
	
	public TemporalSignalList(Language lang) throws IOException {
		super(lang);
		eventList = new HashMap<String, String>();
		timexList = new HashMap<String, String>();
		readSignalFile();
	}
	
	public void readSignalFile() throws IOException {
		BufferedReader eventreader = null, timexreader = null;
		if (language.equals(Language.EN)) {
			eventreader = new BufferedReader(new FileReader("./resource/temporal_signal_event.list"));
			timexreader = new BufferedReader(new FileReader("./resource/temporal_signal_timex.list"));
		} else if (language.equals(Language.IT)) {
			
		}
		if (eventreader != null) {
			String line;
			while ((line = eventreader.readLine()) != null) { 
				String[] cols = line.split("\\|\\|\\|");
				eventList.put(cols[0].trim(), cols[1].trim());
			}
			//for (String key : eventList.keySet()) System.out.println(key + "\t" + eventList.get(key));
		}
		if (timexreader != null) {
			String line;
			while ((line = timexreader.readLine()) != null) { 
				String[] cols = line.split("\\|\\|\\|");
				timexList.put(cols[0].trim(), cols[1].trim());
			}
			//for (String key : timexList.keySet()) System.out.println(key + "\t" + timexList.get(key));
		}
	}

	public Map<String, String> getEventList() {
		return eventList;
	}

	public void setEventList(Map<String, String> eventList) {
		this.eventList = eventList;
	}

	public Map<String, String> getTimexList() {
		return timexList;
	}

	public void setTimexList(Map<String, String> timexList) {
		this.timexList = timexList;
	}
}
