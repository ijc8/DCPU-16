package net.ian.dcpu;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Map;

import net.ian.dcpu.DCPU.Register;

public class Keyboard extends Hardware implements KeyListener {
	public static final int ID = 0x30cf7406;
	public static final int VERSION = 1;
	public static final int MANUFACTURER = 0xCC_743_CA7;
	
	private static Map<Integer, Integer> keyMap = new HashMap<>();
	private char[] keyring = new char[64];
	private int keyPushPtr = 0;
	private int keyAccessPtr = 0;
	// Max key value seems to be 145.
	private boolean[] keyStates = new boolean[146];
	
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
        super(ID, VERSION, MANUFACTURER);
		
		this.cpu = cpu;
		cpu.attachDevice(this);		
	}
	
	public int mapKey(int key) {
		return keyMap.containsKey(key) ? keyMap.get(key) : -1;
	}
	
	private void addKey(char key) {
		keyring[keyPushPtr++] = key;
		keyPushPtr &= 64;
	}
	
	@Override
	public void keyPressed(KeyEvent e) {
		int key = mapKey(e.getKeyCode());
		if (key == -1) return;
		if (key < 20)
			addKey((char)key);
		keyStates[key] = true;
		System.err.printf("Key press: %d (dcpu: %d)\n", e.getKeyCode(), key);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		int key = mapKey(e.getKeyCode());
		if (key == -1) return;
		keyStates[key] = false;
		System.err.printf("Key release: %d (dcpu: %d)\n", e.getKeyCode(), key);
	}
	
	@Override
	public void keyTyped(KeyEvent e) {
		if (e.getKeyChar() >= 0x20 && e.getKeyChar() <= 0x7f) {
			addKey(e.getKeyChar());
			System.err.printf("Key typed: %d = %c\n", (int)e.getKeyChar(), e.getKeyChar());
		}
	}
	
	public void interrupt() {
		int b = cpu.getRegister(Register.B).value;
		int c = cpu.getRegister(Register.C).value;
		switch (cpu.getRegister(Register.A).value) {
		case 0: // Clear buffer
			for (int i = 0; i < keyring.length; i++)
				keyring[i] = 0;
			keyPushPtr = 0;
			keyAccessPtr = 0;
			break;
		case 1: // Store next key in C (or 0 if the buffer's empty).
			if ((c = keyring[keyAccessPtr]) != 0) {
				keyring[keyAccessPtr++] = 0;
				keyAccessPtr &= 64;
			}
			break;
		case 2: // Set C to 1 if key specified by B is pressed, 0 otherwise.
			c = keyStates[b] ? 1 : 0;
			break;
		case 3: // If B is 0, disable interrupts. Otherwise enable them with message B.
			// TODO!
			break;
		}
		cpu.getRegister(Register.C).set(c);
	}
}
