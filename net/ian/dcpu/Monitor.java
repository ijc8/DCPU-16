package net.ian.dcpu;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class Monitor extends JPanel implements Hardware, MouseListener, Runnable {
	private static final long serialVersionUID = 1L;
	
	public static final int COLUMNS = 32;
	public static final int ROWS = 12;
	
	public static final int CHAR_WIDTH = 4;
	public static final int CHAR_HEIGHT = 8;
	
	public static final int BORDER = 12;
	public static final int SCALE = 4;
	
	public static final int WIDTH = COLUMNS * CHAR_WIDTH * SCALE + BORDER * SCALE * 2;
	public static final int HEIGHT = ROWS * CHAR_HEIGHT * SCALE + BORDER * SCALE * 2;
	
	private BufferedImage screen;
	
	public BufferedImage font[];
	public MonitorCell cells[];
	
	public Color borderColor = Color.BLACK;
	
	private AffineTransformOp scaler;
	
	DCPU cpu;

	private boolean shouldRender;
	
	public static class MonitorCell {
		char character;
		Color fgColor, bgColor;
		boolean blink, show;
		
		public MonitorCell(char c, Color fg, Color bg, boolean blink) {
			character = c;
			fgColor = fg;
			bgColor = bg;
			this.blink = blink;
			show = false;
		}
	}
	
	public Monitor(DCPU cpu) {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setMinimumSize(new Dimension(WIDTH, HEIGHT));
        setMaximumSize(new Dimension(WIDTH, HEIGHT));
        
        screen = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        this.cpu = cpu;
        
        cells = new MonitorCell[32 * 12];
        for (int i = 0; i < 32 * 12; i++)
        	cells[i] = new MonitorCell((char)0, Color.BLACK, Color.BLACK, false);
        
        try {
			loadFont(cpu);
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        cpu.attachDevice(this, (char)0x8000, (char)0x280);
        
        AffineTransform scale = new AffineTransform();
        scale.scale(SCALE, SCALE);
        scaler = new AffineTransformOp(scale, null);
        
        setFocusable(true);
        addMouseListener(this);
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
	
	public void buildFont(int location, char word) {
		// Actually builds half a font, because a full font takes two words.
		BufferedImage fontChar = font[location / 2];
		int half = location % 2;
		
		for (int col = 0; col < 2; col++) {
			for (int row = 0; row < 8; row++) {
				int color = ((word >> ((1 - col) * 8 + row)) & 1) == 1 ? Color.WHITE.getRGB() : Color.BLACK.getRGB();
				fontChar.setRGB(col + (half * 2), row, color);
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
						word |= 1 << ((3 - x) * 8 + y);
					char word2 = (char)(word & 0xffff);
					char word1 = (char)(word >> 16);
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
	
	public void run() {
		while (cpu.running) {
			if (System.currentTimeMillis() % 100 == 0) {
				for (int x = 0; x < COLUMNS; x++) {
					for (int y = 0; y < ROWS; y++) {
						MonitorCell cell = cells[y * 32 + x];
						if (cell.blink) {
							shouldRender = true;
							cell.show = !cell.show;
						}
					}
				}
			}
			
			if (shouldRender) {
				render();
				shouldRender = false;
			}
		}
	}
	
	public void render() {
		Graphics2D g = screen.createGraphics();
		
		g.setColor(borderColor);
		g.fillRect(0, 0, WIDTH, HEIGHT);
		for (int x = 0; x < COLUMNS; x++) {
			for (int y = 0; y < ROWS; y++) {
				MonitorCell cell = cells[y * 32 + x];
				if (!cell.show) {
					g.setColor(Color.BLACK);
					g.fillRect(x * 4 * SCALE + BORDER * SCALE, y * 8 * SCALE + BORDER * SCALE, 4 * SCALE, 8 * SCALE);
				} else if (cell.fgColor.equals(cell.bgColor)) {
					g.setColor(cell.bgColor);
					g.fillRect(x * 4 * SCALE + BORDER * SCALE, y * 8 * SCALE + BORDER * SCALE, 4 * SCALE, 8 * SCALE);
				} else
					g.drawImage(replaceColor(font[cell.character], cell.fgColor, cell.bgColor), scaler, x * 4 * SCALE + BORDER * SCALE, y * 8 * SCALE + BORDER * SCALE);
			}
		}
		
		g.dispose();
		getGraphics().drawImage(screen, 0, 0, null);
	}
	
	public void paint(Graphics g) {
		g.drawImage(screen, 0, 0, null);
	}

	@Override
	public void onSet(char location, char value) {
		if (location < 0x8180) {
    		MonitorCell cell = cells[location - 0x8000];
    		cell.character = (char)(value & 127);
    		cell.fgColor = Monitor.convertColor(value >> 12);
    		cell.bgColor = Monitor.convertColor(value >> 8);
    		cell.blink = (value >> 7 & 1) == 1;
    		cell.show = true;
		} else if (location < 0x8280) {
			// BUILD HALF A FONT
			buildFont(location - 0x8180, value);
		} else if (location == 0x8280)
			borderColor = convertColor(value);
		shouldRender = true;
	}

	@Override
	public void onGet(char location, char value) {}

	@Override
	public void mouseClicked(MouseEvent e) {
		requestFocus();
	}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}
}
