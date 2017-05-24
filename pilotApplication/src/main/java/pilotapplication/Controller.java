package pilotapplication;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import eu.portcdm.dto.LocationTimeSequence;
import eu.portcdm.mb.dto.Filter;
import eu.portcdm.mb.dto.FilterType;
import eu.portcdm.messaging.LocationReferenceObject;
import eu.portcdm.messaging.LogicalLocation;
import eu.portcdm.messaging.PortCallMessage;
import eu.portcdm.messaging.ServiceObject;
import eu.portcdm.messaging.ServiceTimeSequence;
import eu.portcdm.messaging.TimeType;

import se.viktoria.stm.portcdm.connector.common.util.StateWrapper;

public class Controller implements Initializable {	
	@FXML
	private ListView<String> idListView; 
	
	@FXML
	private Text idText, vesselStatusText;
	
	@FXML
	private TextField hourETA, minuteETA, dayETA, monthETA, yearETA;
	
	@FXML 
	private GridPane gridPane1; 
	
	@FXML 
	private Label updateLabel, departureLocationLabel, arrivalLocationLabel, etaTimeLabel, bookTimeLabel;
	
	@FXML 
	private HBox hBoxRec1, hBoxRec2; 
	
	@FXML
	private AnchorPane vesselInfoPane, phonePane; 
	
	@FXML
	private ImageView statusImg; 
	
	private PortCDMApi portcdmApi;
	private SimpleDateFormat dateFormat;
	private Map<String, PortCallInfo> portCallTable;
	private Map<LogicalLocation, String> locationMap;
	private Map<String, LogicalLocation> rLocationMap;
	private String requestQueueId;
	private boolean useDevServer;
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {	
		useDevServer = true;
		portcdmApi = new PortCDMApi(useDevServer);
		dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Used to generate timestamps
		
		vesselInfoPane.setVisible(false);
		phonePane.setVisible(false);
		
		createLocationMap();
		createReversedLocationMap();
		
		createPilotageRequestQueue();
		sendTestMessage(); 
		updateRequestList(null);
	}
	
	private Map<LogicalLocation, String> createLocationMap() {
		locationMap = new HashMap<>();
		locationMap.put(LogicalLocation.ANCHORING_AREA, "Ankarplats");
		locationMap.put(LogicalLocation.BERTH, "Kaj");
		locationMap.put(LogicalLocation.ETUG_ZONE, "-/");
		locationMap.put(LogicalLocation.LOC, "-");
		locationMap.put(LogicalLocation.PILOT_BOARDING_AREA, "Lotsombordstigning");
		locationMap.put(LogicalLocation.RENDEZV_AREA, "Träffpunkt");
		locationMap.put(LogicalLocation.TRAFFIC_AREA, "Trafikområde");
		locationMap.put(LogicalLocation.TUG_ZONE, "Bärgningszon");
		locationMap.put(LogicalLocation.VESSEL, "Skepp");
		return locationMap;
	}
	
	
	private Map<String, LogicalLocation> createReversedLocationMap() {
		rLocationMap = new HashMap<>();
		rLocationMap.put("Ankarplats", LogicalLocation.ANCHORING_AREA);
		rLocationMap.put("Kaj", LogicalLocation.BERTH);
		rLocationMap.put("-/", LogicalLocation.ETUG_ZONE);
		rLocationMap.put("-", LogicalLocation.LOC);
		rLocationMap.put("Lotsombordstigning", LogicalLocation.PILOT_BOARDING_AREA);
		rLocationMap.put("Träffpunkt", LogicalLocation.RENDEZV_AREA);
		rLocationMap.put("Trafikområde", LogicalLocation.TRAFFIC_AREA);
		rLocationMap.put("Bärgningszon", LogicalLocation.TUG_ZONE);
		rLocationMap.put("Skepp", LogicalLocation.VESSEL);
		return rLocationMap;
	}
	
	public String getInputETA() {
		String year = parseDateElement(yearETA.getText());
		String month = parseDateElement(monthETA.getText());
		String day = parseDateElement(dayETA.getText());
		
		String hour = parseTimeElement(hourETA.getText(), 0, 24);
		String minute = parseTimeElement(minuteETA.getText(), 0, 60);
		
		return "20" + year + "-" + month +  "-" + day + "T" + hour + ":" + minute + ":00Z";
	}

