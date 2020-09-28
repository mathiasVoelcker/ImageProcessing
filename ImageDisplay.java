
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
				matrices.matrixRed[y][x] = bytes[x + (y * mainHeight)];
			}
		}
		for(int y = 0; y < mainHeight; y++)
		{
			for(int x = 0; x < mainWidth; x++)
			{
				matrices.matrixGreen[y][x] = bytes[x + (y * mainHeight) + matrixSize];
			}
		}
		for(int y = 0; y < mainHeight; y++)
		{
			for(int x = 0; x < mainWidth; x++)
			{
				matrices.matrixBlue[y][x] = bytes[x + (y * mainHeight) + (matrixSize * 2)];;	
			}
		}
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
	}

	private int getPixelAvg(int line, int  column, byte[][] matrix, int filterRatio)
	{
		var totalBits = 0;
		var totalPix = 0;
		int range =  filterRatio >= 2 ? filterRatio / 2 : 1;
		
		for (int c = column - range; c <= column + range; c++)
		{
			if (c >= 0 && c < matrix.length)
			{
				for (int l = line - range; l <= line + range; l++) 
				{
					if (l >= 0 && l < matrix.length)
					{
						totalBits++;
						var mValue = matrix[l][c] & 0xff;
						totalPix += mValue;
					}
				}
			}
		}
		return totalPix / totalBits;
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
			raf.close();
			byte[][] matrixRed = new byte[mainHeight][mainWidth];
			byte[][] matrixGreen = new byte[mainHeight][mainWidth];
			byte[][] matrixBlue = new byte[mainHeight][mainWidth];
			var matrices = new Matrices(matrixRed, matrixGreen, matrixBlue);
			getMatrixes(matrices, bytes);

			int totalBits = (int)Math.pow(2, quant);
			int quantRatio = 256 / totalBits;

			for(int l = 0; l < height; l++)
			{
				for(int c = 0; c < width; c++)
				{
					var scaledL = (l * mainHeight) / height;
					var scaledC = (c * mainWidth) / width;

					byte r, g, b;
					if (height == mainHeight)
					{
						r = matrixRed[scaledL][scaledC];
						g = matrixGreen[scaledL][scaledC];
						b = matrixBlue[scaledL][scaledC];	
					}
					else {
						var filterRatio = mainHeight / height;
						r = (byte)getPixelAvg(scaledL, scaledC, matrixRed, filterRatio);
						g = (byte)getPixelAvg(scaledL, scaledC, matrixGreen, filterRatio);
						b = (byte)getPixelAvg(scaledL, scaledC, matrixBlue, filterRatio);
					}

					var rp = r & 0xff;
					var gp = g & 0xff;
					var bp = b & 0xff;

					//round according to quantization					
					rp = (rp / quantRatio) * quantRatio;
					gp = (gp / quantRatio) * quantRatio;
					bp = (bp / quantRatio) * quantRatio;
					if (mode != -1 && quantRatio != 1)
					{
						rp = getBitsFromLog(rp, mode);
						gp = getBitsFromLog(gp, mode);
						bp = getBitsFromLog(bp, mode);
					}

					//the first 8 bits are blue, bits 9 - 16 are green, and bits 17 - 24 are red
					int rb = (rp << 16);
					int gb = (gp << 8);
					int bb = (bp << 0);
					//combining red, green and blue 
					int pix = 0xff000000 | rb | gb | bb;
					

					img.setRGB(c, l, pix);
				}
			}
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
