import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class SocketClient implements AutoCloseable{
	private Socket server;
	private Thread inputThread;
	private Thread fromServerThread;
	private String clientName;

	private void readUserName(Scanner si){
		System.out.println("Enter a username and press ENTER. ");
		clientName = si.nextLine();
	}

	private Payload buildConnectionStatus(String name, boolean isConnect){
		Payload payload = new Payload();
		if(isConnect){
			payload.setPayloadType(PayloadType.CONNECT);
		}
		else{
			payload.setPayloadType(PayloadType.DISCONNECT);
		}
		payload.setClientName(name);
		return payload;
	}

	private Payload buildMessage(String message){
		Payload payload = new Payload();
		payload.setPayloadType(PayloadType.MESSAGE);
		payload.setClientName(clientName);
		payload.setMessage(message);
		return payload;
	}

	private void sendPayload(Payload pt, ObjectOutputStream out){
		try{
			out.writeObject(pt);
		} catch (IOException e){
			e.printStackTrace();
		}
	}

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
		if (inputThread != null){
			System.out.println("Input listener is likely already running.");
			return;
		}
		inputThread = new Thread(){
			@Override
			public void run(){
				try{
					readUserName(si);
					sendPayload(buildConnectionStatus(clientName, true), out);

					while (!server.isClosed()){
						System.out.println("Waiting for input");
						String line = si.nextLine();

						if (!"quit".equalsIgnoreCase(line) && line !=null){
							sendPayload(buildMessage(line), out);
						} else {
							System.out.println("Stopping the input thread.");
							sendPayload(buildConnectionStatus(clientName, false), out);
							break;
						}
						try{
							sleep(51);
						} catch (Exception e){
							System.out.println("Problem sleeping thread!");
							e.printStackTrace();
						}
					}
				}
				catch (Exception e){
					e.printStackTrace();
				}
				finally{
					close();
					System.out.println("Stopped listening to console input.");
						}
					}
				};
				inputThread.start(); 
			}
	private void listenForServerMessage(ObjectInputStream in){
		if(fromServerThread != null){
			System.out.println("Server Listener is likely already running.");
			return;
		}
		fromServerThread = new Thread(){
		@Override		
		public void run(){
			try{
				Payload fromServer;
				while (!server.isClosed() && (fromServer = (Payload)in.readObject()) != null){
					processPayload(fromServer);
				}
			}
			catch (Exception e){

				if (!server.isClosed()){
					e.printStackTrace();
					System.out.println("Server closed connection.");
				} 
				else {
					System.out.println("Connection closed.");
				}
			}
			finally{
				close();
				System.out.println("Stopped listening to server input");
			}
		}
	};
	fromServerThread.start(); //start the thread
}

	private void processPayload(Payload pt){
		switch (pt.getPayloadType()){
			case CONNECT:
				System.out.println(pt.getClientName() + ": " + pt.getMessage());
				break;
			case DISCONNECT:
				System.out.println(pt.getClientName() + ": " + pt.getMessage());
				break;
			case MESSAGE:
				System.out.println(pt.getClientName() + ": " + pt.getMessage());
				break;
			default:
			System.out.println("Unhandled payload on client : " + pt + "!");
		}
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
					Thread.sleep(69);

				}
			System.out.println("Exited loop.");
			System.out.println("Press ENTER to stop the program.");
		} catch (Exception e){
			e.printStackTrace();
		} finally{
			close();
		}
	}

	@Override
	public void close(){
		if(server != null && !server.isClosed()){
			try{
				server.close();
				System.out.println("Closed socket.");
			}
			catch (IOException e){
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args){
		int port = -1;
		try{
			port = Integer.parseInt(args[0]);
		}
		catch (Exception e){
			System.out.println("Invalid port.");
		}
		if (port > -1){
			try(SocketClient client = new SocketClient();){
				client.connect("127.0.0.1", port);
				client.start();
			}
		catch (IOException e){
			e.printStackTrace();
		}
	}
}
}
