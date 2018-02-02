/**
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
#include <iostream>    //cout
#include <string>
#include <stdio.h> //printf
#include <stdlib.h>
#include <string.h>    //strlen
#include <sys/socket.h>    //socket
#include <arpa/inet.h> //inet_addr
#include <netinet/in.h>
#include <sys/types.h>
#include <unistd.h>
#include <fstream>
#include <netdb.h>

#define BUFFER_LENGTH 2048
//WAITING_TIME is long to compensate for the slow connection at CSULB
#define WAITING_TIME 500000

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

/*
* send a message to the host
* @socket: the an endpoint of a two way communication
* @message: the message to send to the host, normally followed by /r/n
*/
int request(int sock, std::string message)
{
    return send(sock, message.c_str(), message.size(), 0);
}

/*
* Retrieve a reply from the host
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
 *Enter the passive mode
 *@sockpiGet: the socket to act upon on.
 */
int passiveMode(int sockpiGet){
	
    int sockpi;
    std::string strReply;
	
	//Pass in PASV and retrieve the communication from the server and set it to our strReply
	strReply = request_reply(sockpiGet, "PASV\r\n");
	
	//This is just for debugging purposes, just checking to make sure we got the right result, remove this once we are done
	//std::cout << strReply << std::endl;
	
	//initialize our variable to grab the octets
	int A,B,C,D,port1,port2;
	
	//Finding the index at where the parenthesis lies so we can isolate the octect given from the PASV
	int openPar = strReply.find("(");
	int closePar = strReply.find(")");
	std::string numbers = strReply.substr(openPar+1, closePar-openPar-1);
	//sscanf takes information from the string, in this case numbers, and place them into variables.
	//Instead of being able to String.split() just like java
	int lengthCheck = sscanf(numbers.c_str(), "%d, %d, %d, %d, %d, %d", &A, &B, &C, &D, &port1, &port2);
	//Data validity
	//if (lengthCheck == 6){
		//std::cout << "Port 1 (" << port1 << ") Port 2 (" << port2 << ")" << std::endl;
	//}
	
	//Grab the port, if it's either port1 left shift logical by 8 or the just pick the second port
	int portGet = ((port1 << 8)|port2);
	
	//std::cout << "Port: " << portGet << std::endl;

	//Create the new connection with the port
    	std::string compileIP = std::to_string(A) + "." + std::to_string(B) + "." + std::to_string(C) + "." + std::to_string(D);
    //std::cout << compileIP << std::endl;
    	sockpi = create_connection(compileIP, portGet);
	
	//std::cout << " connection established." << sockpi << std::endl;
	
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
    	
	//Issue the command and set strReply to whatever the server replies with
	strReply = request_reply(sockpiGet, commandGet + "\r\n");
	std::cout << strReply.substr(4, strReply.length()) << std::endl;
	//Debugging purposes, delete this when we are done
	
	
	//returnCode function will strip the code from the reply from the server.
	if (std::stoi(strReply.substr(0, 3)) == 150){ //If the code was 150 then it's LIST or retrieve
		//Request a reply without needing to send any sort of command
		strReply = reply(sockpi);
		
		if(commandGet.substr(0, 4).compare("LIST") == 0){
		//For debugging purposes, we might need to delete this when we're done
			std::cout << strReply << std::endl;
		}
		//If the command was RETR we need to do more stuff, if it's list we just list it out
		if (commandGet.substr(0, 4).compare("RETR") == 0){
			//initialize the output stream
            std::ofstream file;
			//Grab the file name
			file.open(commandGet.substr(5, commandGet.length()));
			file << strReply;
			file.close();
			std::cout<< commandGet.substr(5, commandGet.length()) << " retrieved" << std::endl;
		}
		//Close socket since we are done for this turn
		close(sockpi);
        strReply = reply(sockpiGet);
		
        
		//Debugging purposes, just to see
		//std::cout << strReply  << std::endl;		
	} else if (std::stoi(strReply.substr(0, 3)) == 221){
		std::cout << "Goodbye" << std::endl;
	} else if (std::stoi(strReply.substr(0, 3)) == 250){
		issueCmd(sockpiGet, "PWD");
	}else if (std::stoi(strReply.substr(0, 3)) == 550){
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
        
    strReply = request_reply(sockpi, "USER anonymous\r\n");
    //TODO parse srtReply to obtain the status. 
	// Let the system act according to the status and display
    // friendly message to the user 
	// You can see the ouput using std::cout << strReply  << std::endl;
        
    strReply = request_reply(sockpi, "PASS asa@asas.com\r\n");
        
    //TODO implement PASV, LIST, RETR. 
    // Hint: implement a function that set the SP in passive mode and accept commands.	
    
    
    while(flag){
    fail:
	std::cout << "Please enter ls, get ___, cd ___, or quit" << std::endl;
	getline(std::cin, inputGet);
        //data validfication
        bool dataVerify = true;
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
        
        if (dataVerify == false){ //If the user select a invalid number
            std::cout << "Please enter a valid input value." << std::endl;
            goto fail;
        } else if (inputGet == "ls"){ //If the user input ls, which will suppose to display the entire list of files
            issueCmd(sockpi, "LIST");
        } else if (inputGet.substr(0,3) == "get"){ //If the user input is get, it will prompt the user for the file to get
            int fileNamePos = inputGet.find(" ");
            //std::cout << "output: " << fileNamePos << std::endl;
            //std::cout << "output: " << inputGet.substr(fileNamePos+1, 50) << std::endl;
            issueCmd(sockpi, "RETR " + inputGet.substr(fileNamePos+1, 50));
        }else if (inputGet.substr(0,2) == "cd"){
	int fileNamePos = inputGet.find(" "); 
	issueCmd(sockpi, "CWD " + inputGet.substr(fileNamePos+1, 50));
	}else if (inputGet == "quit"){ //If the user select 4 which is quit
            flag = false;
		issueCmd(sockpi, "QUIT");
		return 0;
        }
    }
		
	
    	std::cout << strReply  << std::endl;	
    

}
