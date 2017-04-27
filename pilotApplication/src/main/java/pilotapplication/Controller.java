package pilotapplication;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Predicate;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;

import eu.portcdm.mb.client.MessageQueueServiceApi;

import se.viktoria.stm.portcdm.connector.common.SubmissionService;
import se.viktoria.util.Configuration;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {		
		initSubmissionService(CONFIG_FILE_NAME, CONFIG_FILE_DIR);	
		initMessageBrokerAPI(BASE_URL_VM + "mb", 20000);
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
	
	@FXML
	private Button calcButton;
	
	@FXML
	private Label distLabel, speedLabel, resultLabel;
	
	@FXML
	private Text resultText;
	
	@FXML
	private TextField distTextField, speedTextField;
	
	public void buttonHandler(ActionEvent event) {
		double distance = Double.parseDouble(distTextField.getText());
		double speed = Double.parseDouble(speedTextField.getText()); 
		int result = (int) calculateDistance(distance, speed);
		
		LocalTime dt = new LocalTime();
		LocalTime klockslag = dt.plusMinutes(result);
		
		
		resultText.setText("Anländer om " + Integer.toString(result/60) + ":" + Integer.toString(result%60) + " (h:min)" + "\n"
		+ "Klockslag: " + klockslag);
		
		 
	}
	
	private double calculateDistance(double distance, double speed) {
		return distance/speed*60; // Tid=distans/hastighet*60 ex. 10M/12knop*60 = 50min
	}
	
	
}