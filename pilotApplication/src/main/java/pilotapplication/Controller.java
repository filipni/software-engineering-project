package pilotapplication;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import se.viktoria.stm.portcdm.connector.common.util.StateWrapper;
import eu.portcdm.dto.LocationTimeSequence;
import eu.portcdm.dto.PortCallSummary;
import eu.portcdm.mb.dto.Filter;
import eu.portcdm.mb.dto.FilterType;
import eu.portcdm.messaging.LocationReferenceObject;
import eu.portcdm.messaging.LogicalLocation;
import eu.portcdm.messaging.PortCallMessage;
import eu.portcdm.messaging.ServiceObject;
import eu.portcdm.messaging.ServiceTimeSequence;
import eu.portcdm.messaging.TimeType;

public class Controller implements Initializable {	
	@FXML
	private ListView<String> idListView; 
	
	@FXML
	private Text idText, vesselStatusText, bookTimeText, etaTimeText;
	
	@FXML 
	private GridPane gridPane1; 
	
	@FXML 
	private HBox hBoxRec1, hBoxRec2; 
	
	@FXML
	private AnchorPane vesselInfoPane, phonePane; 
	
	@FXML
	private ImageView statusImg; 
	
	private PortCDMApi portcdmApi;
	private SimpleDateFormat dateFormat;
	private Map<String, PortCallSummary> portCallTable;
	private String requestQueueId;
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {	
		boolean useDevServer = false;
		portcdmApi = new PortCDMApi(useDevServer);
		portCallTable = createPortCallTable(10);
		populateIdList();
		
		dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Used to generate timestamps
		
		createPilotageRequestQueue();
		sendTestMessages(3);
					
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
	 * Creates a hashmap of portcall summaries fetched from PortCDM.
	 * Each summary is index by its IMO. 
	 * 
	 * @param max maximum number of summaries to fetch
	 * @return hashmap with the fetched portcall summaries
	 */
	private HashMap<String, PortCallSummary> createPortCallTable(int max) {
		HashMap<String, PortCallSummary> map = new HashMap<>();
		List<PortCallSummary> summaries = portcdmApi.getPortCalls(max);
		for (PortCallSummary s : summaries) {
			map.put(s.getVessel().getImo(), s);
		}
		return map;
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
		idText.setText(id);
		
		vesselInfoPane.setVisible(true);
		phonePane.setVisible(true);
		
		// Set all portcalls that do not have an IMO starting with "9" to incoming
		if (!id.startsWith("9")) {
			statusImg.setImage(new Image("pilotapplication/img/Inkommande.png"));
			vesselStatusText.setText("Inkommande");
		}	
		else {
			statusImg.setImage(new Image("pilotapplication/img/Avgående.png"));
			vesselStatusText.setText("Avgående");
		}
	}
	
	@FXML
	public void pilotageCommenced(ActionEvent event) {
		String imo = idListView.getSelectionModel().getSelectedItem();
		String timestamp = dateFormat.format(new Date());
        StateWrapper wrapper = new StateWrapper(ServiceObject.PILOTAGE, ServiceTimeSequence.COMMENCED, LogicalLocation.TUG_ZONE, LogicalLocation.VESSEL);
        PortCallMessage pcm = portcdmApi.portCallMessageFromStateWrapper(createVesselIdFromIMO(imo), wrapper, timestamp, TimeType.ACTUAL);
        portcdmApi.sendPortCallMessage(pcm);
	}
	
	@FXML
	public void departurePilotVessel(ActionEvent event) {
		// Departure from vessel
		String imo = idListView.getSelectionModel().getSelectedItem();
		String timestamp = dateFormat.format(new Date());
        StateWrapper wrapperLocation = new StateWrapper(LocationReferenceObject.PILOT, LocationTimeSequence.DEPARTURE_FROM, LogicalLocation.VESSEL);
        PortCallMessage pcmLocation = portcdmApi.portCallMessageFromStateWrapper(createVesselIdFromIMO(imo), wrapperLocation, timestamp, TimeType.ACTUAL);
        portcdmApi.sendPortCallMessage(pcmLocation);
        
        // Pilotage complete
        StateWrapper wrapperService = new StateWrapper(ServiceObject.PILOTAGE, ServiceTimeSequence.COMPLETED, LogicalLocation.TUG_ZONE, LogicalLocation.VESSEL);
        PortCallMessage pcmService = portcdmApi.portCallMessageFromStateWrapper(createVesselIdFromIMO(imo), wrapperService, timestamp, TimeType.ACTUAL);
        portcdmApi.sendPortCallMessage(pcmService);
	}
	
	@FXML
	public void arrivalPilotVessel(ActionEvent event) {
		String imo = idListView.getSelectionModel().getSelectedItem();
		String timestamp = dateFormat.format(new Date());
        StateWrapper wrapperLocation = new StateWrapper(LocationReferenceObject.PILOT, LocationTimeSequence.ARRIVAL_TO, LogicalLocation.VESSEL);
        PortCallMessage pcmLocation = portcdmApi.portCallMessageFromStateWrapper(createVesselIdFromIMO(imo), wrapperLocation, timestamp, TimeType.ACTUAL);
        portcdmApi.sendPortCallMessage(pcmLocation);
	}
	
	@FXML
	public void arrivalPilotBerth(ActionEvent event) {
		String imo = idListView.getSelectionModel().getSelectedItem();
		String timestamp = dateFormat.format(new Date());
        StateWrapper wrapperLocation = new StateWrapper(LocationReferenceObject.PILOT, LocationTimeSequence.ARRIVAL_TO, LogicalLocation.BERTH);
        PortCallMessage pcmLocation = portcdmApi.portCallMessageFromStateWrapper(createVesselIdFromIMO(imo), wrapperLocation, timestamp, TimeType.ACTUAL);
        portcdmApi.sendPortCallMessage(pcmLocation);
	}
	
	private String createVesselIdFromIMO(String imo) {
		return "urn:x-mrn:stm:vessel:IMO:" + imo;
	}
	
	private String createVesselIdFromIMO(int imo) {
		return "urn:x-mrn:stm:vessel:IMO:" + imo;
	}
	
	/**
     * Sends a given number of pilotage requests to the backend
     * 
     * @param nrToSend number of messages to send
     */
	
	private final int MIN_VESSEL_ID = 1000000;
	private final int MAX_VESSEL_ID = 9999999;
	
    private void sendTestMessages(int nrToSend) {
        //List<PortCallSummary> summaries = portcdmApi.getPortCalls(5);
	    for (int i = 0; i < nrToSend; i++) {
	    	int vesselIMO = ThreadLocalRandom.current().nextInt(MIN_VESSEL_ID, MAX_VESSEL_ID + 1);
	        String timestamp = dateFormat.format(new Date());
		    StateWrapper wrapper = new StateWrapper(ServiceObject.PILOTAGE, ServiceTimeSequence.REQUESTED, LogicalLocation.TUG_ZONE, LogicalLocation.VESSEL);
		    PortCallMessage pcm = portcdmApi.portCallMessageFromStateWrapper(createVesselIdFromIMO(vesselIMO), wrapper, timestamp, TimeType.ACTUAL);
		    portcdmApi.sendPortCallMessage(pcm);
	    }    
           
        // Wait for a while to make sure the message arrives at the queue
        try {
			TimeUnit.SECONDS.sleep(3);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        
        List<PortCallMessage> messages = portcdmApi.fetchMessagesFromQueue(requestQueueId);
        
        for (PortCallMessage msg : messages) {
        	System.out.println("Message received with id: " + msg.getMessageId() + " (PortCall: " + msg.getPortCallId() + ")");
        }
    }
		
}