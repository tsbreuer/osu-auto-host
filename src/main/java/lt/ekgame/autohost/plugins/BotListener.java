package lt.ekgame.autohost.plugins;

import java.util.ArrayList;
import java.util.List;

import lt.ekgame.autohost.AutoHost;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class BotListener extends ListenerAdapter {
	
	  @Override
	   public void onMessageReceived(MessageReceivedEvent event){
		  	Message msg = event.getMessage();
		  	/*
		  	List<Role> Operators = new ArrayList<>();
		  	List<Role> OperatorSearch = lt.ekgame.autohost.plugins.DiscordAPI.pushServer.getRolesByName("Operators", true);
		  		for (Role role : OperatorSearch){
		  			if (role.hasPermission(Permission.ADMINISTRATOR)){
		  				Operators.add(role);
		  			}		  				
		  		}
		  
		  		Member player = event.getMember();
		  		List<Role> PlayerRoles = player.getRoles();
		  	for (Role role : PlayerRoles){
		  		if (Operators.contains(role)){
		  			
		  		}
		  	}
		  	*/
		  	if (msg.getContent().startsWith("&") && msg.getAuthor().getId() != event.getJDA().getSelfUser().getId()){
			  	if (event.getMember().hasPermission(Permission.ADMINISTRATOR)){	
		  	String message = msg.getContent();

			String command = message.trim().substring(1);
			String[] rawArgs = command.split(" ");
			if (rawArgs.length == 0)
				return;
			
			String label = rawArgs[0].toLowerCase();
			List<String> args = new ArrayList<>();
			for (int i = 1; i < rawArgs.length; i++)
				args.add(rawArgs[i]);
				if (label.equals("say")){
					String sendMessage = "";
						for (int i = 1; i < rawArgs.length; i++)
							sendMessage = sendMessage+rawArgs[i]+" ";
						
				  	AutoHost.instance.bancho.sendMessage("#multiplayer","*Discord> "+event.getAuthor().getName()+": "+sendMessage);
				  	event.getChannel().sendMessage("```Markdown\n#multiplayer - *Discord> "+event.getAuthor().getName()+": "+sendMessage+"```").queue();
				  	return;
				}
			 AutoHost.instance.bancho.getCommandHandler().handle("#multiplayer", "HyPeX", 711080, label, args);
		  		
		  	
			  	}
		  	}
	  }
}