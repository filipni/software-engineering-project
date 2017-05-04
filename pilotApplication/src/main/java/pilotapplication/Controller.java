package pilotapplication;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Predicate;

import javax.json.Json;
import javax.persistence.GeneratedValue;

import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.gson.JsonObject;

import eu.portcdm.mb.client.MessageQueueServiceApi;
import eu.portcdm.messaging.LocationReferenceObject;
import eu.portcdm.messaging.LogicalLocation;
import eu.portcdm.messaging.PortCallMessage;
import eu.portcdm.messaging.TimeType;
import eu.portcdm.client.ApiException;
import eu.portcdm.client.service.PortcallsApi;
import eu.portcdm.dto.LocationTimeSequence;
import eu.portcdm.dto.PortCallSummary;

import se.viktoria.stm.portcdm.connector.common.SubmissionService;
import se.viktoria.stm.portcdm.connector.common.util.PortCallMessageBuilder;
import se.viktoria.stm.portcdm.connector.common.util.StateWrapper;
import se.viktoria.util.Configuration;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;


public class Controller implements Initializable
{
	// Two base URLs are provided; one for the local virtual machine and one for the external server
	private final String BASE_URL_VM = "http://192.168.56.101:8080/";
	private final String BASE_URL_DEV = "http://dev.portcdm.eu:8080/";
	
	// File path for the configuration file used when creating the submissionService
	public  final String CONFIG_FILE_NAME = "portcdm.conf";
	public  final String CONFIG_FILE_DIR = "portcdm";
	
	// These objects will be our interface to PortCDM 
	private SubmissionService submissionService;
	private MessageQueueServiceApi messageBrokerAPI;
	private PortcallsApi portCallsAPI;
	
	ArrayList <String> IDs = new ArrayList<>(Arrays.asList("IMO:9398917", "IMO:9371878", "IMO:9299707", "IMO:9425356", "IMO:9186728", "IMO:9057173", "IMO:9247168"));
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {		
		initSubmissionService(CONFIG_FILE_NAME, CONFIG_FILE_DIR);	
		initMessageBrokerAPI(BASE_URL_VM + "mb", 20000);
		initPortCallsAPI(BASE_URL_VM + "dmp", 20000);

		
		
		ObservableList<Button> buttons = FXCollections.observableArrayList();
		for (int i = 0; i < IDs.size(); i++) {
			Button b = new Button();
			b.setText(IDs.get(i));
			buttons.add(b);
			
			
			b.setOnAction((event) -> { popWindow(b); });	
			}
		IDListView.setItems(buttons);	
	}
		
	private void popWindow(Button b) {
		String id = b.getText(); 
		AnchorPane1.setVisible(true);
		idText.setText(id);
		bookTimeText.setText("17:25 23-april"); //TODO: ändra till interaktiv
		etaTimeText.setText("03:12 24-april");
		
		
		if (id.equals("IMO:9371878")) { //TODO: ändra kommandot för att byta bild
			imgViewAvg.setVisible(false);
			imgViewInk.setVisible(true);
			inkOrAvgText.setText("Inkommande");
		}	
		else {
			imgViewAvg.setVisible(true); 
			imgViewInk.setVisible(false);
			inkOrAvgText.setText("Avgående");
		}
	}
	
	private List<PortCallSummary> getPortCalls() {

		// Try to read some portcalls from PortCDM
		/*
		List<PortCallSummary> portcalls = null;
		try {
			portcalls = portCallsAPI.getAllPortCalls(10);
		} catch (ApiException e) {
			System.out.println(e.getCode() + " " + e.getMessage());
			System.out.println(e.getResponseBody());
		}
		
		for(PortCallSummary pc : portcalls) {
			System.out.println(pc.getId());
		}*/
	
				List<PortCallSummary> portcalls = null;
				List<String> ids = new ArrayList<>();
				try {
					portcalls = portCallsAPI.getAllPortCalls(10);
				} catch (ApiException e) {
					System.out.println(e.getCode() + " " + e.getMessage());
					System.out.println(e.getResponseBody());
				}
				return portcalls;
	}
	
	/*
	 * 
	 */
	private String getVesselId(PortCallSummary pcs) {
		String id = "IMO:" + pcs.getVessel().getImo();
		return id;
	}
	
	/*
	 * 
	 */
	private List<String> getVesselIds(List<PortCallSummary> portcalls) {
		List<String> ids = new ArrayList<>();
		for(PortCallSummary pc : portcalls) {
		ids.add(getVesselId(pc));
		}
		return ids;
	}
		
	

