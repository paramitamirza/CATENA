package catena.parser.entities;

import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class TemporalRelation extends Relation{
	
	private String relType;
	private String signal;
	private boolean deduced;
	
	public TemporalRelation() {
		
	}

	public TemporalRelation(String source, String target) {
		super(source, target);
		// TODO Auto-generated constructor stub
	}

	public String getSignal() {
		return signal;
	}

	public void setSignal(String signal) {
		this.signal = signal;
	}

	public String getRelType() {
		return relType;
	}

	public void setRelType(String relType) {
		this.relType = relType;
	}
	
	public static String getInverseRelation(String relType) {
        String[] relations = {"BEFORE", "AFTER", "INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", 
        	"IBEFORE", "IAFTER", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
        List<String> rels = Arrays.asList(relations);
        
        if (rels.contains(relType)) {
        	Integer relIdx = rels.indexOf(relType);
        	if (rels.indexOf(relType) % 2 == 0) {
        		return rels.get(relIdx + 1);
        	} else {
        		return rels.get(relIdx - 1);
        	}
        }
        else {
            return relType;
        }
	}
	
	public Node toTimeMLNode(Document doc, int idx) {
		Element tlink = doc.createElement("TLINK");
		Attr source = null, target = null, rel = null, id = null;
		if (sourceType.equals("Event")) {
			source = doc.createAttribute("eventInstanceID");
		    source.setValue(sourceID);
		} else if (sourceType.equals("Timex")) {
			source = doc.createAttribute("timeID");
		    source.setValue(sourceID);
		}
		if (targetType.equals("Event")) {
			target = doc.createAttribute("relatedToEventInstance");
			target.setValue(targetID);
		} else if (targetType.equals("Timex")) {
			target = doc.createAttribute("relatedToTime");
			target.setValue(targetID);
		}
		rel = doc.createAttribute("relType");
		rel.setValue(relType);
		id = doc.createAttribute("lid");
		id.setValue(String.valueOf(idx));
		
		tlink.setAttributeNode(id);
		tlink.setAttributeNode(source);
		tlink.setAttributeNode(target);
		tlink.setAttributeNode(rel);
		
		return tlink;
	}

	public boolean isDeduced() {
		return deduced;
	}

	public void setDeduced(boolean deduced) {
		this.deduced = deduced;
	}
}
