package lt.ekgame.autohost;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import lt.ekgame.autohost.plugins.TableRenderer;
import lt.ekgame.bancho.api.packets.Packet;
import lt.ekgame.bancho.api.packets.client.PacketSlotLock;
import lt.ekgame.bancho.api.packets.client.PacketUpdateRoom;
import lt.ekgame.bancho.api.packets.server.PacketRoomEveryoneFinished;
import lt.ekgame.bancho.api.packets.server.PacketRoomJoined;
import lt.ekgame.bancho.api.packets.server.PacketRoomScoreUpdate;
import lt.ekgame.bancho.api.packets.server.PacketRoomUpdate;
import lt.ekgame.bancho.api.units.Beatmap;
import lt.ekgame.bancho.api.units.MultiplayerRoom;
import lt.ekgame.bancho.client.MultiplayerHandler;
import lt.ekgame.bancho.client.PacketHandler;
import lt.ekgame.beatmap_analyzer.calculator.Difficulty;
import lt.ekgame.beatmap_analyzer.calculator.Performance;
import lt.ekgame.beatmap_analyzer.calculator.PerformanceCalculator;
import lt.ekgame.beatmap_analyzer.parser.BeatmapException;
import lt.ekgame.beatmap_analyzer.parser.BeatmapParser;
import lt.ekgame.beatmap_analyzer.utils.Mod;
import lt.ekgame.beatmap_analyzer.utils.Mods;
import lt.ekgame.beatmap_analyzer.utils.ScoreVersion;

public class RoomHandler implements PacketHandler {
	
	public AutoHost bot;
	public double scores[][] = new double[16][13];
	public double Prevscores[][] = new double[16][10];
	private int slotsTaken;
	public lt.ekgame.beatmap_analyzer.Beatmap currentBeatmap[] = new lt.ekgame.beatmap_analyzer.Beatmap[16];
	public String modList[] = new String[16];
	//public BeatmapParser parser;
	public TimerThread timer;
	private List<Integer> AFK = new ArrayList<>();
	private List<Integer> SkipMe = new ArrayList<>();
	private List<Integer> skipVotes = new ArrayList<>();
	private List<Integer> startVotes = new ArrayList<>();
	Map<Integer, String> usernames = new HashMap<>();

	public int getId(String name){
		int id = 0;
		for (Map.Entry<Integer, String> entry : usernames.entrySet()) {
			  if (entry.getValue().equals(name))			  
			  {
				  id = entry.getKey();
			  }
			}
		if (id == 0)
		{
			try {
				RequestConfig defaultRequestConfig = RequestConfig.custom()
					    .setSocketTimeout(10000)
					    .setConnectTimeout(10000)
					    .setConnectionRequestTimeout(10000)
					    .build();
				HttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(defaultRequestConfig).build();
				URI uri = new URIBuilder()
						.setScheme("http")
						.setHost("osu.ppy.sh")
						.setPath("/api/get_user")
						.setParameter("k", AutoHost.instance.settings.osuApi)
						.setParameter("u", ""+name)
						.setParameter("type", "string")
						.build(); 
				HttpGet request = new HttpGet(uri);
				HttpResponse response = httpClient.execute(request);
				InputStream content = response.getEntity().getContent();
				String stringContent = IOUtils.toString(content, "UTF-8");
				JSONArray array = new JSONArray(stringContent);
				id = array.getJSONObject(0).getInt("user_id");	
					} catch (URISyntaxException | IOException e) {
					e.printStackTrace();
				}
		}
			   
			return id;
	}
	public String getUsername(int userId) {
	  if (usernames.containsKey(userId) && (!usernames.get(userId).equals("")) )
	    return usernames.get(userId);
	  
	  String username = ""; // get username with api
		try {
		RequestConfig defaultRequestConfig = RequestConfig.custom()
			    .setSocketTimeout(10000)
			    .setConnectTimeout(10000)
			    .setConnectionRequestTimeout(10000)
			    .build();
		HttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(defaultRequestConfig).build();
		URI uri = new URIBuilder()
				.setScheme("http")
				.setHost("osu.ppy.sh")
				.setPath("/api/get_user")
				.setParameter("k", AutoHost.instance.settings.osuApi)
				.setParameter("u", ""+userId)
				.setParameter("type", "id")
				.build(); 
		HttpGet request = new HttpGet(uri);
		HttpResponse response = httpClient.execute(request);
		InputStream content = response.getEntity().getContent();
		String stringContent = IOUtils.toString(content, "UTF-8");
		JSONArray array = new JSONArray(stringContent);
			if (array.length()>0)
				username = array.getJSONObject(0).getString("username");	
			} catch (URISyntaxException | JSONException | IOException e) {
			e.printStackTrace();
		}
	  usernames.put(userId, username);
	  return username;
	} 
	
	public RoomHandler(AutoHost bot) {
		this.bot = bot;
	}
	
	public void registerVoteSkip(int userId, String userName) {
		if (!skipVotes.contains(userId)) {
			skipVotes.add(userId);
			bot.bancho.sendMessage("#multiplayer", userName+" Voted for skipping the song! ("+skipVotes.size()+"/"+ Math.round(slotsTaken * 0.5)+")" );
			lt.ekgame.autohost.plugins.DiscordAPI.sendMessage("```Markdown\n#multiplayer - HyPeX: "+userName+" Voted for skipping the song! ("+skipVotes.size()+"/"+ Math.round(slotsTaken * 0.5)+")"+"```");
		}
		if (skipVotes.size() >= (Math.round(slotsTaken * 0.5))) {
			MultiplayerHandler mp = bot.bancho.getMultiplayerHandler();
			bot.bancho.sendMessage("#multiplayer", "The beatmap was voted off.");
			lt.ekgame.autohost.plugins.DiscordAPI.sendMessage("```Markdown\n#multiplayer - HyPeX: The beatmap was voted off."+"```");
			mp.setBeatmap(bot.beatmaps.nextBeatmap());
			onBeatmapChange();
		    
		}
	}

