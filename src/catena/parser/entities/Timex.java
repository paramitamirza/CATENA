package catena.parser.entities;

import catena.model.feature.FeatureEnum.FeatureName;

public class Timex extends Entity{
	
	private String type;
	private String value;
	private Boolean dct;
	private Boolean emptyTag;

	public Timex(String id, String start, String end) {
		super(id, start, end);
		// TODO Auto-generated constructor stub
	}
	
	public void setAttributes(String type, String value) {
		this.type = type;
		this.value = value;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Boolean isDct() {
		return dct;
	}

	public void setDct(Boolean dct) {
		this.dct = dct;
	}

	public Boolean isEmptyTag() {
		return emptyTag;
	}

	public void setEmptyTag(Boolean emptyTag) {
		this.emptyTag = emptyTag;
	}
	
	public String getValueTemplate() {
		String template = "";
		template = this.value.replaceAll("\\d", "N");
		return template;
	}
	
	public String getAttribute(FeatureName feature) {
		switch (feature) {
			case timexType: return this.getType(); 
			case timexValue: return this.getValue();
			case dct: return this.isDct() ? "TRUE" : "FALSE";
			case timexValueTemplate: return getValueTemplate();
			default: return this.getType() + "\t" + this.getValue() + "\t" +
			(this.isDct() ? "TRUE" : "FALSE");
		}
	}
	
}
