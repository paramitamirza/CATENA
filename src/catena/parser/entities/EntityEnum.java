package catena.parser.entities;

public final class EntityEnum {
	
	public static enum Language {
		EN, IT;
	}
	
	public static enum TlinkType {
		BEFORE, AFTER, 
		IBEFORE, IAFTER, 
		BEGINS, BEGUN_BY, 
		ENDS, ENDED_BY,
		INCLUDES, IS_INCLUDED, 
		DURING, DURING_INV, MEASURE, 
		SIMULTANEOUS, IDENTITY;
	}
	
	public static enum ClinkType {
		CAUSE, ENABLE, PREVENT;
	}

}
