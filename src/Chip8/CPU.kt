package Chip8

import java.util.*

class CPU() {
    var memory:Memory;
    var keypad:ByteArray;
    var video:ByteArray;

    // Registers
    var V:UShortArray = UShortArray(16);

    // Address register.
    var I:Int = 0;

    // Program Counter
    var pc:UShort = 0x200.toUShort();

    var stack:ArrayDeque<UShort> = ArrayDeque(16)
    private var sp:Int = 0        // Stack pointer

    private var cpuSpeed:Int = 100

    private var timer:Int = 0

    private var clearScreen: Boolean = false

    init {
        this.cpuSpeed = 100
        this.memory = Memory()
        this.video = ByteArray(256)
        this.keypad = ByteArray(8)
    }

    fun run() {
        this.runOpcode(12.toUShort());
    }

    fun reset() {

    }

    fun stop() {

    }

    private fun runOpcode(opcode:UShort) {
        val X: Int = ((opcode and 0x0F00.toUShort()).toInt() shr 8)
        val Y: Int = ((opcode and 0x00F0.toUShort()).toInt() shr 4)
        val N: Int = (opcode and 0x000F.toUShort()).toInt()
        val NN = opcode and 0x00FF.toUShort()
        val NNN = opcode and 0x0FFF.toUShort()

        //if (this.debug) {
        //    this.eventManager.trigger('debug.opcode', [opcode]);
        //}

        // Read first 4 bits from opcode
        when (opcode and 0xF000.toUShort()) {

            0x0000.toUShort() -> {
                // 0x00E0: Clears the screen.
                // 0x00EE: Returns from subroutine.
                // 0x0NNN Calls RCA 1802 program at address NNN.
                when (opcode) {
                    // clears the screen
                    0x00E0.toUShort() -> {
                        this.clearScreen = true;
                        this.incrementPC();
                    }

                    // Returns from subroutine
                    0x00EE.toUShort() -> this.pc = this.stack.pop();

                    // (S)chip Set SCHIP graphic mode
                    //case 0x00FF:
                    //
                    //           break;

                    // 0x0NNN
                    //'Calls RCA 1802 program at 0x' + NNN );
                    else -> this.incrementPC();
                }
            }

            // Jumps to address NNN
            // 0x1NNN
            0x1000.toUShort() -> this.pc = NNN;

            // Calls subroutine at NNN
            // 0x2NNN
            0x2000.toUShort() -> {
                this.stack.push((this.pc + 2.toUShort()).toUShort());
                this.pc = NNN;
            }


            // Skips the next instruction if VX equals NN
            // 0x3XNN
            0x3000.toUShort() -> {
                if (this.V[X].equals(NNN)) {
                    this.incrementPC();
                    this.incrementPC();
                } else {
                    this.incrementPC();
                }
            }

            // Skips the next instruction if VX doesn't equal NN
            // 0x4XNN
            0x4000.toUShort() -> {
                if (!this.V[X].equals(NN)) {
                    this.incrementPC();
                    this.incrementPC();
                } else {
                    this.incrementPC();
                }
            }


            // Skips the next instruction if VX equals VY
            // 0x5XY0
            0x5000.toUShort() -> {
                if (this.V[X].equals(this.V[Y])) {
                    this.incrementPC();
                    this.incrementPC();
                } else {
                    this.incrementPC();
                }
            }

            // Sets VX to NN
            // 0x6XNN
            0x6000.toUShort() -> {
                this.V[X] = NN;
                this.incrementPC();
            }


            // Adds NN to VX
            // 0x7XNN
            0x7000.toUShort() -> {
                this.V[X] = (this.V[X].plus(NN) and 0xFF.toUInt()).toUShort();
                this.incrementPC();
            }


            0x8000.toUShort() -> {
                when (opcode and 0x000F.toUShort()) {
                    // 8XY0	Sets VX to the value of VY.
                    0.toUShort() -> this.V[X] = this.V[Y];

                    // 8XY1	Sets VX to "VX OR VY".
                    1.toUShort() -> this.V[X] = this.V[X] or this.V[Y];

                    // 8XY2	Sets VX to "VX AND VY".
                    2.toUShort() -> this.V[X] = this.V[X] and this.V[Y];

                    // 8XY3	Sets VX to "VX xor VY".
                    3.toUShort() -> this.V[X] = this.V[X] xor this.V[Y];

                    // 8XY4	Adds VY to VX. VF is set to 1 when there's a carry, and to 0 when there isn't.
                    4.toUShort() -> {
                        if ((this.V[Y] + this.V[X]) > 0xFF.toUInt()) {
                            this.V[0xF] = 1.toUShort();    // Carry.
                        } else {
                            this.V[0xF] = 0.toUShort();    // No carry.
                        }
                        this.V[X] = this.V[X].plus(this.V[Y]).toUShort()
                    }

                    // 8XY5	VY is subtracted from VX. VF is set to 0 when there's a borrow, and 1 when there isn't.
                    5.toUShort() -> {
                        if (this.V[Y] > this.V[X]) {
                            this.V[0xF] = 0;    // Borrow.
                        } else {
                            this.V[0xF] = 1;    // No borrow.
                        }
                        this.V[0xF] = if (this.V[X] >= this.V[Y])  1 else 0;
                        this.V[X] -= this.V[Y];
                    }

                    // 8XY6	Shifts VX right by one. VF is set to the value of the least significant bit of VX before the shift.[2]
                    6.toUShort() -> {
                        this.V[0xF] = this.V[X] and 0x1;
                        this.V[X] = this.V[X] shl 1;
                    }

                    // 8XY7	Sets VX to (VY minus VX). VF is set to 0 when there's a borrow, and 1 when there isn't.
                    7.toUShort() -> {
                        if (this.V[Y] < this.V[X]) {
                            this.V[0xF] = 0.toUShort();    // Borrow.
                        } else {
                            this.V[0xF] = 1.toUShort();    // No Borrow.
                        }
                        this.V[X] = this.V[Y].minus(this.V[X]).toUShort();
                    }

                    // 8XYE	Shifts VX left by one. VF is set to the value of the most significant bit of VX before the shift.[2]
                    0x0E.toUShort() -> {
                        this.V[0xF] = this.V[X] shl 7;
                        this.V[X] = this.V[X] shr 1;
                    }
                }
                this.incrementPC();
            }

            // Skips the next instruction if VX doesn't equal VY.
            // 0x9XY0
            0x9000.toUShort() -> {
                if (this.V[X] !== this.V[Y]) {
                    this.incrementPC();
                    this.incrementPC();
                } else {
                    this.incrementPC();
                }
            }

            // Sets this.I to the address NNN.
            // 0xANNN
            0xA000.toUShort() -> {
                this.I = NNN;
                this.incrementPC();
            }

            // Jumps to the address NNN plus V0.
            // 0xBNNN
            0xB000.toUShort() -> this.pc = NNN + this.V[0];

            // Sets VX to a "random number AND NN".
            // 0xCXNN
            0xC000.toUShort() -> {
                var random = Math.random();
                random = random * (0xFF - 0) + 1;
                this.V[X] = (random and NN);
                this.incrementPC();
            }

            // Draws a sprite at coordinate (VX,VY) that has a width of 8 pixels and a height of N pixels.
            // Each row of 8 pixels is read as bit-coded starting form memory location I.
            // I value doesn't change after the execution of this instruction
            // VF is set to 1 if any screen pixels are flipped from set to unset when the sprite is drawn,
            // and to 0 if that doesn't happen.
            // All drawing is XOR drawing (i.e. it toggles the screen pixels)
            // 0xDXYN
            0xD000.toUShort() -> {
                this.V[0xF] = if (this.video.drawSprite(this.V[X], this.V[Y], this.I, N)) {
                    1
                } else {
                    0
                }

                this.incrementPC()
            }
            //
            0xE000.toUShort() -> {
                when (opcode and 0x0FF.toUShort()) {
                    // EX9E	Skips the next instruction if the key stored in VX is pressed.
                    0x9E.toUShort() -> {
                        if (this.keypadStatus[this.V[X]] === 1) {
                            this.incrementPC();
                        }
                    }

                    // EXA1	Skips the next instruction if the key stored in VX isn't pressed.
                    0xA1.toUShort() -> {
                        if (this.keypadStatus[this.V[X]] === 0) {
                            this.incrementPC();
                        }
                    }
                }
                this.incrementPC();
            }


            0xF000.toUShort() -> {
                when (opcode and 0x00FF.toUShort()) {
                    // FX07	Sets VX to the value of the delay timer.
                    0x07.toUShort() -> this.V[X] = this.timerDelay;

                    // FX0A	A key press is awaited, and then stored in VX.
                    0x0A.toUShort() -> {
                        var keypress: Boolean = false;
                        for (i in 0..16) {
                            if (this.keypadStatus[i] !== 0) {
                                this.V[X] = i;
                                keypress = true;
                            }
                        }
                        if (!keypress) {
                            //	this.pc-= 2;  //Force try again.
                        }
                    }

                    // FX15	Sets the delay timer to VX.
                    0x15.toUShort() -> this.timerDelay = this.V[X];

                    // FX18	Sets the sound timer to VX.
                    0x18.toUShort() -> this.timerSound = this.V[X];

                    // FX1E	Adds VX to I.[3]
                    0x1E.toUShort() -> {
                        this.I += this.V[X];
                        this.V[0xF] = if ((this.I + this.V[X]) > 0xFFF) {
                            1
                        } else {
                            0
                        }
                    }

                    // FX29
                    // I is set the address for the hexadecimal character sprite referred to by the register VX 5 chars high
                    0x29.toUShort() -> this.I = this.V[X] * 5;

                    // FX33	Stores the Binary-coded decimal representation of VX, with the most significant of three digits at the address in I, the middle digit at I plus 1, and the least significant digit at I plus 2. (In other words, take the decimal representation of VX, place the hundreds digit in memory at location in I, the tens digit at location I+1, and the ones digit at location I+2.)
                    0x33.toUShort() -> {
                        this.memory.write(this.I, this.V[X] / 100);
                        this.memory.write(this.I + 1, (this.V[X] / 10) % 10);
                        this.memory.write(this.I + 2, (this.V[X] % 100) % 10);
                    }

                    // FX55	Stores V0 to VX in memory starting at address I.[4]
                    0x55.toUShort() -> {
                        for (i in 0..X) {
                            this.memory.write(this.I + i, this.V[i]);
                        }
                        this.I += X + 1;
                    }

                    // FX65	Fills V0 to VX with values from memory starting at address I.[4]
                    0x65.toUShort() -> {
                        for (i in 0..X) {
                            this.V[i] = this.I + i;
                        }
                        this.I += X + 1;
                    }
                }

                this.incrementPC();
            }
            //else -> this.debug && (description = 'Unknown opcode');
        }
    }

    private fun incrementPC() {
        this.pc = this.pc.plus(2.toUShort()).toUShort()
    }
}