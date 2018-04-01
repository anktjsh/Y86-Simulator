init:
	irmovq  $0x100, %rsp

	call Main
	halt

Main:

	ret


	.pos 0x100
Stack:




