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
#include <netdb.h>

#define BUFFER_LENGTH 2048
//WAITING_TIME is long to compensate for the slow connection at CSULB
#define WAITING_TIME 200000

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
        std::cout << "by ip";
        socketAddress.sin_addr.s_addr =  inet_addr(host.c_str());
    }
    else {
        std::cout << "by name";
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

int request(int sock, std::string message)
{
    return send(sock, message.c_str(), message.size(), 0);
}

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

std::string request_reply(int s, std::string message)
{
	if (request(s, message) > 0)
    {
    	return reply(s);
	}
	return "";
}

//Strip the reply from the server and return only the code.
int returnCode(std::string stringGet){
	
	std::string codeStrip = stringGet.substr(0, 3);
	int code = std::stoi(codeStrip);
	
	return code;
}
// Enter the passive mode
int passiveMode(int sockpiGet){
	
	int sockpi
	std::string strReply
	
	//Pass in PASV and retrieve the communication from the server and set it to our strReply
	strReply = request_reply(sockpi, "PASV\r\n");
	
	//This is just for debugging purposes, just checking to make sure we got the right result, remove this once we are done
	std::cout << strReply << std::endl;
	
	//initialize our variable to grab the octets
	int num1,num2,num3,num4,num5,num6;
	
	//Finding the index at where the parenthesis lies so we can isolate the octect given from the PASV
	int openPar = strReply.find("(");
	int closePar = strReply.find(")");
	std::string numbers = strReply.substr(openPar+1, closePar-openPar-1);
	if (sscanf(numbers.c_str(), "%d, %d, %d, %d, %d", &num1, &num2, &num3, &num4, &num5, &num6) == 6){
		std::cout << "first %: " << b5 << ", second %: " << b6 << std::endl;
	}else {
		std::cout <<"by name";
	}
	
	//Grab the port, if it's either b5 left shift logical by 8 or the b6
	int portGet = ((b5 << 8)|b6);
	
	std::cout << "Port: " << portGet << std::endl;

	//Create the new connection with the port
    	sockpi = create_connection("130.179.16.134", portGet);
	
	std::cout << " connection established." << sockpi << std::endl;
	
	return sockpi;
}
void issueCmd(int sockpiGet, std::string commandGet){

	int sockpi;
	std::string strReply;
	
	//Enter passive mode and get the new sockpi
	sockpi = passiveMode(sockpiGet);
	//Issue the command and set strReply to whatever the server replies with
	strReply = request_reply(sockpi, commandGet + "\r\n");
	
	//Debugging purposes, delete this when we are done
	std::cout << strReply << std::endl;
	
	//returnCode function will strip the code from the reply from the server.
	if (returnCode(strReply) == 150){ //If the code was 150 then it's LIST or retrieve
		//Request a reply without needing to send any sort of command
		strReply = reply(sockpi);
		
		//For debugging purposes, we might need to delete this when we're done
		std::cout << strReply << std::endl;
		//If the command was RETR we need to do more stuff, if it's list we just list it out
		if (commandGet.substr(0, 4).compare("RETR") == 0){
			//initialize the output stream
			std::ofstream file;
			//Grab the file name
			file.open(commandGet.substr(5, commandGet.length()));
			file << strReply;
			file.close();
		}
		//Close socket since we are done for this turn
		close(sockpi);
		strReply = reply(sockpi);
		
		//Debugging purposes, just to see
		std::cout << strReply  << std::endl;		
	}
}
int main(int argc , char *argv[])
{
    int sockpi;
    std::string strReply;
    int inputGet = 0;
    bool flag = true;
	
    //TODO  arg[1] can be a dns or an IP address.
    if (argc > 2)
        sockpi = create_connection(argv[1], atoi(argv[2]));
    if (argc == 2)
        sockpi = create_connection(argv[1], 21);
    else
        sockpi = create_connection("130.179.16.134", 21);
    strReply = reply(sockpi);
    std::cout << strReply  << std::endl;
    
    
    strReply = request_reply(sockpi, "USER anonymous\r\n");
    //TODO parse srtReply to obtain the status. 
	// Let the system act according to the status and display
    // friendly message to the user 
	// You can see the ouput using std::cout << strReply  << std::endl;
    
    
    strReply = request_reply(sockpi, "PASS asa@asas.com\r\n");
        
    //TODO implement PASV, LIST, RETR. 
    // Hint: implement a function that set the SP in passive mode and accept commands.	
	
	while(flag){
		std::cin >> inputGet;
		//data validfication
		if (std::cin.fail()){
			std::cin.clear();
			std::cin.ignore();
			inputGet = -1;
		}
		
		if (inputGet == -1){ //If the user select a invalid number
			std::cout << "Please enter a valid input value.\" << std::endl;
		} else if (inputGet == 1){ //If the user select 1 which is LIST
			issueCmd(sockpi, "LIST");	
		} else if (inputGet == 2){ //If the user select 2 which is RETR
			issueCmd(sockpi, "RETR " + fileGet);	
		} else if (inputGet == 3){ //If the user select 4 which is quit
			flag = false;
		}
	}
		
	strReply = request_reply(sockpi, "QUIT");
    	std::cout << strReply  << std::endl;	
    
	return 0;
}
