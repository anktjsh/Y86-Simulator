.pos 0
Init:
    irmovq Stack, %rbp
    irmovq Stack, %rsp
    jmp Main

.pos 0x100
Stack:

.pos 0x104

# __fastcall int Multiply (int x, int y)
# x is passed in as %ecx, y is passed in as %edx.
Multiply:
    # Set up stack frame.
    pushq %rbp
    rrmovq %rsp, %rbp
    pushq %rsi
    
    irmovq $-1, %rsi  # esi = -1
    xorq %rax, %rax   # eax = 0

    # Skip multiply loop silently if multiplying by <= 0.
    andq %rdx, %rdx
    jle Multiply_End

Multiply_Loop:
    addq %rcx, %rax   # eax += ecx
    addq %rsi, %rdx   # edx -= 1
    jne Multiply_Loop # if (edx != 0) goto Multiply_Loop

Multiply_End:
    # Clean up stack frame.
    popq %rsi
    rrmovq %rbp, %rsp
    popq %rbp
    ret

# _cdecl int pow (int base, int exp)
Pow:
    # Set up stack frame.
    pushq %rbp
    rrmovq %rsp, %rbp
    
    irmovq $-1, %rsi      # esi = -1
    
    # We'll use %esi to demonstrate callee-save in Multiply,
    # since Multiply uses the callee-save %esi register.
    mrmovq 16(%rbp), %rcx  # ecx = base
    mrmovq 24(%rbp), %rdi # edi = exp
    
    rrmovq %rcx, %rdx     # edx = base
    addq %rsi, %rdi       # edi -= 1

Pow_Loop:
    pushq %rdx
    call Multiply         # eax = ecx * edx
    popq %rdx
    addq %rsi, %rdi       # edi -= 1
    rrmovq %rax, %rcx     # ecx = eax
    jne Pow_Loop          # if (edi != 0) goto Pow_Loop

Pow_End:
    # Clean up stack frame.
    rrmovq %rbp, %rsp
    popq %rbp
    ret

Main:

    # eax = Pow(6, 3)
    irmovq $6, %rax # base
    irmovq $3, %rbx # exponent
    pushq %rbx
    pushq %rax
    call Pow
    rrmovq %rbp, %rsp

    halt
    
