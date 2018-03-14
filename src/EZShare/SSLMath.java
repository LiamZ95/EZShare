package EZShare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;



public class SSLMath {
	
	private static final Logger log = Logger.getLogger(Logger.class);
	static JSONArray parseCommand(JSONObject raw,DataOutputStream output)  {
		JSONArray result = new JSONArray();
		JSONObject command=new JSONObject();

		//this solves generic response
		if (raw.containsKey("command")) {
			switch((String) raw.get("command")) {
			//each case handles more explicit situation
			
			case "PUBLISH":
				command=preProcess(raw);
				result=publishJSON(command);
//				System.out.println("Current ResourceList size:"+result.size());
				break;
			case "REMOVE":
				command=preProcess(raw);
				result=removeJSON(command);
//				System.out.println("Current ResourceList size:"+result.size());
				break;
			case "SHARE":
				command=preProcess(raw);
				result=shareJSON(command);
				break;
			case "QUERY":
				command=preProcess(raw);
				result=queryJSON(command);
				break;
			case "FETCH":
				command=preProcess(raw);
				result=fetchJSON(command,output);
				break;
			case "EXCHANGE":
				//println("raw="+raw.toJSONString());
				command=preProcess(raw);
				//println("command="+command.toJSONString());
				result=exchangeJSON(command);
//				System.out.println("Current server List:"+Server.serverRecords.toString());
				break;
			case "SUBSCRIBE":
				command=preProcess(raw);
				result=subscribeJSON(command);
				break;
			case "UNSUBSCRIBE":
				command.put("command", "UNSUBSCRIBE");
				command.put("id", raw.get("id"));
				
				Server.subscribeID.remove(raw.get("id"));
				long time=System.currentTimeMillis();
				while(true){
					if(System.currentTimeMillis()>time+600){
						break;
					}
				}
				JSONObject obj=new JSONObject();
				obj.put("resultSize", Server.subscribedItem.get(raw.get("id")));
				Server.subscribedItem.remove(raw.get("id"));
				result.add(obj);
				break;
			default:
				//return invalid command
				JSONObject obj3=new JSONObject();
				obj3.put("response", "error");
				obj3.put("errorMessage", "invalid command");
				result.add(obj3);
				break;
			}
		} else {
			//return missing or incorrect type
			JSONObject obj=new JSONObject();
			obj.put("response", "error");
			obj.put("errorMessage", "missing or incorrect type for command");
			result.add(obj);
		}
		JSONObject end=new JSONObject();
		end.put("endOfTransmit", true);
		result.add(end);
		return result;
	}
	/////////////////////////////////////////////////////////////////////////////////////////////////////
	static JSONObject preProcess(JSONObject command) {
		// TODO Auto-generated method stub
		//println("preprocess="+command.toJSONString());
	
		if(command.containsKey("command")){
			switch((String)command.get("command")){
			case("PUBLISH"):{
				String name = "";
				if (((HashMap)command.get("resource")).containsKey("name")){
					name = (String) ((HashMap)command.get("resource")).get("name");
				}
				ArrayList<String> tags=new ArrayList<String>();
				if(((HashMap)command.get("resource")).containsKey("tags")){
					tags=(ArrayList<String>) ((HashMap)command.get("resource")).get("tags");
				}
				String des = "";
				if (((HashMap)command.get("resource")).containsKey("description")) {
					des = (String) ((HashMap)command.get("resource")).get("description");
				}
				String uri = "";
				if (((HashMap)command.get("resource")).containsKey("uri")) {
					uri = (String) ((HashMap)command.get("resource")).get("uri");
				}
				String channel = "";
				if (((HashMap)command.get("resource")).containsKey("channel")) {
					channel = (String) ((HashMap)command.get("resource")).get("channel");
				}
				String owner = "";
				if (((HashMap)command.get("resource")).containsKey("owner")) {
					owner = (String) ((HashMap)command.get("resource")).get("owner");
					if(owner.equals(".classpath")) owner="*";
				}
				JSONObject resource = new JSONObject();
				JSONObject commandObj = new JSONObject();
				resource.put("name", name);
				resource.put("tags", tags);
				resource.put("description", des);
				resource.put("uri", uri);
				resource.put("channel", channel);
				resource.put("owner", owner);
				resource.put("ezserver", null);
				commandObj.put("command", "PUBLISH");
				commandObj.put("resource", resource);
				
				//(commandObj.toString());
				return commandObj;
				
			}
			case("REMOVE"):{
				
				String uri = "";
				if (((HashMap)command.get("resource")).containsKey("uri")) {
					uri = (String) ((HashMap)command.get("resource")).get("uri");
				}
				String channel = "";
				if (((HashMap)command.get("resource")).containsKey("channel")) {
					channel = (String) ((HashMap)command.get("resource")).get("channel");
				}
				String owner = "";
				if (((HashMap)command.get("resource")).containsKey("owner")) {
					owner = (String) ((HashMap)command.get("resource")).get("owner");
					if(owner.equals(".classpath")) owner="*";
				}
				JSONObject resource = new JSONObject();
				JSONObject commandObj = new JSONObject();
				resource.put("uri", uri);
				resource.put("channel", channel);
				resource.put("owner", owner);
				resource.put("ezserver", null);
				commandObj.put("command", "REMOVE");
				commandObj.put("resource", resource);
				return commandObj;
				
			}
			case("SHARE"):{
				String name = "";
				if (((HashMap)command.get("resource")).containsKey("name")){
					name = (String) ((HashMap)command.get("resource")).get("name");
				}
				
				ArrayList<String> tags=new ArrayList<String>();
				if(((HashMap)command.get("resource")).containsKey("tags")){
					tags=(ArrayList<String>) ((HashMap)command.get("resource")).get("tags");
				}
				String des = "";
				if (((HashMap)command.get("resource")).containsKey("description")) {
					des = (String) ((HashMap)command.get("resource")).get("description");
				}
				String uri = "";
				if (((HashMap)command.get("resource")).containsKey("uri")) {
					uri = (String) ((HashMap)command.get("resource")).get("uri");
				}
				String channel = "";
				if (((HashMap)command.get("resource")).containsKey("channel")) {
					channel = (String) ((HashMap)command.get("resource")).get("channel");
				}
				String owner = "";
				if (((HashMap)command.get("resource")).containsKey("owner")) {
					owner = (String) ((HashMap)command.get("resource")).get("owner");
					if(owner.equals(".classpath")) owner="*";
				}
				String secret = "";
				if (command.containsKey("secret")) {
					secret = (String) command.get("secret");
				}
				JSONObject resource = new JSONObject();
				JSONObject commandObj = new JSONObject();
				resource.put("name", name);
				commandObj.put("secret", secret);
				resource.put("tags", tags);
				resource.put("description", des);
				resource.put("uri", uri);
				resource.put("channel", channel);
				resource.put("owner", owner);
				resource.put("ezserver", null);
				commandObj.put("command", "SHARE");
				commandObj.put("resource", resource);
				return commandObj;
				
				
			}
			case("QUERY"):{
				String name = "";
				if (((HashMap)command.get("resourceTemplate")).containsKey("name")){
					name = (String) ((HashMap)command.get("resourceTemplate")).get("name");
				}
				ArrayList<String> tags=new ArrayList<String>();
				if(((HashMap)command.get("resourceTemplate")).containsKey("tags")){
					tags=(ArrayList<String>) ((HashMap)command.get("resourceTemplate")).get("tags");
				}
				String des = "";
				if (((HashMap)command.get("resourceTemplate")).containsKey("description")) {
					des = (String) ((HashMap)command.get("resourceTemplate")).get("description");
				}
				String uri = "";
				if (((HashMap)command.get("resourceTemplate")).containsKey("uri")) {
					uri = (String) ((HashMap)command.get("resourceTemplate")).get("uri");
				}
				String channel = "";
				if (((HashMap)command.get("resourceTemplate")).containsKey("channel")) {
					channel = (String) ((HashMap)command.get("resourceTemplate")).get("channel");
				}
				String owner = "";
				if (((HashMap)command.get("resourceTemplate")).containsKey("owner")) {
					owner = (String) ((HashMap)command.get("resourceTemplate")).get("owner");
					if(owner.equals(".classpath")) owner="*";
				}
				boolean relay = false;
				if (command.containsKey("relay")) {
				//	System.out.println("INTO   IF preprocess"+((boolean)command.get("relay")==true));
					relay = ((boolean)command.get("relay")==true)?true:false;
				}
				JSONObject resourceTemplate = new JSONObject();
				JSONObject commandObj = new JSONObject();
				resourceTemplate.put("tags", tags);
				resourceTemplate.put("name", name);
				resourceTemplate.put("description", des);
				resourceTemplate.put("uri", uri);
				resourceTemplate.put("channel", channel);
				resourceTemplate.put("owner", owner);
				resourceTemplate.put("ezserver", null);
				commandObj.put("command", "QUERY");
				commandObj.put("relay", relay);
				commandObj.put("resourceTemplate", resourceTemplate);
				return commandObj;
				
			}
			//////////////////////////////////////////////////////////////////////jkljkljkljkl
			case("FETCH"):{

				String uri = "";
				if (((HashMap)command.get("resourceTemplate")).containsKey("uri")) {
					uri = (String) ((HashMap)command.get("resourceTemplate")).get("uri");
				}
				String channel = "";
				if (((HashMap)command.get("resourceTemplate")).containsKey("channel")) {
					channel = (String) ((HashMap)command.get("resourceTemplate")).get("channel");
				}
				JSONObject resourceTemplate = new JSONObject();
				JSONObject commandObj = new JSONObject();
				resourceTemplate.put("uri", uri);
				resourceTemplate.put("channel", channel);
				resourceTemplate.put("ezserver", null);
				commandObj.put("command", "FETCH");
				commandObj.put("resourceTemplate", resourceTemplate);
				//("after pre-process:"+commandObj.toJSONString());
				return commandObj;
				//////////////////////////////////////////////////////////////////////////////////////////
			}
			//**LIAM**
			case("EXCHANGE"):{
				//println("preprocess="+command.toJSONString());
		
				ArrayList<JSONObject> serverList = new ArrayList<JSONObject>() ;
				if (command.containsKey("serverList")) {
					serverList= (ArrayList<JSONObject>) command.get("serverList");
				}
			
				JSONObject commandObj = new JSONObject();
				commandObj.put("command", "EXCHANGE");
				commandObj.put("serverList", serverList);
		
				return commandObj;
			}
			case("SUBSCRIBE"):{
				String name = "";
				if (((HashMap)command.get("resourceTemplate")).containsKey("name")){
					name = (String) ((HashMap)command.get("resourceTemplate")).get("name");
				}
				String id = "";
				if ((command.containsKey("id"))){
					id = (String) (command.get("id"));
				}
				ArrayList<String> tags=new ArrayList<String>();
				if(((HashMap)command.get("resourceTemplate")).containsKey("tags")){
					tags=(ArrayList<String>) ((HashMap)command.get("resourceTemplate")).get("tags");
				}
				String des = "";
				if (((HashMap)command.get("resourceTemplate")).containsKey("description")) {
					des = (String) ((HashMap)command.get("resourceTemplate")).get("description");
				}
				String uri = "";
				if (((HashMap)command.get("resourceTemplate")).containsKey("uri")) {
					uri = (String) ((HashMap)command.get("resourceTemplate")).get("uri");
				}
				String channel = "";
				if (((HashMap)command.get("resourceTemplate")).containsKey("channel")) {
					channel = (String) ((HashMap)command.get("resourceTemplate")).get("channel");
				}
				String owner = "";
				if (((HashMap)command.get("resourceTemplate")).containsKey("owner")) {
					owner = (String) ((HashMap)command.get("resourceTemplate")).get("owner");
					if(owner.equals(".classpath")) owner="*";
				}
				boolean relay = false;
				if (command.containsKey("relay")) {
					relay = command.get("relay").equals("true")?true:false;
				}
				JSONObject resourceTemplate = new JSONObject();
				JSONObject commandObj = new JSONObject();
				resourceTemplate.put("tags", tags);
				resourceTemplate.put("name", name);
				resourceTemplate.put("description", des);
				resourceTemplate.put("uri", uri);
				resourceTemplate.put("channel", channel);
				resourceTemplate.put("owner", owner);
				resourceTemplate.put("ezserver", null);
				commandObj.put("command", "SUBSCRIBE");
				commandObj.put("relay", relay);
				commandObj.put("id", id);
				commandObj.put("resourceTemplate", resourceTemplate);
				return commandObj;
				
			}
			//**LIAM**
			}
		}else{

		String name = "";
		if (((HashMap)command.get("resourceTemplate")).containsKey("name")){
			name = (String) ((HashMap)command.get("resourceTemplate")).get("name");
		}
		ArrayList<String> tags=new ArrayList<String>();
		if(((HashMap)command.get("resourceTemplate")).containsKey("tags")){
			tags=(ArrayList<String>) ((HashMap)command.get("resourceTemplate")).get("tags");
		}
		String des = "";
		if (((HashMap)command.get("resourceTemplate")).containsKey("description")) {
			des = (String) ((HashMap)command.get("resourceTemplate")).get("description");
		}
		String uri = "";
		if (((HashMap)command.get("resourceTemplate")).containsKey("uri")) {
			uri = (String) ((HashMap)command.get("resourceTemplate")).get("uri");
		}
		String channel = "";
		if (((HashMap)command.get("resourceTemplate")).containsKey("channel")) {
			channel = (String) ((HashMap)command.get("resourceTemplate")).get("channel");
		}
		String owner = "";
		if (((HashMap)command.get("resourceTemplate")).containsKey("owner")) {
			owner = (String) ((HashMap)command.get("resourceTemplate")).get("owner");
			if(owner.equals(".classpath")) owner="*";
		}
		boolean relay = false;
		if (command.containsKey("relay")) {
			relay = command.get("relay").equals("true")?true:false;
		}
		JSONObject resourceTemplate = new JSONObject();
		JSONObject commandObj = new JSONObject();
		resourceTemplate.put("tags", tags);
		resourceTemplate.put("name", name);
		resourceTemplate.put("description", des);
		resourceTemplate.put("uri", uri);
		resourceTemplate.put("channel", channel);
		resourceTemplate.put("owner", owner);
		resourceTemplate.put("ezserver", null);
		commandObj.put("command", "QUERY");
		commandObj.put("relay", relay);
		commandObj.put("resourceTemplate", resourceTemplate);
		return commandObj;
		
	}		
		
		return null;
	}

