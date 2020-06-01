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

    static final float A = 0.3f;
    static final float E = 0.6f;
    static final int N = 3;

    public NeuralNetwork(int inputCount, int hiddenCount, int hiddenCount2, int outputCount) {
        int[] sizes = new int[] {inputCount + 1, hiddenCount, hiddenCount2, outputCount, 0};
        Neuron[][] layers = new Neuron[sizes.length][];

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

    private float[] calculateOutputs(float[] input) {
        float[] outputs = new float[outputLayer.length];

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

    public float[][][] processPicture(float[][][] picture) {
        float[][][] result = new float[picture.length][picture[0].length][3];

        for (int i = 0; i < result.length; i++) {
            for (int j = 0; j < result[0].length; j++) {
                result[i][j] = calculateOutputs(getInputForPixel(i, j, picture));
            }
        }
        return result;
    }

    private void trainOnPicture(float[][][] inputPic, float[][][] outputPic) {
        for (int i = 0; i < outputPic.length; i++) {
            for (int j = 0; j < outputPic[0].length; j++) {
                backPropagation(getInputForPixel(i, j, inputPic), outputPic[i][j]);
            }
        }
    }

    public void trainOnPictures(List<float[][][]> inputPics, List<float[][][]> outputPics, int iterationsCount) {
        int n = iterationsCount*inputPics.size();
        int k = 1;
        for (int i = 0; i < iterationsCount; i++) {
            for (int j = 0; j < inputPics.size(); j++) {
                trainOnPicture(inputPics.get(j), outputPics.get(j));
                System.out.println("train: " + k + " of " + n);
                k++;
            }
        }
    }

    private void backPropagation(float[] inputs, float[] idealOutputs) {
        float[] outs = calculateOutputs(inputs);
        for (int i = 0; i < outputLayer.length; i++) {
            outputLayer[i].delta = (idealOutputs[i] - outs[i]) * activationDerivative(outs[i]);
        }

        float grad;
        float delta;
        float sum;

        Neuron[][] layers = new Neuron[][]{outputLayer, hiddenLayer2, hiddenLayer, inputLayer};
        for (int l = 1; l < layers.length; l++) {
            for (Neuron neuron : layers[l]) {
                sum = 0;
                for (int j = 0; j < layers[l-1].length; j++) {
                    sum += neuron.weights[j] * layers[l-1][j].delta ;

                    grad = neuron.output * layers[l-1][j].delta;
                    delta = E*grad + A *neuron.lastWeightsChanges[j];
                    neuron.lastWeightsChanges[j] = delta;
                    neuron.weights[j] += delta;
                }
                neuron.delta = sum * activationDerivative(neuron.output);
            }
        }
    }

    private float activationDerivative(float x) {
        return x * (1 - x);
    }

    private float[] getInputForPixel(int sx, int sy, float[][][] picture) {
        float[] input = new float[N*N*3];
        int px;
        int py;
        int k;
        for (int x = -N/2; x < N/2f; x++) {
            for (int y = -N/2; y < N/2f; y++) {
                px = sx + x;
                py = sy + y;

                if(px < 0 || px >= picture.length)
                    px = sx - x;
                if(py < 0 || py >= picture[0].length)
                    py = sy - y;

                k = x + N/2 + (y + N/2) * N;

                input[k*3] = picture[px][py][0];
                input[k*3+1] = picture[px][py][1];
                input[k*3+2] = picture[px][py][2];
            }
        }
        return input;
    }

    public boolean saveWeights(File file) {
        try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(inputLayer);
            oos.writeObject(hiddenLayer);
            oos.writeObject(hiddenLayer2);
            oos.writeObject(outputLayer);
        }
        catch (IOException e) {
            log.log(Level.SEVERE, "Could not write the weights", e);
            return false;
        }
        return true;
    }

    public boolean loadWeights(File file) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            inputLayer = (Neuron[]) ois.readObject();
            hiddenLayer = (Neuron[]) ois.readObject();
            hiddenLayer2 = (Neuron[]) ois.readObject();
            outputLayer = (Neuron[]) ois.readObject();
        }
        catch(IOException | ClassNotFoundException e) {
            log.log(Level.SEVERE, "Could not load the weights", e);
            return false;
        }
        return true;
    }
}

class Neuron implements Serializable {
    static final Random r = new Random();

    final float[] weights;
    int index;

    transient float[] lastWeightsChanges;
    transient float input = 0;
    transient float output = 0;
    transient float delta = 0;

    public Neuron(int index, int weightsCount) {
        weights = randomizedArray(weightsCount);
        lastWeightsChanges = new float[weightsCount];
        this.index = index;
    }

    public void getOutput(Neuron[] prevLayer) {
        float res = 0;
        for (Neuron previous : prevLayer) {
            res += previous.output * previous.weights[index];
        }
        input = res;
        output = activation(res);
    }

    float activation(float x) {
        return 1 / (1 + (float)Math.exp(-x));
    }

    float[] randomizedArray(int c) {
        float[] res = new float[c];
        for (int i = 0; i < c; i++) {
            res[i] = r.nextFloat();
        }

        return res;
    }
}