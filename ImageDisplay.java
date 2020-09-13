
import java.awt.*;
import java.awt.image.*;
import java.io.*;

import javax.swing.*;


public class ImageDisplay {

	JFrame frame;
	JLabel lbIm1;
	BufferedImage imgOne;
	int mainWidth = 512;
	int mainHeight = 512;


	private void getMatrixes(Matrices matrices, byte[] bytes)
	{
		var matrixSize = mainHeight * mainWidth;
		for(int y = 0; y < mainHeight; y++)
		{
			for(int x = 0; x < mainWidth; x++)
			{
				matrices.matrixRed[x][y] = bytes[x + (y * mainWidth)];
			}
		}
		for(int y = 0; y < mainHeight; y++)
		{
			for(int x = 0; x < mainWidth; x++)
			{
				matrices.matrixGreen[x][y] = bytes[x + (y * mainWidth) + matrixSize];
			}
		}
		for(int y = 0; y < mainHeight; y++)
		{
			for(int x = 0; x < mainWidth; x++)
			{
				matrices.matrixBlue[x][y] = bytes[x + (y * mainWidth) + (matrixSize * 2)];
			}
		}
	}

	private int getPixelAvg(int line, int  column, byte[][] matrix)
	{
		var totalBits = 0;
		var totalPix = 0;
		for (int l = line - 1; l <= line + 1; l++)
		{
			for (int c = column - 1; c <= column + 1; c++)
			{
				if (l >= 0 && l < matrix.length)
				{
					if (c >= 0 && c < matrix.length)
					{
						totalBits++;
						totalPix += matrix[l][c];
					}
				}
			}
		}
		return totalPix / totalBits;
	}

	private int getBitsFromLog(int bits, int logCenter)
	{
		if (bits > logCenter)
		{
			var exp = (bits - logCenter) / (float)256;
			return logCenter + (int)Math.pow(256, exp);
		}
		else if (bits < logCenter) {
			var exp = (logCenter - bits) / (float)256;
			return logCenter - (int)Math.pow(256, exp);
		}
		else return bits;
		// temp = (int)Math.pow(256, (quantBit / 256));
		// if (temp < logCenter)
		// 	return 
	}

	/** Read Image RGB
	 *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
	 */
	private void readImageRGB(int width, int height, String imgPath, BufferedImage img, int quant, int mode)
	{
		try
		{
			int frameLength = mainWidth*mainHeight*3;

			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];
			
			raf.read(bytes);
			byte[][] matrixRed = new byte[mainHeight][mainWidth];
			byte[][] matrixGreen = new byte[mainHeight][mainWidth];
			byte[][] matrixBlue = new byte[mainHeight][mainWidth];
			var matrices = new Matrices(matrixRed, matrixGreen, matrixBlue);
			getMatrixes(matrices, bytes);

			var sizeRatio = (Math.pow(mainHeight / (float)height, 2));
			int totalBits = (int)Math.pow(2, quant);
			int quantRatio = 256 / totalBits;
			float bytesRatio = 1 / totalBits;

			for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
					var realX = (x * mainHeight) / height;
					var realY = (y * mainHeight) / height;

					// byte a = 0;
					byte r = matrixRed[realX][realY];
					byte g = matrixGreen[realX][realY];
					byte b = matrixBlue[realX][realY];

					// byte r = (byte)getPixelAvg(realX, realY, matrixRed);
					// byte g = (byte)getPixelAvg(realX, realY, matrixGreen);
					// byte b = (byte)getPixelAvg(realX, realY, matrixBlue);

					//make it positive
					var rp = r & 0xff;
					var gp = g & 0xff;
					var bp = b & 0xff;

					//round according to quantization
					// rp = Math.round(rp / (float)quantRatio) * quantRatio;
					// gp = Math.round(gp / (float)quantRatio) * quantRatio;
					// bp = Math.round(bp / (float)quantRatio) * quantRatio;

					
					rp = (rp / quantRatio) * quantRatio;
					gp = (gp / quantRatio) * quantRatio;
					bp = (bp / quantRatio) * quantRatio;
					if (mode != -1)
					{
						rp = getBitsFromLog(rp, mode);
						gp = getBitsFromLog(gp, mode);
						bp = getBitsFromLog(bp, mode);
						// rp = (int)(logCustomBase(256, rp) * 256);
						// gp = (int)(logCustomBase(256, gp) * 256);
						// bp = (int)(logCustomBase(256, bp) * 256);
					}

					//the first 8 bits are blue, bits 9 - 16 are green, and bits 17 - 24 are red
					int rb = (rp << 16);
					int gb = (gp << 8);
					int bb = (bp << 0);
					//adding 
					int pix = 0xff000000 | rb | gb | bb;
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					img.setRGB(x,y,pix);
					if (y + 1 == height && x + 1 == width) 
					{
						System.out.println("Last Line");
					}
				}
			}
			raf.close();
		}
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	public void showIms(String[] args){

		// Read a parameter from command line
		float scale = 1;
		int quantization = 8;
		int mode = -1;
		try {
			scale = Float.parseFloat(args[1]);
			quantization = Integer.parseInt(args[2]);
			if (scale > 1 || scale < 0) throw new NumberFormatException();
			if (quantization < 1 || quantization > 8) throw new NumberFormatException();
		} catch (NumberFormatException ex) {
			System.out.println("Please provide a valid number from 0 to 1 for scale (second parameter) and a valid integer from 1 to 8 for quantization (third parameter)");
			return;
		} catch (Exception ex) {
			System.out.println("Please provide the scale (second parameter) and the quantization (third parameter)");
			return;
		}
		try {
			mode = Integer.parseInt(args[3]);
			if (mode < -1 || mode > 255) throw new NumberFormatException();
		} catch (Exception ex)
		{
			System.out.println("Invalid mode. Mode setted to -1 (Uniform quantization)");
		}

		System.out.println("The third parameter was: " + quantization);

		// Read in the specified image
		int newHeight = Math.round(mainHeight * scale);
		int newWidth = Math.round(mainWidth * scale);
		imgOne = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
		readImageRGB(newWidth, newHeight, args[0], imgOne, quantization, mode);

		// Use label to display the image
		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		lbIm1 = new JLabel(new ImageIcon(imgOne));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(lbIm1, c);

		frame.pack();
		frame.setVisible(true);
	}

	private double logCustomBase(int base, int x)
	{
		return Math.log10(x) / Math.log10(base);
	}

	public static void main(String[] args) {
		ImageDisplay ren = new ImageDisplay();
		ren.showIms(args);
	}

	public class Matrices 
	{
		public Matrices(byte[][] mr, byte[][] mg, byte[][] mb)
		{
			matrixRed = mr;
			matrixGreen = mg;
			matrixBlue = mb;
		}

		byte[][] matrixRed;

		byte[][] matrixGreen;

		byte[][] matrixBlue;
	}


}