	private static JSONArray publishJSON(JSONObject command) {
	
		JSONObject result = new JSONObject();
		JSONArray array=new JSONArray();
		if(!command.containsKey("resource")||
				!((HashMap) command.get("resource")).containsKey("uri")){
			result.put("response", "error");
			result.put("errorMessage", "missing resource");
			array.add(result);
			debug(array);
			return array;
		} else if(((HashMap) command.get("resource")).get("owner").equals("*")){
			result.put("response", "error");
			result.put("errorMessage", "invalid resource");
			array.add(result);
			debug(array);			
			return array;
		} else if(((String)((HashMap) command.get("resource")).get("uri")).equals("")) {
			//this if clause check if the file scheme is "file"
			result.put("response", "error");
			result.put("errorMessage", "missing resource");
			array.add(result);
			debug(array);
			return array;
		} else {
//			System.out.println("file or http::"+(String)((HashMap) command.get("resource")).get("uri"));//////
			if(   isURI((String)((HashMap) command.get("resource")).get("uri"))){
				for (int i = 0; i < Server.resourceList.size(); i++) {
				//this if clause check if there is resource with same channel and uri but different owner
					if (Server.resourceList.get(i).ifduplicated(command)) {
						result.put("response", "error");
						result.put("errorMessage", "cannot publish resource");
						array.add(result);
						debug(array);
						return array;
					}
				//this checks if there is resource with same channel, uri and owner, replace the obj
					if (Server.resourceList.get(i).ifOverwrites(command)) {
						Server.resourceList.get(i).overwrites(command);
						//Server.resourceList.get(i).setTime();
						result.put("response", "success");/////
						array.add(result);
						debug(array);
						return array;
				}	
			}
			
			Server.resourceList.add(new KeyTuple(new Resource(command)));
			//("resourceList:"+Server.resourceList.size());
			for(int k=0;k<Server.resourceList.size();k++){
				//(Server.resourceList.get(k).getUri());
			}
			result.put("response", "success");
			array.add(result);
			debug(array);
			return array;
			
			}else{
				result.put("response", "error");
				result.put("errorMessage", "cannot publish resource");
				array.add(result);
				return array;
				
			}
		}
	}
	
