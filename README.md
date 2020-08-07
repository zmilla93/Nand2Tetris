# Nand2Tetris
Nand2Tetris is a computer science course by the MIT Press. It takes you through the steps of building a computer starting with nothing more than simple NAND gates. It then guides you through the process of building an assembler, virtual machine, and compiler for an objected oriented programming language. This repo contain's only my own work and excludes the extra course files.<br>

**[Course Website](https://www.nand2tetris.org/)**

## The Hardware
The hardware design used by the course is named HACK. Circuits are designed using a Hardware Description Language (HLD) with the `.hdl` extension. Machine code has the `.hack` extension.

#### Computer specs
- 16 bit
- 32K RAM
- 512x256 black & white screen
- Keyboard

**[Hardware Code](https://github.com/zmilla93/Nand2Tetris/tree/master/hardware)**

<details>
  <summary>Chapter 1 - Boolean Logic</summary>
    NOT Gate<br>
    OR Gate<br>
    AND Gate<br>
    XOR Gate<br>
    Multiplexor<br>
    Demultiplexor<br><br>
    16 Bit NOT Gate<br>
    16 Bit OR Gate<br>
    16 Bit AND Gate<br>
    16 Bit Multiplexor<br>
    16 Bit Demultiplexor<br><br>
    4 Way Multiplexor<br>
    8 Way Multiplexor<br>
    4 Way Demultiplexor<br>
    8 Way Demultiplexor<br>
</details>

<details>
  <summary>Chapter 2 - Boolean Arithmetic</summary>
  Half Adder<br>
  Full Adder<br>
  16 Bit Adder<br>
  16 Bit Incrementer<br>
  ALU<br>
</details>

<details>
  <summary>Chapter 3 - Sequential Logic</summary>
  Bit<br>
  Program Counter<br>
  Register<br>
  RAM Chips<br>
</details>

<details>
  <summary>Chapter 4 - Machine Language</summary>
  Assembly program to toggle screen color<br>
  Assembly program to multiple two integers<br>
</details>

<details>
  <summary>Chapter 5 - Computer Architecture</summary>
  CPU<br>
  Memory<br>
  Computer<br>
</details>

## The Jack Programming Language
The target language for the course's compiler is the Jack programming language, an object orient language that somewhat resembles Java. It supports strings, arrays, basic math, basic drawing, a keyboard, and direct memory access. The Jack standard library is, of course, written in Jack.<br>

**[Jack Standard Library Source Code](https://github.com/zmilla93/Nand2Tetris/tree/master/jack)**

## The Compilation Process
The compilation process again resembles Java by being split into 3 parts; a compiler, a virtual machine, and an assembler. The compiler converts an OOP Jack program into a platform agnostic virtual machine language program. The virtual machine then translates the VM program into a hardware specific HACK program. Finally, the assembler converts the HACK program into machine code that is runnable by the HACK hardware described previously.<br>

### Jack Compiler
The Jack Compiler converts any Jack files it finds into a corresponding virtual machine file. The VM output is platform agnostic.<br>

### Virtual Machine
The VM Translator converts platform agnostic virtual machine code into HACK assembly code. To add support for a new hardware platform, you would modify the VM Translator and create a new assembler. Doing so would then allow you to use any language built on top of this VM implementation, in this case the Jack Compiler.

### HACK Assembler
The assembler converts HACK assembly code into HACK machine code. This is a one to one conversion where each line of assembly code converts into exactly 16 bits of machine lanugage.
