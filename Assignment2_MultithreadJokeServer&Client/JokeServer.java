/*--------------------------------------------------------

1. Fernando Araujo 4/20/2019

2. java version: build 1.8.0_181-b13

3. Precise command-line compilation examples / instructions:
> javac JokeServer.java


4. Precise examples / instructions to run this program:

In separate shell windows:

In order to run a primary server
> java JokeServer

In order to run a secondary server.
> java JokeClient secondary

All acceptable commands are displayed on the various consoles.
(This includes s, Enter, quit and shutdown depending on the Client)

This runs across machines, in which case you have to pass the IP address of
the server to the clients. For example, if the server is running at
140.192.1.22 then you would type:

> java JokeClient 140.192.1.22
> java JokeClientAdmin 140.192.1.22

If you have two JokeServers running (a primary and a secondary), it is necessary to pass the IP address
of both servers to the clients in order to be able to switch between them.

>java JokeClient 140.192.1.22 141.222.1.33
>java JokeClientAdmin 140.192.1.22 141.222.1.33

5. List of files needed for running the program.

 a. checklist.html
 b. JokeServer.java
 c. JokeClient.java
 d. JokeClientAdmin.java
 e. JokeLog.txt

5. Notes:

a. Successfully returns a set of 4 jokes or 4 proverbs based on the current mode. 
b. Proverbs and Jokes are randomized utilizing the Collection.shuffle() method at the beginning and after every completed cycle
c. Handles multiple JokeClients and JokeClientAdmins at the same time
d. Successfully preserves the current joke/proverb state for a given user/connection
e. The JokeClientAdmin can change the JokeServer's mode, it can also send a shutdown command which gracefully shuts down the JokeServer
f. Deletes the data of a given user if he is given the quit command by the Joke Client
g. Deletes every user's data if given the shutdown command
h. Can run as a primary or secondary server
g. All communication and logging of a secondary JokeServer is preceded by <S2>
----------------------------------------------------------*/

import java.io.*; // Getting needed external libraries
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap; 


public class JokeServer{
	
	public static final int PRIMARY_PORT = 4545; //Primary port for joke/proverb server connection
	public static final int SECONDARY_PORT = 4546; //Secondary port for joke/proverb server connection
	static String mode = "Joke"; //holds the current mode for the Joke Server (either Joke or Proverb)
	static boolean isPrimary = true; //indicates if the server is the primary or secondary server
	static HashMap<String, UserData> usersMap = new HashMap<String, UserData>(); //holds all user information (Jokes/Proverbs and their current position)
	static boolean isRunning = true; //indicates if the server should keep running, only useful for shutdown functionality
	static String secondMark = "<S2> "; //string that needs to be prepended to all outgoing communication if the server is secondary
	
	public static void main(String ar[]) throws IOException{
	
		String secServ = "Secondary ";
		int port = 0; //holds the port that will be used for this server
		int q_len = 6; 
		
		//sets the port, primary flag and indicators based on whether the server is primary or secondary
		if(ar.length == 1 && ar[0].equalsIgnoreCase("secondary")){
			port = SECONDARY_PORT;
			isPrimary = false;
		}else{
			port = PRIMARY_PORT;
			secondMark = "";
			secServ = "";
		}
		
		Socket sock;
		ServerSocket sSock = new ServerSocket(port, q_len);
		
		Admin adminRun = new Admin(); //We instantiate the Admin class
		Thread ad = new Thread(adminRun); //We create a new thread for the Amin Server
		ad.start();
		
		System.out.println(secondMark +"Fernando Araujo's "+ secServ + "Joke Server starting up, listening at port " + port + ".\n");
		//starts the loop that will keep creating threads based on new connections.
		while(isRunning){
			sock = sSock.accept(); //Waiting for a JokeClient connection
			new JokeWorker(sock).start(); 
		}		
		sSock.close();
	}
}

class JokeWorker extends Thread { 
	// JokeWorker class that handles all communication with the JokeClient
	Socket sock;	
	
	JokeWorker (Socket s) { 
		sock = s;
	}
	
