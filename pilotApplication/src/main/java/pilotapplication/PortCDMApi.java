package pilotapplication;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import eu.portcdm.amss.client.StateupdateApi;
import eu.portcdm.client.ApiException;
import eu.portcdm.client.service.PortcallsApi;
import eu.portcdm.dto.LocationTimeSequence;
import eu.portcdm.dto.PortCall;
import eu.portcdm.dto.PortCallSummary;
import eu.portcdm.mb.client.MessageQueueServiceApi;
import eu.portcdm.mb.dto.Filter;
import eu.portcdm.messaging.LocationReferenceObject;
import eu.portcdm.messaging.LogicalLocation;
import eu.portcdm.messaging.PortCallMessage;
import eu.portcdm.messaging.ServiceObject;
import eu.portcdm.messaging.ServiceTimeSequence;
import eu.portcdm.messaging.TimeType;

import se.viktoria.stm.portcdm.connector.common.util.PortCallMessageBuilder;
import se.viktoria.stm.portcdm.connector.common.util.StateWrapper;

public class PortCDMApi {

	// Parameters for the external development machine
	private static final String DEV_BASE_URL = "http://dev.portcdm.eu:8080/";
	private static final String DEV_USERNAME = "viktoria";
	private static final String DEV_PASSWORD = "vik123"; 
	private static final String DEV_API_KEY = "pilot";
	private static final int DEV_TIMEOUT = 20000;
		
	// Parameters for the local virtual machine
	private static final String VM_BASE_URL = "http://192.168.56.101:8080/";
	private static final String VM_USERNAME = "porter";
	private static final String VM_PASSWORD = "porter"; // LOL security
	private static final String VM_API_KEY = "pilot";	
	private static final int VM_TIMEOUT = 7000;

	// Paths to PortCDMs different modules
	private static final String MESSAGE_BROKER_PATH = "mb";
	private static final String PORT_CDM_SERVICES_PATH = "dmp";
	private static final String PORT_CDM_AMSS_PATH = "amss";
	
	public MessageQueueServiceApi messageBrokerAPI;
	public PortcallsApi portCallsAPI;
	public StateupdateApi AMSSApi; 
		
	private final List<String> locationList = Arrays.asList(
			LogicalLocation.ANCHORING_AREA.toString(), 
			LogicalLocation.BERTH.toString(), 
			LogicalLocation.ETUG_ZONE.toString(), 
			LogicalLocation.TUG_ZONE.toString(),
			LogicalLocation.LOC.toString(), 
			LogicalLocation.PILOT_BOARDING_AREA.toString(), 
			LogicalLocation.RENDEZV_AREA.toString(), 
			LogicalLocation.TRAFFIC_AREA.toString(), 
			LogicalLocation.TUG_ZONE.toString(), 
			LogicalLocation.VESSEL.toString());

	/**
	 * Constructor for the PortCDMApi object
	 * 
	 * @param connectToExternalServer connect to external dev server if <code>true</code>, else connect to local vm
	 */
	public PortCDMApi(boolean connectToExternalServer) {
		if (connectToExternalServer) {
			initMessageBrokerAPI(DEV_BASE_URL + MESSAGE_BROKER_PATH, DEV_TIMEOUT, DEV_USERNAME, DEV_PASSWORD, DEV_API_KEY);
			initPortCallsAPI(DEV_BASE_URL + PORT_CDM_SERVICES_PATH, DEV_TIMEOUT, DEV_USERNAME, DEV_PASSWORD, DEV_API_KEY);
			initAMSSApi(DEV_BASE_URL + PORT_CDM_AMSS_PATH, DEV_TIMEOUT, DEV_USERNAME, DEV_PASSWORD, DEV_API_KEY);
		}
		else {
			initMessageBrokerAPI(VM_BASE_URL + MESSAGE_BROKER_PATH, VM_TIMEOUT, VM_USERNAME, VM_PASSWORD, VM_API_KEY);
			initPortCallsAPI(VM_BASE_URL + PORT_CDM_SERVICES_PATH, VM_TIMEOUT, VM_USERNAME, VM_PASSWORD, VM_API_KEY);
			initAMSSApi(VM_BASE_URL + PORT_CDM_AMSS_PATH, VM_TIMEOUT, VM_USERNAME, VM_PASSWORD, VM_API_KEY); 
		}	
	}

