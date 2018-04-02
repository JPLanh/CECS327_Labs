import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/*!
	@files	Client.java, DFS.Java
	@author	Bryson Sherman, Hung Mach, Jimmy Lanh
	@date	4/3/2018
	@version 1.0
	Creators: Bryson Sherman
			  Hung Mach
			  Jimmy Lanh

	Due Date: 4/3/2018

 */

public class Client
{
    DFS dfs;
    Scanner getInput = new Scanner(System.in);
    boolean running = true;

    public Client(int p) throws Exception {
        dfs = new DFS(p);
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
                    System.out.printf("%1$-10s %2$-25s %3$-30s\n\n\n", "Get", "{file name}", "Download the specified file from the DFS");
                } else if (input[0].toLowerCase().equals("ls")){
                    System.out.println(dfs.ls());
                } else if (input[0].toLowerCase().equals("exit")){
                    running = false;
                    System.exit(0);
                } else {
                    System.out.println("Invalid Command");
                }
            } else if (input.length == 2){
                if (input[0].toLowerCase().equals("del")){
                    dfs.delete(input[1]);
                    System.out.println("File has been deleted from the DFS");
                } else if (input[0].toLowerCase().equals("put")){
                    try {
                        Path path = Paths.get(input[1]);
                        byte[] data = Files.readAllBytes(path);
                        if (data.length == 0){
                            System.out.println("Such file does not exist");
                        } else {
                            dfs.append(input[1], data);
                            System.out.println("File has been uploaded to the DFS");
                        }
                    } catch (Exception e){
                        System.out.println("Such file does not exist");
                        e.printStackTrace();
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
                                e.printStackTrace();
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
        }
        // User interface:
        // tail, head
    }

    static public void main(String args[]) throws Exception
    {
        
        if (args.length < 1 ) {
            throw new IllegalArgumentException("Parameter: <port>");
        }

        Client client=new Client( Integer.parseInt(args[0]));
       
        //Client client = new Client(23245);
    } 
}
