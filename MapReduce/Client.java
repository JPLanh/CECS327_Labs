import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.Arrays;

/*!
	@files	Client.java, DFS.Java, Chord.java, ChordMessageInterface.java, FileStream.java, Mapper.java, MapReduceInterface.java
	@author	Bryson Sherman, Hung Mach, Jimmy Lanh
	@date	5/10/2018
	@version 1.0

	Due Date: 5/10/2018

 */

public class Client
{
    DFS dfs;
    Scanner getInput = new Scanner(System.in);
    boolean running = true;

	/**Client user interface with all the menu and parses string inputs
	* @param p 	the port number being passed in from console
	*/
    public Client(int p) throws Exception {
        dfs = new DFS(p);
        if (p != 2332) dfs.join("localhost", 2332);
        while(running){
            System.out.println("-=Welcome to the BHJ Distributed File System=-\n");
            System.out.print("(enter \"Help\" for assistance): ");
            String[] input = getInput.nextLine().split(" ");

            if(input.length == 0) {
                System.out.println("Please enter a command\n\n");
            } else if (input.length == 1){
                if (input[0].toLowerCase().equals("help")){
                    System.out.printf("%1$-10s %2$-20s %3$-30s\n", "Command", "Argument", "Description");
                    System.out.printf("%070d\n", 0);
                    System.out.printf("%1$-10s %2$-25s %3$-30s\n\n\n", "Join", "{IP} {Port}", "Join a group with the given port number");
                    System.out.printf("%1$-10s %2$-25s %3$-30s\n", "LS", "None", "List all files");
                    System.out.printf("%1$-10s %2$-25s %3$-30s\n", "MV", "{old name} {new name}", "Rename a file to a new name");
                    System.out.printf("%1$-10s %2$-25s %3$-30s\n", "Del", "{file name}", "Delete the specified file name");
                    System.out.printf("%1$-10s %2$-25s %3$-30s\n", "Put", "{file name}", "Upload the specified name into the DFS");
                    System.out.printf("%1$-10s %2$-25s %3$-30s\n", "Touch", "{file name}", "Create a new file in the metadata");
                    System.out.printf("%1$-10s %2$-25s %3$-30s\n", "Append", "{file name}{Byte (in array)}", "Add a new page after the last page");
                    System.out.printf("%1$-10s %2$-25s %3$-30s\n", "Head", "{file name}", "Prints out the head of the file");
					System.out.printf("%1$-10s %2$-25s %3$-30s\n", "Tail", "{file name}", "Prints out the tail of the file");
					System.out.printf("%1$-10s %2$-25s %3$-30s\n\n\n", "Get", "{file name}", "Download the specified file from the DFS");
					System.out.printf("%1$-10s %2$-25s %3$-30s\n\n\n", "MR", "{file name}", "Execute map reduction on a file");
					
                } else if (input[0].toLowerCase().equals("ls")){
                    System.out.println(dfs.ls());
                } else if (input[0].toLowerCase().equals("exit")){
                    running = false;
                    System.exit(0);
                } else if (input[0].toLowerCase().equals("sur")){
                    dfs.surround();
                } else {
                    System.out.println("Invalid Command");
                }
            } else if (input.length == 2){
                if (input[0].toLowerCase().equals("del")){
                    dfs.delete(input[1]);
                    System.out.println("File has been deleted from the DFS");
                } else if (input[0].toLowerCase().equals("head")){
					int size = dfs.getSize(input[1]);
					if(size == -1){
							System.out.println("File does not exist");
					}	else {
							System.out.println(Arrays.toString(dfs.head(input[1])));
					}
                } else if (input[0].toLowerCase().equals("mr")){
                    dfs.runMapReduce(input[1]);
                } else if (input[0].toLowerCase().equals("touch")){
                    try{
                        dfs.touch(input[1]);
                        System.out.println("Touch process completed");
                    } catch (Exception e){
                        e.printStackTrace();
                        System.out.println("Error trying to touch file: " + input[1]);                        
                    }
				} else if (input[0].toLowerCase().equals("tail")){
					int size = dfs.getSize(input[1]);
					if(size == -1){
							System.out.println("File does not exist");
					}	else {
							System.out.println(Arrays.toString(dfs.tail(input[1])));
					}
				} else if (input[0].toLowerCase().equals("put")){
                    try {
                        Path path = Paths.get(input[1]);
                        byte[] data = Files.readAllBytes(path);
                        if (data.length == 0){
                            System.out.println("Such file does not exist");
                        } else {
                            dfs.putFile(input[1], data);
                            System.out.println("File has been uploaded to the DFS");
                        }
                    } catch (Exception e){
                        System.out.println("Such file does not exist");
                    }
                } else if (input[0].toLowerCase().equals("get")){
                    int size = dfs.getSize(input[1]);
                    if (size == -1){
                        System.out.println("File does not exist");
                    } else {
                        FileOutputStream writer = new FileOutputStream(input[1]);
                        for (int i = 0 ; i < size; i++){
                            byte[] getByte = dfs.read(input[1], i);
                            try{
                                writer.write(getByte);
                            } catch (Exception e){
                                System.out.println("Unable to write file since it does not exist");
                            }                        
                        }
                        writer.close();
                        System.out.println("File has been downloaded from the DFS");
                    }
                }
            } else if (input.length == 3){
                if (input[0].toLowerCase().equals("mv")){
                    try {
                        dfs.mv(input[1], input[2]);
                        System.out.println(input[1] + " has been renamed to " + input[2]);
                    } catch (Exception e){
                        System.out.println("Unable to find " + input[1]);
                    }
                } else if (input[0].toLowerCase().equals("join")){
                    dfs.join(input[1], Integer.parseInt(input[2]));
                }
            }
            if (input.length > 2){
                if (input[0].toLowerCase().equals("append")){
                    String tempString = "";
                    for (int i = 2; i < input.length; i++){
                        tempString = tempString.concat(input[i] + " ");
                    }
                    byte[] data = tempString.getBytes();
                    try{
                        dfs.append(input[1], data);
                        System.out.println("String has been appended to the file " + input[1]);
                    } catch (Exception e){
                        System.out.println("Such file does not exist");
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    static public void main(String args[]) throws Exception
    {
        
        if (args.length < 1 ) {
            throw new IllegalArgumentException("Parameter: <port>");
        }

        Client client=new Client( Integer.parseInt(args[0]));
       
    } 
}
