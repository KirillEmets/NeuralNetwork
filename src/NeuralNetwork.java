import java.io.*;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;


public class NeuralNetwork {
    static Logger log = Logger.getLogger(NeuralNetwork.class.getName());
    private Neuron[] inputLayer;
    private Neuron[] hiddenLayer;
    private Neuron[] hiddenLayer2;
    private Neuron[] outputLayer;

    static final double A = 0.3;
    static final double E = 0.6;
    static final int N = 3;

    public NeuralNetwork(int inputCount, int hiddenCount, int hidden2Count, int outputCount) {
        Neuron[][] layers = {inputLayer, hiddenLayer, hiddenLayer2, outputLayer};
        int[] sizes = new int[] {inputCount, hiddenCount, hidden2Count, outputCount, 0};

        for (int i = 0; i < layers.length; i++) {
            layers[i] = new Neuron[sizes[i]];
            for (int j = 0; j < sizes[i]; j++) {
                layers[i][j] = new Neuron(j, sizes[i+1]);
            }
        }

        inputLayer = layers[0];
        hiddenLayer = layers[1];
        hiddenLayer2 = layers[2];
        outputLayer = layers[3];
    }

    private double[] calculateOutputs(double[] input) {
        double[] outputs = new double[outputLayer.length];

        for (int i = 0; i < input.length; i++) { //fill inputLayer output values
            inputLayer[i].output = input[i];
        }

        Neuron[][] layers = new Neuron[][]{inputLayer, hiddenLayer, hiddenLayer2, outputLayer};

        for (int i = 1; i < layers.length; i++) {
            for (Neuron n: layers[i]) {
                n.getOutput(layers[i-1]);
            }
        }

        for (int i = 0; i < outputLayer.length; i++) {
            outputs[i] = outputLayer[i].output;
        }

        return outputs;
    }

    public double[][][] processPicture(double[][][] pixelArray) {
        double[][][] arrayOfInputs = convertArrayToInputs(pixelArray);
        double[][][] result = new double[pixelArray.length][pixelArray[0].length][3];

        for (int i = 0; i < result.length; i++) {
            for (int j = 0; j < result[0].length; j++) {
                result[i][j] = calculateOutputs(arrayOfInputs[i][j]);
            }
        }
        return result;
    }

    private void learnOnPicture(double[][][] inputPic, double[][][] outputPic) {
        double[][][] inputs = convertArrayToInputs(inputPic);
        for (int i = 0; i < outputPic.length; i++) {
            for (int j = 0; j < outputPic[0].length; j++) {
                backPropagation(inputs[i][j], outputPic[i][j]);
            }
        }
    }

    public void learnOnPictures(List<double[][][]> inputPics, List<double[][][]> outputPics, int iterationsCount) {
        for (int i = 0; i < iterationsCount; i++) {
            System.out.println(i + " of " + iterationsCount);
            for (int j = 0; j < inputPics.size(); j++) {
                learnOnPicture(inputPics.get(j), outputPics.get(j));
            }
        }
    }

    private void backPropagation(double[] inputs, double[] ideal) {
        double[] outs = calculateOutputs(inputs);
        for (int i = 0; i < outputLayer.length; i++) {
            outputLayer[i].delta = (ideal[i] - outs[i]) * sigmoidDer(outs[i]);
        }

        Neuron[][] layers = new Neuron[][]{outputLayer, hiddenLayer2, hiddenLayer, inputLayer};
        for (int l = 1; l < layers.length; l++) {
            double sum;
            for (Neuron neuron : layers[l]) {
                sum = 0;
                for (int j = 0; j < layers[l-1].length; j++) {
                    sum += neuron.weights[j] * layers[l-1][j].delta ;

                    double grad = neuron.output * layers[l-1][j].delta;
                    double delta = E*grad + A *neuron.lastWeightsChanges[j];
                    neuron.lastWeightsChanges[j] = delta;
                    neuron.weights[j] += delta;
                }
                neuron.delta = sum * sigmoidDer(neuron.output);
            }
        }
    }

    private double sigmoidDer(double x) {
        return x * (1 - x);
    }

    private double[][][] convertArrayToInputs(double[][][] pixels) {
        int width = pixels.length;
        int height = pixels[0].length;
        double[][][] inputs = new double[width][height][N * N * 3];

        int px = 0;
        int py = 0;
        int k = 0;

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                for (int x = -N/2; x < N/2f; x++) {
                    for (int y = -N/2; y < N/2f; y++) {
                        px = i + x;
                        py = j + y;

                        if(px < 0 || px >= width)
                            px = i - x;
                        if(py < 0 || py >= height)
                            py = j - y;

                        k = x + N/2 + (y + N/2) * N;

                        inputs[i][j][k*3] = pixels[px][py][0];
                        inputs[i][j][k*3+1] = pixels[px][py][1];
                        inputs[i][j][k*3+2] = pixels[px][py][2];
                    }
                }
            }
        }

        return inputs;
    }

    public void saveWeights(String path) {
        try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
            oos.writeObject(inputLayer);
            oos.writeObject(hiddenLayer);
            oos.writeObject(hiddenLayer2);
            oos.writeObject(outputLayer);
        }
        catch (IOException e) {
            log.log(Level.SEVERE, "Could not write the weights", e);
        }
    }

    public void loadWeights(String path) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {
            inputLayer = (Neuron[]) ois.readObject();
            hiddenLayer = (Neuron[]) ois.readObject();
            hiddenLayer2 = (Neuron[]) ois.readObject();
            outputLayer = (Neuron[]) ois.readObject();
        }
        catch(IOException | ClassNotFoundException e) {
            log.log(Level.SEVERE, "Could not load the weights", e);
        }
    }
}

class Neuron implements Serializable {
    static final Random r = new Random();

    final double[] weights;
    double[] lastWeightsChanges;
    double input;
    double output;
    double delta;
    int index;

    public Neuron(int index, int weightsCount) {
        weights = randomizedArray(weightsCount);
        lastWeightsChanges = new double[weightsCount];
        this.index = index;
    }

    public double getOutput(Neuron[] prevLayer) {
        double res = 0;
        for (Neuron previous : prevLayer) {
            res += previous.output * previous.weights[index];
        }
        input = res;
        output = f(res);
        return output;
    }

    double f(double x) {
        return 1 / (1 + Math.exp(-x));
    }

    double[] randomizedArray(int c) {
        double[] res = new double[c];
        for (int i = 0; i < c; i++) {
            res[i] = r.nextDouble();
        }

        return res;
    }
}