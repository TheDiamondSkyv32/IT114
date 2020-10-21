package Help.bin;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SocketServer {
    int port = 3000;
    public static boolean isRunning = false;
    private List<ServerThread> clients = new ArrayList<ServerThread>();

    private void start(int port){
        this.port = port;
        System.out.println("Waiting for client..");
        try(ServerSocket serverSocket = new ServerSocket(port);){
            isRunning = true;
            while (SocketServer.isRunning){
                try{
                    Socket client = serverSocket.accept();
                    System.out.println("Client connecting......");
                    ServerThread thread = new ServerThread(client, this);
                    thread.start();
                    clients.add(thread);
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
                System.out.println("Clsoing server socket.");
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    protected synchronized void disconnect(ServerThread client){
        long id = client.getId();
        clients.remove(client);
        broadcast("Disconnected ", id);
    }

    public synchronized void broadcast(String message, long id){
        message = String.format("User[%d]: %s", id, message);

        Iterator<ServerThread> it = clients.iterator();
        while(it.hasNext()){
            ServerThread client = it.next();
            boolean wasSuccessful = client.send(message);
            if(!wasSuccessful){
                System.out.println("Removing disconnected cleint from client list.");
                it.remove();
                broadcast("Disconnected user[%d]", id);
            }
        }
    }
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