	public void run(){
		
		PrintStream out = null;
		BufferedReader in = null;
		UserData tempData = null; // temporary variable that holds the data for the current user
		
		try{
			// Getting the inputs and outputs of the socket if available
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			out = new PrintStream(sock.getOutputStream());
			
			try {
				String username; //currently connected user's username
				String userId; //currently connected user's userId (UUID)
				Boolean quitCommand; // whether the JokeClient sent the quit command or not
				
				username = in.readLine(); //Reading the client user name from the socket
				userId =  in.readLine(); //Reading the client userId from socket
				quitCommand = Boolean.valueOf(in.readLine()); //Reading the client quit command
				
				if(!quitCommand) { //if we did not get a quit command
					
					System.out.println(JokeServer.secondMark +"Looking up data for user: " + username + " " + userId);
					
					//we look for existing user data in the server's HashMap
					tempData = JokeServer.usersMap.get(username + userId);
					
					//if the data is not found, we proceed to initialize a new set of data (Jokes/Proverbs) and to randomize its order
					//we then proceed to store this new data using both the username and userid in order to have a relevant uniqueId
					if(tempData == null) {
						System.out.println(JokeServer.secondMark +"User data not found, creating a new set");
						tempData = new UserData(username);
						JokeServer.usersMap.put(username + userId, tempData);
					}else {
						System.out.println(JokeServer.secondMark +"User data found");
					}
					
					//we send the JokeClient either a Joke or a Proverb based on the current mode
					if(JokeServer.mode.equals("Joke")) {
						out.println(JokeServer.secondMark + tempData.getJoke());
						System.out.println(JokeServer.secondMark + "Sent " + JokeServer.mode +" " + (tempData.getJokeIndex() + 1) + "/4 to user: " + username + " " + userId);
						tempData.increaseJokeIndex(); //increase the index (0 - 3)
						if(tempData.getJokeIndex() == 0) {
							//log and send back message stating that the current cycle has completed
							System.out.println(JokeServer.secondMark + "JOKE CYCLE COMPLETED");
							out.println(JokeServer.secondMark + "JOKE CYCLE COMPLETED");
						}
					}else {
						out.println(JokeServer.secondMark + tempData.getProverb());
						System.out.println(JokeServer.secondMark + "Sent " + JokeServer.mode +" " + (tempData.getProverbIndex() + 1) + "/4 to user: " + username + " " + userId);
						tempData.increaseProverbIndex(); //increase the index (0 - 3)
						if(tempData.getProverbIndex() == 0) {
							//log and send back message stating that the current cycle has completed
							System.out.println(JokeServer.secondMark + "PROVERB CYCLE COMPLETED");
							out.println(JokeServer.secondMark + "PROVERB CYCLE COMPLETED");
						}
					}
				
				} else {
					
					//if we receive a quit command AND the user UUID is empty, this means we got an internal command telling us to shutdown
					if(userId.isEmpty()) {
						System.out.println(JokeServer.secondMark + "Shutting down server!");
						out.println(JokeServer.secondMark + "Server is Shutting down!");
						JokeServer.usersMap.clear(); //clear all the user information from the server
					
					//if we got a quit command AND a valid UUID, it means that the JokeClient wants to quit
					}else {
						//since the JokeClient session is ending, we follow to delete all of the current user data
						System.out.println(JokeServer.secondMark + "Quit command received from user: " + username + " " + userId + " user data has been deleted");
						if(JokeServer.usersMap.containsKey(username + userId)) {
							JokeServer.usersMap.remove(username + userId);
						}
					}
				}
				
			}catch (IOException ex){
				// Was unable to read/write the socket
				System.out.println(JokeServer.secondMark + "Server read error");
				ex.printStackTrace();
			}
			
			sock.close();
		} catch (IOException ioex){
			System.out.println(ioex);
		}				
	}
	
}

class UserData{
	//Class that represents a new Data Object for a given user
	
	private ArrayList<String> jokesArray = new ArrayList<String>(); //ArrayList containing all available Jokes
	private ArrayList<String> proverbArray = new ArrayList<String>(); //ArrayList containing all available Proverbs
	private int jokeIndex = 0; //the current jokeArray index from which to obtain a joke
	private int proverbIndex = 0; //the current proverbArray index from which to obtain a proverb
	
	//returns jokeIndex
	public int getJokeIndex() {
		return jokeIndex;
	}
	
	//returns proverbIndex
	public int getProverbIndex() {
		return proverbIndex;
	}
	
	//returns a joke based on the current jokeIndex position
	public String getJoke() {
		return jokesArray.get(jokeIndex);
	}
	
	//returns a proverb based on the current proverbIndex position
	public String getProverb() {
		return proverbArray.get(proverbIndex);
	}
	
	//method that both increases the jokeIndex position by 1
	//it also resets the index to 0 once the 4 jokes have been sent, and calls a method to re-randomize them
	public void increaseJokeIndex() {
		jokeIndex++;
		if(jokeIndex > 3) {
			jokeIndex = 0;
			Collections.shuffle(jokesArray);
		}
	}
	
	//method that both increases the proverbIndex position by 1
	//it also resets the index to 0 once the 4 proverbs have been sent, and calls a method to re-randomize them
	public void increaseProverbIndex() {
		proverbIndex++;
		if(proverbIndex > 3) {
			proverbIndex = 0;
			Collections.shuffle(proverbArray);
		}
	}
	
