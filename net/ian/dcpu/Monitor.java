package net.ian.dcpu;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

public class Monitor extends Canvas {
	private static final long serialVersionUID = 1L;
	
	public static final int WIDTH = 128;
	public static final int HEIGHT = 96;
	public static final int SCALE = 4;
	
	public Monitor() {
        setPreferredSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
        setMinimumSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
        setMaximumSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
	}

	public void paint(Graphics g) {
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, WIDTH * SCALE, HEIGHT * SCALE);
	}
}
