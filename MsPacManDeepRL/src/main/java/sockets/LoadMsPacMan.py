from os import O_EXCL
import torch
from torch.autograd import Variable
import socket
import random

class Game():
    def __init__(self, host="localhost", port=38514):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        
        self.error_num = 1
        try:
            self.sock.bind((host, port))
        except socket.error as err:
            print('Bind failed. Error Code : ' .format(err))
        
    def connect(self):
        self.sock.listen(1)
        self.conn, _ = self.sock.accept()
        
    def get_state(self):
        data = self.conn.recv(512)
        data = data.decode(encoding='UTF-8')

        try:
            lista = data.split(";")
            reward = int(lista[1])
            action = int(lista[2])
            if lista[0] == "gameOver":
                self.conn.send(bytes("GAMEOVER\n",'UTF-8'))
                return None, reward, action
                
            state_list = lista[0].split("/")
            
            list_dist_pills = list(map(int, state_list[0].replace("[","").replace("]","").split(",")))
            list_dist_power_pills = list(map(int, state_list[1].replace("[","").replace("]","").split(",")))
            list_dist_ghosts = list(map(int, state_list[2].replace("[","").replace("]","").split(",")))
            

            next_state = list_dist_pills + list_dist_power_pills + list_dist_ghosts
            
            max_num = 500
            min_num = 0

            next_state = [(x - min_num)/(max_num - min_num) for x in next_state]
        
            
        except Exception as e:
            print(e)
            f = open("error_file.txt" ,"a+")
            f.write(str(self.error_num) + ": " + data + "\n")
            f.close()
            self.error_num += 1
            next_state = [-38514, -38514, -38514, -38514, -38514, -38514, -38514, -38514, -38514, -38514, -38514, -38514]
            reward = 0
            action = 0
        return next_state, reward, action
    
    def send_action(self, action1, action2):
        self.conn.send(bytes(str(action1) + ";" + str(action2) + "\n",'UTF-8'))


class DQN():
    ''' Deep Q Neural Network class. '''
    def __init__(self, state_dim=12, action_dim=4, hidden_dim=8, lr=0.0005):
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

class DQN_replay(DQN):
    def replay(self, memory, size=32, gamma=0.9):
        """New replay function"""
        #Try to improve replay speed
        if len(memory) >= size:
            batch = random.sample(memory,size)
            
            batch_t = list(map(list, zip(*batch))) #Transpose batch list

            states = batch_t[0]
            actions = batch_t[1]
            next_states = batch_t[2]
            rewards = batch_t[3]
            is_dones = batch_t[4]
        
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
        
            
            self.update(states.tolist(), all_q_values.tolist())


def q_execute(model,epsilon=0.1):
    """Deep Q Learning algorithm using the DQN. """
    game = Game()  
    game.connect()
    q_values = []
           
    # Reset state
    state, _, _ = game.get_state()
    
    while True:
        # Implement greedy search policy to explore the state space 
        """if random.random() < epsilon:
            action1 = random.randint(0,3)
            action2 = (action1+1) % 4
            game.send_action(action1, action2)
        else:"""
        q_values = model.predict(state)
        prediction = torch.topk(q_values, k=2)
        game.send_action(prediction[1].data[0].item(), prediction[1].data[1].item())
        

        # Take action and add reward to total
        state, _ , _= game.get_state() 

def main():
    #f = open("model1000.mdl",'r')
    model= torch.load('model1500Prueba100.mdl')
    q_execute(model)    

if __name__ == "__main__":
    main()
