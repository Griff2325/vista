# World outside the window

Wildlife adds occasional native sightings to the existing landscape:

- Three small birds cross above the forest for 23 seconds every 113 seconds in daylight.
- A fox walks along the dry left foreground rocks, pauses for nine seconds, then continues. Its 34-second visit repeats every 229 seconds, with gentle entrance/exit fades.
- In winter, a tiny snowboarder traverses a far snowy ridge above the shore for 16 seconds every 167 seconds. The larger foreground snowboard remains in place.

The independent periods give different combinations over time, with quiet gaps. Sightings use shared monotonic uptime so changing between Home and the system dream does not restart their paths. This is a small decorative world, not random wildlife simulation. Birds/rider require daylight; the fox also appears near dusk. All are suppressed by heavier rain, snow or strong wind, using the same eased Cincinnati observation state as the landscape. Halloween and other holiday accents remain independently selected by the scene calendar.

**Settings → Wildlife** or the phone companion toggles the added sightings. An active Home updates immediately; an already running system dream rereads preferences within a minute. Existing seasonal butterflies/fireflies and Halloween animals are controlled by their own seasonal/occasion rules, not this toggle.

WorldOutside reuses a Paint and draws a bounded number of small rectangles and line segments in the existing 480×336 scene coordinates. It adds no images, paths, scene worker, networking, separate frame callbacks or per-frame Java objects. It shares the existing approximately 30 fps rendering and pauses with the scene when hidden. A low-resolution distant rider is intentionally small; the foreground board is the clearer winter detail from a couch.

For reproducible development captures, MainActivity accepts a `sol.preview.world_seconds` float only when the existing `sol.preview.epoch` scene-lighting extra is positive. It freezes the wildlife phase while other motion continues. A plain Home intent clears both overrides; normal seasonal previews do not freeze wildlife. This is a diagnostic capture aid, not a remote-control API or device-clock change.
