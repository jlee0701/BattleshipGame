package server;

import java.net.*;
import java.io.*;
import java.util.*;
import org.json.*;
import java.lang.*;

import buffers.RequestProtos.Request;
import buffers.RequestProtos.Logs;
import buffers.RequestProtos.Message;
import buffers.ResponseProtos.Response;
import buffers.ResponseProtos.Entry;

class SockBaseServer implements Runnable {
    static String logFilename = "logs.txt";

    ServerSocket serv = null;
    int port = 9099; // default port
    Game game;
    Thread runningThread = null;
    boolean isStopped = false;

    public SockBaseServer(int sock, Game game){
        this.port = sock;
        this.game = game;
        /*
        try {
            in = clientSocket.getInputStream();
            out = clientSocket.getOutputStream();
        } catch (Exception e){
            System.out.println("Error in constructor: " + e);
        }

         */
    }

    // Handles the communication right now it just accepts one input and then is done you should make sure the server stays open
    // can handle multiple requests and does not crash when the server crashes
    // you can use this server as based or start a new one if you prefer.
    public void run(){
        synchronized(this){
            this.runningThread = Thread.currentThread();
        }
        openServerSocket();
        while(! isStopped()){
            Socket clientSocket = null;
            try {
                clientSocket = this.serv.accept();
                System.out.println("Connection received from " + clientSocket.getPort());
            } catch (IOException e) {
                if(isStopped()) {
                    //System.out.println("Server Stopped.") ;
                    return;
                }
                throw new RuntimeException(
                        "Error accepting client connection", e);
            }
            new Thread(new clientHandler(clientSocket, game)).start();
        }
        //System.out.println("Server Stopped.") ;
    }

    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop(){
        this.isStopped = true;
        try {
            this.serv.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }

    private void openServerSocket() {
        try {
            this.serv = new ServerSocket(this.port);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port", e);
        }
    }



    public static void main (String args[]) throws Exception {
        Game game = new Game();

        if (args.length != 2) {
            System.out.println("Expected arguments: <port(int)> <delay(int)>");
            System.exit(1);
        }
        int port = 9099; // default port
        int sleepDelay = 10000; // default delay
        Socket clientSocket = null;
        //ServerSocket serv = null;

        try {
            port = Integer.parseInt(args[0]);
            sleepDelay = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port|sleepDelay] must be an integer");
            System.exit(2);
        }
        /*
        try {
            serv = new ServerSocket(port);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }

         */
        while(true) {
            //clientSocket = serv.accept();
            SockBaseServer server = new SockBaseServer(port, game);
            new Thread(server).start();

            try {
                Thread.sleep(sleepDelay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            server.stop();
        }
    }
}

class clientHandler implements Runnable {
    static String logFilename = "logs.txt";
    Socket clientSocket = null;
    Game game;

    public clientHandler(Socket sock, Game game){
        this.clientSocket = sock;
        this.game = game;
    }

    // Handles the communication right now it just accepts one input and then is done you should make sure the server stays open
    // can handle multiple requests and does not crash when the server crashes
    // you can use this server as based or start a new one if you prefer.
    public void run() {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = clientSocket.getInputStream();
            out = clientSocket.getOutputStream();
        } catch (Exception e){
            System.out.println("Error in constructor: " + e);
        }

        String name = "";

        System.out.println("Ready...");
        try {
            // read the proto object and put into new objct

            boolean done = false;
            while (!done) {
                // read the proto object and put into new objct
                Request op = null;
                Request.OperationType type = null;
                String result = null;
                boolean requestReceived = false;
                while (!requestReceived || type == null) {
                    try {
                        op = Request.parseDelimitedFrom(in);
                        type = op.getOperationType();
                    } catch (NullPointerException e) {
                    }
                    requestReceived = true;
                }

                // if the operation is NAME (so the beginning then say there is a commention and greet the client)
                if (type == Request.OperationType.NAME) {
                    // get name from proto object
                    name = op.getName();

                    // writing a connect message to the log with name and CONNENCT
                    writeToLog(name, Message.CONNECT);
                    System.out.println("Got a connection and a name: " + name);
                    Response response = Response.newBuilder()
                            .setResponseType(Response.ResponseType.GREETING)
                            .setMessage("Hello " + name + " and welcome. Welcome to a simple game of battleship. ")
                            .build();
                    response.writeDelimitedTo(out);
                } else if (type == Request.OperationType.NEW) {
                    writeToLog(name, Message.START);
                    game.newGame(); // starting a new game
                    System.out.println("Starting a new game for player " + name);
                    Response response = Response.newBuilder()
                            .setResponseType(Response.ResponseType.TASK)
                            .setMessage("\n++++++++++++++++++++++++++++++++++++++++++++\nYou have entered a new game!\nPlease guess where the ship is by entering the row and column number.\n++++++++++++++++++++++++++++++++++++++++++++")
                            .setImage(game.getImage())
                            .build();
                    response.writeDelimitedTo(out);
                } else if (type == Request.OperationType.ROWCOL) {
                    int row = op.getRow();
                    int col = op.getColumn();

                    if(game.isShip(row,col) && game.isWon()) {
                        writeToLog(name, Message.WIN);
                        Response response = Response.newBuilder()
                                .setResponseType(Response.ResponseType.WON)
                                .setMessage("Congratulations " + name + ", you have won the game!")
                                .setImage(game.getWinImage())
                                .build();
                        response.writeDelimitedTo(out);
                    }

                    if (game.isShip(row,col) && !game.isWon()) {
                        Response response = Response.newBuilder()
                                .setResponseType(Response.ResponseType.TASK)
                                .setMessage("Please guess where the ship is by entering the row and column number.")
                                .setHit(true)
                                .setImage(game.getImage())
                                .build();
                        response.writeDelimitedTo(out);
                    } else if (!game.isShip(row,col) && !game.isWon()){
                        Response response2 = Response.newBuilder()
                                .setResponseType(Response.ResponseType.TASK)
                                .setTask("Please guess where the ship is by entering the row and column number.")
                                .setHit(false)
                                .setImage(game.getImage())
                                .build();
                        response2.writeDelimitedTo(out);

                        // On the client side you would receive a Response object which is the same as the one in line 70, so now you could read the fields
                        System.out.println("Task: " + response2.getResponseType());
                        System.out.println("Image: \n" + response2.getImage());
                        System.out.println("Task: \n" + response2.getTask());
                    } else {
                        Response response = Response.newBuilder()
                                .setResponseType(Response.ResponseType.ERROR)
                                .setMessage("An error has occurred, please try again!")
                                .build();
                        response.writeDelimitedTo(out);

                    }

                } else if (type == Request.OperationType.LEADER) {
                    // Creating Entry and Leader response
                    Response.Builder res = Response.newBuilder()
                            .setResponseType(Response.ResponseType.LEADER);

                    // building an Entry for the leaderboard
                    Entry leader = Entry.newBuilder()
                            .setName("name")
                            .setWins(0)
                            .setLogins(0)
                            .build();

                    // building another Entry for the leaderboard
                    Entry leader2 = Entry.newBuilder()
                            .setName("name2")
                            .setWins(1)
                            .setLogins(1)
                            .build();

                    // adding entries to the leaderboard
                    res.addLeader(leader);
                    res.addLeader(leader2);

                    // building the response
                    Response response3 = res.build();

                    response3.writeDelimitedTo(out);

                    // iterating through the current leaderboard and showing the entries
                    for (Entry lead: response3.getLeaderList()){
                        System.out.println(lead.getName() + ": " + lead.getWins());
                    }

                } else if (type == Request.OperationType.QUIT) {
                    Response response = Response.newBuilder()
                            .setResponseType(Response.ResponseType.BYE)
                            .setMessage("See you next time " + name + "!")
                            .build();
                    response.writeDelimitedTo(out);

                    done = true;
                }

                // Example how to start a new game and how to build a response with the image which you could then send to the server
                // LINE 67-108 are just an example for Protobuf and how to work with the differnt types. They DO NOT
                // belong into this code.


                // adding the String of the game to
                /*
                Response response2 = Response.newBuilder()
                        .setResponseType(Response.ResponseType.TASK)
                        .setImage(game.getImage())
                        .setTask("Select a row and column.")
                        .build();
                 */
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null)   out.close();
                if (in != null)   in.close();
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Writing a new entry to our log
     * @param name - Name of the person logging in
     * @param message - type Message from Protobuf which is the message to be written in the log (e.g. Connect)
     * @return String of the new hidden image
     */
    public static void writeToLog(String name, Message message){
        try {
            // read old log file
            Logs.Builder logs = readLogFile();

            // get current time and data
            Date date = java.util.Calendar.getInstance().getTime();
            System.out.println(date);

            // we are writing a new log entry to our log
            // add a new log entry to the log list of the Protobuf object
            logs.addLog(date.toString() + ": " +  name + " - " + message);

            // open log file
            FileOutputStream output = new FileOutputStream(logFilename);
            Logs logsObj = logs.build();

            // This is only to show how you can iterate through a Logs object which is a protobuf object
            // which has a repeated field "log"

            for (String log: logsObj.getLogList()){

                System.out.println(log);
            }

            // write to log file
            logsObj.writeTo(output);
        }catch(Exception e){
            System.out.println("Issue while trying to save");
        }
    }

    /**
     * Reading the current log file
     * @return Logs.Builder a builder of a logs entry from protobuf
     */
    public static Logs.Builder readLogFile() throws Exception{
        Logs.Builder logs = Logs.newBuilder();

        try {
            // just read the file and put what is in it into the logs object
            return logs.mergeFrom(new FileInputStream(logFilename));
        } catch (FileNotFoundException e) {
            System.out.println(logFilename + ": File not found.  Creating a new file.");
            return logs;
        }
    }


}
