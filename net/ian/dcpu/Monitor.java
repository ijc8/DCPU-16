package net.ian.dcpu;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
	
	public static class MonitorCell {
		boolean show = false;
		char character;
		Color bgColor;
		
		public MonitorCell(char c, Color bg) {
			character = c;
			bgColor = bg;
		}
	}
	
	public Monitor() {
        setPreferredSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
        setMinimumSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
        setMaximumSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
        
        cells = new MonitorCell[32 * 12];
        for (int i = 0; i < 32 * 12; i++)
        	cells[i] = new MonitorCell((char)0, Color.BLACK);
        
        try {
			loadFont();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static Color convertColor(int colorBits) {
		boolean h = (colorBits >> 3 & 1) == 1;
		if (colorBits != 0)
			System.out.printf("color bits: %d (%s)\n", colorBits, Integer.toBinaryString(colorBits));
		int r = 0xAA * (colorBits >> 2 & 1) + (h ? 0x55 : 0);
		int g = 0xAA * (colorBits >> 1 & 1) + (h ? 0x55 : 0);
		int b = 0xAA * (colorBits >> 0 & 1) + (h ? 0x55 : 0);
		if (colorBits != 0)
			System.out.printf("h (%d), r (%d), g (%d), b (%d)\n", h ? 1 : 0, r, g, b);
		return new Color(r, g, b);
	}
	
	public void loadFont() throws IOException {
		BufferedImage img = ImageIO.read(DCPU.class.getResource("/net/ian/dcpu/res/font.png"));
		BufferedImage img2 = new BufferedImage(img.getWidth() * SCALE, img.getHeight() * SCALE, BufferedImage.TYPE_BYTE_BINARY);
		Graphics2D g = img2.createGraphics();
		g.scale(SCALE, SCALE);
		g.drawImage(img, 0, 0, null);
		g.dispose();
		
		System.out.printf("w: %d, h: %d\n", img2.getWidth(), img2.getHeight());
		
		//BufferedImage img3 = (BufferedImage)img.getScaledInstance(img.getWidth() * SCALE, img.getHeight() * SCALE, Image.SCALE_DEFAULT);
		font = new BufferedImage[img2.getWidth() / 4 * (img2.getHeight() / 8)];
		
		for (int x = 0; x < img.getWidth() / 4; x++) {
			for (int y = 0; y < img.getHeight() / 8; y++) {
				font[y * 32 + x] = img2.getSubimage(x * 4 * SCALE, y * 8 * SCALE, 4 * SCALE, 8 * SCALE);
			}
		}
	}

	public void paint(Graphics g) {
		g.setColor(Color.BLACK);
		for (int x = 0; x < WIDTH / 4; x++) {
			for (int y = 0; y < HEIGHT / 8; y++) {
				MonitorCell cell = cells[y * 32 + x];
				if (cell.show)
					g.drawImage(font[cell.character], x * 4 * SCALE, y * 8 * SCALE, cell.bgColor, null);
				else
					g.fillRect(x * 4 * SCALE, y * 8 * SCALE, 4 * SCALE, 8 * SCALE);
			}
		}
	}
}
