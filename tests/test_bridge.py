"""Execute the assembled A32 key routing and exec argv with a bounded CPU model.

No device or third-party emulator required. Unsupported instructions fail the test.
"""
from pathlib import Path
import struct

ROOT = Path(__file__).resolve().parents[1]
obj = (ROOT / 'build/bridge/sol-home-bridge.o').read_bytes()
elf = (ROOT / 'build/bridge/sol-home-bridge').read_bytes()
off = struct.unpack_from('<I', obj, 32)[0]
size, count = struct.unpack_from('<HH', obj, 46)
sections = [struct.unpack_from('<10I', obj, off + i * size) for i in range(count)]
symbols = {}
for section in sections:
    if section[1] != 2:
        continue
    strings = sections[section[6]]
    names = obj[strings[4]:strings[4] + strings[5]]
    for pos in range(section[4], section[4] + section[5], 16):
        name, value = struct.unpack_from('<II', obj, pos)
        symbols[names[name:].split(b'\0')[0].decode()] = 0x10100 + value


class CPU:
    def __init__(self):
        self.mem = dict(enumerate(elf, 0x10000))
        self.r = [0] * 16
        self.r[13] = 0x90000
        self.zero = self.negative = self.overflow = False

    def get(self, reg, pc):
        return pc + 8 if reg == 15 else self.r[reg]

    def read(self, addr, size=4):
        return sum(self.mem.get(addr + i, 0) << (8 * i) for i in range(size))

    def write(self, addr, value):
        for i in range(4):
            self.mem[addr + i] = (value >> (8 * i)) & 255

    def string(self, addr):
        data = bytearray()
        while self.read(addr, 1):
            data.append(self.read(addr, 1)); addr += 1
            assert len(data) < 512
        return data.decode()

    def execute(self, label, stops=()):
        pc = symbols[label]
        for _ in range(250):
            if pc in [symbols[s] for s in stops]:
                return next(s for s in stops if symbols[s] == pc)
            ins = self.read(pc); nextpc = pc + 4; cond = ins >> 28
            condition = {0: self.zero, 1: not self.zero, 10: self.negative == self.overflow,
                         11: self.negative != self.overflow,
                         12: not self.zero and self.negative == self.overflow, 14: True}
            assert cond in condition, hex(ins)
            if not condition[cond]:
                pc = nextpc; continue
            rd, rn = (ins >> 12) & 15, (ins >> 16) & 15
            if ins & 0x0E000000 == 0x0A000000:
                delta = ins & 0xFFFFFF
                if delta & 0x800000: delta -= 0x1000000
                nextpc = pc + 8 + (delta << 2)
            elif ins & 0x0FF00000 in (0x03000000, 0x03400000):
                value = ((ins >> 4) & 0xF000) | (ins & 0xFFF)
                self.r[rd] = value if ins & 0x0FF00000 == 0x03000000 else (self.r[rd] & 65535) | (value << 16)
            elif ins & 0x0FFF0000 == 0x092D0000:
                regs = [i for i in range(16) if ins & (1 << i)]
                self.r[13] -= 4 * len(regs)
                for i, reg in enumerate(regs): self.write(self.r[13] + i * 4, self.r[reg])
            elif ins & 0x0F000000 == 0x0F000000:
                assert self.r[7] == 11, 'Only execve is modeled'
                args, addr = [], self.r[1]
                while self.read(addr):
                    args.append(self.string(self.read(addr))); addr += 4
                    assert len(args) < 16
                assert args[0] == self.string(self.r[0]), (args, self.string(self.r[0]), hex(self.r[0]), hex(self.r[1]), [hex(self.read(self.r[1]+i*4)) for i in range(8)])
                return args
            elif ins & 0x0FC000F0 == 0x00000090:
                self.r[(ins >> 16) & 15] = (self.r[ins & 15] * self.r[(ins >> 8) & 15]) & 0xFFFFFFFF
            elif ins & 0x0E1000F0 == 0x001000B0:
                offset = ((ins >> 4) & 0xF0) | (ins & 15)
                self.r[rd] = self.read(self.get(rn, pc) + offset, 2)
            elif ins & 0x0C000000 == 0x04000000:
                assert not ins & (1 << 25), 'Register offsets not modeled'
                offset = ins & 4095
                base = self.get(rn, pc)
                updated = base + (offset if ins & (1 << 23) else -offset)
                addr = updated if ins & (1 << 24) else base
                if ins & (1 << 20): self.r[rd] = self.read(addr)
                else: self.write(addr, self.r[rd])
                if ins & (1 << 21) or not ins & (1 << 24): self.r[rn] = updated
            elif ins & 0x0C000000 == 0:
                op = (ins >> 21) & 15
                if ins & (1 << 25):
                    shift, value = ((ins >> 8) & 15) * 2, ins & 255
                    value = ((value >> shift) | (value << (32 - shift))) & 0xFFFFFFFF
                else:
                    assert ins & 0xFF0 == 0, 'Shifted operands not modeled'
                    value = self.get(ins & 15, pc)
                left = self.get(rn, pc)
                if op == 13: self.r[rd] = value
                elif op == 4: self.r[rd] = (left + value) & 0xFFFFFFFF
                elif op == 2: self.r[rd] = (left - value) & 0xFFFFFFFF
                elif op == 10:
                    result = (left - value) & 0xFFFFFFFF
                    self.zero, self.negative = result == 0, bool(result & 0x80000000)
                    self.overflow = bool((left ^ value) & (left ^ result) & 0x80000000)
                else: raise AssertionError(('Unsupported ALU', hex(ins)))
            else: raise AssertionError(('Unsupported instruction', hex(ins)))
            pc = nextpc
        raise AssertionError('Instruction budget exhausted')


