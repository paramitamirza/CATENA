package catena.evaluator;

public class QATempEval {
	
	private String goldPath;
	private String systemPath;
	
	public QATempEval(String gold, String system) {
		this.setGoldPath(gold);
		this.setSystemPath(system);
	}
	
	public void evaluate() {
		
	}

	public String getGoldPath() {
		return goldPath;
	}

	public void setGoldPath(String goldPath) {
		this.goldPath = goldPath;
	}

	public String getSystemPath() {
		return systemPath;
	}

	public void setSystemPath(String systemPath) {
		this.systemPath = systemPath;
	}

}
