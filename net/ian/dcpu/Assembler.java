package net.ian.dcpu;

import java.util.Arrays;

public class Assembler {
	public static final String[] basic = { "SET", "ADD", "SUB", "MUL", "DIV", "MOD", "SHL", "SHR", "AND", "BOR", "XOR", "IFE", "IFN", "IFG", "IFB" };
	public static final String[] special = { "EXIT", "JSR" };
	public static final String[] registers;
	
	static {
		DCPU.Register regs[] = DCPU.Register.values();
		registers = new String[regs.length];
		for (DCPU.Register r : regs)
			registers[r.ordinal()] = r.toString();
	}
	
	// This here is a poor man's assembler.
	public static int assemble(String sOp, String sArg1, String sArg2) {
		boolean isBasic = (sArg2 != null);
		int op = Arrays.asList(isBasic ? basic : special).indexOf(sOp) + 1;
		int arg1 = handleArgument(sArg1);
		int arg2 = -1;
		if (isBasic)
			arg2 = handleArgument(sArg2);
		return compile(op, arg1, arg2);
	}
	
	private static int handleArgument(String arg) {
		int index;
		if ((index = Arrays.asList(registers).indexOf(arg)) != -1)
			return index;
		try {
			int n = Integer.parseInt(arg);
			if (n < 31)
				return n + 0x20;
			// TODO: Assemble instructions requiring multiple words.
			else
				return 0x1f;
		} catch (NumberFormatException _) {
			// Whelp, it wasn't a number.
		}
		if (arg.startsWith("[")) {
			if (!arg.endsWith("]"))
				return -1;
			arg = arg.substring(1, arg.length() - 1);
			if ((index = Arrays.asList(registers).indexOf(arg)) != -1)
				return index + 0x8;
		}
		return 0;
	}

	public static int assemble(String op, String arg) {
		return assemble(op, arg, null);
	}
	
	// Changes arguments into machine code. This is nice because arguments are 6 bits.
	public static int compile(int op, int arg1, int arg2) {
		boolean isBasic = (arg2 != -1);
		int opLength = isBasic ? 4 : 6;
		String sOp = String.format("%0" + opLength + "d", Integer.parseInt(Integer.toBinaryString(op)));
		String sArg1 = String.format("%06d", Integer.parseInt(Integer.toBinaryString(arg1)));
		String sArg2 = isBasic ? String.format("%06d", Integer.parseInt(Integer.toBinaryString(arg2))) : "0000";
		return Integer.parseInt(sArg2 + sArg1 + sOp, 2);
	}
	
	public static int compile(int op, int arg) {
		return compile(op, arg, -1);
	}
}