	public String getInputETD() {
		String year = parseDateElement(yearETA.getText());
		String month = parseDateElement(monthETA.getText());
		String day = parseDateElement(dayETA.getText());
		
		String hour = parseTimeElement(hourETA.getText(), 2, 24);
		String minute = parseTimeElement(minuteETA.getText(), 0, 60);
		
		return "20" + year + "-" + month +  "-" + day + "T" + hour + ":" + minute + ":00Z";
	}
	
	private String parseDateElement(String s) {
		String returnString = s;
		if (!s.matches("\\d*")) {
            returnString = s.replaceAll("[^\\d]", "");
        }
		if (returnString != null && returnString.length() == 1) {
			returnString = "0" + returnString;
		}
		return returnString;
	}
	
	private String parseTimeElement(String s, int timeDelay, int mod) {
		String returnString = s;
		if (!s.matches("\\d*")) {
            returnString = s.replaceAll("[^\\d]", "");
        }
		returnString = "" + ((Integer.parseInt(returnString) + timeDelay) % mod);
		if (returnString != null && returnString.length() == 1) {
			returnString = "0" + returnString;
		}
		return returnString;
	}
	
	/*	
	public String getETD(String id) {
		PortCallInfo pcInfo = portCallTable.get(id);
		String[] ETA = pcInfo.getETA().split("T");
		String[] time = ETA[1].split(":");
		String hours = time[0];
		int hoursAsInt = Integer.parseInt(hours);
		int newHoursAsInt = hoursAsInt + 2;
		String newTimestamp = ETA[0] + 'T' + String.valueOf(newHoursAsInt) + ":" + time[1] + ":" + time[2];
		System.out.println(newTimestamp);
		return "";
	}
	*/
	
