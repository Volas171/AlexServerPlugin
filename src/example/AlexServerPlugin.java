package example;

import arc.Core;
import arc.Events;
import arc.Net;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.*;
import mindustry.core.NetClient;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.mod.Plugin;
import mindustry.net.Packets;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import static mindustry.Vars.netServer;
import static mindustry.Vars.state;

public class AlexServerPlugin extends Plugin{
    private ObjectSet<String> authorized = new ObjectSet<>(); // authorized is a list of moderators
    private ObjectMap<String,String> urllist = new ObjectMap<>();
    private ObjectMap<String,String> uuid_color = new ObjectMap<>();
    private ObjectMap<String,String> uuid_displaymotd = new ObjectMap<>();
    public static HashMap<String, secretInfo> currentLogin = new HashMap<>();//uuid to playerinfo
    private boolean authUnits = true;
    private final String serverheader=Core.settings.getString("servername")+": ";
    //"[red]A[yellow]L[teal]E[blue]X [gold]SERVER: ";
    private Net netCommand = new Net();
    private Net netReportStats = new Net();
    private Net net1 = new Net();
    private Net net2 = new Net();
    private Net netBans = new Net();
    private final String reportURL = "https://discord.com/api/webhooks/789405998880129045/pA5W6u15pzEQXEIbXi4LhVSMzZnr1crQRKbbG9D3ogOUUcU2ODlzKs7yux5Dxy_QCvvk";
    private final String statusURL = "https://discord.com/api/webhooks/790217246622220309/mHLRDG1ansIor_AJHztywFMIHgorSH9-UJ2679LNSxa8fX6691wTzFbdcXutwgk75K_K";
    private final String dbURL = "http://209.127.178.218:3000";
    private final String findMUserUrl = dbURL+"/user/m/find";
    private final String createUserUrl = dbURL+"/user/m/create";
    private final String findDUserUrl = dbURL+"/user/d/find";
    private final String createPinUrl = dbURL+"/pin/create";
    private String axurl = "";
    private ObjectMap<Player, Team> rememberSpectate = new ObjectMap<>();
    private final String[] annoucements_impt={
            serverheader+"[accent]Join the discord at: [purple]discord.gg/[white]KPVVsj2MGW",
            serverheader+"[accent]Type [white]/discord[blue] to show discord invite![accent](register to get a color name for [tan]FREE[])" };
    private final String[] annoucements_tips={
            serverheader+"[accent]Want new [red]c[green]o[blue]l[yellow]o[white]r[forest]s[accent]? Registered players can use /color [red]red[]|[green]green[]|[blue]blue[]|[purple]purple[] to change their colours! POGGG",
            serverheader+"[accent]A Map and Meme [royal]Competition[] is [forest]ONGOING.[] Go discord to find out more!",
            serverheader+"[accent]You can report a Griefer using [lightgray]/report[]. By doing so, [forest]Alex Moderators []will be notified. ie: POLIS POLIS HALP HALP!!"
    };
    private final String motd_rules= serverheader+"\n [gold]*** [tan]RULES[] ***[]\n[white]"
                            +"[red]NO[] - Griefing (inform teammates if you are rebuilding)\n"
                            +"[red]NO[] - Building/pixel art unless with permission of teammates in PVP/SURVIVAL/ATTACK (sandbox,ok)\n"
                            +"[red]NO[] - NSFW (text or schemes or any)\n"
                            +"[red]NO[] - Flaming/Racisim/Sexism/Begging\n"
                            +"[red]NO[] - Harassment to others in chat or in PM\n"
                            +"[red]NO[] - Spamming/Lag machines\n"
                            +"[red]NO[] - Advertising for external stuffs\n"
                            +"[green]YES[] - Have FUN :POG:\n"
                            +"[green]YES[] - Register!!\n"
                            +"[green]YES[] - Contribute via Discord!\n\n"
            +"\n [gold]*** [tan]COMMANDS available[] ***[]\n[white]"
            +"[accent]/t[] - talk to teammates (pvp)\n"
            +"[accent]/rtv[] - vote to skip current map\n"
            +"[accent]/votekick[] - vote to kick a player\n"
            +"[accent]/report[] - send a griefer report to discord\n"
            +"[accent]/register[] - shows a pin for registration via discord\n"
            +"[accent]/color[] [red]red[]|[green]green[]|[blue]blue[]|[purple]purple[] - change your color(only for registered)\n"
            +"[accent]/discord[] - discord invite (giveaways and [gold]MORE[])\n"
            +"[accent]/rules[] - toggle [accent]off[] this display next time you log in";
    private final int annoucementsInterval = 90;//60*5; // 60seconds * 5
    //called when game initializes
    class secretInfo {
        private final String uuid;
        private final String role;
        private final String color;
        private final boolean registered;
        private final long duuid;
        private float prev_x,prev_y;
        private int gainedAx=0;
        public secretInfo(String uuid, long duuid, String role, String color, boolean registered,float prev_x, float prev_y){
            this.uuid = uuid;
            this.duuid = duuid;
            this.role=role;
            this.color=color;
            this.registered=registered;
            this.prev_x=prev_x;
            this.prev_y=prev_y;
        }
        public void incrementAx(){
            this.gainedAx ++;
        }
        public void sendAx(){ //todo fix this
            //do the sending of ax here
            Net netSendAx = new Net();
            Net.HttpRequest request = (new Net.HttpRequest())
                    .method(Net.HttpMethod.PUT).header("Content-Type", "application/json")
                    .content("{\"target_muuid\":\""+uuid+"\"}")
                    .url(axurl+"");
            netSendAx.http( request,(s)->{},(t)->{});
            this.gainedAx=0;
        }
        public Boolean isAFK(float x, float y){
            if ((this.prev_x==x) && (this.prev_y==y)){
                return true;
            }else{
                this.prev_x=x;
                this.prev_y=y;
                return false;
            }
        }
    }
    class PlayerSettings{ //todo, join uuid_color and uuid_display_motd into one, and save it as 1 variable in the core.settings
        private final String uuid;
        private String color;
        private Boolean displaymotd;
        private Boolean displaykick;
        public PlayerSettings(String uuid, String color, Boolean displaymotd, Boolean displaykick){
            this.uuid=uuid;
            this.color=color;
            this.displaymotd=displaymotd;
            this.displaykick=displaykick;
        }
    }
    class Announcer{
        int ann_type=0; int curr_tip=0;int curr_impt=0;
        Timer.Task task;
        public Announcer(){
            this.task = Timer.schedule(() -> {
                if (ann_type==0){ // important announcement
                    Call.sendMessage( annoucements_impt[curr_impt]);
                    curr_impt++;ann_type=1;
                    if (curr_impt>=annoucements_impt.length) {curr_impt=0;}
                }else{  // tips announcement
                    Call.sendMessage( annoucements_tips[curr_tip]);
                    curr_tip++;ann_type=0;
                    if (curr_tip>=annoucements_tips.length) {curr_tip=0;}
                }
            }, annoucementsInterval,annoucementsInterval);
        }
    }
    @Override
    public void init(){
        uuid_color = Core.settings.getJson("uuid_color", ObjectMap.class, String.class, ObjectMap::new);
        uuid_displaymotd=Core.settings.getJson("uuid_displaymotd", ObjectMap.class, String.class, ObjectMap::new);
        authorized = Core.settings.getJson("authorized-players", ObjectSet.class, String.class, ObjectSet::new);
        urllist = Core.settings.getJson("url-config", ObjectMap.class, String.class, ObjectMap::new);
        if (urllist.containsKey("axurl")){
            axurl = urllist.get("axurl");
        }
        authUnits = Core.settings.getBool("allow-unauthorized-units", true);
        Events.on(EventType.ServerLoadEvent.class, event -> {
            netServer.admins.addChatFilter((player, text) -> null);
        });
        Events.on(EventType.PlayerChatEvent.class, event -> {//this displays the player's role in the chat
            Player p = event.player;
            String role_tag;
            if (event.message.startsWith("/")) { return;}
            String pcolor="[#"+p.color.toString()+"]";
            if (currentLogin.containsKey(p.uuid())){
                String sprole = currentLogin.get(p.uuid()).role;
                if (sprole!=""){
                    if (sprole.equals("Player")){
                        sprole="Registered";
                    }
                    role_tag = "[accent]<"+sprole+">[]";
                }else{
                    role_tag = "[accent]<Public>[]";
                }
            }else{
                role_tag="";
                Call.sendMessage( serverheader+"Player: "+p.name+" [accent]PLEASE RE-LOGIN");
            }
            Call.sendMessage(event.message,role_tag + pcolor + p.name,p);
        });
        Events.on(EventType.PlayerLeave.class,event-> updateCurrentLogin());
        Events.on(EventType.PlayerBanEvent.class,event-> updateCurrentLogin());
        Events.on(EventType.PlayerIpBanEvent.class,event-> updateCurrentLogin());
        Events.on(EventType.PlayerConnect.class,event->{
            Player p = event.player;
            String uuid = p.uuid(); boolean connected=false;
            if (p.name.contains("[") ||p.name.contains("]") ||p.name.contains(">") || p.name.contains("<") || p.name.contains("\\") || p.name.contains("/")|| p.name.contains("|")){
                //p.con.kick("Illegal characters \"[[,],>,<\" in your name, please change it",0);
                p.name = Strings.stripColors(p.name).replace("[","").replace("]","")
                        .replace("\\","").replace("/","")
                        .replace("<","").replace(">","");
                Log.info("Illegal characters in name, striped" );
            }
            if (p.name.length()<2 || p.name.length()>25){
                p.con.kick("keep name length between 2 and 25, no special characters",2);
                return;
            }
            if (p.name.contains("　")||p.name.contains("  ")||p.name.contains("\u200E")){
                p.con.kick("No illegal blank spaces in name :(",2);
                return;
            }
            /*TODO: fix this and also make it shorter , this is inefficient
             if (p.name.contains("CODEX")||(p.name.contains("VALVE")||(p.name.contains("IgruhaOrg")||(p.name.contains("андрей")||(p.name.contains("tuttop")||(p.name.contains("IGGGAMES")||p.name.contains||(p.name.contains("O7")||p.name.contains("Nexity")){
                //Log.info("your name is too short, minimum 4 letters" );
                p.con.kick("Please install a proper game version at [royal]https://anuke.itch.io/mindustry [white] :(, it's free too",2);
                return;
            } */
            //if (true){return;} // sadddd ;-;
            if (!connected){
                filterColor(p,parseColor("white")); // sets colour white first
                setPlayerData(p,uuid);
                if (uuid_displaymotd.containsKey(uuid)){
                    if (uuid_displaymotd.get(uuid).equals("display")){
                        //Call.infoPopup(p.con,"this is a test message of the rules \n lets see if new line [red]works[]. thank you.",20,1,0,0,0,0);
                        Call.infoMessage(p.con,motd_rules);
                    }
                }else{
                    uuid_displaymotd.put(uuid,"display");
                    Core.settings.putJson("uuid_displaymotd",String.class,uuid_displaymotd);
                    Call.infoMessage(p.con,motd_rules);
                }
            } else{ //player already connected
                p.con.kick("already connected, try again later. or disconnect your other account/s");
            }
            Timer.schedule(()->{// updates after 20 seconds
                updateCurrentLogin();
            },5);
        });
        new Announcer();
        Timer.schedule(()->{//sends the status of server
            Net.HttpRequest request = (new Net.HttpRequest())
                    .method(Net.HttpMethod.POST).header("Content-Type", "application/json")
                    .content("{\"content\":\""+ Strings.stripColors(Core.settings.getString("servername"))+
                            " is **running**: "+state.map.name()+", **PLAYERS**="+Groups.player.size()+", **RAM**="+
                            Core.app.getJavaHeap() / 1024 / 1024+"MB\"}").url(statusURL);
            netReportStats.http(request,(s)->{
            },(t)->{
            });},180,180);
    }

