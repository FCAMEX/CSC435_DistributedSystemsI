/** File is: InetClient.java, Version 1.8
	
	A client for InetServer. Elliott, after Hughes, Shoffner, Winslow
	
	This will not run unless TCP/IP is loaded on your machine.
----------------------------------------------------------------------*/

import java.io.*; 
import java.net.*; //Import needed libraries

public class InetClient{
	public static void main (String args[]){
		String serverName;
		//gets the serverName from console arguments or defaults to localhost
		if(args.length < 1) serverName = "localhost";
		else serverName = args[0];
		
		//prints default information
		System.out.println("Clark Elliott's Inet Client, 1.8.\n");
		System.out.println("Using server: " + serverName + ", Port: 5050");
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		
		try{
			String name;
			do{
				System.out.print("Enter a hostname or an IP address, (quit) to end: ");
				System.out.flush();
				name = in.readLine();
				if(name.indexOf("quit") < 0)
					getRemoteAddress(name, serverName);
			}while(name.indexOf("quit") < 0); //keep asking for hostname or IP until quit
				System.out.println("Cancelled by user request.");
					
		}catch(IOException x){
			x.printStackTrace();
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
	
	static void getRemoteAddress(String name, String serverName){
		Socket sock;
		BufferedReader fromServer;
		PrintStream toServer;
		String textFromServer;
		
		try{
			sock = new Socket(serverName, 5050); //opening a socket with server
			fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			toServer = new PrintStream(sock.getOutputStream());
			
			toServer.println(name); // sending the name to the server (IP/machine name) 
			toServer.flush();
			
			for(int i = 1; i <=3; i++){ //reading 3 lines from the server response
				textFromServer = fromServer.readLine(); //reading a line from the server socket
				if(textFromServer != null) System.out.println(textFromServer); //if the response is not empty, print it
			}
			
			sock.close(); //close the socket connection
		}catch(IOException x){
			System.out.println("Socket error.");
			x.printStackTrace();
		}
	}
}
