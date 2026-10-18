// Recreate Vista's simple window mark using native vector geometry. macOS / Xcode only.
import AppKit
let image = NSImage(size: NSSize(width: 1024, height: 1024))
image.lockFocus()
NSColor(srgbRed: 0.063, green: 0.094, blue: 0.098, alpha: 1).setFill()
NSBezierPath(rect: NSRect(x: 0, y: 0, width: 1024, height: 1024)).fill()
let window = NSRect(x: 200, y: 220, width: 624, height: 584)
NSColor(srgbRed: 0.2, green: 0.32, blue: 0.29, alpha: 1).setFill()
NSBezierPath(roundedRect: window, xRadius: 32, yRadius: 32).fill()
let far = NSBezierPath(); far.move(to: NSPoint(x: 224, y: 310)); far.line(to: NSPoint(x: 436, y: 590)); far.line(to: NSPoint(x: 584, y: 410)); far.line(to: NSPoint(x: 700, y: 540)); far.line(to: NSPoint(x: 800, y: 330)); far.close()
NSColor(srgbRed: 0.48, green: 0.61, blue: 0.51, alpha: 1).setFill(); far.fill()
NSColor(srgbRed: 0.82, green: 0.74, blue: 0.58, alpha: 1).setFill()
NSBezierPath(ovalIn: NSRect(x: 644, y: 634, width: 78, height: 78)).fill()
NSColor(srgbRed: 0.91, green: 0.9, blue: 0.81, alpha: 1).setStroke()
let outline = NSBezierPath(roundedRect: window, xRadius: 32, yRadius: 32); outline.lineWidth = 22; outline.stroke()
let sill = NSBezierPath(); sill.move(to: NSPoint(x: 160, y: 200)); sill.line(to: NSPoint(x: 864, y: 200)); sill.lineWidth = 22; sill.lineCapStyle = .round; sill.stroke()
image.unlockFocus()
let bitmap = NSBitmapImageRep(data: image.tiffRepresentation!)!
try bitmap.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: CommandLine.arguments[1]))
