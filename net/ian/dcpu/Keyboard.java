package net.ian.dcpu;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Keyboard implements KeyListener {
	DCPU cpu;
	
	public Keyboard(DCPU cpu) {
		this.cpu = cpu;
	}
	
	@Override
	public void keyPressed(KeyEvent e) {
		int tmp = cpu.memory[0x9010].value - 0x9000 + 1;
		cpu.memory[0x9010].value = ((tmp < 0 ? 0 : tmp) % 0xf) + 0x9000;
		cpu.memory[cpu.memory[0x9010].value].value = e.getKeyCode();
		System.out.println(e.getKeyCode());
	}

	@Override
	public void keyReleased(KeyEvent arg0) {}
	public void keyTyped(KeyEvent arg0) {}
}
