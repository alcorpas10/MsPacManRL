from os import O_EXCL
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

    #Connects to the engine    
    def connect(self):
        self.sock.listen(1)
        self.conn, _ = self.sock.accept()

    #Gets the state of the game from the engine    
    def get_state(self):
        data = self.conn.recv(512) # get the data
        data = data.decode(encoding='UTF-8') # decode the data

        try:
            lista = data.split(";")     # split the data
            reward = int(lista[1])      # get the reward
            action = int(lista[2])      # get the action
            if lista[0] == "gameOver":  # if the game is over send a string of gameover
                self.conn.send(bytes("GAMEOVER\n",'UTF-8'))
                return None, reward, action
                        
            list_dist_ghosts = list(map(int, lista[0].replace("[","").replace("]","").split(",")))   # get the distances to the nearest ghost  in each direction

            max_num = 300
            min_num = 0
            #Normalize the state
            next_state = [(x - min_num)/(max_num - min_num) for x in list_dist_ghosts]
        
        # In case of the exception save the error and send the error_num to the engine    
        except Exception as e:
            print(e)
            f = open("error_file.txt" ,"a+")
            f.write(str(self.error_num) + ": " + data + "\n")
            f.close()
            self.error_num += 1
            next_state = [-38514, -38514, -38514, -38514]
            reward = 0
            action = 0
        return next_state, reward, action
    
    # Sends the two best actions to the engine
    def send_action(self, action1, action2):
        self.conn.send(bytes(str(action1) + ";" + str(action2) + "\n",'UTF-8'))

# Deep Q Neural Network class
class DQN():
    ''' Deep Q Neural Network class. '''
    #initializes the DQN network by creating the model and the optimizer
    def __init__(self, state_dim=4, action_dim=4, hidden_dim=8, lr=0.0005):
        self.criterion = torch.nn.MSELoss()
        self.model = torch.nn.Sequential(
                        torch.nn.Linear(state_dim, hidden_dim),
                        torch.nn.LeakyReLU(),
                        torch.nn.Linear(hidden_dim, action_dim))
        self.optimizer = torch.optim.Adam(self.model.parameters(), lr) #cambiar
    
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

# #Deep Q Neural Network class with replay memory. 
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
def q_execute(model, port=38514):
    """Deep Q Learning algorithm using the DQN. """
    game = Game(port=port) # Creates the game class with the given port
    game.connect() #Connect to the engine
    q_values = [] 
           
    # Reset state
    state, reward, _ = game.get_state()
    
    while True:
        print("\rState: {} | Reward: {}".format(state, reward))

        # Implement greedy search policy to explore the state space
        # Choose action based on the predict of the model. Takes the two most probable actions and sends them to the game
        q_values = model.predict(state)
        prediction = torch.topk(q_values, k=2)
        game.send_action(prediction[1].data[0].item(), prediction[1].data[1].item())

        #Gets the game state
        state, reward, _= game.get_state()
        if state is None:
            break 

# main that initiates the game and the DQN model
def main():
    args = sys.argv[1:]  # get the arguments from the command line    
    model = torch.load(args[0]) # load the model with the first argument 
    q_execute(model, port=int(args[1]))     # execute the game with port as the second argument

if __name__ == "__main__":
    main()

