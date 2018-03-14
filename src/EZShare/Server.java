package EZShare;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import  java.util.HashMap;

import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Server {
		
	
	// Declare the port number
	static int port;
	static int sport;
	//static String hostname;
	// Identifies the user number connected
	static String advertisedHostName;
	private static int connectionIntervalLimit;
	private static int exchangeInterval;
	private static int counter = 0;
	static boolean debug = false;
	private static final Logger log = Logger.getLogger(Logger.class);
	public static  ArrayList< KeyTuple> resourceList=new ArrayList<KeyTuple>();
	static String secret = null;
	static ArrayList<String> serverRecords=new ArrayList<String>();
	static ArrayList<String> secureServerRecords=new ArrayList<String>();
	static ArrayList<String> subscribeID=new ArrayList<String>();
	static HashMap subscribedItem=new HashMap();
	static HashMap<String, ArrayList> relayedSUB=new HashMap();

	private static void setSSLFactories(InputStream keyStream, String keyStorePassword, 
		    InputStream trustStream) throws Exception
		{    
		  // Get keyStore
		  KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());    

		  // if your store is password protected then declare it (it can be null however)
		  char[] keyPassword = keyStorePassword.toCharArray();

		  // load the stream to your store
		  keyStore.load(keyStream, keyPassword);

		  // initialize a trust manager factory with the trusted store
		  KeyManagerFactory keyFactory = 
		  KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());    
		  keyFactory.init(keyStore, keyPassword);

		  // get the trust managers from the factory
		  KeyManager[] keyManagers = keyFactory.getKeyManagers();

		  // Now get trustStore
		  KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());    

		  // if your store is password protected then declare it (it can be null however)
		  //char[] trustPassword = password.toCharArray();

		  // load the stream to your store
		  trustStore.load(trustStream, null);

		  // initialize a trust manager factory with the trusted store
		  TrustManagerFactory trustFactory = 
		  TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());    
		  trustFactory.init(trustStore);

		  // get the trust managers from the factory
		  TrustManager[] trustManagers = trustFactory.getTrustManagers();

		  // initialize an ssl context to use these managers and set as default
		  SSLContext sslContext = SSLContext.getInstance("SSL");
		  sslContext.init(keyManagers, trustManagers, null);
		  SSLContext.setDefault(sslContext);    
		}
	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws ParseException, org.apache.commons.cli.ParseException, FileNotFoundException {
			// Parse CMD options
			//
		InputStream keystoreInput = Server.class
                .getResourceAsStream("/server/server");
        InputStream truststoreInput = Server.class
                .getResourceAsStream("/server/server");
        try {
			setSSLFactories(keystoreInput, "vangogh", truststoreInput); 
			keystoreInput.close();
        truststoreInput.close();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
//		InputStream keyStoreStream =new FileInputStream("server.jks");
//		InputStream trustStoreStream =new FileInputStream("server.jks");
		Options options = new Options();
		AddOptions(options);
		// accept args from CMD
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;

		try{
			cmd = parser.parse(options, args);
		}catch(Exception e){
			System.out.println("Command is invalid or not found. \nPlease check your command and try again.");
			System.exit(0);
		}
			
		try {
			if (cmd.hasOption("sport")) {
				if (Math.isPort(cmd.getOptionValue("sport"))) {
					sport = Integer.parseInt(cmd.getOptionValue("sport"));
				} else {
					System.out.println("Please provide valid port");
					System.exit(0);
				}
			} else {
				sport = 3781;
			}
		} catch (Exception e) {
			sport = 3781;
		}
			
		try{
			if(Math.isPort(cmd.getOptionValue("port")))
				port = Integer.parseInt(cmd.getOptionValue("port"));
			else{
				System.out.println("Please provide valid port");
				System.exit(0);
			}
		}catch(Exception e){
			port = 3000;
		}
			
		if(cmd.hasOption("connectionintervallimit")){
			try{
				Server.connectionIntervalLimit=Integer.parseInt(cmd.getOptionValue("connectionintervallimit"));
				if(Integer.parseInt(cmd.getOptionValue("connectionintervallimit"))<0){
					System.out.println("Please provide valid connection interval limit( positive integer) arg.");
					System.exit(0);
				}
			}catch(Exception e){
				System.out.println("Please provide valid connection interval limit( positive integer) arg.");
				System.exit(0);
			}
		}else Server.connectionIntervalLimit=1;
		
		if(cmd.hasOption("exchangeinterval")){
			try{
				Server.exchangeInterval=Integer.parseInt(cmd.getOptionValue("exchangeinterval"));
				if(Integer.parseInt(cmd.getOptionValue("exchangeinterval"))<0){
					System.out.println("Please provide valid exchange interval( positive integer) arg.");
					System.exit(0);
				}
			}catch(Exception e){
				System.out.println("Please provide valid exchange interval( postive integer) arg.");
				System.exit(0);
			}
		}else Server.exchangeInterval=600;
		
		if(cmd.hasOption("secret")){
			try{
				Server.secret=cmd.getOptionValue("secret");
			}catch(Exception e){
				System.out.println("Please provide valid secret(String).");
				System.exit(0);
			}
		}else {
			Random rand = new Random();
			Server.secret=getRandomString(rand.nextInt(10)+20);
		}
		if(cmd.hasOption("advertisedhostname")){
			try{
				Server.advertisedHostName=cmd.getOptionValue("advertisedhostname");
			}catch(Exception e){
				System.out.println("Please provide valid advertised hostname(String).");
				System.exit(0);
			}
		}else {
			InetAddress gethost;
			try {
				gethost = InetAddress.getLocalHost();
				Server.advertisedHostName=gethost.getHostName();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				System.out.println("Fail to get hostname of OS.\nTry to provide an advertised hostname manually.");
			} 
		}
		
		if(cmd.hasOption("debug")) {
			Server.debug=true;
		}	
		else debug=false;

		
////////////////////////start to work with initial settings/////////////////////////////
////////////////////////from here we create 2 threads//////////
		
		ScheduledExecutorService twoThread = Executors.newScheduledThreadPool(2);
		
		twoThread.execute(() -> secureThreadGo());
		//twoThread.execute(() -> insecureThreadGo());

		twoThread.schedule(() -> insecureThreadGo(), 500, TimeUnit.MILLISECONDS);
		
	}
	
	
	private static void secureThreadGo() {
		
	    try{
	    	SSLServerSocketFactory sslserversocketfactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
			SSLServerSocket sslserversocket = (SSLServerSocket) sslserversocketfactory.createServerSocket(sport);
			sslserversocket.setNeedClientAuth(true);
			
			log.info("Starting the EZshare Server, opening secure port");
			log.info("using secret: "+Server.secret);
			log.info("using advertiesd hostname: "+advertisedHostName);
			log.info("bound to port "+sport);
			// debugging
			log.info("secure port opened");
			log.info("started");
			
			// This is the thread for exchanging server records between servers
			//***********************
			ScheduledExecutorService secureExecutor = Executors.newScheduledThreadPool(2);
			
			// start to exchange servers
			
			secureExecutor.scheduleAtFixedRate(() -> secureCommunication(), 5, exchangeInterval, TimeUnit.SECONDS);
			//secureExecutor.execute(() -> SSLExchangeServer());
			
			//**********************
			// Wait for connections.
			boolean connected = false;
			long timeLimit = System.currentTimeMillis() + connectionIntervalLimit*1000;
			
			while(true){
				
				//accept connection from client and creat a sslsocket
				SSLSocket sslclient = (SSLSocket) sslserversocket.accept();
				
				if (System.currentTimeMillis() < timeLimit && connected) {
					continue;
				}
				
				// debugging
				counter++;
				//("Secure Server: Client "+counter+": Applying for connection!");
				
				// Start a new thread for a connection
				secureExecutor.execute(() ->serveSSLClient(sslclient));
				
				connected = true;
				timeLimit = System.currentTimeMillis() + connectionIntervalLimit*1000;
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void SSLExchangeServer()  {
		// Creates a socket for another server, the socket that will send msg to

		Timer timer = new Timer();
		TimerTask task = new TimerTask() {

		public void run() {
			// debugging
			String serverName = "Secure Server";
			int serverListSize = secureServerRecords.size();
			//("1: " + serverName + " has server records of size: " + serverListSize);
			
			String secureList = "";
			for (int k = 0; k < serverListSize; k++) {
				secureList += secureServerRecords.get(k) + ", ";
			}		
			//("2: " + serverName + " has serverList:" + secureList);
			
			if (secureServerRecords.size() == 0) {
				// debugging
				//("3: " + serverName + " : No servers to excahnge");	
			} else {
				
				
				// Randomly select
				int selectedIndex = (new Random()).nextInt(secureServerRecords.size());
				
				String host_ip = secureServerRecords.get(selectedIndex);
				// debugging
				//("4: " + serverName + " selected server: " + host_ip);
				
				String[] host_ip_arr = host_ip.split(":");
				String host_name = host_ip_arr[0];
				int ip_add = Integer.parseInt(host_ip_arr[1]);
				
				JSONObject exchangeCommand = new JSONObject();
				String records = "";
				for (int i = 0; i<secureServerRecords.size(); i++) {
					records += secureServerRecords.get(i) + ",";
				}
				try {
					// add local address
					records += InetAddress.getLocalHost().getHostAddress() + ":" + sport;
				} catch (UnknownHostException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				exchangeCommand.put("command", "EXCHANGE");
				exchangeCommand.put("serverList", records);
				
				try(Socket randomServer = new Socket(host_name, ip_add)){
					DataInputStream input = new DataInputStream(randomServer.getInputStream());
					DataOutputStream output = new DataOutputStream(randomServer.getOutputStream());
				
					output.writeUTF(exchangeCommand.toJSONString());
					output.flush();
					
					//System.out.println("Command sent");
					
					// Time limit for execution
					long start = System.currentTimeMillis();
					long end = start + 5 * 1000;
					boolean isReachable = false;
					while(System.currentTimeMillis() < end) {
						if (input.available() > 0) {
							isReachable = true;
							String result = input.readUTF();
							// debugging
							//("5:" + serverName + " : Received from other server: " + result);
						}
					}
					if (!isReachable) {
						secureServerRecords.remove(selectedIndex);
						// debugging
						//("6: " + serverName + ": Removed unreachable server: " + host_ip);
					}
					
				} catch (IOException e) {
					//e.printStackTrace();
					
					secureServerRecords.remove(selectedIndex);
					// debugging
					//("7: " + serverName + ": Removed unreachable server: " + host_ip);
				}
			}
			}
			
		};
		
		timer.schedule(task, 0, exchangeInterval * 1000);
	}
	
	
	private static void insecureThreadGo() {
		ServerSocketFactory factory = ServerSocketFactory.getDefault();
		try(ServerSocket server = factory.createServerSocket(port)){
			log.info("Starting the EZshare Server, opening insecure port");
			log.info("using secret: "+Server.secret);
			log.info("using advertiesd hostname: "+advertisedHostName);
			log.info("bound to port "+port);
			// debugging
			log.info("insecure port opened");
			log.info("started");
			
			// This is the thread for exchanging server records between servers
			//***********************
			
			ScheduledExecutorService secureExecutor = Executors.newScheduledThreadPool(2);
			
			secureExecutor.scheduleAtFixedRate(() -> insecureCommunication(), 5, exchangeInterval, TimeUnit.SECONDS);
			//secureExecutor.execute(()-> ExchangeServer());
			
			//**********************
			// Wait for connections.
			boolean connected = false;
			long timeLimit = System.currentTimeMillis() + connectionIntervalLimit*1000;
			
			while(true){
				Socket client = server.accept();
				
				if (System.currentTimeMillis() < timeLimit && connected) {
					continue;
				}
				
				// debugging
				counter++;
				//("Insecure Sever : Client "+counter+": Applying for connection!");
				// Start a new thread for a connection
				
				secureExecutor.execute(() ->serveClient(client));

				connected = true;
				timeLimit = System.currentTimeMillis() + connectionIntervalLimit*1000;
			}				
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void ExchangeServer() {
		
		// Creates a socket for another server, the socket that will send msg to
		Timer timer = new Timer();
		TimerTask task = new TimerTask() {

			public void run() {
				String serverName = "Insecure Server";
				int serverListSize = serverRecords.size();
				//("1: " + serverName + " has server records of size: " + serverListSize);
				String secureList = "";
				for (int k = 0; k < serverListSize; k++) {
					secureList += serverRecords.get(k);
				}
				//("2: " + serverName + " has serverList:" + secureList);	
				if (serverRecords.size() == 0) {
					//("3: " + serverName + " : No servers to excahnge");
				} else {
					//randomly select 1
					
					int selectedIndex = (new Random()).nextInt(serverRecords.size());	
					String host_ip = serverRecords.get(selectedIndex);
					// debugging
					//("4: " + serverName + "selected server: " + host_ip);
					String[] host_ip_arr = host_ip.split(":");
					String host_name = host_ip_arr[0];
					int ip_add = Integer.parseInt(host_ip_arr[1]);
					JSONObject exchangeCommand = new JSONObject();
					String records = "";
					for (int i = 0; i<serverRecords.size(); i++) {
						records += serverRecords.get(i) + ",";
					}
					try {
						records += InetAddress.getLocalHost().getHostAddress() + ":" + port;
					} catch (UnknownHostException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					exchangeCommand.put("command", "EXCHANGE");
					exchangeCommand.put("serverList", records);
					
					try(Socket randomServer = new Socket(host_name, ip_add)){
						DataInputStream input = new DataInputStream(randomServer.getInputStream());
						DataOutputStream output = new DataOutputStream(randomServer.getOutputStream());
					
						output.writeUTF(exchangeCommand.toJSONString());
						output.flush();
						
						//System.out.println("Command sent");
						
						// Time limit for execution
						long start = System.currentTimeMillis();
						long end = start + 5 * 1000;
						boolean isReachable = false;
						while(System.currentTimeMillis() < end) {
							if (input.available() > 0) {
								isReachable = true;
								String result = input.readUTF();
								//(serverName + " : Received from other server: " + result);
							}
						}
						if (!isReachable) {
							serverRecords.remove(selectedIndex);
							// debugging
							//("6: " + serverName + ": Removed unreachable server: " + host_ip);
						}
						
					} catch (IOException e) {
						//e.printStackTrace();
						serverRecords.remove(selectedIndex);
						// debugging
						//("7: " + serverName + " removed unreachable server: " + host_ip);
					}
				}
			}
			
		};
		
		timer.schedule(task, 0, exchangeInterval * 1000);
	}
	
	private static void serveSSLClient(SSLSocket sslclient) {
		try(SSLSocket clientSocket = sslclient){
			
			// The JSON Parser
			JSONParser parser = new JSONParser();
			// Input stream
			DataInputStream input = new DataInputStream(clientSocket.
					getInputStream());
			// Output Stream
		    DataOutputStream output = new DataOutputStream(clientSocket.
		    		getOutputStream());
//			    //("CLIENT: "+input.readUTF());
//			    output.writeUTF("Server: Hi Client "+counter+" !!!");
		    
		    // debugging
		    String serverName;
		    serverName = "secure port ";
			// Bge
		    boolean end=false;
		    boolean subscribeContinue=false;
		    
		    while(true){
		    	String message;
		    	if((message=input.readUTF())!=null){
		    		// Attempt to convert read data to JSON
		    		JSONObject command = (JSONObject) parser.parse(message);
		    		if(debug){
		    			log.debug(serverName + "RECIEVED: "+command.toJSONString());
		    		}
		    		
		    		// Bge
		    		int category=0;
		    		if(command.containsKey("command")){
		    			switch((String)command.get("command")){
		    			case "PUBLISH":
		    			case "EXCHANGE":
		    			case "SHARE":
		    			case "REMOVE": category=1;break;
		    			case "QUERY":category=2;break;
		    			case "FETCH":category=3;break;
		    			case "SUBSCRIBE": category=4;break;
		    			case "UNSUBSCRIBE": category=5;break;
		    			default: break;
		    			}
		    		}	
		    		
		    		// start to check which thread it belongs to
		    		JSONArray result;
		    		
			    	result = SSLMath.parseCommand(command, output);
		    		
			    	
			    	// Bge
			    	JSONObject subCommand=null;
			    	
		    		for(int i=0;i<result.size();i++){
			    		if(!((JSONObject)result.get(i)).containsKey("endOfTransmit")){
			    			JSONObject temp=(JSONObject) result.get(i);
			    			if(temp.containsKey("response")&&
			    					temp.get("response").equals("success")&&
			    					temp.containsKey("id")){
			    				subCommand=temp;
			    				
			    				subscribeContinue=true;
			    				//("INTO CONTINUE:"+subscribeContinue);
			    			}
			    			output.writeUTF(((JSONObject)result.get(i)).toJSONString());
			    			output.flush();	
			    			if(debug)log.debug(serverName + "SENT: "+((JSONObject) result.get(i)).toJSONString());
			    		}
			    		else if(category==0 ||category==1||category==5){
			    			end=true;
			    			}else if(category==4&&subscribeContinue){
			    				//Thread !!!!!
			    				//("INTO if!!"+"continue:"+subscribeContinue);
			    				Thread Subscribe = new Thread(() -> {
			    					try {
			    						//("INTO Thread Subscribe!!");
			    						doSubscribe(output,command,false);
			    					} catch (UnknownHostException e) {
			    						// TODO Auto-generated catch block
			    						e.printStackTrace();
			    					} catch (IOException e) {
			    						// TODO Auto-generated catch block
			    						e.printStackTrace();
			    					}
			    				});
			    				Subscribe.start();
			    			}
			    		}
		    		
		    		
		    		if(end) break;
		    		
		    	}
		    }
		    
		    output.close();
    		input.close();
		    
		} catch (IOException e) {
			
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
	}
	
	private static void serveClient(Socket client) {
		try(Socket clientSocket = client){
			
			// The JSON Parser
			JSONParser parser = new JSONParser();
			// Input stream
			DataInputStream input = new DataInputStream(clientSocket.
					getInputStream());
			// Output Stream
		    DataOutputStream output = new DataOutputStream(clientSocket.
		    		getOutputStream());
//			    System.out.println("CLIENT: "+input.readUTF());
//			    output.writeUTF("Server: Hi Client "+counter+" !!!");
		    
		    // debugging
		    String serverName;
			serverName = "insecure port ";
			
			boolean end=false;
		    boolean subscribeContinue=false;
			
		    while(true){
		    	if(input.available() > 0){
		    		// Attempt to convert read data to JSON
		    		JSONObject command = (JSONObject) parser.parse(input.readUTF());
		    		if(debug){
		    			log.debug(serverName + "RECIEVED: "+command.toJSONString());
		    		}
		    		
		    		// Bge
		    		int category=0;
		    		if(command.containsKey("command")){
		    			switch((String)command.get("command")){
		    			case "PUBLISH":
		    			case "EXCHANGE":
		    			case "SHARE":
		    			case "REMOVE": category=1;break;
		    			case "QUERY":category=2;break;
		    			case "FETCH":category=3;break;
		    			case "SUBSCRIBE": category=4;break;
		    			case "UNSUBSCRIBE": category=5;break;
		    			default: break;
		    			}
		    		}	
		    		
		    		// start to check which thread it belongs to
		    		JSONArray result;
		    		//println("-----------1");
			    	result = Math.parseCommand(command, output);
			    	
		    		
			    	
			    	// Bge
			    	JSONObject subCommand=null;
			    	
		    		for(int i=0;i<result.size();i++){
			    		if(!result.get(i).toString().equals("{\"endOfTransmit\":true}")){
			    			JSONObject temp=(JSONObject) result.get(i);
			    			if(temp.containsKey("response")&&
			    					temp.get("response").equals("success")&&
			    					temp.containsKey("id")){
			    				subCommand=temp;
			    				
			    				subscribeContinue=true;
			    				//("INTO CONTINUE:"+subscribeContinue);
			    			}
			    			output.writeUTF(((JSONObject)result.get(i)).toJSONString());
			    			output.flush();	
			    			if(debug)log.debug(serverName + "SENT: "+((JSONObject) result.get(i)).toJSONString());
			    		}
			    		else if(category==0 ||category==1||category==5){
			    			end=true;
			    			}else if(category==4&&subscribeContinue){
			    				//Thread !!!!!
			    				//("INTO if!!"+"continue:"+subscribeContinue);
			    				Thread Subscribe = new Thread(() -> {
			    					try {
			    						//("INTO Thread Subscribe!!");
			    						doSubscribe(output,command,false);
			    					} catch (UnknownHostException e) {
			    						// TODO Auto-generated catch block
			    						e.printStackTrace();
			    					} catch (IOException e) {
			    						// TODO Auto-generated catch block
			    						e.printStackTrace();
			    					}
			    				});
			    				Subscribe.start();
			    			}
			    		}
		    		
		    		
		    		if(end) break;
			    	
		    	}
		    }
		    
		    output.close();
    		input.close();
		    
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
	}

	// Bge
	
	private static void doSubscribe(DataOutputStream output, JSONObject command, boolean isSecure) throws IOException {
		// TODO Auto-generated method stub
		//("INTO Method doSubscribe!!");
		JSONObject raw;
		raw=Math.preProcess(command);
		
		String id=(String) raw.get("id");
		subscribeID.add(id);
		subscribedItem.put(id, (int)0);
		JSONObject result=new JSONObject();
		long startTime=System.currentTimeMillis();
		boolean updateTime=false;
		if(command.containsKey("relay")&&((boolean)command.get("relay"))==true){
			JSONObject relaycommand=new JSONObject(command);
			relaycommand.replace("owner", "");
			relaycommand.replace("channel", "");
			relaycommand.replace("relay", false);
			ArrayList<String> hostRelay=(isSecure)?Server.secureServerRecords:Server.serverRecords;
			//
			ArrayList<String> relayResult=new ArrayList<String>();
			ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
			for (int i = 0; i <hostRelay.size(); i++) {
				final int index = i;
				try {
					Thread.sleep(index * 500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			 
				cachedThreadPool.execute(new Runnable() {
			 
					public void run() {
						subscribeRelay(relayResult, hostRelay.get(index).split(":")[0],
								 hostRelay.get(index).split(":")[1],command,isSecure);
					}
				});
				
			}
			
		}
	
		
		while(Server.subscribeID.contains(id)||Server.relayedSUB.containsKey(id)){
		//	System.out.println("templateTime:"+startTime);
			long time1=System.currentTimeMillis();
			while(true){
				if(System.currentTimeMillis()>time1+500)
					break;
			}
				updateTime=false;
			if(Server.relayedSUB.containsKey(id)){
			for(int j=0;j<((ArrayList<JSONObject>)Server.relayedSUB.get(id)).size();j++){
				output.writeUTF(((ArrayList<JSONObject>)Server.relayedSUB.get(id)).get(j).toJSONString());
				output.flush();
				subscribedItem.replace(id, (int)subscribedItem.get(id)+1);
			}
			Server.relayedSUB.remove(id);
			}
			for(int i=0;i<Server.resourceList.size();i++){
				if(Math.queryMatch(Server.resourceList.get(i),command)&&
						startTime<Server.resourceList.get(i).getTime()
						){
					output.writeUTF(      ( new Resource(Server.resourceList.get(i).getObj())    ).toJSON().toJSONString()     );
					output.flush();
					subscribedItem.replace(id, (int)subscribedItem.get(id)+1);
					updateTime=true;
					
				}
			}
			if(updateTime)
			startTime=System.currentTimeMillis();
		}
		
	}
	
	static void subscribeRelay(ArrayList<String> array, String host, String portString, JSONObject command, boolean isSecure){
		try{
			int port=Integer.parseInt(portString);
			if(isSecure){//secure connection:
				SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
				SSLSocket sslsocket = (SSLSocket) sslsocketfactory.createSocket(host, port);
				DataInputStream input = new DataInputStream(sslsocket.getInputStream());
				DataOutputStream output = new DataOutputStream(sslsocket.getOutputStream());		
				boolean exit=false;
			
				try{
					output.writeUTF(command.toJSONString());
					output.flush();
					if(debug){
						log.info("SENT: "+command.toJSONString());	
					}
				}catch(IOException e){
					e.printStackTrace();
					System.exit(0);			
				}
	//			boolean fetchFlag=false;
				try {
					while(true){
						String message;
						if((message=input.readUTF())!=null){
							JSONParser parser1=new JSONParser();
							JSONObject temp=(JSONObject) parser1.parse(message);
							String relayid=null;
							if(debug){
								log.info("RECEIVED: " + message);}
							else{
								log.info(message);}
							if(temp.containsKey("errorMessage")||temp.containsKey("resultSize")) {exit=true;break;}
							else if(temp.containsKey("response")&& ((String)temp.get("response")).equals("success")){
								final String id=(String) temp.get("id");
								relayid=(String) temp.get("id");
								//(relayid);
								Thread t2 = new Thread(() -> {
									try {
										while(Server.subscribeID.contains(id)){
											long time1=System.currentTimeMillis();
											while(true){
												if(System.currentTimeMillis()>time1+800)
													break;
											}									
										}
										JSONObject unsub=new JSONObject();
										unsub.put("command", "UNSUBSCRIBE");
										unsub.put("id", id);
										output.writeUTF(unsub.toJSONString());
										output.flush();
										
									} catch (UnknownHostException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								});
								t2.start();
								}else{
									if(Server.relayedSUB.containsKey(relayid)){
										((ArrayList<JSONObject>)Server.relayedSUB.get(relayid)).add(temp);
									}else{
										ArrayList<JSONObject> item=new ArrayList<JSONObject>();
										item.add(temp);
										Server.relayedSUB.put(relayid, item);
										//(Server.relayedSUB.toString());
									}
								}
						}
					}
					input.close();
					output.close();
				} catch (IOException e) {
						log.warn("Server seems to have closed connection.");
						System.exit(0);
					}				
			}else{//insecure connection:
				Socket socket = new Socket(host, port);
				DataInputStream input2 = new DataInputStream(socket.getInputStream());
				DataOutputStream output2 = new DataOutputStream(socket.getOutputStream());
				String relayid=null;
				boolean exit=false;
				String tag;
				try{
					output2.writeUTF(command.toJSONString());
					output2.flush();
					if(debug){
						log.info("SENT: "+command.toJSONString());	
					}
				}catch(IOException e){
					e.printStackTrace();
					System.exit(0);			
				}
				try {
					while(true){						
						if(input2.available()>0){
							String message = input2.readUTF();
							JSONParser parser1=new JSONParser();
							JSONObject temp=(JSONObject) parser1.parse(message);
							if(debug){
								log.info("RECEIVED: " + message);}
							else{
								log.info(message);}
							if(temp.containsKey("errorMessage")||temp.containsKey("resultSize")) {exit=true;break;}
							else if(temp.containsKey("response")&& ((String)temp.get("response")).equals("success")){
								final String id=(String) temp.get("id");
								relayid=(String) temp.get("id");
								//(relayid);
								Thread t2 = new Thread(() -> {
									try {
										while(Server.subscribeID.contains(id)){
											long time1=System.currentTimeMillis();
											while(true){
												if(System.currentTimeMillis()>time1+800)
													break;
											}									
										}
										JSONObject unsub=new JSONObject();
										unsub.put("command", "UNSUBSCRIBE");
										unsub.put("id", id);
										output2.writeUTF(unsub.toJSONString());
										output2.flush();
										
									} catch (UnknownHostException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								});
								t2.start();
								}else{
									if(Server.relayedSUB.containsKey(relayid)){
										((ArrayList<JSONObject>)Server.relayedSUB.get(relayid)).add(temp);
									}else{
										ArrayList<JSONObject> item=new ArrayList<JSONObject>();
										item.add(temp);
										//("last ID:"+relayid);
										Server.relayedSUB.put(relayid, item);
										//(Server.relayedSUB.toString());
									}
								}
						}
					}
					input2.close();
					output2.close();
				} catch (IOException e) {
						log.warn("Server seems to have closed connection.");
						System.exit(0);
					}			
			}			
		}catch(Exception e){			
		}		
	}
	private static void doSSLSubscribe(DataOutputStream output, JSONObject command) throws IOException {
		// TODO Auto-generated method stub
		//("INTO Method doSubscribe!!");
		JSONObject raw;
		raw=SSLMath.preProcess(command);
		
		String id=(String) raw.get("id");
		subscribeID.add(id);
		subscribedItem.put(id, (int)0);
		JSONObject result=new JSONObject();
		long startTime=System.currentTimeMillis();
		boolean updateTime=false;
		if(command.containsKey("relay")&&((boolean)command.get("relay"))==true){
			JSONObject relaycommand=new JSONObject(command);
			relaycommand.replace("name", "");
			relaycommand.replace("description", "");
			relaycommand.replace("relay", false);
		}
		
		while(Server.subscribeID.contains(id)){
			long time1=System.currentTimeMillis();
			while(true){
				if(System.currentTimeMillis()>time1+1000)
					break;
			}
			updateTime=false;
			for(int i=0;i<Server.resourceList.size();i++){
				if(startTime<Server.resourceList.get(i).getTime()&&
						SSLMath.queryMatch(Server.resourceList.get(i),command)){
					output.writeUTF(      ( new Resource(Server.resourceList.get(i).getObj())    ).toJSON().toJSONString()     );
					output.flush();
					subscribedItem.replace(id, (int)subscribedItem.get(id)+1);
					updateTime=true;
				}
			}
			if(updateTime)
			startTime=System.currentTimeMillis();
		}
		
	}
	
	public static void AddOptions(Options options) {
		options.addOption("debug", false, "Print debut information");
		options.addOption("secret", true, "Server secret");
		options.addOption("port", true, "server port, an integer");
		options.addOption("sport", true, "secure server port, an integeer");
		options.addOption("exchangeinterval", true, "exchange interval in seconds");
		options.addOption("connectionintervallimit", true, "connection interval limit in seconds");
		options.addOption("advertisedhostname", true, "advertised hostname");
	}

	private static String getRandomString(int length){
	     String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	     Random random=new Random();
	     StringBuffer sb=new StringBuffer();
	     for(int i=0;i<length;i++){
	       int number=random.nextInt(62);
	       sb.append(str.charAt(number));
	     }
	     return sb.toString();
	 }

	private static void secureExe() {
		// debugging
		String serverName = "Secure Server";
		int serverListSize = secureServerRecords.size();
		//("1: " + serverName + " has server records of size: " + serverListSize);
		
		String secureList = "";
		for (int k = 0; k < serverListSize; k++) {
			secureList += secureServerRecords.get(k) + ", ";
		}		
		//("2: " + serverName + " has serverList:" + secureList);
		
		if (secureServerRecords.size() == 0) {
			// debugging
			//("3: " + serverName + " : No servers to excahnge");	
		} else {
			
			int selectedIndex = (new Random()).nextInt(secureServerRecords.size());
			
			String host_ip = secureServerRecords.get(selectedIndex);
			// debugging
			//("4: " + serverName + " selected server: " + host_ip);
			
			String[] host_ip_arr = host_ip.split(":");
			String host_name = host_ip_arr[0];
			int ip_add = Integer.parseInt(host_ip_arr[1]);
			
			JSONObject exchangeCommand = new JSONObject();
			String records = "";
			for (int i = 0; i<secureServerRecords.size(); i++) {
				records += secureServerRecords.get(i) + ",";
			}
			try {
				// add local address
				records += InetAddress.getLocalHost().getHostAddress() + ":" + sport;
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			exchangeCommand.put("command", "EXCHANGE");
			exchangeCommand.put("serverList", records);
			SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			try(
					SSLSocket sslsocket = (SSLSocket) sslsocketfactory.createSocket(host_name, ip_add);
					){
				DataInputStream input = new DataInputStream(sslsocket.getInputStream());
				DataOutputStream output = new DataOutputStream(sslsocket.getOutputStream());
			
				output.writeUTF(exchangeCommand.toJSONString());
				output.flush();
				
				//System.out.println("Command sent");
				
				// Time limit for execution
				long start = System.currentTimeMillis();
				long end = start + 5 * 1000;
				boolean isReachable = false;
				while(System.currentTimeMillis() < end) {
					if (input.available() > 0) {
						isReachable = true;
						String result = input.readUTF();
						// debugging
						//("5:" + serverName + " : Received from other server: " + result);
					}
				}
				if (!isReachable) {
					secureServerRecords.remove(selectedIndex);
					// debugging
					//("6: " + serverName + ": Removed unreachable server: " + host_ip);
				}
				
			} catch (IOException e) {
				//e.printStackTrace();
				
				secureServerRecords.remove(selectedIndex);
				// debugging
				//("7: " + serverName + ": Removed unreachable server: " + host_ip);
			}
		}
	}
	
	private static void secureCommunication() {
		// debugging
		String serverName = "Secure Server";
		int serverListSize = secureServerRecords.size();
		//("1: " + serverName + " has server records of size: " + serverListSize);
		
		
		//("2: " + serverName + " has serverList:" + secureList);
		
		if (serverListSize == 0) {
			// debugging
			//("3: " + serverName + " : No servers to excahnge");	
		} else {
			
			int selectedIndex = (new Random()).nextInt(secureServerRecords.size());
			
			String host_ip = secureServerRecords.get(selectedIndex);
			// debugging
			//("4: " + serverName + " selected server: " + host_ip);
			
			String[] host_ip_arr = host_ip.split(":");
			String host_name = host_ip_arr[0];
			int ip_add = Integer.parseInt(host_ip_arr[1]);
			
			JSONArray temp = new JSONArray();
			
			for (int k = 0; k < serverListSize; k++) {
				JSONObject eachServer = new JSONObject();
				String[] temp2 = secureServerRecords.get(k).split(":");
				eachServer.put("hostname", temp2[0]);
				eachServer.put("port", temp2[1]);
				temp.add(eachServer);
			}
			JSONObject currentServer=new JSONObject();
			String localHostName = "";
			try {
				// add local address
				localHostName = InetAddress.getLocalHost().getHostAddress();
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			currentServer.put("hostname", localHostName);
			currentServer.put("port", String.valueOf(sport));
			temp.add(currentServer);
			
			JSONObject exchangeCommand = new JSONObject();
			exchangeCommand.put("command", "EXCHANGE");
			exchangeCommand.put("serverList", temp);
			SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			try(
					SSLSocket sslsocket = (SSLSocket) sslsocketfactory.createSocket(host_name, ip_add);
					){
				DataInputStream input = new DataInputStream(sslsocket.getInputStream());
				DataOutputStream output = new DataOutputStream(sslsocket.getOutputStream());
			
				output.writeUTF(exchangeCommand.toJSONString());
				output.flush();
				
				//System.out.println("Command sent");
				
				// Time limit for execution
				long start = System.currentTimeMillis();
				long end = start + 5 * 1000;
				boolean isReachable = false;
				while(System.currentTimeMillis() < end) {
					if (input.available() > 0) {
						isReachable = true;
						String result = input.readUTF();
						// debugging
						//("5:" + serverName + " : Received from other server: " + result);
					}
				}
				if (!isReachable) {
					secureServerRecords.remove(selectedIndex);
					// debugging
					//("6: " + serverName + ": Removed unreachable server: " + host_ip);
				}
				
			} catch (IOException e) {
				//e.printStackTrace();
				
				secureServerRecords.remove(selectedIndex);
				// debugging
				//("7: " + serverName + ": Removed unreachable server: " + host_ip);
			}
		}
	}
	
	
	
	private static void insecureExe() {
		
		String serverName = "Insecure Server";
		int serverListSize = serverRecords.size();
		//("1: " + serverName + " has server records of size: " + serverListSize);
		String secureList = "";
		for (int k = 0; k < serverListSize; k++) {
			secureList += serverRecords.get(k);
		}
		//("2: " + serverName + " has serverList:" + secureList);	
		if (serverRecords.size() == 0) {
			//("3: " + serverName + " : No servers to excahnge");
		} else {		
			int selectedIndex = (new Random()).nextInt(serverRecords.size());	
			String host_ip = serverRecords.get(selectedIndex);
			// debugging
			//("4: " + serverName + "selected server: " + host_ip);
			String[] host_ip_arr = host_ip.split(":");
			String host_name = host_ip_arr[0];
			int ip_add = Integer.parseInt(host_ip_arr[1]);
			JSONObject exchangeCommand = new JSONObject();
			String records = "";
			for (int i = 0; i<serverRecords.size(); i++) {
				records += serverRecords.get(i) + ",";
			}
			try {
				records += InetAddress.getLocalHost().getHostAddress() + ":" + port;
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			exchangeCommand.put("command", "EXCHANGE");
			exchangeCommand.put("serverList", records);
			
			try(Socket randomServer = new Socket(host_name, ip_add)){
				DataInputStream input = new DataInputStream(randomServer.getInputStream());
				DataOutputStream output = new DataOutputStream(randomServer.getOutputStream());
			
				output.writeUTF(exchangeCommand.toJSONString());
				output.flush();
				
				//System.out.println("Command sent");
				
				// Time limit for execution
				long start = System.currentTimeMillis();
				long end = start + 5 * 1000;
				boolean isReachable = false;
				while(System.currentTimeMillis() < end) {
					if (input.available() > 0) {
						isReachable = true;
						String result = input.readUTF();
						//(serverName + " : Received from other server: " + result);
					}
				}
				if (!isReachable) {
					serverRecords.remove(selectedIndex);
					// debugging
					//("6: " + serverName + ": Removed unreachable server: " + host_ip);
				}
				
			} catch (IOException e) {
				//e.printStackTrace();
				serverRecords.remove(selectedIndex);
				// debugging
				//("7: " + serverName + " removed unreachable server: " + host_ip);
			}
		}
	}
	
private static void insecureCommunication() {
		
		String serverName = "Insecure Server";
		int serverListSize = serverRecords.size();
		//("1: " + serverName + " has server records of size: " + serverListSize);
		
		//("2: " + serverName + " has serverList:" + secureList);	
		if (serverListSize== 0) {
			//("3: " + serverName + " : No servers to excahnge");
		} else {		
			int selectedIndex = (new Random()).nextInt(serverRecords.size());	
			String host_ip = serverRecords.get(selectedIndex);
			// debugging
			//("4: " + serverName + "selected server: " + host_ip);
			String[] host_ip_arr = host_ip.split(":");
			String host_name = host_ip_arr[0];
			int ip_add = Integer.parseInt(host_ip_arr[1]);
			
			JSONObject exchangeCommand = new JSONObject();
			
			JSONArray temp = new JSONArray();
			for (int i = 0; i<serverRecords.size(); i++) {
				JSONObject eachServer = new JSONObject();
				String[] list = serverRecords.get(i).split(":");
				eachServer.put("hostname", list[0]);
				eachServer.put("port", list[1]);
				temp.add(eachServer);
			}
			
			String currentHost = "";
			try {
				currentHost = InetAddress.getLocalHost().getHostAddress();
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			JSONObject currentHostJSON = new JSONObject();
			currentHostJSON.put("host", currentHost);
			currentHostJSON.put("port", String.valueOf(port));
			temp.add(currentHostJSON);
			
			exchangeCommand.put("command", "EXCHANGE");
			exchangeCommand.put("serverList", temp);
			
			try(Socket randomServer = new Socket(host_name, ip_add)){
				DataInputStream input = new DataInputStream(randomServer.getInputStream());
				DataOutputStream output = new DataOutputStream(randomServer.getOutputStream());
			
				output.writeUTF(exchangeCommand.toJSONString());
				output.flush();
				
				//System.out.println("Command sent");
				
				// Time limit for execution
				long start = System.currentTimeMillis();
				long end = start + 5 * 1000;
				boolean isReachable = false;
				while(System.currentTimeMillis() < end) {
					if (input.available() > 0) {
						isReachable = true;
						String result = input.readUTF();
						//(serverName + " : Received from other server: " + result);
					}
				}
				if (!isReachable) {
					serverRecords.remove(selectedIndex);
					// debugging
					//("6: " + serverName + ": Removed unreachable server: " + host_ip);
				}
				
			} catch (IOException e) {
				//e.printStackTrace();
				serverRecords.remove(selectedIndex);
				// debugging
				//("7: " + serverName + " removed unreachable server: " + host_ip);
			}
		}
	}
}