init:
	irmovq  Stack, %rsp
	call Main
	halt
Main:
	irmovq $12, %rax
	rmmovq %rax, -8(%rsp)
	mrmovq -8(%rsp), %rcx
	addq %rax, %rax
	pushq %rax
	popq %rax
	ret
	

	
	.pos 0x100
Stack:
	
