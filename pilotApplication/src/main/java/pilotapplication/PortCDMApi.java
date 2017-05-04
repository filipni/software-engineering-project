package pilotapplication;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import eu.portcdm.client.service.PortcallsApi;
import eu.portcdm.dto.LocationTimeSequence;
import eu.portcdm.mb.client.MessageQueueServiceApi;
import eu.portcdm.messaging.LocationReferenceObject;
import eu.portcdm.messaging.LogicalLocation;
import eu.portcdm.messaging.PortCallMessage;
import eu.portcdm.messaging.TimeType;
import se.viktoria.stm.portcdm.connector.common.SubmissionService;
import se.viktoria.stm.portcdm.connector.common.util.PortCallMessageBuilder;
import se.viktoria.stm.portcdm.connector.common.util.StateWrapper;
import se.viktoria.util.Configuration;

public class PortCDMApi {
	
	// Parameters for the external development machine
	private final String DEV_BASE_URL = "http://dev.portcdm.eu:8080/";
	private final String DEV_USERNAME = "viktoria";
	private final String DEV_PASSWORD = "vik123"; 
	private final String DEV_API_KEY = "pilot";
	private final int DEV_TIMEOUT = 20000;
			
	// Parameters for the local virtual machine
	private final String VM_BASE_URL = "http://192.168.56.101:8080/";
	private final String VM_USERNAME = "porter";
	private final String VM_PASSWORD = "porter"; // LOL security
	private final String VM_API_KEY = "pilot";	
	private final int VM_TIMEOUT = 7000;
	
	// Paths to PortCDMs different modules
	private final String MESSAGE_BROKER_PATH = "mb";
	private final String PORT_CDM_SERVICES_PATH = "dmp";
	
	public  final String CONFIG_FILE_NAME = "portcdm.conf";
	public  final String CONFIG_FILE_DIR = "portcdm";
		
	public SubmissionService submissionService;
	public MessageQueueServiceApi messageBrokerAPI;
	public PortcallsApi portCallsAPI;
	
	public PortCDMApi(boolean connectToExternalServer) {
		if (connectToExternalServer) {
			initSubmissionService(CONFIG_FILE_NAME, CONFIG_FILE_DIR);
			initMessageBrokerAPI(DEV_BASE_URL + MESSAGE_BROKER_PATH, DEV_TIMEOUT);
			initPortCallsAPI(DEV_BASE_URL + PORT_CDM_SERVICES_PATH, DEV_TIMEOUT, DEV_USERNAME, DEV_PASSWORD, DEV_API_KEY);
		}
		else {
			initSubmissionService(CONFIG_FILE_NAME, CONFIG_FILE_DIR);
			initMessageBrokerAPI(VM_BASE_URL + MESSAGE_BROKER_PATH, VM_TIMEOUT);
			initPortCallsAPI(VM_BASE_URL + PORT_CDM_SERVICES_PATH, VM_TIMEOUT, VM_USERNAME, VM_PASSWORD, VM_API_KEY);
		}	
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
	
	private void initPortCallsAPI(String baseUrl, int timeout, String username, String password, String apikey) {
		eu.portcdm.client.ApiClient connectorClient = new eu.portcdm.client.ApiClient();
		connectorClient.setBasePath(baseUrl);
		connectorClient.setConnectTimeout(timeout);
		connectorClient.addDefaultHeader("X-PortCDM-UserId", username);
        connectorClient.addDefaultHeader("X-PortCDM-Password", password);
        connectorClient.addDefaultHeader("X-PortCDM-APIKey", apikey);
		portCallsAPI = new PortcallsApi(connectorClient);				
	}
	
	public void sendPortCallMessages(List<PortCallMessage> portCallMessages) {
		submissionService.submitUpdates(portCallMessages);
	}
	
	/**
	 * Returns a portcall message that can be used for testing.
	 * 
	 * @return bogus portcall message
	 */
    public PortCallMessage getExampleMessage() {
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
}
