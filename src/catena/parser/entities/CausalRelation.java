package catena.parser.entities;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class CausalRelation extends Relation{
	
	private String signal;
	
	public CausalRelation() {
		
	}

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
	
	public Node toTimeMLNode(Document doc, int idx) {
		Element clink = doc.createElement("CLINK");
		
		Attr source = doc.createAttribute("eventInstanceID");
		source.setValue(sourceID);
			
		Attr target = doc.createAttribute("relatedToEventInstance");
		target.setValue(targetID);
		
		Attr id = doc.createAttribute("lid");
		id.setValue(String.valueOf(idx));
		
		clink.setAttributeNode(id);
		clink.setAttributeNode(source);
		clink.setAttributeNode(target);
		
		return clink;
	}
}