    private void setPlayerData(Player p , String uuid) {
        Net.HttpRequest request = (new Net.HttpRequest())
                .method(Net.HttpMethod.POST).header("Content-Type", "application/json")
                .content("{\"target_muuid\":\""+uuid+"\"}")
                .url(findMUserUrl);
        net1.http( request,(s)->{
            if(s.getStatus()==Net.HttpStatus.OK){//found
                String res= s.getResultAsString();
                JSONParser parser = new JSONParser();
                try{
                    JSONObject json = (JSONObject) parser.parse(res);
                    if (json.get("status").equals("Ban")){
                        p.con.kick("You have been banned from our server, go to Discord to appeal.",2);
                        return;
                    }
                    if ( (Boolean) json.get("verified")){ //already verified and look for role
                        long duuid = (long) json.get("duuid");
                        Log.info("verified user, start to find role from database");
                        Net.HttpRequest request2 = (new Net.HttpRequest())
                                .method(Net.HttpMethod.GET)
                                .url(findDUserUrl+"/"+ duuid);
                        net2.http( request2,(s2)-> {
                            if (s2.getStatus()==Net.HttpStatus.OK){
                                Log.info("role found, start to parse");
                                String res2=s2.getResultAsString();
                                try {
                                    JSONObject json2 = (JSONObject) parser.parse(res2);
                                    String gotten_role = (String) json2.get("role");
                                    String gotten_color = (String) json.get("color");
                                    if (gotten_color.equals("white")){
                                        if (uuid_color.containsKey(uuid)){
                                            gotten_color = uuid_color.get(uuid);
                                        }else{
                                            uuid_color.put(uuid,"blue");
                                            gotten_color="blue";
                                            Core.settings.putJson("uuid_color", String.class, uuid_color);
                                        }
                                    }
                                    currentLogin.put(p.uuid(), new secretInfo(p.uuid(),duuid , gotten_role, gotten_color, true,p.x,p.y));
                                    filterColor(p, parseColor(gotten_color));
                                    Log.info("Parse successful, done");
                                    if (gotten_role.equals("Admin")){
                                        p.admin(true);
                                    }
                                }catch (ParseException e){
                                    p.sendMessage("failed to parse in database, re-login to get verified perks");
                                    Log.info("error,line179,"+e.toString());
                                    currentLogin.put(p.uuid(), new secretInfo(p.uuid(),duuid , "", "purple", true,p.x,p.y));
                                    filterColor(p, parseColor("purple"));
                                }
                            } else{//duuid not found, so, revert to normal status
                                Log.info("failed to find user in database, relogin to get verified perks");
                                p.sendMessage("failed to find user in database, relogin to get verified perks");
                                currentLogin.put(p.uuid(),new secretInfo(p.uuid(),0,"","purple",true,p.x,p.y));//normal player
                                filterColor(p,Color.purple);
                            }
                        },(t2)->{
                            Log.info("failed to connect, relogin to get verified perks");
                            p.sendMessage("failed to find user in database, relogin to get verified perks");
                            setNoConnectionMode(p);//normal player
                        });
                    }else { // not verified, but found from database
                        Log.info("returning player");
                        //p.sendMessage("[accent]welcome back");
                        setNoConnectionMode(p);//normal player
                    }
                }
                catch (ParseException e){
                    Log.info("error,line203,"+e.toString());
                    setNoConnectionMode(p);//normal player
                }
            } else { // not found from database
                Log.info("user creation here");
                Net.HttpRequest request2 = (new Net.HttpRequest())
                        .method(Net.HttpMethod.POST).header("Content-Type", "application/json")
                        .content(
                                "{\"muuid\":\""+p.uuid()+"\","+
                                        "\"mname\": \""+ p.name+"\"}"
                        ).url(createUserUrl);
                net2.http( request2,(s2)->{
                    if(s2.getStatus()==Net.HttpStatus.OK){//create ok
                        p.sendMessage("new user is saved into database!");
                        Log.info("new user is saved into database!");
                    } else{
                        Log.info("failed 152");
                        p.sendMessage("Something went wrong, please log in again, error 223");
                    }
                    setNoConnectionMode(p);//normal player
                },(t2)->{
                    p.sendMessage("Something went wrong, please log in again, error 227");
                    Log.info("Something went wrong t2"+t2.getMessage());
                    setNoConnectionMode(p);//normal player
                });
            }
        },(t)->{ // unable to fetch
            Log.info("Something went wrong t string"+t.toString());
            p.sendMessage("Something went wrong, please log in again, error 234");
            Log.info("Something went wrong t message"+t.getMessage());
            setNoConnectionMode(p);
            //currentLogin.put(p.uuid(),new secretInfo(p.uuid(),0,"","white",false));//normal player
        });

    }

