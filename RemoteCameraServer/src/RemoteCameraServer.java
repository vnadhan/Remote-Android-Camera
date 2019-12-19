
/**
* RemoteCameraServer that extends WebSocketServer to control multiple cameras via a socket connection
*
* @author  Vishnu Raveendra Nadhan
* @version 1.0 
*/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.simple.JSONObject;

// Reusable Constants
class Constants {
	public static final String START_COMMAND = "start";
	public static final String STOP_COMMAND = "stop";
}

// Attributes for each camera.
class CameraAttributes {
	private String cameraId;
    private String storeId;
    private String command;
    private String delay;
    private String iso;
    private String exposureTime;
    private String jpeg_quality;
    private String upload_url;

    public CameraAttributes(String cameraId, String storeId, String command, String delay, String iso,
			String exposureTime, String jpeg_quality, String upload_url) {
		super();
		this.cameraId = cameraId;
		this.storeId = storeId;
		this.command = command;
		this.delay = delay;
		this.iso = iso;
		this.exposureTime = exposureTime;
		this.jpeg_quality = jpeg_quality;
		this.upload_url = upload_url;
	}

	public String getCommand() {
		return command;
	}

	public String getDelay() {
		return delay;
	}

	public String getCameraId() {
		return cameraId;
	}

	public String getStoreId() {
		return storeId;
	}

	public String getIso() {
		return iso;
	}

	public String getExposureTime() {
		return exposureTime;
	}

	public String getJpeg_quality() {
		return jpeg_quality;
	}

	public String getUpload_url() {
		return upload_url;
	}
}

public class RemoteCameraServer extends WebSocketServer {
	
	// Latch to wait for all cameras to connect to the server 
	static private CountDownLatch latch = null;
	
	// a list of known cameras and their associated properties
	static JSONObject connections = new JSONObject();
	
	// Logger init
	static Logger logger = Logger.getLogger(RemoteCameraServer.class.getName());
	
	// Not used.
	public RemoteCameraServer(int port) throws UnknownHostException {
		super(new InetSocketAddress(port));
	}
	
	// Constructor
	public RemoteCameraServer(InetSocketAddress address, String[] args) {
		super(address);
	}

	@SuppressWarnings("unchecked")
	// The main method
	public static void main(String[] args) throws IOException, InterruptedException {
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
		
		// Each arg in args corresponds to a camera
		// If no cameras, then dont start the websocketserver
		if (args.length > 0) {
			// Init
			String hostip, delay = "";
			String[] splits = null;
			JSONObject innerObj = null;;
			
			logger.info("Analying inputs");
			
			// For each arg i.e. each camera
			for (String string : args) {
				// New JSON
				innerObj = new JSONObject();
				// Split and get respective values.
				splits = string.split(";"); // hostname cameraId storeId command delay uploadURL iso exposureTime JPEG_Quality
				
				// Get HOST IP
				// hostname = uri.split(":")[0];
				hostip = splits[0];
				
				// No need of delay in the case of STOP Command
				if (splits[4].equalsIgnoreCase(Constants.START_COMMAND))
					delay = splits[4];
				else
					delay = "null";
				
				// socket connection with this camera is null in the beginning. once a connection is estabilished with this camera, this attribute will be updated with the socket object.
				innerObj.put("socket", null);
				
				// insert camera attribues. this will be shared to the camera via the socket connection.
				innerObj.put("cameraAttributes", new CameraAttributes(splits[1], splits[2], splits[3], delay, splits[5], splits[6], splits[7], splits[8]));
				
				// Insert a new host or update existing one
				connections.put(hostip, innerObj);
				
				logger.info(" -------------------------------------------------------------------------------- ");
			}
				
			// Latch size is == the number of camera / args
			latch = new CountDownLatch(args.length);
			
			// start the socketserver
			RemoteCameraServer server = new RemoteCameraServer(new InetSocketAddress("192.168.1.193", 27017), args);
			server.start();
			
			logger.info("RemoteCameraServer started on " + server.getAddress().getHostName() + ":" + server.getPort());
			
			// waiting for all clients/cameras to connect to this server.
			try {
				// keep waiting until latch count is 0
				// each camera that connects to the server will decrement the latch count.
				latch.await();
			} catch(InterruptedException e) {
				e.printStackTrace();
				logger.info(e.getMessage());
			}

			// Input Buffer
			// Not required??
			BufferedReader sysin = new BufferedReader(new InputStreamReader(System.in));
			while (true) {
				String in = sysin.readLine();
				if (in.equals("exit")) {
					server.stop(1000);
					break;
				}
			}
		} else {
			// E.g. java RemoteCameraClient [<mobile_ip>:<port>]*
			logger.info("Invalid arguments! ");
		}
	}

	@Override
	public void onClose(WebSocket arg0, int arg1, String arg2, boolean arg3) {
		logger.info("Socket connection closed! Message: " + arg1);
	}

	@Override
	public void onError(WebSocket arg0, Exception arg1) {
		logger.info("Socket connection error! Message: " + arg1);
	}

	@Override
	public void onMessage(WebSocket arg0, String arg1) {
		String host = getHostName(arg0.getRemoteSocketAddress());
		
		logger.info("Received a message from : " + host + ": "+ arg1);
		
		if(connections.get(host) != null && connections.get(host) != "") {
			if(arg1.equalsIgnoreCase("SUCCESSFUL_CONNECTION")) {
				CameraAttributes attrs = (CameraAttributes)((JSONObject)(connections.get(host))).get("cameraAttributes");
//				clickPhoto;cameraId;storeId;command;delay;iso;exposureTime;jpeg_quality;upload_url
				arg0.send("clickPhoto;" + attrs.getCameraId() + ";" + attrs.getStoreId() + ";" + attrs.getCommand() + ";" + attrs.getDelay() + ";" + attrs.getIso() + ";" + attrs.getExposureTime() + ";" + attrs.getJpeg_quality() + ";" + attrs.getUpload_url());
				logger.info("Sent a message to " + host);
			}
		} else {
			logger.info("Client " + host + " not found. Please add this host in the hosts list!!");
		}
		
		// Camera connected with the server, therefore, decrement latch count.
		if(latch.getCount() > 0)
			latch.countDown();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onOpen(WebSocket arg0, ClientHandshake arg1) {
		logger.info("Socket connection opened with host: " + getHostName(arg0.getRemoteSocketAddress()));
		logger.info("Message: " + arg1);
		
		String host = getHostName(arg0.getRemoteSocketAddress());
		JSONObject obj = (JSONObject) connections.get(host);
		
		obj.put("socket", arg0);
		connections.put(host, obj);
	}
	
	private String getHostName(InetSocketAddress address) {
		return address.getHostName();
	}
}
