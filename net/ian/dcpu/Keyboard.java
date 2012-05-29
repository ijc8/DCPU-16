package net.ian.dcpu;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Keyboard extends Hardware implements KeyListener {
	DCPU cpu;
	
	public Keyboard(DCPU cpu) {
		this.cpu = cpu;
	}
	
	private void addKey(int key) {
		int tmp = cpu.memory[0x9010].value - 0x9000 + 1;
		cpu.memory[0x9010].value = (char)(((tmp < 0 ? 0 : tmp) % 0xf) + 0x9000);
		cpu.memory[cpu.memory[0x9010].value].value = (char)key;
	}
	
	@Override
	public void keyPressed(KeyEvent e) {
		if (e.isActionKey()) {
			addKey(e.getKeyCode());
			System.out.printf("Key press: %d = <ACTION>\n", e.getKeyCode());
		}
	}

	@Override
	public void keyReleased(KeyEvent arg0) {}
	public void keyTyped(KeyEvent e) {
		if (e.getKeyChar() >= 0x20 && e.getKeyChar() < 0x7f) {
			addKey(e.getKeyChar());
			System.out.printf("Key press: %d = %c\n", e.getKeyChar(), (char)e.getKeyChar());
		}
	}
}
