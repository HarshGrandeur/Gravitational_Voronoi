import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.lang.Math;
  
public class Client
{
    // Initialize socket and input output streams
    private Socket socket            = null;
    private BufferedReader input   	 = null;
    private PrintWriter out     	 = null;

    // Track game variables
    private int num_players;
    private int num_stones;
    private int player_num;
    private String team;
    private int[][] grid;
    private List<Move> moves;
    private final int MIN_DIST = 66;
    private final int GRID_SIZE = 1000;
  
    // Constructor to put ip address and port
    public Client(String address, int port, String team) throws IOException 
    {
        // Establish a connection
        try
        {
            socket = new Socket(address, port);
            this.team = team;
            System.out.println("Team : " + team + " connected.");
  
            // Client takes input from socket
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
  
            // And also sends its output to the socket
            out = new PrintWriter(socket.getOutputStream(), true);
        }
        catch(UnknownHostException u)
        {
            System.out.println(u);
        }
        catch(IOException i)
        {
            System.out.println(i);
        }

        // get game info and send team name
         // receive game info from server  
        String[] gameInfoSplit = getMove();
        this.num_players = Integer.parseInt(gameInfoSplit[0]);
        this.num_stones = Integer.parseInt(gameInfoSplit[1]);
        this.player_num = Integer.parseInt(gameInfoSplit[2]);
        // Send our name to server
        this.grid = new int[GRID_SIZE][GRID_SIZE];
        out.println(this.team);

    }

    public static void main(String args[]) throws IOException
    {
    	int portNumber;
        String host;
        String team;
        if (args.length == 0) {
            throw new IllegalArgumentException("Please provide arguements in form : Client.java host port team_name");
        }

        //parse the arguements 
        try {
            host = args[0];
            portNumber = Integer.parseInt(args[1]);
            team = args[2];
        }
        catch(Exception e) {
            throw new IllegalArgumentException("Error parsing arguements");
        }

        Client client = new Client(host, portNumber, team);
        client.run();
    }



    /**
     * @throws IOException
     */
    public void run() throws IOException {
        // we play # of games = # of players
        for(int player = 1; player <= this.num_players; player++) {
            while(true) {
                String[] moveData = getMove();
                System.out.println("received move " + Arrays.toString(moveData));
                // check if game is over
                if(Integer.parseInt(moveData[0]) == 1) {
                    System.out.println("Game Over");
                    break;
                }
                // save the player scores
                int[] scores = new int[this.num_players + 1];
                for(int i = 0; i < this.num_players; i++) {
                    scores[i + 1] = Integer.parseInt(moveData[i + 1]);
                }
                
                // new moves 
                int new_move_length = moveData.length - 1 - this.num_players;
                if((new_move_length / 3) * 3 != new_move_length) {
                    System.out.println("Error parsing new moves");
                }
                // create a list of moves 
                moves = new LinkedList<Move>();
                int offset = 1 + this.num_players;
                int ind = 0;

                for(int i = 0; i < new_move_length / 3; i++) {
                    int move_row = Integer.parseInt(moveData[offset + ind++]);
                    int move_col = Integer.parseInt(moveData[offset + ind++]);
                    int p = Integer.parseInt(moveData[offset + ind++]);
                    // sanity check, this should always be true
                    if(p > 0) {
                        this.grid[move_row][move_col] = p;
                        moves.add(new Move(move_row, move_col, p));
                    } else {
                        System.out.println("Error: player info incorrect");
                    }
                }

                // generate move 

                Move myMove = generateMyMove();
                this.moves.add(myMove);
                sendMove(myMove.row, myMove.col);
                System.out.println("Player " + this.player_num + " played at " + myMove.row + " " + myMove.col);

            }
        }

        this.socket.close();

    }


    class Move {
        int row;
        int col;
        int player;
        Move(int row, int col, int player) {
            this.row = row;
            this.col = col;
            this.player = player;
        }
    }

    public double computeDistance(int r1, int c1, int r2, int c2) {
        return Math.sqrt((r2 - r1) * (r2 - r1) + (c2  - c1) * (c2  - c1));
    }
    // check valid move

    public boolean isVaidMove(int row, int col) {
        for(Move move : this.moves) {
            if(computeDistance(move.row, move.col, row, col) < MIN_DIST) {
                return false;
            }
        }
        return true;
    }

    // add your algorithm here

    public Move generateMyMove() {
        int moveRow = 0;
        int moveCol = 0;
        while(true) {
            Random ran = new java.util.Random();
            moveRow = ran.nextInt(1000);
            moveCol = ran.nextInt(1000);
            if(isVaidMove(moveRow, moveCol)) {
                break;
            }
        }
        return new Move(moveRow, moveCol, this.player_num);
    }

    public String[] getMove() throws IOException{
        return input.readLine().split(" ");
    }

    public void sendMove(int row, int col) throws IOException{
        String move = String.valueOf(row) + " " + String.valueOf(col);
        out.println(move);
    }

}


