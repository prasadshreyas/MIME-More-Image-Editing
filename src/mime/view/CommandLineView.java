package mime.view;

import java.awt.image.BufferedImage;

/**
 * This class represents the command line view.
 */
public class CommandLineView implements View {

  private final Appendable out;


  /**
   * Constructs a CommandLineView object.
   */
  public CommandLineView() {
    this.out = System.out;
  }

  @Override
  public void display(String message) {
    try {
      this.out.append(message).append("\n");
    } catch (Exception e) {
      throw new IllegalStateException("Could not write to file");
    }
  }

  @Override
  public void displayImage(BufferedImage bufferedImage) {
    this.display("Displaying image:\n");
    this.display(bufferedImage.toString());
  }
}
