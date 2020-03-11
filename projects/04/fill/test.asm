// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Fill.asm

// Runs an infinite loop that listens to the keyboard input.
// When a key is pressed (any key), the program blackens the screen,
// i.e. writes "black" in every pixel;
// the screen should remain fully black as long as the key is pressed. 
// When no key is pressed, the program clears the screen, i.e. writes
// "white" in every pixel;
// the screen should remain fully clear as long as no key is pressed.

// Put your code here.



//	NOTES:
//	The end of @SCREEN is the start of @KBD
//	Pixel Color:
//	0=white,1=black

@99
D=A
@0
M=-1
@0
D=A
@IF_TRUE
D;JEQ
@99
D=A
@4
M=D
@END
0;JMP


(IF_TRUE)
	@99
	D=A
	@2
	M=D

(END)
	@END
	0;JMP
