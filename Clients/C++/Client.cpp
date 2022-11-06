#include <iostream>
#include <string>
#include <cstring>
#include <vector>
#include <sstream>

// these includes may need to be modified depending on your system
#include <sys/socket.h>
#include <arpa/inet.h>
#include <unistd.h>

using namespace std;

int socket_id;
struct sockaddr_in server_address;

void socket_connect(int port) {
	// create socket
	socket_id = socket(AF_INET, SOCK_STREAM, 0);
	if(socket_id < 0) {
		cout << "Error creating socket" << endl;
		exit(-1);
	}
	
    // set additional required connection info
	server_address.sin_family = AF_INET;
    server_address.sin_port = htons(port);
	
    // convert ip address to correct form
	inet_pton(AF_INET, "localhost", &server_address.sin_addr);
	
    // attempt connection
	if(connect(socket_id, (struct sockaddr*) &server_address, sizeof(server_address)) < 0) {
		cout << "Connection failed" << endl;
		exit(-1);
	}
}

class Client{
    string host, name;
    int port;
    
    int grid_size, min_dist;

    int num_players, num_stone, player_number = 0;

    vector<vector<int>> grid;
    vector<vector<int>> moves; // store history of moves

    public:
    Client(string host, int port, string name){
        this->host = host;
        this->port = port;
        this->name = name;

        grid_size = 1000;
        min_dist = 66;

        socket_connect(this->port);
        receive_init_data();

        grid = vector<vector<int> >(grid_size, vector<int>(grid_size, 0));

        send_(this->name);
        cout << "Client initialized" << endl;
    }

    void send_(string message_str){
        char message[2048] = {};
        message_str += "\n";
        strcpy(message, message_str.c_str());
        send(socket_id, message, message_str.length(), 0);
    }

    void send_move(int row, int col){
        send_(to_string(row) + " " + to_string(col) + " " + to_string(player_number));
    }

    string receive_(){
        char message[2048] = {};
        read(socket_id, message, 2048);

        string result = message;
        return result;
    }

    void receive_init_data(){
        string message = receive_();
        istringstream ss(message);

        ss >> num_players;
        ss >> num_stone;
        ss >> player_number;
    }

    void receive_move(int &game_over, int &score1, int &score2, int &row, int &col, int &player_number){
        string message = receive_();
        istringstream ss(message);

        ss >> game_over;
        ss >> score1;
        ss >> score2;
        ss >> row;
        ss >> col;
        ss >> player_number;
    }

    bool check(int row, int col){
        for(auto &move: moves){
            if(move[0] == row && move[1] == col) return false;
        }

        return true;
    }

    void get_move(int &row, int &col){
        // TODO: Implement your logic here
        // The variable row and col should get updated with the desired value
        // These parameters are being passed by reference

        bool is_valid = false;
        
        while(!is_valid){
            row = 1 + rand() % 1000;
            col = 1 + rand() % 1000;

            is_valid = check(row, col);
        }
    }

    void start(){
        for(int i = 0; i < num_players; i++){
            int game_over = 0, score1 = 0, score2 = 0, row = -1, col = -1, other_player_number = 0;

            while(true){
                receive_move(game_over, score1, score2, row, col, other_player_number);
                // check if game is over
                if(game_over == 1){
                    cout << "Game over" << endl;
                    break;
                }

                if(row > 0 && col > 0){
                    grid[row][col] = other_player_number;
                    vector<int> other_move{row, col, other_player_number};
                    moves.push_back(other_move);
                }

                int my_move_row, my_move_col;
                get_move(my_move_row, my_move_col);

                grid[my_move_row][my_move_col] = player_number;
                vector<int> my_move{my_move_row, my_move_col, player_number};
                moves.push_back(my_move);

                send_move(my_move_row, my_move_col);

                cout << "Played at row " << my_move_row << ", col " << my_move_col << endl;
            }
        }
    }
};

int main(int argc, char** argv){
    string host = argv[1], port = argv[2], name = argv[3];

    Client client = Client(host, stoi(port), name);
    client.start();
}