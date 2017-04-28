package pilotapplication;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Predicate;

import org.joda.time.LocalTime;

import eu.portcdm.mb.client.MessageQueueServiceApi;
import eu.portcdm.client.ApiException;
import eu.portcdm.client.service.PortcallsApi;
import eu.portcdm.dto.PortCallSummary;

import se.viktoria.stm.portcdm.connector.common.SubmissionService;
import se.viktoria.util.Configuration;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {		
		initSubmissionService(CONFIG_FILE_NAME, CONFIG_FILE_DIR);	
		initMessageBrokerAPI(BASE_URL_VM + "mb", 20000);
		initPortCallsAPI(BASE_URL_VM + "dmp", 20000);
		
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
	private ImageView img; 
	/*@FXML
	private Button incButton, calcButton;*/
	
	/*@FXML
	private Label distLabel; */
	
	//@FXML
	//private Text resultText;
	
	/*@FXML
	private TextField distTextField, speedTextField;*/
	
	/*@FXML
	private Rectangle rectangle1;*/  
	
	/*public void incButtonHandler(ActionEvent event) {
		
		distTextField.setOpacity(1);	
		speedTextField.setOpacity(1); 
		calcButton.setOpacity(1); 
		rectangle1.setOpacity(1);
	}*/
	
	/*public void calcButtonHandler(ActionEvent event) {
		resultText.setOpacity(1);
		
		double distance = Double.parseDouble(distTextField.getText());
		double speed = Double.parseDouble(speedTextField.getText());  
		int result = (int) calculateDistance(distance, speed);
		
		LocalTime dt = new LocalTime();
		LocalTime klockslag = dt.plusMinutes(result);
				
		resultText.setText("Anländer om " + Integer.toString(result/60) + ":" + Integer.toString(result%60) + " (h:min)" + "\n" + "Klockslag: " + klockslag);
	}*/
	
	private double calculateDistance(double distance, double speed) {
		return distance/speed*60; // Tid=distans/hastighet*60 ex. 10M/12knop*60 = 50min
	}
	
	
}