def route(code, value=0, last=0, now=10_000_000, home_hold=0):
    cpu = CPU(); sp = cpu.r[13]
    for offset, data in [(0, now // 1_000_000), (4, now % 1_000_000), (8, 1 | (code << 16)),
                         (12, value), (16, last // 1_000_000), (20, last % 1_000_000)]:
        cpu.write(sp + offset, data)
    cpu.r[8], cpu.r[9] = divmod(now - home_hold, 1_000_000)
    stop = cpu.execute('routeevent', ('forward', 'homedown', 'loop', 'forklaunch'))
    return cpu, stop


for code in [690, 645]:
    cpu, stop = route(code)
    assert stop == 'forklaunch'
    assert cpu.execute('launch') == ['/system/bin/cmd', 'activity', 'start-foreground-service', '-n', 'com.griffin.solhome/.PicturePanelService']
    for gap in [1, 150_000, 449_999]:
        cpu, stop = route(code, last=10_000_000 - gap)
        assert cpu.read(cpu.r[13] + 16) == 0  # A third press starts a new sequence.
        argv = cpu.execute('launch')
        assert argv[:2] == ['/system/bin/sh', '-c'] and 'input keyevent 176' in argv[2]
    for gap in [450_000, 900_000, 9_000_000]:
        cpu, _ = route(code, last=10_000_000 - gap)
        assert cpu.execute('launch')[1:3] == ['activity', 'start-foreground-service']
    for gap in [2_200_000_000, 8_640_000_000]:
        cpu, _ = route(code, now=20_000_000_000, last=20_000_000_000-gap)
        assert cpu.execute('launch')[1:3] == ['activity', 'start-foreground-service']
    for value in [1, 2]: assert route(code, value)[1] == 'loop'
for code in [103, 108, 105, 106, 158, 115, 114, 113, 217]:
    assert route(code)[1] == 'forward'
assert route(172, 1)[1] == 'homedown'
cpu, _ = route(172, home_hold=100_000)
assert cpu.execute('launch') == ['/system/bin/cmd', 'activity', 'start', '-n', 'com.griffin.solhome/.MainActivity']
cpu, _ = route(172, home_hold=950_000)
assert cpu.execute('launch')[-2:] == ['-a', 'android.settings.SETTINGS']
print('Assembled ARM routing passed: Home/held Home, Settings/Picture, double-press boundaries, argv, key forwarding')
