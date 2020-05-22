/*--------------------------------------------------------

1. Fernando Araujo 4/20/2019

2. java version: build 1.8.0_181-b13

3. Precise command-line compilation examples / instructions:
> javac JokeClientAdmin.java


4. Precise examples / instructions to run this program:
In separate shell windows:

>java JokeClientAdmin
in order to connect to the JokeServer using the default localhost address and port 5050

OR

> java JokeClientAdmin <IPaddr>
in order to connect to the JokeServer using a custom address and port 5050

OR

> java JokeClientAdmin <IPaddr> <IPaddr> 
in order to connect to the JokeServer using a custom address and port 5050
and be able to switch to the secondary JokeServer using a custom address and port 5051


5. List of files needed for running the program.

 a. checklist.html
 b. JokeServer.java
 c. JokeClient.java
 d. JokeClientAdmin.java
 e. JokeLog.txt

5. Notes:

a. Can switch between Primary and Secondary JokeServers
b. Is able to Shutdown the JokeServer by sending the shutdown command
c. Can exit seamlessly with the quit command
d. Can switch the JokeServer mode between Joke/Proverb with the ENTER command
----------------------------------------------------------*/
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class JokeClientAdmin {
	
	private static final int ADMIN_PRIMARY_PORT = 5050; //default primary port for connection with Admin Server
	private static final int ADMIN_SECONDARY_PORT = 5051; //default secondary port for connection with Admin Server
	static boolean isPrimary = true; //indicates whether we are connected with a primary or secondary server
	
	public static void main(String args[]) {
		
		String primaryAddress = "localhost"; //primaryAddress, initialized by default to localhost
		String secondaryAddress = ""; //secondaryAddress, only initialized if passed as an argument
		
		System.out.println("Fernando Araujo's Joke Admin Client.\n");
		
		//changes primaryAddress based on arguments
		if(args.length > 0) {
			primaryAddress = args[0];
		}
		//changes secondaryAddress based on arguments
		if(args.length > 1) {
			secondaryAddress = args[1];
		}
		
		//announces available Admin servers to connect to
		System.out.println("Admin server one: " + primaryAddress + ", port " + ADMIN_PRIMARY_PORT);
		if(!secondaryAddress.isEmpty()) {
			System.out.println("Admin server two: " + secondaryAddress + ", port "+ ADMIN_SECONDARY_PORT);
		}
		
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String input; //holds the commands to be sent to the Admin Server
		
		try {
			//instructions related to available commands
			System.out.println("Please input a command, press Enter to toggle between Joke/Proverb mode, s to toggle between servers, type \"quit\" to exit or \"shutdown\" to shutdown the server.");
			System.out.flush();
			
			
			do {
				input = in.readLine(); //reading the user command
				
				//if the toggle between server's command is received, change the primary flag and announce which server we will communicate to next
				if(input.equalsIgnoreCase("s")) {
					if(secondaryAddress.isEmpty()) {
						System.out.println("No secondary server being used.");
					}else {
						if(isPrimary) {
							System.out.println("Now communicating with: " + secondaryAddress + ", port " + ADMIN_SECONDARY_PORT);
						}else {
							System.out.println("Now communicating with: " + primaryAddress + ", port " + ADMIN_PRIMARY_PORT);
						}
						isPrimary = !isPrimary;
					}
				//if we receive either Enter or shutdown commands, we just pass them over to the Admin Server
				}else if(input.isEmpty() || input.equalsIgnoreCase("shutdown")) {
					
					if(isPrimary) {
						sendServerCommand(input, primaryAddress, ADMIN_PRIMARY_PORT);
					}else {
						sendServerCommand(input, secondaryAddress, ADMIN_SECONDARY_PORT);
					}
				}
			}while(input.indexOf("quit") < 0);
			//if the quit command is received, just exit the loop and announce that you have exited
			System.out.println("Received quit command and exited.");
			
		}catch(IOException io) {
			System.out.println(io);
		}
	}
	
	static void sendServerCommand(String input, String serverName, int port) {
		//method that handles communication with the Admin Server
		
		Socket sock;
		BufferedReader fromServer;
		PrintStream toServer;
		String textFromServer; //holds response from the Admin Server
		
		try{
			sock = new Socket(serverName, port);
			fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			toServer = new PrintStream(sock.getOutputStream());
			
			toServer.println(input); //pass the user command through 
			toServer.flush();
			textFromServer = fromServer.readLine();// ready any response from the Admin Server
			
			if(textFromServer != null) {
				System.out.println(textFromServer);
			}
			
			sock.close();
		}catch(IOException io) {
			System.out.println(io);
		}
	}

}
