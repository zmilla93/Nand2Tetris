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


//	FUNCTION:
//	Program continually loops, checking if they current keyboard input matches the expected screen color
//	If color is incorrect, program will loop and recolor entire screen before returning to main listening loop
//	Could be optimized by continuing to listen for input change during color loop
//	NOTES:
//	The end of @SCREEN is the start of @KBD
//	Pixel Color:
//	0=white,1=black


@color
M=0

(LOOP)

	@SCREEN
	D=A
	@adr
	M=D
	@KBD
	D=M
	@SCREEN_WHITE
	D;JEQ
	@SCREEN_BLACK
	0;JMP

(SCREEN_WHITE)
	//Jump to loop if color is already white
	@color
	D=M
	@LOOP
	D;JEQ
	
	//Change color and jump to COLOR_SCREEN
	@color
	M=0
	@COLOR_SCREEN
	0;JMP
	
(SCREEN_BLACK)
	//Jump to loop if color is already black
	@color
	D=M
	@LOOP
	D;JLT

	@color
	M=-1
	@COLOR_SCREEN
	0;JMP
	
(COLOR_SCREEN)
	//Check for exit
	@KBD
	D=A
	@adr
	D=D-M
	@LOOP
	D;JEQ
	
	@color
	D=M
	@adr
	A=M
	M=D
	@adr
	M=M+1
	
	@COLOR_SCREEN
	0;JMP

(END)
