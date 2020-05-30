# NeuralNetwork
This is a program that uses a neural network to process images. Might be used to add filters or effects, such as blur or color correction.
## NeuralNetwork class
NeuralNetwork class represents a basic perceptron network, that can accept inputs and produce output. It can be trained on sets of inputs and outputs.
Name | Parameters | Return | Desription 
---- | ---------- | ------ | ---------- 
processPicture | float[][][] picture | float[][][] | divides picture on pixels and calculates the result.
trainOnPicure | float[][][] inputPic, float[][][] outputPic | void | uses backPropagation method to train network.
trainOnPictures | List<float[][][]> inputPics, List<float[][][]> outputPics, int iterationsCount | void | trains network using sets of input and output pictures.
backPropagation | float[] inputs, float[] idealOutputs | void | calculates error and changes weights based on it.
getInputForPixel | int sx, int sy, float[][][] picture | float[] | takes all pixels around the current one and makes an array of inputs out of them
