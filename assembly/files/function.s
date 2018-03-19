	.pos   0x00
init:
	irmovq Stack, %rsp
	call Main
	halt

Main:
	call   sumInts	#call function
	ret


sumInts:
	andq   %rdi, %rdi
	jle    done
	irmovq $1, %rcx
	irmovq $0, %rax
	irmovq $1, %rdx 
loop:
	rrmovq %rdi, %rsi
	addq   %rdx, %rax
	addq   %rcx, %rdx
	subq   %rdx, %rsi
	jge    loop
	ret

done:	
	irmovq $0, %rax
	ret

	.pos	0x100
Stack:
 

	