	private static JSONArray shareJSON(JSONObject command) {
		JSONObject result = new JSONObject();
		JSONArray array=new JSONArray();
		if(!command.containsKey("resource")||
				!command.containsKey("secret") ||(command.get("secret")==""||
				!((HashMap) command.get("resource")).containsKey("uri")||
				((HashMap) command.get("resource")).get("uri")=="")
				){
			result.put("response", "error");
			result.put("errorMessage", "missing resource and\\/or secret");
			array.add(result);
			return array;
		} else if(((HashMap) command.get("resource")).get("owner").equals("*")){
			result.put("response", "error");
			result.put("errorMessage", "invalid resource");
			array.add(result);
			return array;
		} else {
			
			//this checks whether the secret is correct
			boolean eligible = false;
			
			if (Server.secret.equals(command.get("secret"))) 
					eligible = true;
				
			
			if (!eligible) {
				result.put("response", "error");
				result.put("errorMessage", "incorrect secret");
				array.add(result);
				return array;
			}
		}
		//this if clause check if the file scheme is "file"
		String temp=((String)((HashMap) command.get("resource")).get("uri"));
		if( (temp==null || temp.equals("") ||temp.length()<=7 || !temp.substring(0,7).equals("file://") )){
			result.put("response", "error");
			result.put("errorMessage", "cannot share resource");
			array.add(result);
			return array;
		} else if (!(new File(temp.substring(7))).exists()){
			result.put("response", "error");
			result.put("errorMessage", "invalid resource");
			array.add(result);
			return array;
		} else {
			for (int i = 0; i < Server.resourceList.size(); i++) {
				//this if clause check if there is resource with same channel and uri but different owner
				if (Server.resourceList.get(i).ifduplicated(command)) {
					result.put("response", "error");
					result.put("errorMessage", "cannot share resource");
					array.add(result);
					return array;
				}
				//this checks if there is resource with same channel, uri and owner, replace the obj
				if (Server.resourceList.get(i).ifOverwrites(command)) {
					Server.resourceList.get(i).overwrites(command);
					result.put("response", "success");
					array.add(result);
					return array;
				}	
			}
			Server.resourceList.add(new KeyTuple(new Resource(command)));
			result.put("response", "success");
			array.add(result);
			return array;
		}
	}
	
