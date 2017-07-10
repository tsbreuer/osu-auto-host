# AutoHost for osu!
A bot for osu! multiplayer to allow automatic beatmap rotation. This software was created to test capabilities of ekgame's [Bancho client framework](https://github.com/ekgame/bancho-client) project *(please note these links do not work with the current bot, for such look down the readme)*.

*This bot was originally done by ekgame and I've recieved his permission to modify it. All API's and dependencies used are made by him but contain some slight modifications*


 
**Please get permissiom from peppy by emailing him or accounts@ppy.sh for creating a bot account. Creating a bot account without peppy's permission is considered multiaccounting and will most likely end up with your account being restricted.**
 
## When does a game start?

Before starting a game, the bot will wait 2 minutes (3 if someone requests for time) for everyone to get ready. If everyone is ready before the time runs out - the game will start instantly. If about 70% people are ready when the timer runs out - the game will be force-started.

Alternatively, players can use `!ready` to be counted as ready instead of clicking on the "ready" button ingame.

## Interaction

To begin your interactions with AutoHost, I suggest adding it as a friend for PM-exclusive commands. Friending him is not mandatory, but friending it will grant you the permissions to create your own lobby, delete your lobby, and more. There are only two permission levels at the moment: user (default) and an operator. Operator is an administrative role assigned to the creator of the lobby and whoever he deems worthy.

## Commands

| Command       | Description |
|---|---|
| !add [link to beatmap]  | Adds a beatmap to a queue of maps to play. The beatmaps must match a criteria decribed below. |
| !adddt [link to beatmap] | Adds a beatmap to a queue of maps to play WITH DT. The beatmaps must match a criteria decribed below. DT rating is calculated, so dont worry. |
| !afk | Sets yourself to away. Use this command again to be removed from AFK list |
| !author | Displays beatmap's author info. |
| !help | Only works in private chat. Links you to this page. |
| !info | Displays text specified in info-text at settings.conf |
| !isop | Only works in private chat. Tells you whether or not you're an operator. |
| !queue | Displays upcoming beatmaps in queue. Long queue lists will probably bug out. |
| !ready (or !r) | Indicate that you're ready. Lobby will automatically start if 75% of the players are ready. |
| !searchsong [name] | Searches for a beatmap in the current star rating. If a song has more than one difficulty in the star limit, thte bot will pick the most difficult one. If there are three or less matches, it will output the options with links to use !add. |
| !skip | Bot will ignore your status for current round and start regardless if conditions are met. |
| !slotinfo | Shows occupied slots by slot number and player name occupying it. |
| !voteskip | Vote to skip a map. Over 50% of users have to vote to skip the current map. |
| !wait | Extends wait time by 1 minute. Only once per map. |

## Operator Commands

| Command       | Description |
|---|---|
| !kick [player] [reason] | Kicks a player. |
| !addop [osuId] | Adds an operator to the OP list by osu account ID. This doesn't modify settings.conf and lasts until the bot restarts.  |
| !delay | Resets the waiting timer and requested extra time. |
| !dt | Force enables/disables DT |
| !forceskip | Instantly skips a map. |
| !freemods | Force enables/disables freemods. |
| !graveyard | Enables/Disables graveyard maps |
| !lock [0-15] | Used to lock a slot. [0-15] |
| !maxdiff [rating] | Changes maximum star difficulty. |
| !mindiff [rating] | Changes minimum star difficulty. |
| !pm [player] msg | Bot PMs specified player with a message. |
| !removeop [osuId] | Removes an operator from the OP list by osu account ID. This doesnt modify settings.conf and lasts only until bot restart.  |
| !rename [name] | An operator command. Renames lobby. |

## Adding beatmaps

When you're adding a beatmap, the link should look something like this: `https://osu.ppy.sh/b/665240` or simply `osu.ppy.sh/b/665240`. Notice the `/b/` - it denotes that this link points to a specific difficulty in a beatmap set. If the link has `/s/` - it points to an entire beatmap set, rendering it invalid. If you want to get the valid link, you'll need to make sure that a difficulty is selected, don't be afraid to click it again to double check. You'll notice in the URL box that the numbers will change along with the letter. 

When adding a DT beatmap, the bot will calculate the new star rating of the song after adding DT to it.

Players can only add a single beatmap at once at the queue (Ignoring current song to be played) to enforce host rotation.

## Beatmap criteria
This is the current criteria for using the !add command. Some settings will be configurable, but for now, the map needs to:
* Be for osu! standard gamemode.   
* Have a star difficulty between X and Y. (Changes depending on the lobby)
* Be ranked, qualified or pending. (Changeable)
* Be less then 6 minutes long.
* Not be a repeat of the last 30 songs played.

## Compiling
First of all, you will need some dependencies. Most of the dependency management is done with Maven. There are three libraries that you will need to reference manually. **Note they're originally done by ekgame, but since I've done some personal modifications, I've uploaded them under my repositories. Make sure to show him some love. Their URLs are literally the same as ekgame, just replace my username with his and you will get the pointer to his work.**
* [Bancho API](https://github.com/tsbreuer/bancho-api) - the commons API used for packet parsing.
* [Bancho Client](https://github.com/tsbreuer/bancho-client) - the framework for Bancho client.
* [Beatmap Analyzer](https://github.com/tsbreuer/beatmap-analyzer) - The analyzer tool for beatmaps.

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
