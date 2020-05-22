/*--------------------------------------------------------

1. Fernando Araujo 5/30/2019

2. java version: build 1.8.0_181-b13

3. Precise command-line compilation examples / instructions:
> javac *.java

4. Precise examples / instructions to run this program:

To run the server in shell windows:

> java Blockchain [process Number] 
 -Process number must be between 0 and 2 for now
 
 -The Blockchain will not start until process 2 has been started too. 
 -In order for the Blockchain to function properly please start process 2 after already starting process 0 and 1 or use the provided scripts
 
> java Blockchain 0
> java Blockchain 1
> java Blockchain 2


After running the BlockChain 2:

On each console, display the possible commands you provide, and enter an input loop to accept, e.g., the following commands:

C— Credit. Loop through the blockchain and keep a tally of which process has verified each block. Display the results on the console.
R [filename] — Read a file of records to create new data.
V [sumcommand] —Verify the entire blockchain and report errors if any. (Several forms such as: threshold, hash, signature)
L— On a single line each list block num, timestamp, name of patient, diagnosis, etc. for each record.

5. List of files needed for running the program.

 a. checklist-block.html
 b. Blockchain.java

5. Notes:

a. Successfully waits for process 2 to start the blockchain
b. Successfully creates BlockchainLedger.xml
c. Successfully multicasts and listens for unverified blocks, public keys and new blockchains
d. Successfully validates unvalidated blocks and adds them to the blockchain
e. Successfully signs and encodes all important information (Hash, BlockId, etc.)
f. Successfully runs all console commands (R, V, L, C) to read a new file, validate the chain, list the records and generate a credit report
----------------------------------------------------------*/

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;

public class Blockchain {
	
	//All the ports necessary for the multicasting
	public final static int PUBLIC_PORT = 4710;
	public final static int UNVERIFIED_PORT = 4820;
	public final static int UPDATED_PORT = 4930;
	public final static String SERVERNAME = "localhost";
	public final static int MAX_PROCESS = 3; //max number of processes, the code was left dynamic to allow for growth
	
	//group of idexes that point to where each specific data point is located in the XML
	public final static int IND_FNAME = 0;
	public final static int IND_LNAME = 1;
	public final static int IND_DOB = 2;
	public final static int IND_SSN = 3;
	public final static int IND_DG = 4;
	public final static int IND_TRT = 5;
	public final static int IND_RX = 6;
	
	public static int pId; //the local process id
	public static BlockingQueue<Block> queue; //the queue where all the unverified blocks reside
	public static LinkedList<Block> blockChain; //the final, verified blockchain
	public static boolean startFlag; //boolean flag that signals processes to start
	
