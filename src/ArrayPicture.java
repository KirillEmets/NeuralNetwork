import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

public class ArrayPicture {
  private float[][][] pixels;
  public float[][][] getPixels() {
    return pixels;
  }
  public int getHeight() {
    return pixels[0].length;
  }
  public int getWidth() {
    return pixels.length;
  }

  public ArrayPicture(BufferedImage image) {
    if (image == null) {
      return;
    }

    final WritableRaster r = image.getRaster();
    final int width = r.getWidth();
    final int height = r.getHeight();
    int count = width * height;

    float[] rawPixels = new float[count * 3];
    r.getPixels(0, 0, width, height, rawPixels);

    pixels = new float[width][height][3];
    for (int i = 0; i < count; i++) {
      pixels[i % width][i / width][0] = (rawPixels[i * 3]) / 255f;
      pixels[i % width][i / width][1] = (rawPixels[i * 3 + 1]) / 255f;
      pixels[i % width][i / width][2] = (rawPixels[i * 3 + 2]) / 255f;
    }
  }

  public ArrayPicture(float[][][] pixels) {
    this.pixels = pixels;
  }


  public BufferedImage toBufferedImage() {
    int w = pixels.length;
    int h = pixels[0].length;
    int[] rawPixels = new int[w * h * 3];

    for (int j = 0; j < h; j++) {
      for (int i = 0; i < w; i++) {
        rawPixels[(i + j * w) * 3] = (int) (pixels[i][j][0] * 255);
        rawPixels[(i + j * w) * 3 + 1] = (int) (pixels[i][j][1] * 255);
        rawPixels[(i + j * w) * 3 + 2] = (int) (pixels[i][j][2] * 255);
      }
    }

    DataBufferInt buffer = new DataBufferInt(rawPixels, rawPixels.length);
    int[] bandMasks = {0x00ff0000, 0x0000ff00, 0x000000ff};
    WritableRaster raster = Raster.createPackedRaster(buffer, w, h, w, bandMasks, null);
    raster.setPixels(0, 0, w, h, rawPixels);
    BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
    image.setData(raster);
    return image;
  }
}