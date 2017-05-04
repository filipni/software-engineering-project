package pilotapplication;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Predicate;

import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import eu.portcdm.mb.client.MessageQueueServiceApi;
import eu.portcdm.client.ApiException;
import eu.portcdm.client.service.PortcallsApi;
import eu.portcdm.dto.PortCallSummary;

import se.viktoria.stm.portcdm.connector.common.SubmissionService;
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
	
	class ButtonHandler implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent  arg0) {
			System.out.println("afdsadsf"); 
		}
	}
	
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
			b.setOnAction(new ButtonHandler());
		};
		
		IDListView.setItems(buttons);
		
		}
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
	private ImageView img; 
	
	/*@FXML
	private Button Button1, Button2, Button3;*/
	
	@FXML
	private TextField distTextField, speedTextField;
	
	@FXML
	private Rectangle rectangle1, rectangle2;
	
	@FXML
	private Text resulttxt; 
	
	@FXML 
	private GridPane gridPane1; 
	
	@FXML 
	private HBox hBoxRec1, hBoxRec2; 
	
	
	public void incButtonHandler(ActionEvent event) {
		
		distTextField.setPromptText("Distans i sjömil");
		speedTextField.setPromptText("Hastighet i knop");
		
		gridPane1.setVisible(true);
		hBoxRec1.setVisible(true);
		
		hBoxRec2.setVisible(false);
	}
	
	public void depButtonHandler(ActionEvent event) {
		
		hBoxRec2.setVisible(true);
		
		gridPane1.setVisible(false);
		hBoxRec1.setVisible(false);
		
		progressBar(12); 
		
		
	}	
	
	public void calcButtonHandler(ActionEvent event) {
		
		double distance = Double.parseDouble(distTextField.getText());
		double speed = Double.parseDouble(speedTextField.getText());  
		int result = (int) calculateDistance(distance, speed);
		DateTimeFormatter formatter = DateTimeFormat.forPattern("HH:mm");
		
		LocalTime dt = new LocalTime();
		LocalTime klockslag = dt.plusMinutes(result);
		String tid = formatter.print(klockslag);


		resulttxt.setText("Anländer om " + Integer.toString(result/60) + ":" + Integer.toString(result%60) + " (h:min)" + "\n"
		+ "Klockslag: " + tid);
	}
	
	private double calculateDistance(double distance, double speed) {
		return distance/speed*60; // Tid=distans/hastighet*60 ex. 10M/12knop*60 = 50min
	}
}