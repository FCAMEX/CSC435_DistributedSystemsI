/* 2012-05-20 Version 2.0

Thanks John Reagan for this well-running code which repairs the original
obsolete code for Elliott's HostServer program. I've made a few additional
changes to John's code, so blame Elliott if something is not running.

-----------------------------------------------------------------------

Play with this code. Add your own comments to it before you turn it in.

-----------------------------------------------------------------------
NOTE: This is NOT a suggested implementation for your agent platform,
but rather a running example of something that might serve some of
your needs, or provide a way to start thinking about what YOU would like to do.
You may freely use this code as long as you improve it and write your own comments.

-----------------------------------------------------------------------

TO EXECUTE: 

1. Start the HostServer in some shell. >> java HostServer

1. start a web browser and point it to http://localhost:1565. Enter some text and press
the submit button to simulate a state-maintained conversation.

2. start a second web browser, also pointed to http://localhost:1565 and do the same. Note
that the two agents do not interfere with one another.

3. To suggest to an agent that it migrate, enter the string "migrate"
in the text box and submit. The agent will migrate to a new port, but keep its old state.

During migration, stop at each step and view the source of the web page to see how the
server informs the client where it will be going in this stateless environment.

-----------------------------------------------------------------------------------

COMMENTS:

This is a simple framework for hosting agents that can migrate from
one server and port, to another server and port. For the example, the
server is always localhost, but the code would work the same on
different, and multiple, hosts.

State is implemented simply as an integer that is incremented. This represents the state
of some arbitrary conversation.

The example uses a standard, default, HostListener port of 1565.

-----------------------------------------------------------------------------------

DESIGN OVERVIEW

Here is the high-level design, more or less:

HOST SERVER
  Runs on some machine
  Port counter is just a global integer incrememented after each assignment
  Loop:
    Accept connection with a request for hosting
    Spawn an Agent Looper/Listener with the new, unique, port

AGENT LOOPER/LISTENER
  Make an initial state, or accept an existing state if this is a migration
  Get an available port from this host server
  Set the port number back to the client which now knows IP address and port of its
         new home.
  Loop:
    Accept connections from web client(s)
    Spawn an agent worker, and pass it the state and the parent socket blocked in this loop
  
AGENT WORKER
  If normal interaction, just update the state, and pretend to play the animal game
  (Migration should be decided autonomously by the agent, but we instigate it here with client)
  If Migration:
    Select a new host
    Send server a request for hosting, along with its state
    Get back a new port where it is now already living in its next incarnation
    Send HTML FORM to web client pointing to the new host/port.
    Wake up and kill the Parent AgentLooper/Listener by closing the socket
    Die

WEB CLIENT
  Just a standard web browser pointing to http://localhost:1565 to start.

 -------------------------------------------------------------------------------------------------------------------------------------
 My ONW EXPLANATION:
 
 The Host Server runs at a given machine, residing in port 1565.
 It waits for a connection to this port from the browser and spins up a new Agent Listener.
 This listener starts up with a state of 0 and at port 300X (starting with 1 and increasing on each connection to the Host Server)
 The listener waits for connections and initiates new agent workers to play the animal game or to migrate
 Depending on the user request the Agent Worker pretends to play the game or its port gets swapped around to a new one
 The user gets to see HTML responses in order to interact with the Worker
 If the Agent gets Migrated, the previous Agent Listener gets killed and its socket is closed.
  -------------------------------------------------------------------------------*/


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

class AgentWorker extends Thread {
	//this class handles the communication with the user. It contains the logic to generate HTML responses 
	//to the user migrate and person commands.
	
	Socket sock; //socket that will be utilized to connect to server
	agentHolder parentAgentHolder; //keeps the state of the agent and port count
	int localPort; //the port to be used by this specific request
	
