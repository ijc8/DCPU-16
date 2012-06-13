package net.ian.dcpu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

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
	
	public static enum Special { SP, PC, EX }; 
	
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
				if (scanner.hasNext("(?i)" + r.toString())) {
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
	
	static class SpecialArgument extends Argument {
		Special special;
		
		public SpecialArgument(Special s) {
			special = s;
		}
		
		public char getCode() {
			return (char)(special.ordinal() + 0x1b);
		}
		
		public static SpecialArgument read(Scanner scanner) {
			for (Special s : Special.values()) {
				if (scanner.hasNext("(?i)" + s.toString())) {
					scanner.next();
					System.out.printf("Found special: %s\n", s.toString());
					return new SpecialArgument(s);
				}
			}
			
			return null;
		}
		
		public String toString() {
			return special.toString();
		}
	}
	
	static class RegisterDeref extends Argument {
		Register register;
		
		public RegisterDeref(Register r) {
			register = r;
		}
		
		public char getCode() {
			return (char)(0x8 + register.ordinal());
		}
		
		public static RegisterDeref read(Scanner scanner) {
			Pattern backup = scanner.delimiter();
			scanner.useDelimiter("(?<=])");
			if (!scanner.hasNext("(?i)\\s*,?\\s*\\[\\s*([ABCXYZIJ])\\s*,?\\s*\\]")) {
				scanner.useDelimiter(backup);
				return null;
			}
			String register = scanner.match().group(1);
			scanner.next();
			scanner.useDelimiter(backup);
			
			for (Register r : Register.values()) {
				if (r.toString().equalsIgnoreCase(register)) {
					System.out.printf("Found register deref: [%s]\n", register);
					return new RegisterDeref(r);
				}
			}
			
			return null;
		}
		
		public String toString() {
			return "[" + register + "]";
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
			if (!scanner.hasNext("(?i)[+-]?(0[xb])?[\\da-f]+"))
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
			try {
				return new IntArgument(sign * Integer.parseInt(s, radix));
			} catch (NumberFormatException _) {
				// Invalid string for that base
				// For example, 0b1234 or ffff (notice the lack of 0x).
				// Also, ffff is a valid label, so this might not be an error.
				System.out.printf("Error: %d: format error in integer %s in base %s.\n", scanner.match().start(), s, radix);
				return null;
			}
		}
		
		public String toString() {
			return Integer.toString(n);
		}
	}
	
	static class IntReference extends Argument {
		char n;
		
		public IntReference(char n) {
			this.n = n;
		}
		
		public IntReference(int n) {
			this.n = (char)n;
		}
		
		public char getCode() {
			return 0x1e; // [next word]
		}
		
		public int getExtraWord() {
			return n;
		}
		
		public static IntReference read(Scanner scanner) {
			Pattern backup = scanner.delimiter();
			scanner.useDelimiter("(?<=])");
			if (!scanner.hasNext("(?i)\\s*,?\\s*\\[\\s*([+-]?(?:0[xb])?[\\da-f]+)\\s*,?\\s*\\]")) {
				scanner.useDelimiter(backup);
				return null;
			}
			MatchResult m = scanner.match();
			String s = m.group(1);
			scanner.next();
			scanner.useDelimiter(backup);
					
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
			
			System.out.printf("Found integer reference %s in base %d.\n", s, radix);
			try {
				return new IntReference(sign * Integer.parseInt(s, radix));
			} catch (NumberFormatException _) {
				System.out.printf("Error: %d: format error in integer %s in base %s.\n", m.start(1), s, radix);
				return null;
			}
		}
		
		public String toString() {
			return "[" + (int)n + "]";
		}
	}
	
	static class IndirectReference extends Argument {
		Register register;
		char n;
		
		public IndirectReference(Register r, char n) {
			register = r;
			this.n = n;
		}
		
		public IndirectReference(Register r, int n) {
			this(r, (char)n);
		}
		
		public char getCode() {
			return (char)(register.ordinal() + 0x10);
		}
		
		public int getExtraWord() {
			return n;
		}
		
		public static IndirectReference read(Scanner scanner) {
			Pattern backup = scanner.delimiter();
			scanner.useDelimiter("(?<=])");
			// Oh god why
			if (!scanner.hasNext("(?i)\\s*,?\\s*\\[\\s*((?:[+-]?(?:0[xb])?[\\da-f]+)|[ABCXYZIJ])\\s*\\+\\s*((?:[+-]?(?:0[xb])?[\\da-f]+)|[ABCXYZIJ])\\s*,?\\s*\\]")) {
				scanner.useDelimiter(backup);
				return null;
			}
			MatchResult m = scanner.match();
			String thing1 = m.group(1);
			String thing2 = m.group(2);
			scanner.next();
			scanner.useDelimiter(backup);
			
			String register = null, n = null;
			if (thing1.matches("(?i)[ABCXYZIJ]")) {
				register = thing1;
				n = thing2;
				if (!n.matches("(?i)[+-]?(?:0[xb])?[\\da-f]+")) {
					System.out.printf("Error: %d: Expected integer in indirect reference, got %s.\n", m.start(2), n);
					return null;
				}
			} else {
				n = thing1;
				register = thing2;
				if (!register.matches("(?i)[ABCXYZIJ]")) {
					System.out.printf("Error: %d: Expected register in indirected reference, got %s.\n", m.start(2), register);
					return null;
				}
			}
			String original = n;
					
			int sign = n.startsWith("-") ? -1 : 1;
			if (n.startsWith("+") || n.startsWith("-"))
				n = n.substring(1);
			
			int radix = 10;
			if (n.startsWith("0x") || n.startsWith("0X")) {
				radix = 16;
				n = n.substring(2);
			}			
			if (n.startsWith("0b") || n.startsWith("0B")) {
				radix = 2;
				n = n.substring(2);
			}
			
			System.out.printf("Found integer reference %s in base %d.\n", n, radix);
			try {
				return new IndirectReference(stringToRegister(register), sign * Integer.parseInt(n, radix));
			} catch (NumberFormatException _) {
				System.out.printf("Error: %d: format error in integer in indirect reference %s in base %s.\n", m.start(original.equals(thing1) ? 1 : 2), n, radix);
				return null;
			}
		}
		
		public String toString() {
			return "[" + register + "+" + (int)n + "]";
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
			if (isBasic && b.getExtraWord() != -1)
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
			if (scanner.hasNext("(?i)" + op)) {
				System.out.printf("Found basic op: %s\n", op);
				scanner.next();
				Argument b = readArgument();
				Argument a = readArgument();
				return new Operation(op, a, b);
			}
		}
		
		for (String op : specialOps) {
			if (scanner.hasNext("(?i)" + op)) {
				System.out.printf("Found special op: %s\n", op);
				scanner.next();
				Argument a = readArgument();
				return new Operation(op, a);
			}
		}
		return null;
	}
	
	private Argument readArgument() {
		Argument arg;
		if ((arg = RegisterArgument.read(scanner)) != null)
			return arg;
		if ((arg = SpecialArgument.read(scanner)) != null)
			return arg;
		if ((arg = RegisterDeref.read(scanner)) != null)
			return arg;
		if ((arg = IntArgument.read(scanner)) != null)
			return arg;
		if ((arg = IntReference.read(scanner)) != null)
			return arg;
		if ((arg = IndirectReference.read(scanner)) != null)
			return arg;
		String s = scanner.next();
		System.out.printf("Error: %d: Unexpected token: %s\n", scanner.match().start(), s);
		return null;
	}
	
	public static Register stringToRegister(String s) {
		for (Register r : Register.values()) {
			if (r.toString().equalsIgnoreCase(s))
				return r;
		}
		return null;
	}
}