    //register commands that player can invoke in-game
    @Override
    public void registerClientCommands(CommandHandler handler){
        //register a simple reply command
        //handler.<Player>register("reply25", "<text...>", "A simple ping command that echoes a player's text.", (args, player) -> {
        //    player.sendMessage("You said: [accent] " + args[0]);
        //});
        handler.removeCommand("t");
        handler.removeCommand("a");
        handler.<Player>register("t", "<message...>", "Send a message only to your teammates.", (args, player) -> {
            String message = args[0];
            if(message != null){
                Groups.player.each(p -> p.team() == player.team(), o -> o.sendMessage(message, player, "[#" + player.team().color.toString() + "]<T>" + NetClient.colorizeName(player.id(), player.name)));
            }
        });
        handler.<Player>register("discord", "Prints the discord link", (args, player) -> {
            player.sendMessage("[blue] discord.gg/[white]KPVVsj2MGW");
        });
        handler.<Player>register("register", "register with discord account.", (args, player) -> {
            if (!currentLogin.get(player.uuid()).registered) {
                String pin = "1"+generatePin(player.uuid());
                Net.HttpRequest request = (new Net.HttpRequest())
                        .method(Net.HttpMethod.POST).header("Content-Type", "application/json")
                        .content("{\"pin\":" + pin + ",\"muuid\":\"" + player.uuid() + "\"}").url(createPinUrl);
                netCommand.http(request, (s) -> {
                    player.sendMessage("[accent]Type this command into discord #bot-channel: a?register " + pin);
                }, (t) -> {
                    player.sendMessage("[accent]Something went wrong, try again later");
                });
            }else{
                player.sendMessage("[accent]You are already registered xD. Re-login to see its effect.");
            }
        });
        handler.<Player>register("color", "<color>","Choose your color (purple,green,red,blue)(only for registered players)", (args, player) -> {
            String color=args[0]; Boolean allowed = false;
            String[] allowed_admin_color = {"orange","royal","purple","green","red","blue"};
            String[] allowed_mod_color = {"royal","purple","green","red","blue"};
            String[] allowed_reg_color = {"purple","green","red","blue"};
            if (currentLogin.get(player.uuid()).registered){
                if ( currentLogin.get(player.uuid()).role.equals("Admin") && (Arrays.asList(allowed_admin_color).contains(color) )){
                    allowed=true;
                } else if (currentLogin.get(player.uuid()).role.equals("Mod") && ( Arrays.asList(allowed_mod_color).contains(color) )){
                    allowed=true;
                } else if (  Arrays.asList(allowed_reg_color).contains(color)  ) {
                    allowed = true;
                }
                if (allowed) {
                    uuid_color.put(player.uuid(),color);
                    Core.settings.putJson("uuid_color", String.class, uuid_color);
                    player.color = parseColor(color);
                    player.sendMessage("[accent]Color changed!");
                } else {
                    player.color = parseColor("blue");
                    uuid_color.put(player.uuid(),color);
                    Core.settings.putJson("uuid_color", String.class, uuid_color);
                    player.sendMessage("[accent]Color not allowed, defaulted to [blue]blue[]. Choose purple|red|green|blue. Admins/orange. Mods/royal");
                }
            }else{
                player.sendMessage("[accent]Color changed not allowed! Please register first");
            }
        });
        handler.<Player>register("rules","toggle on/off rules",(args,player)->{
            if (uuid_displaymotd.containsKey(player.uuid())){
                if (uuid_displaymotd.get(player.uuid()).equals("display")){
                    uuid_displaymotd.put(player.uuid(),"offdisplay");
                    player.sendMessage("[accent]rules display toggled [red]off");
                }else{
                    uuid_displaymotd.put(player.uuid(),"display");
                    player.sendMessage("[accent]rules display toggled [green]on");
                    Call.infoMessage(player.con,motd_rules);
                }
            }else{
                uuid_displaymotd.put(player.uuid(),"display");
                player.sendMessage("[accent]rules display toggled [green]on");
                Call.infoMessage(player.con,motd_rules);
            }
            Core.settings.putJson("uuid_displaymotd",String.class,uuid_displaymotd);
        });
        handler.<Player>register("report","[#ID]","sends a report to discord admins",(args,player)->{
            if(args.length == 0) {
                player.sendMessage(strBuildPlayerIDs("[accent]Add the player's ID, include the '#'",player));
                return;
            }
            Player found;
            if(args[0].length() > 1 && args[0].startsWith("#") && Strings.canParseInt(args[0].substring(1))){
                int id = Strings.parseInt(args[0].substring(1));
                found = Groups.player.find(p -> p.id() == id);
            }else{
                found = Groups.player.find(p -> p.name.equalsIgnoreCase(args[0]));
            }
            if (found !=null){
                if (found.name ==player.name){return;}
                Net.HttpRequest request = (new Net.HttpRequest())
                        .method(Net.HttpMethod.POST).header("Content-Type", "application/json")
                        .content("{\"content\":\"In port:"+ Strings.stripColors(Core.settings.getString("servername"))+
                                ", <@&787871315037913128> Report: "+
                                found.name+" UUID:"+found.uuid()+" USID:"+
                                found.usid()+" IP:"+found.con.address+
                                " Reported by:"+
                                player.name + " UUID:"+player.uuid()+" USID:"+
                                player.usid()+" IP:"+player.con.address+
                                "\"}").url(reportURL);
                netCommand.http(request,(s)->{
                    player.sendMessage("Report sent to discord! Thank you!");
                },(t)->{
                    player.sendMessage("Something went wrong, report not sent! ;-;");
                });
            }else{
                player.sendMessage("Player not found");
            }
        });
        //register kick command //duration of a a kick in seconds
        int kickDuration = 60 * 60;
        handler.<Player>register("kick", "[#ID]", "Kick a player for 1 hr. ID includes the #", (args, player) -> {
            if ( (!player.admin) && (currentLogin.get(player.uuid()).role!="Admin")){
                if ((!authorized.contains(player.usid()) ) && (currentLogin.get(player.uuid()).role!="Mod")) {
                    player.sendMessage("only admins/mods can do this");
                    return;}
            }
            if(args.length == 0) {
                player.sendMessage(strBuildPlayerIDs(" Add the player's ID, include the '#'",player));
                return;
            }
            Player found;
            if(args[0].length() > 1 && args[0].startsWith("#") && Strings.canParseInt(args[0].substring(1))){
                int id = Strings.parseInt(args[0].substring(1));
                found = Groups.player.find(p -> p.id() == id);
            }else{
                found = Groups.player.find(p -> p.name.equalsIgnoreCase(args[0]));
            }
            if(found != null){
                found.getInfo().lastKicked = Time.millis() + kickDuration*1000;
                Groups.player.each(p -> p.uuid().equals(found.uuid()), p -> p.kick(Packets.KickReason.kick));
                Call.sendMessage(serverheader+"[accent]Please behave![white] "+found.name+" [accent]was kicked!");}
            else{
                player.sendMessage("Player not found!");
            }
        });
        handler.<Player>register("players","[page]","show player IDs",(args,player)-> {
            if(args.length > 0 && !Strings.canParseInt(args[0])){
                player.sendMessage("[scarlet]'page' must be a number.");
                return;
            }
            int commandsPerPage = 8;
            int page = args.length > 0 ? Strings.parseInt(args[0]) : 1;
            int pages = Mathf.ceil((float)Groups.player.size() / commandsPerPage);
            page --;
            if(page >= pages || page < 0){
                player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and[orange] " + pages + "[scarlet].");
                return;
            }
            StringBuilder result = new StringBuilder();
            result.append(Strings.format("[orange]-- Players #ID Page[lightgray] @[gray]/[lightgray]@[orange] --\n\n", (page+1), pages));
            for(int i = commandsPerPage * page; i < Math.min(commandsPerPage * (page + 1), Groups.player.size()); i++){
                Player p = Groups.player.index(i);
                result.append("[lightgray] ").append(p.name).append("[accent] (#").append(p.id()).append(")\n");
            }
            player.sendMessage(result.toString());
        });
        handler.<Player>register("ban", "<#ID/UUID>", "[scarlet]Admin only[]Ban uuid from all servers. #ID includes the #, else its the full UUID", (args, player) -> {
            if ((!player.admin) && (currentLogin.get(player.uuid()).role != "Admin")) {
                player.sendMessage("[scarlet]This command is only for admins/mods!");
                return;
            }
            if ( !urllist.containsKey("banurl")){ player.sendMessage("yikes, banurl not set, contact alex asap");
            }
            String uuid = "";
            if (args[0].startsWith("#")){
                Player found = Groups.player.find(p -> p.id() == Strings.parseInt(args[0].substring(1)));
                if (found!=null){ uuid = found.uuid();
                }else{ player.sendMessage("player not found, type properly");
                }
            } else{ uuid=args[0];
            }
            String tosend = "{\"target_muuid\":\""+uuid+"\","
                    +"\"staff_muuid\":\""+player.uuid()+"\""
                    +" }";
            Net.HttpRequest request = (new Net.HttpRequest())
                    .method(Net.HttpMethod.POST).header("Content-Type", "application/json")
                    .content(tosend)
                    .url(urllist.get("banurl"));
            String finalUuid = uuid;
            netBans.http( request,(sban)->{
                if(sban.getStatus()==Net.HttpStatus.OK) {
                    Player p2 = Groups.player.find(p -> p.uuid().equals(finalUuid));
                    if (p2!=null){
                        p2.con.kick("banned from server :(",5);
                    }
                    player.sendMessage("the uuid is banned, good job!");
                }else{
                    player.sendMessage("player not found in db:"+sban.getStatus().toString());
                }
            },(tban)->{player.sendMessage("something went wrong, line446, try again soon:"+tban.getMessage());});
        });
        handler.<Player>register("unban", "<UUID>", "[scarlet]Admin only[]Unban uuid from all servers.", (args, player) -> {
            if ((!player.admin) && (currentLogin.get(player.uuid()).role != "Admin")) {
                player.sendMessage("[scarlet]This command is only for admins/mods!");
                return;
            }
            if ( !urllist.containsKey("unbanurl")){ player.sendMessage("yikes, unbanurl not set, contact alex asap");
            }
            String uuid = args[0];
            Net.HttpRequest request = (new Net.HttpRequest())
                    .method(Net.HttpMethod.POST)
                    .header("Content-Type", "application/json")
                    .content("{\"target_muuid\":\""+uuid+"\","
                            +"\"staff_muuid\":\""+player.uuid()+"\""
                            +" }")
                    .url(urllist.get("unbanurl"));
            netBans.http( request,(sban)->{
                if(sban.getStatus()==Net.HttpStatus.OK) {
                    player.sendMessage("the uuid is unbanned, good job!");
                }else{
                    player.sendMessage("player not found in db:"+sban.getStatus().toString());
                }
            },(tban)->{player.sendMessage("something went wrong, line462, try again soon:"+tban.getMessage());});
        });
        handler.<Player>register("spectate", "[scarlet]Admin only[] goes into spectate mode, but cant undo :(", (args, player) -> {
            if(!state.rules.pvp) return;
            if((!player.admin) && (currentLogin.get(player.uuid()).role!="Admin")){
                if( (authorized.contains(player.usid()) )&& (currentLogin.get(player.uuid()).role!="Mod")) {
                    player.sendMessage("[scarlet]This command is only for admins/mods!");
                    return;
                }
            }
            if(player.team() == Team.all[6]){
                player.team(rememberSpectate.get(player));
                rememberSpectate.remove(player);
                player.sendMessage("[gold]PLAYER MODE[]");
            }else{
                rememberSpectate.put(player, player.team());
                player.team(Team.all[6]);
                player.sendMessage("[green]SPECTATE MODE[]");
            }
        });

        handler.<Player>register("mod","<add/remove> <ID/UUID> <player...>","Add or Remove a moderator, needs player #ID or UUID",(args,player)->{
            if (!player.admin){
                if (currentLogin.get(player.uuid()).role!="Admin"){
                    player.sendMessage("Only admins can do this");
                    return;
                }
            }
            Player found;
            if (args[1].equals("ID")) {
                if (args[2].length() > 1 && args[2].startsWith("#") && Strings.canParseInt(args[2].substring(1))) {
                    int id = Strings.parseInt(args[2].substring(1));
                    found = Groups.player.find(p -> p.id() == id);
                } else {
                    found = Groups.player.find(p -> p.name.equalsIgnoreCase(args[2]));
                }
            } else if (args[1].equals("UUID")){
                player.sendMessage("yikes, not implemented,uuid, use ID instead");
                return;
            } else{
                player.sendMessage("please input a ID");
                return;
            }
            if (found.isNull()){
                player.sendMessage("player not found! type properly. if using ID, include the #");
                return;
            } else if (args[0].equals("add")){// add/remove mod
                authorized.add(found.usid());
                save();
                player.sendMessage(found.name +" is modded!!"); Call.sendMessage(serverheader+"[gold]Welcome our new mod: [royal]"+found.name);
                return;
            } else if (args[0].equals("remove")){
                authorized.remove(found.usid());
                save();
                player.sendMessage(found.name +" is UNmodded!! sad-pepe"); Call.sendMessage(serverheader+"[royal]" +found.name+"[gold] is DOWNGRADED!");
                found.con.kick("you are unmodded, pepe sad :(",0);
                return;
            }
        });
        // /modlist, to show the moderator list
        handler.<Player>register("modlist","<skipnumberofrows>","(admin)show moderator list, input skipnumber to skip X rows, must be 0 for first row",(args,player)-> {
            if (!player.admin){
                if (currentLogin.get(player.uuid()).role!="Admin"){
                    player.sendMessage("Only admins can do this");
                    return;
                }
            }
            String strbuilder=""; int count =0; int pcount=0;
            count = Integer.parseInt(args[0]);
            for (String usid : authorized){
                pcount+=1;
                if (pcount>=count){
                    strbuilder += "[teal]row"+pcount+": [white]"+usid+"\n";
                }
                player.sendMessage(strbuilder);
            }
        });
        handler.<Player>register("showcurrentlogins","(admin)showcurrentlogins ",( args,player)->{
            if (!player.admin) { player.sendMessage("Only admins can do this"); return;}
            player.sendMessage("uuid players in current login");
            for (String uuid:currentLogin.keySet()){
                Player p1 = Groups.player.find(p ->p.uuid().equals(uuid) );
                player.sendMessage(p1.name+","+uuid+","+currentLogin.get(uuid).role+","+currentLogin.get(uuid).registered+","+currentLogin.get(uuid).color);
            }
        });
    }
    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("setdata","<field> <data>","set all data",args->{
            urllist.put(args[0],args[1]);
            Core.settings.putJson("url-config", String.class, urllist);
            Log.info("saved field: "+args[0]+" data: "+args[1]);
        });
        handler.register("readdata","read all data",args->{
            for (String key:urllist.keys()) {
                Log.info("field: "+key+" data: "+urllist.get(key));
            }
        });
        handler.register("wipedata","<field>","wipe data, input all to wipe all",args->{
            if (args[0].equals("all")){
                urllist.clear();
                Core.settings.putJson("url-config", String.class, urllist);
                Log.info("cleared data ;-;");
            }else{
                if (urllist.containsKey(args[0])){
                    urllist.remove(args[0]);
                    Log.info("removed:"+args[0]);
                    Core.settings.putJson("url-config", String.class, urllist);
                }else{
                    Log.info("field not found");
                }
            }
        });
    }


    private void updateCurrentLogin(){ // ensure people in playInfo are online
        Seq<String> temp = new Seq<>();
        for(String uuid : currentLogin.keySet()){
            Player found = Groups.player.find(p -> p.uuid() == uuid);
            if ((found==null) || (!found.con.isConnected())){
                temp.add(uuid);
            }
        }
        if (temp.size>0) {
            for (int i = 0; i < temp.size; i++) {
                currentLogin.remove(temp.get(i));
            }
        }

        for (int i = 0; i < Groups.player.size(); i++) {
            Player p = Groups.player.index(i);
            String uuid = p.uuid();
            if (!currentLogin.containsKey(uuid)){
                setPlayerData(p,uuid);
            }
        }
    }
    private void getRoleSetPlayerInfo(String muuid){
        return;
    }
    private String strBuildPlayerIDs(String msg,Player player){
        StringBuilder builder = new StringBuilder();
        builder.append("[orange]Players #ID:"+msg+"\n");
        Groups.player.each(p -> p.con != null && p != player, p -> {
            builder.append("[lightgray] ").append(p.name).append("[accent] (#").append(p.id()).append(")\n");
        });
        return builder.toString();
    };

    private void save(){
        Core.settings.put("allow-unauthorized-units", authUnits);
        Core.settings.putJson("authorized-players", String.class, authorized);
    }
    public void filterColor(Player p, Color c){
        p.name = Strings.stripColors(p.name);
        p.color = c;
    }
    private void setNoConnectionMode(Player p){ // adds items into currentlogin and sets colors
        if( p.admin  ){ // is admin
            currentLogin.put(p.uuid(), new secretInfo(p.uuid(), 0, "Admin", "orange", false,p.x,p.y));
            filterColor(p, Color.orange);
        } else if (authorized.contains(p.usid()) ){ //is moderator
            currentLogin.put(p.uuid(), new secretInfo(p.uuid(), 0, "Mod", "royal", false,p.x,p.y));
            filterColor(p, Color.royal);
        } else{
            currentLogin.put(p.uuid(), new secretInfo(p.uuid(), 0, "", "white", false,p.x,p.y));
            filterColor(p, Color.white);
        }
    }
    private Color parseColor(String color){
        Color c;
        switch (color){
            case "red":
                c = Color.red; break;
            case "green":
                c = Color.green; break;
            case "blue":
                c = Color.blue; break;
            case "royal":
                c = Color.royal; break;
            case "purple":
                c = Color.purple; break;
            case "orange":
                c = Color.orange; break;
            default:
                c=Color.white;
        }return c;
    }
    private static String generatePin(String uuid) {
        // use the first 5 char of uuid to generate a 4 digit pin
        String ss = uuid.substring(0, 5);
        long seed = 0;
        for (
                int i = 0; i < ss.length(); i++) {
            int base = 10, exponent = i;
            long result = 1;
            for (; exponent != 0; --exponent) {
                result *= base;
            }
            seed = seed + (result) * (int) ss.charAt(i);
        }
        Random rand = new Random(seed);
        return String.format("%04d", rand.nextInt(10000));
    }

}
