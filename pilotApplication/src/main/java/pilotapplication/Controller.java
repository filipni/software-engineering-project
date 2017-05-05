package pilotapplication;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import eu.portcdm.dto.PortCallSummary;
import eu.portcdm.messaging.PortCallMessage;

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
	private AnchorPane vesselInfoPane; 
	
	@FXML
	private ImageView statusImg, vesselImg; 
	
	private ArrayList <String> IDs = new ArrayList<>(Arrays.asList("IMO:9398917", "IMO:9371878", "IMO:9299707", "IMO:9425356", "IMO:9186728", "IMO:9057173", "IMO:9247168"));
	private PortCDMApi portcdmApi;
	private Map<String, PortCallSummary> portCallTable;
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {	
		boolean useDevServer = true;
		portcdmApi = new PortCDMApi(useDevServer);
		portCallTable = createPortCallTable(10);
		populateIdList();
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
		
		// Get the portcall summary corresponding to the list element,
		// and update the information in the vesselInfoPane. 
		PortCallSummary summary = portCallTable.get(id);
		bookTimeText.setText(summary.getCreatedAt().substring(10));
		etaTimeText.setText(summary.getLastUpdate().substring(10));
		
		// Download the image of the vessel and adjust its size
		Image vesselPhoto = PortCallSummaryUtils.downloadVesselImage(summary);
		vesselImg.setImage(vesselPhoto);
		vesselImg.setFitWidth(300);
		vesselImg.setFitHeight(200);
		
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