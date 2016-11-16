package catena.parser.entities;

import java.util.ArrayList;
import java.util.Map;

public class Entity {
	
	private String ID;
	private Integer index;
	private String startTokID;
	private String endTokID;
	private String sentID;
	
	public Entity(String id, String start, String end) {
		this.ID = id;
		this.startTokID = start;
		this.endTokID = end;
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

	public String getSentID() {
		return sentID;
	}

	public void setSentID(String sentID) {
		this.sentID = sentID;
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
}
