import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerThread extends Thread{
    private Socket client;
    private ObjectInputStream in; // from client
    private ObjectOutputStream out; // to client
    private boolean isRunning = false;
    private Room currentRoom; // current room -> lobby by default

    protected synchronized Room getCurrentRoom(){
        return currentRoom;
    }

    protected synchronized void setCurrentRoom(Room room){
        if (room != null){
            currentRoom = room;

        } else {
            System.out.println("Passed in room was null, this is a problem");
        }
    }

    public ServerThread(Socket myClient, Room room) throws IOException{ // Change SocketServer to Room 
        this.client = myClient;
        this.currentRoom = room;
        out = new ObjectOutputStream(client.getOutputStream());
        in = new ObjectInputStream(client.getInputStream());
    }
    /***
     * Sends message to client represented by this ServerThread
     * @param message 
     * @return
     */


    protected boolean send(String message){ // changes to protected boolean method
        try{
            out.writeObject(message);
            return true;
        } catch (IOException e){
            System.out.println("Error sending message to client {most likely disconnected}");
            e.printStackTrace();
            cleanup();
            return false;
        }
    }

    @Override
    public void run(){
        try{
            isRunning = true;
            String fromClient;
            while (isRunning && !client.isClosed() && (fromClient = (String) in.readObject()) != null){
                System.out.println("Received from client: " + fromClient);
                currentRoom.sendMessage(this, fromClient); // server updated to implement room functionality
            } // close the while loop
        }
        catch (Exception e){
                e.printStackTrace();
                System.out.println("Client has disconnected.");
        } finally {
            isRunning = false;
            System.out.println("Cleaning up connection for ServerThread.");
            cleanup();
            }
        }

    private void cleanup(){
        if (currentRoom != null){ // more server functionality offloaded to the Room
            System.out.println(getName() + " removing self from the room: " + currentRoom.getName());
            currentRoom.removeClient(this);
        }

        if (in != null){
            try{
                in.close();
            } catch (IOException e){
                System.out.println("Input is already closed!");
            }
        }

        if (out != null){
            try{
                out.close();
            } catch (IOException e){
                System.out.println("Client is already closed!");
            }
        }

        if (client != null && !client.isClosed()){
            try{
                client.shutdownInput();
            } catch(IOException e){
                System.out.println("Socket / Input is already closed!");
            }
            try{
                client.shutdownInput();
            } catch (IOException e){
                System.out.println("Socket / Output already closed!");
            }
            try{
                client.close();
            } catch (IOException e){
                System.out.println("Client is already closed!");
            }
        }
    }
}
