This is an assembler and emulator for the DCPU-16. It doesn't really have a name yet, I'm afraid.

The emulator supports most of the current specifications, including current hardware specs and the current DCPU spec, 1.7.

The assembler is pretty simple, and doesn't really support anything fancy, just normal instructions, syntax, and DAT.

Distinctive Features
--------------------

- The monitor does not update anything unless it actually needs to. It listens to relevant areas of DCPU memory for changes and updates as needed.
- Supports an integrated assembler + emulator mode which shows various register values as they update, and the assembler and emulator can be used a separate command line tools.
- When the emulator is run standalone, via command line, it does not create a window unless it has to, and only when it has to. A window is only created if a hardware interrupt is sent to either the monitor or keyboard

Missing Features
----------------

- Emulation speed is not throttled to 100 khz (though it fully supports the Generic Clock).
- The assembler does not support PICK yet.
- The monitor does not support changing or dumping the palette.

Building
--------

It requires JDK 7 to be built (for binary literals and the diamond operator). Just compile everything in net/ian/dcpu together via javac, or an IDE.

Usage
-----

It can be used as an integrated assembler + emulator in a GUI by running the DCPULauncher class.
To use either the assembler or emulator on their own, with no GUI, just run the Assembler or Emulator class, respectively.

Video: [http://youtu.be/t4n3NFtjXWI](http://youtu.be/t4n3NFtjXWI)
