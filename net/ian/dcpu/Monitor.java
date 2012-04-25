package net.ian.dcpu;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Monitor extends Canvas {
	private static final long serialVersionUID = 1L;
	
	public static final int WIDTH = 128;
	public static final int HEIGHT = 96;
	public static final int SCALE = 5;
	
	public BufferedImage font[];
	public MonitorCell cells[];
	
	private AffineTransformOp scaler;
	
	public static class MonitorCell {
		char character;
		Color fgColor, bgColor;
		
		public MonitorCell(char c, Color fg, Color bg) {
			character = c;
			fgColor = fg;
			bgColor = bg;
		}
	}
	
	public Monitor(DCPU cpu) {
        setPreferredSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
        setMinimumSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
        setMaximumSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
        
        cells = new MonitorCell[32 * 12];
        for (int i = 0; i < 32 * 12; i++)
        	cells[i] = new MonitorCell((char)0, Color.BLACK, Color.BLACK);
        
        try {
			loadFont(cpu);
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        AffineTransform scale = new AffineTransform();
        scale.scale(SCALE, SCALE);
        scaler = new AffineTransformOp(scale, null);
	}
	
	public static Color convertColor(int colorBits) {
		boolean h = (colorBits >> 3 & 1) == 1;

		int r = 0xAA * (colorBits >> 2 & 1) + (h ? 0x55 : 0);
		int g = 0xAA * (colorBits >> 1 & 1) + (h ? 0x55 : 0);
		int b = 0xAA * (colorBits >> 0 & 1) + (h ? 0x55 : 0);
		
		if ((colorBits & 0xf) == 6)
			g -= 0x55;
		
		return new Color(r, g, b);
	}
	 
	public void buildFontCharacter(int i, int word1, int word2) {
		BufferedImage fontChar = font[i];
		int word = (word1 << 16) | word2;

		for (int col = 0; col < 4; col++) {
			for (int row = 0; row < 8; row++) {
				int color = ((word >> (col * 8 + row)) & 1) == 1 ? Color.WHITE.getRGB() : Color.BLACK.getRGB();
				fontChar.setRGB(col, row, color);
			}
		}
	}
	
	public void loadFont(DCPU cpu) throws IOException {
		BufferedImage img = ImageIO.read(DCPU.class.getResource("/net/ian/dcpu/res/font.png"));	
		BufferedImage img2 = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
		Graphics2D g = img2.createGraphics();
		g.drawImage(img, 0, 0, null);
		g.dispose();
				
		font = new BufferedImage[img2.getWidth() / 4 * (img2.getHeight() / 8)];
		
		for (int x = 0; x < img2.getWidth() / 4; x++) {
			for (int y = 0; y < img2.getHeight() / 8; y++) {
				font[y * 32 + x] = img2.getSubimage(x * 4, y * 8, 4, 8);
			}
		}
		
		// Stick the default font in DCPU memory.
		for (int i = 0; i < font.length; i++) {
			int word = 0;
			for (int x = 0; x < font[i].getWidth(); x++) {
				for (int y = 0; y < font[i].getHeight(); y++) {
					if (font[i].getRGB(x, y) == Color.WHITE.getRGB())
						word |= 1 << (x * 8 + y);
					int word2 = word & 0xffff;
					int word1 = word >> 16;
					cpu.memory[0x8180 + (i * 2)].value = word1;
					cpu.memory[0x8180 + (i * 2) + 1].value = word2;
				}
			}
		}
	}

	private BufferedImage replaceColor(BufferedImage img, Color fgColor, Color bgColor) {
		int fgColorRGB = fgColor.getRGB();
		int bgColorRGB = bgColor.getRGB();
		
		BufferedImage img2 = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img2.createGraphics();
		g.drawImage(img, 0, 0, null);
		g.dispose();
		
		for (int x = 0; x < img2.getWidth(); x++) {
			for (int y = 0; y < img2.getHeight(); y++) {
				int rgb = img2.getRGB(x, y);
				if (rgb == Color.BLACK.getRGB())
					img2.setRGB(x, y, bgColorRGB);
				else
					img2.setRGB(x, y, fgColorRGB);
			}
		}
		return img2;
	}

	public void paint(Graphics gr) {
		Graphics2D g = (Graphics2D)gr; 
		g.setColor(Color.BLACK);
		for (int x = 0; x < WIDTH / 4; x++) {
			for (int y = 0; y < HEIGHT / 8; y++) {
				MonitorCell cell = cells[y * 32 + x];
				g.drawImage(replaceColor(font[cell.character], cell.fgColor, cell.bgColor), scaler, x * 4 * SCALE, y * 8 * SCALE);
			}
		}
	}
}
