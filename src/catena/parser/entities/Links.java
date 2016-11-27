package catena.parser.entities;

import java.util.ArrayList;
import java.util.List;

public class Links {
	
	private List<String> tt;
	private List<String> ed;
	private List<String> et;
	private List<String> ee;
	
	public Links() {
		setTT(new ArrayList<String>());
		setED(new ArrayList<String>());
		setET(new ArrayList<String>());
		setEE(new ArrayList<String>());
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
}