	/**
     * Sends a given number of pilotage requests to the backend
     * 
     * @param nrToSend number of messages to send
     */	
    private void sendTestMessage() {
	    String[] vesselIds = {"urn:mrn:stm:vessel:IMO:8027298", "urn:mrn:stm:vessel:IMO:8327105"};
	    String timestamp = dateFormat.format(new Date());
	    for (String vesselId : vesselIds) {
	    	StateWrapper wrapper = new StateWrapper(ServiceObject.PILOTAGE, ServiceTimeSequence.REQUESTED, LogicalLocation.TUG_ZONE, LogicalLocation.VESSEL);
	    	PortCallMessage pcm = portcdmApi.portCallMessageFromStateWrapper(vesselId, wrapper, timestamp, TimeType.ACTUAL);
	    	portcdmApi.sendPortCallMessage(pcm);
	    }
           
        // Wait for a while to make sure the message arrives at the queue
        try {
			TimeUnit.SECONDS.sleep(3);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }
	
	/**
	 * Create a queue in portCDM for pilotage request messages
	 */	
	private void createPilotageRequestQueue() {
		List<Filter> filters = new ArrayList<>();
        Filter timeSequence = new Filter();

        timeSequence.setType(FilterType.TIME_SEQUENCE);
        timeSequence.setElement(ServiceTimeSequence.REQUESTED.toString());
        filters.add(timeSequence);    
        
        requestQueueId = portcdmApi.postQueue(filters);
	}
	
	/**
	 * Update portCallTable with info from messages fetched from the pilotage request queue.
	 * 
	 * @return hashmap with summaries of each port call, index by vessel id
	 */
	private void updatePortCallTable() {
		if (portCallTable == null) {
			portCallTable = new HashMap<>();
		}
		
		List<PortCallMessage> messages;
		if (useDevServer) {
			messages = portcdmApi.fetchMessagesFromDevQueue(requestQueueId);
		}
		else {
			messages = portcdmApi.fetchMessagesFromQueue(requestQueueId);
		}
		
		for (PortCallMessage pcm : messages) {
			if (pcm.getServiceState().getServiceObject().toString().equals("PILOTAGE")) {
				String boatName = portcdmApi.getPortCall(pcm.getPortCallId()).getVessel().getName();
				String toLocation = locationMap.get(pcm.getServiceState().getBetween().getTo().getLocationType());
				String fromLocation = locationMap.get(pcm.getServiceState().getBetween().getFrom().getLocationType());
				PortCallInfo pcInfo = new PortCallInfo(pcm.getVesselId(), pcm.getVesselId(), pcm.getServiceState().getTime().toString(), pcm.getServiceState().getTimeType().toString(), boatName, fromLocation, toLocation );
				portCallTable.put(boatName, pcInfo);
			}
		}
	}
	
	/**
	 * Populates the idListView with ids found in portCallTable.
	 */	
	private void populateIdList() {
		ObservableList<String> listElements = FXCollections.observableArrayList(portCallTable.keySet());
		idListView.setItems(listElements); // What if listElements is empty?
	}
	
	/**
	 * This method handles mouse clicks from the idListView.
	 * It makes a new panel visible containing information about the vessel corresponding to the clicked id.
	 * 
	 * @param e mouse event with information about the clicked element
	 */	
	@FXML
	public void handleMouseClick(MouseEvent event) {
		String id = idListView.getSelectionModel().getSelectedItem();
		if (id == null) { // In case we clicked on an empty cell
			return;
		}
		idText.setText(id);
		
		vesselInfoPane.setVisible(true);
		phonePane.setVisible(true);
		
		PortCallInfo pcInfo = portCallTable.get(id);
		String[] eta = pcInfo.getETA().split("T");
		if (pcInfo.getConfirmationStatus()) {
			bookTimeLabel.setVisible(true);
			bookTimeLabel.setText(pcInfo.bookedTime);
			etaTimeLabel.setText(eta[0] + "\n" + eta[1].substring(0, (eta[1].length()-4))); 
		}
		else {
			bookTimeLabel.setVisible(false);
		}
		
		statusImg.setImage(new Image("pilotapplication/img/Inkommande.png")); // Sets ship image on popup window
		etaTimeLabel.setText(eta[0] + "\n" + eta[1].substring(0, (eta[1].length()-4))); 
		departureLocationLabel.setText(pcInfo.getFromLocation());
		arrivalLocationLabel.setText(pcInfo.getToLocation());
	}
	
	@FXML
	public void updateRequestList(ActionEvent event) {
		updatePortCallTable();
		populateIdList();
		String updateDate = dateFormat.format(new Date()).split("T")[0]; // Take just first part of date
		updateLabel.setText("Uppdaterad: " + updateDate);
	}
	
	@FXML
	public void pilotageCommenced(ActionEvent event) {
		String boatName = idListView.getSelectionModel().getSelectedItem();
		PortCallInfo pcInfo = portCallTable.get(boatName);
		String vesselId = pcInfo.getVesselId();
		String timestamp = dateFormat.format(new Date());
        StateWrapper wrapper = new StateWrapper(ServiceObject.PILOTAGE, ServiceTimeSequence.COMMENCED, rLocationMap.get(pcInfo.getToLocation()), rLocationMap.get(pcInfo.getFromLocation()));
        PortCallMessage pcm = portcdmApi.portCallMessageFromStateWrapper(vesselId, wrapper, timestamp, TimeType.ACTUAL);
        portcdmApi.sendPortCallMessage(pcm);
	}
	
	public void pilotageCommencedEstimated(String boatName) {
		PortCallInfo pcInfo = portCallTable.get(boatName);
		String vesselId = pcInfo.getVesselId();
        StateWrapper wrapper = new StateWrapper(ServiceObject.PILOTAGE, ServiceTimeSequence.COMMENCED, rLocationMap.get(pcInfo.getToLocation()), rLocationMap.get(pcInfo.getFromLocation()));
        PortCallMessage pcm = portcdmApi.portCallMessageFromStateWrapper(vesselId, wrapper, getInputETA(), TimeType.ESTIMATED);
        portcdmApi.sendPortCallMessage(pcm);
	}
	
	@FXML
	public void departurePilotVessel(ActionEvent event) {
		// Departure from vessel
		String boatName = idListView.getSelectionModel().getSelectedItem();
		PortCallInfo pcInfo = portCallTable.get(boatName);
		String vesselId = pcInfo.getVesselId();
		String timestamp = dateFormat.format(new Date());
        StateWrapper wrapperLocation = new StateWrapper(LocationReferenceObject.PILOT, LocationTimeSequence.DEPARTURE_FROM, LogicalLocation.VESSEL);
        PortCallMessage pcmLocation = portcdmApi.portCallMessageFromStateWrapper(vesselId, wrapperLocation, timestamp, TimeType.ACTUAL);
        portcdmApi.sendPortCallMessage(pcmLocation);
        
        // Pilotage complete
        StateWrapper wrapperService = new StateWrapper(ServiceObject.PILOTAGE, ServiceTimeSequence.COMPLETED, rLocationMap.get(pcInfo.getToLocation()), rLocationMap.get(pcInfo.getFromLocation()));
        PortCallMessage pcmService = portcdmApi.portCallMessageFromStateWrapper(vesselId, wrapperService, timestamp, TimeType.ACTUAL);
        portcdmApi.sendPortCallMessage(pcmService);
        
        removeRequest(boatName);  
	}
	
	public void pilotageCompleteEstimated(String boatName) {
		PortCallInfo pcInfo = portCallTable.get(boatName);
		String vesselId = pcInfo.getVesselId();
        StateWrapper wrapperService = new StateWrapper(ServiceObject.PILOTAGE, ServiceTimeSequence.COMPLETED, rLocationMap.get(pcInfo.getToLocation()), rLocationMap.get(pcInfo.getFromLocation()));
        PortCallMessage pcmService = portcdmApi.portCallMessageFromStateWrapper(vesselId, wrapperService, getInputETD(), TimeType.ESTIMATED);
        portcdmApi.sendPortCallMessage(pcmService);
        
	}
	
	@FXML
	public void pilotageDenied(ActionEvent event) {
		String boatName = idListView.getSelectionModel().getSelectedItem();
		PortCallInfo pcInfo = portCallTable.get(boatName);
		String vesselId = pcInfo.getVesselId();
		String timestamp = dateFormat.format(new Date());
		StateWrapper wrapperService = new StateWrapper(ServiceObject.PILOTAGE, ServiceTimeSequence.DENIED, rLocationMap.get(pcInfo.getToLocation()), rLocationMap.get(pcInfo.getFromLocation()));
        PortCallMessage pcmService = portcdmApi.portCallMessageFromStateWrapper(vesselId, wrapperService, timestamp, TimeType.ACTUAL);
        portcdmApi.sendPortCallMessage(pcmService);
        
        removeRequest(boatName);
	}
	
	/**
	 * Remove a vessel from the request list and update the GUI accordingly.
	 * 
	 * @param boatName name of the boat to remove
	 */
	private void removeRequest(String boatName) {
		portCallTable.remove(boatName);
        populateIdList();
        vesselInfoPane.setVisible(false);
		phonePane.setVisible(false);
	}
	
	@FXML
	public void arrivalPilotVessel(ActionEvent event) {
		String boatName = idListView.getSelectionModel().getSelectedItem();
		PortCallInfo pcInfo = portCallTable.get(boatName);
		String vesselId = pcInfo.getVesselId();
		String timestamp = dateFormat.format(new Date());
        StateWrapper wrapperLocation = new StateWrapper(LocationReferenceObject.PILOT, LocationTimeSequence.ARRIVAL_TO, LogicalLocation.VESSEL);
        PortCallMessage pcmLocation = portcdmApi.portCallMessageFromStateWrapper(vesselId, wrapperLocation, timestamp, TimeType.ACTUAL);
        portcdmApi.sendPortCallMessage(pcmLocation);
	}
	
	@FXML
	public void arrivalPilotVesselEstimated(ActionEvent event) {
		String boatName = idListView.getSelectionModel().getSelectedItem();
		PortCallInfo pcInfo = portCallTable.get(boatName);
		String vesselId = pcInfo.getVesselId();
        StateWrapper wrapperLocation = new StateWrapper(LocationReferenceObject.PILOT, LocationTimeSequence.ARRIVAL_TO, LogicalLocation.VESSEL);
        PortCallMessage pcmLocation = portcdmApi.portCallMessageFromStateWrapper(vesselId, wrapperLocation, getInputETA(), TimeType.ESTIMATED);
        portcdmApi.sendPortCallMessage(pcmLocation);
	}
	
	@FXML
	public void arrivalPilotBerth(ActionEvent event) {
		String boatName = idListView.getSelectionModel().getSelectedItem();
		PortCallInfo pcInfo = portCallTable.get(boatName);
		String vesselId = pcInfo.getVesselId();
		String timestamp = dateFormat.format(new Date());
        StateWrapper wrapperLocation = new StateWrapper(LocationReferenceObject.PILOT, LocationTimeSequence.ARRIVAL_TO, LogicalLocation.BERTH);
        PortCallMessage pcmLocation = portcdmApi.portCallMessageFromStateWrapper(vesselId, wrapperLocation, timestamp, TimeType.ACTUAL);
        portcdmApi.sendPortCallMessage(pcmLocation);
	}
	
	@FXML
	public void arrivalPilotBerthEstimated(ActionEvent event) {
		String boatName = idListView.getSelectionModel().getSelectedItem();
		PortCallInfo pcInfo = portCallTable.get(boatName);
		String vesselId = pcInfo.getVesselId();
        StateWrapper wrapperLocation = new StateWrapper(LocationReferenceObject.PILOT, LocationTimeSequence.ARRIVAL_TO, LogicalLocation.BERTH);
        PortCallMessage pcmLocation = portcdmApi.portCallMessageFromStateWrapper(vesselId, wrapperLocation, getInputETA(), TimeType.ESTIMATED);
        portcdmApi.sendPortCallMessage(pcmLocation);
	}
	
	@FXML
	public void pilotageConfirmed(ActionEvent event) {
		String boatName = idListView.getSelectionModel().getSelectedItem();
		PortCallInfo pcInfo = portCallTable.get(boatName);
		String vesselId = pcInfo.getVesselId();
		System.out.println(vesselId);
		String timestamp = dateFormat.format(new Date());
		StateWrapper wrapperService = new StateWrapper(ServiceObject.PILOTAGE, ServiceTimeSequence.CONFIRMED, rLocationMap.get(pcInfo.getToLocation()), rLocationMap.get(pcInfo.getFromLocation()));
        PortCallMessage pcmService = portcdmApi.portCallMessageFromStateWrapper(vesselId, wrapperService, timestamp, TimeType.ACTUAL);
        
        String input = getInputETA();
        String[] bookedDate = input.split("T");
        
        pcInfo.bookedTime = bookedDate[0] + "\n" + bookedDate[1].substring(0, bookedDate[1].length() - 4);
        bookTimeLabel.setText(pcInfo.bookedTime);
        
        portcdmApi.sendPortCallMessage(pcmService);
        pilotageCommencedEstimated(boatName);
        pilotageCompleteEstimated(boatName);
		
        pcInfo.confirmRequest();
        bookTimeLabel.setVisible(true);		
	}
	
	@FXML 
	public void mooringRequestedOutgoing(ActionEvent event) {
		String boatName = idListView.getSelectionModel().getSelectedItem();
		String vesselId = portCallTable.get(boatName).getVesselId();
		String timestamp = dateFormat.format(new Date());
		StateWrapper wrapperService = new StateWrapper(ServiceObject.DEPARTURE_MOORING_OPERATION, ServiceTimeSequence.REQUESTED, LogicalLocation.BERTH);
		PortCallMessage pcmService = portcdmApi.portCallMessageFromStateWrapper(vesselId, wrapperService, timestamp, TimeType.ESTIMATED);
		portcdmApi.sendPortCallMessage(pcmService);
	}		
			
	@FXML 
	public void mooringRequestedIncoming(ActionEvent event) {
		String boatName = idListView.getSelectionModel().getSelectedItem();
		String vesselId = portCallTable.get(boatName).getVesselId();
		String timestamp = dateFormat.format(new Date());
		StateWrapper wrapperService = new StateWrapper(ServiceObject.ARRIVAL_MOORING_OPERATION, ServiceTimeSequence.REQUESTED, LogicalLocation.BERTH);
		PortCallMessage pcmService = portcdmApi.portCallMessageFromStateWrapper(vesselId, wrapperService, timestamp, TimeType.ESTIMATED);
		portcdmApi.sendPortCallMessage(pcmService);
	}		

}