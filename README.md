### Main plugin for Alex's server

![Java Check](https://github.com/alexpvpmindustry/AlexServerPlugin/workflows/Java%20Check/badge.svg)

**phase 1:** make admins and mods ability asap
* use https://github.com/Anuken/AuthorizePlugin/blob/master/src/authorize/AuthorizePlugin.java to save data

commands needed in phase1
* Admin commands:
* /ban – bans players

~~* /kick – forcekicks players for 1 hr~~
* /skipmap – skips the current map and starts another

* Mod commands:
   ~~* /kick – forcekicks players for 1 hr~~
* Normal commands:
    * /report – allows people to report players that cheat/grief
    
**phase 2:** sync this across a database


## TODO:
make it pull stuff from database (maybe making json but uploading them to pipedream like urls)
make rank system be displayed
make rtv system integrated to this plugin
make chat be paired with discord and moderation tools for admins

### Setup


Clone this repository first.
To edit the plugin display name and other data, take a look at `plugin.json`.
Edit the name of the project itself by going into `settings.gradle`.

### Basic Usage

See `src/example/AlexServerPlugin.java` for some basic commands and event handlers.  
Every main plugin class must extend `Plugin`. Make sure that `plugin.json` points to the correct main plugin class.

Please note that the plugin system is in beta, and as such is subject to changes.

### Building a Jar

`gradlew jar` / `./gradlew jar`

Output jar should be in `build/libs with the name AlexServerPlugin.jar`.


### Installing

Simply place the output jar from the step above in your server's `config/mods` directory and restart the server.
List your currently installed plugins/mods by running the `mods` command.
