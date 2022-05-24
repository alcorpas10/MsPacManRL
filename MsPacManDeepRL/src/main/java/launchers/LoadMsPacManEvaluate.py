import torch
from torch.autograd import Variable
import socket
import random
import sys

# class that interacts with the game in java
class Game():
    #Initializes the game by initializing the socket at the given port and connecting to the engine in Java 
    def __init__(self, host="localhost", port=38514):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.error_num = 1
        try:
            self.sock.bind((host, port))
        except socket.error as err:
            print('Bind failed. Error Code : ' .format(err))
        self.sock.listen(2)

    #Connects to the engine
    def connect(self):
        self.conn, _ = self.sock.accept()

    #Closes the connection
    def disconnect(self):
        self.conn.close()

    #Gets the state of the game from the engine
    def get_state(self):
        data = self.conn.recv(512) # get the data from the engine using the socket
        data = data.decode(encoding='UTF-8')    # decode the data

        try:
            lista = data.split(";")    # split the data
            reward = int(lista[1])     # get the reward
            action = int(lista[2])    # get the action
            if lista[0] == "gameOver": # if the game is over send a string of gameover
                self.conn.send(bytes("GAMEOVER\n",'UTF-8'))
                return None, reward, action
                
            state_list = lista[0].split("/") # get the state of the game
            
            list_dist_pills = list(map(int, state_list[0].replace("[","").replace("]","").split(",")))      # get the distances to the  nearest pill in each direction
            list_dist_power_pills = list(map(int, state_list[1].replace("[","").replace("]","").split(",")))    # get the distances to the  nearest power pill in each direction
            list_dist_ghosts = list(map(int, state_list[2].replace("[","").replace("]","").split(",")))         # get the distances to the nearest ghost  in each direction
            list_edible_ghosts = list(map(int, state_list[3].replace("[","").replace("]","").split(",")))   # get the edible time of the neares ghost  in each direction
            
            # concatenate the lists to make the next state
            next_state = list_dist_pills + list_dist_power_pills + list_dist_ghosts + list_edible_ghosts
            
            max_num = 300
            min_num = 0
            #Normalize the state
            next_state = [(x - min_num)/(max_num - min_num) for x in next_state]
        
        # In case of the exception save the error and send the error_num to the engine     
        except Exception as e:
            print(e)
            f = open("error_file.txt" ,"a+")
            f.write(str(self.error_num) + ": " + data + "\n")
            f.close()
            self.error_num += 1
            next_state = [-38514, -38514, -38514, -38514, -38514, -38514, -38514, -38514, -38514, -38514, -38514, -38514, -38514, -38514, -38514, -38514]
            reward = 0
            action = 0
        return next_state, reward, action
    
    # Sends the two best actions to the engine
    def send_action(self, action1, action2):
        self.conn.send(bytes(str(action1) + ";" + str(action2) + "\n",'UTF-8'))

#Deep Q Neural Network class. 
class DQN():
    ''' Deep Q Neural Network class. '''
    def __init__(self, state_dim=12, action_dim=4, hidden_dim=8, lr=0.0005, mom=0.9):
        self.criterion = torch.nn.MSELoss()
        self.model = torch.nn.Sequential(
                        torch.nn.Linear(state_dim, hidden_dim),
                        torch.nn.LeakyReLU(),
                        torch.nn.Linear(hidden_dim, action_dim))
        self.optimizer = torch.optim.SGD(self.model.parameters(), lr=lr, momentum=mom)
        
    def update(self, state, y):
        """Update the weights of the network given a training sample. """
        tensor = torch.Tensor(state)
        y_pred = self.model(tensor)
        loss = self.criterion(y_pred, Variable(torch.Tensor(y)))
        self.optimizer.zero_grad()
        loss.backward()
        self.optimizer.step()
        
    def predict(self, state):
        """ Compute Q values for all actions using the DQL. """
        with torch.no_grad():
            return self.model(torch.Tensor(state))

#Deep Q Neural Network class with replay memory. 
class DQN_replay(DQN):
    def replay(self, memory, size=32, gamma=0.9):
        """New replay function"""
        #Sample a random minibatch of size transitions from memory and updates the network
        if len(memory) >= size:
            batch = random.sample(memory,size)
            
            batch_t = list(map(list, zip(*batch))) #Transpose batch list

            states = batch_t[0]
            actions = batch_t[1]
            next_states = batch_t[2]
            rewards = batch_t[3]
            is_dones = batch_t[4]
        
            # Compute the states,action,rewards, is_dones, next_states
            states = torch.Tensor(states)
            actions_tensor = torch.Tensor(actions)
            next_states = torch.Tensor(next_states)
            rewards = torch.Tensor(rewards)
            is_dones_tensor = torch.Tensor(is_dones)
        
            is_dones_indices = torch.where(is_dones_tensor==True)[0]
        
            all_q_values = self.model(states) # predicted q_values of all states
            all_q_values_next = self.model(next_states)
            #Update q values
            all_q_values[range(len(all_q_values)),actions]=rewards+gamma*torch.max(all_q_values_next, axis=1).values
            all_q_values[is_dones_indices.tolist(), actions_tensor[is_dones].tolist()]=rewards[is_dones_indices.tolist()]
        
            # Update the weights of the network
            self.update(states.tolist(), all_q_values.tolist())

# Executes the DQN model with the game engine
def q_execute(model, num_ghosts=4, trials=250, port=38514):
    """Deep Q Learning algorithm using the DQN. """
    game = Game(port=port)              # Creates the game class with the given port
    for i in range(num_ghosts):         #For the number of ghosts in the game
        game.connect()                  #Connect to the engine
        for e in range(trials):         #For the number of trials
            # init
            q_values = []
            state, _, _ = game.get_state() #Get the state of the game

            if state is not None:
                # game loop
                while True:
                    # predict and send action 
                    q_values = model.predict(state)
                    prediction = torch.topk(q_values, k=2)
                    game.send_action(prediction[1].data[0].item(), prediction[1].data[1].item())

                    # get next state
                    state, _ , _= game.get_state()
                    if state is None:
                        # game over
                        break            

        game.disconnect()

    
#Main that initiates the game and the DQN model
def main():
    args = sys.argv[1:]   # Get the arguments from the command line
    model = torch.load(args[0])  # Load the model with the first argument
    q_execute(model, port=int(args[1]))     # Execute the game with the second argument

if __name__ == "__main__":
    main()
