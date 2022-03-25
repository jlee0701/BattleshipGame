package client;

import java.net.*;
import java.io.*;

import org.json.*;

import buffers.RequestProtos.Request;
import buffers.ResponseProtos.Response;
import buffers.ResponseProtos.Entry;

import java.util.*;
import java.util.stream.Collectors;

class SockBaseClient {

    public static void main (String args[]) throws Exception {
        Socket serverSock = null;
        OutputStream out = null;
        InputStream in = null;
        int i1=0, i2=0;
        int port = 9099; // default port

        // Make sure two arguments are given
        if (args.length != 2) {
            System.out.println("Expected arguments: <host(String)> <port(int)>");
            System.exit(1);
        }
        String host = args[0];
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port] must be integer");
            System.exit(2);
        }

        // Ask user for username
        System.out.println("Please provide your name for the server.");
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        String strToSend = stdin.readLine();

        // Build the first request object just including the name
        Request op = Request.newBuilder()
                .setOperationType(Request.OperationType.NAME)
                .setName(strToSend).build();
        Response response;

        // connect to the server
        serverSock = new Socket(host, port);

        // write to the server
        out = serverSock.getOutputStream();
        in = serverSock.getInputStream();

        op.writeDelimitedTo(out);

        // read from the server
        response = Response.parseDelimitedFrom(in);

        // print the server response.
        System.out.println(response.getMessage());

        boolean done = false;
        while (!done) {
            Response.ResponseType type = response.getResponseType();
            try {
                if (type == Response.ResponseType.GREETING) {
                    System.out.println("* \nWhat would you like to do? \n 1 - to see the leader board \n 2 - to enter a game \n 3 - quit the game");

                    strToSend = stdin.readLine();

                    if (strToSend.equals("1")) {
                        op = Request.newBuilder()
                                .setOperationType(Request.OperationType.LEADER)
                                .build();

                        op.writeDelimitedTo(out);
                        response = Response.parseDelimitedFrom(in);
                        for (Entry lead: response.getLeaderList()){
                            System.out.println(lead.getName() + ": " + lead.getWins());
                        }

                        response = Response.newBuilder()
                                .setResponseType(Response.ResponseType.GREETING)
                                .build();

                    } else if(strToSend.equals("2")) {
                        op = Request.newBuilder()
                                .setOperationType(Request.OperationType.NEW).build();

                        op.writeDelimitedTo(out);
                        response = Response.parseDelimitedFrom(in);
                        System.out.println(response.getMessage());

                    } else if(strToSend.equals("3")) {
                        System.out.println("Exiting the game...");

                        op = Request.newBuilder()
                                .setOperationType(Request.OperationType.QUIT)
                                .build();

                        op.writeDelimitedTo(out);
                        response = Response.parseDelimitedFrom(in);

                    } else {
                        System.out.println("Please enter a valid option!");
                    }

                } else if (type == Response.ResponseType.TASK) {
                    int row = 0;
                    int col = 0;

                    boolean valid = false;

                    System.out.println("\nImage:");
                    System.out.println(response.getImage());

                    while(!valid) {
                        try {
                            System.out.println("Now enter your guesses: ");
                            System.out.print("Row(starts from the top): ");
                            row = Integer.parseInt(stdin.readLine()) - 1;
                            System.out.print("Column(starts from the left): ");
                            col = Integer.parseInt(stdin.readLine()) - 1;
                            valid = true;
                        } catch (Exception e) {
                            System.out.print("\nPlease enter a valid row/column number!\n");
                        }
                    }

                    op = Request.newBuilder()
                            .setOperationType(Request.OperationType.ROWCOL)
                            .setRow(row)
                            .setColumn(col)
                            .build();

                    op.writeDelimitedTo(out);
                    response = Response.parseDelimitedFrom(in);

                    if (response.getHit() == true) {
                        System.out.println("\nCongratulations, you found the ship!\n");
                    } else if (response.getHit() == false) {
                        System.out.println("\nSorry, the ship is no where to be found...\n\n");
                    } else {
                        //response = Response.parseDelimitedFrom(in);
                        valid = true;
                    }

                    //System.out.println("\nImage:");
                    //System.out.println(response.getImage());

                    //if (response.getResponseType() != )
                    /*
                    if (response.getHit() == true) {
                        System.out.println("\nCongratulations, you found the ship!\n");
                    } else if (response.getHit() == false) {
                        System.out.println("\nSorry, the ship is no where to be found...\n\n");
                    } else {
                        continue;
                    }

                     */

                } else if (type == Response.ResponseType.WON) {
                    System.out.println("\nImage:");
                    System.out.println(response.getImage());

                    System.out.println("\n++++++++++++++++++++++++++++++++++++++++++++\n" + response.getMessage() + "\n++++++++++++++++++++++++++++++++++++++++++++\n");

                    response = Response.newBuilder()
                            .setResponseType(Response.ResponseType.GREETING)
                            .build();
                    /*
                    System.out.println("* \nWhat would you like to do now? \n 1 - to see the leader board \n 2 - to enter a game \n 3 - quit the game");
                    strToSend = stdin.readLine();

                    if (strToSend.equals("1")) {
                        op = Request.newBuilder()
                                .setOperationType(Request.OperationType.LEADER)
                                .build();

                        op.writeDelimitedTo(out);
                        response = Response.parseDelimitedFrom(in);
                        for (Entry lead: response.getLeaderList()){
                            System.out.println(lead.getName() + ": " + lead.getWins());
                        }
                        continue;

                    } else if(strToSend.equals("2")) {
                        op = Request.newBuilder()
                                .setOperationType(Request.OperationType.NEW)
                                .build();

                        op.writeDelimitedTo(out);
                        response = Response.parseDelimitedFrom(in);
                        System.out.println(response.getMessage());

                    } else if(strToSend.equals("3")) {
                        System.out.println("Exiting the game...");

                        op = Request.newBuilder()
                                .setOperationType(Request.OperationType.QUIT)
                                .build();

                        op.writeDelimitedTo(out);
                        response = Response.parseDelimitedFrom(in);

                    } else {
                        System.out.println("Please enter a valid option!");
                    }

                     */

                } else if (type == Response.ResponseType.BYE) {
                    System.out.println(response.getMessage());
                    done = true;
                } else if (type == Response.ResponseType.LEADER) {
                    System.out.println(response.getMessage());

                    response = Response.newBuilder()
                            .setResponseType(Response.ResponseType.GREETING)
                            .build();

                } else {
                    response = Response.newBuilder()
                            .setResponseType(Response.ResponseType.GREETING)
                            .build();

                    /*
                    System.out.println("* \nWhat would you like to do? \n 1 - to see the leader board \n 2 - to enter a game \n 3 - quit the game");
                    strToSend = stdin.readLine();

                    if (strToSend.equals("1")) {
                        op = Request.newBuilder()
                                .setOperationType(Request.OperationType.LEADER)
                                .build();

                        op.writeDelimitedTo(out);
                        response = Response.parseDelimitedFrom(in);
                        for (Entry lead: response.getLeaderList()){
                            System.out.println(lead.getName() + ": " + lead.getWins());
                        }
                        continue;

                    } else if(strToSend.equals("2")) {
                        op = Request.newBuilder()
                                .setOperationType(Request.OperationType.NEW)
                                .build();

                        op.writeDelimitedTo(out);
                        response = Response.parseDelimitedFrom(in);
                        System.out.println(response.getMessage());

                    } else if(strToSend.equals("3")) {
                        System.out.println("Exiting the game...");

                        op = Request.newBuilder()
                                .setOperationType(Request.OperationType.QUIT)
                                .build();

                        op.writeDelimitedTo(out);

                        response = Response.parseDelimitedFrom(in);

                    } else {
                        System.out.println("Please enter a valid option!");
                    }

                     */
                }

            } catch (Exception e) {
                if (in != null) in.close();
                if (out != null) out.close();
                if (serverSock != null) serverSock.close();
            } finally {

            }
        }
        if (in != null) in.close();
        if (out != null) out.close();
        if (serverSock != null) serverSock.close();

        System.exit(0);
    }
}