	//the UserData constructor, requires the username.
	//initializes both joke and proverb arrays and randomizes them
	UserData(String username){
		jokesArray.add("JA " + username + ": What happens to a frog's car when it breaks down?....It gets toad away.");
		jokesArray.add("JB " + username + ": What did the duck say when he bought lipstick?....Put it on my bill.");
		jokesArray.add("JC " + username + ": Can a kangaroo jump higher than the Empire State Building?....Of course. The Empire State Building can't jump.");
		jokesArray.add("JD " + username + ": Why couldn't the leopard play hide and seek?....Because he was always spotted.");
		proverbArray.add("PA " + username + ": Actions speak louder than words.");
		proverbArray.add("PB " + username + ": You can lead a horse to water, but you can't make him drink it.");
		proverbArray.add("PC " + username + ": The squeaky wheel gets the grease.");
		proverbArray.add("PD " + username + ": The enemy of my enemy is my friend.");
		Collections.shuffle(jokesArray);
		Collections.shuffle(proverbArray);
	}
}

class Admin implements Runnable {
	//class that represents the "Admin Server" that listens to commands fromt he JokeClientAdmin
	
	public static final int ADMIN_PRIMARY_PORT = 5050; //primary default port for admin communication
	public static final int ADMIN_SECONDARY_PORT = 5051; //secondary default port for admin communication
	
	public void run() {
		
		Socket sock;
		int q_len= 6;
		int adminPort; //holds the port chosen based on whether the JokeServer is primary or secondary
		
		if(JokeServer.isPrimary) {
			adminPort = ADMIN_PRIMARY_PORT;
		}else {
			adminPort = ADMIN_SECONDARY_PORT;
		}
		
		try {
		
		System.out.println(JokeServer.secondMark + "Fernando Araujo's Admin Server starting up, listening at port " + adminPort + ".\n");
		ServerSocket sSock = new ServerSocket(adminPort, q_len);
		
		//loop that keeps creating new threads based on new JokeClientAdmin connections, similar to the JokeClient
		while(JokeServer.isRunning) {
				sock = sSock.accept(); //waiting for JokeClientAdmin connection
				new AdminWorker(sock).start();
		}
		
		sSock.close();
		
		}catch(IOException io){
			System.out.println(io);
		}
	}
	
}

class AdminWorker extends Thread{
	//Class that extends thread and handles all the communication with a given JokeClientAdmin connection
	
	Socket sock;
	
	AdminWorker(Socket s){
		sock = s;
	}
	
	public void run(){
		
		PrintStream out = null;
		BufferedReader in = null;
		String input = ""; //holds the currently received command from the JokeClientAdmin
		
		try{
			// Getting the data in and out of the socket
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			out = new PrintStream(sock.getOutputStream());
			
			input = in.readLine(); //read the command
			
			/*if we receive the shutdown command, we will follow to set the isRunning flag to false
			This by itself is not enough to shutdown the server, since both the Admin and Joke Server are stuck trying to listen to a connection.
			We follow to create a local socket and send a shutdown command to both the Admin and Joke Server 
			*/
			if(input.equalsIgnoreCase("shutdown")) {
				
				JokeServer.isRunning = false; //set running flag to false
				System.out.println(JokeServer.secondMark + "Shutdown command received");
				out.println(JokeServer.secondMark + "Server has shutdown"); //notify the JokeClientAdmin that the server is shutting down
                int tempPort  = JokeServer.PRIMARY_PORT;
                if(!JokeServer.isPrimary) {
                	tempPort = JokeServer.SECONDARY_PORT;
                }
                Socket tempSock = new Socket("localhost", tempPort); //create a local socket to send the shutdown command to the JokeServer
                PrintStream outTemp = new PrintStream(tempSock.getOutputStream());
                outTemp.println();
                outTemp.flush();
                outTemp.println();
                outTemp.flush();
                outTemp.println("true");
                outTemp.flush();
                tempSock.close();
                
                tempPort = Admin.ADMIN_PRIMARY_PORT;
                if(!JokeServer.isPrimary) {
                	tempPort = Admin.ADMIN_SECONDARY_PORT;
                }
                tempSock = new Socket("localhost", tempPort); // send shutdown command to Admin
                outTemp = new PrintStream(tempSock.getOutputStream());
                outTemp.println();
                outTemp.flush();
                tempSock.close();
                
			}else if(input.isEmpty()) { //if we receive an Enter command
											
				//We switch the current JokeServer mode between Joke/Proverb
				if(JokeServer.mode.equals("Joke")) {
					JokeServer.mode = "Proverb";
				}else {
					JokeServer.mode = "Joke";
				}
				//We log the change and notify the JokeClientAdmin of the change
				String temp = JokeServer.secondMark + "JokeServer switched to " + JokeServer.mode + " mode";
				System.out.println(temp);
				out.println(temp);
				out.flush();
			}
			
		} catch(IOException io) {
			System.out.println(io);
		}
	}
}