	public void registerVoteStart(int userId, String userName) {
		if (!startVotes.contains(userId)) {
			startVotes.add(userId);
			bot.bancho.sendMessage("#multiplayer", userName+" is ready! ("+ startVotes.size() +"/"+ Math.round(slotsTaken*0.75) +")" );
		}
		if ((double)startVotes.size() >= ((double) Math.round(slotsTaken * 0.75))) {
		    bot.bancho.sendMessage("#multiplayer", "Game started by vote.");
			startGame();
			timer.skipEvents();
		}
	}
	public void resetVotestart() {
		startVotes.clear();
	}
	
	public void resetVoteSkip() {
		skipVotes.clear();
	}
	
	public boolean isAFK(int ID) {
		if (AFK.contains(ID)){
		return true;
		}
		return false;
	}
	
	public boolean setAFK(int ID) {
		if (AFK.contains(ID)){
			return false;
		}
		else {
			AFK.add(ID);
			return true;
		}
	}
	
	
	public boolean setSkip(int ID) {
		if (SkipMe.contains(ID)){
			return false;
		}
		else {
			SkipMe.add(ID);
			return true;
		}
	}
	
	public boolean removeAFK(int ID) {
		if (AFK.contains(ID)){
			for (int i=0; i < AFK.size(); i++)
			{
			if (AFK.get(i) == ID)
				AFK.remove(i);
			}
			return true;
		}
		else {		
			return true;
		}
	}
	
	
	public void noMoreBeatmaps() {
		Beatmap beatmap = AutoHost.instance.beatmaps.completed.iterator().next();
		AutoHost.instance.beatmaps.completed.remove(beatmap);
		bot.beatmaps.push(beatmap);
		bot.roomHandler.onBeatmapAdded(beatmap, beatmap.getId());
	}

    public void editSlot(int slot)
    {
        bot.bancho.sendPacket(new PacketSlotLock(slot));    
        bot.bancho.sendMessage("#multiplayer","Toggled lock on slot ");    
    }
	public void onBeatmapChange() {
		resetVoteSkip();
		timer.resetTimer();
		long ssNOMOD = 0;
		long ssHIDDEN = 0;
		long ssHR = 0;
		long ssHDHR = 0;
		Beatmap beatmap = bot.beatmaps.getBeatmap();
		if (beatmap == null) {
			bot.bancho.sendMessage("#multiplayer", "No more beatmaps in the queue. Use !add [link to beatmap] to add more beatmaps. Selecting the oldest beatmap played.");
			noMoreBeatmaps();
		} else {
			if (beatmap.getDT()){
				if (!isDTEnabled) {
				MultiplayerHandler mp = bot.bancho.getMultiplayerHandler();
				mp.setMods(new Mods(Mod.DOUBLE_TIME).getFlags());
				isDTEnabled = true;
				bot.bancho.sendMessage("#multiplayer", "Double Time Enabled for this beatmap");
			    }
				}else{
				if (isDTEnabled){
				MultiplayerHandler mp = bot.bancho.getMultiplayerHandler();
				mp.setMods(0);
				isDTEnabled = false;
				bot.bancho.sendMessage("#multiplayer", "Double Time Disabled for this beatmap");
				}
			}
			if (beatmap.getHT()) {
				if (!isHTEnabled){
					MultiplayerHandler mp = bot.bancho.getMultiplayerHandler();
					mp.setMods(new Mods(Mod.HALF_TIME).getFlags());
					isHTEnabled = true;
					bot.bancho.sendMessage("#multiplayer", "Half Time Enabled for this beatmap");
				}
			}else{
				if (isHTEnabled){
					MultiplayerHandler mp = bot.bancho.getMultiplayerHandler();
					mp.setMods(0);
					isHTEnabled = false;
					bot.bancho.sendMessage("#multiplayer", "Half Time Disabled for this beatmap");
				}
			}
			
			  try {
					RequestConfig defaultRequestConfig = RequestConfig.custom()
						    .setSocketTimeout(10000)
						    .setConnectTimeout(10000)
						    .setConnectionRequestTimeout(10000)
						    .build();
					
					HttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(defaultRequestConfig).build();
					URI uri = new URIBuilder()
							.setScheme("http")
							.setHost("osu.ppy.sh")
							.setPath("/osu/"+beatmap.getId())
							.build();
					HttpGet request = new HttpGet(uri);
					HttpResponse response = httpClient.execute(request);
					InputStream content = response.getEntity().getContent();
					//String stringContent = IOUtils.toString(content, "UTF-8");
					BeatmapParser parser = new BeatmapParser();
					lt.ekgame.beatmap_analyzer.Beatmap cbp = parser.parse(content);
					lt.ekgame.beatmap_analyzer.Beatmap cbp1 = null;
					lt.ekgame.beatmap_analyzer.Beatmap cbp2 = null;
					lt.ekgame.beatmap_analyzer.Beatmap cbp3 = null;
					lt.ekgame.beatmap_analyzer.Beatmap cbp4 = null;
					if (isDTEnabled)
					{
						cbp1 = cbp.applyMods(new Mods(Mod.DOUBLE_TIME));	
						Arrays.fill(currentBeatmap, cbp);
						cbp2 = cbp.applyMods(new Mods(Mod.HIDDEN,Mod.DOUBLE_TIME));
						cbp3 = cbp.applyMods(new Mods(Mod.HARDROCK,Mod.DOUBLE_TIME));
				  		cbp4 = cbp.applyMods(new Mods(Mod.HIDDEN,Mod.HARDROCK,Mod.DOUBLE_TIME));
					}
					
					if (isHTEnabled){
						Arrays.fill(currentBeatmap, cbp);
						cbp1 = cbp.applyMods(new Mods(Mod.HALF_TIME));
						cbp2 = cbp.applyMods(new Mods(Mod.HIDDEN,Mod.HALF_TIME));
						cbp3 = cbp.applyMods(new Mods(Mod.HARDROCK,Mod.HALF_TIME));
				  		cbp4 = cbp.applyMods(new Mods(Mod.HIDDEN,Mod.HARDROCK,Mod.HALF_TIME));
					}

					if (!isHTEnabled && !isDTEnabled){
						Arrays.fill(currentBeatmap, cbp);
						cbp1 = cbp.applyMods(new Mods());
						cbp2 = cbp.applyMods(new Mods(Mod.HIDDEN));
						cbp3 = cbp.applyMods(new Mods(Mod.HARDROCK));
				  		cbp4 = cbp.applyMods(new Mods(Mod.HIDDEN,Mod.HARDROCK));
					}
					Performance perf = cbp1.getPerformance(cbp.getMaxCombo(),0,0,0);
					ssNOMOD = ((long) perf.getPerformance());
					beatmap.difficulty = ""+round(cbp.getDifficulty().getStarDifficulty(),2);

					Performance perf2 = cbp2.getPerformance(cbp2.getMaxCombo(),0,0,0);
					ssHIDDEN = ((long) perf2.getPerformance());
					Performance perf3 = cbp3.getPerformance(cbp3.getMaxCombo(),0,0,0);
					ssHR = ((long) perf3.getPerformance());
					Performance perf4 = cbp4.getPerformance(cbp4.getMaxCombo(),0,0,0);
					ssHDHR = ((long) perf4.getPerformance());
					
			  }	catch ( JSONException | IOException | URISyntaxException | BeatmapException e) {
					e.printStackTrace();
					bot.bancho.sendMessage("#multiplayer", "Error Parsing beatmap");
		 }
				if (isDTEnabled){
				String result2 = "[http://osu.ppy.sh/b/"+beatmap.getId()+" Link]";
				bot.bancho.sendMessage("#multiplayer", String.format("Up Next DT %s - %s [%s] - [ %s* ] || "+ result2 +" || Mapped by %s",					
				beatmap.getArtist(), beatmap.getTitle(), beatmap.getVersion(), beatmap.getDiff(), beatmap.getCreator()));				
				bot.bancho.sendMessage("#multiplayer","DT: "+ssNOMOD+"pp || DTHD: "+ssHIDDEN+"pp || DTHR: "+ssHR+"pp || DTHDHR: "+ssHDHR+"pp");
				lt.ekgame.autohost.plugins.DiscordAPI.sendMessage("```Markdown\nUp Next DT "+beatmap.getArtist()+" - "+beatmap.getTitle()+" ["+beatmap.getVersion()+"] - [ "+beatmap.getDiff()+"* ] || "+"[link](http://osu.ppy.sh/b/"+beatmap.getId()+") || Mapped by "+beatmap.getCreator()+"```");			
				}
				if (isHTEnabled){
				String result2 = "[http://osu.ppy.sh/b/"+beatmap.getId()+" Link]";
				bot.bancho.sendMessage("#multiplayer", String.format("Up Next HT %s - %s [%s] - [ %s* ] || "+ result2 +" || Mapped by %s",					
				beatmap.getArtist(), beatmap.getTitle(), beatmap.getVersion(), beatmap.getDiff(), beatmap.getCreator()));				
				bot.bancho.sendMessage("#multiplayer","DT: "+ssNOMOD+"pp || DTHD: "+ssHIDDEN+"pp || DTHR: "+ssHR+"pp || DTHDHR: "+ssHDHR+"pp");
				lt.ekgame.autohost.plugins.DiscordAPI.sendMessage("```Markdown\nUp Next HT "+beatmap.getArtist()+" - "+beatmap.getTitle()+" ["+beatmap.getVersion()+"] - [ "+beatmap.getDiff()+"* ] || "+"[link](http://osu.ppy.sh/b/"+beatmap.getId()+") || Mapped by "+beatmap.getCreator()+"```");		
				}
				
				if (!isHTEnabled && !isDTEnabled){ 
				String result2 = "[http://osu.ppy.sh/b/"+beatmap.getId()+" Link]";
				bot.bancho.sendMessage("#multiplayer", String.format("Up Next %s - %s [%s] - [ %s* ] || "+ result2 +" || Mapped by %s",					
				beatmap.getArtist(), beatmap.getTitle(), beatmap.getVersion(), beatmap.getDiff(), beatmap.getCreator()));
				bot.bancho.sendMessage("#multiplayer","NOMOD: "+ssNOMOD+"pp || HD: "+ssHIDDEN+"pp || HR: "+ssHR+"pp || HDHR: "+ssHDHR+"pp");
				lt.ekgame.autohost.plugins.DiscordAPI.sendMessage("```Markdown\nUp Next "+beatmap.getArtist()+" - "+beatmap.getTitle()+" ["+beatmap.getVersion()+"] - [ "+beatmap.getDiff()+"* ] || "+"[link](http://osu.ppy.sh/b/"+beatmap.getId()+") || Mapped by "+beatmap.getCreator()+"```");		
				}
			  

		}
	}


