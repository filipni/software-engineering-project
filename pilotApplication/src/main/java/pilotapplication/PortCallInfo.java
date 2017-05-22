package pilotapplication;

public class PortCallInfo {
	
	private final String vesselId;
	private final String portCallId;
	private String ETA;
	private String timeType;
	private String boatName;
	private String fromLocation;
	private String toLocation;
	private boolean confirmed;
	
	public PortCallInfo(String vesselId, String portCallId, String ETA, String timeType, String boatName, String fromLocation, String toLocation) {
		this.vesselId = vesselId;
		this.portCallId = portCallId;
		this.ETA = ETA;
		this.timeType = timeType;
		this.fromLocation = fromLocation;
		this.toLocation = toLocation;
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
	
	public String getFromLocation() {
		return fromLocation;
	}
	
	public String getToLocation() {
		return toLocation;
	}
	
	public String getTimeType() {
		return timeType;
	}
	
}
