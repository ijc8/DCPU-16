package net.ian.dcpu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

public class Assembler {
	
	public static final List<String> basicOps = Arrays.asList(
		"SET", "ADD", "SUB", "MUL", "MLI", "DIV", "DVI", "MOD",
		"MDI", "AND", "BOR", "XOR", "SHR", "ASR", "SHL", "IFB",
		"IFC", "IFE", "IFN", "IFG", "IFA", "IFL", "IFU", "   ",
		"   ", "ADX", "SBX", "   ", "   ", "STI", "STD"
	);
	
	public static final List<String> specialOps = Arrays.asList(
			"JSR", "   ", "   ", "   ", "   ", "   ", "HCF", "INT",
			"IAG", "IAS", "RFI", "IAQ", "   ", "   ", "   ", "HWN",
			"HWQ", "HWI", "   ", "   ", "   ", "   ", "   ", "   ",
			"   ", "   ", "   ", "   ", "   ", "   ", "   "
	);
	
	abstract static class Argument {
		public abstract char getCode();
		
		public int getExtraWord() {
			return -1;
		}
		
		public static Argument read(Scanner scanner) {
			return null;
		}
	}
	
	static class RegisterArgument extends Argument {
		Register register;
		
		public RegisterArgument(Register r) {
			register = r;
		}
		
		public char getCode() {
			return (char)register.ordinal();
		}
		
		public static RegisterArgument read(Scanner scanner) {
			for (Register r : Register.values()) {
				if (scanner.hasNext(r.toString()) || scanner.hasNext(r.toString().toLowerCase())) {
					scanner.next();
					System.out.printf("Found register: %s\n", r.toString());
					return new RegisterArgument(r);
				}
			}
			
			return null;
		}
		
		public String toString() {
			return register.toString();
		}
	}
	
	static class IntArgument extends Argument {
		char n;
		
		public IntArgument(char n) {
			this.n = n;
		}
		
		public IntArgument(int n) {
			this.n = (char)n;
		}
		
		public char getCode() {
			if ((short)n >= -1 && (short)n <= 30)
				return (char)(0x21 + n); // Max is 63, which is the largest integer that fits in 6 bits.
			return 0x1f; // Next word (literal).
		}
		
		public int getExtraWord() {
			if ((short)n >= -1 && (short)n <= 30)
				return -1;
			else
				return n;
		}
		
		public static IntArgument read(Scanner scanner) {
			if (!scanner.hasNext("[+-]?(0[XxBb])?\\d+"))
				return null;
			
			String s = scanner.next();
			
			int sign = s.startsWith("-") ? -1 : 1;
			if (s.startsWith("+") || s.startsWith("-"))
				s = s.substring(1);
			
			int radix = 10;
			if (s.startsWith("0x") || s.startsWith("0X")) {
				radix = 16;
				s = s.substring(2);
			}			
			if (s.startsWith("0b") || s.startsWith("0B")) {
				radix = 2;
				s = s.substring(2);
			}
			
			System.out.printf("Found integer %s in base %d.\n", s, radix);
			return new IntArgument(sign * Integer.parseInt(s, radix));
		}
		
		public String toString() {
			return Integer.toString(n);
		}
	}
	
	abstract class Statement {
		public abstract List<Character> compile();
	}
	
	class Operation extends Statement {
		String op;
		Argument a, b;
		boolean isBasic;
		
		public Operation(String op, Argument a, Argument b) {
			this.op = op;
			this.a = a;
			this.b = b;
			isBasic = (b != null);
		}
		
		public Operation(String op, Argument a) {
			this.op = op;
			this.a = a;
			isBasic = false;
		}
		
		private int binPad(int value) {
			return Integer.parseInt(Integer.toBinaryString(value));
		}
		
		public List<Character> compile() {
			List<Character> result = new ArrayList<>();
			int opcode = (isBasic ? basicOps : specialOps).indexOf(op) + 1;
			
			String format = String.format("%06d%05d%05d", binPad(a.getCode()), binPad(isBasic ? b.getCode() : opcode), binPad(isBasic ? opcode : 0));
			System.out.printf("Compiled instruction %s %s, %s to %s.\n", op, b, a, format);
			
			result.add((char)Integer.parseInt(format, 2));
			if (a.getExtraWord() != -1)
				result.add((char)a.getExtraWord());
			if (b.getExtraWord() != -1)
				result.add((char)b.getExtraWord());
			
			return result;
		}
	}
	
	private Scanner scanner;
	
	public List<Character> assemble(String input) {
		scanner = new Scanner(input);
		scanner.useDelimiter("\\s*(\\s|,)\\s*");
		List<Character> output = new ArrayList<>();
		
		while (scanner.hasNext()) {
			output.addAll(readStatement().compile());
		}
		
		return output;
	}

	private Statement readStatement() {
		for (String op : basicOps) {
			if (scanner.hasNext(op) || scanner.hasNext(op.toLowerCase())) {
				System.out.printf("Found op: %s\n", op);
				scanner.next();
				Argument b = readArgument();
				Argument a = readArgument();
				return new Operation(op, a, b);
			}
		}
		return null;
	}
	
	private Argument readArgument() {
		Argument arg;
		if ((arg = RegisterArgument.read(scanner)) != null)
			return arg;
		if ((arg = IntArgument.read(scanner)) != null)
			return arg;
		return null;
	}
}