	@Override
	public void handle(Packet packet) {
		MultiplayerHandler mp = bot.bancho.getMultiplayerHandler();
		
		if (AutoHost.instance.settings.freemodsEnabled)
			if (!mp.isFreeModsEnabled())
			bot.bancho.getMultiplayerHandler().setFreeMods(true);
		
		if (!AutoHost.instance.settings.freemodsEnabled)
			if (mp.isFreeModsEnabled())
			bot.bancho.getMultiplayerHandler().setFreeMods(false);
		
		if (packet instanceof PacketRoomJoined) {
			bot.beatmaps.reset();
			mp.setBeatmap(bot.beatmaps.nextBeatmap());
			timer = new TimerThread(this);
			timer.start();
		}
		
		if (packet instanceof PacketRoomScoreUpdate) {
			  PacketRoomScoreUpdate update = (PacketRoomScoreUpdate) packet;
			  Beatmap current = bot.beatmaps.getBeatmap();
			  int userID = mp.getRoom().slotId[update.byte1];
			  int bID = current.getId();

			  try {
				  //lt.ekgame.beatmap_analyzer.Beatmap beatmap = currentBeatmap[update.byte1];
				  //List<Mod> mods = Mod.getMods(mp.getRoom().slotMods[update.byte1]);
				  
			  int MaxComboPossible = currentBeatmap[update.byte1].getMaxCombo();
			  int MaxComboHit = update.short7;
			  int accok = update.short1;
			  int accmeh = update.short2;
			  int accf = update.short3;
			  int accmiss = update.short6;
			  int totalhits = accok+accmeh+accf+accmiss;
			  double acc = ((accok*6 + accmeh*2 + accf)/((double)totalhits*6));
			  Performance perf =  currentBeatmap[update.byte1].getPerformance(MaxComboHit,accmeh,accf,accmiss);
			  
			  //System.out.println(mp.getRoom().slotMods[update.byte1]); // 0-NOMOD NF-1 8-HD 16-HR 24-HDHR 
				for (int i = 0; i < 16; i++)  {
					if (scores[i][0] == ((int) mp.getRoom().slotId[update.byte1])){
					Arrays.fill(scores[i],0);
					};				
				}
			  
			  scores[update.byte1][0] = mp.getRoom().slotId[update.byte1];
			  scores[update.byte1][1] = update.integer2;
			  scores[update.byte1][2] = update.byte1;
			  scores[update.byte1][3] = acc;
			  scores[update.byte1][4] = perf.getPerformance();
			  scores[update.byte1][5] = accok;
			  scores[update.byte1][6] = accmeh;
			  scores[update.byte1][7] = accf;
			  scores[update.byte1][8] = accmiss;
			  scores[update.byte1][9] = MaxComboHit;
			  scores[update.byte1][10] = MaxComboPossible;
			  scores[update.byte1][11] = mp.getRoom().slotMods[update.byte1];
			  scores[update.byte1][12] = update.short8;
			  
			  Arrays.sort(scores, new Comparator<double[]>() {				  
				  public int compare(double[] o1, double[] o2) { 
					    return Double.compare(o2[1], o1[1]);
					}
				});
			  //System.out.println(""+update.integer1); Time passed in ms
			  //System.out.println("byte1: "+update.byte1); // Lobby Slot
			  //System.out.println("short1: "+update.short1); // Correct x300s
			  //System.out.println("short2: "+update.short2); // x100s
			  //System.out.println("short3: "+update.short3); // x50s
			  //System.out.println("short4: "+update.short4); // Extra x300s
			  //System.out.println("short5: "+update.short5); // Extra x100s 
			  //System.out.println("short6: "+update.short6); // Misses
			  //System.out.println("short7: "+update.short7); // Max Combo
			  //System.out.println("short8: "+update.short8); // Current Combo
			  //System.out.println("integer2: "+update.integer2); // Score
			  //System.out.println("boolean1: "+update.boolean1); // False always..
			  //System.out.println("byte2: "+update.byte2); // Drain
			  //System.out.println("byte3: "+update.byte3); // 1-6?
			  //System.out.println("boolean2: "+update.boolean2); // Alive
			  }	catch ( JSONException /*| BeatmapException*/ e) {
					e.printStackTrace();
					bot.bancho.sendMessage("#multiplayer", "Error Parsing beatmap");
		}
		}
		
		if (packet instanceof PacketRoomUpdate && mp.isHost()) {
			PacketRoomUpdate update = (PacketRoomUpdate) packet;
			if (update.room.matchId == mp.getMatchId()) {
				byte[] status = update.room.slotStatus;
				String statuses = "";
				slotsTaken = 0;
				int slotsReady = 0;
				for (int i = 0; i < 16; i++)  {
					statuses += status[i] + " ";
					if (status[i] != 1 && status[i] != 2) {
						if (update.room.slotId[i] != bot.bancho.getClientHandler().getUserId()) {
							if (!AFK.contains(mp.getRoom().slotId[i])){
								if (!SkipMe.contains(mp.getRoom().slotId[i])){
									slotsTaken++;
									if (status[i] == 8){
										slotsReady++;
									} else if (startVotes.contains(mp.getRoom().slotId[i])){
										slotsReady++;
									}
								}
							}
						}
					}
				}
				
				for (int i = 0; i < AFK.size(); i++)  {
					boolean isInLobby = false;
					for (int a = 0; a < 16; a++){
						if (status[a] != 1 && status[a] != 2) {
							if (update.room.slotId[a] == AFK.get(i)) {
							isInLobby = true;
							}
						}
					}
					if (!isInLobby)
						removeAFK(AFK.get(i));
				}
				
				//System.out.println(statuses);
				if (slotsTaken > 0 && slotsTaken == slotsReady) {
					startGame();
					timer.skipEvents();
				}
			}
		}
		
		if (packet instanceof PacketRoomEveryoneFinished) {
			resetVoteSkip();
			resetVotestart();
			
			int id = mp.getRoom().getBeatmap().getId();
			String name = mp.getRoom().getBeatmap().getName();
			String output = "```Markdown\nBeatmap ";
				if (mp.getRoom().getBeatmap().getDT())
					output = output+"DT ";
				if (mp.getRoom().getBeatmap().getHT())
					output = output+"HT ";
				
			output = output + "["+name+"](http://osu.ppy.sh/b/"+id+" Ended. \n\n";
			TableRenderer table = new TableRenderer();
			table.setHeader("Name", "Score", "Mods", "300/100/50/X", "Combo", "Acc", "PP");
			for (int i=0; i < Prevscores.length; i++)  {
				//1st Place hmax (Nice Choke) NOMOD 627/46/7/22 (843/1012) 91,67% PP:96,34
					Prevscores[i][0] = scores[i][0]; // Id
					Prevscores[i][1] = scores[i][11]; // Mods
					Prevscores[i][2] = scores[i][5]; // x300
					Prevscores[i][3] = scores[i][6]; // x100
					Prevscores[i][4] = scores[i][7]; // x50
					Prevscores[i][5] = scores[i][8]; // x0
					Prevscores[i][6] = scores[i][9]; // Combo
					Prevscores[i][7] = scores[i][10]; // MaxCombo
					Prevscores[i][8] = scores[i][3]; // Acc
					Prevscores[i][9] = scores[i][4]; // PP
					int score = (int) scores[i][1];
					if (score>0)
							table.addRow(
							getUsername((int) Math.round(scores[i][0])),
							score,
							getMods((int) Prevscores[i][1]),
							(int) Prevscores[i][2]+"/"+ (int) Prevscores[i][3]+"/"+ (int) Prevscores[i][4]+"/"+ (int)Prevscores[i][5],
							"("+ (int) Prevscores[i][6]+"/"+ (int) Prevscores[i][7]+")",
							String.format("%.02f", Prevscores[i][8]*100)+"%",
							String.format("%.02f", Prevscores[i][9])
							);
			}
			output=output+table.build()+"```";
			mp.setBeatmap(bot.beatmaps.nextBeatmap());
			onBeatmapChange();
			String IDs[] = new String[4];
			//mp.getRoom().openSlots(2);
			byte[] status = mp.getRoom().slotStatus;
			lt.ekgame.autohost.plugins.DiscordAPI.sendMessage(output);
			SkipMe.clear();

			if (scores[0][1] != 0){
				if  ((int) (scores[0][0]) != 0)
					if (scores[0][9]>=(scores[0][10]*0.8) && scores[0][12]<(scores[0][9])){
				bot.bancho.sendMessage("#multiplayer", "1st Place "+ getUsername( (int) (scores[0][0])) +" (Nice Choke) "+ getMods((int) scores[0][11]) +" "+ (int) scores[0][5]+"/"+ (int) scores[0][6]+"/"+ (int) scores[0][7]+"/"+ (int) scores[0][8]+" ("+ (int) scores[0][9]+"/"+ (int) scores[0][10]+") "+ String.format("%.02f", scores[0][3]*100)+"% PP:"+ String.format("%.02f", scores[0][4]));				
					} else if (scores[0][9]<=(scores[0][10]*0.2)) {
						bot.bancho.sendMessage("#multiplayer", "1st Place (Nice combo bro) "+ getUsername( (int) (scores[0][0])) +" "+ getMods((int) scores[0][11]) +" "+ (int) scores[0][5]+"/"+ (int) scores[0][6]+"/"+ (int) scores[0][7]+"/"+ (int) scores[0][8]+" ("+ (int) scores[0][9]+"/"+ (int) scores[0][10]+") "+ String.format("%.02f", scores[0][3]*100)+"% PP:"+ String.format("%.02f", scores[0][4]));				
					}
					else if (scores[0][9]>=(scores[0][10]*0.9)) {
						bot.bancho.sendMessage("#multiplayer", "1st Place (Dat FC 5/7) "+ getUsername( (int) (scores[0][0])) +" "+ getMods((int) scores[0][11]) +" "+ (int) scores[0][5]+"/"+ (int) scores[0][6]+"/"+ (int) scores[0][7]+"/"+ (int) scores[0][8]+" ("+ (int) scores[0][9]+"/"+ (int) scores[0][10]+") "+ String.format("%.02f", scores[0][3]*100)+"% PP:"+ String.format("%.02f", scores[0][4]));				
					} else{
						bot.bancho.sendMessage("#multiplayer", "1st Place "+ getUsername( (int) (scores[0][0])) +" "+ getMods((int) scores[0][11]) +" "+ (int) scores[0][5]+"/"+ (int) scores[0][6]+"/"+ (int) scores[0][7]+"/"+ (int) scores[0][8]+" ("+ (int) scores[0][9]+"/"+ (int) scores[0][10]+") "+ String.format("%.02f", scores[0][3]*100)+"% PP:"+ String.format("%.02f", scores[0][4]));				
					}
					}
			if (scores[1][1] != 0){
				if  ((int) (scores[1][0]) != 0)
					if (scores[0][9]>=(scores[0][10]*0.9)){
						bot.bancho.sendMessage("#multiplayer", "2nd Place "+ getUsername( (int) (scores[1][0])) +" (rip fc & 2nd) "+ getMods((int) scores[1][11])+" "+ (int) scores[1][5]+"/"+ (int) scores[1][6]+"/"+ (int) scores[1][7]+"/"+ (int) scores[1][8]+" ("+ (int) scores[1][9]+"/"+ (int) scores[1][10]+") "+ String.format("%.02f", scores[1][3]*100)+"% PP:"+ String.format("%.02f", scores[1][4]));			
					} else
					{
						bot.bancho.sendMessage("#multiplayer", "2nd Place "+ getUsername( (int) (scores[1][0])) +"  "+ getMods((int) scores[1][11])+" "+ (int) scores[1][5]+"/"+ (int) scores[1][6]+"/"+ (int) scores[1][7]+"/"+ (int) scores[1][8]+" ("+ (int) scores[1][9]+"/"+ (int) scores[1][10]+") "+ String.format("%.02f", scores[1][3]*100)+"% PP:"+ String.format("%.02f", scores[1][4]));			
					}
				}

			if (scores[2][1] != 0){
				if  ((int) (scores[2][0]) != 0)
			bot.bancho.sendMessage("#multiplayer", "3rd Place "+ getUsername( (int) (scores[2][0])) +" "+ getMods((int) scores[2][11])+" "+ (int) scores[2][5]+"/"+ (int) scores[2][6]+"/"+ (int) scores[2][7]+"/"+ (int) scores[2][8]+" ("+ (int) scores[2][9]+"/"+ (int) scores[2][10]+") "+ String.format("%.02f", scores[2][3]*100)+"% PP:"+ String.format("%.02f", scores[2][4]));
			}
		}
	}
	
