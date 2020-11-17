import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class SocketClient{
	private Socket server;
	private Thread inputThread;
	private Thread fromServerThread;

	public void connect(String address, int port){
		try{
			server = new Socket(address, port);
			System.out.println("Client has connected.");
		} catch(UnknownHostException e){
			e.printStackTrace();
		} catch(IOException e){
			e.printStackTrace();
		}
	}

	private void listenForKeyboard(Scanner si, ObjectOutputStream out){
		inputThread = new Thread(){
			@Override
			public void run(){
				try{
					
					while(!server.isClosed()){
						System.out.println("Waiting for input..");
						String line = si.NextLine();

						if(!"quit".equalsIgnoreCase(line) && line !=null){
							out.writeObject(line);
						} else{
							System.out.println("Stopping input thread.");
							out.writeObject("Bye!");
							break;
						}
					}
					try{
						sleep(50);
					} catch (Exception e){
						System.out.println("Problem sleeping thread");
						e.printStackTrace();
					}
				}
				catch (Exception e){
					e.printStackTrace();
				} finally {
					close();
					System.out.println("Stopped listening to console input.");
				}
			}
		};
		inputThread.start();
	}
	private void listenForServerMessage(ObjectInputStream in){
		fromServerThread = new Thread(){
		@Override
		public void run(){
			try{
				String fromServer;
				while (!server.isClosed() && (fromServer = (String) in.readObject()) != null){
					System.out.println(fromServer);
				}
			} catch (Exception e){
				if (!server.isClosed()){
					e.printStackTrace();
					System.out.println("Server closed connection.");
				} else{
					System.out.println("Connection closed.");
				}
			} finally{
				close();
				System.out.println("Stopped listening to server input");
			}
		}
	};
	fromServerThread.start();
}
	public void start() throws IOException{
		if (server == null){
			return;
		}
		System.out.println("Client started.");
		try(Scanner si = new Scanner(System.in);
			ObjectOutputStream out = new ObjectOutputStream(server.getOutputStream());
			ObjectInputStream in = new ObjectInputStream(server.getInputStream());){
				listenForKeyboard(si, out);

				listenForServerMessage(in);

				while(!server.isClosed()){
					Thread.sleep(50);

				}
			System.out.println("Exited loop.");
			System.out.println("Press enter to stop the program.");
		} catch (Exception e){
			e.printStackTrace();
		} finally{
			close();
		}
	}
	private void close(){
		if(server != null && !server.isClosed()){
			try{
				server.close();
				System.out.println("Closed socket.");
			} catch (IOException e){
				e.printStackTrace();
			}
		}
	}
	public static void main(String[] args){
		SocketClient client = new SocketClient();
		int port = -1;
		try{
			port = Integer.parseInt(args[0]);
		} catch (Exception e){
			System.out.println("Invalid port!");
		}
		if (port == -1){
			return;
		}
		client.connect("127.0.0.1", port);
		try{
			client.start();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
}