	public static void main(String args[]) throws Exception{
		
		//basic initialization of local ports and the process id
		pId = 0;
		int publicKeyPort = PUBLIC_PORT;
		int unverifiedBlockPort = UNVERIFIED_PORT; 
		int updatedBlockChainPort = UPDATED_PORT;
		
		//obtain the process id from the user input
		if(args.length > 0) {
			pId = Integer.parseInt(args[0]);
		}
		
		//define the porst that we will use based on the base port constants and the pid
		publicKeyPort = 4710 + pId ;
		unverifiedBlockPort = 4820 + pId;
		updatedBlockChainPort = 4930 + pId;
		
		//dynamically populate the process data array that holds the keys for each process
		ProcessData[] pArr = new ProcessData[MAX_PROCESS];
		for(int i = 0; i < MAX_PROCESS; i++) {
			pArr[i] = new ProcessData();
		}
		
		// if the process is the last one (process 2 in this case) we will start immediately
		if(pId == (MAX_PROCESS -1)) {
			startFlag = true;
		}else {
			startFlag = false;
		}
		
		//creating the private/public key pair that we will use to sign our blocks
		KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
		SecureRandom rng = SecureRandom.getInstance("SHA1PRNG", "SUN");
		SecureRandom.getSeed(999 + pId);
		keygen.initialize(1024, rng);
		
		//the key pair is created, along with the queue and the blockChain
		KeyPair key = keygen.generateKeyPair();
		queue = new PriorityBlockingQueue<Block>(8 , new BlockComparator());
		blockChain = new LinkedList<Block>();
		
		new Thread(new KeyServer(publicKeyPort, pArr)).start(); //Thread in charge of listening to the multicasted Keys and storing them
		new Thread(new UnverifiedBlockServer(unverifiedBlockPort, pArr)).start(); //Thread in charge of listening for new unverified blocks and adding them to the queue
		new Thread(new BlockChainServer(updatedBlockChainPort, pArr)).start(); //Thread in charge of listening for new blockchains, in case a different process has beaten us to a block
		
		while(!startFlag){ 
			Thread.sleep(300);  //make other processes sleep until the last process is started (process 2 in this case)
		}
		
		try {
			Thread.sleep(1000);
			MultiCastKeys(pId, key); //we start by multicasting all the public keys along the blockchain
			Thread.sleep(1000);
			MultiCastBlocks(pId, key); //we follow to multicast all the unverified blocks that we have made by reading the input files
			Thread.sleep(1000);
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		if(pId == (MAX_PROCESS -1)) {
			Thread.sleep(3000);
		}
		
		new VerificationWorker(pArr, key).start(); //we start the verification worker that is in charge of verifying blocks in the queue and adding them to the final blockchain
		Thread.sleep(1000);
		
		Scanner scan = new Scanner(System.in);
		
		Thread.sleep(1000);
		while(true) { //we do a while loop that runs forever, listening for the user commands
			
			System.out.println();
			System.out.println("Please select from the following commands:");
			System.out.println("C - To show a report of the number of blocks verified by each process");
			System.out.println("R [filename] - To process new records from a file");
			System.out.println("V [subcommand]- To validate the entire blockchain. Add 'hash', 'signature', or 'threshold' as a subcommand to just target that test");
			System.out.println("L - To list each Block in the Blockchain");
			System.out.println();
			String input = scan.nextLine(); //we read the user command
			
			
			if(input != null && !input.isEmpty()) {
				
				if(input.substring(0,1).equalsIgnoreCase("R")) { //if the command is to read records from a file
					
					String file = null; 
					int counter = 0;
					
					// we make sure that we are given a filename as a second argument
					if(input.length() > 2) {
						file  = input.substring(2);
					}
					
					if(file != null) {
						//we read the file based on the filename
						File f =  new File(file);
						FileReader fr = new FileReader(f);
						BufferedReader buf = new BufferedReader(fr);
						String line = "";
						Socket sock;
						ObjectOutputStream toServer;
						
						//we loop reading a line at a time from the file and making new unverified blocks based on that data
						while (!((line = buf.readLine()) == null)) {
							Block tempBlock = new Block(pId, line, key); //here we are making a new block based on a line of data
							
							for(int i = 0; i < MAX_PROCESS; i++) { //once a new unverified block is made, we proceed to multicast it to the rest of the processes
								sock  = new Socket(SERVERNAME, UNVERIFIED_PORT + i);
								toServer = new ObjectOutputStream(sock.getOutputStream());
								String xmlBlock = printBlockXML(tempBlock, false); //we get the block as an XML string
								String encodedBlock = Base64.getEncoder().encodeToString(xmlBlock.getBytes()); //we encode the XML string to send it 
								toServer.writeObject(encodedBlock); //we send the object to the other processes who are listening for unverified blocks
								toServer.flush();
								sock.close();
							}
							
							counter++; //we keep a tally of the new blocks in order to inform the user of the total new records added
						}
					}
					
					System.out.println( counter + " records have been added to the unverified blocks");
					
				}else if(input.equalsIgnoreCase("C")) { //if we receive a credit report command
					
					generateCreditReport(); //we call this method to generate a report based on how many blocks have been verified by each process
					
				}else if(input.substring(0,1).equalsIgnoreCase("V")) { // if we receive the verify command
					
					String command = null;
					
					if(input.length() > 2) { //if there is a subcommand pass it along
						command  = input.substring(2);
					}
					verifyChain(pArr, command); //this method verifies the entire chain, but it can also target a specific test based on the subcommand passed
					
				}else if(input.equalsIgnoreCase("L")) { //if we receive the list blockchain command
					
					listBlocks(); //we call this method that outputs all blocks currently in the blockchain to the user
					
				}
			}
		}
		
	}
	
	public static void listBlocks() { 
		//method in charge of listing all the blocks and their data for the user
	
		for (int i = blockChain.size()-1 ; i > -1; i--) { //we iterate through the most recent blockchain, from latest to oldest
			Block temp = blockChain.get(i); //temporary variable holding the current block
			
			if(i >= 1) { //if this is not the first fake block show all the block data
			System.out.println(temp.blockNumber + ". " + temp.timeStamp + " "  + temp.firstName + " " + 
			temp.lastName + " " + temp.DOB + " " + temp.SSN + " " + temp.diagnostic + " " + temp.treatment + " " + temp.Rx);	
			} else { //if it is the fake block, just show the block number
				System.out.println(temp.blockNumber + ". ");
			}
		}
	}
	
	public static void generateCreditReport() {
		//generates a report that specifies how many blocks each process has validated
		//the method was programmed flexibly to allow for more processes in the future
		int[] totals = new int[MAX_PROCESS];
		
		//iterate through the blockchain
		for(Block b : blockChain) {
			
			if(!b.blockNumber.equals("1")) { //if it is the first block we ignore it
				
				int pId = Integer.parseInt(b.getVerificationProcessId()); //we get the verification process id for the block
				totals[pId] = totals[pId] + 1; //we add 1 block to the total of that process 
			}
		}
		
		//we generate the report for the user based on our totals
		System.out.println("Verification credit:");
		for(int i = 0; i < totals.length; i++) {
			System.out.println("Process " + i + " = " + totals[i]);
		}
		
	}
	
	public static void verifyChain(ProcessData[] pData, String command) {
		//this method contains all the logic related to verifying the validity of the entire blockchain
		try {
			
			for(int i = 1; i < Blockchain.blockChain.size(); i++) { //we will iterate through the entire blockchain
				
				boolean allMatches = true; //flag to keep track if the current block passed all verifications
				Block currentBlock = Blockchain.blockChain.get(i);//get the current block
				Block previousBlock = Blockchain.blockChain.get(i - 1); //get the previous block
				
				System.out.println("Validating Block #: " + currentBlock.getBlockNumber()); //announcing that we are validating a block
				
				//the context, marshaller, string writer, that we will use to format our block as XML
				JAXBContext context = JAXBContext.newInstance(Block.class);
				Marshaller marsh = context.createMarshaller();
				StringWriter sw = new StringWriter();		
				marsh.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
				
				//message digest for the Hash
				MessageDigest md = MessageDigest.getInstance("SHA-256");
				//decoding the current blocks SHA256 Hash
				byte[] blockHashBytes = Base64.getDecoder().decode(currentBlock.getSHA256());
				//getting an XML string only from the current block's data
				marsh.marshal(currentBlock.getCoreBlock(), sw);
				String blockXML = sw.toString();
				//System.out.println("Block # " + currentBlock.getBlockNumber() + " HASH" +  previousBlock.getSHA256() + " " + blockXML + " " + currentBlock.seed);
				
				//creating a new SHA based on the previous block's SHA, the current block's data and the seed
				md.update((previousBlock.getSHA256() + blockXML + currentBlock.getSeed()).getBytes()); 
				byte[] verifyingHashBytes = md.digest();
				String verifyingHash = Base64.getEncoder().encodeToString(verifyingHashBytes);
				String workString = DatatypeConverter.printHexBinary(verifyingHashBytes);
				int work = Integer.parseInt(workString.substring(0, 4),16); //getting a number based on the hash to generate "WORK"
				
				int currentProcessId = Integer.parseInt(currentBlock.getCreatingProcess()); //getting the creatorId
				int verificationProcessId = Integer.parseInt(currentBlock.getVerificationProcessId()); //getting the verifierId
				byte[] signatureBytes = Base64.getDecoder().decode(currentBlock.getSignedSHA256()); 
				
				//comparing the current block's SHA string to the signed SHA string, making sure that we can read it with the public key
				boolean signatureMatches = Block.verifySig(blockHashBytes, pData[verificationProcessId].getKey(), signatureBytes);
				
				String blockId = currentBlock.getBlockId();
				String sigBlockId = currentBlock.getSignedBlockId();
				byte[] signBlockIdBytes = Base64.getDecoder().decode(sigBlockId);
				//comparing the blockID with the signedBlockId, making sure that we can read them with the piblic key
				boolean idSignatureMatches = Block.verifySig(blockId.getBytes(), pData[currentProcessId].getKey(), signBlockIdBytes);
				
				//System.out.println("Block ID = " + blockId +  " " + pData[currentProcessId].getKey() + " " + sigBlockId );
				
				if(command == null || command.equalsIgnoreCase("threshold")) { //if we receive no command or if we specify for threshold
				
					System.out.println("Determining if the Calculated Hash meets work threshold");
					//System.out.println("WORK " + work);
					if(!(work < 15000)) { //our work threshold, if the work number we generated is under this number, the block is correct
						System.out.println("Calculated Hash does NOT meet work threshold!");
						allMatches = false;
					}
				}
				
				if(command == null || command.equalsIgnoreCase("hash")){ //if we get no commands or we specify for hash testing
				
					System.out.println("Comparing Block Hash with Calculated Hash");
					if(!currentBlock.getSHA256().equals(verifyingHash)) {//compares the SHA string with a new one generated on the fly
						System.out.println("Hashes do NOT match!");
						allMatches = false;
					}
				}
				
				if(command == null || command.equalsIgnoreCase("signature")){ //if we get no commands or the signature command
					System.out.println("Determining if the Block's SHA256 signature matches");
					if(!signatureMatches) { //making sure that the SHA256 signed string matches with the normal one
						System.out.println("Block's signature is incorrect!");
						allMatches = false;
					}
				}
				
				if(command == null || command.equalsIgnoreCase("signature")){//if we get no commands or the signature command
					System.out.println("Veryfing the BlockID signature");
					
					if(!idSignatureMatches) { //making sure that the signed BlockID matches the normal one
						System.out.println("The BlockID signature is incorrect!");
						allMatches = false;
					}
				}
				
				if(allMatches) { //if we pass either all the tests or the specified ones we announce success!
					System.out.println("Block # " + currentBlock.getBlockNumber() + " has been verified successfully!");
					if(Integer.parseInt(currentBlock.getBlockNumber()) == Blockchain.blockChain.size()) {
						System.out.println("All blocks have been verified successfully!");
					}
				}else {//if we get ANY block that is invalid
					int st = Integer.parseInt(currentBlock.getBlockNumber()) + 1;
					System.out.println("Block # " + currentBlock.getBlockNumber() + " is invalid!"); //we announce the invalid block
					System.out.println("Blocks " + (st) + " to " + Blockchain.blockChain.size() + " follow an invalid block"); //we announce which blocks depend on the invalid block
					break; //we stop verifying the rest of the chain
				}
				
			}
			
		}catch(Exception c) {
			c.printStackTrace();
		}
		
	}
	
	public static void MultiCastKeys(int pId, KeyPair key) {
		//Method used to multicas the public keys amongst all the running processes
		
		Socket sock;
		PrintStream toServer;
		
		try {
			//we get our public key and we encode it
			byte[] bytePubkey = key.getPublic().getEncoded();
			String stringKey = Base64.getEncoder().encodeToString(bytePubkey);
			
			//we iterate through the available processes and send the key to their public key port
			for(int i = 0;  i < MAX_PROCESS; i++) {
				sock =  new Socket(SERVERNAME, (PUBLIC_PORT + i));
				toServer = new PrintStream(sock.getOutputStream());
				toServer.println(pId);
				toServer.println(stringKey);
				toServer.flush();
				sock.close();
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void MultiCastBlocks(int pId, KeyPair key) {
		//Method that multicasts all the unvalidated blocks that we create based on the input files
		Socket sock;
		ObjectOutputStream toServer;
		
		System.out.println("Process number: " + pId + " Ports: " + (PUBLIC_PORT + pId) + " " + (UNVERIFIED_PORT)  +"\n");
		File f = new File("BlockInput" + pId + ".txt");
		try {
			
			if(pId == (Blockchain.MAX_PROCESS -1)) { //if we are the last process running, we create the first fake block
				
				JAXBContext context = JAXBContext.newInstance(Block.class);
				Marshaller marsh = context.createMarshaller();
				StringWriter sw = new StringWriter();		
				marsh.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
				MessageDigest md = MessageDigest.getInstance("SHA-256");
				
				//here we are creating the first block filled with fake data
				Block firstBlock = new Block();
				firstBlock.setBlockNumber("1");
				firstBlock.setBlockId(UUID.randomUUID().toString()); //we negerate a random UUID
				
				marsh.marshal(firstBlock, sw); //we get it in XML
				String firstXML = sw.toString();
				md.update(firstXML.getBytes());
				String shaString = Base64.getEncoder().encodeToString(md.digest()); //we get the hash and set the SHA256 STRING
				firstBlock.setSHA256(shaString); 
				
				Blockchain.blockChain.add(firstBlock);
				System.out.println("First Block has been added to the BlockChain by Process: " + pId);
			}
			
			//reading data from the process's respective input file
			FileReader fr = new FileReader(f);
			BufferedReader buf = new BufferedReader(fr);
			String line = "";
			
			//reading the file data one line at a time
			while (!((line = buf.readLine()) == null)) {
				//creating a new unvalidated block per line of data
				Block tempBlock = new Block(pId, line, key);
				String xmlBlock = printBlockXML(tempBlock, false); //getting the XML string representing the data block
				
				for(int i = 0; i < MAX_PROCESS; i++) { //iterate through the available processes
					sock  = new Socket(SERVERNAME, UNVERIFIED_PORT + i); 
					toServer = new ObjectOutputStream(sock.getOutputStream());
					String encondedBlock = Base64.getEncoder().encodeToString(xmlBlock.getBytes()); //encode the block XML
					toServer.writeObject(encondedBlock); //send the encoded unverified block to all other processes
					toServer.flush();
					sock.close();
				}
			}

			System.out.println("Process"+ pId +": The input file has been read and the new unvalidated blocks have been multicasted");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static String printBlockXML(Block block, boolean testingMode) {
		//Method in charge of returning a xml string based on the current block data
        StringWriter sw = new StringWriter();
        String marshalledBlock = "";
		 try {
		
			JAXBContext jContext = JAXBContext.newInstance(Block.class);
	        Marshaller marsh = jContext.createMarshaller();
			marsh.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marsh.marshal(block, sw);
			
			marshalledBlock = sw.toString();
			String xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
			String XMLBlock = xmlHeader + "\n<BlockLedger>" +  marshalledBlock.replace(xmlHeader, "") + "</BlockLedger>"; //we remove all headers and put it on top
	        if(testingMode) {
	        System.out.println(XMLBlock);
	        }
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		 
		 return marshalledBlock;
       
	}
	
}

@XmlRootElement
class Block{
	//class representing each of the Blocks of the Blockchain
	
	String blockNumber; //number of the block in the blockchain
	String timeStamp; //when it was created
	String SHA256; //the hash of its data
	String signedSHA256; //the signed hash
	String blockId; //the unique identifier for this block
	String signedBlockId; //signed blockId
	String verificationProcessId; //the id of the process that verified this block
	//block data based on the input files
	String firstName;
	String lastName;
	String diagnostic;
	String treatment;
	String Rx;
	String SSN;
	String DOB;
	String seed; //seed guess in order to fulfill work threshold
	String creatingProcess; //processId of the process that created this block
	
	Block(){	
	}
	
	Block(int pId, String fileData, KeyPair key){
		//block constructor, in charge of taking the file data and parsing it into this object
		try {
		String[] splitString = new String[10]; //string array that will hold the XML data values passed
		UUID id = UUID.randomUUID(); //unique identifier for the blockId
		String sid = id.toString();
		MessageDigest md = MessageDigest.getInstance("SHA-256");

		byte[] digitalSignatureUUID= signData(sid.getBytes(), key.getPrivate()); //signing the UniqueId with our private key so that other processes can read it with the shared public keys
		
		boolean verified = verifySig(sid.getBytes(), key.getPublic(), digitalSignatureUUID);
		
		String signedUUID = Base64.getEncoder().encodeToString(digitalSignatureUUID); //signed UniqueId 
		
		//System.out.println("Block ID = " + sid +  " " + key.getPublic() + " " + signedUUID );
		Date date = new Date();
		String T1 = String.format("%1$s %2$tF.%2$tT", "", date); //timestamp for the creation of the block
		
		//filling all the Block field with the available data
		splitString = fileData.split(" +");
		this.setSHA256("N/A");
		this.setSignedSHA256("N/A");
		this.setSignedBlockId(signedUUID);
		this.setBlockId(sid);
		this.setCreatingProcess(pId + "");
		this.setTimeStamp(T1);
		this.setFirstName(splitString[Blockchain.IND_FNAME]);
		this.setLastName(splitString[Blockchain.IND_LNAME]);
		this.setSSN(splitString[Blockchain.IND_SSN]);
		this.setDiagnostic(splitString[Blockchain.IND_DG]);
		this.setDOB(splitString[Blockchain.IND_DOB]);
		this.setTreatment(splitString[Blockchain.IND_TRT]);
		this.setRx(splitString[Blockchain.IND_RX]);
		this.setSeed("");
	
		JAXBContext jContext = JAXBContext.newInstance(Block.class);
	    Marshaller marsh = jContext.createMarshaller();
	    StringWriter sw = new StringWriter();
	    marsh.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
	    
	    Block coreBlock = this.getCoreBlock(); //get a core block with just the important data
	    sw = new StringWriter();
	    marsh.marshal(coreBlock, sw); //get the XML string based on the core block
	    String coreBlockString = sw.toString();
	    
	    MessageDigest mdBlock = MessageDigest.getInstance("SHA-256");
	    md.update (coreBlockString.getBytes());
	    byte blockData[] = mdBlock.digest();
		
	    String blockSHAString = Base64.getEncoder().encodeToString(blockData); //hash the block data
	    byte[] signatureBlockSHA = signData(blockSHAString.getBytes(), key.getPrivate()); //sign the block hash
	    boolean verifiedBlockSHA = verifySig(blockSHAString.getBytes(), key.getPublic(), signatureBlockSHA);
	    
	    //encode the hash and set it on the Block
	    String signedSHA256 = Base64.getEncoder().encodeToString(signatureBlockSHA);
	    this.setSHA256(blockSHAString);
	    this.setSignedSHA256(signedSHA256);
	    
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
   public static byte[] signData(byte[] data, PrivateKey key) throws Exception {
	   //provided method to sign the data with our private key
	   Signature signer = Signature.getInstance("SHA1withRSA");
	   signer.initSign(key);
	   signer.update(data);
	   return (signer.sign());
    }
   
    public static boolean verifySig(byte[] data, PublicKey key, byte[] sig) throws Exception {
    	//provided method to verify the data signature with our public key
	    Signature signer = Signature.getInstance("SHA1withRSA");
	    signer.initVerify(key);
	    signer.update(data);
	    return (signer.verify(sig));
	 }
	
    public Block getCoreBlock() {
    	//method that returns a new block made of just the core data that we will use for hashing
    	
    	Block coreBlock = new Block();
    	coreBlock.setSignedBlockId(this.signedBlockId);
    	coreBlock.setBlockId(this.blockId);
    	coreBlock.setCreatingProcess(this.creatingProcess);
    	coreBlock.setTimeStamp(this.timeStamp);
    	coreBlock.setFirstName(this.firstName);
    	coreBlock.setLastName(this.lastName);
    	coreBlock.setSSN(this.SSN);
    	coreBlock.setDiagnostic(this.diagnostic);
    	coreBlock.setDOB(this.DOB);
    	coreBlock.setTreatment(this.treatment);
    	coreBlock.setRx(this.Rx);
    	
    	return coreBlock;
    }
   
    //getters and setters
	public String getBlockNumber() {
		return blockNumber;
	}
	public void setBlockNumber(String blockNumber) {
		this.blockNumber = blockNumber;
	}
	public String getTimeStamp() {
		return timeStamp;
	}
	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}
	public String getSHA256() {
		return SHA256;
	}
	public void setSHA256(String sHA256) {
		SHA256 = sHA256;
	}
	public String getSignedSHA256() {
		return signedSHA256;
	}
	public void setSignedSHA256(String signedSHA256) {
		this.signedSHA256 = signedSHA256;
	}
	public String getBlockId() {
		return blockId;
	}
	public void setBlockId(String blockId) {
		this.blockId = blockId;
	}
	public String getSignedBlockId() {
		return signedBlockId;
	}
	public void setSignedBlockId(String signedBlockId) {
		this.signedBlockId = signedBlockId;
	}
	public String getVerificationProcessId() {
		return verificationProcessId;
	}
	public void setVerificationProcessId(String verificationProcessId) {
		this.verificationProcessId = verificationProcessId;
	}
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public String getDiagnostic() {
		return diagnostic;
	}
	public void setDiagnostic(String diagnostic) {
		this.diagnostic = diagnostic;
	}
	public String getTreatment() {
		return treatment;
	}
	public void setTreatment(String treatment) {
		this.treatment = treatment;
	}
	public String getRx() {
		return Rx;
	}
	public void setRx(String rx) {
		Rx = rx;
	}
	public String getCreatingProcess() {
		return creatingProcess;
	}
	public void setCreatingProcess(String creatingProcess) {
		this.creatingProcess = creatingProcess;
	}
	public String getSSN() {
		return SSN;
	}
	public void setSSN(String sSN) {
		SSN = sSN;
	}
	public String getDOB() {
		return DOB;
	}
	public void setDOB(String dOB) {
		DOB = dOB;
	}
	public String getSeed() {
		return seed;
	}
	public void setSeed(String sed) {
		seed = sed;
	}
}

@XmlRootElement
class BlockList{
	//object that represents the entirety of the blockchain, used to send it between processes
	LinkedList<Block> chain;
	
	BlockList(){
	}
	
	BlockList( LinkedList<Block> ch){
		chain = ch;
	}

	public LinkedList<Block> getChain() {
		return chain;
	}

	public void setChain(LinkedList<Block> chain) {
		this.chain = chain;
	}
	
}

class BlockComparator implements Comparator<Block>{
	//class that defines a comparator neede for our Block in order to be able to use PriorityQueues
	
	public int compare(Block x, Block y)
	{
		//defines how we will compare blocks and decide the block position
		if(x == null && y == null) {
			return 0;
		}else {
			if(y == null) {
				return -1;
			} 
			if(x == null) {
				return 1;
			}
			
			return x.getTimeStamp().compareTo(y.getTimeStamp()); //we decide the position based on the created timestamp
		}		
	}
	
}

class ProcessData {
	//this object holds the processId and the respective public key value for all the processes
	//it gets populated by the KeyWorker that listens for new public keys
	int processId;
	PublicKey key;
	
	public ProcessData(){
	}
	
	public int getProcessId() {
		return processId;
	}
	public void setProcessId(int processId) {
		this.processId = processId;
	}
	public PublicKey getKey() {
		return key;
	}
	public void setKey(PublicKey key) {
		this.key = key;
	}
}


class KeyServer implements Runnable{
	//server that listens for connections and generates new KeyWorker threads
	ProcessData[] pArr;
	int port;
	
	KeyServer(int port, ProcessData[] arr) {
		this.pArr = arr;
		this.port = port;
	}
	
	public void run() {
		int q_len = 6;
		Socket sock;
		
		try {
			System.out.println("Starting Key Server using port: " + port);
			
			ServerSocket ss = new ServerSocket(port, q_len);
			while(true) { //iterate forever and keep making new KeyWorkers to handle every connection
				sock = ss.accept();
				new KeyWorker(sock, pArr).start();
			}
			
		}catch (IOException io) {
			io.printStackTrace();
		}
		
	}
	
}

class KeyWorker extends Thread{
	//Class in charge of handling new public keys, it decodes them and stores them for later usage
	
	Socket sock;
	ProcessData[] pArr;
	
	KeyWorker(Socket s, ProcessData[] p){
		sock = s;
		pArr = p;
	}
	
	public void run() {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			
			int inputPId = Integer.parseInt(in.readLine()); //read the processId
			byte[] byteKey  = Base64.getDecoder().decode(in.readLine()); //read the keybytes
			
			KeyFactory keyF = KeyFactory.getInstance("RSA"); 
			PublicKey key = keyF.generatePublic(new X509EncodedKeySpec(byteKey)); //decode and cast the input into a PublicKey object
			
			//store the PublicKey object and processId in the ProcessData array
			pArr[inputPId].setProcessId(inputPId);
			pArr[inputPId].setKey(key);
			
			System.out.println("Received Public Key for Process: " + inputPId);
			
			sock.close();
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

class UnverifiedBlockServer implements Runnable{
	//server that listens for any connections and spins up new unverifiedBlockWorkers
	//together, they handle any new unverified Block that is multicasted
	
	ProcessData[] pArr;
	int port;
	
	UnverifiedBlockServer(int port, ProcessData[] pArr){
		this.port = port;
		this.pArr = pArr;
	}
	
	public void run() {
		int q_len = 6;
		Socket sock;
		
		try {
			System.out.println("Starting Unverified Block Server using port: " + port);
			
			ServerSocket ss = new ServerSocket(port, q_len);
			while(true) { //run infinitely and listen for any connections
				sock = ss.accept();
				new UnverifiedBlockWorker(sock, pArr).start(); //create a new UnverifiedBlockWorker to handle the connection
			}
			
		}catch (IOException io) {
			io.printStackTrace();
		}
		
	}
}

class UnverifiedBlockWorker extends Thread{
	//class in charge of handling new unverified block data being multicasted
	
	Socket sock;
	ProcessData[] pArr;
	
	UnverifiedBlockWorker(Socket s, ProcessData[] p){
		this.sock = s;
		this.pArr = p;
	}
	
	public void run() {
		try {
			
			ObjectInputStream os = new ObjectInputStream(sock.getInputStream());
			JAXBContext jContext = JAXBContext.newInstance(Block.class);
	        Unmarshaller unmarsh = jContext.createUnmarshaller();
	        
			String input = (String) os.readObject(); //read the object from the stream
			byte[] decodedBytes = Base64.getDecoder().decode(input.getBytes()); //decode the object string
			String decodedInput = new String(decodedBytes);
			StringReader r = new StringReader(decodedInput);
			
			Block temp = (Block) unmarsh.unmarshal(r); //unmarshal the object string back into a Block object
			Blockchain.queue.put(temp); //add the Block object into the queue based on the createstamp
			os.close();
			
			System.out.println("Successfully acquired and queued unverified block with blockId:" + temp.getBlockId());
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
}

class BlockChainServer implements Runnable{
	//Server in charge of listening for new multicasted blockchains
	
	ProcessData[] pArr;
	int port;
	
	BlockChainServer(int port, ProcessData[] pArr){
		this.port = port;
		this.pArr = pArr;
	}
	
	public void run() {
		int q_len = 6;
		Socket sock;
		
		try {
			System.out.println("Starting Block Chain Server using port: " + port);
			
			ServerSocket ss = new ServerSocket(port, q_len);
			while(true) { //runs indefinitely listening for connections
				
				sock = ss.accept();
				new BlockChainWorker(sock, pArr).start(); //spins a new BlockChainWorker in order to handle the Blockchain data
				Blockchain.startFlag = true;
			}
			
		}catch (IOException io) {
			io.printStackTrace();
		}
		
	}
}

class BlockChainWorker extends Thread{
	//class in charge of handling new versions of the blockchain being multicasted
	
	Socket sock;
	ProcessData[] pArr;
	
	BlockChainWorker(Socket s, ProcessData[] p){
		this.sock = s;
		this.pArr = p;
	}
	
	public void run() {
		try {
			ObjectInputStream os = new ObjectInputStream(sock.getInputStream());
			JAXBContext jContext = JAXBContext.newInstance(BlockList.class);
	        Unmarshaller unmarsh = jContext.createUnmarshaller();
	        LinkedList<Block> tempChain = new LinkedList<Block>();
	        
			String input = (String) os.readObject(); //read the BlockList object
			StringReader r = new StringReader(input);
			
			BlockList temp = (BlockList) unmarsh.unmarshal(r); //unmarshall the object back into a BlockList
			tempChain = new LinkedList<Block>(temp.getChain()); //get the final blockchain from within the BlockList
			Blockchain.blockChain = tempChain; //replace the local blockChain with the new one
			
			System.out.println("Successfully retrieved and updated the Blockchain");
			
			if(Blockchain.pId == (Blockchain.MAX_PROCESS -1)) {
				printBlockChain(temp);
			}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void printBlockChain(BlockList temp) {
		try {
			//method in charge of writing the up to date blockchain into the BlockchainLedger.xml file
			JAXBContext jContext = JAXBContext.newInstance(BlockList.class);
			BufferedWriter bw = new BufferedWriter(new FileWriter("BlockchainLedger.xml"));
	        Marshaller marsh = jContext.createMarshaller();
			marsh.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true); //Marshall the BlockList object into XML
			marsh.marshal(temp, bw); //write it into the BlockchainLedger.xml file
			bw.close();
			
			System.out.println("The BlockchainLedger.xml was succesfully updated");
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
}

class VerificationWorker extends Thread {
	//worker in charge of verifying all the unverified Blocks in the queue
	
	ProcessData[] pd;
	KeyPair key;
	LinkedList<Block> tempChain = new LinkedList<Block>();
	
	VerificationWorker(ProcessData[] p, KeyPair k){
		this.pd = p;
		this.key = k;
	}
	
	public void run() {
		
		boolean noErrors = true;
		
		while(noErrors) { //keep verifying the blocks as long as there are no errors
			try {
				Block temp = Blockchain.queue.take(); //take the first block in the queue
				
				if(temp !=  null) {
					tempChain.addAll(Blockchain.blockChain); //create a temporary copy of the blockchain
					
					if(!isBlockInChain(temp)) { //if the currently unverified block is not in the blockchain verify it
						
						System.out.println("Verifying a block");
						byte[]uuidSigned = Base64.getDecoder().decode(temp.getSignedBlockId());
						int cp = Integer.parseInt(temp.getCreatingProcess());
						PublicKey pub = pd[cp].key; 
						
						boolean idMatch = Block.verifySig(temp.getBlockId().getBytes(), pub, uuidSigned); //check if the blockId matches with the signedBlockId
						
						byte[]shaSigned =  Base64.getDecoder().decode(temp.getSignedSHA256()); 
						
						boolean shaMatch = Block.verifySig(temp.getSHA256().getBytes(), pub, shaSigned); //check if the SHA256 string matches with the signed version
						
						if(idMatch && shaMatch) { //if both checks are successful, proceed to do work. Otherwise, there is an error and the block was tampered with
							
							doWork(temp); //method to do work and to multicast new versions of the blockchain if necessary
								
						}else {
							noErrors = false;
							
							if(!idMatch) { //if there are errors, show a relevant message
								System.out.println("Error verifying a block, the uuid doesn't match");
							}else {
								System.out.println("Error verifying a block, the SHA256 doesn't match");
							}
						}
					}
					
				}
				
				
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public boolean isBlockInChain(Block b1) {
		//Method that checks if a given Block is already in the blockchain
		for(Block b2 : Blockchain.blockChain){
			if (b1.getBlockId().equals(b2.getBlockId())) //the check is based solely on the blockId since it is a UUID
				return true;
		}
		return false;
	}
	
	public void doWork(Block temp) {
		//method that does all the work for the verification of the block and multicasting of new blockchains
		
		try {
			boolean workIsOver = false;
		
			while(!workIsOver) { //look until we have finished the work and sent the new blockchain
				
				if(!isBlockInChain(temp)) { // if the current unverified block is not already in the blockchain
					
					int guessNumber = 0;
					JAXBContext context = JAXBContext.newInstance(Block.class);
					Marshaller marsh = context.createMarshaller();
					StringWriter sw = new StringWriter();		
					marsh.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
					MessageDigest md = MessageDigest.getInstance("SHA-256");
					
					String random = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
					String oldHash = tempChain.getLast().getSHA256();
					temp.setSeed(random); //generate a new seed for hashing
					Block coreBlock = temp.getCoreBlock(); //get the core block of just the core Block data
					marsh.marshal(coreBlock, sw); 					
					String xmlNoSeed = sw.toString(); //create xml string based on the core block
					
					md.update((oldHash + xmlNoSeed + random).getBytes()); //combine the old hash, the block and the seed
					byte[] hashBytes = md.digest();
					String hashResult = DatatypeConverter.printHexBinary(hashBytes);  //generate a new hash
					guessNumber = Integer.parseInt(hashResult.substring(0,4),16); //use the hash to guess a new number and try to beat the work threshold
					
					if(guessNumber < 15000) { //if the guessNumber is below the threshold
						
						String updatedSHA = Base64.getEncoder().encodeToString(hashBytes); 
						byte[] signedBytes = Block.signData(hashBytes, key.getPrivate());
						String signedString = Base64.getEncoder().encodeToString(signedBytes); //encode and sign the new SHA256 string
						
						temp.setSHA256(updatedSHA); //set the new SHAString into the block
						temp.setSignedSHA256(signedString); //set the new signed SHAString into the block
						
						workIsOver = true; // work should be over since we guessed correctly
						
						boolean differentChains = areChainsDifferent(); //check if the blockchain has not changed
						
						if(differentChains) { //if the chain has been modified
							if(!isBlockInChain(temp)) { //if it was modified but no one beat us to verify this block
								workIsOver = false; //start working again
								tempChain = new LinkedList<Block>();
								tempChain.addAll(Blockchain.blockChain); //create a new local copy of the blockchain
							}
						}else { //if the chain has NOT been modified we can verify and add the block to the blockchain!
							
							temp.setBlockNumber(Integer.toString((tempChain.size() + 1))); //set the new block number
							temp.setVerificationProcessId(Blockchain.pId + ""); //take credit for verifying this block
							Blockchain.blockChain.add(temp); //add the block to the blockchain
							
							//System.out.println("Block # " + temp.getBlockNumber() + " HASH" + oldHash + " " + xmlNoSeed + " " + random);
							System.out.println("Block " + temp.getBlockId() +" has been verified and added to the BlockChain");
							
							for(int i = 0; i < Blockchain.MAX_PROCESS; i++) { //iterate though the processes in order to multicast the new blockchain
								
								context = JAXBContext.newInstance(BlockList.class);
								marsh = context.createMarshaller();
								marsh.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
								sw = new StringWriter();
								Socket sock = new Socket(Blockchain.SERVERNAME, Blockchain.UPDATED_PORT + i);
								ObjectOutputStream os = new ObjectOutputStream(sock.getOutputStream());
								
								BlockList bl = new BlockList(Blockchain.blockChain); //store the blockchain in a BlockList object		
								marsh.marshal(bl, sw);	//marshal the BlockList into XML				
								os.writeObject(sw.toString()); //write the object and send it to all other processes
								
								os.flush();
								os.close();	
							}
							System.out.println("Multicasted the new BlockChain to all other processes");
						}
					}
					
				}else {
					workIsOver = true;
					System.out.println("Block is already in the BlockChain!");
				}
			}
			System.out.println("Work is finished!");
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean areChainsDifferent() {
		//Method that returns true if the blockChain has been replaced or modified while we were doing work 
		
		boolean chainsAreDifferent = false;
		
		if(tempChain.size() != Blockchain.blockChain.size()) { //if the blockchain size is different from our local copy
			chainsAreDifferent = true; //the blockchain has changed
		}else {
			for(int x = 0; x < tempChain.size(); x++ ) { //if the size is the same, still make sure that the blockIds on the entire chain are the same
				
				if(!tempChain.get(x).getBlockId().equals(Blockchain.blockChain.get(x).blockId)) {
					chainsAreDifferent = true;
				}
			}
		}
		return chainsAreDifferent;	
	}
}