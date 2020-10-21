package Help.bin;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerThread extends Thread{
    private Socket client;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private boolean isRunning = false;
    private SocketServer server;

    public ServerThread(Socket myClient, SocketServer server) throws IOException{
        this.client = myClient;
        this.server = server;
        out = new ObjectOutputStream(client.getOutputStream());
        in = new ObjectInputStream(client.getInputStream());
    }

    public boolean send(String message){
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
                server.broadcast(fromClient, this.getId());
            }
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
        if (server != null){
            server.disconnect(this);
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