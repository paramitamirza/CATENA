package catena.parser.entities;

public class Relation {
	
	protected String sourceID;
	protected String targetID;
	protected String sourceType;
	protected String targetType;
	
	public Relation() {
		
	}
	
	public Relation(String source, String target) {
		this.setSourceID(source);
		this.setTargetID(target);
	}

	public String getSourceID() {
		return sourceID;
	}

	public void setSourceID(String sourceID) {
		this.sourceID = sourceID;
	}

	public String getTargetID() {
		return targetID;
	}

	public void setTargetID(String targetID) {
		this.targetID = targetID;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
	        return false;
	    }
	    if (getClass() != obj.getClass()) {
	        return false;
	    }
	    final Relation other = (Relation) obj;
	    return (this.sourceID.equals(other.sourceID) && 
	    	this.targetID.equals(other.targetID));
	}

	public String getSourceType() {
		return sourceType;
	}

	public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}

	public String getTargetType() {
		return targetType;
	}

	public void setTargetType(String targetType) {
		this.targetType = targetType;
	}

}