	/**
	 * Init API to portCDM message broker.
	 */
	private void initMessageBrokerAPI(String baseUrl, int timeout, String username, String password, String apikey) {
		eu.portcdm.mb.client.ApiClient connectorClient = new eu.portcdm.mb.client.ApiClient();
		connectorClient.setBasePath(baseUrl);
		connectorClient.setConnectTimeout(timeout);
		connectorClient.addDefaultHeader("X-PortCDM-UserId", username);
        connectorClient.addDefaultHeader("X-PortCDM-Password", password);
        connectorClient.addDefaultHeader("X-PortCDM-APIKey", apikey);
		messageBrokerAPI = new MessageQueueServiceApi(connectorClient);
	}

	/**
	 * Init API to portCDM port call manager.
	 */
	private void initPortCallsAPI(String baseUrl, int timeout, String username, String password, String apikey) {
		eu.portcdm.client.ApiClient connectorClient = new eu.portcdm.client.ApiClient();
		connectorClient.setBasePath(baseUrl);
		connectorClient.setConnectTimeout(timeout);
		connectorClient.addDefaultHeader("X-PortCDM-UserId", username);
        connectorClient.addDefaultHeader("X-PortCDM-Password", password);
        connectorClient.addDefaultHeader("X-PortCDM-APIKey", apikey);
		portCallsAPI = new PortcallsApi(connectorClient);				
	}

	/**
     *  Init API to portCDM assisted message submission service (AMSS)
     */
    private void initAMSSApi(String baseUrl, int timeout, String username, String password, String apikey) {
        eu.portcdm.amss.client.ApiClient connectorClient = new eu.portcdm.amss.client.ApiClient();
        connectorClient.setBasePath(baseUrl);    
        connectorClient.addDefaultHeader("X-PortCDM-UserId", username);
        connectorClient.addDefaultHeader("X-PortCDM-Password", password);
        connectorClient.addDefaultHeader("X-PortCDM-APIKey", apikey);
        AMSSApi = new StateupdateApi(connectorClient);
    }

	/**
	 * Get summary of portcalls from portCDM.
	 * 
	 * @param max maximum number of portcalls to fetch
	 * @return list of portcall summaries
	 */
	public List<PortCallSummary> getPortCalls(int max) {
		List<PortCallSummary> portcalls = null;
		try {
			portcalls = portCallsAPI.getAllPortCalls(max);
			System.out.println("Fetched " + portcalls.size() + " portcalls.");
		} 
		catch (ApiException e) {
			System.out.println("Couldn't fetch portcalls.");
			System.out.println(e.getCode() + " " + e.getMessage() + '\n' + e.getResponseBody());
		}
		return portcalls;
	}
	
	/**
	 * Fetch a specific portcall identified by a portcall id.
	 * 
	 * @param portCallId
	 * @return portcall fetched from portCDM
	 */
	public PortCall getPortCall(String portCallId) {
		PortCall portcall = null;
		try {
			portcall = portCallsAPI.getPortCall(portCallId);
		} catch (ApiException e) {
			System.out.println("Couldn't fetch portcall.");
			System.out.println(e.getCode() + " " + e.getMessage() + '\n' + e.getResponseBody());
		}
		return portcall;
	}

	/**
	 * Submit a portcall message to PortCDM. 
	 * 
	 * @param pcm portcall messages to be sent
	 */
	public void sendPortCallMessage(PortCallMessage pcm) {
		try {
            AMSSApi.sendMessage(pcm);
            String[] vesselId = pcm.getVesselId().split(":");
            int idLength = vesselId.length;
            String imo = vesselId[idLength - 1];
            System.out.println("Message successfully sent. (IMO: " + imo + ")");
        } 
        catch (eu.portcdm.amss.client.ApiException e) {
        	System.out.println("Couldn't send message.");
        	System.out.println(e.getCode() + " " + e.getMessage() + '\n' + e.getResponseBody());
        }
	}

	/**
	 * Post a queue with the given filters to portCDM.
	 * 
	 * @param filters list of filters that 
	 * @return id of the newly created queue
	 */
	public String postQueue(List<Filter> filters) {
		String queueId = null;
		try {
			queueId = messageBrokerAPI.mqsPost(filters);
			System.out.println("Created queue with id: " + queueId);
		} 
		catch (eu.portcdm.mb.client.ApiException e) {
			System.out.println("Couldn't post queue");
			System.out.println(e.getCode() + " " + e.getMessage() + '\n' + e.getResponseBody());
		}
		return queueId;
	}
	
