# NeuralNetwork
This is a program that uses a neural network to process images. Might be used to add filters or effects, such as blur or color correction.
## NeuralNetwork class
NeuralNetwork class represents a basic perceptron network, that can accept inputs and produce output. It can be trained on sets of inputs and outputs.  
Note: `float[width][height][3]` picture is a representation of an image as 3d array. Third dimension contains colors in RGB.
### Main methods
Name | Parameters | Return | Desription 
---- | ---------- | ------ | ---------- 
processPicture | float[][][] picture | float[][][] | divides picture on pixels and calculates the result.
trainOnPicure | float[][][] inputPic, float[][][] outputPic | void | uses backPropagation method to train network.
backPropagation | float[] inputs, float[] idealOutputs | void | calculates error and changes weights based on it.
getInputForPixel | int sx, int sy, float[][][] picture | float[] | takes all pixels around the current one and makes an array of inputs out of them
## Program
Main part of the program that contains ui and file processing.  
At the beginnig program initializes the network and sets all the necessary settings. Then it uses `getCommand` method to get command from the terminal and executes it with `execute`.
### UI commands
Name | Desription | Usage
---- | ---------- | -----
lw | load weighs (network config) from file | lw *'path'*
sw | save weights to file | sw *'path'*
process | process picture | process *'path'*
train | train network on two sets of pictures containig in two folders* N times | train *'input folder path'* *'output folder path'* '*N*'
sp | save picture as file | *not yet*
processDir | process all picters in folder | *not yet*
show | show picture in window | show
help | help | help
  
*pairs of pictures must have the same name and resolution.

> To be continued