	private static JSONArray queryJSON(JSONObject command){	
		int resultSize=0;
		ArrayList<KeyTuple> tempList=new ArrayList<KeyTuple>();
		////////////////////////////////////////////////////////////////////////////
		JSONArray relayList=new JSONArray();
		boolean relaysuccess=false;
		//("++++++"+command.toJSONString());
		if(command.containsKey("relay")&&((boolean)command.get("relay"))==true){
			//("----------INTO if");
			JSONObject relaycommand=new JSONObject(command);
			//if()
			relaycommand.replace("owner", "");
			relaycommand.replace("channel", "");
			relaycommand.replace("relay", false);
			JSONParser parser = new JSONParser();
			for(int i=0;i<Server.secureServerRecords.size();i++){
				String relayhost=Server.secureServerRecords.get(i).split(":")[0];
				//("----------host+1"+relayhost);
				int relayport=Integer.parseInt(Server.secureServerRecords.get(i).split(":")[1]);
				//("----------port+1"+relayport);
				SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
				try (
						SSLSocket sslsocket = (SSLSocket) sslsocketfactory.createSocket(relayhost, relayport);
				) {

					// Get I/O streams for connection
					DataInputStream input2 = new DataInputStream(sslsocket.getInputStream());
					DataOutputStream output2 = new DataOutputStream(sslsocket.getOutputStream());
					//("----------establish");

					try {
						output2.writeUTF(relaycommand.toJSONString());
						output2.flush();
						//("----------erelaySENT");
					} catch (IOException e) {
						e.printStackTrace();
					}

					try {
						while(true){
							String message;
							if((message = input2.readUTF())!=null){
								JSONParser parser1=new JSONParser();
								JSONObject relaytemp=(JSONObject)parser.parse(message);
								if(relaytemp.containsKey("response")&&relaytemp.get("response").equals("success"))
									relaysuccess=true;
								if(relaytemp.containsKey("uri")){
									//("----------get+1");
									relayList.add(relaytemp);
								}
								if(relaytemp.containsKey("errorMessage"))
									break;
								if(relaytemp.containsKey("resultSize"))
									break;
							}
						}
							input2.close();
							output2.close();

						} catch (IOException e) {
//							System.out.println("Server seems to have closed connection.");
						}
					}

				 catch (Exception e) {
					e.printStackTrace();
				}
				
			}
		}
		//("INTO!!!");

		if(command.containsKey("resourceTemplate")){
			if(((HashMap) command.get("resourceTemplate")).get("owner").equals("*")){
				JSONObject obj=new JSONObject();
				obj.put("response", "error");
				obj.put("errorMessage", "invalid resourceTemplate");
				JSONArray result=new JSONArray();
				result.add(obj);
				if (!relaysuccess) {
					debug(result);
					return result;
				}
				else{
					JSONArray relayresult=new JSONArray();
					JSONObject obj1=new JSONObject();
					JSONObject obj2=new JSONObject();
					obj1.put("response", "success");
					obj2.put("resultSize", relayList.size());
					relayresult.add(obj1);
					for(int j=0;j<relayList.size();j++){
						relayresult.add(relayList.get(j));
					}
					relayresult.add(obj2);
					debug(relayresult);
					return relayresult;
					
				}
			}else{
				for(int i=0;i<Server.resourceList.size();i++){
					if(queryMatch(Server.resourceList.get(i),command)){
						tempList.add(Server.resourceList.get(i));
					}
				}
			}
			
		}else{
			JSONObject obj=new JSONObject();
			obj.put("response", "error");
			obj.put("errorMessage", "missing resourceTemplate");
			JSONArray result=new JSONArray();
			result.add(obj);
			if (!relaysuccess) {
				debug(result);
				return result;
			}
			else{
				JSONArray relayresult=new JSONArray();
				JSONObject obj1=new JSONObject();
				JSONObject obj2=new JSONObject();
				obj1.put("response", "success");
				obj2.put("resultSize", relayList.size());
				relayresult.add(obj1);
				for(int j=0;j<relayList.size();j++){
					relayresult.add(relayList.get(j));
				}
				relayresult.add(obj2);
				debug(relayresult);
				return relayresult;
				
			}
		}
		if(tempList.size()==0){
			JSONObject obj=new JSONObject();
			JSONObject obj2=new JSONObject();
			obj.put("response", "success");
			obj2.put("resultSize", 0);
			JSONArray result=new JSONArray();
			result.add(obj);
			result.add(obj2);
			if (!relaysuccess) {
				debug(result);
				return result;
			}
			else{
				JSONArray relayresult=new JSONArray();
				JSONObject obj1=new JSONObject();				
				obj1.put("resultSize", relayList.size());
				relayresult.add(obj);
				for(int j=0;j<relayList.size();j++){
					relayresult.add(relayList.get(j));
				}
				relayresult.add(obj1);
				debug(relayresult);
				return relayresult;
				
			}
		}else{
//			for(int i=0;i<Server.resourceList.size();i++){
//				if(queryMatch(Server.resourceList.get(i),command))
//					tempList.add(Server.resourceList.get(i));
//			}
			JSONArray result=new JSONArray();
			JSONObject obj=new JSONObject();
			obj.put("response", "success");
			result.add(obj);
			for(int i=0;i<tempList.size();i++){           /////////////////////
				result.add(( new Resource(tempList.get(i).getObj())).toJSON());
				
			}
			if(relaysuccess){
				for(int j=0;j<relayList.size();j++){
					result.add(relayList.get(j));
				}
			}
				
				
			obj=new JSONObject();
			obj.put("resultSize", tempList.size()+relayList.size());
			result.add(obj);
			debug(result);
			return result;
			
		}
		
		
		
	}
	private static JSONArray subscribeJSON(JSONObject command){	
		if(command.containsKey("resourceTemplate") && command.containsKey("id") && command.get("id")!=null&&(!command.get("id").equals(""))){
			if(((HashMap) command.get("resourceTemplate")).get("owner").equals("*")){
				JSONObject obj=new JSONObject();
				obj.put("response", "error");
				obj.put("errorMessage", "invalid resourceTemplate");
				JSONArray result=new JSONArray();
				result.add(obj);
				debug(result);
				return result;
			}else{
				JSONObject obj1=new JSONObject();
				obj1.put("response", "success");
				obj1.put("id", command.get("id"));
				JSONArray formatResult=new JSONArray();
				formatResult.add(obj1);
				return formatResult;
			}			
		}else{
			JSONObject obj=new JSONObject();
			obj.put("response", "error");
			obj.put("errorMessage", "missing resourceTemplate");
			JSONArray result=new JSONArray();
			result.add(obj);
			debug(result);
			return result;
			}

	}
	
