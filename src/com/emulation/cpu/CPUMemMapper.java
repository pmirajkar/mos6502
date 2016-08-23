package com.emulation.cpu;


public interface CPUMemMapper {
	public byte read(int address);
	public void write(int address, int value);
}
