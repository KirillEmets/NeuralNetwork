import javax.imageio.ImageIO;
import javax.swing.*;
//DASJHAJSK
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.util.logging.*;

public class Program extends JFrame
{
    final int N = 2; //weight and height of unit in pixels
    static Logger log = Logger.getLogger(Program.class.getName());

    JLabel lbl;

    public static void main(String[] args) {
        new Program();

    }

    public Program() {
        super("Picture");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        lbl = new JLabel();
        lbl.setVerticalAlignment(SwingConstants.CENTER);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel contents = new JPanel();
        contents.add(lbl);

        setContentPane(contents);
        setSize(600, 600);
        setVisible(true);

        BufferedImage source = readImage("/home/kirill/Pictures/Network/5.png");
        BufferedImage prototype = readImage("/home/kirill/Pictures/Network/6.png");
        BufferedImage toDisplay = readImage("/home/kirill/Pictures/Network/3.png");


        NeuralNetwork neuralNetwork = new NeuralNetwork(N*N*3, N*N*3, N*N*3);

        double[][] inputs = convertArrayToInputs(convertImageToPixelArray(source));
        double[][] outputs = convertArrayToInputs(convertImageToPixelArray(prototype));

        double[][] result = convertArrayToInputs(convertImageToPixelArray(toDisplay));

        System.out.println("learn s");
        neuralNetwork.learn(inputs, outputs, 20);
        System.out.println("learn f");

        for (int i = 0; i < result.length; i++) {
            result[i] = neuralNetwork.getResult(result[i]);
        }
        System.out.println("got the picture");

        int[] p = convertOutputToImage(result, toDisplay.getWidth());
        WritableRaster r = toDisplay.getRaster();
        r.setPixels(0,0,toDisplay.getWidth(),toDisplay.getHeight(), p);

        setImage(toDisplay);
    }

    BufferedImage readImage(String path) {
        try {
            return ImageIO.read(new File(path));
        }
        catch (IOException e) {
            log.log(Level.SEVERE,"Couldn't read the file: ".concat(path), e);
            return null;
        }
    }

    void setImage(BufferedImage image) {
        if(image != null) {
            ImageIcon icon = new ImageIcon(image.getScaledInstance(image.getWidth(), image.getHeight(), Image.SCALE_SMOOTH));
            lbl.setIcon(icon);
        }
    }

    double[][][] convertImageToPixelArray(BufferedImage image) {
        final WritableRaster r = image.getRaster();
        final int width = r.getWidth() - r.getWidth() % N;
        final int height = r.getHeight() - r.getHeight() % N;
        int count = width*height;

        double[] rawPixels = new double[count * 3];
        r.getPixels(0, 0, width, height, rawPixels);

        double[][][] pixels = new double[width][height][3];
        for (int i = 0; i < count; i++) {
            pixels[i % width][i / width][0] = (rawPixels[i * 3]) / 255D;
            pixels[i % width][i / width][1] = (rawPixels[i * 3 + 1]) / 255D;
            pixels[i % width][i / width][2] = (rawPixels[i * 3 + 2]) / 255D;
        }

        return pixels;
    }

    double[][] convertArrayToInputs(double[][][] pixels) {
        int width = pixels.length / N;
        int height = pixels[0].length / N;
        double[][] inputs = new double[width*height][N * N * 3];

        int square = 0;
        int index = 0;
        for (int x = 0; x < width; x+= 1) {
            for (int y = 0; y < height; y+= 1) {
                square = y * width + x;

                for (int j = 0; j < N; j++) {
                    for (int i = 0; i < N; i++) {
                        index = j * N + i;

                        inputs[square][index * 3] = pixels[x * N + i][y * N + j][0];
                        inputs[square][index * 3 + 1] = pixels[x * N + i][y * N + j][1];
                        inputs[square][index * 3 + 2] = pixels[x * N + i][y * N + j][2];
                    }
                }
            }
        }
        return inputs;
    }

    int[] convertOutputToImage(double[][] output, int imageWidth) {
        int w = imageWidth - imageWidth % N;
        int h = (output.length * N * N) / w;
        int[] rawPixels = new int[output.length * output[0].length];

        for (int i = 0; i < output.length; i++) {
            int x = i % (w / N);
            int y = i / (w / N);
            for (int j = 0; j < output[i].length / 3; j++) {
                int px = j % N;
                int py = j / N;
                int c = (y*N + py) * imageWidth + (x*N + px);

                rawPixels[c*3] = (int)(output[i][j * 3] * 255);
                rawPixels[c*3 + 1] = (int)(output[i][j * 3 + 1]* 255);
                rawPixels[c*3 + 2] = (int)(output[i][j * 3 + 2] * 255);
            }
        }

        return rawPixels;

       /* DataBufferInt buffer = new DataBufferInt(rawPixels, rawPixels.length);
        int[] bandMasks = {0xFF00, 0xFF, 0xFF000000};
        WritableRaster raster = Raster.createPackedRaster(buffer, w, h, w, bandMasks, null);
        raster.setPixels(0,0, w, h, rawPixels);
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        image.setData(raster);
        return image;*/
    }
}