	public String getLastScore(int userId){
		String returnString = "";
		boolean playedLast = false;
		for (int i=0; i < 10; i++)  {
			if ( (int) ((Prevscores[i][0])) == userId) {
				playedLast = true;
				returnString = ""+getUsername((int) Math.round(Prevscores[i][0])) +" "+ getMods((int) Prevscores[i][1]) +" "+ (int) Prevscores[i][2]+"/"+ (int) Prevscores[i][3]+"/"+ (int) Prevscores[i][4]+"/"+ (int) Prevscores[i][5]+" ("+ (int) Prevscores[i][6]+"/"+ (int) Prevscores[i][7]+") "+ String.format("%.02f", Prevscores[i][8]*100)+"% PP:"+ String.format("%.02f", Prevscores[i][9]);
				break;
			}
		}
			if (!playedLast)
				returnString = "You didnt play last map!";
				
		return returnString;
	}
	
	public void editSlot(int slot, int slotType)
	{
		MultiplayerHandler mp = bot.bancho.getMultiplayerHandler();
		mp.getRoom().openSlots = 5;
		bot.bancho.sendPacket(new PacketUpdateRoom(mp.getRoom()));	
		bot.bancho.sendMessage("#multiplayer","edited slot");	
	}
	 public boolean isDTEnabled = false;
	 public boolean isHTEnabled = false;
	public void editMods()
	{
		if (isDTEnabled){
			MultiplayerHandler mp = bot.bancho.getMultiplayerHandler();
			mp.setMods(0);
			bot.bancho.sendPacket(new PacketUpdateRoom(mp.getRoom()));	
			bot.bancho.sendMessage("#multiplayer","Double Time Disabled");	
			lt.ekgame.autohost.plugins.DiscordAPI.sendMessage("```Markdown\n#multiplayer - HyPeX: Double Time Disabled.");
			isDTEnabled = false;
		}else{		
		MultiplayerHandler mp = bot.bancho.getMultiplayerHandler();
		mp.setMods(new Mods(Mod.DOUBLE_TIME).getFlags());
		bot.bancho.sendPacket(new PacketUpdateRoom(mp.getRoom()));	
		bot.bancho.sendMessage("#multiplayer","Double Time Enabled");	
		lt.ekgame.autohost.plugins.DiscordAPI.sendMessage("```Markdown\n#multiplayer - HyPeX: Double Time Enabled.");
		isDTEnabled = true;
		
		}
	}
	public String getMods(int Mods)
	{

		  String[] modNames = {"NF", "EZ", "", "HD", "HR", "SD", "DT", "RX", "HT", "FL", "AP", "SO", "AO", "PF"};
		  String modsEnabled = "";
		  for (int i = 0; i < 14; i++) {
		      // stupid NC
		      if ((Mods & (int)Math.pow(2, i)) != 0) {
		          modsEnabled += modNames[i];
		      }
		  }
		  
		  if (modsEnabled.equals("")) 
		  { modsEnabled = "NOMOD"; }
		return modsEnabled;
	}
	public void startGame() {
		MultiplayerHandler mp = bot.bancho.getMultiplayerHandler();
		mp.setReady(true);
		mp.startGame();
		resetVotestart();
		try{
		for (int i = 0; i < scores.length; i++) 
		{
			Arrays.fill(scores[i],0);
			  byte[] status = mp.getRoom().slotStatus;
			  if (status[i] != 1) {				  
				  currentBeatmap[i] = currentBeatmap[i].applyMods(Mods.parse(mp.getRoom().slotMods[i]));				 
			  }
		}
		} catch ( NullPointerException | IllegalStateException e) {
			e.printStackTrace();
			bot.bancho.sendMessage("#multiplayer", "Error starting beatmap. Skipping");
			lt.ekgame.autohost.plugins.DiscordAPI.sendMessage("```Markdown\n#multiplayer - HyPeX: Error starting beatmap. Skipping"+"```");
			mp.setBeatmap(bot.beatmaps.nextBeatmap());
			onBeatmapChange();
		}
	}

