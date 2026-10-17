from pathlib import Path
import struct,sys
p=Path(sys.argv[1]);o=p.read_bytes();shoff=struct.unpack_from('<I',o,32)[0];size,count,strings=struct.unpack_from('<HHH',o,46);sh=[struct.unpack_from('<10I',o,shoff+i*size) for i in range(count)];s=sh[strings];names=o[s[4]:s[4]+s[5]]
textidx=None
for idx,s in enumerate(sh):
 name=names[s[0]:].split(b'\0')[0]
 if name==b'.text':code=bytearray(o[s[4]:s[4]+s[5]]);textidx=idx
for s in sh:
 if s[1]!=9 or not s[5]:continue
 if s[7]!=textidx:raise ValueError('non-text relocation')
 symtab=sh[s[6]]
 for pos in range(s[4],s[4]+s[5],8):
  at,info=struct.unpack_from('<II',o,pos);symidx,kind=info>>8,info&255
  name,value,symsize,syminfo,other,section=struct.unpack_from('<IIIBBH',o,symtab[4]+symidx*16)
  if kind not in (28,29) or section!=textidx:raise ValueError((kind,section))
  ins=struct.unpack_from('<I',code,at)[0];imm=ins&0xffffff
  if imm&0x800000:imm-=0x1000000
  target=value+(imm<<2)-at
  struct.pack_into('<I',code,at,(ins&0xff000000)|((target>>2)&0xffffff))
base=0x10000;off=0x100;length=off+len(code)
h=struct.pack('<16sHHIIIIIHHHHHH',b'\x7fELF\x01\x01\x01'+b'\0'*9,2,40,1,base+off,52,0,0x5000200,52,32,1,0,0,0);ph=struct.pack('<8I',1,0,base,base,length,length,5,0x1000)
p.with_suffix('').write_bytes((h+ph).ljust(off,b'\0')+code)
