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
    private String clientName;

    public String getClientName(){
        return clientName;
    }

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

    @Deprecated
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
    /***
     * replacement for send(message)
     * @param clientName
     * @param message
     * @return
     */
    protected boolean send(String clientName, String message){
        Payload payload = new Payload();
        payload.setPayloadType(PayloadType.MESSAGE);
        payload.setClientName(clientName);
        payload.setMessage(message);

        return sendPayload(payload);
    }
    
    protected boolean sendConnectionStatus(String clientName, boolean isConnect){
        Payload payload = new Payload();
        if(isConnect){
            payload.setPayloadType(PayloadType.CONNECT);
        } else{
            payload.setPayloadType(PayloadType.DISCONNECT);
        }
        payload.setClientName(clientName);
        return sendPayload(payload);
    }

    private boolean sendPayload(Payload pt){
        try{
            out.writeObject(pt);
            return true;
        } catch (Exception e){
            System.out.println("Error sending message to client (this means they most likely disconnected).");
            e.printStackTrace();
            cleanup();
            return false;
        }
    }

    private void processPayload(Payload pt){
        switch (pt.getPayloadType()){
            case CONNECT:
                String n = pt.getClientName();
                if (n != null){
                    clientName = n;
                    System.out.println("Username has been set to " + clientName);
                    if(currentRoom != null){
                        currentRoom.joinLobby(this);
                    }
                }
                break;
            case DISCONNECT:
                isRunning = false;
                break;
            case MESSAGE:
                currentRoom.sendMessage(this, pt.getMessage());
                break;
            default:
                System.out.println("Unhandled payload on the server : " + pt);
                break;
        } 
    }


    @Override
    public void run(){
        try{
            isRunning = true;
            Payload fromClient; // changed to payload type
            while (isRunning && !client.isClosed() && (fromClient =(Payload)in.readObject()) != null){
                System.out.println("Received from client: " + fromClient);
                processPayload(fromClient);
                
            }           // close the while loop
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
        if (currentRoom != null){ // more server functionality off-loaded to the Room
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
