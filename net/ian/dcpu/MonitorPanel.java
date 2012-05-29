package net.ian.dcpu;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

public class MonitorPanel extends JPanel implements MouseListener, Runnable {
	private static final long serialVersionUID = 1L;
	
	public static final int SCALE = 4;
	
	public static final int WIDTH = Monitor.WIDTH * SCALE;
	public static final int HEIGHT = Monitor.HEIGHT * SCALE;
	
	private BufferedImage screen;
		
	private AffineTransformOp scaler;
	
	public Monitor monitor;

	public boolean running = false;
	
	public MonitorPanel(Monitor m) {
		monitor = m;
		
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setMinimumSize(new Dimension(WIDTH, HEIGHT));
        setMaximumSize(new Dimension(WIDTH, HEIGHT));
        
        screen = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        
        AffineTransform scale = new AffineTransform();
        scale.scale(SCALE, SCALE);
        scaler = new AffineTransformOp(scale, null);
        
        
        setFocusable(true);
        addMouseListener(this);
	}

	public void tick() {
		if (monitor.tick())
			render();
	}
	
	public void run() {
		running = true;
		while (running)		
			tick();
	}
	
	public void render() {
		Graphics2D g = screen.createGraphics();
		g.drawImage(monitor.screen, scaler, 0, 0);
		g.dispose();
		
		getGraphics().drawImage(screen, 0, 0, null);
	}
	
	public void paint(Graphics g) {
		g.drawImage(screen, 0, 0, null);
	}

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
