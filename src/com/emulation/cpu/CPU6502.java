package com.emulation.cpu;

import java.util.Formatter;

public class CPU6502 {

    /*
     * List of CPU registers, flags etc.
     */
  private int  PC; // 16 bit program counter
  private byte SP; // 8 bit stack pointer
  private byte A;
  private byte X;
  private byte Y;
  private byte C;  // carry flag
  private byte Z;  // zero flag
  private byte I;  // interrupt disable
  private byte D;  // decimal mode flag
  private byte B;  // break command flag
  private byte U;  // unused flag
  private byte V;  // overflow flag
  private byte N;  // negative flag
  private Interrupt interrupt = Interrupt.NONE;

  private enum Interrupt {
    NONE, NMI, IRQ
  }

  public int              stall;
  public long             cycles;
  public static final int CPUFrequency = 1789773;

  private CPUMemMapper m;


|------+--------+------+------+--------+--------+------+--------+------+------+--------+--------|
|  Hex | Opcode | mode | size | cycles |   page |  Hex | Opcode | mode | size | cycles |   page |
|      |        |      |      |        | cycles |      |        |      |      |        | cycles |
|------+--------+------+------+--------+--------+------+--------+------+------+--------+--------|
| 0x00 | BRK    | imp  |    1 |      7 |      1 | 0x20 | JSR    | abs  |    3 |      6 |      3 |
| 0x01 | ORA    | izx  |    2 |      6 |      2 | 0x21 | AND    | izx  |    2 |      6 |      2 |
| 0x02 | KIL    | imp  |    0 |      2 |      0 | 0x22 | KIL    | imp  |    0 |      2 |      0 |
| 0x03 | SLO    | izx  |    0 |      8 |      0 | 0x23 | RLA    | izx  |    0 |      8 |      0 |
| 0x04 | NOP    | zpg  |    2 |      3 |      2 | 0x24 | BIT    | zpg  |    2 |      3 |      2 |
| 0x05 | ORA    | zpg  |    2 |      3 |      2 | 0x25 | AND    | zpg  |    2 |      3 |      2 |
| 0x06 | ASL    | zpg  |    2 |      5 |      2 | 0x26 | ROL    | zpg  |    2 |      5 |      2 |
| 0x07 | SLO    | zpg  |    0 |      5 |      0 | 0x27 | RLA    | zpg  |    0 |      5 |      0 |
| 0x08 | PHP    | imp  |    1 |      3 |      1 | 0x28 | PLP    | imp  |    1 |      4 |      1 |
| 0x09 | ORA    | imm  |    2 |      2 |      2 | 0x29 | AND    | imm  |    2 |      2 |      2 |
| 0x0A | ASL    | acc  |    1 |      2 |      1 | 0x2A | ROL    | acc  |    1 |      2 |      1 |
| 0x0B | ANC    | imm  |    0 |      2 |      0 | 0x2B | ANC    | imm  |    0 |      2 |      0 |
| 0x0C | NOP    | abs  |    3 |      4 |      3 | 0x2C | BIT    | abs  |    3 |      4 |      3 |
| 0x0D | ORA    | abs  |    3 |      4 |      3 | 0x2D | AND    | abs  |    3 |      4 |      3 |
| 0x0E | ASL    | abs  |    3 |      6 |      3 | 0x2E | ROL    | abs  |    3 |      6 |      3 |
| 0x0F | SLO    | abs  |    0 |      6 |      0 | 0x2F | RLA    | abs  |    0 |      6 |      0 |
| 0x10 | BPL    | rel  |    2 |      2 |      2 | 0x30 | BMI    | rel  |    2 |      2 |      2 |
| 0x11 | ORA    | izy  |    2 |      5 |      2 | 0x31 | AND    | izy  |    2 |      5 |      2 |
| 0x12 | KIL    | imp  |    0 |      2 |      0 | 0x32 | KIL    | imp  |    0 |      2 |      0 |
| 0x13 | SLO    | izy  |    0 |      8 |      0 | 0x33 | RLA    | izy  |    0 |      8 |      0 |
| 0x14 | NOP    | zpx  |    2 |      4 |      2 | 0x34 | NOP    | zpx  |    2 |      4 |      2 |
| 0x15 | ORA    | zpx  |    2 |      4 |      2 | 0x35 | AND    | zpx  |    2 |      4 |      2 |
| 0x16 | ASL    | zpx  |    2 |      6 |      2 | 0x36 | ROL    | zpx  |    2 |      6 |      2 |
| 0x17 | SLO    | zpx  |    0 |      6 |      0 | 0x37 | RLA    | zpx  |    0 |      6 |      0 |
| 0x18 | CLC    | imp  |    1 |      2 |      1 | 0x38 | SEC    | imp  |    1 |      2 |      1 |
| 0x19 | ORA    | aby  |    3 |      4 |      3 | 0x39 | AND    | aby  |    3 |      4 |      3 |
| 0x1A | NOP    | imp  |    1 |      2 |      1 | 0x3A | NOP    | imp  |    1 |      2 |      1 |
| 0x1B | SLO    | aby  |    0 |      7 |      0 | 0x3B | RLA    | aby  |    0 |      7 |      0 |
| 0x1C | NOP    | abx  |    3 |      4 |      3 | 0x3C | NOP    | abx  |    3 |      4 |      3 |
| 0x1D | ORA    | abx  |    3 |      4 |      3 | 0x3D | AND    | abx  |    3 |      4 |      3 |
| 0x1E | ASL    | abx  |    3 |      7 |      3 | 0x3E | ROL    | abx  |    3 |      7 |      3 |
| 0x1F | SLO    | abx  |    0 |      7 |      0 | 0x3F | RLA    | abx  |    0 |      7 |      0 |
|------+--------+------+------+--------+--------+------+--------+------+------+--------+--------|
|  Hex | Opcode | mode | size | cycles |   page |  Hex | Opcode | mode | size | cycles |   page |
|      |        |      |      |        | cycles |      |        |      |      |        | cycles |
|------+--------+------+------+--------+--------+------+--------+------+------+--------+--------|
| 0x40 | RTI    | imp  |    1 |      6 |      1 | 0x60 | RTS    | imp  |    1 |      6 |      1 |
| 0x41 | EOR    | izx  |    2 |      6 |      2 | 0x61 | ADC    | izx  |    2 |      6 |      2 |
| 0x42 | KIL    | imp  |    0 |      2 |      0 | 0x62 | KIL    | imp  |    0 |      2 |      0 |
| 0x43 | SRE    | izx  |    0 |      8 |      0 | 0x63 | RRA    | izx  |    0 |      8 |      0 |
| 0x44 | NOP    | zpg  |    2 |      3 |      2 | 0x64 | NOP    | zpg  |    2 |      3 |      2 |
| 0x45 | EOR    | zpg  |    2 |      3 |      2 | 0x65 | ADC    | zpg  |    2 |      3 |      2 |
| 0x46 | LSR    | zpg  |    2 |      5 |      2 | 0x66 | ROR    | zpg  |    2 |      5 |      2 |
| 0x47 | SRE    | zpg  |    0 |      5 |      0 | 0x67 | RRA    | zpg  |    0 |      5 |      0 |
| 0x48 | PHA    | imp  |    1 |      3 |      1 | 0x68 | PLA    | imp  |    1 |      4 |      1 |
| 0x49 | EOR    | imm  |    2 |      2 |      2 | 0x69 | ADC    | imm  |    2 |      2 |      2 |
| 0x4A | LSR    | acc  |    1 |      2 |      1 | 0x6A | ROR    | acc  |    1 |      2 |      1 |
| 0x4B | ALR    | imm  |    0 |      2 |      0 | 0x6B | ARR    | imm  |    0 |      2 |      0 |
| 0x4C | JMP    | abs  |    3 |      3 |      3 | 0x6C | JMP    | ind  |    3 |      5 |      3 |
| 0x4D | EOR    | abs  |    3 |      4 |      3 | 0x6D | ADC    | abs  |    3 |      4 |      3 |
| 0x4E | LSR    | abs  |    3 |      6 |      3 | 0x6E | ROR    | abs  |    3 |      6 |      3 |
| 0x4F | SRE    | abs  |    0 |      6 |      0 | 0x6F | RRA    | abs  |    0 |      6 |      0 |
| 0x50 | BVC    | rel  |    2 |      2 |      2 | 0x70 | BVS    | rel  |    2 |      2 |      2 |
| 0x51 | EOR    | izy  |    2 |      5 |      2 | 0x71 | ADC    | izy  |    2 |      5 |      2 |
| 0x52 | KIL    | imp  |    0 |      2 |      0 | 0x72 | KIL    | imp  |    0 |      2 |      0 |
| 0x53 | SRE    | izy  |    0 |      8 |      0 | 0x73 | RRA    | izy  |    0 |      8 |      0 |
| 0x54 | NOP    | zpx  |    2 |      4 |      2 | 0x74 | NOP    | zpx  |    2 |      4 |      2 |
| 0x55 | EOR    | zpx  |    2 |      4 |      2 | 0x75 | ADC    | zpx  |    2 |      4 |      2 |
| 0x56 | LSR    | zpx  |    2 |      6 |      2 | 0x76 | ROR    | zpx  |    2 |      6 |      2 |
| 0x57 | SRE    | zpx  |    0 |      6 |      0 | 0x77 | RRA    | zpx  |    0 |      6 |      0 |
| 0x58 | CLI    | imp  |    1 |      2 |      1 | 0x78 | SEI    | imp  |    1 |      2 |      1 |
| 0x59 | EOR    | aby  |    3 |      4 |      3 | 0x79 | ADC    | aby  |    3 |      4 |      3 |
| 0x5A | NOP    | imp  |    1 |      2 |      1 | 0x7A | NOP    | imp  |    1 |      2 |      1 |
| 0x5B | SRE    | aby  |    0 |      7 |      0 | 0x7B | RRA    | aby  |    0 |      7 |      0 |
| 0x5C | NOP    | abx  |    3 |      4 |      3 | 0x7C | NOP    | abx  |    3 |      4 |      3 |
| 0x5D | EOR    | abx  |    3 |      4 |      3 | 0x7D | ADC    | abx  |    3 |      4 |      3 |
| 0x5E | LSR    | abx  |    3 |      7 |      3 | 0x7E | ROR    | abx  |    3 |      7 |      3 |
| 0x5F | SRE    | abx  |    0 |      7 |      0 | 0x7F | RRA    | abx  |    0 |      7 |      0 |
|------+--------+------+------+--------+--------+------+--------+------+------+--------+--------|
|  Hex | Opcode | mode | size | cycles |   page |  Hex | Opcode | mode | size | cycles |   page |
|      |        |      |      |        | cycles |      |        |      |      |        | cycles |
|------+--------+------+------+--------+--------+------+--------+------+------+--------+--------|
| 0x80 | NOP    | imm  |    2 |      2 |      2 | 0xA0 | LDY    | imm  |    2 |      2 |      2 |
| 0x81 | STA    | izx  |    2 |      6 |      2 | 0xA1 | LDA    | izx  |    2 |      6 |      2 |
| 0x82 | NOP    | imm  |    0 |      2 |      0 | 0xA2 | LDX    | imm  |    2 |      2 |      2 |
| 0x83 | SAX    | izx  |    0 |      6 |      0 | 0xA3 | LAX    | izx  |    0 |      6 |      0 |
| 0x84 | STY    | zpg  |    2 |      3 |      2 | 0xA4 | LDY    | zpg  |    2 |      3 |      2 |
| 0x85 | STA    | zpg  |    2 |      3 |      2 | 0xA5 | LDA    | zpg  |    2 |      3 |      2 |
| 0x86 | STX    | zpg  |    2 |      3 |      2 | 0xA6 | LDX    | zpg  |    2 |      3 |      2 |
| 0x87 | SAX    | zpg  |    0 |      3 |      0 | 0xA7 | LAX    | zpg  |    0 |      3 |      0 |
| 0x88 | DEY    | imp  |    1 |      2 |      1 | 0xA8 | TAY    | imp  |    1 |      2 |      1 |
| 0x89 | NOP    | imm  |    0 |      2 |      0 | 0xA9 | LDA    | imm  |    2 |      2 |      2 |
| 0x8A | TXA    | imp  |    1 |      2 |      1 | 0xAA | TAX    | imp  |    1 |      2 |      1 |
| 0x8B | XAA    | imm  |    0 |      2 |      0 | 0xAB | LAX    | imm  |    0 |      2 |      0 |
| 0x8C | STY    | abs  |    3 |      4 |      3 | 0xAC | LDY    | abs  |    3 |      4 |      3 |
| 0x8D | STA    | abs  |    3 |      4 |      3 | 0xAD | LDA    | abs  |    3 |      4 |      3 |
| 0x8E | STX    | abs  |    3 |      4 |      3 | 0xAE | LDX    | abs  |    3 |      4 |      3 |
| 0x8F | SAX    | abs  |    0 |      4 |      0 | 0xAF | LAX    | abs  |    0 |      4 |      0 |
| 0x90 | BCC    | rel  |    2 |      2 |      2 | 0xB0 | BCS    | rel  |    2 |      2 |      2 |
| 0x91 | STA    | izy  |    2 |      6 |      2 | 0xB1 | LDA    | izy  |    2 |      5 |      2 |
| 0x92 | KIL    | imp  |    0 |      2 |      0 | 0xB2 | KIL    | imp  |    0 |      2 |      0 |
| 0x93 | AHX    | izy  |    0 |      6 |      0 | 0xB3 | LAX    | izy  |    0 |      5 |      0 |
| 0x94 | STY    | zpx  |    2 |      4 |      2 | 0xB4 | LDY    | zpx  |    2 |      4 |      2 |
| 0x95 | STA    | zpx  |    2 |      4 |      2 | 0xB5 | LDA    | zpx  |    2 |      4 |      2 |
| 0x96 | STX    | zpy  |    2 |      4 |      2 | 0xB6 | LDX    | zpy  |    2 |      4 |      2 |
| 0x97 | SAX    | zpy  |    0 |      4 |      0 | 0xB7 | LAX    | zpy  |    0 |      4 |      0 |
| 0x98 | TYA    | imp  |    1 |      2 |      1 | 0xB8 | CLV    | imp  |    1 |      2 |      1 |
| 0x99 | STA    | aby  |    3 |      5 |      3 | 0xB9 | LDA    | aby  |    3 |      4 |      3 |
| 0x9A | TXS    | imp  |    1 |      2 |      1 | 0xBA | TSX    | imp  |    1 |      2 |      1 |
| 0x9B | TAS    | aby  |    0 |      5 |      0 | 0xBB | LAS    | aby  |    0 |      4 |      0 |
| 0x9C | SHY    | abx  |    0 |      5 |      0 | 0xBC | LDY    | abx  |    3 |      4 |      3 |
| 0x9D | STA    | abx  |    3 |      5 |      3 | 0xBD | LDA    | abx  |    3 |      4 |      3 |
| 0x9E | SHX    | aby  |    0 |      5 |      0 | 0xBE | LDX    | aby  |    3 |      4 |      3 |
| 0x9F | AHX    | aby  |    0 |      5 |      0 | 0xBF | LAX    | aby  |    0 |      4 |      0 |
|------+--------+------+------+--------+--------+------+--------+------+------+--------+--------|
|  Hex | Opcode | mode | size | cycles |   page |  Hex | Opcode | mode | size | cycles |   page |
|      |        |      |      |        | cycles |      |        |      |      |        | cycles |
|------+--------+------+------+--------+--------+------+--------+------+------+--------+--------|
| 0xC0 | CPY    | imm  |    2 |      2 |      2 | 0xE0 | CPX    | imm  |    2 |      2 |      2 |
| 0xC1 | CMP    | izx  |    2 |      6 |      2 | 0xE1 | SBC    | izx  |    2 |      6 |      2 |
| 0xC2 | NOP    | imm  |    0 |      2 |      0 | 0xE2 | NOP    | imm  |    0 |      2 |      0 |
| 0xC3 | DCP    | izx  |    0 |      8 |      0 | 0xE3 | ISC    | izx  |    0 |      8 |      0 |
| 0xC4 | CPY    | zpg  |    2 |      3 |      2 | 0xE4 | CPX    | zpg  |    2 |      3 |      2 |
| 0xC5 | CMP    | zpg  |    2 |      3 |      2 | 0xE5 | SBC    | zpg  |    2 |      3 |      2 |
| 0xC6 | DEC    | zpg  |    2 |      5 |      2 | 0xE6 | INC    | zpg  |    2 |      5 |      2 |
| 0xC7 | DCP    | zpg  |    0 |      5 |      0 | 0xE7 | ISC    | zpg  |    0 |      5 |      0 |
| 0xC8 | INY    | imp  |    1 |      2 |      1 | 0xE8 | INX    | imp  |    1 |      2 |      1 |
| 0xC9 | CMP    | imm  |    2 |      2 |      2 | 0xE9 | SBC    | imm  |    2 |      2 |      2 |
| 0xCA | DEX    | imp  |    1 |      2 |      1 | 0xEA | NOP    | imp  |    1 |      2 |      1 |
| 0xCB | AXS    | imm  |    0 |      2 |      0 | 0xEB | SBC    | imm  |    0 |      2 |      0 |
| 0xCC | CPY    | abs  |    3 |      4 |      3 | 0xEC | CPX    | abs  |    3 |      4 |      3 |
| 0xCD | CMP    | abs  |    3 |      4 |      3 | 0xED | SBC    | abs  |    3 |      4 |      3 |
| 0xCE | DEC    | abs  |    3 |      6 |      3 | 0xEE | INC    | abs  |    3 |      6 |      3 |
| 0xCF | DCP    | abs  |    0 |      6 |      0 | 0xEF | ISC    | abs  |    0 |      6 |      0 |
| 0xD0 | BNE    | rel  |    2 |      2 |      2 | 0xF0 | BEQ    | rel  |    2 |      2 |      2 |
| 0xD1 | CMP    | izy  |    2 |      5 |      2 | 0xF1 | SBC    | izy  |    2 |      5 |      2 |
| 0xD2 | KIL    | imp  |    0 |      2 |      0 | 0xF2 | KIL    | imp  |    0 |      2 |      0 |
| 0xD3 | DCP    | izy  |    0 |      8 |      0 | 0xF3 | ISC    | izy  |    0 |      8 |      0 |
| 0xD4 | NOP    | zpx  |    2 |      4 |      2 | 0xF4 | NOP    | zpx  |    2 |      4 |      2 |
| 0xD5 | CMP    | zpx  |    2 |      4 |      2 | 0xF5 | SBC    | zpx  |    2 |      4 |      2 |
| 0xD6 | DEC    | zpx  |    2 |      6 |      2 | 0xF6 | INC    | zpx  |    2 |      6 |      2 |
| 0xD7 | DCP    | zpx  |    0 |      6 |      0 | 0xF7 | ISC    | zpx  |    0 |      6 |      0 |
| 0xD8 | CLD    | imp  |    1 |      2 |      1 | 0xF8 | SED    | imp  |    1 |      2 |      1 |
| 0xD9 | CMP    | aby  |    3 |      4 |      3 | 0xF9 | SBC    | aby  |    3 |      4 |      3 |
| 0xDA | NOP    | imp  |    1 |      2 |      1 | 0xFA | NOP    | imp  |    1 |      2 |      1 |
| 0xDB | DCP    | aby  |    0 |      7 |      0 | 0xFB | ISC    | aby  |    0 |      7 |      0 |
| 0xDC | NOP    | abx  |    3 |      4 |      3 | 0xFC | NOP    | abx  |    3 |      4 |      3 |
| 0xDD | CMP    | abx  |    3 |      4 |      3 | 0xFD | SBC    | abx  |    3 |      4 |      3 |
| 0xDE | DEC    | abx  |    3 |      7 |      3 | 0xFE | INC    | abx  |    3 |      7 |      3 |
| 0xDF | DCP    | abx  |    0 |      7 |      0 | 0xFF | ISC    | abx  |    0 |      7 |      0 |
|------+--------+------+------+--------+--------+------+--------+------+------+--------+--------|




