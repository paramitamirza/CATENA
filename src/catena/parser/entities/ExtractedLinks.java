package catena.parser.entities;

public class ExtractedLinks {
	
	private TLINK tlink;
	private CLINK clink;
	
	public ExtractedLinks() {
		this.setTlink(new TLINK());
		this.setClink(new CLINK());
	}
	
	public ExtractedLinks(TLINK tlinks, CLINK clinks) {
		this.setTlink(tlinks);
		this.setClink(clinks);
	}
	
	public void appendLinks(ExtractedLinks links) {
		this.getTlink().getTT().addAll(links.getTlink().getTT());
		this.getTlink().getED().addAll(links.getTlink().getED());
		this.getTlink().getET().addAll(links.getTlink().getET());
		this.getTlink().getEE().addAll(links.getTlink().getEE());
		this.getClink().getEE().addAll(links.getClink().getEE());
	}

	public TLINK getTlink() {
		return tlink;
	}

	public void setTlink(TLINK tlink) {
		this.tlink = tlink;
	}

	public CLINK getClink() {
		return clink;
	}

	public void setClink(CLINK clink) {
		this.clink = clink;
	}

}
