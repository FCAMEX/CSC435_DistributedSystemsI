/*--------------------------------------------------------

1. Fernando Araujo 5/4/2019

2. java version: build 1.8.0_181-b13

3. Precise command-line compilation examples / instructions:
> javac MyWebServer.java


4. Precise examples / instructions to run this program:

To run the server in shell windows:

> java MyWebServer

After running the server, go to a browser and test the following functionality.

Use the following or similar urls to retreive specific files:
http://localhost:2540/dog.txt
http://localhost:2540/cat.html
http://localhost:2540/sub-a/sub-b/cat.html

Use the following urls to look at the root directory structure:
http://localhost:2540/ or...
http://localhost:2540

Use the following url to test the addnums form adding functionality
http://condor.depaul.edu/elliott/435/hw/programs/mywebserver/addnums.html


5. List of files needed for running the program.

 a. checklist-mywebserver.html
 b. MyWebServer.java
 c. serverlog.txt
 d. http-streams.txt

5. Notes:

a. Successfully returns the full Directory and subdirectories link HTML
b. Successfully returns the content of html, Java and text files
c. Successfully adds numbers when using the addnums webform
d. It throws relevant errors when a file is not found, request is wrong or when under attack
e. Has simple safe measures to prevent attacks
f. It uses relevant MIME headers based on the file type
g. It was submitted on 5/5/2019 in order to obtain the extra credit
I assumed that there will be no white spaces in the names of the files or directories, otherwise this could couse problems.
----------------------------------------------------------*/

import java.io.*; // Getting needed external libraries
import java.net.*;

public class MyWebServer{
	
	public static final int PRIMARY_PORT = 2540; //Primary port for web server connection
	public static boolean isRunning = true;
	
	public static void main(String ar[]) throws IOException{
	
		int port = 0; //holds the port that will be used for this server
		int q_len = 6; 
		
		port = PRIMARY_PORT;
		
		Socket sock;
		ServerSocket sSock = new ServerSocket(port, q_len);
				
		System.out.println("Fernando Araujo's MyWebServer starting up, listening at port " + port + ".\n");
		//starts the loop that will keep creating threads based on new connections.
		while(isRunning){
			sock = sSock.accept(); //Waiting for a MyWebServer connections
			new WebWorker(sock).start(); 
		}		
		sSock.close();
	}
}

class WebWorker extends Thread { 
	// WebWorker class that handles all the requests
	Socket sock;	
	//Final variables used to build the HTML responses
	final String separator = "\r\n"; 
	final String CL = "Content-Length: "; 
	final String CT = "Content-Type: "; 
	final String HTTP_OK = "HTTP/1.1 200 OK";
	final String SERV = "Server: Fernando Araujo's Server";
	final String ERROR_404 = "404";
	final String ERROR_400 = "400";
	
	
	WebWorker (Socket s) { 
		sock = s;
	}
	
	public void run(){
		
		PrintStream out = null;
		BufferedReader in = null;
			
		try{
			// Getting the inputs and outputs of the socket if available
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			out = new PrintStream(sock.getOutputStream());
			String input = "";
			
			try {
				
				//keep reading until we get access to the GET command
				do{
					input = in.readLine();
					System.out.println(input);
					
				}while(input.isEmpty());
				
				//once the GET command is obtained we follow to build the HTTP response
				BuildHTTP(input, out);

				
			}catch (IOException ex){
				// Was unable to read/write the socket
				System.out.println("Server read error");
				ex.printStackTrace();
			}
			
			sock.close();
		} catch (IOException ioex){
			System.out.println(ioex);
		}				
	}
	
