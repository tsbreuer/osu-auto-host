package lt.ekgame.autohost.plugins;

import java.util.List;

import lt.ekgame.autohost.AutoHost;
import net.dv8tion.jda.core.*;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;



public class DiscordAPI {

	public static JDA jda;
	public static String token = "";
	public static TextChannel pushChannel = null;
	public static Guild pushServer = null;
	public static String pushChannelID = "";
	public static String pushServerID ="";
	public static boolean Enabled = false;
	
	public static void startDiscord (){
		try {
			
			jda = new JDABuilder(AccountType.BOT)
					.setToken(token)
					.setAutoReconnect(true)
					.addListener(new BotListener())
					.buildBlocking();
			
			
		  	List<Guild> guilds = jda.getGuilds();
		  	for (Guild guild : guilds){
		  		if (guild.getId().equals(pushServerID)){
		  			pushServer = guild;
		  			break;
		  		}
		  	}
		  		if (pushServer != null){
		  	List<TextChannel> channels = pushServer.getTextChannels();
		  		for (TextChannel channel : channels)		  			
		  			if (channel.getId().equals(pushChannelID)) {
		  				pushChannel = channel;
		  				System.out.println("Discord Channel loaded");
		  				Enabled = true;
		  				break;
		  			}
		  		}
		  		  	//pushChannel.sendMessage("Bot loaded.\n```First Header | Second Header\n----- | -----\ncell | cell```").queue();
		  
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void sendMessage(String text) {
		if (Enabled)
		pushChannel.sendMessage(text).queue();
	}
	

}