	/**
	 * Let me present to you, *drumroll*, the ugliest code in the world! 
	 * 
	 * Fetch all messages from the queue with the given id.
	 * This method should only be used when communicating with the development server.
	 * 
	 * @param queueId id of the queue of interest
	 * @return all messages fetched from the queue
	 */
	public List<PortCallMessage> fetchMessagesFromDevQueue(String queueId) {
		List<PortCallMessage> requestList = new LinkedList<>();
		NodeList messages = null;
		try {
			URL url = new URL(DEV_BASE_URL + "mb/mqs/" + queueId);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/xml");
			conn.setRequestProperty("X-PortCDM-UserId", DEV_USERNAME);
			conn.setRequestProperty("X-PortCDM-Password", DEV_PASSWORD);
			conn.setRequestProperty("X-PortCDM-APIKey", DEV_API_KEY);
	
			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
						+ conn.getResponseCode() + " " + conn.getResponseMessage());
			}
			
			String xml;
			InputStreamReader ir = new InputStreamReader(conn.getInputStream(), "UTF-8");
			
			BufferedReader br = null;
			try {
				br = new BufferedReader(ir);	
				xml = br.readLine();
				if (xml == null) {
					return requestList;
				}
			}
			finally {
				if (br != null) {
					br.close();
				}
			}
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		    DocumentBuilder builder = factory.newDocumentBuilder();
		    InputSource is = new InputSource(new StringReader(xml));
		    Document doc = builder.parse(is);
		    
