package catena.parser.entities;

import java.util.ArrayList;

import catena.model.feature.FeatureEnum.FeatureName;

public class Event extends Entity{
	
	private String eventClass;
	private String tense;
	private String aspect;
	private String polarity;
	private ArrayList<String> corefList;

	public Event(String id, String start, String end) {
		super(id, start, end);
		// TODO Auto-generated constructor stub
	}
	
	public void setAttributes(String eventClass, String tense, String aspect, String pol) {
		this.eventClass = eventClass;
		this.tense = tense;
		this.aspect = aspect;
		this.polarity = pol;
		this.corefList = new ArrayList<String>();
	}

	public String getEventClass() {
		return eventClass;
	}

	public void setEventClass(String eventClass) {
		this.eventClass = eventClass;
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

	public ArrayList<String> getCorefList() {
		return corefList;
	}

	public void setCorefList(ArrayList<String> corefList) {
		this.corefList = corefList;
	}

	public String getAttribute(FeatureName feature) {
		switch (feature) {
			case eventClass: return this.getEventClass(); 
			case tense: return this.getTense();
			case aspect: return this.getAspect();
			case polarity: return this.getPolarity();
			default: return this.getEventClass() + "\t" + this.getTense() + "\t" +
			this.getAspect() + "\t" + this.getPolarity();
		}
	}
	
}
