package catena.model.feature;

import catena.parser.entities.EntityEnum;

public class SignalList {
	
	protected EntityEnum.Language language;
	
	public SignalList(EntityEnum.Language lang) {
		this.setLanguage(lang);
	}

	public EntityEnum.Language getLanguage() {
		return language;
	}

	public void setLanguage(EntityEnum.Language language) {
		this.language = language;
	}	
}
