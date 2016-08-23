package com.emulation.cpu;

import java.lang.reflect.Method;
import java.util.Formatter;

public class CPU6502 {

  private int             PC;
  private byte            SP;
  private byte            A;
  private byte            X;
  private byte            Y;
  private byte            C;                            // carry flag
  private byte            Z;                            // zero flag
  private byte            I;                            // interrupt disable
                                                        // flag
  private byte            D;                            // decimal mode flag
  private byte            B;                            // break command flag
  private byte            U;                            // unused flag
  private byte            V;                            // overflow flag
  private byte            N;                            // negative flag
  private Interrupt       interrupt    = Interrupt.NONE;
  public long             cycles;
  public int              stall;
  public static final int CPUFrequency = 1789773;
  private CPUMemMapper    m;

  private String[] instructionNames = { //
      "BRK", "ORA", "KIL", "SLO", "NOP", "ORA", "ASL", "SLO", // 00
      "PHP", "ORA", "ASL", "ANC", "NOP", "ORA", "ASL", "SLO", // 01
      "BPL", "ORA", "KIL", "SLO", "NOP", "ORA", "ASL", "SLO", // 02
      "CLC", "ORA", "NOP", "SLO", "NOP", "ORA", "ASL", "SLO", // 03
      "JSR", "AND", "KIL", "RLA", "BIT", "AND", "ROL", "RLA", // 04
      "PLP", "AND", "ROL", "ANC", "BIT", "AND", "ROL", "RLA", // 05
      "BMI", "AND", "KIL", "RLA", "NOP", "AND", "ROL", "RLA", // 06
      "SEC", "AND", "NOP", "RLA", "NOP", "AND", "ROL", "RLA", // 07
      "RTI", "EOR", "KIL", "SRE", "NOP", "EOR", "LSR", "SRE", // 08
      "PHA", "EOR", "LSR", "ALR", "JMP", "EOR", "LSR", "SRE", // 09
      "BVC", "EOR", "KIL", "SRE", "NOP", "EOR", "LSR", "SRE", // 10
      "CLI", "EOR", "NOP", "SRE", "NOP", "EOR", "LSR", "SRE", // 11
      "RTS", "ADC", "KIL", "RRA", "NOP", "ADC", "ROR", "RRA", // 12
      "PLA", "ADC", "ROR", "ARR", "JMP", "ADC", "ROR", "RRA", // 13
      "BVS", "ADC", "KIL", "RRA", "NOP", "ADC", "ROR", "RRA", // 14
      "SEI", "ADC", "NOP", "RRA", "NOP", "ADC", "ROR", "RRA", // 15
      "NOP", "STA", "NOP", "SAX", "STY", "STA", "STX", "SAX", // 16
      "DEY", "NOP", "TXA", "XAA", "STY", "STA", "STX", "SAX", // 17
      "BCC", "STA", "KIL", "AHX", "STY", "STA", "STX", "SAX", // 18
      "TYA", "STA", "TXS", "TAS", "SHY", "STA", "SHX", "AHX", // 19
      "LDY", "LDA", "LDX", "LAX", "LDY", "LDA", "LDX", "LAX", // 20
      "TAY", "LDA", "TAX", "LAX", "LDY", "LDA", "LDX", "LAX", // 21
      "BCS", "LDA", "KIL", "LAX", "LDY", "LDA", "LDX", "LAX", // 22
      "CLV", "LDA", "TSX", "LAS", "LDY", "LDA", "LDX", "LAX", // 23
      "CPY", "CMP", "NOP", "DCP", "CPY", "CMP", "DEC", "DCP", // 24
      "INY", "CMP", "DEX", "AXS", "CPY", "CMP", "DEC", "DCP", // 25
      "BNE", "CMP", "KIL", "DCP", "NOP", "CMP", "DEC", "DCP", // 26
      "CLD", "CMP", "NOP", "DCP", "NOP", "CMP", "DEC", "DCP", // 27
      "CPX", "SBC", "NOP", "ISC", "CPX", "SBC", "INC", "ISC", // 28
      "INX", "SBC", "NOP", "SBC", "CPX", "SBC", "INC", "ISC", // 29
      "BEQ", "SBC", "KIL", "ISC", "NOP", "SBC", "INC", "ISC", // 30
      "SED", "SBC", "NOP", "ISC", "NOP", "SBC", "INC", "ISC" // 31
  };

