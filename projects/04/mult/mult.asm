// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Mult.asm

// Multiplies R0 and R1 and stores the result in R2.
// (R0, R1, R2 refer to RAM[0], RAM[1], and RAM[2], respectively.)

// Put your code here.

@R2
M=0					//Zero answer before looping

(LOOP)

	@R1
	D=M		//D=R1
	@END
	D;JEQ	//Exit loop if D == 0
	
	@R2
	D=M		//D=R2
	@R0
	D=D+M	//D=D+R0
	@R2
	M=D		//R2=D
	
	@R1
	M=M-1	//R1=R1-1
	
	@LOOP
	0;JMP	//Loop

(END)
	@END
	0;JMP


