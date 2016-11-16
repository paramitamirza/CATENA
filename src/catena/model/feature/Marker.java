package catena.model.feature;

public class Marker {
	
	private String txt;
	private String cluster;
	private String position;
	private String depRelE1;
	private String depRelE2;
	
	public Marker() {
		
	}
	
	public Marker(String txt, String cluster, String position, String depRelE1, String depRelE2) {
		this.setText(txt);
		this.setCluster(cluster);
		this.setPosition(position);
		this.setDepRelE1(depRelE1);
		this.setDepRelE2(depRelE2);
	}
	
	public String getText() {
		return txt;
	}
	
	public void setText(String text) {
		this.txt = text;
	}
	
	public String getPosition() {
		return position;
	}
	
	public void setPosition(String position) {
		this.position = position;
	}

	public String getCluster() {
		return cluster;
	}

	public void setCluster(String cluster) {
		this.cluster = cluster;
	}

	public String getDepRelE1() {
		return depRelE1;
	}

	public void setDepRelE1(String depRelE1) {
		this.depRelE1 = depRelE1;
	}

	public String getDepRelE2() {
		return depRelE2;
	}

	public void setDepRelE2(String depRelE2) {
		this.depRelE2 = depRelE2;
	}

}
