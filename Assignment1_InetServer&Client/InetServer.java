/** File is: InetServer.java, Version 1.8 (small)

	A multithreaded server for InetClient. Elliott, after Hughes, Shoffner, Winslow

	This will not run unless TCP/IP is loaded on your machine.
--------------------------------------------------------------------------------*/

import java.io.*; // Getting needed external libraries
import java.net.*; 

class Worker extends Thread { 
	// Defining Worker class that extends basic Thread class functionality by adding Sockets
	Socket sock;	// Variable that holds the passed in Socket
	Worker (Socket s) { // Basic Constructor with socket as argument
		sock = s;
	}
	
	public void run(){
		
		PrintStream out = null;
		BufferedReader in = null;
		
		try{
			// Getting the inputs and outputs of the socket if available
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			out = new PrintStream(sock.getOutputStream());
			
			try {
				String name;
				name = in.readLine(); //Reading the machine name from the socket
				System.out.println("Looking up " + name);
				printRemoteAddress(name, out); //Prints address information of the client
				
			}catch (IOException ex){
				// Was unable to read/write the socket
				System.out.println("Server read error");
				ex.printStackTrace();
			}
			
			sock.close(); //Closing the connection, but this does not shut down remote server
		} catch (IOException ioex){
			System.out.println(ioex);
		}				
	}
	
	static void printRemoteAddress(String name, PrintStream out){
		try {
			out.println("Looking up " + name + "...");
			InetAddress machine = InetAddress.getByName(name); //Get InetAddress object based on the machine name
			out.println("Host name : " + machine.getHostName()); //Print the host name of the client
			out.println("Host IP : " + toText(machine.getAddress())); //Print the host IP
		}catch(UnknownHostException ex){
			out.println("Failed in attempt to look up " + name);
		}
	}
	
	static String toText(byte ip[]){
		StringBuffer result = new StringBuffer();
		for(int i = 0; i < ip.length; ++i){
			if(i > 0) result.append(".");
			result.append(0xff & ip[i]);
		}
		return result.toString();
	}
}

public class InetServer{
	
	public static void main(String a[]) throws IOException{
		int q_len = 6; // Queue length that system honors
		int port = 5050; //Port to listen to
		Socket sock;
		
		ServerSocket servsock = new ServerSocket(port, q_len);
		
		System.out.println("Clark Elliott's Inet server 1.8 starting up, listening at port 5050.\n");
		while(true){
			sock = servsock.accept(); //Waiting for a client to connect
			new Worker(sock).start(); // Creates a new Worker to handle the client
		}
	}
}