    // List of all the opcodes
  private String[] instructionNames = { //
      "BRK", "ORA", "KIL", "SLO", "NOP", "ORA", "ASL", "SLO", // 00-08
      "PHP", "ORA", "ASL", "ANC", "NOP", "ORA", "ASL", "SLO", // 08-16
      "BPL", "ORA", "KIL", "SLO", "NOP", "ORA", "ASL", "SLO", // 16-32
      "CLC", "ORA", "NOP", "SLO", "NOP", "ORA", "ASL", "SLO", // 24
      "JSR", "AND", "KIL", "RLA", "BIT", "AND", "ROL", "RLA", // 32
      "PLP", "AND", "ROL", "ANC", "BIT", "AND", "ROL", "RLA", // 40
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
    abs, abx, aby, acc, imm, imp, izx, ind, //
    izy, rel, zpg, // zero page
    zpx, zpy
  }

  private Modes[] instructionModes = { //
      Modes.imp, Modes.izx, Modes.imp, Modes.izx, Modes.zpg, Modes.zpg,
      Modes.zpg, Modes.zpg, Modes.imp, Modes.imm, Modes.acc, Modes.imm,
      Modes.abs, Modes.abs, Modes.abs, Modes.abs, Modes.rel, Modes.izy,
      Modes.imp, Modes.izy, Modes.zpx, Modes.zpx, Modes.zpx, Modes.zpx,
      Modes.imp, Modes.aby, Modes.imp, Modes.aby, Modes.abx, Modes.abx,
      Modes.abx, Modes.abx, Modes.abs, Modes.izx, Modes.imp, Modes.izx,
      Modes.zpg, Modes.zpg, Modes.zpg, Modes.zpg, Modes.imp, Modes.imm,
      Modes.acc, Modes.imm, Modes.abs, Modes.abs, Modes.abs, Modes.abs,
      Modes.rel, Modes.izy, Modes.imp, Modes.izy, Modes.zpx, Modes.zpx,
      Modes.zpx, Modes.zpx, Modes.imp, Modes.aby, Modes.imp, Modes.aby,
      Modes.abx, Modes.abx, Modes.abx, Modes.abx, Modes.imp, Modes.izx,
      Modes.imp, Modes.izx, Modes.zpg, Modes.zpg, Modes.zpg, Modes.zpg,
      Modes.imp, Modes.imm, Modes.acc, Modes.imm, Modes.abs, Modes.abs,
      Modes.abs, Modes.abs, Modes.rel, Modes.izy, Modes.imp, Modes.izy,
      Modes.zpx, Modes.zpx, Modes.zpx, Modes.zpx, Modes.imp, Modes.aby,
      Modes.imp, Modes.aby, Modes.abx, Modes.abx, Modes.abx, Modes.abx,
      Modes.imp, Modes.izx, Modes.imp, Modes.izx, Modes.zpg, Modes.zpg,
      Modes.zpg, Modes.zpg, Modes.imp, Modes.imm, Modes.acc, Modes.imm,
      Modes.ind, Modes.abs, Modes.abs, Modes.abs, Modes.rel, Modes.izy,
      Modes.imp, Modes.izy, Modes.zpx, Modes.zpx, Modes.zpx, Modes.zpx,
      Modes.imp, Modes.aby, Modes.imp, Modes.aby, Modes.abx, Modes.abx,
      Modes.abx, Modes.abx, Modes.imm, Modes.izx, Modes.imm, Modes.izx,
      Modes.zpg, Modes.zpg, Modes.zpg, Modes.zpg, Modes.imp, Modes.imm,
      Modes.imp, Modes.imm, Modes.abs, Modes.abs, Modes.abs, Modes.abs,
      Modes.rel, Modes.izy, Modes.imp, Modes.izy, Modes.zpx, Modes.zpx,
      Modes.zpy, Modes.zpy, Modes.imp, Modes.aby, Modes.imp, Modes.aby,
      Modes.abx, Modes.abx, Modes.aby, Modes.aby, Modes.imm, Modes.izx,
      Modes.imm, Modes.izx, Modes.zpg, Modes.zpg, Modes.zpg, Modes.zpg,
      Modes.imp, Modes.imm, Modes.imp, Modes.imm, Modes.abs, Modes.abs,
      Modes.abs, Modes.abs, Modes.rel, Modes.izy, Modes.imp, Modes.izy,
      Modes.zpx, Modes.zpx, Modes.zpy, Modes.zpy, Modes.imp, Modes.aby,
      Modes.imp, Modes.aby, Modes.abx, Modes.abx, Modes.aby, Modes.aby,
      Modes.imm, Modes.izx, Modes.imm, Modes.izx, Modes.zpg, Modes.zpg,
      Modes.zpg, Modes.zpg, Modes.imp, Modes.imm, Modes.imp, Modes.imm,
      Modes.abs, Modes.abs, Modes.abs, Modes.abs, Modes.rel, Modes.izy,
      Modes.imp, Modes.izy, Modes.zpx, Modes.zpx, Modes.zpx, Modes.zpx,
      Modes.imp, Modes.aby, Modes.imp, Modes.aby, Modes.abx, Modes.abx,
      Modes.abx, Modes.abx, Modes.imm, Modes.izx, Modes.imm, Modes.izx,
      Modes.zpg, Modes.zpg, Modes.zpg, Modes.zpg, Modes.imp, Modes.imm,
      Modes.imp, Modes.imm, Modes.abs, Modes.abs, Modes.abs, Modes.abs,
      Modes.rel, Modes.izy, Modes.imp, Modes.izy, Modes.zpx, Modes.zpx,
      Modes.zpx, Modes.zpx, Modes.imp, Modes.aby, Modes.imp, Modes.aby,
      Modes.abx, Modes.abx, Modes.abx, Modes.abx, };

