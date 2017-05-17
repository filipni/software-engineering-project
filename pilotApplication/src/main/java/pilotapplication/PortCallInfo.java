package pilotapplication;

public class PortCallInfo {
	
	private final String vesselId;
	private final String portCallId;
	private String ETA;
	private String boatName;
	private boolean confirmed;
	
	public PortCallInfo(String vesselId, String portCallId, String ETA, String boatName) {
		this.vesselId = vesselId;
		this.portCallId = portCallId;
		this.ETA = ETA;
		confirmed = false;
	}
	
	public void updateETA(String newETA) {
		ETA = newETA;
	}
	
	public String getETA() {
		return ETA;
	}
	
	public String getVesselId() {
		return vesselId;
	}
	
	public String getPortCallId() {
		return portCallId;
	}
	
	public boolean getConfirmationStatus() {
		return confirmed;
	}
	
	public void confirmRequest() {
		confirmed = true;
	}
	
	public String getName() {
		return boatName;
	}
	
}