	private static JSONArray fetchJSON(JSONObject command, DataOutputStream output){
		JSONArray result = new JSONArray();
		JSONObject obj = new JSONObject();
		if (!command.containsKey("resourceTemplate")||
				!((HashMap) command.get("resourceTemplate")).containsKey("uri")||
				((String) ((HashMap) command.get("resourceTemplate")).get("uri")) == ""
				) {

			obj.put("response", "error");
			obj.put("errorMessage", "missing resourceTemplate");
			result.add(obj);
			debug(result);
			return result;
		}	
		String channel = (String) ((HashMap) command.get("resourceTemplate")).get("channel");
		String uri = (String) ((HashMap) command.get("resourceTemplate")).get("uri");
		for (int i = 0; i < Server.resourceList.size(); i++) {
			
			if (Server.resourceList.get(i).getChannel().equals(channel) &&
					Server.resourceList.get(i).getUri().equals(uri)) {
				//if the command matches a KeyTuple storeed in the server, the obj in that KeyTuple will be returned
				URI uriIns = null;
				try {
					uriIns = new URI(Server.resourceList.get(i).getUri());
				} catch (URISyntaxException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				//(uriIns.getPath());
				File f = new File(uriIns.getPath());
				if (f.exists()) {
					JSONObject obj0 = new JSONObject();
					JSONObject obj1 = new JSONObject();
					JSONObject obj2 = Server.resourceList.get(i).toJSON();
					JSONObject obj3 = new JSONObject();
					JSONObject obj4 = new JSONObject();
					
					obj0.put("response", "success");
				
					try {
						output.writeUTF(obj0.toJSONString());
						if(Server.debug){
							log.debug("SENT: "+obj0.toJSONString());
						}
				
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					obj2.put("resourceSize", f.length());
					try {
						output.writeUTF(obj2.toJSONString());
						if(Server.debug){
							log.debug("SENT: "+obj2.toJSONString());
						}
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					obj3.put("resultSize", 1);
					try {
						output.writeUTF(obj3.toJSONString());
						if(Server.debug){
							log.debug("SENT: "+obj3.toJSONString());
						}
						output.flush();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					obj4.put("endOfTransmit",true);
					try {
						output.writeUTF(obj4.toJSONString());
						if(Server.debug){
							log.debug("SENT: "+obj4.toJSONString());
						}
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					
//
//					result.add(obj3);
					// start to transimit file -liam
					try{
						JSONObject fileSize = new JSONObject();
						fileSize.put("resourceSize", f.length());
						fileSize.put("uri", uriIns.toString());
						output.writeUTF(fileSize.toJSONString());
						// start sending file -liam
						RandomAccessFile byteFile = new RandomAccessFile(f, "r");
						byte[] sendingBuffer = new byte[1024*1024];
						int num;
						while((num = byteFile.read(sendingBuffer))>0) {
							output.write(Arrays.copyOf(sendingBuffer, num));
						}
						byteFile.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					return result;
				}
				
			}
		}
		
		obj.put("response", "error");
		obj.put("errorMessage", "invalid resourceTemplate");
		result.add(obj);
		debug(result);
		return result;
		}

	private static JSONArray removeJSON(JSONObject command){
		JSONObject result= new JSONObject();
		JSONArray array=new JSONArray();
		if(
				(((HashMap) command.get("resource")).get("uri")==""||((HashMap) command.get("resource")).get("uri").equals(""))){
			result.put("response", "error");
			result.put("errorMessage", "missing resource");
			array.add(result);
			debug(array);
			return array;
		}else if(((HashMap) command.get("resource")).get("owner").equals("*")){
			result.put("response", "error");
			result.put("errorMessage", "invalid resource");
			array.add(result);
			debug(array);
			return array;
		}else {
			boolean removed=false;
			for(int i=0;i<Server.resourceList.size();i++){
				
				if(Server.resourceList.get(i).ifOverwrites(command)){
					Server.resourceList.remove(i);
//					System.out.println("resourceList:"+Server.resourceList.size());
//					for(int k=0;k<Server.resourceList.size();k++){
//						System.out.println(Server.resourceList.get(k).getUri());
//					}
					result.put("response", "success");
					removed=true;
					break;
				}
				
			}
			if(!removed){
				result.put("response", "error");
				result.put("errorMessage", "cannot remove resource(not exsit)");	
			}
			array.add(result);
			debug(array);
			return array;
			
		}
			
		}
	private static JSONArray exchangeJSON(JSONObject command){
		// debugging
		//("********in exchange*********");
		//println("INTO exchangeJSON+"+command.toJSONString());
		String hostAddress = "";
		try {
			hostAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if((!command.containsKey("serverList"))||   ((ArrayList) command.get("serverList")).size()==0  ){
			JSONObject obj=new JSONObject();
			obj.put("response", "error");
			obj.put("errorMessage", "missing or invalid server list");
			JSONArray result=new JSONArray();
			result.add(obj);
			debug(result);
			return result;
		}else {	
			ArrayList<JSONObject> lists=(ArrayList<JSONObject>) ( command.get("serverList"));
			int length=lists.size();
			boolean success=false;
			int failNum=0;
			JSONObject obj=new JSONObject();
			for(int i=0;i<length;i++){
				JSONObject temp=lists.get(i);
				boolean duplicated=false;
				if(temp.containsKey("hostname")&& temp.containsKey("port")){
					String host=checker((String) temp.get("hostname"));
					String port=checker((String) temp.get("port"));
					if(isPort(port)){
						success=true;
						for(int j=0;j<Server.secureServerRecords.size();j++){
							if(Server.secureServerRecords.get(j).equals(host+":"+port)){
								duplicated=true;
								break;
							}	
						}
						if(!duplicated){
							Server.secureServerRecords.add(host+":"+port);
						}
						
					}else{
						obj.put("response", "error");
						obj.put("errorMessage", "missing resourceTemplate");
						failNum++;
						continue;
					}
				}else{
					failNum++;
				}
			}
			JSONArray result=new JSONArray();
			if(success){
				JSONObject temp=new JSONObject();
				temp.put("response", "success");
				result.add(temp);
				return result;
			}else{
				result.add(obj);
				for(int k=0;k<result.size();k++){
					//println("&&&&&&&&&&&&&&&"+result.get(k).toString());
				}
				return result;
				
			}
		
				
		}
		
	}
	
	private static String checker(String input){
		String b=input.replaceAll("\\s", "");
		 b=b.replace("\0", "");
		return b;	
	}
	
	 private static boolean isIpv4(String ipAddress) {
	        String ip = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."
	                + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
	                + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
	                + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";
	        Pattern pattern = Pattern.compile(ip);
	        Matcher matcher = pattern.matcher(ipAddress);
	        return matcher.matches();
	    }
	 
	  static boolean ishostPort(String HP) {
	        String hostPort = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."
	                + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
	                + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
	                + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)"+":"+"\\d{1,5}$";
	        Pattern pattern = Pattern.compile(hostPort);
	        Matcher matcher = pattern.matcher(HP);
	        return matcher.matches();
	    }
	  static boolean isPort(String port) {
		 int portNumber =Integer.parseInt(port);
	        if(portNumber<0||portNumber>65535){
	        	return false;
	        }
	        return true;
	    }
	  private static boolean isURI(String str){
		if(str==null ||str.equals("")) return false;
		else{
			int length=str.length();
			if(length<4) return false;
			if(length<5){if(str.substring(0,4).equals("ftp:")||str.substring(0,4).equals("jar:")) return true; else return false;}
			if(length<6){if(str.substring(0,4).equals("ftp:") || str.substring(0,5).equals("http:")||str.substring(0,4).equals("jar:")) return true; else return false;}
			if(length>=6){if(str.substring(0,4).equals("ftp:") || str.substring(0,5).equals("http:")||str.substring(0,6).equals("https:")||str.substring(0,4).equals("jar:")) return true; else return false;}
		}
		return false;
	}
		
	  static boolean queryMatch(KeyTuple Tuple, JSONObject command) {
		  
		  // TODO Auto-generated method stub
		  boolean[] rules=new boolean[11];
		  rules[0]=Tuple.getChannel().equals(((HashMap) command.get("resourceTemplate")).get("channel"));
		  if(!rules[0]) {//System.out.println("rules 0 wrong");
		  return false;}
		  rules[1]= (((HashMap) command.get("resourceTemplate")).get("owner")).equals("") ||Tuple.getOwner().equals("")||Tuple.getOwner().equals(((HashMap) command.get("resourceTemplate")).get("owner"));
		  if(!rules[1]) {//System.out.println("rules 1 wrong");
		  return false;}
		  rules[2]=true;
		  int commandTag=((ArrayList) ((HashMap) command.get("resourceTemplate")).get("tags")).size();
		  if( commandTag!=0){
			  if(Tuple.getObj().getTags().size()<commandTag) {
				  rules[2]=false;
				  //("rules 2 wrong1");
				  return false;
			  }
			  else{
				  ArrayList<String> tempTags=( (ArrayList<String>) ((HashMap) command.get("resourceTemplate")).get("tags"));	
				  for(int j=0;j<tempTags.size();j++){
					  if(!Tuple.getObj().getTags().contains(tempTags.get(j))){
						  rules[2]=false;
						  //("rules 2 wrong");
						  return false;
					  }
				  }
			  }
		  }
		  if(!((HashMap) command.get("resourceTemplate")).containsKey("uri")) rules[3]=true;
		  else if(((HashMap) command.get("resourceTemplate")).get("uri").equals("")) rules[3]=true;
		  else {
			  if(((HashMap) command.get("resourceTemplate")).get("uri").equals(Tuple.getUri())) rules[3]=true;
			  else {//System.out.println("rules 3 wrong");
			  return false;}
		  }
		  rules[8]=false;
		  if(!((HashMap) command.get("resourceTemplate")).containsKey("name")) rules[7]=true;
		  else if(((HashMap) command.get("resourceTemplate")).get("name").equals("")) rules[8]=true;
		  else {
			  if(   Tuple.getObj().get("name").contains(  (String) ((HashMap) command.get("resourceTemplate")).get("name")     )  ) {rules[4]=true; return true;}
			  else rules[4]= false;
		  }
		  rules[10]=false;
		  if(!((HashMap) command.get("resourceTemplate")).containsKey("description")) rules[9]=true;
		  else if(((HashMap) command.get("resourceTemplate")).get("description").equals("")) rules[10]=true;
		  else {
			  if(   Tuple.getObj().get("description").contains(  (String) ((HashMap) command.get("resourceTemplate")).get("description")     )  ){rules[5]=true; return true;}
			  else rules[5]= false;
		  }		
		  if((rules[7]&&rules[9])||(rules[8]&&rules[10])) {rules[6]=true;return true;}
		  else rules[6]=false;	
		  if(rules[4]==true ||rules[5]==true||rules[6]==true) return true;
		  else return false;
	  }
	  
	  private static void debug(JSONArray array) {
		  if(Server.debug){
			  if(array.contains("error")){
				  log.error("SENT:"+array.toJSONString());
			  }else{
				  log.debug("SENT:"+array.toJSONString());
			  }	
		  }
	  }
	  
	}