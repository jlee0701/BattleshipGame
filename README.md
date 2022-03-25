#### Description:
Demonstrate simple Client and Server communication using protobuf as protocol.

The code implements the design for a simple game, where users can play a "version" of
Battleship(similar to minesweeper, where you guess the position of the "battleships(landmines)", but instead you win when all battleships are found) together. The server is the one that sets the ships and knows the board, the clients are the one trying to find all the ships together.
The server also keeps track of a leaderboard and a log file.

### The procotol
You will see a response.proto and a request.proto file. You should implement these in your program. 
Some constraints on them:
Request:
- NAME: a name is sent to the server
	- name
	Response: GREETING
			- message
- LEADER: client wants to get leader board
	- no further data
	Response: LEADER
			- leader
- NEW: client wants to enter a game
	- no further data
	Response: TASK
			- image
			- task
- ANSWER: client sent an answer to a server task
	- answer
	Response: TASK
			- image
			- task
			- eval
	OR
	Response: WON
			- image
- QUIT: clients wants to quit connection
	- no further data
	Response: BYE
		- message

Response ERROR: anytime there is an error you should send the ERROR response and give an appropriate message. Client should act appropriately
	- message

### How to run
The proto file can be compiled using

``gradle generateProto``

This will also be done when building the project. 

You should see the compiled proto file in Java under build/generated/source/proto/main/java/buffers

Now you can run the client and server 

#### Default 
Server is Java
Per default on 9099
runServer

You have one example client in Java using the Protobuf protocol

Clients runs per default on 
host localhost, port 9099
Run Java:
	runClient


#### With parameters:
Java
gradle runClient -Pport=9099 -Phost='localhost'
gradle runServer -Pport=9099