  private byte[] instructionSizes = { 1, 2, 0, 0, 2, 2, 2, 0, 1, 2, 1, 0, 3, 3,
      3, 0, 2, 2, 0, 0, 2, 2, 2, 0, 1, 3, 1, 0, 3, 3, 3, 0, 3, 2, 0, 0, 2, 2, 2,
      0, 1, 2, 1, 0, 3, 3, 3, 0, 2, 2, 0, 0, 2, 2, 2, 0, 1, 3, 1, 0, 3, 3, 3, 0,
      1, 2, 0, 0, 2, 2, 2, 0, 1, 2, 1, 0, 3, 3, 3, 0, 2, 2, 0, 0, 2, 2, 2, 0, 1,
      3, 1, 0, 3, 3, 3, 0, 1, 2, 0, 0, 2, 2, 2, 0, 1, 2, 1, 0, 3, 3, 3, 0, 2, 2,
      0, 0, 2, 2, 2, 0, 1, 3, 1, 0, 3, 3, 3, 0, 2, 2, 0, 0, 2, 2, 2, 0, 1, 0, 1,
      0, 3, 3, 3, 0, 2, 2, 0, 0, 2, 2, 2, 0, 1, 3, 1, 0, 0, 3, 0, 0, 2, 2, 2, 0,
      2, 2, 2, 0, 1, 2, 1, 0, 3, 3, 3, 0, 2, 2, 0, 0, 2, 2, 2, 0, 1, 3, 1, 0, 3,
      3, 3, 0, 2, 2, 0, 0, 2, 2, 2, 0, 1, 2, 1, 0, 3, 3, 3, 0, 2, 2, 0, 0, 2, 2,
      2, 0, 1, 3, 1, 0, 3, 3, 3, 0, 2, 2, 0, 0, 2, 2, 2, 0, 1, 2, 1, 0, 3, 3, 3,
      0, 2, 2, 0, 0, 2, 2, 2, 0, 1, 3, 1, 0, 3, 3, 3, 0, };

