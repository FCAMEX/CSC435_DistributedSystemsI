/*--------------------------------------------------------

1. Fernando Araujo 4/20/2019

2. java version: build 1.8.0_181-b13

3. Precise command-line compilation examples / instructions:
> javac JokeClient.java


4. Precise examples / instructions to run this program:
In separate shell windows:

>java JokeClient
in order to connect to the JokeServer using the default localhost address and port 4545

OR

> java JokeClient <IPaddr>
in order to connect to the JokeServer using a custom address and port 4545

OR

> java JokeClient <IPaddr> <IPaddr> 
in order to connect to the JokeServer using a custom address and port 4545
and be able to switch to the secondary JokeServer using a custom address and port 4546


5. List of files needed for running the program.

 a. checklist.html
 b. JokeServer.java
 c. JokeClient.java
 d. JokeClientAdmin.java
 e. JokeLog.txt

5. Notes:

a. Able to request and display a Joke/Proverb with the Enter command
b. Able to switch between primary and secondary JokeServers with the s command
c. Can exit with the quit command and the user data is deleted on the JokeServer
----------------------------------------------------------*/

import java.io.*; 
import java.net.*; //Import needed libraries
import java.util.UUID;

public class JokeClient{
	
	private static final int PRIMARY_PORT = 4545; //default port for connection with primary JokeServer
	private static final int SECONDARY_PORT = 4546;//default port for connection with secondary JokeServer
	
	public static void main (String args[]){
		
		String primaryAddress = "localhost"; //primary address/IP to connect, starts initialized to "localhost"
		String secondaryAddress = ""; //secondary addresss/IP, starts empty, only initialized if passed in as an argument
		String userName = ""; //current user's username
		Boolean isPrimary = true; //indicates whether we are currently connected to a primary JokeServer or not
		UUID id = UUID.randomUUID(); //uniqueId used to identify this connection
		String userId = id.toString(); 
		
		//gets the primaryAddress from console arguments or defaults to localhost
		if(args.length > 0) 
			primaryAddress = args[0];
		//gets the secondaryAddress from arguments
		if(args.length > 1) {
			secondaryAddress = args[1];
		}
		
		//Announces the start of the client
		System.out.println("Fernando Araujo's Joke Client.\n");
		
		//Announces available servers to connect to
		System.out.println("Server one: " + primaryAddress + ", port " + PRIMARY_PORT);
		if(!secondaryAddress.isEmpty()) {
			System.out.println("Server two: " + secondaryAddress + ", port "+ SECONDARY_PORT);
		}
		
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String input;
		
		try{
			
			do {
				//Asks for user to input username
				System.out.println("Please enter your User Name: ");
				System.out.flush();
				userName = in.readLine();
				if(userName.isEmpty()) {
					System.out.println("Empty User Names are not allowed");
				}
				}while(userName.isEmpty()); //keep asking for a username until we get a valid input
				
				//explain the list of available commands
				System.out.println("Please input a command, press Enter for a Joke/Proverb, s to toggle servers or type \"quit\" to exit.");
				System.out.flush();
				
				do{
					//wait for user command input
					input = in.readLine();
					
					//if we receive the quit command, pass this command to the JokeServer in order to delete this user's data
					//we delete data from both primary and secondary server if possible
					if(input.equalsIgnoreCase("quit")){
						getServerData(userName, userId, true, primaryAddress, PRIMARY_PORT);
						if(!secondaryAddress.isEmpty()) {
							getServerData(userName, userId, true, secondaryAddress, SECONDARY_PORT);
						}
					
					//if we receive the "s" toggle command, we will switch between the primary and secondary server if available
					}else if(input.equalsIgnoreCase("s")) {
						if(secondaryAddress == null || secondaryAddress.isEmpty()) {
							System.out.println("No secondary server being used");
						}else {
							
							if(isPrimary) {
								System.out.println("Now communicating with: " + secondaryAddress + ", port " + SECONDARY_PORT);
							}else {
								System.out.println("Now communicating with: " + primaryAddress + ", port " + PRIMARY_PORT);
							}
							isPrimary = !isPrimary; //toggle the primary indicator
						}
					//if we receive an empty string (Enter) we will request a Joke/Proverb depending on the current JokeServer mode
					}else if (input.isEmpty()) {
						
						if(isPrimary) {
							getServerData(userName, userId, false, primaryAddress, PRIMARY_PORT);
						}else {
							getServerData(userName, userId, false, secondaryAddress, SECONDARY_PORT);
						}
					}
			
			}while(input.indexOf("quit") < 0); //keep asking for new commands until user quits
				
				System.out.println("Received quit command and exited.");
		
		}catch(IOException x){
			x.printStackTrace();
		}
			
	}
	
	static void getServerData(String userName, String userId, Boolean quit, String serverName, int port){
		//method that sends requests to the JokeServer
		
		Socket sock;
		BufferedReader fromServer;
		PrintStream toServer;
		String textFromServer; //variable that holds the reply from the JokeServer
		
		try{
			sock = new Socket(serverName, port); //opening a socket to communicate with JokeServer
			fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			toServer = new PrintStream(sock.getOutputStream());
			
			toServer.println(userName); //sending the userName to the JokeServer
			toServer.flush();
			
			toServer.println(userId); //sending the userId (UUID) to the JokeServer
			toServer.flush();
			
			toServer.println(quit.toString()); //sending a quit indicator to the JokeServer
			toServer.flush();
			
			for(int i = 1; i <=3; i++){ 
				textFromServer = fromServer.readLine(); 
				if(textFromServer != null) System.out.println(textFromServer); //print response if available
			}
			sock.close();
			
		}catch(IOException x){
			System.out.println("Socket error.");
			x.printStackTrace();
		}
	}
}
