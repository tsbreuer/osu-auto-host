# AutoHost for osu!
A bot for osu! multiplayer to allow automatic beatmap rotation. This software was created to test capabilities of ekgame's [Bancho client framework](https://github.com/ekgame/bancho-client) project.

*This bot was originally done by ekgame and i've got his permission to modify it. All API's and dependency used are also made by him but contain some slight modifications*

## When does a game start?

Before starting a game, the bot will wait 3 minutes for everyone to get ready. If everyone is ready before the time runs out - the game will start instantly. If about 70% people are ready when the timer runs out - the game will be force-started.

Alternatively, players can use !ready to be counted as ready instead of clicking on "ready" button ingame.

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
| !lock [0-15] | Toggles lock on a slot [0-15] (Can be used to kick) |
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
| !wait | An operator command. Resets the waiting timer (default: 3 minutes). |
| !isop | Only works in private chat. Tells you whether or not you are an operator. |
| !help | Only works in private chat. Links you to this page. |

## Adding beatmaps

When you're adding a beatmap, the link should look something like this: `https://osu.ppy.sh/b/665240` of just `osu.ppy.sh/b/665240`. Notice the `/b/` - it denotes that this link point to a specific difficulty in a beatmap set. If the link has `/s/` - it points to a beatmap set and therefore is too ambigious and invalid. If you want to get the valid link, on the beatmap set page, click on one of the **difficulty tabs**.

When adding a DT beatmap, the bot will calculate the new star rating of the song after adding DT to it.

Players can only add a single beatmap at once at the queue (Ignoring current song to be played) to enforce host rotation.

## Beatmap queue
To setup a beatmap queue, you will need to create `beatmaps` folder next to the executable file. This folder should contain various `.osu` files of beatmaps to play. If folder is empty, bot will stay without any beatmap loaded until someone loads one.

## Beatmap criteria
This is the current criteria for using the !add command. This will be configurable later, but for now it's hardcoded to:
* The map must be for osu! standard gamemode.   
* The map's star difficulty must be between 4 and 6 stars. (Changeable)
* The map must be either ranked, qualified or pending. (Changeable)
* The map can not be longer that 6 minutes.
* The map can not be a repeat of the last 30 songs.

## Compiling
First of all, you will need some dependencies. Most of the dependency management is done with Maven. There are three libraries that you will need to reference manually. **Note they're originally done by ekgame, but since i've done some personal modifications, i've uploaded them under my repositories. Make sure to show him some love. Their URLs are literally the same as ekgame, just replace my username with his and you will get the pointer to his work.**
* [Bancho API](https://github.com/tsbreuer/bancho-api) - the commons API used for packet parsing.
* [Bancho Client](https://github.com/tsbreuer/bancho-client) - the framework for Bancho client.
* [Beatmap Analyzer](https://github.com/tsbreuer/beatmap-analyzer) - The analyzer tool for beatmaps.

## Player Managing
Since i'm kinda limited by ekgame's API, currently there is no method to kick players or edit slot count after a lobby is created. When his API is updated, i'll work on those methods.


## Running
First of all compile or download the executable file from [releases page](https://github.com/tsbreuer/osu-host-bot/releases).
Then to run the bot, you will first need to make a configuration file `settings.conf` like this:
```PYTHON
account {
  username = "username"
  password = "password"
  osu-api-key = "apikey" # obtained here https://osu.ppy.sh/p/api
}

general {
  operators = [ # user IDs of trusted users
	711080
  ]
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

Then to actually run the bot the bot use the file as an argument:

```java -jar autohost.jar settings.conf```
