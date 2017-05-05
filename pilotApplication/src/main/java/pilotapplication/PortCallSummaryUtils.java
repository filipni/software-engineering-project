package pilotapplication;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import eu.portcdm.dto.PortCallSummary;
import javafx.scene.image.Image;

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
	
	/**
	 * Download vessel image from photo URL in given portcall summary
	 * 
	 * @param summary portcall summary where the URL can be found
	 * @return image of the vessel
	 */
	public static Image downloadVesselImage(PortCallSummary summary) {
		return downloadImage(summary.getVessel().getPhotoURL());		
	}
		
	/**
	 * Download image from URL.
	 * 
	 * @param url string representation of the URL pointing to the image
	 * @return the downloaded image if no exceptions were caught
	 */
	public static Image downloadImage(String url) {
		Image img = null;
		try {
			URLConnection conn = new URL(url).openConnection();
			conn.setRequestProperty("User-Agent", "Wget/1.13.4 (linux-gnu)");
			InputStream in = conn.getInputStream();
			img = new Image(in);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return img;
	}
	
}
