package pilotapplication;

import java.util.ArrayList;
import java.util.List;

import eu.portcdm.dto.PortCallSummary;

public class PortCallSummaryUtils {
	
	/**
	 * DOCUMENTATION HERE PLEASE!
	 * 
	 * @param portcalls description of parameter
	 * @return description of return value
	 */
	public static List<String> getVesselIds(List<PortCallSummary> portcalls) {
		List<String> ids = new ArrayList<>();
		for(PortCallSummary pc : portcalls) {
		ids.add(getVesselId(pc));
		}
		return ids;
	}
	
	/**
	 * DOCUMENTATION HERE PLEASE!
	 * 
	 * @param pcs description of parameter
	 * @return description of return value
	 */
	public static String getVesselId(PortCallSummary pcs) {
		String id = "IMO:" + pcs.getVessel().getImo();
		return id;
	}
	
}