	/*
	 * Skapar ett exempel-pcm som används i test-syfte.
	 */
    private PortCallMessage getExampleMessage() {
        StateWrapper stateWrapper = new StateWrapper(
                LocationReferenceObject.VESSEL, //referenceObject
                LocationTimeSequence.ARRIVAL_TO, //ARRIVAL_TO or DEPARTURE_FROM
                LogicalLocation.BERTH, //Type of required location
                53.50, //Latitude of required location
                53.50, //Longitude of required location
                "Skarvik Harbour 518", //Name of required location
                LogicalLocation.ANCHORING_AREA, //Type of optional location
                52.50, //Latitude of optional location
                52.50, //Longitude of optional location
                "Dana Fjord D1" );//Name of optional location
        //Change dates from 2017-03-23 06:40:00 to 2017-03-23T06:40:00Z 
        PortCallMessage portCallMessage = PortCallMessageBuilder.build(
                "urn:mrn:stm:portcdm:local_port_call:SEGOT:DHC:52723", //localPortCallId
                "urn:mrn:stm:portcdm:local_job:FENIX_SMA:990198125", //localJobId
                stateWrapper, //StateWrapper created above
                "2017-03-23T06:40:00Z", //Message's time
                TimeType.ESTIMATED, //Message's timeType
                "urn:mrn:stm:vessel:IMO:9259501", //vesselId
                "2017-03-23T06:38:56Z", //reportedAt (optional)
                "Viktoria", //reportedBy (optional)
                "urn:mrn:stm:portcdm:message:5eadbb1c-6be7-4cf2-bd6d-f0af5a0c35dc", //groupWith (optional), messageId of the message to group with.
                "example comment" //comment (optional)
        );

        return portCallMessage;
    }
    
	private void initSubmissionService(String configFileName, String configFileDir) {
		Configuration config = new Configuration(
				configFileName, 
				configFileDir,
				new Predicate<Map.Entry<Object, Object>>() {
					@Override
					public boolean test(Map.Entry<Object, Object> objectObjectEntry) {
						return !objectObjectEntry.getKey().toString().equals("pass");
				}	
		});
		config.reload();		
		
		submissionService = new SubmissionService();
		submissionService.addConnectors(config);
	}
	
	private void initMessageBrokerAPI(String baseUrl, int timeout) {
		eu.portcdm.mb.client.ApiClient connectorClient = new eu.portcdm.mb.client.ApiClient();
		connectorClient.setBasePath(baseUrl);
		connectorClient.setConnectTimeout(timeout);
		messageBrokerAPI = new MessageQueueServiceApi(connectorClient);
	}
	
	private void initPortCallsAPI(String baseUrl, int timeout) {
		eu.portcdm.client.ApiClient connectorClient = new eu.portcdm.client.ApiClient();
		connectorClient.setBasePath(baseUrl);
		connectorClient.setConnectTimeout(timeout);
		connectorClient.addDefaultHeader("X-PortCDM-UserId", "porter");
        connectorClient.addDefaultHeader("X-PortCDM-Password", "porter");
        connectorClient.addDefaultHeader("X-PortCDM-APIKey", "pilot");
		portCallsAPI = new PortcallsApi(connectorClient);				
	}
	
	@FXML
	private ListView<Button> IDListView; 
	
	@FXML
	private Text idText, inkOrAvgText, bookTimeText, etaTimeText;
	
	@FXML 
	private GridPane gridPane1; 
	
	@FXML 
	private HBox hBoxRec1, hBoxRec2; 
	
	@FXML
	private AnchorPane AnchorPane1; 
	
	@FXML
	private ImageView imgViewInk, imgViewAvg; 
	
	
	
		
	
	/*public void calcButtonHandler(ActionEvent event) {
		
		double distance = Double.parseDouble(distTextField.getText());
		double speed = Double.parseDouble(speedTextField.getText());  
		int result = (int) calculateDistance(distance, speed);
		DateTimeFormatter formatter = DateTimeFormat.forPattern("HH:mm");
		
		LocalTime dt = new LocalTime();
		LocalTime klockslag = dt.plusMinutes(result);
		String tid = formatter.print(klockslag);


		resulttxt.setText("Anländer om " + Integer.toString(result/60) + ":" + Integer.toString(result%60) + " (h:min)" + "\n"
		+ "Klockslag: " + tid);
	}*/
	
	private double calculateDistance(double distance, double speed) {
		return distance/speed*60; // Tid=distans/hastighet*60 ex. 10M/12knop*60 = 50min
	}
}