import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
// Server will be responsible for who's joining & leaving rooms


public class SocketServer {
    int port = 3000;
    public static boolean isRunning = false;
    private List<Room> rooms = new ArrayList<Room>();
    private Room lobby;


    private void start(int port){
        this.port = port;
        System.out.println("Waiting for client..");
        try(ServerSocket serverSocket = new ServerSocket(port);){
            isRunning = true;
            lobby = new Room("Lobby", this);
            rooms.add(lobby);

            while (SocketServer.isRunning){
                try{
                    Socket client = serverSocket.accept();
                    System.out.println("Client connecting......");
                    ServerThread thread = new ServerThread(client, lobby);
                    thread.start();
                    lobby.addClient(thread); 
                    System.out.println("Client added to the clients pool.");
                } catch(IOException e){
                    e.printStackTrace();
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        } finally{
            try{
                isRunning = false;
                System.out.println("Closing server socket.");
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    protected Room getLobby(){
        return lobby;
    }

    /***
     * Helper function to check if room exists (case-insensitive)
     * @param roomName is the name of the room
     * @return either we matched the room or null if it wasn't found
     */
     private Room getRoom(String roomName){
         for (int i = 0, L = rooms.size(); i < L; i++){
             if (rooms.get(i).getName().equalsIgnoreCase(roomName)){
                 return rooms.get(i);
             }
         }
         return null;
     }

    /*** Attempts to join room by name and will remove client from old room 
     *   & put them in the new room.
     * @param roomName is the desired room to join
     * @client is the client moving rooms
     * @return true if the reassignment worked and false if the new room doesn't exist
     */
     protected synchronized boolean joinRoom(String roomName, ServerThread client){
         Room newRoom = getRoom(roomName);
         Room oldRoom = client.getCurrentRoom();
         if (newRoom != null){
             if (oldRoom != null){
                 System.out.println(client.getName() + " leaving room " + oldRoom.getName());
                 oldRoom.removeClient(client);
             }
             System.out.println(client.getName() + " joining room " + newRoom.getName());
             newRoom.addClient(client);
             return true;
         }
         return false;

     }

     protected synchronized boolean createNewRoom(String roomName){
         if (getRoom(roomName) != null){
             System.out.println("Room already exists, fool!");
             return false;

         } else{
             Room room = new Room(roomName, this);
             rooms.add(room);
             System.out.println("Successfully created new room: " + roomName);
             return true;
         }
     }
    
     // Disconnect and Broadcast methods discontinued from Server
     // Their functions are now within the room/ServerThread
    public static void main(String[] args){
        int port = -1;
        if (args.length >= 1){
            String arg = args[0];
            try{
                port = Integer.parseInt(arg);
            } catch(Exception e){

            }
        }
        if (port > -1){
            System.out.println("Starting the server...");
            SocketServer server = new SocketServer();
            System.out.println("Listening on the port " + port);
            server.start(port);
            System.out.println("Server has stopped!");
        }
    }
}