	public void onBeatmapAdded(Beatmap beatmap, int bID) {
		
		BeatmapHandler beatmapHandler = AutoHost.instance.beatmaps;
		int size = beatmapHandler.queueSize();
		long ssNOMOD = 0;
		long ssHIDDEN = 0;
		long ssHR = 0;
		long ssHDHR = 0;
		
		if (bot.beatmaps.getBeatmap() == null) {
			timer.resetTimer();
			MultiplayerHandler mp = bot.bancho.getMultiplayerHandler();
			mp.setBeatmap(bot.beatmaps.nextBeatmap());
			if (beatmap.getDT()){
				if (!isDTEnabled){
				mp.setMods(new Mods(Mod.DOUBLE_TIME).getFlags());
				isDTEnabled = true;
				bot.bancho.sendMessage("#multiplayer", "Double Time Enabled for this beatmap");
				}
			}else{
				if (isDTEnabled){
				mp.setMods(0);
				isDTEnabled = false;
				bot.bancho.sendMessage("#multiplayer", "Double Time Disabled for this beatmap");
				}
			}
			if (beatmap.getHT()){
				if (!isHTEnabled){
					mp.setMods(new Mods(Mod.HALF_TIME).getFlags());
					isHTEnabled = true;
					bot.bancho.sendMessage("#multiplayer", "Half Time Enabled for this beatmap");
				}
			}else{
			if (isHTEnabled){
				isHTEnabled = false;
				mp.setMods(0);
				bot.bancho.sendMessage("#multiplayer", "Half Time Disabled for this beatmap");
			}
			}
			  try {
					RequestConfig defaultRequestConfig = RequestConfig.custom()
						    .setSocketTimeout(10000)
						    .setConnectTimeout(10000)
						    .setConnectionRequestTimeout(10000)
						    .build();
					
					HttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(defaultRequestConfig).build();
					URI uri = new URIBuilder()
							.setScheme("http")
							.setHost("osu.ppy.sh")
							.setPath("/osu/"+beatmap.getId())
							.build();
					HttpGet request = new HttpGet(uri);
					HttpResponse response = httpClient.execute(request);
					InputStream content = response.getEntity().getContent();
					//String stringContent = IOUtils.toString(content, "UTF-8");
					BeatmapParser parser = new BeatmapParser();
					lt.ekgame.beatmap_analyzer.Beatmap cbp = parser.parse(content);
					lt.ekgame.beatmap_analyzer.Beatmap cbp1 = null;
					lt.ekgame.beatmap_analyzer.Beatmap cbp2 = null;
					lt.ekgame.beatmap_analyzer.Beatmap cbp3 = null;
					lt.ekgame.beatmap_analyzer.Beatmap cbp4 = null;
					if (isDTEnabled)
					{
						Arrays.fill(currentBeatmap, cbp);
						cbp1 = cbp.applyMods(new Mods(Mod.DOUBLE_TIME));	
						cbp2 = cbp.applyMods(new Mods(Mod.HIDDEN,Mod.DOUBLE_TIME));
						cbp3 = cbp.applyMods(new Mods(Mod.HARDROCK,Mod.DOUBLE_TIME));
				  		cbp4 = cbp.applyMods(new Mods(Mod.HIDDEN,Mod.HARDROCK,Mod.DOUBLE_TIME));
					} 
					if (isHTEnabled){
						Arrays.fill(currentBeatmap, cbp);
						cbp1 = cbp.applyMods(new Mods(Mod.HALF_TIME));
						cbp2 = cbp.applyMods(new Mods(Mod.HIDDEN,Mod.HALF_TIME));
						cbp3 = cbp.applyMods(new Mods(Mod.HARDROCK,Mod.HALF_TIME));
				  		cbp4 = cbp.applyMods(new Mods(Mod.HIDDEN,Mod.HARDROCK,Mod.HALF_TIME));
					} 
					if (!isHTEnabled && !isDTEnabled) {
						Arrays.fill(currentBeatmap, cbp);
						cbp1 = cbp.applyMods(new Mods());
						cbp2 = cbp.applyMods(new Mods(Mod.HIDDEN));
						cbp3 = cbp.applyMods(new Mods(Mod.HARDROCK));
				  		cbp4 = cbp.applyMods(new Mods(Mod.HIDDEN,Mod.HARDROCK));
					}
					Performance perf = cbp1.getPerformance(cbp.getMaxCombo(),0,0,0);
					ssNOMOD = ((long) perf.getPerformance());
					beatmap.difficulty = ""+round(cbp.getDifficulty().getStarDifficulty(),2);
					
					Performance perf2 = cbp2.getPerformance(cbp2.getMaxCombo(),0,0,0);
					ssHIDDEN = ((long) perf2.getPerformance());
					Performance perf3 = cbp3.getPerformance(cbp3.getMaxCombo(),0,0,0);
					ssHR = ((long) perf3.getPerformance());
					Performance perf4 = cbp4.getPerformance(cbp4.getMaxCombo(),0,0,0);
					ssHDHR = ((long) perf4.getPerformance());
					
			  }	catch ( JSONException | IOException | NullPointerException | URISyntaxException | BeatmapException e) {
					e.printStackTrace();
					bot.bancho.sendMessage("#multiplayer", "Error Parsing beatmap");
		 }
				String result2 = "[http://osu.ppy.sh/b/"+bID+" Link]";
				if (beatmap.getDT()){					
					bot.bancho.sendMessage("#multiplayer", String.format("Selected DT %s - %s [%s] - [ %s* ] || "+ result2 +" || Mapped by %s",
					beatmap.getArtist(), beatmap.getTitle(), beatmap.getVersion(), beatmap.getDiff(), beatmap.getCreator()));		
					bot.bancho.sendMessage("#multiplayer","DT: "+ssNOMOD+"pp || DTHD: "+ssHIDDEN+"pp || DTHR: "+ssHR+"pp || DTHDHR: "+ssHDHR+"pp");
					lt.ekgame.autohost.plugins.DiscordAPI.sendMessage("```Markdown\nUp Next DT "+beatmap.getArtist()+" - "+beatmap.getTitle()+" ["+beatmap.getVersion()+"] - [ "+beatmap.getDiff()+"* ] || "+"[link](http://osu.ppy.sh/b/"+beatmap.getId()+") || Mapped by "+beatmap.getCreator()+"```");		
				}
				if (beatmap.getHT()){					
					bot.bancho.sendMessage("#multiplayer", String.format("Selected HT %s - %s [%s] - [ %s* ] || "+ result2 +" || Mapped by %s",
					beatmap.getArtist(), beatmap.getTitle(), beatmap.getVersion(), beatmap.getDiff(), beatmap.getCreator()));		
					bot.bancho.sendMessage("#multiplayer","HT: "+ssNOMOD+"pp || HTHD: "+ssHIDDEN+"pp || HTHR: "+ssHR+"pp || HTHDHR: "+ssHDHR+"pp");
					lt.ekgame.autohost.plugins.DiscordAPI.sendMessage("```Markdown\nUp Next HT "+beatmap.getArtist()+" - "+beatmap.getTitle()+" ["+beatmap.getVersion()+"] - [ "+beatmap.getDiff()+"* ] || "+"[link](http://osu.ppy.sh/b/"+beatmap.getId()+") || Mapped by "+beatmap.getCreator()+"```");	
				}

				if (!beatmap.getDT() && !beatmap.getHT()){
					bot.bancho.sendMessage("#multiplayer", String.format("Selected %s - %s [%s] - [ %s* ] || "+ result2 +" || Mapped by %s",
					beatmap.getArtist(), beatmap.getTitle(), beatmap.getVersion(), beatmap.getDiff(), beatmap.getCreator()));
					bot.bancho.sendMessage("#multiplayer","NOMOD: "+ssNOMOD+"pp || HD: "+ssHIDDEN+"pp || HR: "+ssHR+"pp || HDHR: "+ssHDHR+"pp");
					lt.ekgame.autohost.plugins.DiscordAPI.sendMessage("```Markdown\nUp Next "+beatmap.getArtist()+" - "+beatmap.getTitle()+" ["+beatmap.getVersion()+"] - [ "+beatmap.getDiff()+"* ] || "+"[link](http://osu.ppy.sh/b/"+beatmap.getId()+") || Mapped by "+beatmap.getCreator()+"```");		
				}
		} else{
		if (beatmap.getDT()){		
			String result2 = "[http://osu.ppy.sh/b/"+bID+" Link]";
			bot.bancho.sendMessage("#multiplayer", String.format("Queue'd ("+size+") DT %s - %s [%s] - [ %s* ] || "+ result2 +" || Mapped by %s",
			beatmap.getArtist(), beatmap.getTitle(), beatmap.getVersion(), beatmap.getDiff(), beatmap.getCreator()));		
		}
		if (beatmap.getHT()){
			String result2 = "[http://osu.ppy.sh/b/"+bID+" Link]";
			bot.bancho.sendMessage("#multiplayer", String.format("Queue'd ("+size+") HT %s - %s [%s] - [ %s* ] || "+ result2 +" || Mapped by %s",
			beatmap.getArtist(), beatmap.getTitle(), beatmap.getVersion(), beatmap.getDiff(), beatmap.getCreator()));		
		}
		if (!beatmap.getDT() && !beatmap.getHT()){
			String result2 = "[http://osu.ppy.sh/b/"+bID+" Link]";
			bot.bancho.sendMessage("#multiplayer", String.format("Queue'd ("+size+") %s - %s [%s] - [ %s* ] || "+ result2 +" || Mapped by %s",
			beatmap.getArtist(), beatmap.getTitle(), beatmap.getVersion(), beatmap.getDiff(), beatmap.getCreator()));
		}
		}
	}
	
