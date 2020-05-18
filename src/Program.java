import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.*;

public class Program extends JFrame
{
    final int N = 3; //weight and height of unit in pixels
    static Logger log = Logger.getLogger(Program.class.getName());

    JLabel lbl;
    JLabel lbl2;

    public static void main(String[] args) {
        new Program();
    }

    public Program() {
        super("Picture");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        lbl = new JLabel();
        lbl2 = new JLabel();

        JPanel contents = new JPanel();
        contents.add(lbl);
        contents.add(lbl2);

        setContentPane(contents);
        setSize(600, 600);
        setVisible(true);


        BufferedImage toDisplay = readImage("/home/kirill/Pictures/Network/2.png");

        ArrayList<double[][][]> inputImages = new ArrayList<>();
        ArrayList<double[][][]> outputImages = new ArrayList<>();
        for (int i = 2; i <= 2; i++) {
            inputImages.add(convertImageToPixelArray(readImage("/home/kirill/Pictures/Network/" + i + ".png")));
            outputImages.add(convertImageToPixelArray(readImage("/home/kirill/Pictures/Network/" + i + ".png")));
        }

        NeuralNetwork neuralNetwork = new NeuralNetwork(N * N * 3, 5, 5, 3);

        System.out.println("learn s");
        neuralNetwork.learnOnPictures(inputImages, outputImages, 10);
        System.out.println("learn f");


        var result = neuralNetwork.processPicture(convertImageToPixelArray(toDisplay));
        System.out.println("got the picture");

        toDisplay = convertPixelArrayToImage(result);

        setImage(toDisplay, toDisplay);
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

    void setImage(BufferedImage image, BufferedImage image2) {
        if(image != null) {
            ImageIcon icon = new ImageIcon(image.getScaledInstance(image.getWidth(), image.getHeight(), Image.SCALE_SMOOTH));
            lbl.setIcon(icon);
        }
        if(image != null) {
            ImageIcon icon = new ImageIcon(image2.getScaledInstance(image2.getWidth(), image2.getHeight(), Image.SCALE_SMOOTH));
            lbl2.setIcon(icon);
        }
    }

    double[][][] convertImageToPixelArray(BufferedImage image) {
        final WritableRaster r = image.getRaster();
        final int width = r.getWidth();
        final int height = r.getHeight();
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

    BufferedImage convertPixelArrayToImage(double[][][] arrayPic) {
        int w = arrayPic.length;
        int h = arrayPic[0].length;
        int[] rawPixels = new int[w * h * 3];

        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                rawPixels[(i + j * w) * 3] = (int) (arrayPic[i][j][0] * 255);
                rawPixels[(i + j * w) * 3 + 1] = (int) (arrayPic[i][j][1] * 255);
                rawPixels[(i + j * w) * 3 + 2] = (int) (arrayPic[i][j][2] * 255);
            }
        }

        DataBufferInt buffer = new DataBufferInt(rawPixels, rawPixels.length);
        int[] bandMasks = {0x00ff0000, 0x0000ff00, 0x000000ff};
        WritableRaster raster = Raster.createPackedRaster(buffer, w, h, w, bandMasks, null);
        raster.setPixels(0,0, w, h, rawPixels);
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
        image.setData(raster);
        return image;
    }
}