  private byte[] instructionCycles = { 7, 6, 2, 8, 3, 3, 5, 5, 3, 2, 2, 2, 4, 4,
      6, 6, 2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7, 6, 6, 2, 8, 3, 3, 5,
      5, 4, 2, 2, 2, 4, 4, 6, 6, 2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
      6, 6, 2, 8, 3, 3, 5, 5, 3, 2, 2, 2, 3, 4, 6, 6, 2, 5, 2, 8, 4, 4, 6, 6, 2,
      4, 2, 7, 4, 4, 7, 7, 6, 6, 2, 8, 3, 3, 5, 5, 4, 2, 2, 2, 5, 4, 6, 6, 2, 5,
      2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7, 2, 6, 2, 6, 3, 3, 3, 3, 2, 2, 2,
      2, 4, 4, 4, 4, 2, 6, 2, 6, 4, 4, 4, 4, 2, 5, 2, 5, 5, 5, 5, 5, 2, 6, 2, 6,
      3, 3, 3, 3, 2, 2, 2, 2, 4, 4, 4, 4, 2, 5, 2, 5, 4, 4, 4, 4, 2, 4, 2, 4, 4,
      4, 4, 4, 2, 6, 2, 8, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6, 2, 5, 2, 8, 4, 4,
      6, 6, 2, 4, 2, 7, 4, 4, 7, 7, 2, 6, 2, 8, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6,
      6, 2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7, };