  public enum Modes {
    abs, //
    abx, //
    aby, //
    modeAccumulator, //
    imm, //
    modeImplied, //
    izx, //
    ind, //
    izy, //
    rel, //
    zp, //
    zpx, //
    zpy //
  }

  private Modes[] instructionModes = { //
      Modes.modeImplied, Modes.izx, Modes.modeImplied, Modes.izx, Modes.zp, Modes.zp, Modes.zp, Modes.zp,
      Modes.modeImplied, Modes.imm, Modes.modeAccumulator, Modes.imm, Modes.abs, Modes.abs, Modes.abs, Modes.abs,
      Modes.rel, Modes.izy, Modes.modeImplied, Modes.izy, Modes.zpx, Modes.zpx, Modes.zpx, Modes.zpx, Modes.modeImplied,
      Modes.aby, Modes.modeImplied, Modes.aby, Modes.abx, Modes.abx, Modes.abx, Modes.abx, Modes.abs, Modes.izx,
      Modes.modeImplied, Modes.izx, Modes.zp, Modes.zp, Modes.zp, Modes.zp, Modes.modeImplied, Modes.imm,
      Modes.modeAccumulator, Modes.imm, Modes.abs, Modes.abs, Modes.abs, Modes.abs, Modes.rel, Modes.izy,
      Modes.modeImplied, Modes.izy, Modes.zpx, Modes.zpx, Modes.zpx, Modes.zpx, Modes.modeImplied, Modes.aby,
      Modes.modeImplied, Modes.aby, Modes.abx, Modes.abx, Modes.abx, Modes.abx, Modes.modeImplied, Modes.izx,
      Modes.modeImplied, Modes.izx, Modes.zp, Modes.zp, Modes.zp, Modes.zp, Modes.modeImplied, Modes.imm,
      Modes.modeAccumulator, Modes.imm, Modes.abs, Modes.abs, Modes.abs, Modes.abs, Modes.rel, Modes.izy,
      Modes.modeImplied, Modes.izy, Modes.zpx, Modes.zpx, Modes.zpx, Modes.zpx, Modes.modeImplied, Modes.aby,
      Modes.modeImplied, Modes.aby, Modes.abx, Modes.abx, Modes.abx, Modes.abx, Modes.modeImplied, Modes.izx,
      Modes.modeImplied, Modes.izx, Modes.zp, Modes.zp, Modes.zp, Modes.zp, Modes.modeImplied, Modes.imm,
      Modes.modeAccumulator, Modes.imm, Modes.ind, Modes.abs, Modes.abs, Modes.abs, Modes.rel, Modes.izy,
      Modes.modeImplied, Modes.izy, Modes.zpx, Modes.zpx, Modes.zpx, Modes.zpx, Modes.modeImplied, Modes.aby,
      Modes.modeImplied, Modes.aby, Modes.abx, Modes.abx, Modes.abx, Modes.abx, Modes.imm, Modes.izx, Modes.imm,
      Modes.izx, Modes.zp, Modes.zp, Modes.zp, Modes.zp, Modes.modeImplied, Modes.imm, Modes.modeImplied, Modes.imm,
      Modes.abs, Modes.abs, Modes.abs, Modes.abs, Modes.rel, Modes.izy, Modes.modeImplied, Modes.izy, Modes.zpx,
      Modes.zpx, Modes.zpy, Modes.zpy, Modes.modeImplied, Modes.aby, Modes.modeImplied, Modes.aby, Modes.abx, Modes.abx,
      Modes.aby, Modes.aby, Modes.imm, Modes.izx, Modes.imm, Modes.izx, Modes.zp, Modes.zp, Modes.zp, Modes.zp,
      Modes.modeImplied, Modes.imm, Modes.modeImplied, Modes.imm, Modes.abs, Modes.abs, Modes.abs, Modes.abs, Modes.rel,
      Modes.izy, Modes.modeImplied, Modes.izy, Modes.zpx, Modes.zpx, Modes.zpy, Modes.zpy, Modes.modeImplied, Modes.aby,
      Modes.modeImplied, Modes.aby, Modes.abx, Modes.abx, Modes.aby, Modes.aby, Modes.imm, Modes.izx, Modes.imm,
      Modes.izx, Modes.zp, Modes.zp, Modes.zp, Modes.zp, Modes.modeImplied, Modes.imm, Modes.modeImplied, Modes.imm,
      Modes.abs, Modes.abs, Modes.abs, Modes.abs, Modes.rel, Modes.izy, Modes.modeImplied, Modes.izy, Modes.zpx,
      Modes.zpx, Modes.zpx, Modes.zpx, Modes.modeImplied, Modes.aby, Modes.modeImplied, Modes.aby, Modes.abx, Modes.abx,
      Modes.abx, Modes.abx, Modes.imm, Modes.izx, Modes.imm, Modes.izx, Modes.zp, Modes.zp, Modes.zp, Modes.zp,
      Modes.modeImplied, Modes.imm, Modes.modeImplied, Modes.imm, Modes.abs, Modes.abs, Modes.abs, Modes.abs, Modes.rel,
      Modes.izy, Modes.modeImplied, Modes.izy, Modes.zpx, Modes.zpx, Modes.zpx, Modes.zpx, Modes.modeImplied, Modes.aby,
      Modes.modeImplied, Modes.aby, Modes.abx, Modes.abx, Modes.abx, Modes.abx, };

