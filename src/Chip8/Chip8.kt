package Chip8

class Chip8 {
    private var cpu:CPU = CPU();

    fun run() {
        this.cpu.run();
    }
}