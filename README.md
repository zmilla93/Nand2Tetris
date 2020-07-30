# Nand2Tetris
Nand2Tetris is a computer science course by the MIT Press. It takes you through the steps of building a computer starting with nothing more than simple NAND gates. It then guides you through the process of building an assembler, virtual machine, and compiler for an objected oriented programming language. This repo contain's only my own work and excludes the extra course files.<br>

**[Course Website](https://www.nand2tetris.org/)**

## The Hardware
The hardware design used by the course is named HACK. Machine code has the .hack extension. Circuits are designed using a Hardware Description Language (HLD) with the `.hdl` extension.

#### Computer specs
- 16 bit
- 32K RAM
- 512x256 black & white screen
- Keyboard

The course starts by using a NAND gate to first design all other logic gates, then computer chips like an adder, RAM, ALU, CPU, and finally a full blown computer. While this machine supports basic drawings and text writing functions, it notably lacks things like a mouse, hard drive, and file system. If you are curious about the details of the hardware, or even testing the HDL code yourself, visit the course's website for information and testing tools (chapters 1-5).<br>

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
The target language for the course's compiler is the Jack programming language, an object orient language that resembles Java. Jack is a simple object oriented language where the standard library doubles as a very primitive operating system. It supports strings, arrays, basic math, basic drawing, a keyboard, and direct memory access. The Jack standard library is, of course, written in Jack.<br>

## The Compilation Process
The compilation process again resembles Java by being split into 3 parts; a compiler, a virtual machine, and an assembler. The compiler converts an OOP Jack program into a platform agnostic virtual machine language program. The virtual machine then translates the VM program into a hardware specific HACK program. Finally, the assembler converts the HACK program into machine code that is readable by the HACK hardware described previously.<br>

### HACK Assembler
The assembler converts HACK assembly code into HACK machine code. This is a one to one conversion where each line of assembly code converts into exactly 16 bits of machine lanugage.

### Virtual Machine
The VM Translator converts platform agnostic virtual machine code into HACK assembly code. To add support for a new hardware platform, you would modify the VM Translator and create a new assembler. Doing so would then allow you to use any language built on top of this VM implementation, in this case the Jack Compiler.

### Jack Compiler
The Jack Compiler converts any Jack files it finds into a corresponding VM file. Due the the VM layer, a new language could be created by writing a new compiler that translates into the previously described VM language.
