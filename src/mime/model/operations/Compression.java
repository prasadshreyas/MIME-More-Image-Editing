package mime.model.operations;

import java.util.Arrays;

import mime.model.image.Image;
import mime.model.image.RGBImage;

/**
 * This class represents the Compression operation for an RGB image.
 */
public class Compression {
  Image originalImage;
  int originalHeight;
  int originalWidth;
  double compressionRatio;


  /**
   * Constructs a new Compression object initializing
   */
  public Compression(Image image, int percentage) {
    this.originalImage = image;
    this.originalHeight = image.getHeight();
    this.originalWidth = image.getWidth();
    this.compressionRatio = percentage / 100.0;
  }

  public Image compressAndUncompress() {

    int[][][] uncompressedChannels = new int[3][][];

    int s = Math.max(calculateNextPowerOfTwo(originalHeight),
            calculateNextPowerOfTwo(originalWidth));

    double paddedMatrix[][][] = padToNextPowerOfTwo(originalImage.getChannels(), s);

    // Apply the Haar wavelet transform
    double transformedMatrix[][][] = new double[3][s][s];
    for (int i = 0; i < 3; i++) {
      transformedMatrix[i] = haar2D(paddedMatrix[i], s);
    }

    // Calculate the compression threshold
    double threshold = calculateThreshold(transformedMatrix, originalHeight, originalWidth);


    // for all channels now,
    for (int i = 0; i < 3; i++) {

      // Apply the threshold to compress the elements in the matrix X
      transformedMatrix[i] = setBelowThreshold(transformedMatrix[i], threshold);


      // Apply the inverse Haar wavelet transform
      transformedMatrix[i] = invHaar2D(transformedMatrix[i], s);


      double[][] unPaddedMatrix = unpadToOriginalSize(transformedMatrix[i], originalHeight, originalWidth);

      int[][] unPaddedMatrixInt = new int[unPaddedMatrix.length][unPaddedMatrix[0].length];

      for (int j = 0; j < unPaddedMatrix.length; j++) {
        for (int k = 0; k < unPaddedMatrix[j].length; k++) {
          unPaddedMatrixInt[j][k] = (int) Math.round(unPaddedMatrix[j][k]);
        }
      }
      uncompressedChannels[i] = unPaddedMatrixInt;
    }

    return new RGBImage(uncompressedChannels[0], uncompressedChannels[1], uncompressedChannels[2]);

  }


  /**
   * Calculates the next power of two of the given number.
   *
   * @param n The number to calculate the next power of two.
   * @return The next power of two of the given number.
   */
  private int calculateNextPowerOfTwo(int n) {
    int power = 1;
    while (power < n) {
      power *= 2;
    }
    return power;
  }


  /**
   * Applies the threshold to the given matrix.
   *
   * @param X         The matrix to be compressed.
   * @param threshold The threshold to be applied.
   */
  private double[][] setBelowThreshold(double[][] X, double threshold) {
    // Apply threshold to compress the elements in the matrix X
    for (int i = 0; i < X.length; i++) {
      for (int j = 0; j < X[i].length; j++) {
        if (Math.abs(X[i][j]) < threshold) {
          X[i][j] = 0.0; // Compress elements below the threshold by setting them to zero
        }
      }
    }
    return X;
  }

  /**
   * Calculates the compression threshold for the given matrix.
   *
   * @param X      The padded matrix.
   * @param height The height of the original matrix.
   * @param width  The width of the original matrix.
   * @return The compression threshold.
   */
  private double calculateThreshold(double[][][] X, int height, int width) {
    double[] flattenedMatrix = flattenMatrix(X, height, width);
    Arrays.sort(flattenedMatrix);
    int valuesToKeep = calculateValuesToKeep(flattenedMatrix.length, compressionRatio);
    return findThreshold(flattenedMatrix, valuesToKeep);
  }

  private int calculateValuesToKeep(int totalElements, double compressionRatio) {
    int valuesToKeep = (int) (totalElements * (1 - compressionRatio));
    // Ensure that valuesToKeep is within the array bounds
    return Math.max(0, Math.min(valuesToKeep, totalElements - 1));
  }

  private double findThreshold(double[] sortedArray, int valuesToKeep) {
    // Adjust index to account for zero-based array indexing
    return sortedArray[sortedArray.length - valuesToKeep - 1];
  }

  private double[] flattenMatrix(double[][][] X, int height, int width) {
    double[] temp = new double[height * width * 3]; // 3 for the three color channels
    int count = 0;
    for (int c = 0; c < 3; c++) {
      for (int i = 0; i < height; i++) {
        System.arraycopy(X[c][i], 0, temp, count, width);
        count += width;
      }
    }

    return temp;
  }

