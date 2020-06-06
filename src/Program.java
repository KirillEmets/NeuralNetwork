import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.*;
import java.io.Console;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.*;

public class Program extends JFrame
{
  static final int N = 3; //weight and height of unit in pixels
  static Logger log = Logger.getLogger(Program.class.getName());

  JLabel lbl;

  File currentLoadedWeights = null;
  File currentLoadedPicture = null;
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
            "pf", this::exProcessFolder
    );
    functionsWithoutParams = Map.of(
            "help", this::exHelp,
            "show", this::exShow,
            "sp", this::exSavePicture
            );

    network = new NeuralNetwork(new int[]{N * N * 3, 10, 10, 3});
    getCommand();
  }

  void getCommand() {
    console = System.console();
    if (console == null) {
      log.log(Level.SEVERE, "No console found. Use java -jar to launch this from terminal");
      return;
    }

    String[] command = console.readLine().split(" ");
    execute(command);
  }

  // выполняю введенную команду (коллекция с ссылками на методы выше)
  void execute(String[] command) {
    if(functionsWithParams.containsKey(command[0])) {
      functionsWithParams.get(command[0]).accept(command);
    }
    else if(functionsWithoutParams.containsKey(command[0])) {
      functionsWithoutParams.get(command[0]).run();
    }
    else {
      println("No such function, use help.");
    }

    getCommand();
  }

  void exLoadWeights(String[] command) {
    if (command.length < 2) {
      println("Load path is required. Use 'load *path*'");
      return;
    }
    if (network.loadWeights(new File(command[1]))) {
      println("Weighs loaded.");
    }
  }

  void exSaveWeights(String[] command) {
    if (command.length < 2) {
      if (currentLoadedWeights != null) {
        println("Want to save to the current loaded file? y/n");
        String s = console.readLine();
        if ((s.equals("y") || s.equals("yes")) && network.saveWeights(currentLoadedWeights)) {
          println("Weights saved.");
          return;
        }
      }
      println("Load path is required. Use 'save *path*'.");
      return;
    }

    network.saveWeights(new File(command[1]));
    println("Weights saved.");
  }

  // загружаю изображение и кидаю в нейросеть
  void exProcess(String[] command) {
    if (command.length < 2) {
      println("Picture path is required. Use 'process *path*'.");
      return;
    }

    File file = new File(command[1]);
    BufferedImage image = loadImageFromFile(file);
    if (image != null) {
      ArrayPicture picArray = network.processPicture(new ArrayPicture(image));
      currentImage = picArray.toBufferedImage();
      currentLoadedPicture = file;
      println("Image processed.");
    }
  }

  // подтягиваю все изображения из папки и обрабатываю их
  void exProcessFolder(String[] command) {
    if (command.length < 2) {
      println("Folder path is required. Use 'pf *path to folder*'.");
      return;
    }

    File folder = new File(command[1]);
    if(!folder.isDirectory()) {
      println("Path must lead to a directory.");
      return;
    }

    File newFolder = new File(folder.getAbsolutePath() + " (result)");
    if (!newFolder.exists() && !newFolder.mkdir()) {
      println("Directory cannot be created.");
      return;
    }

    File[] images = getImagesFromDirectory(folder);
    BufferedImage loadedImage = null;
    for (File file : images) {
      loadedImage = loadImageFromFile(file);
      ArrayPicture pic = network.processPicture(new ArrayPicture(loadedImage));
      File toSave = new File(newFolder.getPath(), file.getName());
      saveImage(toSave, pic.toBufferedImage());
    }
    println("Done.");
  }

  // нахожу пары фотографий в папках и передаю их сети для тренировки
  void exTrain(String[] command) {
    if (command.length < 4) {
      println("Use 'train *folder with inputs* *folder with outputs* *count of iterations*'.");
      return;
    }
    int count = 0;
    try {
      count = Integer.parseInt(command[3]);
    } catch (Exception e) {
      println("4th parameter must be a number.");
      return;
    }

    File[] inputImages = getImagesFromDirectory(new File(command[1]));
    File[] outputImages = getImagesFromDirectory(new File(command[2]));

    ArrayPicture inputPicture;
    ArrayPicture outputPicture;
    println("Training started");
    for (int c = 0; c < count; c++) {
      int i;
      for (File file : inputImages) {
        println(file.getName());
        i = findFileWithName(outputImages, file.getName());
        if (i != -1) {
          inputPicture = new ArrayPicture(loadImageFromFile(file));
          outputPicture = new ArrayPicture(loadImageFromFile(outputImages[i]));
          if (checkForCompatibility(inputPicture, outputPicture)) {
            network.trainOnPicture(inputPicture, outputPicture);
          } else {
            println("File " + file.getName() + " doesn't match to it's pair.");
          }
        } else {
          println("File " + file.getName() + " doesn't have a pair.");
        }
      }
    }
    println("Training is finished.");
  }

  void exSavePicture() {
    if (currentLoadedPicture == null)  {
      println("You need to process a picture first.");
      return;
    }

    File file = new File(currentLoadedPicture.getPath() + " (copy)");
    saveImage(file, currentImage);
  }

  void exShow() {
    setVisible(true);
    setImage(currentImage);
  }

  void exHelp() {
    println("List of commands: \n" +
            "lw - load weights \n" +
            "sw - save weights \n" +
            "process - process picture \n" +
            "train - train on sets of images \n" +
            "pf - process folder \n" +
            "sp - save processed picture \n");
  }

  void saveImage(File file, BufferedImage image) {
    try {
      ImageIO.write(image, "jpg", file);
      println(file.getName() + " saved.");
    }
    catch (IOException e) {
      println("Could not save the file.");
    }
  }

  int findFileWithName(File[] files, String name) {
    for (int i = 0; i < files.length; i++) {
      if(files[i].getName().equals(name))
        return i;
    }
    return -1;
  }

  // сравниваю размеры изображений
  boolean checkForCompatibility(ArrayPicture a, ArrayPicture b) {
    return (a.getWidth() == b.getWidth() && a.getHeight() == b.getHeight());
  }

  void println(String text) {
    System.out.println(text);
  }

  File[] getImagesFromDirectory(File dir) {
    String[] extensions = {".png", ".jpg", ".jpeg", ".bmp"};
    if (!dir.isDirectory()) {
      println("Path has to lead to a directory");
      return new File[]{};
    }

    FileFilter filter = file -> {
      for (String e : extensions) {
        if (file.getName().toLowerCase().endsWith(e))
          return true;
      }
      return false;
    };
    return dir.listFiles(filter);
  }

  BufferedImage loadImageFromFile(File file) {
    BufferedImage image = null;
    try {
      image = ImageIO.read(file);
    }
    catch (IOException e) {
      println("Error: Could not read the image from " + file.getName());
    }
    return image;
  }

  void setImage(BufferedImage image) {
    if(image != null) {
      ImageIcon icon = new ImageIcon(image);
      lbl.setIcon(icon);
    }
  }
}
