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
        self.fork_return = 0
        self.writes = []

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
                syscall = self.r[7]
                if syscall == 2:
                    self.r[0] = self.fork_return
                elif syscall in (114, 162):
                    self.r[0] = 0
                elif syscall == 4:
                    self.writes.append(bytes(self.read(self.r[1]+i, 1) for i in range(self.r[2])))
                    self.r[0] = self.r[2]
                elif syscall == 1:
                    return ['exit', self.r[0]]
                elif syscall == 11:
                    args, addr = [], self.r[1]
                    while self.read(addr):
                        args.append(self.string(self.read(addr))); addr += 4
                        assert len(args) < 16
                    assert args[0] == self.string(self.r[0])
                    return args
                else: raise AssertionError(('Unsupported syscall', syscall))
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
                    assert ins & 0x70 == 0, 'Only immediate LSL operands modeled'
                    value = (self.get(ins & 15, pc) << ((ins >> 7) & 31)) & 0xFFFFFFFF
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


for argc in [1, 2, 10]:
    cpu = CPU(); cpu.write(cpu.r[13], argc)
    assert cpu.execute('_start', ('environmentready',)) == 'environmentready'
    assert cpu.r[8] == cpu.r[13]+4*(argc+2)


def route(code, value=0, now=10_000_000, picture_hold=None, home_hold=0):
    cpu = CPU(); sp = cpu.r[13]
    cpu.write(sp+28, 0xA0000)
    picture_down = 0 if picture_hold is None else now-picture_hold
    for offset, data in [(0, now // 1_000_000), (4, now % 1_000_000), (8, 1 | (code << 16)),
                         (12, value), (16, picture_down // 1_000_000), (20, picture_down % 1_000_000)]:
        cpu.write(sp + offset, data)
    cpu.r[8], cpu.r[9] = divmod(now - home_hold, 1_000_000)
    stop = cpu.execute('routeevent', ('forward', 'homedown', 'picturedown', 'loop', 'forklaunch'))
    return cpu, stop


for code in [690, 645]:
    for duration in [0, 100_000, 799_999]:
        cpu, stop = route(code, picture_hold=duration)
        assert stop == 'forklaunch'
        assert cpu.read(cpu.r[13]+16) == 0
        assert cpu.execute('launch') == ['/system/bin/cmd', 'activity', 'stop-service', '-n', 'com.griffin.solhome/.PicturePanelService']
        native, _ = route(code, picture_hold=duration)
        native.fork_return = 123
        assert native.execute('launch') == ['exit', 0]
        events = [struct.unpack_from('<llHHi', native.writes[0], i)[2:] for i in range(0, 64, 16)]
        assert events == [(1,690,1),(0,0,0),(1,690,0),(0,0,0)]
        assert cpu.r[2] == 0xA0000
    for duration in [800_000, 950_000, 2_000_000, 2_200_000_000]:
        cpu, stop = route(code, now=20_000_000_000, picture_hold=duration)
        assert stop == 'forklaunch'
        argv = cpu.execute('launch')
        assert cpu.r[2] == 0xA0000
        assert argv == ['/system/bin/cmd', 'activity', 'stop-service', '-n', 'com.griffin.solhome/.PicturePanelService']
        cpu, _ = route(code, now=20_000_000_000, picture_hold=duration)
        cpu.fork_return = 123
        assert cpu.execute('launch') == ['exit', 0]
        assert len(cpu.writes) == 1 and len(cpu.writes[0]) == 64
        events = [struct.unpack_from('<llHHi', cpu.writes[0], i) for i in range(0, 64, 16)]
        assert [event[2:] for event in events] == [(1,690,1),(0,0,0),(1,690,0),(0,0,0)]
    assert route(code)[1] == 'loop'  # Ignore release without a matching press.
    assert route(code, 2, picture_hold=500_000)[1] == 'loop'
    cpu, stop = route(code, 1)
    assert stop == 'picturedown'
    assert cpu.execute('picturedown', ('loop',)) == 'loop'
    assert cpu.read(cpu.r[13]+16) == 10 and cpu.read(cpu.r[13]+20) == 0
for code in [103, 108, 105, 106, 158, 115, 114, 113, 217]:
    assert route(code)[1] == 'forward'
assert route(172, 1)[1] == 'homedown'
cpu, _ = route(172, home_hold=100_000)
assert cpu.execute('launch') == ['/system/bin/cmd', 'activity', 'start', '-n', 'com.griffin.solhome/.MainActivity']
cpu, _ = route(172, home_hold=950_000)
assert cpu.execute('launch')[-2:] == ['-a', 'android.settings.SETTINGS']
print('Assembled ARM routing passed: Home/held Home, native Settings/Picture on short and long presses, argv, key forwarding')
