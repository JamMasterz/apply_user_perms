package apply_user_perms;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class Main {
	public static void main(String[] args){
		
		ArrayList<String> players = getWhitelistedPlayers();
		
		try {
			applyPerms(players);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private static void applyPerms(ArrayList<String> players) throws InterruptedException{
		for(String uuid: players){
			String username = getUsername(uuid);
			while(username != null && username.equals("[ratelimit]")){
				Thread.sleep(5000);
				username = getUsername(uuid);
			}
			if(username == null){
				System.out.println("Warning: could not find username matching uuid " + uuid + "!");
				continue;
			}
			while(!applyTo(username)){
				Thread.sleep(1000);
			}
			Thread.sleep(1000);
		}
	}
	
	private static boolean applyTo(String username){
		String socketFilename = "plugins/Spinalpack/sockets/command.sock";
		try {
			AFUNIXSocket socket = AFUNIXSocket.connectTo(new AFUNIXSocketAddress(new File(socketFilename)));
			OutputStream os = socket.getOutputStream();
			String command = "pex user " + username + " group add user";
			System.out.println("Running command: " + command);
			os.write(command.getBytes());
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private static String getUsername(String uuid){
		try {
			System.out.println("uuid: " + uuid);
			URL url = new URL("https://api.mojang.com/user/profiles/" + uuid.replace("-", "") + "/names");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
			int code = conn.getResponseCode();
			if(code != 200){
				System.out.println("Got response code " + code);
				if(code == 429)
					return "[ratelimit]";
				else
					return null;
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));
			JsonParser parser = new JsonParser();
			String json = reader.readLine();
			if(json == null)
				return null;
			System.out.println("Response: " + json);
			JsonArray arr = parser.parse(json).getAsJsonArray();
			JsonObject obj = arr.get(arr.size() - 1).getAsJsonObject();
			if(obj == null || obj.isJsonNull())
				return null;
			JsonElement name = obj.get("name");
			if(name == null || name.isJsonNull())
				return null;
			
			String username = name.getAsString();
			if(username == null){
				
			}
			return name.getAsString();
		} catch (JsonParseException e){
			e.printStackTrace();
			return null;
		} catch(IOException e1) {
			e1.printStackTrace();
			return null;
		}
	}
	
	
	
	private static ArrayList<String> getWhitelistedPlayers(){
		String whitelistFilename = "whitelist.json";
		ArrayList<String> ret = new ArrayList<String>();
		try {
			String json = new String(Files.readAllBytes(Paths.get(whitelistFilename)));
			JsonParser parser = new JsonParser();
			JsonArray arr = parser.parse(json).getAsJsonArray();
			for(JsonElement ele: arr){
				JsonObject obj = ele.getAsJsonObject();
				String uuid = obj.get("uuid").getAsString();
				ret.add(uuid);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return ret;
	}
}
