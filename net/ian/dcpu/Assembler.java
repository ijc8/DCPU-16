package net.ian.dcpu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Assembler {
	public static final String[] basicOps = { "SET", "ADD", "SUB", "MUL", "DIV", "MOD", "SHL", "SHR", "AND", "BOR", "XOR", "IFE", "IFN", "IFG", "IFB" };
	public static final String[] specialOps = { "EXIT", "JSR" };
	public static final String[] registers;
	public static final String[] special = { "SP", "PC", "O" };

	public ArrayList<Integer> instructions;
	
	public Map<String, Integer> labels;
	public Map<Integer, Instruction> fixes;
		
	static {
		DCPU.Register regs[] = DCPU.Register.values();
		registers = new String[regs.length];
		for (DCPU.Register r : regs)
			registers[r.ordinal()] = r.toString();
	}
	
	private class Instruction {
		public String op, argA, argB;
		
		// I miss tuples.
		public Instruction(String op, String argA, String argB) {
			this.op = op;
			this.argA = argA;
			this.argB = argB;
		}
	}
	
	public List<Integer> assemble(String code) {
		instructions = new ArrayList<Integer>();
		labels = new HashMap<String, Integer>();
		fixes = new HashMap<Integer, Instruction>();
		
		String[] lines = code.trim().split("\\s*\n\\s*");
		for (String line : lines) {
			// Comments
			if (line.startsWith(";"))
				continue;
			
			String[] tokens = line.split(",?\\s+");
			
			// Labels
			if (line.startsWith(":")) {
				String label = tokens[0].substring(1).toUpperCase();
				if (labels.containsKey(label))
					System.out.println("ERROR: Label " + label + " defined twice!");
				else
					labels.put(label, instructions.size());
				tokens = Arrays.copyOfRange(tokens, 1, tokens.length);
			}
			
			if (tokens.length < 2)
				continue;
			
			String op = tokens[0];
			String arg1 = tokens[1];
			String arg2 = null;
			if (tokens.length > 2)
				arg2 = tokens[2];
			List<Integer> assembled = assemble(op, arg1, arg2);
			if (assembled == null)
				instructions.add(-1);
			else
				instructions.addAll(assemble(op, arg1, arg2));
		}
		
		insertLabels();
		
		System.out.println(labels);
		return instructions;
	}
	
	// This inserts labels in parts of the program where they were used before they existed,
	// via the "fixes" map.
	private void insertLabels() {
		for (Map.Entry<Integer, Instruction> entry : fixes.entrySet()) {
			System.out.printf("Hello. %d, %s\n", entry.getKey(), entry.getValue());
			Instruction ins = entry.getValue();
			List<Integer> eval = assemble(ins.op, ins.argA, ins.argB);
			if (eval != null) {
				int index = entry.getKey();
				instructions.remove(index);
				instructions.addAll(index, eval);
			} else
				System.out.printf("Error: True assembly error (in insertLabels): %s %s %s\n", ins.op, ins.argA, ins.argB);
		}
	}

	public List<Integer> assemble(String sOp, String sArg1, String sArg2) {
		boolean isBasic = (sArg2 != null);
		int op = Arrays.asList(isBasic ? basicOps : specialOps).indexOf(sOp.toUpperCase()) + (isBasic ? 1 : 0);
		int a, b = -1;
		List<Integer> argsA = handleArgument(sArg1.toUpperCase());
		
		if (argsA == null) {
			fixes.put(instructions.size(), new Instruction(sOp, sArg1, sArg2));
			return null;
		}
		
		a = argsA.get(0);
			
		List<Integer> argsB = null;
		if (isBasic) {
			argsB = handleArgument(sArg2.toUpperCase());
			if (argsB == null) {
				fixes.put(instructions.size(), new Instruction(sOp, sArg1, sArg2));
				return null;
			}
			b = argsB.get(0);
		}
		
		ArrayList<Integer> words = new ArrayList<Integer>();
		words.add(compile(op, a, b));
		words.addAll(argsA.subList(1, argsA.size()));
		if (argsB != null)
			words.addAll(argsB.subList(1, argsB.size()));
				
		return words;
	}
	
	public List<Integer> assemble(String op, String arg) {
		return assemble(op, arg, null);
	}
	
	private static List<Integer> single(int n) {
		List<Integer> s = Arrays.asList(n);
		return s;
	}
	
	private static List<Integer> pair(int a, int b) {
		List<Integer> p = Arrays.asList(a, b);
		return p;
	}
	
	private static int parseInt(String s) {
		try {
			int n = Integer.parseInt(s);
			return n;
		} catch (NumberFormatException _) {
			// Whelp, it wasn't a decimal number.
		}
		if (s.toLowerCase().startsWith("0x")) {
			try {
				int n = Integer.parseInt(s.substring(2), 16);
				return n;
			} catch (NumberFormatException _) {
				// Whelp, it wasn't a hexadecimal number.				
			}
		}
		return -1;
	}
	 
	private List<Integer> handleArgument(String arg) {
		int index = Arrays.asList(registers).indexOf(arg);
		if (index != -1)
			return single(index);
		
		if ((index = Arrays.asList(special).indexOf(arg)) != -1)
			return single(index + 0x1b);
		
		if (labels.containsKey(arg)) {
			int loc = labels.get(arg);
			if (loc < 31)
				return single(loc + 0x20);
			return pair(0x1f, loc);
		}
		
		int n = parseInt(arg);
		if (n != -1) {
			if (n < 31)
				return single(n + 0x20);
			return pair(0x1f, n);
		}
		
		if (arg.startsWith("[")) {
			if (!arg.endsWith("]"))
				return single(-1);
			arg = arg.substring(1, arg.length() - 1);
			if ((index = Arrays.asList(registers).indexOf(arg)) != -1)
				return single(index + 0x8);
			if ((n = parseInt(arg)) != -1)
				return pair(0x1e, n);
			if (labels.containsKey(arg))
				return pair(0x1e, labels.get(arg));
		}
		
		System.out.println("Error: Invalid Argument (assembly): " + arg + " (maybe a label?)");
		return null;
	}
	
	// Changes arguments into machine code. This is nice because arguments are 6 bits.
	public static int compile(int op, int arg1, int arg2) {
		boolean isBasic = (arg2 != -1);
		int opLength = isBasic ? 4 : 6;
		
		String sOp = String.format("%0" + opLength + "d", Integer.parseInt(Integer.toBinaryString(op))) + (isBasic ? "" : "0000");
		String sArg1 = String.format("%06d", Integer.parseInt(Integer.toBinaryString(arg1)));
		String sArg2 = isBasic ? String.format("%06d", Integer.parseInt(Integer.toBinaryString(arg2))) : "";
		
		return Integer.parseInt(sArg2 + sArg1 + sOp, 2);
	}
	
	public static int compile(int op, int arg) {
		return compile(op, arg, -1);
	}
}
