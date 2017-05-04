package pilotapplication;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import eu.portcdm.messaging.PortCallMessage;

public class Controller implements Initializable {	
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
	
	private ArrayList <String> IDs = new ArrayList<>(Arrays.asList("IMO:9398917", "IMO:9371878", "IMO:9299707", "IMO:9425356", "IMO:9186728", "IMO:9057173", "IMO:9247168"));
	private PortCDMApi portcdmApi;
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {	
		boolean useDevServer = true;
		portcdmApi = new PortCDMApi(useDevServer);
		getAndSend();
		
		ObservableList<Button> buttons = FXCollections.observableArrayList();
		for (int i = 0; i < IDs.size(); i++) {
			Button b = new Button();
			b.setText(IDs.get(i));
			buttons.add(b);
			b.setOnAction((event) -> { popWindow(b); });	
		}
		IDListView.setItems(buttons);	
	}
	
	/**
	 * DOCUMENTATION HERE PLEASE!
	 * 
	 * @param b description of parameter
	 */
	private void popWindow(Button b) {
		String id = b.getText(); 
		AnchorPane1.setVisible(true);
		idText.setText(id);
		bookTimeText.setText("17:25 23-april"); //TODO: make interactive
		etaTimeText.setText("03:12 24-april");
		
		if (id.equals("IMO:9371878")) { //TODO: change the image in some other way
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
	
	/**
	 * Simple method for testing the api to portcdm
	 */
	private void getAndSend() {		
		String portcallId = portcdmApi.getPortCalls(1).get(0).getId();
		
		PortCallMessage pcm = portcdmApi.getExampleMessage();
		pcm.setPortCallId(portcallId);	
		
		portcdmApi.sendPortCallMessages(Arrays.asList(pcm));
	}
		
}