package server;
import java.util.ArrayList;
import java.awt.Color;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Room implements AutoCloseable {

    private static SocketServer server;// used to refer to accessible server functions
    private String name;
    private final static Logger log = Logger.getLogger(Room.class.getName());

    // Commands
    private final static String COMMAND_TRIGGER = "/";
    private final static String CREATE_ROOM = "createroom";
    private final static String FLIP = "flip";
    private final static String ROLL = "roll";
	private final static String JOIN_ROOM = "joinroom";
	private final static String DM = "@";
	private final static String UNMUTE = "unmute";
	private final static String MUTE = "mute";
	private List<ServerThread> clients = new ArrayList<ServerThread>();


    public Room(String name) {
	this.name = name;
    }

    public static void setServer(SocketServer server) {
	Room.server = server;
    }

    public String getName() {
	return name;
    }

    

    protected synchronized void addClient(ServerThread client) {
	client.setCurrentRoom(this);
	if(clients.indexOf(client) > 1){
		log.log(Level.INFO, "Client already exists, you dunce!");
	}
	else{
		clients.add(client);
		if (client.getClientName() != null){
			client.sendClearList();
			sendConnectionStatus(client, true, " joined the room " + getName());
			updateClientList(client);
		}
	}
}
	

    /**
     * Syncs the existing clients in the room with our newly connected client
     * 
     * @param client
     */
    private synchronized void updateClientList(ServerThread client) {
	Iterator<ServerThread> iter = clients.iterator();
	while (iter.hasNext()) {
	    ServerThread c = iter.next();
	    if (c != client) {
	    	client.sendConnectionStatus(c.getClientName(), true, null);
	    }
	}
    }

    protected synchronized void removeClient(ServerThread client) {
		clients.remove(client);

		if (clients.size() > 0){
			sendConnectionStatus(client, false, " left the room " + getName());
		}
		else {
		cleanupEmptyRoom();
		}
    }

    private void cleanupEmptyRoom() {
	// If name is null it's already been closed. And don't close the Lobby
	if (name == null || name.equalsIgnoreCase(SocketServer.LOBBY)) {
	    return;
	}
	try {
	    log.log(Level.INFO, "Closing empty room: " + name);
	    close();
	}
	catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    protected void joinRoom(String room, ServerThread client) {
	server.joinRoom(room, client);
    }

    protected void joinLobby(ServerThread client) {
	server.joinLobby(client);
    }

    protected void createRoom(String room, ServerThread client) {
	if (server.createNewRoom(room)) {
	    joinRoom(room, client);
	}
    }


    /***
     * Helper function to process messages to trigger different functionality.
     * 
     * @param message The original message being sent
     * @param client  The sender of the message (since they'll be the ones
     *                triggering the actions)
     */
	private String processCommands(String message, ServerThread client) {
		String response = null;
		try {
			if (message.indexOf(COMMAND_TRIGGER) > -1) {
			String[] comm = message.split(COMMAND_TRIGGER);
			log.log(Level.INFO, message);
			String part1 = comm[1];
			String[] comm2 = part1.split(" ");
			String command = comm2[0];
			if (command != null) {
				command = command.toLowerCase();
			}
			String roomName;
			switch (command) {
			case CREATE_ROOM:
				roomName = comm2[1];
				if (server.createNewRoom(roomName)) {
				joinRoom(roomName, client);
				}
				break;
			case JOIN_ROOM:
				roomName = comm2[1];
				joinRoom(roomName, client);
				break;

			case FLIP:
				Random rand = new Random();
				int coin = rand.nextInt(2);
				String tempMessage_;
				if (coin == 1){
					tempMessage_ = ("<font color=orange>The results of the coin toss is... Heads!</font>");
				} 
				else{
					tempMessage_ = ("<font color=red>The results of the coin toss is... Tails!</font>");
				}
				//sendMessage(client, tempMessage_);
				response = tempMessage_;
				break;
		case ROLL:
			//TODO - let the user decide the upper-bound of the roll
				Random rand_1 = new Random();
				int dice = rand_1.nextInt(100) + 1;
				String tempMessage = ("<b style=color:yellow>You rolled a " + Integer.toString(dice) + " point(s)</b>");
				//sendMessage(client, tempMessage);
				response = tempMessage;
				break;
				
		case MUTE:
			String[] parseUserName = message.split(" ");
			String userToBeMuted = parseUserName[1]; // /mute Meme -> [0] == /mute, [1] == Meme, the username we are looking for
			client.mutedList.add(userToBeMuted);
			client.mute(userToBeMuted);
			//sendMessage(client, "<i> has muted " + userToBeMuted + "</i>!");
			//client.onIsMuted(userToBeMuted, true);
			
			sendDM(client, "<font color=red>You have muted " + userToBeMuted + "!</font>");
			break;
			
		case UNMUTE:
			String[] parseUserName1 = message.split(" ");
			String userToBeUnmuted = parseUserName1[1];
			for (String name : client.mutedList) {
				if(name.equals(userToBeUnmuted)) {
					client.mutedList.remove(userToBeUnmuted);
					client.unmute(userToBeUnmuted);
					//sendMessage(client, "<i> has unmuted " + "</b>!");
					sendDM(client, "<font color=blue>You have unmuted " + userToBeUnmuted + "!</font>");
					break;
				}
			}
		default:
			response = message;
			break;
			
			}
			}
			else {	
				response = message;
				
				if (response.indexOf("##") > -1) {
					String[] s1 = response.split("##");
					String m = "";					
					for (int i = 1; i < s1.length; i++) {
						if(i % 2 == 0) {
							m += s1[i];
						}
						else {
							m += "<b><font style=color:blue>" + s1[i] + "</font></b>";
						}
					}
					response = m;
				}
				if (response.indexOf("!!") > -1) {
					String[] s1 = response.split("!!");
					String m = "";
					
					for (int i = 1; i < s1.length; i++) {
						if(i % 2 == 0) {
							m += s1[i];
						}
						else {
							m += "<u><font style=color:red>" + s1[i] + "</font></u>";
						}
					}
					response = m;
				}

			}
		} // can't use @@, logic / something funky is happening
		catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}


    protected void sendConnectionStatus(ServerThread client, boolean isConnect, String message) {
	Iterator<ServerThread> iter = clients.iterator();
	while (iter.hasNext()) {
	    ServerThread c = iter.next();
	    boolean messageSent = c.sendConnectionStatus(client.getClientName(), isConnect, message);
	    if (!messageSent) {
		iter.remove();
		log.log(Level.INFO, "Removed the following client:  " + c.getId());
	    }
	}
    }

    /***
     * Takes a sender and a message and broadcasts the message to all clients in
     * this room. Client is mostly passed for command purposes but we can also use
     * it to extract other client info.
     * 
     * @param sender  The client sending the message
     * @param message The message to broadcast inside the room
     */
    protected void sendMessage(ServerThread sender, String message) {
	log.log(Level.INFO, getName() + ": Sending message to " + clients.size() + " clients");
	String resp = processCommands(message, sender);
	
	if (resp == null) {
	    // it was a command, don't broadcast
	    return;
	}
	
	if (sendDM(sender, message)) {
		return;
	}
	
	message = resp;
	
	Iterator<ServerThread> iter = clients.iterator();
	while (iter.hasNext()) {
	    ServerThread client = iter.next();
	    
	    if(!client.isMuted(sender.getClientName())) {
		    boolean messageSent = client.send(sender.getClientName(), message);
	    	if (!messageSent) {
	    		iter.remove();
	    		log.log(Level.INFO, "Removed client " + client.getId());
	    	}
	    }
	}
    }
	
    protected boolean sendDM(ServerThread sender, String message) {
    	boolean wasDM = false;
    	String temp = null;
    	
    	if (message.indexOf("@") > -1) {
    		String[] listWords = message.split(" ");
    		for(String word : listWords) {
    			if(word.charAt(0) == '@') {
    				temp = word.substring(1);
    				wasDM = true;
    			Iterator<ServerThread> iter = clients.iterator();
    			while(iter.hasNext()) {
    				ServerThread c = iter.next();
    				if (c.getClientName().equals(temp)&&(!c.isMuted(sender.getClientName()))){ // Make sure we're not sending a message to a muted user
    					c.send(sender.getClientName(),  message);
    				}
    			}
    		}
    	}
    	}
    	sender.send(sender.getClientName(), message);
    	return wasDM; //return boolean value
    	
    }
	

    public List<String> getRooms() {
	return server.getRooms();
    }

    /***
     * Will attempt to migrate any remaining clients to the Lobby room. Will then
     * set references to null and should be eligible for garbage collection
     */
    @Override
    public void close() throws Exception {
	int clientCount = clients.size();
	if (clientCount > 0) {
	    log.log(Level.INFO, "Migrating " + clients.size() + " to Lobby");
	    Iterator<ServerThread> iter = clients.iterator();
	    Room lobby = server.getLobby();
	    while (iter.hasNext()) {
		ServerThread client = iter.next();
		lobby.addClient(client);
		iter.remove();
	    }
	    log.log(Level.INFO, "Done Migrating " + clients.size() + " to Lobby");
	}
	server.cleanupRoom(this);
	name = null;
	// should be eligible for garbage collection now
	}
}
