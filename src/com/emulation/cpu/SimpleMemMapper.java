package com.emulation.cpu;

public class SimpleMemMapper implements CPUMemMapper {
  // max memory size is 2^16
  // can address only 0xffff mem locations
  byte[] mem = new byte[0xffff];
  
  public SimpleMemMapper() {
    this.mem[0] = 0;
    this.mem[0x3412] = (byte) 0xa9;
    this.mem[0x3413] = 0x15;
    this.mem[0xfffc] = 0x12;
    this.mem[0xfffd] = 0x34;
  }
  
  @Override
  public byte read(int address) {
    return this.mem[address & 0xffff];
  }

  @Override
  public void write(int address, int value) {
    this.mem[address & 0xffff] = (byte) (value & 0xff);
  }

}
