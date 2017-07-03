# AutoHost for osu!
A bot for osu! multiplayer to allow automatic beatmap rotation. This software was created to test capabilities of ekgame's [Bancho client framework](https://github.com/ekgame/bancho-client) project *(Please note these links do not work with the current bot, for such look down the readme)*.

*This bot was originally done by ekgame and I've got his permission to modify it. All API's and dependencies used are also made by him but contain some slight modifications*


 
 **  Please get permissiom from peppy by emailing him or accounts@ppy.sh for creating a bot account. Creating a bot account without peppy's permission is considered multiaccounting and will most likely end up with your account being restricted.**
 
## When does a game start?

Before starting a game, the bot will wait 2 minutes (3 if someone requests for time) for everyone to get ready. If everyone is ready before the time runs out - the game will start instantly. If about 70% people are ready when the timer runs out - the game will be force-started.

Alternatively, players can use `!ready` to be counted as ready instead of clicking on "ready" button ingame.

## Interaction

To use the bot you can type `!command` into the chat. Some commands only work in the multiplayer chat, some only in private chat. There are only two permission levels at the moment: user (default) and an operator. Operator is an administrative role and therefore it can use some more commands.

## Commands

| Command       | Description |
|---|---|
| !add [link to beatmap]  | Adds a beatmap to a queue of maps to play. The beatmaps must match a criteria decribed below. |
| !adddt [link to beatmap] | Adds a beatmap to a queue of maps to play WITH DT. The beatmaps must match a criteria decribed below. DT Rating is calculated, so dont worry. |
| !searchsong [Name] | Searches a beatmap by name in the current star rating. If a song has more than one option in current rating, it will pick the highest automatically. If there are three or less matches, it will output the options with links to use !add |
| !voteskip | Vote to skip a map. Over 50% of users have to vote to skip the current song. |
| !info | Displays text specified in info-text at settings.conf |
| !author | Displays author info. Non-editable. |
| !queue | Displays upcoming beatmaps in queue. Be aware long queue lists may be buggy and the chat not enough. |
| !ready (or !r) | Vote start a map/counts you as ready. Lobby will automatically start if 75% of players are ready of have voted as. |
| !skip | Bot will ignore your status for current round and start regardless if conditions are met (Ignoring you) |
| !afk | Literally means you will be on "Skip" status for each round. Use this command again to be removed from AFK list |
| !lock [0-15] | Toggles lock on a slot [0-15] (Can be used to kick) |
| !pm [player] msg | OP only. Bot PMs player with MSG. ignores if player exists/is online. (only works PM'ing the bot) |
| !wait | Extends wait time by 1 minute. Only once per map. |
| !slotinfo | Shows occupied slots by slot number and player name occupying it. Usefull for !lock comamnd.  |
| !kick [name] | Kicks a player by name. Optional: Add reason (!kick name reason) |
| !addop [osuId] | Adds an operator to the OP list by osu account ID. This doesnt modify settings.conf and lasts only until bot restart.  |
| !removeop [osuId] | Removes an operator from the OP list by osu account ID. This doesnt modify settings.conf and lasts only until bot restart.  |
| !rename [name] | An operator command. Renames lobby to specified |.
| !maxdiff [rating] | An operator command. Changes maximum star difficulty. |
| !mindiff [rating] | An operator command. Changes minimum star difficulty. |
| !graveyard | An operator command. Enables/Disables graveyard maps |
| !forceskip | An operator command. Instantly skips a map. |
| !dt | An operator command. Force enables/disables DT |
| !freemods | An operator command. Force enables/disables freemods. |
| !delay | An operator command. Resets the waiting timer and requested extra time. (default: 2 minutes). |
| !isop | Only works in private chat. Tells you whether or not you are an operator. |
| !help | Only works in private chat. Links you to this page. |

## Adding beatmaps

When you're adding a beatmap, the link should look something like this: `https://osu.ppy.sh/b/665240` or simply `osu.ppy.sh/b/665240`. Notice the `/b/` - it denotes that this link points to a specific difficulty in a beatmap set. If the link has `/s/` - it points to an entire beatmap set, rendering it invalid. If you want to get the valid link, you'll need to make sure that a difficulty is selected, don't be afraid to click it again to double check. You'll notice in the URL box that the numbers will change along with the letter. 

When adding a DT beatmap, the bot will calculate the new star rating of the song after adding DT to it.

Players can only add a single beatmap at once at the queue (Ignoring current song to be played) to enforce host rotation.

## Beatmap queue
To setup a beatmap queue, you will need to create a `beatmaps` folder next to the executable file. This folder should contain various `.osu` files of beatmaps to play. If folder is empty, bot will stay without any beatmap loaded until someone loads one.

## Beatmap criteria
This is the current criteria for using the !add command. Some settings will be configurable, but for now, the map needs to:
* Be for osu! standard gamemode.   
* Have a star difficulty between 4 and 6. (Changeable)
* Be ranked, qualified or pending. (Changeable)
* Be less then 6 minutes long.
* Not be a repeat of the last 30 songs played.

## Compiling
First of all, you will need some dependencies. Most of the dependency management is done with Maven. There are three libraries that you will need to reference manually. **Note they're originally done by ekgame, but since I've done some personal modifications, I've uploaded them under my repositories. Make sure to show him some love. Their URLs are literally the same as ekgame, just replace my username with his and you will get the pointer to his work.**
* [Bancho API](https://github.com/tsbreuer/bancho-api) - the commons API used for packet parsing.
* [Bancho Client](https://github.com/tsbreuer/bancho-client) - the framework for Bancho client.
* [Beatmap Analyzer](https://github.com/tsbreuer/beatmap-analyzer) - The analyzer tool for beatmaps.

## Player Managing
- Check readme.

## Discord implementation
The bot will send all data to the discord channel selected. Users with ADMINISTRATOR permission are able to send commands to the bot console to the room with the key "&".
Ex: &add osu.ppy.sh/b/665240

## Running
First of all compile or download the executable file from [releases page](https://github.com/tsbreuer/osu-host-bot/releases).
Then to run the bot, you will first need to make a configuration file `settings.conf` like this:
```PYTHON
account {
  username = "username"
  password = "password"
  osu-api-key = "apikey" # obtained here https://osu.ppy.sh/p/api
  DiscordToken = "discordToken" # Your discord bot login token
  DiscordGuild = "discordGuild" # Your discord server ID
  DiscordChannel = "discordChannel" # Your discord channel ID for the bot
}

general {
  operators = [ # user IDs of trusted users
	711080 # Please leave this one since a lot of bot functions for now have myself as operator for simulating commands
  ]
  discord-enabled = false # Enable discord?
}

room {
  name = "AutoHost testing"
  password = null  
  slots = 16
}
```
Before running the bot, don't forget to setup a `beatmaps` folder. Your file structure should be something like this:
```
/autohost
|--/beatmaps
|  |--beatmap1.osu
|  |--beatmap2.osu
|  |--etc...
|--settings.conf
|--autohost.jar
```

Then to actually run the bot, all you have to do is execute the `run.cmd` or `run.sh`. Just make sure the text is as following:

```java -jar autohost.jar settings.conf```
