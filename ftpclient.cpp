/**

	Creators: Bryson Sherman
		  Hung Mach
		  Jimmy Lanh
		  
	Due Date: 2/6/2018
	
    C++ client example using sockets.
    This programs can be compiled in linux and with minor modification in 
	   mac (mainly on the name of the headers)
    Windows requires extra lines of code and different headers
#define WIN32_LEAN_AND_MEAN

#include <windows.h>
#include <winsock2.h>
#include <ws2tcpip.h>

// Need to link with Ws2_32.lib, Mswsock.lib, and Advapi32.lib
#pragma comment(lib, "Ws2_32.lib")
...
WSADATA wsaData;
iResult = WSAStartup(MAKEWORD(2,2), &wsaData);
...
*/
#include <iostream>    		//cout
#include <string>
#include <stdio.h> 		//printf
#include <stdlib.h>
#include <string.h>    		//strlen
#include <sys/socket.h>    	//socket
#include <arpa/inet.h> 		//inet_addr
#include <netinet/in.h>
#include <sys/types.h>
#include <unistd.h>
#include <fstream>		//File I/O
#include <netdb.h>

#define BUFFER_LENGTH 2048
//WAITING_TIME is long to compensate for the slow connection at CSULB
#define WAITING_TIME 500000
//For debugging purposes, 1 will turn the debug on, 0 will turn the debug off
#define DEBUG 0

/*
* Method use to connect the user to a server, which was created by our instructor
* @Hostname: The name of the host
* @Port: the port that is needed to connect to the host
*/
int create_connection(std::string host, int port)
{
    int s;
    struct sockaddr_in socketAddress;
    
    memset(&socketAddress,0, sizeof(socketAddress));
    s = socket(AF_INET,SOCK_STREAM,0);
    socketAddress.sin_family=AF_INET;
    socketAddress.sin_port= htons(port);
    
    int a1,a2,a3,a4;
    if (sscanf(host.c_str(), "%d.%d.%d.%d", &a1, &a2, &a3, &a4 ) == 4)
    {
        //std::cout << "by ip";
        socketAddress.sin_addr.s_addr =  inet_addr(host.c_str());
    }
    else {
        //std::cout << "by name";
        hostent *record = gethostbyname(host.c_str());
        in_addr *addressptr = (in_addr *)record->h_addr;
        socketAddress.sin_addr = *addressptr;
    }
    if(connect(s,(struct sockaddr *)&socketAddress,sizeof(struct sockaddr))==-1)
    {
        perror("connection fail");
        exit(1);
        return -1;
    }
    return s;
}

/**
* send a message to the host and return a int from the FTP reply
* @socket: the an endpoint of a two way communication
* @message: the message to send to the host, normally followed by /r/n
**/
int request(int sock, std::string message)
{
    return send(sock, message.c_str(), message.size(), 0);
}

/*
* Retrieve a reply from the host returning the message retrieved from the server.
* @s: ????
*/
std::string reply(int s)
{
    std::string strReply;
    int count;
    char buffer[BUFFER_LENGTH+1];
    
    usleep(WAITING_TIME);
    do {
        count = recv(s, buffer, BUFFER_LENGTH, 0);
        buffer[count] = '\0';
        strReply += buffer;
    }while (count ==  BUFFER_LENGTH);
    return strReply;
}

/*
*Send a request to the host and recieve it's message, technically combining two method into one
* @socket: the socket that is being in focus
* @message: the message that is needed to be sent
*/
std::string request_reply(int s, std::string message)
{
	if (request(s, message) > 0)
    {
    	return reply(s);
	}
	return "";
}

/*
 *Enter the passive mode and return the socket retrieved.
 *@sockpiGet: the socket to act upon on.
 */
