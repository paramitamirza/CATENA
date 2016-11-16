package catena.parser.entities;

import java.util.ArrayList;
import java.util.Map;

public class Sentence {
	
	private String ID;
	private Integer index;
	private String startTokID;
	private String endTokID;
	private ArrayList<String> tokIDArr;
	private ArrayList<String> entityArr;
	
	public Sentence(String id, String start, String end) {
		this.setID(id);
		this.setStartTokID(start);
		this.setEndTokID(end);
		this.setEntityArr(new ArrayList<String>());
	}

	public String getID() {
		return ID;
	}

	public void setID(String iD) {
		ID = iD;
	}

	public String getStartTokID() {
		return startTokID;
	}

	public void setStartTokID(String startTokID) {
		this.startTokID = startTokID;
	}

	public String getEndTokID() {
		return endTokID;
	}

	public void setEndTokID(String endTokID) {
		this.endTokID = endTokID;
	}

	public Integer getIndex() {
		return index;
	}

	public void setIndex(Integer index) {
		this.index = index;
	}

	public ArrayList<String> getTokIDArr() {
		return tokIDArr;
	}

	public void setTokIDArr(ArrayList<String> tokIDArr) {
		this.tokIDArr = tokIDArr;
	}

	public ArrayList<String> getEntityArr() {
		return entityArr;
	}

	public void setEntityArr(ArrayList<String> entityArr) {
		this.entityArr = entityArr;
	}
	
	public String toString(Doc docTxp) {
		String str = "";
		Map<String, Token> tokens = docTxp.getTokens();
		ArrayList<String> tokenArr = docTxp.getTokenArr();
		int iter = tokenArr.indexOf(startTokID);
		while (!tokenArr.get(iter).equals(endTokID)) {
			str += tokens.get(tokenArr.get(iter)).getText() + " ";
			iter ++;
		}
		str += tokens.get(tokenArr.get(iter)).getText();
		return str;		
	}
	
	public String toLowerString(Doc docTxp) {
		String str = "";
		Map<String, Token> tokens = docTxp.getTokens();
		ArrayList<String> tokenArr = docTxp.getTokenArr();
		int iter = tokenArr.indexOf(startTokID);
		while (!tokenArr.get(iter).equals(endTokID)) {
			str += tokens.get(tokenArr.get(iter)).getText().toLowerCase() + " ";
			iter ++;
		}
		str += tokens.get(tokenArr.get(iter)).getText().toLowerCase();
		return str;		
	}
	
	public String toLemmaString(Doc docTxp) {
		String str = "";
		Map<String, Token> tokens = docTxp.getTokens();
		ArrayList<String> tokenArr = docTxp.getTokenArr();
		int iter = tokenArr.indexOf(startTokID);
		while (!tokenArr.get(iter).equals(endTokID)) {
			str += tokens.get(tokenArr.get(iter)).getLemma() + " ";
			iter ++;
		}
		str += tokens.get(tokenArr.get(iter)).getLemma();
		return str;		
	}
	
	public String toAnnotatedString(Doc docTxp, ArrayList<Entity> entities) {
		String str = "";
		Map<String, Token> tokens = docTxp.getTokens();
		ArrayList<String> tokenArr = docTxp.getTokenArr();
		docTxp.getEntities();
		int iter = tokenArr.indexOf(startTokID);
		
		ArrayList<String> starts = new ArrayList<String>();
		ArrayList<String> ends = new ArrayList<String>();
		for (Entity e : entities) {
			starts.add(e.getStartTokID());
			ends.add(e.getEndTokID());
		}
		
		while (!tokenArr.get(iter).equals(endTokID)) {
			if (starts.contains(tokenArr.get(iter))) {
				str += "[";
			}
			str += tokens.get(tokenArr.get(iter)).getText();
			if (ends.contains(tokenArr.get(iter))) {
				str += "]";
			}
			str += " ";
			iter ++;
		}
		str += tokens.get(tokenArr.get(iter)).getText();
		return str;		
	}

}
