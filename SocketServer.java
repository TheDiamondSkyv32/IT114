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
    private List<Room> isolatedPrelobbies = new ArrayList<Room>();
    private static final String PRELOBBY = "PreLobby";
    protected static final String LOBBY = "Lobby";



    private void start(int port){
        this.port = port;
        System.out.println("Waiting for client..");
        try(ServerSocket serverSocket = new ServerSocket(port);){
            isRunning = true;
            Room.setServer(this);
            lobby = new Room(LOBBY);
            rooms.add(lobby);

            while (SocketServer.isRunning){
                try{
                    Socket client = serverSocket.accept();
                    System.out.println("Client connecting......");
                    ServerThread thread = new ServerThread(client, lobby);
                    thread.start();
                    Room prelobby = new Room(PRELOBBY);
                    prelobby.addClient(thread); 
                    isolatedPrelobbies.add(prelobby);

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


    protected void cleanupRoom(Room r){
        isolatedPrelobbies.remove(r);
    }

    protected void cleanup(){
        Iterator<Room> rooms = this.rooms.iterator();
        while (rooms.hasNext()){
            Room r = rooms.next();
            try{
                r.close();
            }catch (Exception e){
                //ignored
            }
        }
        Iterator<Room> pl = isolatedPrelobbies.iterator();
        while (pl.hasNext()){
            Room r = pl.next();
            try{
                r.close();
            } catch (Exception e){
                //ignored
            }
        }
        try{
            lobby.close();
        }catch (Exception e){
            //ignored
        }
    }

    protected Room getLobby(){
        return lobby;
    }

    protected void joinLobby(ServerThread client){
        Room prelobby = client.getCurrentRoom();
        if (joinRoom(LOBBY, client)){
            prelobby.removeClient(client);
            System.out.println("Added " + client.getClientName() + " to Lobby. Prelobby is yeeting itself.");
        } else{
            System.out.println("Problem moving " + client.getClientName() + " to the lobby.");
        }
    }


    /***
     * Helper function to check if room exists (case-insensitive)
     * @param roomName is the name of the room
     * @return either we matched the room or null if it wasn't found
     * Updated to search for the room
     */
     private Room getRoom(String roomName){
         for (int i = 0, L = rooms.size(); i < L; i++){
             Room r = rooms.get(i);
             if (r == null || r.getName() == null){
                 continue;
             }
             if (r.getName().equalsIgnoreCase(roomName)){
                 return r;
             }
         }
         return null;
     }

    /*** Attempts to join room by name and will remove client from old room 
     *   & put them in the new room.
     * @param roomName is the desired room to join
     * @client is the client moving rooms
     * @return true if the reassignment worked and false if the new room doesn't exist
     * Updated in Part 5.
     */
     protected synchronized boolean joinRoom(String roomName, ServerThread client){
         if (roomName == null || roomName.equalsIgnoreCase(PRELOBBY)){
             return false;
         }
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


     /***
      * Attempt to create room w/given name if it does not already exist
      * @param roomName is the room to create
      * @return returns true if the room was created successfully and false otherwise
      */
     protected synchronized boolean createNewRoom(String roomName){
         if (roomName == null || roomName.equalsIgnoreCase(PRELOBBY)){
             return false;
         }
         if (getRoom(roomName) != null){
             System.out.println("Room already exists, fool!");
             return false;

         } else{
             Room room = new Room(roomName);
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