int passiveMode(int sockpiGet){
	
    int sockpi;
    std::string strReply;
	
	//Pass in PASV and retrieve the communication from the server and set it to our strReply
	strReply = request_reply(sockpiGet, "PASV\r\n");
	
	if (DEBUG == 1) std::cout << "strReply for passing PASV: " << strReply << std::endl;
	
	//initialize our variable to grab the octets
	int A,B,C,D,port1,port2;
	
	//Finding the index at where the parenthesis lies so we can isolate the numbers given from the PASV
	int openPar = strReply.find("(");
	int closePar = strReply.find(")");
	std::string numbers = strReply.substr(openPar+1, closePar-openPar-1);
	
	if (DEBUG == 1) {
		std::cout << "{}> Index of the open parenthesis: " << openPar << endl;
		std::cout << "{}> Index of the closed parenthesis: " << closePar << endl;
		std::cout << "{}> numbers isolated: " << numbers << endl;
	}
	
	//Instead of being able to String.split() just like java
	//sscanf takes information from the string, in this case numbers, and place them into variables.
	int lengthCheck = sscanf(numbers.c_str(), "%d, %d, %d, %d, %d, %d", &A, &B, &C, &D, &port1, &port2);

	if (DEBUG == 1) {
		std::cout << "{}> IP: " << A << "." << B << "." << C << "." << D << endl;
		std::cout << "{}> Port 1 (" << port1 << ") Port 2 (" << port2 << ")" << std::endl;
	}
	
	//Grab the port, if it's either port1 left shift logical by 8 or the just pick the second port
	int portGet = ((port1 << 8)|port2);
	
	if (DEBUG == 1) std::cout << "{}> Port: " << portGet << std::endl;

	//Create the new connection with the port
    	std::string compileIP = std::to_string(A) + "." + std::to_string(B) + "." + std::to_string(C) + "." + std::to_string(D);
    	sockpi = create_connection(compileIP, portGet);
	
	return sockpi;
}
/*
* Allow the user to communicate between the client and server
*@sockpiGet: The socket which we're acting upon on
*@commandGet: the command that is sent to the server acting upon the socket
*/
void issueCmd(int sockpiGet, std::string commandGet){

	int sockpi;
    
	std::string strReply;
	
	//Enter passive mode and get the new sockpi
	sockpi = passiveMode(sockpiGet);

	if (DEBUG == 1) std::cout << "{}> Command Sending: " << strReply.substr(4, strReply.length()) << std::endl;
	
	//Issue the command and set strReply to whatever the server replies with
	strReply = request_reply(sockpiGet, commandGet + "\r\n");

	if (DEBUG == 1) std::cout << "{}> Code recieved from the server : " << strReply.substr(0, 3) << std::endl;
	
	//Strips the code so that way we only get the returned code
	if (std::stoi(strReply.substr(0, 3)) == 150){ //If the code was 150 then it's LIST or retrieve
		//Request a reply without needing to send any sort of command
		strReply = reply(sockpi);
		
		//Getting the 150 code does not determine if LIST of RETR was passed, so here we are determing which one it is.
		if(commandGet.substr(0, 4).compare("LIST") == 0){
			//Print out the list of files
			std::cout << strReply << std::endl;
		}
		if (commandGet.substr(0, 4).compare("RETR") == 0){
			//initialize the output stream
		        std::ofstream file;
			//Grab the file name and then open it.
			file.open(commandGet.substr(5, commandGet.length()));
			//Write everything that was retrieved from the server into the file 
			file << strReply;
			//We always close our stream about we finish using it.
			file.close();
			std::cout<< commandGet.substr(5, commandGet.length()) << " retrieved" << std::endl;
		}
		//Close socket since we are done for this turn
		close(sockpi);
        	strReply = reply(sockpiGet);
	
	if (DEBUG == 1) std::cout << "{}> strReply after file finish transfering: " << strReply << std::endl;		
	
	} else if (std::stoi(strReply.substr(0, 3)) == 221){ //IF Quit was sent to the server
		std::cout << "Goodbye" << std::endl;
	} else if (std::stoi(strReply.substr(0, 3)) == 250){ //If CWD was passed and we were able to get into the directory
		issueCmd(sockpiGet, "PWD");
	}else if (std::stoi(strReply.substr(0, 3)) == 550){ // IF CWD was passed and we were not able to get into the directory
		std::cout << "Directory is not found." << std::endl;
		issueCmd(sockpiGet, "PWD");
	}
}
int main(int argc , char *argv[])
{
    int sockpi;
    std::string strReply;
    std::string inputGet = "";
    bool flag = true;
	
    //TODO  arg[1] can be a dns or an IP address.
    if (argc > 2)
        sockpi = create_connection(argv[1], atoi(argv[2]));
    if (argc == 2)
        sockpi = create_connection(argv[1], 21);
    else
        sockpi = create_connection("130.179.16.134", 21);
    strReply = reply(sockpi);
        
    if (DEBUG == 1) std::cout << "{}> Connect status: " << strReply << std::endl;
	
    //We don't have to worry about this since our professor mention that we dont' have to worry about it.
    strReply = request_reply(sockpi, "USER anonymous\r\n");        
	
    if (DEBUG == 1) std::cout << "{}> Username status: " << strReply << std::endl;
    
    strReply = request_reply(sockpi, "PASS asa@asas.com\r\n");
        
    if (DEBUG == 1) std::cout << "{}> Password status: " << strReply << std::endl;

    //This will allow the user be in the menu command forever until they quit
    while(flag){
    fail:	//Default or invalid input starts from here
	std::cout << "Please enter ls, get ___, cd ___, or quit" << std::endl;
	getline(std::cin, inputGet);
        //data validfication
        bool dataVerify = true;
	//Make sure all character input follows an invalid input
        for(int i = 0; i < inputGet.length() && dataVerify; i++){
            if(isalpha(inputGet[i])){
                continue;
            }if(inputGet[i] == ' '){
                continue;
            }if(inputGet[i] == '.'){
                continue;
            }if(inputGet[i] == '/'){
                continue;
            }if(inputGet[i] == '-'){
                continue;
            }if(inputGet[i] == '_'){
                continue;
            }
            dataVerify = false;
        }
        
        if (dataVerify == false){ 	//If the user select a invalid number
            std::cout << "Please enter a valid input value." << std::endl;
            goto fail;			//Jump back to the beginning of loop
        } else if (inputGet == "ls"){ 	//If the user input ls, which will suppose to display the entire list of files
            issueCmd(sockpi, "LIST");
        } else if (inputGet.substr(0,3) == "get"){ //If the user input is get, it will prompt the user for the file to get
	    //The first index right after the command
	    int fileNamePos = inputGet.find(" ");
	    if (DEBUG == 1){
	            std::cout << "{}> Index of the first space: " << fileNamePos << std::endl;
        	    std::cout << "{}> Filename: " << inputGet.substr(fileNamePos+1, 50) << std::endl;
	    }
	    //Send RETR and the file name to the server
	    issueCmd(sockpi, "RETR " + inputGet.substr(fileNamePos+1, 50));
        }else if (inputGet.substr(0,2) == "cd"){ //If the user wishes to change directory
	    //The first inde right after the command
	    int fileNamePos = inputGet.find(" "); 
	    //Send CWD and the directory to the server
	    issueCmd(sockpi, "CWD " + inputGet.substr(fileNamePos+1, 50));
	}else if (inputGet == "quit"){ //If the user wants to quit
            flag = false;
	    issueCmd(sockpi, "QUIT");
	    return 0;
        }
    }
}
