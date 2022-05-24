# %%
import random
import torch
from torch.autograd import Variable
import socket
import sys

# %% # class that interacts with the game in java
class Game():
    def __init__(self, host="localhost", port=38514, num_episodes=100):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.episodes = num_episodes
        self.error_num = 1
        try:
            self.sock.bind((host, port))
        except socket.error as err:
            print('Bind failed. Error Code : ' .format(err))
    
    #Connects to the engine
    def connect(self):
        self.sock.listen(1)
        self.conn, _ = self.sock.accept()
        self.conn.send(bytes(str(self.episodes) + "\n",'UTF-8'))

    #Gets the state of the game from the engine    
    def get_state(self):
        data = self.conn.recv(512)  # get the data from the engine using the socket
        data = data.decode(encoding='UTF-8') # decode the data

        try:
            lista = data.split(";")     # split the data
            reward = int(lista[1])      # get the reward
            action = int(lista[2])      # get the action
            if lista[0] == "gameOver":    # if the game is over send a string of gameover
                self.conn.send(bytes("GAMEOVER\n",'UTF-8'))
                return None, reward, action
                
            state_list = lista[0].split("/")    # get the state
            
            list_dist_pills = list(map(int, state_list[0].replace("[","").replace("]","").split(",")))  # get the distances to the nearest pill in each direction
            list_dist_power_pills = list(map(int, state_list[1].replace("[","").replace("]","").split(",")))    # get the distances to the nearest power pill in each direction
            list_dist_ghosts = list(map(int, state_list[2].replace("[","").replace("]","").split(",")))    # get the distances to the nearest ghost in each direction
            list_edible_ghosts = list(map(int, state_list[3].replace("[","").replace("]","").split(",")))   # get the edible time to the nearest  ghost in each direction

            #Concatenate the lists to make the next state
            next_state = list_dist_pills + list_dist_power_pills + list_dist_ghosts + list_edible_ghosts
            
            max_num = 300
            min_num = 0
            #Normalize the state
            next_state = [(x - min_num)/(max_num - min_num) for x in next_state]
            
            # if state is not valid raise an exception
            if any(x > 1 for x in next_state):
                raise Exception("Next state contains a number greater than 1")
        
        # if the data raises an exception send a string of error      
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

    #Sends the action to the engine
    def send_action(self, action1, action2):
        self.conn.send(bytes(str(action1) + ";" + str(action2) + "\n",'UTF-8'))

# %% Deep Q Neural Network class  
class DQN():
    ''' Deep Q Neural Network class. '''
    #Initializes the DQN network by creating the model and the optimizer
    def __init__(self, state_dim=16, action_dim=4, hidden_dim=8, lr=0.0005, mom=0.9):
        self.criterion = torch.nn.MSELoss()
        self.model = torch.nn.Sequential(
                        torch.nn.Linear(state_dim, hidden_dim),
                        torch.nn.LeakyReLU(),
                        #torch.nn.Linear(hidden_dim,hidden_dim),
                        #torch.nn.LeakyReLU(),
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

# %% Deep Q Neural Network class with replay memory. 
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
            
            #Update the network
            self.update(states.tolist(), all_q_values.tolist())

# %% # Executes the DQN model with replay memory and with the game engine
def q_learning_replay(model, episodes=100, port=38514, gamma=0.7, epsilon=0.2, replay_size=32, memory_size=10000, title='DQN Replay'):
    """Deep Q Learning algorithm using the DQN. """
    game = Game(num_episodes=episodes, port=port)   # creates the game class with the given port and number of episodes
    game.connect()                                 # connects to the game engine
    q_values = []
    memory = []                                   # memory of the replay
    episode_i = 0
    #epsilon = 1
    #epsilon_aux = 11
    #episodeLimit = (episodes-1) * 5/8 + 1
    
    for episode in range(episodes):
        episode_i += 1
        
        # Reset state
        state, _, _ = game.get_state()
        
        while True:
            # Implement greedy search policy to explore the state space
            if random.random() < epsilon: # if random number is less than epsilon, random action
                action1 = random.randint(0,3)
                action2 = (action1+1) % 4
                game.send_action(action1, action2)
            else:
                q_values = model.predict(state)
                prediction = torch.topk(q_values, k=2)
                game.send_action(prediction[1].data[0].item(), prediction[1].data[1].item())

                
            # Take action and add reward to total
            next_state, reward, action = game.get_state()
            
            # Update total
            if type(q_values) != list:
                q_values = q_values.tolist()
            else:
                 q_values = model.predict(state).tolist()

            if next_state is None:
                break
            
            #remove first element from memory
            if len(memory) >= memory_size:
                memory.pop(0)
            memory.append((state, action, next_state, reward, next_state is None))
            
            model.replay(memory, replay_size, gamma)
            #update state
            state = next_state
        
        #Save the model if the episode is mod 10 and the episode is not 0
        if (episode % ((episodes-1)/10)) == 0 and episode != 0:
            torch.save(model, "model" + str(episode) + title + ".mdl")
        
        #if (episode % ((episodeLimit-1)/10)) == 0 and episode <= episodeLimit:
        #    epsilon_aux -= 1
        #    epsilon = epsilon_aux/10
        #    print("At the episode", episode, "Epsilon is", epsilon)

# %% # Executes the DQN model with replay memory and with the game engine
def main():
    # get command line arguments
    args = sys.argv[1:] 
    # create the model with the third parameter as the hidden layer size
    model = DQN_replay(hidden_dim=int(args[3]), lr=0.001, mom=0.9) 
    # execute the model with the first parameter as the number of episodes and and the second as port and the third as the port
    q_learning_replay(model, episodes=int(args[0])+1, replay_size=170, memory_size=100000, port=int(args[1]), title=args[2])    

if __name__ == "__main__":
    main()
