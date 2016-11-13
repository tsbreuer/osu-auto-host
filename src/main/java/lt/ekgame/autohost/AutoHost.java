package lt.ekgame.autohost;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import lt.ekgame.bancho.api.exceptions.LoginException;
import lt.ekgame.bancho.api.packets.Packet;
import lt.ekgame.bancho.api.packets.server.PacketReceivingFinished;
import lt.ekgame.bancho.api.packets.server.PacketRoomJoined;
import lt.ekgame.bancho.client.BanchoClient;
import lt.ekgame.bancho.client.MultiplayerHandler;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

public class AutoHost {

	public static void main(String... args) throws Exception {
		new AutoHost(args);
	}
	
	public BanchoClient bancho;
	public Permissions perms;
	public BeatmapHandler beatmaps = new BeatmapHandler("beatmaps");
	public RoomHandler roomHandler;
	public Settings settings;
	
	public static AutoHost instance;

	public AutoHost(String... args) throws IOException, URISyntaxException, LoginException, ObjectMappingException {
		if (args.length == 0) {
			System.err.println("You must specify a settings file.");
			return;
		}
		
		instance = this;
		
		settings = new Settings(args[0]);
		perms = new Permissions(settings.operatorIds);
		
		bancho = new BanchoClient(settings.username, settings.password, false, true);
		
		bancho.getCommandHandler().addExecutor(new CommandsGeneral(this));
		bancho.getCommandHandler().addExecutor(new CommandsRoom(this, settings.osuApi));
		new ConsoleListener().start();
		bancho.registerHandler(roomHandler = new RoomHandler(this));
		bancho.registerHandler((Packet packet) -> {
			if (packet instanceof PacketReceivingFinished) {
				System.out.println("Creating room...");
				MultiplayerHandler mp = bancho.getMultiplayerHandler();
				mp.enableMultiplayer();
				mp.createRoom(settings.roomName, settings.roomPassword, settings.roomSlots);
				if (settings.freemodsEnabled)
					mp.setFreeMods(true);
			}
			if (packet instanceof PacketRoomJoined) {
				System.out.println("Room created!");
			}
		});
		
		
		System.out.println("Running client...");
		bancho.connect();
		System.out.println("Authanticated, starting...");
		bancho.start();
		System.out.println("Started.");
	}
	
}
class ConsoleListener extends Thread {

    public void run() {
            
    	
    	
          Scanner scanner = new Scanner(System.in);
          while (true){
          String scommand = scanner.nextLine();
          if (scommand.trim().startsWith("!")) {
              String command = scommand.trim().substring(1);
              String[] rawArgs = command.split(" ");
              if (rawArgs.length == 0)
              return;
              
              String label = rawArgs[0].toLowerCase();
              List<String> args = new ArrayList<>();
              for (int i = 1; i < rawArgs.length; i++)
              args.add(rawArgs[i]);
              
              //System.out.println("Calling "+command);
              AutoHost.instance.bancho.getCommandHandler().handle("#multiplayer", "HyPeX", 711080, label, args);
          }
          if (scommand.trim().startsWith(".")) {
              String command = scommand.trim().substring(1);
              String[] rawArgs = command.split(" ");
              if (rawArgs.length == 0)
              return;
              
              String label = rawArgs[0].toLowerCase();
              List<String> args = new ArrayList<>();
              for (int i = 1; i < rawArgs.length; i++)
              args.add(rawArgs[i]);
              
              //System.out.println("Calling "+command);
              AutoHost.instance.bancho.getCommandHandler().handle("HyPeX", "HyPeX", 711080, label, args);
          }
      }
  }
}

