import java.io.*;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;


public class NeuralNetwork {
  static Logger log = Logger.getLogger(NeuralNetwork.class.getName());
  private Neuron[][] layers;
  public Neuron[] getOutputLayer() {
    return layers[layers.length - 1];
  }

  static final float A = 0.0f;
  static final float E = 0.1f;
  static final int N = 3;

  public NeuralNetwork(int count, int[] sizes) {
    layers = new Neuron[count][];
    sizes = Arrays.copyOf(sizes, sizes.length + 1);

    for (int i = 0; i < layers.length; i++) {
      layers[i] = new Neuron[sizes[i]];
      for (int j = 0; j < sizes[i]; j++) {
        layers[i][j] = new Neuron(j, sizes[i+1]);
      }
    }
  }

  private float[] calculateOutputs(float[] input) {
    float[] outputs = new float[getOutputLayer().length];

    for (int i = 0; i < input.length; i++) { //fill inputLayer output values
      layers[0][i].output = input[i];
    }

    for (int i = 1; i < layers.length; i++) {
      for (Neuron n: layers[i]) {
        n.getOutput(layers[i-1]);
      }
    }

    for (int i = 0; i < getOutputLayer().length; i++) {
      outputs[i] = getOutputLayer()[i].output;
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

  public void trainOnPicture(float[][][] inputPic, float[][][] outputPic) {
    for (int i = 0; i < outputPic.length; i++) {
      for (int j = 0; j < outputPic[0].length; j++) {
        backPropagation(getInputForPixel(i, j, inputPic), outputPic[i][j]);
      }
    }
  }

  private void backPropagation(float[] inputs, float[] idealOutputs) {
    float[] outs = calculateOutputs(inputs);
    for (int i = 0; i < getOutputLayer().length; i++) {
      getOutputLayer()[i].delta = (idealOutputs[i] - outs[i]) * activationDerivative(outs[i]);
    }

    float grad;
    float delta;
    float sum;

    for (int l = layers.length - 2; l >= 0; l--) {
      for (Neuron neuron : layers[l]) {
        sum = 0;
        for (int j = 0; j < layers[l+1].length; j++) {
          sum += neuron.weights[j] * layers[l+1][j].delta ;

          grad = neuron.output * layers[l+1][j].delta;
          delta = E*grad + A*neuron.lastWeightsChanges[j];
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

  private float[] getInputForPixel(int x, int y, float[][][] picture) {
    float[] input = new float[N*N*3];
    int currentX;
    int currentY;
    int indexInInput;
    for (int i = -N/2; i < N/2f; i++) {
      for (int j = -N/2; j < N/2f; j++) {
        currentX = x + i;
        currentY = y + j;

        if(currentX < 0 || currentX >= picture.length)
          currentX = x - i;
        if(currentY < 0 || currentY >= picture[0].length)
          currentY = y - j;

        indexInInput = i + N/2 + (j + N/2) * N;

        input[indexInInput*3] = picture[currentX][currentY][0];
        input[indexInInput*3+1] = picture[currentX][currentY][1];
        input[indexInInput*3+2] = picture[currentX][currentY][2];
      }
    }
    return input;
  }

  public boolean saveWeights(File file) {
    try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
      oos.writeObject(layers);
    }
    catch (IOException e) {
      log.log(Level.SEVERE, "Could not write the weights", e);
      return false;
    }
    return true;
  }

  public boolean loadWeights(File file) {
    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
      layers = (Neuron[][]) ois.readObject();
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