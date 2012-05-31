package net.ian.dcpu;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Map;

public class Keyboard extends Hardware implements KeyListener {
	public static final int ID = 0x30cf7406;
	public static final int VERSION = 1;
	
	private static Map<Integer, Integer> keyMap = new HashMap<>();
	
	DCPU cpu;
	
	static {
		keyMap.put(  8, 0x10); // BACKSPACE
		keyMap.put( 10, 0x11); // RETURN
		keyMap.put(155, 0x12); // INSERT
		keyMap.put(127, 0x13); // DELETE
		keyMap.put( 38, 0x80); // ARROW UP
		keyMap.put( 40, 0x81); // ARROW DOWN
		keyMap.put( 37, 0x82); // ARROW LEFT
		keyMap.put( 39, 0x83); // ARROW RIGHT
		keyMap.put( 16, 0x90); // SHIFT
		keyMap.put( 17, 0x91); // CONTROL
	}
	
	public Keyboard(DCPU cpu) {
        super(ID, VERSION, 0xB65100);
		
		this.cpu = cpu;
		cpu.attachDevice(this);		
	}
	
	public int mapKey(int key) {
		return keyMap.containsKey(key) ? keyMap.get(key) : -1;
	}
	
	private void addKey(int key) {
		int tmp = cpu.memory[0x9010].value - 0x9000 + 1;
		cpu.memory[0x9010].value = (char)(((tmp < 0 ? 0 : tmp) % 0xf) + 0x9000);
		cpu.memory[cpu.memory[0x9010].value].value = (char)key;
	}
	
	@Override
	public void keyPressed(KeyEvent e) {
		int key = mapKey(e.getKeyCode());
		if (key > 0)
			addKey(key);
		System.err.printf("Key press: %d (dcpu: %d)\n", e.getKeyCode(), key);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		int key = mapKey(e.getKeyCode());
		System.err.printf("Key release: %d (dcpu: %d)\n", e.getKeyCode(), key);
	}
	
	@Override
	public void keyTyped(KeyEvent e) {
		if (e.getKeyChar() >= 0x20 && e.getKeyChar() <= 0x7f) {
			addKey(e.getKeyChar());
			System.err.printf("Key typed: %d = %c\n", (int)e.getKeyChar(), e.getKeyChar());
		}
	}
}
