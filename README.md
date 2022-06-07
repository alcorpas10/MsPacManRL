# MsPacManRL
Implementation of reinforcement learning techniques for behavioral learning in the game MsPacMan Vs. Ghosts.

<img src="https://user-images.githubusercontent.com/60663710/170696442-634389db-e182-480f-861b-56fc7eaaf0e2.png" alt="MsPacMan" width="350"/>

## MsPacManQLearn
Implementation of Q-Learning for the MsPacMan behavior learning.

### Train
* TrainChaseModel: class that trains the chase model.  
  * Params: model name, number of episodes.  
* TrainFleeModel: class that trains the flee model.
  * Params: model name, number of episodes.      
* TrainOriginalModel: class that trains the original model.
  * Params: model name, number of episodes.
* TrainPillsModel: class that trains the pills model.
  * Params: model name, number of episodes.  
* LoadAndTrainModel: class that loads a model that has been trained and trains it more.
  * Params: model name of the exisiting model, number of episodes.

### Run model
* MainQ: class that trains a model and runs it.  
* RunFsmModel: class that runs the game with a fsm.  
* RunRandomModel: class that loads an original model and runs it in a randomly intialized game. 
  * Params: model name.

### Evaluation
* Evaluate: class that evalutes the model following the settings of *config.properties*.


## MsPacManDeepRL
Implementation of Deep Reinforcement Learning for the MsPacMan behavior learning.  
In every train, evaluate or run game you have to run both python server and java server.  

### Train
#### Java
* MainTrain: class that trains the general model.  
  * Params: port.  
* MainTrainEdible: class that trains the edible model(default:long state).    
  * Params: port.  
* MainTrainNotEdible: class that trains the not edible model.    
  * Params: port.  
#### Python
* trainerGeneral: class that trains the general model.    
  * Params: number of episodes, port, name of the model, number of neurons of the hidden layer.    
* trainerShortState: class that trains the edible model with short state.  
  * Params: number of episodes, port, name of the model, number of neurons of the hidden layer.    
* trainerLongState: class that trains the not edible model and the edible model with long state.  
  * Params: number of episodes, port, name of the model, number of neurons of the hidden layer.    
* trainerGPU: class that trains the general model using CUDA.  
  * Params: number of episodes, port, name of the model, number of neurons of the hidden layer.    

### Run model
#### Java
* MainExecute: class that executes a general game.     
* MainFSM: class that executes an fsm game. It requieres to load the edible model and the not edible model with the python launchers.    
#### Python
* LoadMsPacManGeneral: class that loads and runs the general model.
  * Params: model file's name, port.
* LoadMsPacManShortState: class that loads and runs the edible model with short state.   
  * Params: model file's name, port. 
* LoadMsPacManLongState: class that loads and runs the not edible model and the edible model with long state.  
  * Params: model file's name, port.

### Evaluation
#### Java
* Evaluate: class that evalutes the model following the settings of *config.properties*.    
#### Python
* LoadMsPacManEvaluate: class that evaluates the general model.      
  * Params: model file's name, port.    
* LoadMsPacManEvaluateFSM: class that evaluates the fsm. It has to be run two times. One for an edible model with long state and one for a not edible model.   
  * Params: model file's name, number of different type of ghosts, number of episodes, port.