  private byte[] instructionSizes = { 1, 2, 0, 0, 2, 2, 2, 0, 1, 2, 1, 0, 3, 3, 3, 0, 2, 2, 0, 0, 2, 2, 2, 0, 1, 3, 1,
      0, 3, 3, 3, 0, 3, 2, 0, 0, 2, 2, 2, 0, 1, 2, 1, 0, 3, 3, 3, 0, 2, 2, 0, 0, 2, 2, 2, 0, 1, 3, 1, 0, 3, 3, 3, 0, 1,
      2, 0, 0, 2, 2, 2, 0, 1, 2, 1, 0, 3, 3, 3, 0, 2, 2, 0, 0, 2, 2, 2, 0, 1, 3, 1, 0, 3, 3, 3, 0, 1, 2, 0, 0, 2, 2, 2,
      0, 1, 2, 1, 0, 3, 3, 3, 0, 2, 2, 0, 0, 2, 2, 2, 0, 1, 3, 1, 0, 3, 3, 3, 0, 2, 2, 0, 0, 2, 2, 2, 0, 1, 0, 1, 0, 3,
      3, 3, 0, 2, 2, 0, 0, 2, 2, 2, 0, 1, 3, 1, 0, 0, 3, 0, 0, 2, 2, 2, 0, 2, 2, 2, 0, 1, 2, 1, 0, 3, 3, 3, 0, 2, 2, 0,
      0, 2, 2, 2, 0, 1, 3, 1, 0, 3, 3, 3, 0, 2, 2, 0, 0, 2, 2, 2, 0, 1, 2, 1, 0, 3, 3, 3, 0, 2, 2, 0, 0, 2, 2, 2, 0, 1,
      3, 1, 0, 3, 3, 3, 0, 2, 2, 0, 0, 2, 2, 2, 0, 1, 2, 1, 0, 3, 3, 3, 0, 2, 2, 0, 0, 2, 2, 2, 0, 1, 3, 1, 0, 3, 3, 3,
      0, };

  private byte[] instructionCycles = { 7, 6, 2, 8, 3, 3, 5, 5, 3, 2, 2, 2, 4, 4, 6, 6, 2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2,
      7, 4, 4, 7, 7, 6, 6, 2, 8, 3, 3, 5, 5, 4, 2, 2, 2, 4, 4, 6, 6, 2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7, 6,
      6, 2, 8, 3, 3, 5, 5, 3, 2, 2, 2, 3, 4, 6, 6, 2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7, 6, 6, 2, 8, 3, 3, 5,
      5, 4, 2, 2, 2, 5, 4, 6, 6, 2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7, 2, 6, 2, 6, 3, 3, 3, 3, 2, 2, 2, 2, 4,
      4, 4, 4, 2, 6, 2, 6, 4, 4, 4, 4, 2, 5, 2, 5, 5, 5, 5, 5, 2, 6, 2, 6, 3, 3, 3, 3, 2, 2, 2, 2, 4, 4, 4, 4, 2, 5, 2,
      5, 4, 4, 4, 4, 2, 4, 2, 4, 4, 4, 4, 4, 2, 6, 2, 8, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6, 2, 5, 2, 8, 4, 4, 6, 6, 2,
      4, 2, 7, 4, 4, 7, 7, 2, 6, 2, 8, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6, 2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7,
      7, };

