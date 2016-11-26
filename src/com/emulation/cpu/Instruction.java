package com.emulation.cpu;

import com.emulation.cpu.CPU6502.Modes;

public class Instruction {

  public int     opcode;
  public Modes   mode;
  public boolean pageCrossed;
  public int     address;
  
}