	//basic constructor
	AgentWorker (Socket s, int prt, agentHolder ah) {
		sock = s;
		localPort = prt;
		parentAgentHolder = ah;
	}
	public void run() {
		
		PrintStream out = null;
		BufferedReader in = null;
		//the server is just the local machine
		String NewHost = "localhost";
		int NewHostMainPort = 1565;		//the port for the main worker 
		String buf = "";
		int newPort;
		Socket clientSock;
		BufferedReader fromHostServer;
		PrintStream toHostServer;
		
		try {
			out = new PrintStream(sock.getOutputStream()); //get the output stream from the socket
			in = new BufferedReader(new InputStreamReader(sock.getInputStream())); //get the input stream from the socket
			
			//get one line input from the client
			String inLine = in.readLine();
			//to allow for usage on non-ie browsers, I had to accurately determine the content
			//length and as a result need to build the html response so i can determine its length.
			StringBuilder htmlString = new StringBuilder();
			
			//put the request in the log
			System.out.println();
			System.out.println("Request line: " + inLine);
			
			if(inLine.indexOf("migrate") > -1) {
				//check the input for the existance of the migrate keyword. If it is present, executie code to change to a new port.
				
				clientSock = new Socket(NewHost, NewHostMainPort); //make a new socket with the main host and port 
				fromHostServer = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
				//communicate with the main port and ask for migration, so that it accepts our current state and port and moves us to a new port
				toHostServer = new PrintStream(clientSock.getOutputStream());
				toHostServer.println("Please host me. Send my port! [State=" + parentAgentHolder.agentState + "]");
				toHostServer.flush();
				
				//wait for the response
				for(;;) {
					//read the line and check for provided port
					buf = fromHostServer.readLine();
					if(buf.indexOf("[Port=") > -1) {
						break;
					}
				}
				//get the new target port, parse its value and log it
				String tempbuf = buf.substring( buf.indexOf("[Port=")+6, buf.indexOf("]", buf.indexOf("[Port=")) );
				newPort = Integer.parseInt(tempbuf);
				System.out.println("newPort is: " + newPort);
				
				//prepare the html message that will be seen by the user
				htmlString.append(AgentListener.sendHTMLheader(newPort, NewHost, inLine));
				//tell the user that the migration command has been successfuly acquired
				htmlString.append("<h3>We are migrating to host " + newPort + "</h3> \n");
				htmlString.append("<h3>View the source of this page to see how the client is informed of the new location.</h3> \n");
				htmlString.append(AgentListener.sendHTMLsubmit());

				//here we proceed to log when we are killing the parent process and we close the socket.
				System.out.println("Killing parent listening loop.");
				ServerSocket ss = parentAgentHolder.sock;
				ss.close();
				
				
			} else if(inLine.indexOf("person") > -1) {
				//increase the agent state count
				parentAgentHolder.agentState++;
				//just send an HTML message back to the user that contains the current state count and migration question
				htmlString.append(AgentListener.sendHTMLheader(localPort, NewHost, inLine));
				htmlString.append("<h3>We are having a conversation with state   " + parentAgentHolder.agentState + "</h3>\n");
				htmlString.append(AgentListener.sendHTMLsubmit());

			} else {
				//we couldnt find a person variable, so we probably are looking at a fav.ico request
				//tell the user it was invalid
				htmlString.append(AgentListener.sendHTMLheader(localPort, NewHost, inLine));
				htmlString.append("You have not entered a valid request!\n");
				htmlString.append(AgentListener.sendHTMLsubmit());		
				
		
			}
			//actually send the HTML response
			AgentListener.sendHTMLtoStream(htmlString.toString(), out);
			
			//close the socket
			sock.close();
			
			
		} catch (IOException ioe) {
			System.out.println(ioe);
		}
	}
	
}
/**
 * Object utilized to store the socket and the current state of a given agent
 * It is useful since it allows it to pass the agent to a different port
 */
class agentHolder {
	ServerSocket sock;
	int agentState; //state counter
	
	//constructor
	agentHolder(ServerSocket s) { sock = s;}
}
/**
 * Object Thread created by the hostserver whenever it receives a new request at port 1565
 *
 */
class AgentListener extends Thread {
	
	Socket sock;
	int localPort;
	
	//basic constructor
	AgentListener(Socket As, int prt) {
		sock = As;
		localPort = prt;
	}
	//start the agent at state = 0
	int agentState = 0;
	