	public void tryStart() {
		MultiplayerHandler mp = bot.bancho.getMultiplayerHandler();
		MultiplayerRoom room = mp.getRoom();
		byte[] status = room.slotStatus;
		slotsTaken = 0;
		int slotsReady = 0;
		String statuses = "";
		for (int i = 0; i < 16; i++)  {
			statuses += status[i] + " ";
			if (status[i] != 1 && status[i] != 2) {
				if (room.slotId[i] != bot.bancho.getClientHandler().getUserId()) {
					if (!AFK.contains(mp.getRoom().slotId[i])){
					slotsTaken++;
					if (status[i] == 8)
						slotsReady++;						
					}
					
				}
			}
		}
		
		System.out.println(statuses);
		if (slotsTaken > 0 && ((double)slotsReady)/((double)slotsTaken) > 0.7) {
			bot.bancho.sendMessage("#multiplayer", String.format("%d/%d people are ready - starting the game.", slotsReady, slotsTaken));
			startGame();
		} else {
			if (slotsTaken == 0) {
				bot.bancho.sendMessage("#multiplayer", "Lobby is empty, skipping current beatmap.");
				lt.ekgame.autohost.plugins.DiscordAPI.sendMessage("```Markdown\n#multiplayer - HyPeX: Lobby is empty, skipping current beatmap."+"```");
				mp.setBeatmap(bot.beatmaps.nextBeatmap());
				onBeatmapChange();
			} else {
			bot.bancho.sendMessage("#multiplayer", String.format("%d/%d people are ready - extending wait time. If you are ready, please use !ready or click on ready", slotsReady, slotsTaken));
			lt.ekgame.autohost.plugins.DiscordAPI.sendMessage("```Markdown\n#multiplayer - HyPeX: "+String.format("%d/%d people are ready - extending wait time. If you are ready, please use !ready or click on ready", slotsReady, slotsTaken)+"```");
			timer.resetTimer();
			}
		}
	}
	