  private byte[] instructionPageCycles = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1,
      0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1,
      0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0,
      0, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1,
      0, 0, };

  public static void main(String[] args) {
    CPU6502 cpu = new CPU6502(null);
    for (int i = 0; i < 256; i++) {
      // System.out.printf("%s,%s,%d,%d\n", cpu.instructionNames[i],
      // getMode(cpu.instructionModes[i]),
      // cpu.instructionCycles[i], cpu.instructionSizes[i]);
    }
  }

  private static String getMode(Modes addressingModes) {
    switch (addressingModes) {
      case abs:
        return "abs";
      case abx:
        return "abx";
      case aby:
        return "aby";
      case modeAccumulator:
        return "";
      case imm:
        return "imm";
      case modeImplied:
        return null;
      case izx:
        return "izx";
      case ind:
        return "ind";
      case izy:
        return "izy";
      case rel:
        return "rel";
      case zp:
        return "zp";
      case zpx:
        return "zpx";
      case zpy:
        return "zpy";
    }
    return null;
  }

  private enum Interrupt {
    NONE, NMI, IRQ
  }

  public CPU6502(CPUMemMapper m) {
    this.m = m;
    reset();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    Formatter format = new Formatter(sb);
    format.format(
        "+---------------------------------------------------------------+\n"
            + "| PC\t\t| SP\t| A\t| X\t| Y\t| C\t| Z\t|\n"
            + "+---------------------------------------------------------------+\n"
            + "| 0x%X\t| 0x%X\t| 0x%X\t| 0x%X\t| 0x%X\t| 0x%X\t| 0x%X\t|\n"
            + "+===============================================================+\n"
            + "| I\t| D\t| B\t| U\t| V\t| N\t| interrupt\t|\n"
            + "+---------------------------------------------------------------+\n"
            + "| 0x%X\t| 0x%X\t| 0x%X\t| 0x%X\t| 0x%X\t| 0x%X\t| %s\t\t|\n"
            + "+---------------------------------------------------------------+\n",
        this.PC, this.SP, this.A, this.X, this.Y, this.C, this.Z, this.I, this.D, this.B, this.U, this.V, this.N,
        this.interrupt);
    format.close();
    return sb.toString();
  }

  public byte read(int address) {
    return this.m.read(address);
  }

  public void write(int address, int value) {
    this.m.write(address, value);
  }

  public long step() throws Exception {

    // // System.out.println("********** CPU step ************");

    if (this.stall > 0) {
      this.stall--;
      return 1;
    }

    long cycles = this.cycles;

    // Step executes a single CPU instruction
    switch (this.interrupt) {
      case NMI:
        nmi();
        break;
      case IRQ:
        irq();
        break;
      default:
        break;
    }

    this.interrupt = Interrupt.NONE;
    int opcode = read(this.PC) & 0xff;
    Modes mode = this.instructionModes[opcode];

    int address;
    boolean pageCrossed = false;

    switch (mode) {
      case abs:
        address = read16(this.PC + 1);
        break;
      case abx:
        address = read16(this.PC + 1) + this.X;
        pageCrossed = pagesDiffer(address - this.X, address);
        break;
      case aby:
        address = read16(this.PC + 1) + this.Y;
        pageCrossed = pagesDiffer(address - this.Y, address);
        break;
      case modeAccumulator:
        address = 0;
        break;
      case imm:
        address = this.PC + 1;
        break;
      case modeImplied:
        address = 0;
        break;
      case izx:
        address = read16bug(read(this.PC + 1) + this.X);
        break;
      case ind:
        address = read16bug(read16(this.PC + 1));
        break;
      case izy:
        address = read16bug(read(this.PC + 1)) + this.Y;
        pageCrossed = pagesDiffer(address - this.Y, address);
        break;
      case rel:
        int offset = read(this.PC + 1) & 0xFF;
        if (offset < 0x80) {
          address = this.PC + 2 + offset;
        } else {
          // way to simulate signed 8 bit operations
          address = (this.PC + 2 + offset) - 0x100;
        }
        break;
      case zp:
        address = read(this.PC + 1);
        break;
      case zpx:
        address = read(this.PC + 1) + this.X;
        break;
      case zpy:
        address = read(this.PC + 1) + this.Y;
        break;
      default:
        address = 0;
        break;
    }

    this.PC += this.instructionSizes[opcode];
    this.cycles += this.instructionCycles[opcode];
    if (pageCrossed) {
      cycles += this.instructionPageCycles[opcode];
    }

    if (opcode == 0x4C) {
      // System.exit(0);
    }

    // System.out.printf("PC 0x%x Instruction: %s opcode 0x%X address 0x%X
    // addressing mode %s\n", PC,
    // instructionNames[opcode], opcode, address & 0xFFFF, mode);

    // // System.out.printf(
    // "PC 0x%x Instruction: %s opcode 0x%X instructionSizes[opcode] 0x%X "
    // + "instructionCycles[opcode] 0x%X instructionPageCycles[opcode] 0x%X
    // "
    // + "address 0x%X addressing mode %s\n",
    // PC, instructionNames[opcode], opcode, instructionSizes[opcode],
    // instructionCycles[opcode],
    // instructionPageCycles[opcode], address & 0xFFFF, mode);

    Class[] arg = new Class[3];
    arg[0] = int.class;
    arg[1] = int.class;
    arg[2] = Modes.class;
    Method m = this.getClass().getMethod(this.instructionNames[opcode].toLowerCase(), arg);
    m.invoke(this, address & 0xFFFF, this.PC, mode);
    
    // // System.out.println("CPU: " + this);
    // // System.out.println("********** CPU step complete ************");

    return this.cycles - cycles;
  }

  // addBranchCycles adds a cycle for taking a branch and adds another cycle
  // if the branch jumps to a new page
  public void addBranchCycles(int address, int PC, Modes mode) {
    this.cycles++;
    if (pagesDiffer(PC, address)) {
      this.cycles++;
    }
  }

  public void compare(byte a, byte b) {
    setZN((byte) (a - b));
    if (a >= b) {
      this.C = 1;
    } else {
      this.C = 0;
    }
  }

  // Read16 reads two memory locations using read to return a double-word
  // value
  private int read16(int address) {
    int lo = read(address) & 0xff;
    int hi = read(address + 1) & 0xff;
    return ((hi << 8) | lo);
  }

  // read16bug emulates a 6502 bug that caused the low byte to wrap without
  // incrementing the high byte
  private int read16bug(int address) {
    int a = address;
    int b = (a & 0xFF00) | ((a) + 1);
    int lo = read(a);
    int hi = read(b);
    return ((hi) << 8) | (lo);
  }

  // push pushes a byte onto the stack
  public void push(byte value) {
    write(0x100 | this.SP, value);
    this.SP--;
  }

  // pull pops a byte from the stack
  private byte pull() {
    this.SP++;
    return read(0x100 | (this.SP));
  }

  // push16 pushes two bytes onto the stack
  public void push16(int value) {
    byte hi = (byte) (value >> 8);
    byte lo = (byte) (value & 0xFF);
    push(hi);
    push(lo);
  }

  // pull16 pops two bytes from the stack
  private int pull16() {
    int lo = (pull());
    int hi = (pull());
    return (hi << 8) | lo;
  }

  // Flags returns the processor status flags
  private byte Flags() {
    byte flags = 0;
    flags |= this.C << 0;
    flags |= this.Z << 1;
    flags |= this.I << 2;
    flags |= this.D << 3;
    flags |= this.B << 4;
    flags |= this.U << 5;
    flags |= this.V << 6;
    flags |= this.N << 7;
    return flags;
  }

  // SetFlags sets the processor status flags
  public void setFlags(int flags) {
    this.C = (byte) ((flags >> 0) & 1);
    this.Z = (byte) ((flags >> 1) & 1);
    this.I = (byte) ((flags >> 2) & 1);
    this.D = (byte) ((flags >> 3) & 1);
    this.B = (byte) ((flags >> 4) & 1);
    this.U = (byte) ((flags >> 5) & 1);
    this.V = (byte) ((flags >> 6) & 1);
    this.N = (byte) ((flags >> 7) & 1);
  }

  // setZ sets the zero flag if the argument is zero
  public void setZ(byte value) {
    if (value == 0) {
      this.Z = 1;
    } else {
      this.Z = 0;
    }
  }

  // setN sets the negative flag if the argument is negative (high bit is set)
  public void setN(byte value) {
    /* 1000 0000b */
    if ((value & 0x80) != 0) {
      this.N = 1;
    } else {
      this.N = 0;
    }
  }

  // setZN sets the zero flag and the negative flag
  public void setZN(byte value) {
    setZ(value);
    setN(value);
  }

  // triggerNMI causes a non-maskable interrupt to occur on the next cycle
  public void triggerNMI() {
    this.interrupt = Interrupt.NMI;
  }

  // triggerIRQ causes an IRQ interrupt to occur on the next cycle
  public void triggerIRQ() {
    if (this.I == 0) {
      this.interrupt = Interrupt.IRQ;
    }
  }

  private boolean pagesDiffer(int j, int address) {
    // TODO Auto-generated method stub
    return false;
  }

  // NMI - Non-Maskable Interrupt
  public void nmi() {
    int address = 0;
    Modes mode = null;
    push16(this.PC);
    php(address, this.PC, mode);
    this.PC = read16(0xFFFA);
    this.I = 1;
    this.cycles += 7;
  }

  // IRQ - IRQ Interrupt
  public void irq() {
    int address = 0;
    Modes mode = null;
    push16(this.PC);
    php(address, this.PC, mode);
    this.PC = read16(0xFFFE);
    this.I = 1;
    this.cycles += 7;
  }

  // ADC - Add with Carry
  public void adc(int address, int PC, Modes mode) {
    byte a = this.A;
    byte b = read(address);
    byte c = this.C;
    this.A = (byte) (a + b + c);
    setZN(this.A);
    if (((a) + (b) + (c)) > 0xFF) {
      this.C = 1;
    } else {
      this.C = 0;
    }
    if ((((a ^ b) & 0x80) == 0) && (((a ^ this.A) & 0x80) != 0)) {
      this.V = 1;
    } else {
      this.V = 0;
    }
  }

  // AND - Logical AND
  public void and(int address, int PC, Modes mode) {
    this.A = (byte) (this.A & read(address));
    setZN(this.A);
  }

  // ASL - Arithmetic Shift Left
  public void asl(int address, int PC, Modes mode) {
    if (mode == Modes.modeAccumulator) {
      this.C = (byte) ((this.A >> 7) & 1);
      this.A <<= 1;
      setZN(this.A);
    } else {
      byte value = read(address);
      this.C = (byte) ((value >> 7) & 1);
      value <<= 1;
      write(address, value);
      setZN(value);
    }
  }

  // BCC - Branch if Carry Clear
  public void bcc(int address, int PC, Modes mode) {
    if (this.C == 0) {
      PC = address;
      addBranchCycles(address, PC, mode);
    }
  }

  // BCS - Branch if Carry Set
  public void bcs(int address, int PC, Modes mode) {
    if (this.C != 0) {
      PC = address;
      addBranchCycles(address, PC, mode);
    }
  }

  // BEQ - Branch if Equal
  public void beq(int address, int PC, Modes mode) {
    if (this.Z != 0) {
      PC = address;
      addBranchCycles(address, PC, mode);
    }
  }

  // BIT - Bit Test
  public void bit(int address, int PC, Modes mode) {
    byte value = read(address);
    this.V = (byte) ((value >> 6) & 1);
    setZ((byte) (value & this.A));
    setN(value);
  }

  // BMI - Branch if Minus
  public void bmi(int address, int PC, Modes mode) {
    if (this.N != 0) {
      PC = address;
      addBranchCycles(address, PC, mode);
    }
  }

  // BNE - Branch if Not Equal
  public void bne(int address, int PC, Modes mode) {
    if (this.Z == 0) {
      PC = address;
      addBranchCycles(address, PC, mode);
    }
  }

  // BPL - Branch if Positive
  public void bpl(int address, int PC, Modes mode) {
    if (this.N == 0) {
      // System.out.println("Here");
      this.PC = address;
      addBranchCycles(address, this.PC, mode);
    }
  }

  // BRK - Force Interrupt
  public void brk(int address, int PC, Modes mode) {
    push16(PC);
    php(address, PC, mode);
    sei(address, PC, mode);
    PC = read16(0xFFFE);
  }

  // BVC - Branch if Overflow Clear
  public void bvc(int address, int PC, Modes mode) {
    if (this.V == 0) {
      PC = address;
      addBranchCycles(address, PC, mode);
    }
  }

  // BVS - Branch if Overflow Set
  public void bvs(int address, int PC, Modes mode) {
    if (this.V != 0) {
      PC = address;
      addBranchCycles(address, PC, mode);
    }
  }

  // CLC - Clear Carry Flag
  public void clc(int address, int PC, Modes mode) {
    this.C = 0;
  }

  // CLD - CLear Decimal Mode - $D8
  public void cld(int address, int PC, Modes mode) {
    this.D = 0;
  }

  // CLI - Clear Interrupt Disable
  public void cli(int address, int PC, Modes mode) {
    this.I = 0;
  }

  // CLV - Clear Overflow Flag
  public void clv(int address, int PC, Modes mode) {
    this.V = 0;
  }

  // CMP - Compare
  public void cmp(int address, int PC, Modes mode) {
    byte value = read(address);
    compare(this.A, value);
  }

  // CPX - Compare X Register
  public void cpx(int address, int PC, Modes mode) {
    byte value = read(address);
    compare(this.X, value);
  }

  // CPY - Compare Y Register
  public void cpy(int address, int PC, Modes mode) {
    byte value = read(address);
    compare(this.Y, value);
  }

  // DEC - Decrement Memory
  public void dec(int address, int PC, Modes mode) {
    byte value = (byte) (read(address) - 1);
    write(address, value);
    setZN(value);
  }

  // DEX - Decrement X Register
  public void dex(int address, int PC, Modes mode) {
    this.X--;
    setZN(this.X);
  }

  // DEY - Decrement Y Register
  public void dey(int address, int PC, Modes mode) {
    this.Y--;
    setZN(this.Y);
  }

  // EOR - Exclusive OR
  public void eor(int address, int PC, Modes mode) {
    this.A = (byte) (this.A ^ read(address));
    setZN(this.A);
  }

  // INC - Increment Memory
  public void inc(int address, int PC, Modes mode) {
    byte value = (byte) (read(address) + 1);
    write(address, value);
    setZN(value);
  }

  // INX - Increment X Register
  public void inx(int address, int PC, Modes mode) {
    this.X++;
    setZN(this.X);
  }

  // INY - Increment Y Register
  public void iny(int address, int PC, Modes mode) {
    this.Y++;
    setZN(this.Y);
  }

  // JMP - Jump
  public void jmp(int address, int PC, Modes mode) {
    this.PC = address;
  }

  // JSR - Jump to Subroutine
  public void jsr(int address, int PC, Modes mode) {
    push16(PC - 1);
    this.PC = address;
  }

  // LDA - Load Accumulator
  public void lda(int address, int PC, Modes mode) {
    this.A = read(address);
    setZN(this.A);
  }

  // LDX - Load X Register
  public void ldx(int address, int PC, Modes mode) {
    this.X = read(address);
    setZN(this.X);
  }

  // LDY - Load Y Register
  public void ldy(int address, int PC, Modes mode) {
    this.Y = read(address);
    setZN(this.Y);
  }

  // LSR - Logical Shift Right
  public void lsr(int address, int PC, Modes mode) {
    if (mode == Modes.modeAccumulator) {
      this.C = (byte) (this.A & 1);
      this.A >>= 1;
      setZN(this.A);
    } else {
      byte value = read(address);
      this.C = (byte) (value & 1);
      value >>= 1;
      write(address, value);
      setZN(value);
    }
  }

  // NOP - No Operation
  public void nop(int address, int PC, Modes mode) {
  }

  // ORA - Logical Inclusive OR
  public void ora(int address, int PC, Modes mode) {
    this.A = (byte) (this.A | read(address));
    setZN(this.A);
  }

  // PHA - Push Accumulator
  public void pha(int address, int PC, Modes mode) {
    push(this.A);
  }

  // PHP - Push Processor Status
  public void php(int address, int PC, Modes mode) {
    push((byte) (Flags() | 0x10));
  }

  // PLA - Pull Accumulator
  public void pla(int address, int PC, Modes mode) {
    this.A = pull();
    setZN(this.A);
  }

  // PLP - Pull Processor Status
  public void plp(int address, int PC, Modes mode) {
    setFlags((pull() & 0xEF) | 0x20);
  }

  // ROL - Rotate Left
  public void rol(int address, int PC, Modes mode) {
    if (mode == Modes.modeAccumulator) {
      byte c = this.C;
      this.C = (byte) ((this.A >> 7) & 1);
      this.A = (byte) ((this.A << 1) | c);
      setZN(this.A);
    } else {
      byte c = this.C;
      byte value = read(address);
      this.C = (byte) ((value >> 7) & 1);
      value = (byte) ((value << 1) | c);
      write(address, value);
      setZN(value);
    }
  }

  // ROR - Rotate Right
  public void ror(int address, int PC, Modes mode) {
    if (mode == Modes.modeAccumulator) {
      byte c = this.C;
      this.C = (byte) (this.A & 1);
      this.A = (byte) ((this.A >> 1) | (c << 7));
      setZN(this.A);
    } else {
      byte c = this.C;
      byte value = read(address);
      this.C = (byte) (value & 1);
      value = (byte) ((value >> 1) | (c << 7));
      write(address, value);
      setZN(value);
    }
  }

  // RTI - Return from Interrupt
  public void rti(int address, int PC, Modes mode) {
    setFlags((pull() & 0xEF) | 0x20);
    PC = pull16();
  }

  // RTS - Return from Subroutine
  public void rts(int address, int PC, Modes mode) {
    PC = pull16() + 1;
  }

  // SBC - Subtract with Carry
  public void sbc(int address, int PC, Modes mode) {
    byte a = this.A;
    byte b = read(address);
    byte c = this.C;
    this.A = (byte) (a - b - (1 - c));
    setZN(this.A);
    if (((a) - (b) - (1 - c)) >= 0) {
      this.C = 1;
    } else {
      this.C = 0;
    }
    if ((((a ^ b) & 0x80) != 0) && (((a ^ this.A) & 0x80) != 0)) {
      this.V = 1;
    } else {
      this.V = 0;
    }
  }

  // SEC - Set Carry Flag
  public void sec(int address, int PC, Modes mode) {
    this.C = 1;
  }

  // SED - Set Decimal Flag
  public void sed(int address, int PC, Modes mode) {
    this.D = 1;
  }

  // SEI - SEt Interrupt Disable - $78
  public void sei(int address, int PC, Modes mode) {
    this.I = 1;
  }

  // STA - Store Accumulator
  public void sta(int address, int PC, Modes mode) {
    write(address, this.A);
  }

  // STX - Store X Register
  public void stx(int address, int PC, Modes mode) {
    write(address, this.X);
  }

  // STY - Store Y Register
  public void sty(int address, int PC, Modes mode) {
    write(address, this.Y);
  }

  // TAX - Transfer Accumulator to X
  public void tax(int address, int PC, Modes mode) {
    this.X = this.A;
    setZN(this.X);
  }

  // TAY - Transfer Accumulator to Y
  public void tay(int address, int PC, Modes mode) {
    this.Y = this.A;
    setZN(this.Y);
  }

  // TSX - Transfer Stack Pointer to X
  public void tsx(int address, int PC, Modes mode) {
    this.X = this.SP;
    setZN(this.X);
  }

  // TXA - Transfer X to Accumulator
  public void txa(int address, int PC, Modes mode) {
    this.A = this.X;
    setZN(this.A);
  }

  // TXS - Transfer X to Stack Pointer
  public void txs(int address, int PC, Modes mode) {
    this.SP = this.X;
  }

  // TYA - Transfer Y to Accumulator
  public void tya(int address, int PC, Modes mode) {
    this.A = this.Y;
    setZN(this.A);
  }

  // illegal opcodes below

  public void ahx(int address, int PC, Modes mode) {
  }

  public void alr(int address, int PC, Modes mode) {
  }

  public void anc(int address, int PC, Modes mode) {
  }

  public void arr(int address, int PC, Modes mode) {
  }

  public void axs(int address, int PC, Modes mode) {
  }

  public void dcp(int address, int PC, Modes mode) {
  }

  public void isc(int address, int PC, Modes mode) {
  }

  public void kil(int address, int PC, Modes mode) {
  }

  public void las(int address, int PC, Modes mode) {
  }

  public void lax(int address, int PC, Modes mode) {
  }

  public void rla(int address, int PC, Modes mode) {
  }

  public void rra(int address, int PC, Modes mode) {
  }

  public void sax(int address, int PC, Modes mode) {
  }

  public void shx(int address, int PC, Modes mode) {
  }

  public void shy(int address, int PC, Modes mode) {
  }

  public void slo(int address, int PC, Modes mode) {
  }

  public void sre(int address, int PC, Modes mode) {
  }

  public void tas(int address, int PC, Modes mode) {
  }

  public void xaa(int address, int PC, Modes mode) {
  }

  public void reset() {
    System.out.println("********** Resetting CPU ************");
    this.PC = read16(0xFFFC);
    System.out.printf("Reset CPU PC: 0x%x\n", this.PC);
    this.SP = (byte) 0xFD;
    /* Set flags I = 1 and U = 1 */
    setFlags(0x24);
    // System.out.println(this);
    System.out.println("********** CPU Reset Complete ************");
  }
}
