package net.ian.dcpu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class DCPU {
	public Cell[] register;
	public MemoryCell[] memory;
	public Cell SP, PC, EX;
	
	public boolean running = false;
	private boolean skipping = false;
	
	public int instructionCount = 0;
	
	public static final boolean debug = false;
	
	public Map<Integer, String> labels;
	
	public enum Register { A, B, C, X, Y, Z, I, J };
	
	public List<Hardware> devices;
	
	public DCPU() {
		this(new char[0]);
	}

	public DCPU(char[] mem) {
		register = new Cell[11];
		for (int i = 0; i < 11; i++)
			register[i] = new Cell(0);
		memory = new MemoryCell[0x10000]; // 0x10000 words in size
		for (int i = 0; i < 0x10000; i++)
			memory[i] = new MemoryCell(this, (char)i, i < mem.length ? mem[i] : 0);

		SP = new Cell(0);
		PC = new Cell(0);
		EX = new Cell(0);
		
		devices = new ArrayList<Hardware>();
	}
	
	public DCPU(List<Character> mem) {
		this(unboxArray(mem));
	}
	
	public void setMemory(List<Character> listMem) {
		char[] mem = unboxArray(listMem);
		for (int i = 0; i < mem.length; i++)
			memory[i].value = mem[i];
	}
	
	// Bleh.
	private static char[] unboxArray(List<Character> mem) {
		Character[] chars = mem.toArray(new Character[0]);
		char[] ints = new char[chars.length];
		for (int i = 0; i < ints.length; i++)
			ints[i] = (char)chars[i];
		return ints;
	}
	
	private void debug(Object o) {
		if (debug)
			System.out.print(o);
	}
	
	private void debugln(Object o) {
		if (debug)
			System.out.println(o);
	}
	
	private void debugf(String s, Object... o) {
		if (debug)
			System.out.printf(s, o);
	}
	
	public void attachDevice(Hardware h) {
		devices.add(h);
	}
	public Cell getRegister(Register r) {
		return register[r.ordinal()];
	}
	
	private Cell handleArgument(int code, boolean isA) {
		debugf("0x%s: ", Integer.toHexString(code));
		if (code >= 0x0 && code <= 0x7) {
			debug(Register.values()[code]);
			return register[code];
		} else if (code >= 0x8 && code <= 0xf) {
			debugf("[%s]", Register.values()[code - 0x8]);
			return memory[register[code - 0x8].value];
		} else if (code >= 0x10 && code <= 0x17) {
			debugf("[next word + %s]", Register.values()[code - 0x10]);
			return memory[memory[PC.value++].value + register[code - 0x10].value];
		} else if (code == 0x18) {
			debug(isA ? "POP" : "PUSH");
			return isA ? memory[SP.value++] : memory[--SP.value];
		} else if (code == 0x19) {
			debug("PEEK");
			return memory[SP.value];
		} else if (code == 0x1a) {
			debug("PICK " + memory[PC.value].value);
			return memory[SP.value + memory[PC.value++].value];
		} else if (code == 0x1b) {
			debug("SP");
			return SP;
		} else if (code == 0x1c) {
			debug("PC");
			return PC;
		} else if (code == 0x1d) {
			debug("EX");
			return EX;
		} else if (code == 0x1e) {
			debug("[next word]");
			return memory[memory[PC.value++].value];
		} else if (code == 0x1f) {
			debug("next word (literal)");
			return new Cell(memory[PC.value++].value);
		}
		// Only should happen if argument is A.
		debug("literal: " + (code - 0x21));
		return new Cell(code - 0x21);
	}
	
	private void processBasic(int opcode, Cell cellA, Cell cellB) {
		int a = cellA.value;
		int b = cellB.value;
		int ex = 0;
		
		if ((opcode - 1) < Assembler.basicOps.length)
			debugln(Assembler.basicOps[opcode - 1]);
		
		switch (opcode) {
		case 0x1: // SET - sets b to a
			b = a;
			break;
		case 0x2: // ADD - add a to b
			ex = (b += a) > 0xffff ? 1 : 0;
			break;
		case 0x3: // SUB - subtract from b
			ex = (b -= a) < 0 ? 0xffff : 0;
			break;
		case 0x4: // MUL - multiplies b by a
			ex = (b *= a) >> 16 & 0xffff;
			break;
		case 0x5: // MLI - multiplies signed values
			b = (short)a * (short)b;
			ex = b >> 16 & 0xffff;
			break;
		case 0x6: // DIV divides b by a
			if (a == 0) {
				b = 0;
				ex = 0;
			} else {
				ex = (b << 16) / a;
				b /= a;
			}
			break;
		case 0x7: // DVI - divides signed values
			if (a == 0) {
				b = 0;
				ex = 0;
			} else {
				ex = ((short)b << 16) / (short)a;
				b = (short)b / (short)a;
			}
			break;
		case 0x8: // MOD - (sets b to b % a)
			b = (a == 0) ? 0 : b % a;
			break;
		case 0x9: // MDI - MOD with signed values
			b = (a == 0) ? 0 : (short)b % (short)a;
		case 0xa: // AND - sets b to b & a
			b &= a;
			break;
		case 0xb: // BOR - sets b to b | a
			b |= a;
			break;
		case 0xc: // XOR - sets b to b ^ a
			b ^= a;
			break;
		case 0xd: // SHR - shifts b right by a (logical shift)
			ex = b << 16 >> a;
			b >>>= a;
			break;
		case 0xe: // ASR - shift b right by a (arithmetic shift)
			ex = b << 16 >>> a;
			b >>= a;
			break;
		case 0xf: // SHL - shifts b left by a
			ex = b << a >> 16;
			b = b << a;
			break;
		case 0x10: // IFB - performs next instruction if (b & a) != 0
			skipping = (b & a) == 0;
			break;
		case 0x11: // IFC - performs next instruction if (b & a) == 0
			skipping = (b & a) != 0;
		case 0x12: // IFE - performs next instruction if b == a
			skipping = b != a;
			break;
		case 0x13: // IFN - performs next instruction if b != a
			skipping = b == a;
			break;
		case 0x14: // IFG - performs next instruction if b > a
			skipping = b <= a;
			break;
		case 0x15: // IFA - IFG with signed values
			skipping = (short)b <= (short)a;
			break;
		case 0x16: // IFL - performs next instruction if b < a
			skipping = b >= a;
			break;
		case 0x17: // IFU - IFL with signed values
			skipping = (short)b >= (short)a; 
			break;
		case 0x1a: // ADX - sets b to b+a+EX
			ex = (b += a + ex) > 0xffff ? 1 : 0;
			break;
		case 0x1b: // SBX - sets b to b-a+EX
			ex = (b = b - a + ex) < 0 ? 0xffff : 0;
			break;
		case 0x1e: // STI - sets b to a, then increments I and J
			b = a;
			getRegister(Register.I).value++;
			getRegister(Register.J).value++;
			break;
		case 0x1f: // STD - sets b to a, then decrements I and J
			b = a;
			getRegister(Register.I).value--;
			getRegister(Register.J).value--;
			break;	
		default:
			debugln("Error: Unimplemented basic instruction: 0x" + Integer.toHexString(opcode));
		}
		cellA.set(a);
		cellB.set(b);
		EX.set(ex);
	}

	private void processSpecial(int opcode, Cell a) {
		if (opcode > 0 && (opcode - 1) < Assembler.specialOps.length)
			debugln(Assembler.specialOps[opcode - 1]);
		
		switch (opcode) {
		case 0x0: // EXIT - custom code, makes the processor stop.
			// This is nice because what to do at an empty instruction is undefined, and
			// this provides a clean end for simple programs that don't loop forever.
			debugln("EXIT");
			running = false;
			break;
		case 0x1: // JSR - pushes the address of the next instruction to the stack, sets PC to a
			memory[--SP.value].value = PC.value;
			PC.value = a.value;
			break;
		default:
			debug("Error: Unimplemented special instruction: 0x" + Integer.toHexString(opcode));
		}
	}
	
	@SuppressWarnings("unused")
	public void cycle() {
		if (debug && labels != null && labels.containsKey(PC.value)) {
			System.out.println(labels.get(PC.value));
		}
		
		int instruction = memory[PC.value].value;
		int opcode = 0;
		int rawA = 0, rawB = -1;
		if ((instruction & 0b11111) == 0 && !skipping) {
			// Non-basic opcode. aaaaaaooooo00000
			instruction >>= 5;
			opcode = instruction & 0b11111;
			rawA = instruction >> 5 & 0b111111;
		} else if (!skipping) {
			// Basic opcode. aaaaaabbbbbooooo
			opcode = instruction & 0b11111;
			rawA = instruction >> 10 & 0b111111;
			rawB = instruction >>  5 & 0b11111;
		}
		
		PC.value++;
		
		if (skipping) {
			skipping = false;
			if (opcode >= 0x10 && opcode <= 0x17)
				skipping = true;
			return;
		}

		debug("A: ");
		Cell a = handleArgument(rawA, true), b = null;
		debugln(" = " + (int)a.value);
		if (rawB != -1) {
			debug("B: ");
			b = handleArgument(rawB, false);
			debugln(" = " + (int)b.value);
		}
		
		if (b != null)
			processBasic(opcode, a, b);
		else
			processSpecial(opcode, a);
		
		instructionCount++;
	}
	
	public void run() {
		running = true;
		while (running)
			cycle();
	}
	
	public String dump() {
		String s = "";
		for (Register r : Register.values())
			s += r.toString() + ": " + Integer.toHexString(getRegister(r).value) + "\n";
		return s;
	}
	
	public static void main(String args[]) {
		Scanner s = new Scanner(System.in);
		List<Character> code = new ArrayList<>();
		while (s.hasNextInt(16))
			code.add((char)s.nextInt(16));
		DCPU cpu = new DCPU(code);
		cpu.run();
		System.out.print(cpu.dump());
	}
}