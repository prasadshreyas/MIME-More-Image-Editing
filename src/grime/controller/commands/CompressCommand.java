package grime.controller.commands;

import grime.model.Model;

/**
 * This class represents the CompressCommand class that implements the Command interface. It
 * represents the command that compresses an image.
 */
public class CompressCommand implements Command {
  private final Model model;

  /**
   * Constructor for CompressCommand that takes in a model.
   * @param model model to be used
   */
  public CompressCommand(Model model) {
    this.model = model;
  }

  @Override
  public void execute(String[] args) throws Exception {
    ArgValidator.validate(args, 3);
    if (Integer.parseInt(args[0]) < 0 || Integer.parseInt(args[0]) > 100) {
      throw new IllegalArgumentException("Percentage must be between 0 and 100.");
    }

    if (model.containsImage(args[1])) {
      if (!model.containsImage(args[2])) {
        model.compressImage(Integer.parseInt(args[0]), args[1], args[2]);
      } else {
        throw new IllegalArgumentException("Image " + args[2] + " already exists.");
      }
    } else {
      throw new IllegalArgumentException("Image " + args[1] + " does not exist.");
    }

  }
}
