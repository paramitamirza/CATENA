package catena.parser.entities;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

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

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class TimeMLDoc {
	
	private Document doc;
	private Node root;
	
	public TimeMLDoc(String filepath) throws ParserConfigurationException, SAXException, IOException {
		File fXmlFile = new File(filepath);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setIgnoringElementContentWhitespace(true);
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		doc = dBuilder.parse(fXmlFile);
				
		//optional, but recommended
		//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
		doc.getDocumentElement().normalize();
		
		root = doc.getDocumentElement();
	}
	
	public void trimWhitespace(Node node)
	{
	    NodeList children = node.getChildNodes();
	    for(int i = 0; i < children.getLength(); ++i) {
	        Node child = children.item(i);
	        if(child.getNodeType() == Node.TEXT_NODE
	        		&& !child.getNodeName().equals("TEXT")) {
	            child.setTextContent(child.getTextContent().trim());
	        }
	        trimWhitespace(child);
	    }
	}
	
	public void removeLinks() {
		NodeList tlinks = doc.getElementsByTagName("TLINK");
		for (int index = tlinks.getLength() - 1; index >= 0; index--) {
			root.removeChild(tlinks.item(index));
		}
		NodeList slinks = doc.getElementsByTagName("SLINK");
		for (int index = slinks.getLength() - 1; index >= 0; index--) {
		    root.removeChild(slinks.item(index));
		}
		NodeList alinks = doc.getElementsByTagName("ALINK");
		for (int index = alinks.getLength() - 1; index >= 0; index--) {
		    root.removeChild(alinks.item(index));
		}
		//this.trimWhitespace(root);
	}
	
	public void removeInstances() {
		NodeList tlinks = doc.getElementsByTagName("MAKEINSTANCE");
		for (int index = tlinks.getLength() - 1; index >= 0; index--) {
			root.removeChild(tlinks.item(index));
		}
		//this.trimWhitespace(root);
	}
	
	public void removeText() {
		NodeList tlinks = doc.getElementsByTagName("TEXT");
		for (int index = tlinks.getLength() - 1; index >= 0; index--) {
			root.removeChild(tlinks.item(index));
		}
		//this.trimWhitespace(root);
	}
	
	public void addLink(Node link) {
		root.appendChild(link);
	}
	
	public void addInstance(Node instance) {
		root.appendChild(instance);
	}
	
	public void addText(Node text) {
		root.appendChild(text);
	}
	
	public String nodeToString(Node node) throws TransformerException {
		// Set up the output transformer
		TransformerFactory transfac = TransformerFactory.newInstance();
		Transformer trans = transfac.newTransformer();
		//trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		trans.setOutputProperty(OutputKeys.INDENT, "yes");
		
		// Print the DOM node
		StringWriter sw = new StringWriter();
		StreamResult result = new StreamResult(sw);
		DOMSource source = new DOMSource(node);
		trans.transform(source, result);
		String xmlString = sw.toString();
		return xmlString;
	}
	
	public String toString() {
		// Set up the output transformer
		TransformerFactory transfac = TransformerFactory.newInstance();
		Transformer trans = null;
		String xmlString = null;
		try {
			trans = transfac.newTransformer();
			//trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			trans.setOutputProperty(OutputKeys.INDENT, "yes");
			trans.setOutputProperty(OutputKeys.METHOD, "xml");
		    trans.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			
			// Print the DOM node
			StringWriter sw = new StringWriter();
			StreamResult result = new StreamResult(sw);
			doc.setXmlStandalone(true);
			DOMSource source = new DOMSource(doc);
			trans.transform(source, result);
			xmlString = sw.toString();
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return xmlString;
	}
	
	public List<String> splitText() throws Exception {
		List<String> wordList = new ArrayList<String>();
		NodeList text = doc.getElementsByTagName("TEXT");
		
		String textStr = text.item(0).getTextContent();
		textStr = textStr.replaceAll("\\<.*?>","");
		textStr = textStr.replaceAll("\n", " \n");
		for (String s : textStr.split(" ")) {
			wordList.add(s);
		}
		
		return wordList;
	}

	public Document getDoc() {
		return doc;
	}

	public void setDoc(Document doc) {
		this.doc = doc;
	}

	public Node getRoot() {
		return root;
	}

	public void setRoot(Node root) {
		this.root = root;
	}

}
