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

import java.rmi.*;
import java.net.*;
import java.util.*;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.nio.file.*;


public class Client
{
    DFS dfs;
    public Client(int p) throws Exception {
        dfs = new DFS(p);
//        dfs.touch("test");
//        System.out.println();
       // dfs.delete("File2");
            // User interface:
            // join, ls, touch, delete, read, tail, head, append, move
        menu();
    }
    
    public static void printMenu(){
		System.out.println("Enter a Command");
		System.out.println("1. Join");
		System.out.println("2. LS");
		System.out.println("3. Touch");
		System.out.println("4. Delete");
		System.out.println("5. Read");
		System.out.println("6. Tail");
		System.out.println("7. Head");
		System.out.println("8. Append");
		System.out.println("9. Move");
		System.out.println("0. Quit\n");
    }
    
    public static void menu() throws Exception{
    	Scanner in = new Scanner(System.in);
    	
    	boolean running = true;
    	while(running){
    		printMenu();
    		String input = in.nextLine();    		
    		try{
    			switch(input){
    			case "1":
    				//join
    				break;
    			case "2":
    				//LS
    				break;
    			case "3":
    				//touch
    				break;
    			case "4":
    				//delete
    				break;
    			case "5":
    				//read
    				break;
    			case "6":
    				//tail
    				break;
    			case "7":
    				//head
    				break;
    			case "8":
    				//append
    				break;
    			case "9":
    				//move
    				break;
    			case "0":
    				//quit
    				System.exit(0);
    				break;
    			}
    		}catch(Exception e){
    			e.printStackTrace();
    			System.out.println("Invalid Input");
    		}
    		
    	}
    }
    
    static public void main(String args[]) throws Exception
    {
        
       /* if (args.length < 1 ) {
            throw new IllegalArgumentException("Parameter: <port>");
        }
        
        Client client=new Client( Integer.parseInt(args[0]));
        */
        
        Client client = new Client(23245);
     } 
}
