import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// I won't be using utils.Debug because my editor 
// makes it a pain to use
public class Room implements AutoCloseable {
    private static SocketServer server; // changed to static
    private String name;

    // commands
    private final static String COMMAND_TRIGGER = "/";
    private final static String CREATE_ROOM = "createroom";
    private final static String JOIN_ROOM = "joinroom";

    public Room(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }

    public static void setServer(SocketServer server){
        Room.server = server;
    }

    private List<ServerThread> clients = new ArrayList<ServerThread>();

    protected synchronized void addClient(ServerThread client){
        client.setCurrentRoom(this);

        if (clients.indexOf(client) > -1){
            System.out.println("Attemping to add a client that already exists!");

        } else{
            clients.add(client);
            if (client.getClientName() != null){ // broadcast that user has joined the room if name isn't null
                sendMessage(client, " has joined the room " + getName());
            }
        }
    }

    protected synchronized void removeClient(ServerThread client){
        clients.remove(client);
        if (clients.size() > 0){ // while someone's in the room, we don't close it
            sendMessage(client, "has departed from the room.");
        } else {
            cleanupEmptyRoom();
        }
    }

    private void cleanupEmptyRoom(){
        if (name == null || name.equalsIgnoreCase(SocketServer.LOBBY)){
            return;
        }
        try {
            System.out.println("Closing empty room : " + name);
            close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    protected void joinRoom(String room, ServerThread client){
        server.joinRoom(room, client);
    }

    protected void joinLobby(ServerThread client){
        server.joinLobby(client);
    }

    /*** 
    * This next part basically provides the base functionality for commands
    * Thing such as /joinroom and /createroom 
    * @parameter message is the message being sent
    * @parameter client is the sender of the message
    *
    */

    private boolean processCommands(String message, ServerThread client){
        boolean wasCommand = false;
        
        try{
            if (message.indexOf(COMMAND_TRIGGER) > -1){
                String[] comm = message.split(COMMAND_TRIGGER);

                String part_1 = comm[1];
                String[] comm2 = part_1.split(" ");
                String command = comm2[0];
                if (command != null){
                    command = command.toLowerCase();
                }
                String roomName;

                switch(command){
                    case CREATE_ROOM: 
                        roomName = comm2[1];
                        if (server.createNewRoom(roomName)){
                            joinRoom(roomName, client);
                        }
                        wasCommand = true;
                        break;
                    case JOIN_ROOM:
                        roomName = comm2[1];
                        joinRoom(roomName, client);
                        wasCommand = true;
                        break;     
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return wasCommand;
    }

    protected void sendConnectionStatus(String clientName, boolean isConnect){
        Iterator<ServerThread> iter = clients.iterator();
        while (iter.hasNext()){
            ServerThread client = iter.next();
            boolean messageSent = client.sendConnectionStatus(clientName, isConnect);
            if (!messageSent){
                iter.remove();
                System.out.println("Removed client " + client.getId());
            }
        }
    }

    /***
     * Takes the sender & message and broadcasts message to all clients in present room.
     * @parameter sender is the client sending the message
     * @parameter message is the message to broadcast in the room.
     */
    protected void sendMessage(ServerThread sender, String message){
        System.out.println(getName() + ": Sending message to " + clients.size() + " clients");
        if (processCommands(message, sender)){
            return; // if this check returns true, the message was a command and to ignore it

        }
        Iterator<ServerThread> iter = clients.iterator();
        while (iter.hasNext()){
            ServerThread client = iter.next();
            boolean messageSent = client.send(sender.getClientName(), message);
            if (!messageSent){ // if messageSent is false, the ! turns the false to true and removes the user
                iter.remove();
                System.out.println("Removed client " + client.getId());
            }
        }
    }

    @Override 
    public void close() throws Exception{
        int clientCount = clients.size();
        if (clientCount > 0){
            System.out.print("Migrating " + clients.size() + " to Lobby");
            Iterator<ServerThread> iter = clients.iterator();
            Room lobby = server.getLobby();
            while (iter.hasNext()){
                ServerThread client = iter.next();
                lobby.addClient(client);
                iter.remove();
            }
            System.out.println("Done migrating " + clientCount + " to Lobby");
        }
        server.cleanupRoom(this);
        name = null;
    }
}