	public void resetBeatmaps() {
		bot.beatmaps.reset();
		MultiplayerHandler mp = bot.bancho.getMultiplayerHandler();
		mp.setBeatmap(bot.beatmaps.nextBeatmap());
	}
	
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}

	public class TimerThread extends Thread {
		
		private RoomHandler handler;
		
		private boolean stopped = false;
		private long prevTime = System.currentTimeMillis();
		private long startTime;
		private long startAfter = 2*60*1000;
		private boolean added = false;
		
		public TimerThread(RoomHandler handler) {
			this.handler = handler;
		}
		
		public void stopTimer() {
			stopped = true;
		}
		
		public boolean extendTimer() {
			if (added) 
				return false;
			
			added = true;
			startTime = startTime + 1*60*1000;
			return true;
		
		}
		
		
		public void  skipEvents() {
			startTime = System.currentTimeMillis() - 5000;
		}
		
		public void resetTimer() {
			added = false;
			startTime = System.currentTimeMillis() + startAfter + 200;
		}
		
		private void sendMessage(String message) {
			handler.bot.bancho.sendMessage("#multiplayer", message);
		}
		
		public void run() {
			resetTimer();
			while (!stopped) {
				//System.out.println("tick");
				long currTime = System.currentTimeMillis();
				//long min3mark = startTime - 3*60*1000;
				long min2mark = startTime - 2*60*1000;
				long min1mark = startTime - 1*60*1000;
				long sec10mark = startTime - 10*1000;
				//if (currTime >= min3mark && prevTime<min3mark) {
				//	sendMessage("Starting in 3 minutes. If you are ready, please use !ready or click on ready");
				//}
				if (AutoHost.instance.beatmaps.getNextBeatmap() != null)
						{
					if (currTime >= min2mark && prevTime<min2mark) {
						int id = AutoHost.instance.beatmaps.getNextBeatmap().getId();
					sendMessage("Starting in 2 minutes. If you are ready, please use !ready or click on ready. You can also download the [http://osu.ppy.sh/b/"+id+" next] beatmap.");
					lt.ekgame.autohost.plugins.DiscordAPI.sendMessage("```Markdown\n#multiplayer - HyPeX: Starting in 2 minutes."+"```");
					}
						}else{
					if (currTime >= min2mark && prevTime<min2mark) {
						int id = AutoHost.instance.beatmaps.completed.iterator().next().getId();
					sendMessage("Starting in 2 minutes. If you are ready, please use !ready or click on ready. You can also download the [http://osu.ppy.sh/b/"+id+" next] beatmap.");
					lt.ekgame.autohost.plugins.DiscordAPI.sendMessage("```Markdown\n#multiplayer - HyPeX: Starting in 2 minutes."+"```");
					}
				}
				if (currTime >= min1mark && prevTime<min1mark) {
					if (added){
					sendMessage("Starting in 1 minute. If you are ready, please use !ready or click on ready. ");
					lt.ekgame.autohost.plugins.DiscordAPI.sendMessage("```Markdown\n#multiplayer - HyPeX: Starting in 1 minutes."+"```");
					}
					if (!added){
					sendMessage("Starting in 1 minute. If you are ready, please use !ready or click on ready. If you need more time, use !wait.");						
					lt.ekgame.autohost.plugins.DiscordAPI.sendMessage("```Markdown\n#multiplayer - HyPeX: Starting in 1 minutes."+"```");
					}
				}
				if (currTime >= sec10mark && prevTime<sec10mark) {
					sendMessage("Starting in 10 seconds.");
					lt.ekgame.autohost.plugins.DiscordAPI.sendMessage("```Markdown\n#multiplayer - HyPeX: Starting in 10 seconds."+"```");
				}
				if (currTime >= startTime && prevTime<=startTime) {
					handler.tryStart();
				}
				try {
					Thread.sleep(1000);
				} catch (Exception e) {}
				prevTime = currTime;
			}
		}
	}

	public void waitTimer() {
		timer.resetTimer();
	}
	public boolean extendTimer() {
		return timer.extendTimer();
	}
}
