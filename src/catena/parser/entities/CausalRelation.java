package catena.parser.entities;

public class CausalRelation extends Relation{
	
	private String signal;

	public CausalRelation(String source, String target) {
		super(source, target);
		// TODO Auto-generated constructor stub
	}

	public String getSignal() {
		return signal;
	}

	public void setSignal(String signal) {
		this.signal = signal;
	}
	
	public static String getInverseRelation(String relType) {
        if (relType.equals("CLINK")) return "CLINK-R";
        else if (relType.equals("CLINK-R")) return "CLINK";
        else return "NONE";
	}
}