  /**
   * Applies the Haar wavelet transform to the given matrix.
   *
   * @param X The matrix to be transformed.
   * @param c The size of the square matrix.
   */
  private double[][] haar2D(double[][] X, int c) {
    while (c > 1) {
      for (int i = 0; i < c; i++) {
        // Apply transform T to each row
        double[] row = new double[c];
        System.arraycopy(X[i], 0, row, 0, c);
        row = T(row, c);
        System.arraycopy(row, 0, X[i], 0, c);
      }
      for (int j = 0; j < c; j++) {
        // Apply transform T to each column
        double[] col = new double[c];
        for (int i = 0; i < c; i++) {
          col[i] = X[i][j];
        }
        col = T(col, c);
        for (int i = 0; i < c; i++) {
          X[i][j] = col[i];
        }
      }
      c = c / 2;
    }
    return X;
  }

  private double[][] invHaar2D(double[][] X, int s) {
    int c = 2;
    while (c <= s) {
      for (int j = 0; j < c; j++) {
        // Apply inverse transform I to each column
        double[] col = new double[c];
        for (int i = 0; i < c; i++) {
          col[i] = X[i][j];
        }
        col = I(col, c);
        for (int i = 0; i < c; i++) {
          X[i][j] = col[i];
        }
      }
      for (int i = 0; i < c; i++) {
        // Apply inverse transform I to each row
        double[] row = new double[c];
        System.arraycopy(X[i], 0, row, 0, c);
        row = I(row, c);
        System.arraycopy(row, 0, X[i], 0, c);
      }
      c = c * 2;
    }
    return X;
  }

  /**
   * Applies the inverse Haar wavelet transform to the given array.
   *
   * @param s The array to be transformed.
   * @param l The size of the array.
   * @return The transformed array.
   */
  private double[] I(double[] s, int l) {
    double[] result = new double[l];
    int m = 2; // Start with the smallest grouping of 2

    // We will be working with a temp array for each step of the inverse transform
    while (m <= l) {
      double[] temp = new double[l]; // Temporary array to store intermediate values

      // Copy s to temp for manipulation
      for (int i = 0; i < l; i++) {
        temp[i] = s[i];
      }

      // Perform the inverse operation on pairs within the current grouping
      for (int i = 0; i < m / 2; i++) {
        // Calculate the original values from the averages and differences
        result[2 * i] = (temp[i] + temp[m / 2 + i]) / Math.sqrt(2);
        result[2 * i + 1] = (temp[i] - temp[m / 2 + i]) / Math.sqrt(2);
      }

      // Prepare for the next iteration by copying the results back to s
      for (int i = 0; i < m; i++) {
        s[i] = result[i];
      }

      m *= 2; // Double the grouping size for the next iteration
    }
    return result;
  }

  /**
   * Applies the Haar wavelet transform to the given array.
   *
   * @param s The array to be transformed.
   * @param m The size of the array.
   * @return The transformed array.
   */
  private double[] T(double[] s, int m) {
    while (m > 1) {
      double[] temp = new double[m];
      for (int i = 0; i < m; i++) {
        temp[i] = s[i]; // Copy current segment to a temp array
      }
      for (int i = 0; i < m / 2; i++) {
        s[i] = (temp[2 * i] + temp[2 * i + 1]) / Math.sqrt(2); // Normalized average
        s[i + m / 2] = (temp[2 * i] - temp[2 * i + 1]) / Math.sqrt(2); // Normalized difference
      }
      m = m / 2; // Reduce the size by half for the next iteration
    }
    return s;
  }

  /**
   * Pads the matrix to the next power of two.
   *
   * @param channels The channels to be padded.
   * @param s        The next power of two.
   * @return The padded matrix.
   */
  private double[][][] padToNextPowerOfTwo(int[][][] channels, int s) {

    double[][][] paddedChannels = new double[channels.length][s][s];

    for (int i = 0; i < channels.length; i++) {
      for (int j = 0; j < channels[i].length; j++) {
        for (int k = 0; k < channels[i][j].length; k++) {
          paddedChannels[i][j][k] = channels[i][j][k];
        }
      }
    }

    return paddedChannels;

  }

  /**
   * Unpads the matrix to the original size.
   *
   * @param matrix The matrix to be unpadded.
   * @param height The original height of the matrix.
   * @param width  The original width of the matrix.
   * @return The unpadded matrix.
   */
  private double[][] unpadToOriginalSize(double[][] matrix, int height, int width) {
    double[][] newMatrix = new double[height][width];
    for (int i = 0; i < newMatrix.length; i++) {
      for (int j = 0; j < newMatrix[i].length; j++) {
        newMatrix[i][j] = matrix[i][j];
      }
    }
    return newMatrix;
  }


}