package Chip8

class Memory() {
    private var ram:ByteArray;

    // 4096 bytes of memory.
    init {
        this.ram = ByteArray(0);
        this.flush();
    }

    fun read(address: Int): Byte {
        return this.ram.get(address);
    }

    fun write(address: Int, value:Byte) {
        this.ram.set(address, value);
    }

    fun flush() {
        this.ram = ByteArray(4096);
    }
}