	public void SendHTTPError(String errorType, PrintStream out) {
		//method in charge of sending Not Found and Bad Request error messages
		
		String backMessage = "";
		String badHTTP = "";
		
		if(errorType.equalsIgnoreCase(ERROR_404)) { //Not Found file error message
			backMessage = "File Not Found";
			badHTTP = "HTTP/1.1 404 Not Found";
		}else if(errorType.equalsIgnoreCase(ERROR_400)) { // Bad Request error message
			backMessage = "Bad Request";
			badHTTP = "HTTP/1.1 400 Bad Request";
		}
		
		//Both log and also send the HTTP error message
		System.out.println();
		System.out.println("Sending HTTP SUM to Server");
		System.out.println(HTTP_OK);
		System.out.println(SERV);
		System.out.println(CL +  backMessage.getBytes().length);
		System.out.println(CT + "text/html");
		System.out.println();						
		
		out.print(badHTTP + separator);
		out.print(SERV + separator);
		out.print(CL + backMessage.getBytes().length + separator);
		out.print(CT + "text/html" + separator);
		out.print(separator);
		out.print(backMessage);
		out.flush();
		
	}
	
	public void BuildHTTP(String input, PrintStream out){
		//method in charge of building all the HTTP response messages based on the GET command received
		
		String file = null;
		String[] subInput = null;
		File realFile = null;
	
		//parsing the input based on the GET command
		if(input.contains("GET")){
			//splitting the command based on white spaces
			subInput = input.split("\\s+");
		}
		
		if(subInput != null && subInput.length > 1) {
			//if the received input is not empty add a .
			file = "." + subInput[1];
		}
		
		//if we did not receive a parseable GET command or we get a security attack, send a Bad Request error
		if(file == null || file.contains("../") || file.contains(":") || file.contains("|") )  {
			//send bad request error
			SendHTTPError(ERROR_400, out);
		}else {			
			
			//we read the file requested
			realFile = new File(file);
			
			//if the file name obtained contains cgi.... follow the cgi addition code path
			if(file.contains("cgi/addnums.fake-cgi")) {
				//cgi addition code path
				
				//code to parse userName, number1 and number2 from the received URL
				String[] cgiURL = file.split("cgi?");
				String secondHalf = cgiURL[2].split(" ")[0];
				String[] cgiURL2 = secondHalf.split("&");
				String userName = cgiURL2[0].split("=")[1];
				String number1 = cgiURL2[1].split("=")[1];
				String number2 = cgiURL2[2].split("=")[1];
				int total = 0;
				
				//calculating the addition between the two numbers
				total = Integer.parseInt(number1) + Integer.parseInt(number2);
				
				//message to be sent back
				String backMessage = "";
				
				//if userName, number1 and number2 are actually available, do the addition and send it back
				if(!userName.isEmpty() && !number1.isEmpty() && !number2.isEmpty()) {
					backMessage = "Dear "+ userName + ", the sum of " + number1 + " and "+ number2 + " is " + total + ".";
				}else {
					//otherwise, send appropriate error messages
					if(userName.isEmpty()) {
						backMessage = backMessage + "The UserName is empty, please input a valid UserName. \n";
					}
					if(number1.isEmpty()) {
						backMessage = backMessage + "The First Number is empty, please input a valid First Number. \n";
						
					}
					if(number2.isEmpty()) {
						backMessage = backMessage + "The Second Number is empty, please input a valid Second Number. \n";
						
					}
					
				}
				
				//Send and logg the message sent back, be it the sum or an error
				System.out.println();
				System.out.println("Sending HTTP SUM to Server");
				System.out.println(HTTP_OK);
				System.out.println(SERV);
				System.out.println(CL +  backMessage.getBytes().length);
				System.out.println(CT + "text/html");
				System.out.println();						
				
				out.print(HTTP_OK + separator);
				out.print(SERV + separator);
				out.print(CL + backMessage.getBytes().length + separator);
				out.print(CT + "text/html" + separator);
				out.print(separator);
				out.print(backMessage);
				out.flush();
				
				
			}else {
				//normal file or directory code path
				
				if(!file.endsWith("/")) {
					//if the request is for a file
					if(realFile.exists() && realFile.isFile()) {
						//if the file exists and is valid continue this code path
						
						//all relevant information needed for the header
						String fileData = "";
						String currentLine = "";
						String content = "";
						String extension = realFile.getName();
						extension = extension.substring(extension.lastIndexOf(".") + 1);
						
						//choose a content type based on the file extension
						if(extension.equals("html") || extension.equals("htm")) { //for html
							content = "text/html";
						}else if(extension.equals("java") || extension.equals("txt")) { //for java or txt
							content = "text/plain";
						}else if(extension.equals("ico")) { //for the icons
							content = "image/x-icon";
						
							out.println("\r\n\r\n");
							return;
						}
						
						try {
							
							//Read the entire contents of the file and concatenate it into a string
							BufferedReader fileReader = new BufferedReader(new FileReader(realFile));
							
							while(currentLine != null) {
									currentLine = fileReader.readLine(); //read the file contents line by line
									fileData = fileData + currentLine + separator; //concatenate the separator in order to improve formatting
							}
							fileReader.close();
						} catch (FileNotFoundException e) { //error related to the file we are trying to read not being found
							e.printStackTrace();
						} catch (IOException e) { //error when actually trying to read a found file
							e.printStackTrace();
						}
						
						//sending and logging the appropriate HTTP File response
						System.out.println();
						System.out.println("Sending HTTP File to Server");
						System.out.println(HTTP_OK);
						System.out.println(SERV);
						System.out.println(CL + Long.toString(realFile.length()));
						System.out.println(CT + content);
						System.out.println();						
						
						out.print(HTTP_OK + separator);
						out.print(SERV + separator);
						out.print(CL + Long.toString(realFile.length()) + separator);
						out.print(CT + content + separator);
						out.print(separator);
						out.print(fileData);
						out.flush();
						
						
					}else {
						//Send File Not Found Error
						SendHTTPError(ERROR_404, out);
					}	
				}else{
					//If the request is for a directory
					
					File[] fileArray = realFile.listFiles();
					String treeString = "<html><pre>"; //the variable where the entire directory structure will be stored
					
					//tying to parse the name of the root directory where we are located
					String tempPath = realFile.getAbsolutePath();
					String[] tempPathArr = tempPath.split("\\\\");
					String firstDirectory = "";
					String secondDirectory = "";
					
					if(tempPathArr.length > 1) { //if I was able to parse it successfully
						firstDirectory = tempPathArr[tempPathArr.length -2];
						secondDirectory = tempPathArr[tempPathArr.length -1];
						
						treeString = treeString + "<h1>Index of "+ firstDirectory + "/"+ secondDirectory + "</h1><br>";
					}else { //otherwise send boring path
						treeString = treeString + "<h1>Index of "+ realFile.getPath() + "</h1><br>";
					}
					
					//adding a button to facilitate navigation back to the root directory
					if(realFile.getParent() == null) {
						treeString = treeString + "<a href=\"./\">Root Directory</a><br>\n";
					}else {
						treeString = treeString + "<a href=\"../\">Root Directory</a><br>\n";
					}
					
					int files = 0; //counts the number of files
					
					//transverse the directory and create a link for every single file or subdirectory
					while (files < fileArray.length) {
						
						File currentFile = fileArray[files];
						String currentFileName = currentFile.getName();
						
						if (currentFile.isDirectory() == true) {
							treeString = treeString + "<a href=\" " + currentFileName + "/\">\\ " + currentFileName + "</a><br>\n";
						}
						if (currentFile.isFile() == true) { 
							treeString = treeString + "<a href=\" " + currentFileName + "\" > " + currentFileName + " </a><br>\n";
						}
						
						files++;
					}
					
					treeString = treeString + "</pre></html>";
					
					//Both send and log the appropriate HTTP Directory message
					System.out.println();
					System.out.println("Sending HTTP Directory to Server");
					System.out.println(HTTP_OK);
					System.out.println(SERV);
					System.out.println(CL +  treeString.getBytes().length);
					System.out.println(CT + "text/html");
					System.out.println();
					System.out.println(treeString);
					System.out.println();
					
					out.print(HTTP_OK + separator);
					out.print(SERV + separator);
					out.print(CL + treeString.getBytes().length + separator);
					out.print(CT + "text/html" + separator);
					out.print(separator);
					out.print(treeString);
					out.flush();
					
				}
			}
		}
	}
}


