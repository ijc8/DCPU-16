package net.ian.dcpu;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;

import net.ian.dcpu.DCPU.Register;

public class DCPULauncher extends JPanel implements ActionListener, Runnable {
	private static final long serialVersionUID = 1L;
	
	DCPU cpu;
	Keyboard keyboard;
	Monitor monitor;
	MonitorPanel display;
	
	Assembler assembler;
	
	JTextArea codeEntry;
	
	JLabel[][] registers;
    Cell special[];
	JLabel[][] specialLabels;
	JLabel instructionLabel;

	private boolean started;
	
	public DCPULauncher() {
		super();
		cpu = new DCPU();
		keyboard = new Keyboard(cpu);
		monitor = new Monitor(cpu);
		
		assembler = new Assembler();
	}
	
	public void init() {	
        JFrame frame = new JFrame("DCPU-16");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        frame.setContentPane(this);
        
        JPanel output = new JPanel(new BorderLayout()); 
        
        codeEntry = new JTextArea(0, 40);
        codeEntry.setFont(new Font("Monospaced", Font.BOLD, 16));
        JScrollPane codeScroll = new JScrollPane(codeEntry);
        frame.add(codeScroll);
       
        display = new MonitorPanel(monitor);
        display.addKeyListener(keyboard);
        output.add(display, BorderLayout.NORTH);
        
        JPanel buttonBox = new JPanel(new GridLayout(1, 0));
        
        JButton runButton = new JButton("Run");
        runButton.setActionCommand("run");
        runButton.addActionListener(this);
        buttonBox.add(runButton);
        
        JButton stepButton = new JButton("Step");
        stepButton.setActionCommand("step");
        stepButton.addActionListener(this);
        buttonBox.add(stepButton);
        
        JButton stopButton = new JButton("Stop");
        stopButton.setActionCommand("stop");
        stopButton.addActionListener(this);
        buttonBox.add(stopButton);
        
        output.add(buttonBox, BorderLayout.CENTER);
               
        JPanel panel = new JPanel(new GridLayout(0, 4));
        panel.add(new JLabel("Registers"));
        panel.add(new JLabel("Bin"));
        panel.add(new JLabel("Hex"));
        panel.add(new JLabel("Dec"));
        
        // 3 for Bin, Hex, and Dec
        registers = new JLabel[Register.values().length][];
        for (Register r : Register.values()) {
        	registers[r.ordinal()] = new JLabel[3];
        	panel.add(new JLabel(r.toString() + ": "));
        	for (int i = 0; i < 3; i++) {
        		registers[r.ordinal()][i] = new JLabel();
        		panel.add(registers[r.ordinal()][i]);
        	}
        }
        
        String specialNames[] = {"SP", "PC", "EX"};
        special = new Cell[3];
        special[0] = cpu.SP;
        special[1] = cpu.PC;
        special[2] = cpu.EX;
        specialLabels = new JLabel[specialNames.length][];

        for (int i = 0; i < specialNames.length; i++) {
        	specialLabels[i] = new JLabel[3];
        	panel.add(new JLabel(specialNames[i] + ": "));
        	for (int j = 0; j < 3; j++) {
        		specialLabels[i][j] = new JLabel();
        		panel.add(specialLabels[i][j]);
        	}
        }
        
        panel.add(new JLabel("Instruction:"));
        instructionLabel = new JLabel();
        panel.add(new JLabel("..."));
        panel.add(new JLabel("..."));
        panel.add(instructionLabel);
        
        output.add(panel, BorderLayout.SOUTH);
        
        frame.add(output);
 
        // Display the window.
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
	
	@Override
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equals("run")) {
			cpu.clear(assembler.assemble(codeEntry.getText()));
			cpu.labels = reverseLabels();
			
			new Thread(cpu).start();
			new Thread(this).start();
		} else if (command.equals("step")) {
			if (!started) {
				cpu.clear(assembler.assemble(codeEntry.getText()));
				cpu.labels = reverseLabels();
				
				monitor = new Monitor(cpu);
				display.monitor = monitor;
				
				started = true;
				cpu.running = true;
			}
			cpu.cycle();
			display.tick();
			tick();
		} else if (command.equals("stop"))
			cpu.running = false;
	}
	
	private Map<Integer, String> reverseLabels() {
		Map<String, Integer> labels = assembler.labels;
		Map<Integer, String> reversed = new HashMap<>();
		for (Map.Entry<String, Integer> pair : labels.entrySet())
			reversed.put(pair.getValue(), pair.getKey());
		return reversed;
	}
	
	public void run() {
		started = true;
		while (cpu.running) {
			display.tick();
			tick();
		}
		display.tick();
		tick();
	}

	public void tick() {
    	for (Register r : Register.values())
    		setLabels(registers[r.ordinal()], cpu.getRegister(r).value);
    	
    	for (int i = 0; i < special.length; i++)
    		setLabels(specialLabels[i], special[i].value);
    	
    	instructionLabel.setText(Integer.toString(cpu.instructionCount));
	}
	
	private void setLabels(JLabel[] labels, int value) {
    	labels[0].setText(Integer.toBinaryString(value));
    	labels[1].setText("0x" + Integer.toHexString(value));
    	labels[2].setText(Integer.toString(value));
	}
	
	public static void main(String[] args) {
		DCPULauncher launcher = new DCPULauncher();
		launcher.init();
	}
}
