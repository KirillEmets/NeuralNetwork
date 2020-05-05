import java.util.Random;

public class NeuralNetwork {
    Neuron[] inputLayer;
    Neuron[] hiddenLayer;
    Neuron[] outputLayer;

    static final double A = 0.3;
    static final double E = 0.7;

    public NeuralNetwork(int inputCount, int hiddenCount, int outputCount) {

        inputLayer = new Neuron[inputCount];
        for (int i = 0; i < inputCount; i++) {
            inputLayer[i] = new Neuron(i, hiddenCount);
        }

        hiddenLayer = new Neuron[hiddenCount];
        for (int i = 0; i < hiddenCount; i++) {
            hiddenLayer[i] = new Neuron(i, outputCount);
        }

        outputLayer = new Neuron[outputCount];
        for (int i = 0; i < outputCount; i++) {
            outputLayer[i] = new Neuron(i, 0);
        }
    }

    public double[] getResult(double[] input) {
        for (int i = 0; i < input.length; i++) {
            inputLayer[i].output = input[i];
        }

        for (Neuron neuron : hiddenLayer) {
            neuron.getOutput(inputLayer);
        }

        double[] outputs = new double[outputLayer.length];

        for (int i = 0; i < outputLayer.length; i++) {
            outputs[i] = outputLayer[i].getOutput(hiddenLayer);
        }

        return outputs;
    }

    void learn(double[][] inputs, double[][] outputs, int iterationsCount) {
        for (int c = 0; c < iterationsCount; c++) {
            for (int i = 0; i < inputs.length; i++) {
                changeWeights(inputs[i], outputs[i]);
            }
        }
    }

    void changeWeights(double[] inputs, double[] ideal) {
        double[] outs = getResult(inputs);
        for (int i = 0; i < outputLayer.length; i++) {
            outputLayer[i].delta = (ideal[i] - outs[i]) * sigmoidDer(outs[i]);
        }

        Neuron[][] layers = new Neuron[][]{outputLayer, hiddenLayer, inputLayer};
        for (int l = 1; l < 3; l++) {
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

    double sigmoidDer(double x) {
        return x * (1 - x);
    }
}

class Neuron {
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