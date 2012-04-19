package net.ian.dcpu;

public class DCPU {
	public Cell[] register;
	public Cell[] memory;
	public Cell SP, PC, O;
	public boolean running;
	
	public enum Register { A, B, C, X, Y, Z, I, J };
	
	public DCPU() {
		this(new short[0]);
	}

	public DCPU(short[] mem) {
		register = new Cell[11];
		for (int i = 0; i < 11; i++)
			register[i] = new Cell(0);
		memory = new Cell[0x10000]; // 0x10000 words in size
		for (int i = 0; i < 0x10000; i++)
			memory[i] = new Cell(i < mem.length ? mem[i] : 0);

		SP = new Cell(0xffff);
		PC = new Cell(0);
		O = new Cell(0);
	}

	public Cell getRegister(Register r) {
		return register[r.ordinal()];
	}
	
	public void setRegister(Register r, int value) {
		register[r.ordinal()].value = value;
	}
	
	private Cell handleArgument(int code) {
		System.out.println(code);
		if (code >= 0x0 && code <= 0x7)
			return register[code];
		else if (code >= 0x8 && code <= 0xf)
			return memory[register[code % 8].value];
		else if (code >= 0x10 && code <= 0x17)
			return null; // Not really sure what this is supposed to do...
		else if (code == 0x18)
			return memory[SP.value++];
		else if (code == 0x19)
			return memory[SP.value];
		else if (code == 0x1a)
			return memory[--SP.value];
		else if (code == 0x1b)
			return SP;
		else if (code == 0x1c)
			return PC;
		else if (code == 0x1d)
			return O;
		else if (code == 0x1e)
			return memory[memory[PC.value++].value];
		else if (code == 0x1f)
			return memory[PC.value++];
		return new Cell(code);
	}
	
	private void skipInstruction() {
		// This skips to the end of the next instruction - used in IF operations.
		int instruction = memory[++PC.value].value;
		int a, b = -1;
		if ((instruction & 0x3) == 0) {
			a = instruction >> 6 & 0x3f;
		} else {
			a = instruction >> 4 & 0x3;
			b = instruction >> 8 & 0x3;
		}
		if ((a >= 0x10 && a <= 0x17) || a == 0x1e || a == 0x1f)
			PC.value++;
		if ((b >= 0x10 && b <= 0x17) || b == 0x1e || b == 0x1f)
			PC.value++;
	}
	
	private void processBasic(int opcode, Cell cellA, Cell cellB) {
		int a = cellA.value;
		int b = cellB.value;
		switch (opcode) {
		case 0x1: // SET a to b
			a = b;
			break;
		case 0x2: // ADD b to a
			O.value = (a += b) > 0xffff ? 1 : 0;
			a &= 0xffff;
			break;
		case 0x3: // SUBTRACT b from a
			O.value = (a -= b) < 0 ? 0xffff : 0;
			a = a < 0 ? 0 : a;
			break;
		case 0x4: // MUL multiplies a by b
			O.value = (a *= b) >> 16 & 0xffff;
			a &= 0xffff;
			break;
		case 0x5: // DIV divides a by b
			if (b == 0) {
				a = 0;
				O.value = 0;
			} else {
				O.value = ((a << 16) / b) & 0xffff;
				a /= b;
			}
			break;
		case 0x6: // MOD (sets a to a % b)
			a = (b == 0) ? 0 : a % b;
			break;
		case 0x7: // SHL shifts a left by b
			O.value = a << b >> 16 & 0xffff;
			a = a << b & 0xffff;
			break;
		case 0x8: // SHR shifts a right by b
			O.value = a << 16 >>b & 0xffff;
			a >>= b;
			break;
		case 0x9: // AND sets a to a & b
			a &= b;
			break;
		case 0xa: // BOR sets a to a | b
			a |= b;
			break;
		case 0xb: // XOR sets a to a ^ b
			a ^= b;
			break;
		case 0xc: // IFE performs next instruction if a == b
			// TODO: This *will not work properly* if the next instruction is > 1 word
			if (a != b)
				skipInstruction();
			break;
		case 0xd: // IFN performs next instruction if a != b
			if (a == b)
				skipInstruction();
			break;
		case 0xe: // IFG performs next instruction if a > b
			if (a <= b)
				skipInstruction();
			break;
		case 0xf: // IFB performs next instructions if (a & b) != 0
			if ((a & b) == 0)
				skipInstruction();
			break;
		}
		cellA.value = a;
		cellB.value = b;
	}

	private void processSpecial(int opcode, Cell a) {
		switch (opcode) {
		case 0x0: // EXIT custom code, makes the processor stop.
			running = false;
			break;
		case 0x1: // JSR pushes the address of the next instruction to the stack, sets PC to a
			memory[--SP.value].value = a.value;
			break;
		}
	}
	
	public void cycle() {
		int instruction = memory[PC.value].value;
		int opcode;
		int rawA, rawB = -1;
		if ((instruction & 0x3) == 0) {
			// Non-basic opcode
			opcode = instruction & 0x3f;
			rawA = instruction >> 6 & 0x3f;
		} else {
			// Basic opcode
			opcode = instruction & 0x3;
			rawA = instruction >> 4 & 0x3;
			rawB = instruction >> 8 & 0x3;
		}
		
		System.out.println(Integer.toBinaryString(instruction));
		System.out.printf("opcode: %s\n", Integer.toBinaryString(opcode));
		System.out.printf("argument A: %s; argument B: %s\n", Integer.toBinaryString(rawA), rawB == -1 ? null : Integer.toBinaryString(rawB));
		
		Cell a = handleArgument(rawA), b = null;
		if (rawB != -1)
			b = handleArgument(rawB);
		
		System.out.printf("eval'd A: %s (%s); eval'd B: %s (%s)\n", a, a, b, b == null ? null : b.value);
		
		if (b != null)
			processBasic(opcode, a, b);
		else
			processSpecial(opcode, a);

		PC.value += 1;
	}
	
	public void run() {
		System.out.println("Running...");
		running = true;
		while (running == true) {
			cycle();
			System.out.println("Next cycle...\n");
		}
	}
}