	//this method is called during start()
	public void run() {
		BufferedReader in = null;
		PrintStream out = null;
		String NewHost = "localhost";
		System.out.println("In AgentListener Thread");		
		try {
			String buf;
			out = new PrintStream(sock.getOutputStream());
			in =  new BufferedReader(new InputStreamReader(sock.getInputStream()));
			
			//read the first input line
			buf = in.readLine();
			
			//if the input contains a state, parse and store it
			if(buf != null && buf.indexOf("[State=") > -1) {
				//get the state from the input line
				String tempbuf = buf.substring(buf.indexOf("[State=")+7, buf.indexOf("]", buf.indexOf("[State=")));
				//parse and log it
				agentState = Integer.parseInt(tempbuf);
				System.out.println("agentState is: " + agentState);
					
			}
			
			System.out.println(buf);
			//string builder to concatenate the generated HTML response text
			StringBuilder htmlResponse = new StringBuilder();
			//output first request html to user
			//show the port and display the form. starting at state = 0
			htmlResponse.append(sendHTMLheader(localPort, NewHost, buf));
			htmlResponse.append("Now in Agent Looper starting Agent Listening Loop\n<br />\n");
			htmlResponse.append("[Port="+localPort+"]<br/>\n");
			htmlResponse.append(sendHTMLsubmit());
			sendHTMLtoStream(htmlResponse.toString(), out);
			
			//now open a connection at current port and store it in agentHolder
			ServerSocket servsock = new ServerSocket(localPort,2);
			agentHolder agenthold = new agentHolder(servsock);
			agenthold.agentState = agentState;
			
			//continuously wait for new connections
			while(true) {
				//accept and log the new connection
				sock = servsock.accept();
				System.out.println("Got a connection to agent at port " + localPort);
				//spint up a new AgentWorker to handle the new connection.
				new AgentWorker(sock, localPort, agenthold).start();
			}
		
		} catch(IOException ioe) {
			//error and port switch catcher
			System.out.println("Either connection failed, or just killed listener loop for agent at port " + localPort);
			System.out.println(ioe);
		}
	}
	//send the html header but NOT the response header
	//otherwise same as original implementation. Load html, load form,
	//add port to action attribute so the next request goes back to the port
	//or goes to the new one we are listening on
	static String sendHTMLheader(int localPort, String NewHost, String inLine) {
		
		StringBuilder htmlString = new StringBuilder();

		htmlString.append("<html><head> </head><body>\n");
		htmlString.append("<h2>This is for submission to PORT " + localPort + " on " + NewHost + "</h2>\n");
		htmlString.append("<h3>You sent: "+ inLine + "</h3>");
		htmlString.append("\n<form method=\"GET\" action=\"http://" + NewHost +":" + localPort + "\">\n");
		htmlString.append("Enter text or <i>migrate</i>:");
		htmlString.append("\n<input type=\"text\" name=\"person\" size=\"20\" value=\"YourTextInput\" /> <p>\n");
		
		return htmlString.toString();
	}
	//complete the HTML that was initiated by sendHTMLheader
	static String sendHTMLsubmit() {
		return "<input type=\"submit\" value=\"Submit\"" + "</p>\n</form></body></html>\n";
	}
	//send the response headers and calculate the content length to allow for multiple browser communication
	static void sendHTMLtoStream(String html, PrintStream out) {
		
		out.println("HTTP/1.1 200 OK");
		out.println("Content-Length: " + html.length());
		out.println("Content-Type: text/html");
		out.println("");		
		out.println(html);
	}
	
}
/**
 * 
 * main hostserver class. this listens on port 1565 for requests. Assumes that all ports >3000 are free
 * creates a new AgentListener thread on every connenction
 */
public class HostServer {
	
	public static int NextPort = 3000; //base listening port
	
	public static void main(String[] a) throws IOException {
		int q_len = 6;
		int port = 1565;
		Socket sock;
		
		ServerSocket servsock = new ServerSocket(port, q_len);
		System.out.println("John Reagan's DIA Master receiver started at port 1565.");
		System.out.println("Connect from 1 to 3 browsers using \"http:\\\\localhost:1565\"\n");
		
		//while loop that continously listens to connections at port 1565
		while(true) {
			//we increment the nextport variable after each connection has been received at port 1565
			NextPort = NextPort + 1;
			sock = servsock.accept();
			//here we create an AgentListener thread and log its creation at the nextport
			System.out.println("Starting AgentListener at port " + NextPort);
			new AgentListener(sock, NextPort).start(); 
		}
		
	}
}
