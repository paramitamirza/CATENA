package catena.parser;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import catena.parser.entities.CausalRelation;
import catena.parser.entities.Doc;
import catena.parser.entities.EntityEnum;
import catena.parser.entities.TemporalRelation;
import catena.parser.entities.TimeMLDoc;

public class TimeMLParser {
	
	private EntityEnum.Language language;
	
	public TimeMLParser(EntityEnum.Language lang) {
		this.setLanguage(lang);
	}

	public EntityEnum.Language getLanguage() {
		return language;
	}

	public void setLanguage(EntityEnum.Language language) {
		this.language = language;
	}
	
	public Document getTimeML(String filepath) throws ParserConfigurationException, SAXException, IOException {
		TimeMLDoc tmlDoc = new TimeMLDoc(filepath);
		return tmlDoc.getDoc();
	}
	
	public Doc parseDocument(String filepath) throws ParserConfigurationException, SAXException, IOException, TransformerException {
		TimeMLDoc tmlDoc = new TimeMLDoc(filepath);
		
		Doc d = new Doc();
		setInstances(tmlDoc, d);
		setTlinks(tmlDoc, d);
		setClinks(tmlDoc, d);
		d.setFilename(new File(filepath).getName());
		
		return d;
	}
	
	public List<String> getEvents(TimeMLDoc tmlDoc) {
		List<String> arrEvents = new ArrayList<String>();
		NodeList events = tmlDoc.getDoc().getElementsByTagName("EVENT");
		for (int index = 0; index < events.getLength(); index++) {
			Node event = events.item(index);
			NamedNodeMap attrs = event.getAttributes();			
			for (int i = 0; i < attrs.getLength(); i++) {
				if (attrs.item(i).getNodeName().equals("eid")) {
					arrEvents.add(attrs.item(i).getNodeValue());
				}
			}
		}
		return arrEvents;
	}
	
	public void setTlinks(TimeMLDoc tmlDoc, Doc d) {
		NodeList tlinks = tmlDoc.getDoc().getElementsByTagName("TLINK");
		ArrayList<TemporalRelation> tlinkArr = d.getTlinks();
		String source = null, target = null, relType = null;
		String sourceType = null, targetType = null;
		
		for (int index = tlinks.getLength() - 1; index >= 0; index--) {
			boolean deduced = false;
			Node tlink = tlinks.item(index);
			NamedNodeMap attrs = tlink.getAttributes();			
			for (int i = 0; i < attrs.getLength(); i++) {
				switch(attrs.item(i).getNodeName()) {
					case "eventInstanceID": 
						source = attrs.item(i).getNodeValue();
						sourceType = "Event";
						break;
					case "timeID":
						source = attrs.item(i).getNodeValue().replace("t", "tmx");
						sourceType = "Timex";
						break;
					case "relatedToEventInstance":
						target = attrs.item(i).getNodeValue();
						targetType = "Event";
						break;
					case "relatedToTime":
						target = attrs.item(i).getNodeValue().replace("t", "tmx");
						targetType = "Timex";
						break;
					case "relType":
						relType = attrs.item(i).getNodeValue();
						break;
					case "deduced":
						if (attrs.item(i).getNodeValue().equals("true")) {
							deduced = true;
						}
						break;
				}
				if (d.getInstances().containsKey(source)) {
					source = d.getInstances().get(source);
				} 
				if (d.getInstances().containsKey(target)) {
					target = d.getInstances().get(target);
				}
			}
			TemporalRelation tl = new TemporalRelation(source, target);
			tl.setSourceType(sourceType); tl.setTargetType(targetType);
			tl.setRelType(relType);
			tl.setDeduced(deduced);
			tlinkArr.add(tl);
		}
	}
	
	public void setClinks(TimeMLDoc tmlDoc, Doc d) {
		NodeList clinks = tmlDoc.getDoc().getElementsByTagName("CLINK");
		ArrayList<CausalRelation> clinkArr = d.getClinks();
		String source = null, target = null;
		String sourceType = null, targetType = null;
		
		for (int index = clinks.getLength() - 1; index >= 0; index--) {
			Node tlink = clinks.item(index);
			NamedNodeMap attrs = tlink.getAttributes();			
			for (int i = 0; i < attrs.getLength(); i++) {
				switch(attrs.item(i).getNodeName()) {
					case "eventInstanceID": 
						source = attrs.item(i).getNodeValue();
						sourceType = "Event";
						break;
					case "relatedToEventInstance":
						target = attrs.item(i).getNodeValue();
						targetType = "Event";
						break;
				}
				if (d.getInstances().containsKey(source)) {
					source = d.getInstances().get(source);
				} 
				if (d.getInstances().containsKey(target)) {
					target = d.getInstances().get(target);
				}
			}
			CausalRelation cl = new CausalRelation(source, target);
			cl.setSourceType(sourceType); cl.setTargetType(targetType);
			clinkArr.add(cl);
		}
	}
	
	public void setInstances(TimeMLDoc tmlDoc, Doc d) {
		NodeList instances = tmlDoc.getDoc().getElementsByTagName("MAKEINSTANCE");
		Map<String, String> instMap = d.getInstances();
		Map<String, String> instInvMap = d.getInstancesInv();
		String eid = null, eiid = null;
		for (int index = instances.getLength() - 1; index >= 0; index--) {
			Node instance = instances.item(index);
			NamedNodeMap attrs = instance.getAttributes();			
			for (int i = 0; i < attrs.getLength(); i++) {
				if (attrs.item(i).getNodeName().equals("eventID")) {
					eid = attrs.item(i).getNodeValue();
				} else if (attrs.item(i).getNodeName().equals("eiid")) {
					eiid = attrs.item(i).getNodeValue();
				} 
			}
			instMap.put(eiid, eid);
			instInvMap.put(eid, eiid);
		}		
	}
	
	

}