  private byte[] instructionPageCycles = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0,
      0, 0, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0,
      1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0,
      0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, };
  
  /**
   * print current state of 6502 chip
   */
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
        this.PC, this.SP, this.A, this.X, this.Y, this.C, this.Z, this.I,
        this.D, this.B, this.U, this.V, this.N, this.interrupt);
    format.close();
    return sb.toString();
  }
  
  // NMI - Non-Maskable Interrupt
  public void nmi() {
    this.push16(this.PC);
    this.php(0, this.PC, null);
    this.PC = this.read16(0xFFFA);
    this.I = 1;
    this.cycles += 7;
  }
  
  // IRQ - IRQ Interrupt
  public void irq() {
    this.push16(this.PC);
    this.php(0, this.PC, null);
    this.PC = this.read16(0xFFFE);
    this.I = 1;
    this.cycles += 7;
  }

  private void handleInterrupt() {
    // Attend interrupt if it is set
    switch (this.interrupt) {
      case NMI:
        this.nmi();
        break;
      case IRQ:
        this.irq();
        break;
      default:
        break;
    }
    // clear interrupt after handling
    this.interrupt = Interrupt.NONE;
    
  }
  
  /**
   * Execute one CPU instruction
   *
   * @return number of cycles executed
   * @throws Exception
   */
  public long step() throws Exception {

    // wait till stall cycles are complete
    if (this.stall > 0) {
      this.stall--;
      return 1;
    }

    long cycles = this.cycles;
    this.handleInterrupt();
    Instruction instr = this.fetchInstruction();

    // increment PC based on opcode
    this.PC += this.instructionSizes[instr.opcode];
    // increment cycles based on opcode
    this.cycles += this.instructionCycles[instr.opcode];
    if (instr.pageCrossed) {
      cycles += this.instructionPageCycles[instr.opcode];
    }

    System.out.println(this);
    this.executeInstrutcion(instr);
    System.out.println(this);

    return this.cycles - cycles;
  }
  
  private void executeInstrutcion(Instruction instr) {
    
    switch (instr.opcode) {
      case 0x00:
        this.brk(instr.address, instr.opcode, instr.mode);
        break;
      case 0xA1:
      case 0xA5:
      case 0xA9:
      case 0xAD:
      case 0xB1:
      case 0xB5:
      case 0xB9:
      case 0xBD:
        this.lda(instr.address, instr.opcode, instr.mode);
        break;

      default:
        break;
    }
    
  }
  
  private Instruction fetchInstruction() {
    Instruction instr = new Instruction();
    // fetch instruction
    // step 1: fetch Opcode
    // step 2: fetch operand

    // step 1: fetch Opcode
    // assume PC is set correctly. Next read is an opcode value
    instr.opcode = this.read(this.PC) & 0xff;
    // given opcode corresponds to only one addressing mode
    instr.mode = this.instructionModes[instr.opcode];
    
    // step 2: fetch operand
    // instr.address;
    instr.pageCrossed = false;

    // read operands based on addressing mode. This does not depend on opcode.
    switch (instr.mode) {
      case abs:
        instr.address = this.read16(this.PC + 1);
        break;
      case abx:
        instr.address = this.read16(this.PC + 1) + this.X;
        instr.pageCrossed = this.pagesDiffer(instr.address - this.X,
            instr.address);
        break;
      case aby:
        instr.address = this.read16(this.PC + 1) + this.Y;
        instr.pageCrossed = this.pagesDiffer(instr.address - this.Y,
            instr.address);
        break;
      case acc:
        instr.address = 0;
        break;
      case imm:
        instr.address = this.PC + 1;
        break;
      case imp:
        instr.address = 0;
        break;
      case izx:
        instr.address = this.read16bug(this.read(this.PC + 1) + this.X);
        break;
      case ind:
        instr.address = this.read16bug(this.read16(this.PC + 1));
        break;
      case izy:
        instr.address = this.read16bug(this.read(this.PC + 1)) + this.Y;
        instr.pageCrossed = this.pagesDiffer(instr.address - this.Y,
            instr.address);
        break;
      case rel:
        int offset = this.read(this.PC + 1) & 0xFF;
        if (offset < 0x80) {
          instr.address = this.PC + 2 + offset;
        } else {
          // way to simulate signed 8 bit operations
          instr.address = (this.PC + 2 + offset) - 0x100;
        }
        break;
      case zpg:
        System.out.println("here1");
        instr.address = this.read(this.PC + 1);
        // instr.address = this.PC + 1;
        break;
      case zpx:
        instr.address = this.read(this.PC + 1) + this.X;
        break;
      case zpy:
        instr.address = this.read(this.PC + 1) + this.Y;
        break;
      default:
        instr.address = 0;
        break;
    }
    return instr;
  }

  public byte read(int address) {
    return this.m.read(address);
  }

  public void write(int address, int value) {
    this.m.write(address & 0xff, value & 0xff);
  }

  // Read16 reads two memory locations using read to return a double-word
  // value
  private int read16(int address) {
    int lo = this.read(address) & 0xff;
    int hi = this.read(address + 1) & 0xff;
    return ((hi << 8) | lo) & 0xffff;
  }

  /**
   * Reset 6502 chip
   */
  public void reset() {
    System.out.println("********** Resetting CPU ************");
    
    // read instruction at location 0xfffc
    this.PC = this.read16(0xfffc);
    System.out.printf("Reset CPU PC: 0x%x\n", this.PC);

    // Initialize stack pointer
    this.SP = (byte) 0xFD;
    
    // Set flags I = 1 and U = 1
    this.setFlags(0x24);
    
    System.out.println("********** CPU Reset Complete ************");
  }
  
  public CPU6502(CPUMemMapper m) {
    this.m = m;
    this.reset();
  }

  public static void main(String[] args) throws Exception {
    CPU6502 cpu = new CPU6502(new SimpleMemMapper());
    cpu.step();
    for (int i = 0; i < 256; i++) {
      // System.out.printf("%s,%s,%d,%d\n", cpu.instructionNames[i],
      // cpu.instructionModes[i], cpu.instructionCycles[i],
      // cpu.instructionSizes[i]);
    }
  }

  // addBranchCycles adds a cycle for taking a branch and adds another cycle
  // if the branch jumps to a new page
  public void addBranchCycles(int address, int PC, Modes mode) {
    this.cycles++;
    if (this.pagesDiffer(PC, address)) {
      this.cycles++;
    }
  }

  public void compare(byte a, byte b) {
    this.setZN((byte) (a - b));
    if (a >= b) {
      this.C = 1;
    } else {
      this.C = 0;
    }
  }

  // read16bug emulates a 6502 bug that caused the low byte to wrap without
  // incrementing the high byte
  private int read16bug(int address) {
    int a = address;
    int b = (a & 0xFF00) | ((a) + 1);
    int lo = this.read(a);
    int hi = this.read(b);
    return ((hi) << 8) | (lo);
  }

  // push pushes a byte onto the stack
  public void push(byte value) {
    this.write(0x100 | this.SP, value & 0xff);
    this.SP--;
  }

  // pull pops a byte from the stack
  private byte pull() {
    this.SP++;
    return this.read(0x100 | (this.SP));
  }

  // push16 pushes two bytes onto the stack
  public void push16(int value) {
    byte hi = (byte) ((value >> 8) & 0xff);
    byte lo = (byte) (value & 0xff);
    this.push(hi);
    this.push(lo);
  }

  // pull16 pops two bytes from the stack
  private int pull16() {
    int lo = (this.pull());
    int hi = (this.pull());
    return (hi << 8) | lo;
  }

  // Flags returns the processor status flags
  private byte getFlags() {
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
    this.C = (byte) ((flags >> 0) & 0x1);
    this.Z = (byte) ((flags >> 1) & 0x1);
    this.I = (byte) ((flags >> 2) & 0x1);
    this.D = (byte) ((flags >> 3) & 0x1);
    this.B = (byte) ((flags >> 4) & 0x1);
    this.U = (byte) ((flags >> 5) & 0x1);
    this.V = (byte) ((flags >> 6) & 0x1);
    this.N = (byte) ((flags >> 7) & 0x1);
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
    this.setZ((byte) (value & 0xff));
    this.setN((byte) (value & 0xff));
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

  // ADC - Add with Carry
  public void adc(int address, int PC, Modes mode) {
    byte a = this.A;
    byte b = this.read(address);
    byte c = this.C;
    this.A = (byte) (a + b + c);
    this.setZN(this.A);
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
    this.A = (byte) (this.A & this.read(address));
    this.setZN(this.A);
  }

  // ASL - Arithmetic Shift Left
  public void asl(int address, int PC, Modes mode) {
    if (mode == Modes.acc) {
      this.C = (byte) ((this.A >> 7) & 1);
      this.A <<= 1;
      this.setZN(this.A);
    } else {
      byte value = this.read(address);
      this.C = (byte) ((value >> 7) & 1);
      value <<= 1;
      this.write(address, value);
      this.setZN(value);
    }
  }

  // BCC - Branch if Carry Clear
  public void bcc(int address, int PC, Modes mode) {
    if (this.C == 0) {
      PC = address;
      this.addBranchCycles(address, PC, mode);
    }
  }

  // BCS - Branch if Carry Set
  public void bcs(int address, int PC, Modes mode) {
    if (this.C != 0) {
      PC = address;
      this.addBranchCycles(address, PC, mode);
    }
  }

  // BEQ - Branch if Equal
  public void beq(int address, int PC, Modes mode) {
    if (this.Z != 0) {
      PC = address;
      this.addBranchCycles(address, PC, mode);
    }
  }

  // BIT - Bit Test
  public void bit(int address, int PC, Modes mode) {
    byte value = this.read(address);
    this.V = (byte) ((value >> 6) & 1);
    this.setZ((byte) (value & this.A));
    this.setN(value);
  }

  // BMI - Branch if Minus
  public void bmi(int address, int PC, Modes mode) {
    if (this.N != 0) {
      PC = address;
      this.addBranchCycles(address, PC, mode);
    }
  }

  // BNE - Branch if Not Equal
  public void bne(int address, int PC, Modes mode) {
    if (this.Z == 0) {
      PC = address;
      this.addBranchCycles(address, PC, mode);
    }
  }

  // BPL - Branch if Positive
  public void bpl(int address, int PC, Modes mode) {
    if (this.N == 0) {
      // System.out.println("Here");
      this.PC = address;
      this.addBranchCycles(address, this.PC, mode);
    }
  }

  // BRK - Force Interrupt
  public void brk(int address, int PC, Modes mode) {
    this.push16(PC);
    this.php(address, PC, mode);
    this.sei(address, PC, mode);
    this.PC = this.read16(0xFFFE);
  }

  // BVC - Branch if Overflow Clear
  public void bvc(int address, int PC, Modes mode) {
    if (this.V == 0) {
      PC = address;
      this.addBranchCycles(address, PC, mode);
    }
  }

  // BVS - Branch if Overflow Set
  public void bvs(int address, int PC, Modes mode) {
    if (this.V != 0) {
      PC = address;
      this.addBranchCycles(address, PC, mode);
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
    byte value = this.read(address);
    this.compare(this.A, value);
  }

  // CPX - Compare X Register
  public void cpx(int address, int PC, Modes mode) {
    byte value = this.read(address);
    this.compare(this.X, value);
  }

  // CPY - Compare Y Register
  public void cpy(int address, int PC, Modes mode) {
    byte value = this.read(address);
    this.compare(this.Y, value);
  }

  // DEC - Decrement Memory
  public void dec(int address, int PC, Modes mode) {
    byte value = (byte) (this.read(address) - 1);
    this.write(address, value);
    this.setZN(value);
  }

  // DEX - Decrement X Register
  public void dex(int address, int PC, Modes mode) {
    this.X--;
    this.setZN(this.X);
  }

  // DEY - Decrement Y Register
  public void dey(int address, int PC, Modes mode) {
    this.Y--;
    this.setZN(this.Y);
  }

  // EOR - Exclusive OR
  public void eor(int address, int PC, Modes mode) {
    this.A = (byte) (this.A ^ this.read(address));
    this.setZN(this.A);
  }

  // INC - Increment Memory
  public void inc(int address, int PC, Modes mode) {
    byte value = (byte) (this.read(address) + 1);
    this.write(address, value);
    this.setZN(value);
  }

  // INX - Increment X Register
  public void inx(int address, int PC, Modes mode) {
    this.X++;
    this.setZN(this.X);
  }

  // INY - Increment Y Register
  public void iny(int address, int PC, Modes mode) {
    this.Y++;
    this.setZN(this.Y);
  }

  // JMP - Jump
  public void jmp(int address, int PC, Modes mode) {
    this.PC = address;
  }

  // JSR - Jump to Subroutine
  public void jsr(int address, int PC, Modes mode) {
    this.push16(PC - 1);
    this.PC = address;
  }

  // LDA - Load Accumulator
  public void lda(int address, int PC, Modes mode) {
    this.A = this.read(address);
    this.setZN(this.A);
  }

  // LDX - Load X Register
  public void ldx(int address, int PC, Modes mode) {
    this.X = this.read(address);
    this.setZN(this.X);
  }

  // LDY - Load Y Register
  public void ldy(int address, int PC, Modes mode) {
    this.Y = this.read(address);
    this.setZN(this.Y);
  }

  // LSR - Logical Shift Right
  public void lsr(int address, int PC, Modes mode) {
    if (mode == Modes.acc) {
      this.C = (byte) (this.A & 1);
      this.A >>= 1;
      this.setZN(this.A);
    } else {
      byte value = this.read(address);
      this.C = (byte) (value & 1);
      value >>= 1;
      this.write(address, value);
      this.setZN(value);
    }
  }

  // NOP - No Operation
  public void nop(int address, int PC, Modes mode) {
  }

  // ORA - Logical Inclusive OR
  public void ora(int address, int PC, Modes mode) {
    this.A = (byte) (this.A | this.read(address));
    this.setZN(this.A);
  }

  // PHA - Push Accumulator
  public void pha(int address, int PC, Modes mode) {
    this.push(this.A);
  }

  // PHP - Push Processor Status
  public void php(int address, int PC, Modes mode) {
    this.push((byte) (this.getFlags() | 0x10));
  }

  // PLA - Pull Accumulator
  public void pla(int address, int PC, Modes mode) {
    this.A = this.pull();
    this.setZN(this.A);
  }

  // PLP - Pull Processor Status
  public void plp(int address, int PC, Modes mode) {
    this.setFlags((this.pull() & 0xEF) | 0x20);
  }

  // ROL - Rotate Left
  public void rol(int address, int PC, Modes mode) {
    if (mode == Modes.acc) {
      byte c = this.C;
      this.C = (byte) ((this.A >> 7) & 1);
      this.A = (byte) ((this.A << 1) | c);
      this.setZN(this.A);
    } else {
      byte c = this.C;
      byte value = this.read(address);
      this.C = (byte) ((value >> 7) & 1);
      value = (byte) ((value << 1) | c);
      this.write(address, value);
      this.setZN(value);
    }
  }

  // ROR - Rotate Right
  public void ror(int address, int PC, Modes mode) {
    if (mode == Modes.acc) {
      byte c = this.C;
      this.C = (byte) (this.A & 1);
      this.A = (byte) ((this.A >> 1) | (c << 7));
      this.setZN(this.A);
    } else {
      byte c = this.C;
      byte value = this.read(address);
      this.C = (byte) (value & 1);
      value = (byte) ((value >> 1) | (c << 7));
      this.write(address, value);
      this.setZN(value);
    }
  }

  // RTI - Return from Interrupt
  public void rti(int address, int PC, Modes mode) {
    this.setFlags((this.pull() & 0xEF) | 0x20);
    PC = this.pull16();
  }

  // RTS - Return from Subroutine
  public void rts(int address, int PC, Modes mode) {
    PC = this.pull16() + 1;
  }

  // SBC - Subtract with Carry
  public void sbc(int address, int PC, Modes mode) {
    byte a = this.A;
    byte b = this.read(address);
    byte c = this.C;
    this.A = (byte) (a - b - (1 - c));
    this.setZN(this.A);
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
    this.write(address, this.A);
  }

  // STX - Store X Register
  public void stx(int address, int PC, Modes mode) {
    this.write(address, this.X);
  }

  // STY - Store Y Register
  public void sty(int address, int PC, Modes mode) {
    this.write(address, this.Y);
  }

  // TAX - Transfer Accumulator to X
  public void tax(int address, int PC, Modes mode) {
    this.X = this.A;
    this.setZN(this.X);
  }

  // TAY - Transfer Accumulator to Y
  public void tay(int address, int PC, Modes mode) {
    this.Y = this.A;
    this.setZN(this.Y);
  }

  // TSX - Transfer Stack Pointer to X
  public void tsx(int address, int PC, Modes mode) {
    this.X = this.SP;
    this.setZN(this.X);
  }

  // TXA - Transfer X to Accumulator
  public void txa(int address, int PC, Modes mode) {
    this.A = this.X;
    this.setZN(this.A);
  }

  // TXS - Transfer X to Stack Pointer
  public void txs(int address, int PC, Modes mode) {
    this.SP = this.X;
  }

  // TYA - Transfer Y to Accumulator
  public void tya(int address, int PC, Modes mode) {
    this.A = this.Y;
    this.setZN(this.A);
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
}
