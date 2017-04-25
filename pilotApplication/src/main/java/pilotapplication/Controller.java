package pilotapplication;

import java.net.URL;
import java.util.ResourceBundle;

import org.jboss.as.controller.client.helpers.standalone.ServerUpdateActionResult.Result;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

public class Controller implements Initializable
{

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		// TODO Auto-generated method stub
		
	}
	
	@FXML
	private Button calcButton;
	
	@FXML
	private Label distLabel, speedLabel, resultLabel;
	
	@FXML
	private Text resultText;
	
	@FXML
	private TextField distTextField, speedTextField;
	
	public void buttonHandler(ActionEvent event)
	{
		double distance = Double.parseDouble(distTextField.getText());
		double speed = Double.parseDouble(speedTextField.getText())*1852; //1 knop=1.852km/h = 1852m/h
		double result = calculateDistance(distance, speed);
		resultText.setText("Anländer om " + Double.toString(result) + "h");
	}
	
	private double calculateDistance(double distance, double speed){
		return distance/speed;
	}
	
	
}