		    messages = doc.getFirstChild().getChildNodes();
		    for (int i = 0; i < messages.getLength(); i++) {
		    		
		    	NodeList messageElems = messages.item(i).getChildNodes();
		    	
		    	Node service = getElement("serviceState", messageElems);
		    	// We must be sure that this is a service request
		    	if (service == null) {
		    		continue;
		    	}
		    	
		    	Node serviceObject = getElement("serviceObject", service.getChildNodes());
		    	// Skip to the next message if this is not a pilotage request
		    	if (serviceObject == null || !serviceObject.getTextContent().equals("PILOTAGE")) {
					continue;
		    	}
		    	
		    	Node portCallNode = getElement("portCallId", messageElems);
		    	Node vesselNode = getElement("vesselId", messageElems); // Not all groups include a vesselId, which is problematic  	
		    	Node msgIdNode = getElement("messageId", messageElems);
		    	
		    	// We must make sure that no field that we require is missing
		    	if (portCallNode == null || vesselNode == null || msgIdNode == null) {
		    		continue;
		    	}
		    	
		    	String portCallId = portCallNode.getTextContent();
		    	String vesselId = vesselNode.getTextContent(); 
		    	String messageId = msgIdNode.getTextContent();
		    	
		    	Node timeNode = getElement("time", service.getChildNodes());
		    	Node timeTypeNode = getElement("timeType", service.getChildNodes());
		    	
		    	if (timeNode == null || timeTypeNode == null) {
		    		continue;
		    	}
		    	
		    	String timestamp = timeNode.getTextContent();
		    	String timeType = timeTypeNode.getTextContent();
		    	
		    	Node between = getElement("between", service.getChildNodes());
		    	
		    	Node to = getElement("to", between.getChildNodes()); 
		    	if (to == null) {
		    		continue;
		    	}
		    	
		    	String[] locationToWithPrefix = to.getTextContent().split(":");
		    	String locationToAsString = locationToWithPrefix[locationToWithPrefix.length - 1];
		    	
		    	// Since the location string might come with a name appended at the end (urn:mrn:stm:location:segot:BERTH:optionalName), the only way
		    	// to parse the logical location is to differ between the two cases.
		    	LogicalLocation locationTo = null;
		    	if (locationList.contains(locationToAsString)) {
		    		locationTo = LogicalLocation.valueOf(locationToAsString);
		    	}
		    	else {
		    		locationToAsString = locationToWithPrefix[locationToWithPrefix.length - 2];
		    		locationTo = LogicalLocation.valueOf(locationToAsString);
		    	}	    	
		    	
		    	Node from = getElement("from", between.getChildNodes());
		    	if (from == null) {
		    		continue;
		    	}
		    	
		    	String[] locationFromWithPrefix = from.getTextContent().split(":");
		    	String locationFromAsString = locationFromWithPrefix[locationFromWithPrefix.length - 1];
		    	
		    	LogicalLocation locationFrom = null;
		    	if (locationList.contains(locationFromAsString)) {
		    		locationFrom = LogicalLocation.valueOf(locationFromAsString);
		    	}
		    	else {
		    		locationFromAsString = locationFromWithPrefix[locationFromWithPrefix.length - 2];
		    		locationFrom = LogicalLocation.valueOf(locationFromAsString);
		    	}
		    	
		    	// Create portcall message and add additional attributes
		    	PortCallMessage pcm = createServiceMessage(vesselId, ServiceObject.PILOTAGE, ServiceTimeSequence.REQUESTED, locationTo, locationFrom, timestamp, TimeType.valueOf(timeType));
		    	pcm.setPortCallId(portCallId);
		    	pcm.setMessageId(messageId);
		    	requestList.add(pcm);
		    	
		    }
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
		    e.printStackTrace();
		}
		if (messages != null) {
			System.out.println("Received " + messages.getLength() + " message(s) from queue " + queueId);
		}
		return requestList;
	}
	
	private Node getElement(String elementName, NodeList elements) {
		for (int i = 0; i < elements.getLength(); i++) {
			Node element = elements.item(i);
			String[] nameSplit = element.getNodeName().split(":");
			String noPrefixName = nameSplit[nameSplit.length - 1];
			
			if (noPrefixName.equals(elementName)) {
				return element;
			}
		}
		return null;
	}

	/**
	 * Fetch all messages from the queue with the given id.
	 * 
	 * @param queueId id of the queue of interest
	 * @return all messages fetched from the queue
	 */
	public List<PortCallMessage> fetchMessagesFromQueue(String queueId) {
		List<PortCallMessage> messages = null;
		try {
			messages = messageBrokerAPI.mqsQueueGet(queueId);
			System.out.println("Received " + messages.size() + " message(s) from queue " + queueId);
		} 
		catch (eu.portcdm.mb.client.ApiException e) {
			System.out.println("Couldn't fetch messages.");
			System.out.println(e.getCode() + " " + e.getMessage() + '\n' + e.getResponseBody());
		} 
		return messages;
	}

    public PortCallMessage createLocationMessage(String vesselId, LocationReferenceObject referenceObject, LocationTimeSequence sequence, LogicalLocation location, String timestamp, TimeType timeType) {
        StateWrapper wrapper = new StateWrapper(referenceObject, sequence, location);
        return portCallMessageFromStateWrapper(vesselId, wrapper, timestamp, timeType);
    }
    
    public PortCallMessage createServiceMessage(String vesselId, ServiceObject service, ServiceTimeSequence sequence, LogicalLocation locationTo, LogicalLocation locationFrom, String timestamp, TimeType timeType) {
        StateWrapper wrapper = new StateWrapper(service, sequence, locationTo, locationFrom);
        return portCallMessageFromStateWrapper(vesselId, wrapper, timestamp, timeType);
    }
    
    /**
     * Create a portcall message from a state wrapper for a given vessel.
     * A timestamp is required with a TimeType describing it.  
     * After creation, the message will be given a unique identifier.
     * 
     * @param vesselId IMO (+prefix) of the vessel the message concerns
     * @param wrapper StateWrapper containing information we want to send
     * @param timestamp timestamp in the format: "yyyy-MM-dd'T'HH:mm:ss'Z'"
     * @param timeType timestamp type, e.g. ACTUAL, ESTIMATE etc.
     * @return the newly created portcall message
     */	
    public PortCallMessage portCallMessageFromStateWrapper(String vesselId, StateWrapper wrapper, String timestamp, TimeType timeType) {
    	PortCallMessage pcm = PortCallMessageBuilder.build(
    			null, //localPortCallId
    			null, //localJobId
                wrapper, //StateWrapper created above
                timestamp, //Message's time
                timeType, //Message's timeType
                vesselId, //vesselId
                null, //reportedAt (optional)
                null, //reportedBy (optional)
                null, //groupWith (optional), messageId of the message to group with.
                null //comment (optional)
        );
    	pcm.setMessageId("urn:mrn:stm:portcdm:message:" + UUID.randomUUID().toString());
    	return pcm;
    }
}
