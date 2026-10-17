@ Sol Home: ARM32 Linux syscall-only physical remote bridge.
@ argv[1] is the evdev path discovered by supervisor.sh.
@ EVIOCGRAB prevents stock Home dispatch; uinput forwards mapped non-Home keys.
@ Expose only actual keymap codes: advertising all keys misclassifies as stylus.
@ Home release launches Sol; hold >800ms opens stock Settings.
@ Process exit closes both descriptors and restores the original input route.
.syntax unified
.arm
.global _start
_start:
 ldr r0,[sp]
 cmp r0,#2
 ldrge r10,[sp,#8]
 adrlt r10,remote
 adr r0,uinput
 mov r1,#2
 mov r2,#0
 mov r7,#5
 svc #0
 mov r5,r0
 cmp r0,#0
 blt fail
 movw r1,#0x5564
 movt r1,#0x4004
 mov r2,#1
 bl .Lsetcap
 mov r2,#4
 bl .Lsetcap
 adr r11,keycodes
keycaps:
 ldrh r2,[r11],#2
 movw r0,#0xffff
 cmp r2,r0
 beq keycaps_done
 movw r1,#0x5565
 movt r1,#0x4004
 bl .Lsetcap
 b keycaps
keycaps_done:
 movw r1,#0x5568
 movt r1,#0x4004
 mov r2,#4
 bl .Lsetcap
 mov r0,r5
 adr r1,setup
 movw r2,#1116
 mov r7,#4
 svc #0
 mov r0,r5
 movw r1,#0x5501
 mov r2,#0
 mov r7,#54
 svc #0
 cmp r0,#0
 blt fail
 sub sp,sp,#32
 mov r0,#0
 str r0,[sp]
 movw r0,#0xc200
 movt r0,#0x0beb
 str r0,[sp,#4]
 mov r0,sp
 mov r1,#0
 mov r7,#162
 svc #0
 mov r0,r10
 mov r1,#0
 mov r2,#0
 mov r7,#5
 svc #0
 mov r4,r0
 cmp r0,#0
 blt fail
 movw r1,#0x4590
 movt r1,#0x4004
 mov r2,#1
 mov r7,#54
 svc #0
 cmp r0,#0
 blt fail
loop:
 mvn r0,#0
 add r1,sp,#24
 mov r2,#1
 mov r3,#0
 mov r7,#114
 svc #0
 mov r0,r4
 mov r1,sp
 mov r2,#16
 mov r7,#3
 svc #0
 cmp r0,#16
 bne fail
 ldrh r0,[sp,#8]
 cmp r0,#1
 bne forward
 ldrh r0,[sp,#10]
 cmp r0,#172
 bne forward
 ldr r0,[sp,#12]
 cmp r0,#1
 beq homedown
 cmp r0,#0
 bne loop
 ldr r0,[sp]
 sub r0,r0,r8
 movw r1,#0x4240
 movt r1,#0x000f
 mul r0,r0,r1
 ldr r1,[sp,#4]
 add r0,r0,r1
 sub r0,r0,r9
 adr r6,sol
 movw r1,#0x3500
 movt r1,#0x000c
 cmp r0,r1
 adrge r6,settings
 mov r7,#2
 svc #0
 cmp r0,#0
 beq launch
 b loop
homedown:
 mov r0,#1
 adr r1,marker
 mov r2,#9
 mov r7,#4
 svc #0
 ldr r8,[sp]
 ldr r9,[sp,#4]
 b loop
forward:
 mov r0,r5
 mov r1,sp
 mov r2,#16
 mov r7,#4
 svc #0
 cmp r0,#16
 bne fail
 b loop
launch:
 mov r0,#0
 push {r0}
 push {r6}
 adr r0,nflag
 push {r0}
 adr r0,start
 push {r0}
 adr r0,activity
 push {r0}
 adr r0,cmd
 push {r0}
 mov r1,sp
 mov r2,#0
 mov r7,#11
 svc #0
fail:
 mov r0,#1
 mov r7,#1
 svc #0
.Lsetcap:
 mov r0,r5
 mov r7,#54
 svc #0
 cmp r0,#0
 blt fail
 bx lr
.balign 4
marker: .ascii "SOL_HOME\n"
.balign 4
cmd: .asciz "/system/bin/cmd"
.balign 4
activity: .asciz "activity"
.balign 4
start: .asciz "start"
.balign 4
nflag: .asciz "-n"
.balign 4
sol: .asciz "com.griffin.solhome/.MainActivity"
.balign 4
settings: .asciz "com.amazon.tv.launcher/.ui.MainSettingsActivity"
.balign 4
uinput: .asciz "/dev/uinput"
.balign 4
remote: .asciz "/dev/input/event6"
.balign 4
.balign 4
keycodes: .hword 116,217,96,103,105,106,108,172,158,139,168,208,164,113,114,115,362,402,403,2,3,4,5,6,7,8,9,10,11,370,644,358,638,744,745,746,747,398,399,400,401,641,642,643,645,647,646,633,748,249,676,677,678,679,680,681,682,683,684,685,686,687,689,690,691,692,695,672,673,674,675,688,694,65535

setup: .asciz "Sol Home Remote"
 .space 80-(.-setup)
 .short 5,0x0171,0x042c,1
 .space 1028
