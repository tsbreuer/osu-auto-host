package lt.ekgame.autohost;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.json.*;

import lt.ekgame.bancho.api.packets.client.PacketMatchJoin;
import lt.ekgame.bancho.client.CommandExecutor;
import lt.ekgame.bancho.client.MultiplayerHandler;
import lt.ekgame.autohost.AutoHost;

public class CommandsGeneral implements CommandExecutor {
	
	private AutoHost bot;
	
	public CommandsGeneral(AutoHost bot) {
		this.bot = bot;
	}

	@Override
	public boolean accept(String channel, String sender) {
		return !channel.startsWith("#");
	}

	@Override
	public void handle(String channel, String sender, int userId, String label, List<String> args) {
		Settings settings = AutoHost.instance.settings;
		if (label == null){
			System.out.println("Null Call");
			bot.bancho.sendMessage(sender, sender + ": My account is currently being run by a bot. If you want to talk with me PM me on my userpage ;)");
			return;
		}
		
		if (label.equals("isop")) {
			String response = "You are" + (bot.perms.isOperator(userId) ? "" : " not") + " an operator.";
			bot.bancho.sendMessage(sender, response);
		}
		else if (label.equals("cookie")) {
			String response = "Saddly this game hasnt got a cookie emoji ¯\\_(O.O)_/¯";
			bot.bancho.sendMessage(sender, response);
		}
		
		else if (label.equals("join") && args.size()>0){
			PacketMatchJoin packet = new PacketMatchJoin();
			String password = "";
			
				if (args.size()>1)
					password = args.get(1);
				
			int id = Integer.valueOf(args.get(0));
			packet.matchId = id;
			packet.password = password;
			 bot.bancho.sendPacket(packet);
		}

		else if (label.equals("op")) {
			if (!bot.perms.isOperator(userId)) {
				bot.bancho.sendMessage(sender, "Insufficient permissions.");
			}
			else if (args.size() == 0) {
				bot.bancho.sendMessage(sender, "You must specify a user ID.");
			}
			else {
				try {
					int id = Integer.parseInt(args.get(0));
					boolean result = bot.perms.addOperator(id);
					if (result)
						bot.bancho.sendMessage(sender, "User #" + id + " is now an operator.");
					else
						bot.bancho.sendMessage(sender, "User #" + id + " is already an operator.");
				}
				catch (Exception e) {
					bot.bancho.sendMessage(sender, "The user ID \"" + args.get(0) + "\" is invalid.");
				}
			}
		}
		if (label.equals("createroom") && bot.perms.isOperator(userId)) {
			System.out.println("Creating room");
			MultiplayerHandler mp = bot.bancho.getMultiplayerHandler();
			mp.enableMultiplayer();
			String roomName = "Test room";
			if (args.size() != 0) {
				roomName = args.stream().map(i -> i).collect(Collectors.joining(" "));
			}
			mp.createRoom(roomName, null, 16);
		}
		else if (label.equals("help") || label.equals("info")) {
			bot.bancho.sendMessage(sender, AutoHost.instance.settings.helpText);
		}
		else if (label.equals("pm") && args.size()>1) {
			String search = "";
			for (int i=1; i < args.size(); i++) {
				search = search + " " + args.get(i);
			};
			bot.bancho.sendMessage(args.get(0), search);
		}
		else if (label.equals("request") && args.size() > 0) {
			/*
	        for(int i = 0; i < args.size() ; i++) {
	        	bot.bancho.sendMessage(sender, ""+args.get(i));;
	        }
	        */
			
			try { 
			RequestConfig defaultRequestConfig = RequestConfig.custom()
				    .setSocketTimeout(10000)
				    .setConnectTimeout(10000)
				    .setConnectionRequestTimeout(10000)
				    .build();
			HttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(defaultRequestConfig).build();
			String search = args.get(0);
			for (int i=1; i < args.size(); i++) {
				search = search + " " + args.get(i);
			};
			URI uri = new URIBuilder()
					.setScheme("http")
					.setHost("osusearch.com")
					.setPath("/query/")
					.setParameter("title", search)
					.setParameter("statuses", "Ranked")
					.setParameter("modes", "Standard")
					.setParameter("order", "play_count")
					.setParameter("star", "( "+ settings.minDifficulty + "," + settings.maxDifficulty + ")")
					.build(); 
			//bot.bancho.sendMessage(sender, search);
			//settings.minDifficulty settings.maxDifficulty
			//System.out.println(uri);
			HttpGet request = new HttpGet(uri);
			HttpResponse response = httpClient.execute(request);
			InputStream content = response.getEntity().getContent();
			String stringContent = IOUtils.toString(content, "UTF-8");
			JSONObject obj = new JSONObject(stringContent);
			JSONArray Info = obj.getJSONArray("beatmaps");
			//Beatmap Maps = gson.fromJson(stringContent);
			int size = 0;
			for (int i=0; i < Info.length(); i++) {
				//System.out.println( ""+Info.get(i));
				size = size + 1;
			};
			//bot.bancho.sendMessage(sender, ""+size);
			if ( size > 1 ) {
				if (size > 3) {
				bot.bancho.sendMessage(sender,"Found "+size+" maps, please be more precise!");
				} else if (size < 4) {
					bot.bancho.sendMessage(sender,"Please retry being more specific from the one of the following maps:");
					String returnMaps = "";
					for (int i=0; i < Info.length(); i++) {
						String str = ""+Info.get(i);
						JSONObject beatmap = new JSONObject(str);
						String artist = beatmap.getString("artist");
						String title = beatmap.getString("title");
						returnMaps = returnMaps+" || "+artist+" - "+title; 
					};
					bot.bancho.sendMessage(sender,returnMaps);
				}
			}			
			else if (size == 1) {
				//bot.bancho.sendMessage(sender, "Correct!");
				//int result = Info.getInt(1);
				String str = ""+Info.get(0);
				JSONObject beatmap = new JSONObject(str);
				String artist = beatmap.getString("artist");
				String title = beatmap.getString("title");
				String difficulty = beatmap.getString("difficulty_name");
				String rating = BigDecimal.valueOf(Math.round( (beatmap.getDouble("difficulty")*100d) )/100d).toPlainString();
				int bID = beatmap.getInt("beatmap_id");
				String result = artist + " - " + title + " [ "+difficulty+" ] - [ "+rating+"* ]";
				String result2 = "Link: "+"http://osu.ppy.sh/b/"+bID;
				bot.bancho.sendMessage(sender,result + " || " + result2);
 
			}
			else if (size < 1){
					bot.bancho.sendMessage(sender, "No beatmaps found!");
			}

			//bot.bancho.sendMessage(sender, result);
			} catch ( JSONException | URISyntaxException | IOException e) {
				e.printStackTrace();
				bot.bancho.sendMessage(sender, sender + ": Error");
			}
		}
	}
}
