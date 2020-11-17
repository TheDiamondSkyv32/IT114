import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// I won't be using utils.Debug because my editor 
// makes it a pain to use
public class Room {
    private SocketServer server;
    private String name;

    public Room(String name, SocketServer server){
        this.name = name;
        this.server = server;

    }

    public String getName(){
        return name;
    }

    private List<ServerThread> clients = new ArrayList<ServerThread>();

    protected synchronized void addClient(ServerThread client){

        client.setCurrentRoom(this);

        if (clients.indexOf(client) > -1){
            System.out.println("Attemping to add a client that already exists!");

        } else{
            clients.add(client);
            sendMessage(client, " has joined the room " + getName());
        }
    }
    protected synchronized void removeClient(ServerThread client){
        clients.remove(client);
        sendMessage(client, "has departed from the room.");
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
            if (message.indexOf("/") > -1){
                String[] comm = message.split("/");
                String part_1 = comm[1];
                String[] comm2 = part_1.split(" ");
                String command = comm2[0];
                String roomName;

                switch (command){
                    case "createroom": 
                        roomName = comm2[1];
                        if (server.createNewRoom(roomName)){
                            server.joinRoom(roomName, client);
                        }
                        wasCommand = true;
                        break;
                    case "joinroom":
                        roomName = comm2[1];
                        server.joinRoom(roomName, client);
                        wasCommand = true;
                        break;     
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return wasCommand;
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
        message = String.format("User[%s]: %s", sender.getName(), message);
        while (iter.hasNext()){
            ServerThread client = iter.next();
            boolean messageSent = client.send(message);
            if (!messageSent){ // if messageSent is false, the ! turns the false to true and removes the user
                iter.remove();
                System.out.println("Removed client " + client.getId());
            }
        }
    }
}
