@ Sol Home: ARM32 Linux syscall-only physical remote bridge.
@ argv[1] is the evdev path discovered by supervisor.sh.
@ EVIOCGRAB prevents stock Home dispatch; uinput forwards mapped non-Home keys.
@ Expose only actual keymap codes: advertising all keys misclassifies as stylus.
@ Home release launches Vista; hold >800ms opens stock Settings.
@ Settings/Picture: any matched release opens Panasonic; Vista controls stay optional.
@ Process exit closes both descriptors and restores the original input route.
@ MOVW+PC addresses avoid ADR's limited immediate encodings as the text grows.
.syntax unified
.arm
.global _start
_start:
 ldr r0,[sp]
 @ Preserve Android's environment for app_process-based commands such as input.
 add r8,sp,#8
 add r8,r8,r0,lsl #2
environmentready:
 cmp r0,#2
 ldrge r10,[sp,#8]
 movwlt r10,#:lower16:(remote - . - 12)
 addlt r10,pc,r10
 movw r0,#:lower16:(uinput - . - 12)
 add r0,pc,r0
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
 movw r11,#:lower16:(keycodes - . - 12)
 add r11,pc,r11
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
 movw r1,#:lower16:(setup - . - 12)
 add r1,pc,r1
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
 str r8,[sp,#28]
 mov r0,#0
 str r0,[sp,#16]
 str r0,[sp,#20]
 str r0,[sp]
 movw r0,#0xc200
 movt r0,#0x0beb
 str r0,[sp,#4]
 mov r0,sp
 mov r1,#0
 mov r7,#162
 svc #0
inputmode:
 ldr r0,[r10]
 movw r1,#0x2d2d
 movt r1,#0x6e69
 cmp r0,r1
 bne openremote
 ldr r0,[r10,#4]
 movw r1,#0x7570
 movt r1,#0x7374
 cmp r0,r1
 bne openremote
 ldrb r0,[r10,#8]
 cmp r0,#0
 bne openremote
emitinput:
 mov r0,#0
 str r0,[sp]
 movw r0,#0xa300
 movt r0,#0x11e1
 str r0,[sp,#4]
 mov r0,sp
 mov r1,#0
 mov r7,#162
 svc #0
 @ One-shot native Input press; never open or grab the physical remote.
 mov r0,r5
 movw r1,#:lower16:(inputpacket - . - 12)
 add r1,pc,r1
 mov r2,#64
 mov r7,#4
 svc #0
 cmp r0,#64
 bne fail
 mov r0,sp
 mov r1,#0
 mov r7,#162
 svc #0
 mov r0,#0
 mov r7,#1
 svc #0
openremote:
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
routeevent:
 ldrh r0,[sp,#8]
 cmp r0,#1
 bne forward
 ldrh r0,[sp,#10]
 cmp r0,#172
 beq homekey
 movw r1,#690
 cmp r0,r1
 beq picturekey
 movw r1,#645
 cmp r0,r1
 bne forward
picturekey:
 ldr r0,[sp,#12]
 cmp r0,#1
 beq picturedown
 cmp r0,#0
 bne loop
 ldr r1,[sp,#16]
 cmp r1,#0
 beq loop
 mov r0,#0
 str r0,[sp,#16]
 ldr r0,[sp]
 sub r0,r0,r1
 cmp r0,#1
 bgt longpicture
 cmp r0,#0
 blt singlepicture
 movw r6,#0x4240
 movt r6,#0x000f
 mul r0,r0,r6
 ldr r2,[sp,#4]
 ldr r3,[sp,#20]
 add r0,r0,r2
 sub r0,r0,r3
 movw r2,#0x3500
 movt r2,#0x000c
 cmp r0,r2
 blt singlepicture
longpicture:
 movw r6,#:lower16:(nativepacket - . - 12)
 add r6,pc,r6
 b forklaunch
picturedown:
 ldr r0,[sp]
 str r0,[sp,#16]
 ldr r0,[sp,#4]
 str r0,[sp,#20]
 b loop
singlepicture:
 @ Keep the tested native path as the default for a short press too.
 b longpicture
homekey:
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
 movw r6,#:lower16:(sol - . - 12)
 add r6,pc,r6
 movw r11,#:lower16:(start - . - 12)
 add r11,pc,r11
 mov r12,#0
 movw r1,#0x3500
 movt r1,#0x000c
 cmp r0,r1
 movwge r6,#:lower16:(picture - . - 12)
 addge r6,pc,r6
 movwge r11,#:lower16:(startservice - . - 12)
 addge r11,pc,r11
 movwge r12,#:lower16:(nativeaction - . - 12)
 addge r12,pc,r12
forklaunch:
 mov r7,#2
 svc #0
 cmp r0,#0
 beq launch
 b loop
homedown:
 mov r0,#1
 movw r1,#:lower16:(marker - . - 12)
 add r1,pc,r1
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
 movw r0,#:lower16:(nativepacket - . - 12)
 add r0,pc,r0
 cmp r6,r0
 beq quicklaunch
 ldr r2,[sp,#28]
 mov r0,#0
 push {r0}
 cmp r12,#0
 beq noaction
 push {r12}
 movw r0,#:lower16:(aflag - . - 12)
 add r0,pc,r0
 push {r0}
noaction:
 push {r6}
 movw r0,#:lower16:(nflag - . - 12)
 add r0,pc,r0
 push {r0}
 push {r11}
 movw r0,#:lower16:(activity - . - 12)
 add r0,pc,r0
 push {r0}
 movw r0,#:lower16:(cmd - . - 12)
 add r0,pc,r0
 push {r0}
 mov r1,sp
 mov r7,#11
 svc #0
 b fail
quicklaunch:
 @ Only this child waits for overlay teardown; the remote-reading parent stays free.
 mov r7,#2
 svc #0
 cmp r0,#0
 beq stopquick
 blt emitquick
 add r1,sp,#24
 mov r2,#0
 mov r7,#114
 svc #0
emitquick:
 mov r0,#0
 str r0,[sp,#16]
 movw r0,#0xa300
 movt r0,#0x11e1
 str r0,[sp,#20]
 add r0,sp,#16
 mov r1,#0
 mov r7,#162
 svc #0
 mov r0,r5
 movw r1,#:lower16:(nativepacket - . - 12)
 add r1,pc,r1
 mov r2,#64
 mov r7,#4
 svc #0
 mov r0,#0
 mov r7,#1
 svc #0
stopquick:
 ldr r2,[sp,#28]
 mov r0,#0
 push {r0}
 movw r0,#:lower16:(picture - . - 12)
 add r0,pc,r0
 push {r0}
 movw r0,#:lower16:(nflag - . - 12)
 add r0,pc,r0
 push {r0}
 movw r0,#:lower16:(stopservice - . - 12)
 add r0,pc,r0
 push {r0}
 movw r0,#:lower16:(activity - . - 12)
 add r0,pc,r0
 push {r0}
 movw r0,#:lower16:(cmd - . - 12)
 add r0,pc,r0
 push {r0}
 mov r1,sp
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
startservice: .asciz "start-foreground-service"
.balign 4
nflag: .asciz "-n"
.balign 4
aflag: .asciz "-a"
.balign 4
sol: .asciz "com.griffin.solhome/.MainActivity"
.balign 4
nativeaction: .asciz "android.settings.SETTINGS"
.balign 4
picture: .asciz "com.griffin.solhome/.PicturePanelService"
.balign 4
stopservice: .asciz "stop-service"
.balign 4
inputpacket:
 .word 0,0
 .short 1,641
 .word 1
 .word 0,0
 .short 0,0
 .word 0
 .word 0,0
 .short 1,641
 .word 0
 .word 0,0
 .short 0,0
 .word 0
.balign 4
nativepacket:
 .word 0,0
 .short 1,690
 .word 1
 .word 0,0
 .short 0,0
 .word 0
 .word 0,0
 .short 1,690
 .word 0
 .word 0,0
 .short 0,0
 .word 0
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
