package catena.parser.entities;

import java.util.ArrayList;
import java.util.List;

public class CLINK {
	
	private List<String> ee;
	
	public CLINK() {
		setEE(new ArrayList<String>());
	}

	public List<String> getEE() {
		return ee;
	}

	public void setEE(List<String> ee) {
		this.ee = ee;
	}
}
