import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.*;
import java.io.Console;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.*;

public class Program extends JFrame
{
    static final int N = 7; //weight and height of unit in pixels
    static Logger log = Logger.getLogger(Program.class.getName());

    JLabel lbl;

    File currentLoadedFile = null;
    transient BufferedImage currentImage;

    transient NeuralNetwork network;
    transient Console console;

    transient Map<String, Consumer<String[]>> functionsWithParams;
    transient Map<String, Runnable> functionsWithoutParams;

    public static void main(String[] args) {
        new Program();
    }

    public Program() {
        super("Picture");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        lbl = new JLabel();
        JPanel contents = new JPanel();
        contents.add(lbl);
        setContentPane(contents);
        setSize(600, 600);

        functionsWithParams = Map.of(
                "lw", this::exLoadWeights,
                "sw", this::exSaveWeights,
                "process", this::exProcess,
                "train", this::exTrain,
                "sp", this::exSavePicture,
                "processDir", this::exProcess
        );
        functionsWithoutParams = Map.of(
                "help", this::exShow,
                "show", this::exShow
        );

        network = new NeuralNetwork(N * N * 3, 12, 12, 3);
        getKey();
    }

    void getKey() {
        console = System.console();
        if(console != null) {
            String[] command = console.readLine().split(" ");
            execute(command);
        }
        else {
            log.log(Level.SEVERE, "No console found. Use java -jar to launch this from terminal");
        }
    }

    void execute(String[] command) {
        if(functionsWithParams.containsKey(command[0])) {
            functionsWithParams.get(command[0]).accept(command);
        }
        else if(functionsWithoutParams.containsKey(command[0])) {
            functionsWithoutParams.get(command[0]).run();
        }
        else {
            System.out.println("No such function, use help.");
        }

        getKey();
    }

    void exLoadWeights(String[] command) {
        if(command.length < 2)
            System.out.println("Load path is required. Use 'load *path*'");
        else {
            if(network.loadWeights(new File(command[1]))) {
                System.out.println("Weighs loaded.");
            }
        }
    }

    void exSaveWeights(String[] command) {
        if (command.length < 2) {
            if (currentLoadedFile != null) {
                System.out.println("Want to save to the current loaded file? y/n");
                String s = console.readLine();
                if((s.equals("y") || s.equals("yes")) && network.saveWeights(currentLoadedFile)) {
                    System.out.println("Weights saved.");
                    return;
                }
            }
            System.out.println("Load path is required. Use 'save *path*'.");
        }
        else {
            network.saveWeights(new File(command[1]));
            System.out.println("Weights saved.");
        }
    }

    void exProcess(String[] command) {
        if (command.length < 2) {
            System.out.println("Picture path is required. Use 'process *path*'.");
        }
        else {
            BufferedImage image = loadImageFromFile(new File(command[1]));
            if(image != null) {
                float[][][] picArray = network.processPicture(convertImageToPixelArray(image));
                currentImage = convertPixelArrayToImage(picArray);
                System.out.println("Image processed.");
            }
        }
    }

    void exTrain(String[] command) {
        if (command.length < 4) {
            System.out.println("Use 'train *directory with inputs* *directory with outputs* *count of iterations*'.");
        }
        else {
            int count = 0;
            try {
                count = Integer.parseInt(command[3]) ;
            }
            catch (Exception e) {
                return;
            }

            File[] inputImages = getImagesFromDirectory(new File(command[1]));
            File[] outputImages = getImagesFromDirectory(new File(command[2]));

            ArrayList<float[][][]> inputArray = new ArrayList<>();
            ArrayList<float[][][]> outputArray = new ArrayList<>();

            System.out.println("Got the pictures");

            BufferedImage inputImage;
            BufferedImage outputImage;

            int i;
            for (File file: inputImages) {
                System.out.println(file.getName());
                i = findFileWithName(outputImages, file.getName());
                if(i != -1) {
                    inputImage = loadImageFromFile(file);
                    outputImage = loadImageFromFile(outputImages[i]);

                    if (inputImage != null && outputImage != null) {
                        if(checkForCompatibility(inputImage, outputImage)) {
                            inputArray.add(convertImageToPixelArray(inputImage));
                            outputArray.add(convertImageToPixelArray(outputImage));
                        }
                        else {
                            System.out.println("File " + file.getName() + " doesn't match to it's pair.");
                        }
                    }
                }
                else {
                    System.out.println("File " + file.getName() + " doesn't have a pair.");
                }
            }
            System.out.println("Training started");
            network.trainOnPictures(inputArray, outputArray, count);
            System.out.println("Training is finished.");
        }
    }

    void exSavePicture(String[] command) {

    }

    void exShow() {
        setVisible(true);
        setImage(currentImage);
    }

    int findFileWithName(File[] files, String name) {
        for (int i = 0; i < files.length; i++) {
            if(files[i].getName().equals(name))
                return i;
        }
        return -1;
    }

    boolean checkForCompatibility(BufferedImage a, BufferedImage b) {
        return (a.getWidth() == b.getWidth() && a.getHeight() == b.getHeight());
    }

    File[] getImagesFromDirectory(File dir) {
        String[] extensions = {".png",".jpg",".jpeg",".bmp"};
        if(dir.isDirectory()) {
            FileFilter filter = file -> {
                for (String e: extensions) {
                    if (file.getName().toLowerCase().endsWith(e))
                        return true;
                }
                return false;
            };

            return dir.listFiles(filter);
        }
        else {
            System.out.println("Path has to lead to directory");
            return new File[] {};
        }
    }

    BufferedImage loadImageFromFile(File file) {
        BufferedImage image = null;
        try {
             image = ImageIO.read(file);
        }
        catch (IOException e) {
            System.out.println("Error: Could not read the image from " + file.getName());
        }
        return  image;
    }

    void setImage(BufferedImage image) {
        if(image != null) {
            ImageIcon icon = new ImageIcon(image);
            lbl.setIcon(icon);
        }
    }

    float[][][] convertImageToPixelArray(BufferedImage image) {
        final WritableRaster r = image.getRaster();
        final int width = r.getWidth();
        final int height = r.getHeight();
        int count = width*height;

        float[] rawPixels = new float[count * 3];
        r.getPixels(0, 0, width, height, rawPixels);

        float[][][] pixels = new float[width][height][3];
        for (int i = 0; i < count; i++) {
            pixels[i % width][i / width][0] = (rawPixels[i * 3]) / 255f;
            pixels[i % width][i / width][1] = (rawPixels[i * 3 + 1]) / 255f;
            pixels[i % width][i / width][2] = (rawPixels[i * 3 + 2]) / 255f;
        }

        return pixels;
    }

    BufferedImage convertPixelArrayToImage(float[][][] arrayPic) {
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
