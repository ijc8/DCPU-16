package net.ian.dcpu;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.*;

import net.ian.dcpu.DCPU.Register;

public class DCPULauncher {
	DCPU cpu;

	public DCPULauncher() {
		int mem[] = {0x7c01, 0x8000, 0x7c81, 0x0004};
		cpu = new DCPU(mem);
	}

	
	public void launch() {	
        JFrame frame = new JFrame("DCPU-16");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        Monitor monitor = new Monitor();
        monitor.repaint();
        frame.add(monitor);
               
        JPanel panel = new JPanel(new GridLayout(0, 4));
        panel.add(new JLabel("Registers:"));
        panel.add(new JLabel("Bin"));
        panel.add(new JLabel("Hex"));
        panel.add(new JLabel("Dec"));
        
        // 3 for Bin, Hex, and Dec
        JLabel registers[][] = new JLabel[3][];
        for (int i = 0; i < 3; i++)
            registers[i] = new JLabel[Register.values().length];
        for (Register r : Register.values()) {
        	panel.add(new JLabel(r.toString() + ": "));
        	for (int i = 0; i < 3; i++) {
        		registers[i][r.ordinal()] = new JLabel();
        		panel.add(registers[i][r.ordinal()]);
        	}
        }
        
        String specialNames[] = {"SP", "PC", "O"};
        Cell special[] = {cpu.SP, cpu.PC, cpu.O};
        JLabel specialLabels[][] = new JLabel[specialNames.length][];

        for (int i = 0; i < specialNames.length; i++) {
        	specialLabels[i] = new JLabel[3];
        	panel.add(new JLabel(specialNames[i] + ": "));
        	for (int j = 0; j < 3; j++) {
        		specialLabels[i][j] = new JLabel();
        		panel.add(specialLabels[i][j]);
        	}
        }
        
        frame.getContentPane().add(panel, BorderLayout.SOUTH);        
 
        // Display the window.
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        
        cpu.running = true;
        while (cpu.running) {
        	cpu.cycle();
        	
        	for (int i = 0x8000; i < 0x8180; i++) {
        		Color color = Monitor.convertColor(cpu.memory[i].value);
        		if (cpu.memory[i].value != 0)
        			System.out.println(cpu.memory[i]);
        		monitor.colors[i - 0x8000] = color;
        	}
        	monitor.repaint();
        	
        	for (Register r : Register.values()) {
        		int value = cpu.getRegister(r).value;
        		registers[0][r.ordinal()].setText(Integer.toBinaryString(value));
        		registers[1][r.ordinal()].setText("0x" + Integer.toHexString(value));
        		registers[2][r.ordinal()].setText(Integer.toString(value));
        	}
        	
        	for (int i = 0; i < special.length; i++) {
        		int value = special[i].value;
        		specialLabels[i][0].setText(Integer.toBinaryString(value));
        		specialLabels[i][1].setText("0x" + Integer.toHexString(value));
        		specialLabels[i][2].setText(Integer.toString(value));
        	}
        	
        	System.out.println("Next cycle...");
        }
	}
	
	public static void main(String[] args) {
		DCPULauncher launcher = new DCPULauncher();
		launcher.launch();
	}

}
