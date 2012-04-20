package net.ian.dcpu;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Monitor extends Canvas {
	private static final long serialVersionUID = 1L;
	
	public static final int WIDTH = 128;
	public static final int HEIGHT = 96;
	public static final int SCALE = 5;
	
	public BufferedImage font[][];
	
	public Color colors[];
	
	public Monitor() {
        setPreferredSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
        setMinimumSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
        setMaximumSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
        
        colors = new Color[32 * 12];
        
        try {
			loadFont();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static Color convertColor(int colorBits) {
		boolean h = (colorBits & 0x8) == 1;
		int r = 0xAA * (colorBits & 0x4) + (h ? 0x55 : 0);
		int g = 0xAA * (colorBits & 0x2) + (h ? 0x55 : 0);
		int b = 0xAA * (colorBits & 0x1) + (h ? 0x55 : 0);
		return new Color(r, g, b);
	}
	
	public void loadFont() throws IOException {
		BufferedImage img = ImageIO.read(DCPU.class.getResource("/net/ian/dcpu/res/font.png"));
		font = new BufferedImage[img.getWidth() / 4][];
		
		for (int x = 0; x < img.getWidth() / 4; x++) {
			font[x] = new BufferedImage[img.getHeight() / 8];
			for (int y = 0; y < img.getHeight() / 8; y++) {
				font[x][y] = img.getSubimage(x * 4, y * 8, 4, 8);
			}
		}
	}

	public void paint(Graphics g) {
		for (int x = 0; x < WIDTH / 4; x++) {
			for (int y = 0; y < HEIGHT / 8; y++) {
				g.setColor(colors[x * 12 + y]);
				g.fillRect(x * 4 * SCALE, y * 8 * SCALE, 4 * SCALE, 8 * SCALE);
			}
		}
	}
}
