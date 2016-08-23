#6502 emulator
This is a 6502 https://en.wikipedia.org/wiki/MOS_Technology_6502 emulator written in Java.
It is inspired by a CPU written by Fogleman in Go (https://github.com/fogleman/nes).
There are still some bugs in this and some edge cases are not handled 
(not yet ready to be used in actual implementation of NES or other 6502 base system).
However for all other practical and educational purposes it works well.
The user needs to implement a simple MemMapper, which is essentially to provide set of memory (array) to read from and write to.