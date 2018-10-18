package network.xyo.ble.firmware

import android.os.Environment
import android.util.Log
import network.xyo.core.XYBase
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.*
import kotlin.experimental.xor

@Suppress("unused")
class OtaFile @Throws(IOException::class)
private constructor(private val inputStream: InputStream?) {

    private var bytes: ByteArray? = null
    private var blocks: Array<Array<ByteArray>>? = null
    private val bytesAvailable: Int = inputStream!!.available()

    var crc: Byte = 0
        private set

    var fileBlockSize = 128
        private set

    var fileChunkSize = 20
        private set

    var numberOfBlocks = -1
        private set

    var chunksPerBlockCount: Int = 0
        private set
    var totalChunkCount: Int = 0
        private set

    val numberOfBytes: Int
        get() = bytes!!.size

    init {
        bytes = ByteArray(bytesAvailable + 1)
        inputStream!!.read(bytes!!)
        crc = calculateCrc()
        bytes!![bytesAvailable] = crc

        //Default block/chunk sizes for XY4 devices.
        setFileBlockSize(128, 20)
    }

    // Set the file blockSize and ChunkSize if not using the default 128/20 values
    fun setFileBlockSize(blockSize: Int, chunkSize: Int) {
        fileBlockSize = Math.max(blockSize, chunkSize)
        fileChunkSize = chunkSize
        chunksPerBlockCount = fileBlockSize / fileChunkSize + if (fileBlockSize.rem(fileChunkSize) != 0) 1 else 0
        numberOfBlocks = bytes!!.size / fileBlockSize + if (bytes!!.size.rem(fileBlockSize) != 0) 1 else 0
        initBlocksSuota()
    }

    // override arrayOfNulls to let us specify the size
    private inline fun <reified T> fileEmptyArray(size: Int): Array<T> =
            @Suppress("UNCHECKED_CAST")
            (arrayOfNulls<T>(size) as Array<T>)


    //Chunk the OtaFile into correct block sizes.
    private fun initBlocksSuota() {
        totalChunkCount = 0
        blocks = fileEmptyArray(numberOfBlocks)

        XYBase.logInfo("OtaFile", "initBlocksSuota numberOfBlocks: $numberOfBlocks")

        var byteOffset = 0
        // Loop through all the bytes and split them into pieces the size of the default chunk size
        for (i in 0 until numberOfBlocks) {
            var blockSize = fileBlockSize
            var numberOfChunksInBlock = chunksPerBlockCount
            // Check if the last block needs to be smaller
            if (byteOffset + fileBlockSize > bytes!!.size) {
                blockSize = bytes!!.size.rem(fileBlockSize)
                numberOfChunksInBlock = blockSize / fileChunkSize + if (blockSize.rem(fileChunkSize) != 0) 1 else 0
            }
            var chunkNumber = 0
            blocks!![i] = fileEmptyArray(numberOfChunksInBlock)

            var j = 0
            while (j < blockSize) {
                // Default chunk size
                var chunkSize = fileChunkSize
                // Last chunk in block
                if (j + fileChunkSize > blockSize) {
                    chunkSize = blockSize.rem(fileChunkSize)
                }

                //XYBase.logInfo("OtaFile", "total bytes: " + bytes!!.size + ", offset: " + byteOffset + ", block: " + i + ", chunk: " + (chunkNumber + 1) + ", blockSize: " + blockSize + ", chunkSize: " + chunkSize)
                val chunk = Arrays.copyOfRange(bytes!!, byteOffset, byteOffset + chunkSize)
                blocks!![i][chunkNumber] = chunk
                byteOffset += chunkSize
                chunkNumber++
                totalChunkCount++
                j += fileChunkSize
            }
        }
    }

    fun getBlock(index: Int): Array<ByteArray> {
        return blocks!![index]
    }

    fun close() {
        if (inputStream != null) {
            try {
                inputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    @Throws(IOException::class)
    private fun calculateCrc(): Byte {
        var crcCode: Byte = 0
        for (i in 0 until bytesAvailable) {
            val byteValue = bytes!![i]
            val intVal = byteValue.toInt()
            crcCode = crcCode xor intVal.toByte()
        }
        //XYBase.logInfo("OtaFile", String.format("Firmware CRC: %#04x", crc_code and 0xff.toByte()))

        return crcCode
    }

    companion object {
        private val filesDir = Environment.getExternalStorageDirectory().absolutePath + "/Xyo"

        fun getByFileName(filename: String): OtaFile {
            // Get the file and store it in fileStream

            val inputStream = FileInputStream("$filesDir/$filename")
            return OtaFile(inputStream)
        }

        fun getByFileStream(stream: FileInputStream): OtaFile {
            return OtaFile(stream)
        }

        fun list(): ArrayList<String>? {
            val f = java.io.File(filesDir)
            val file = f.listFiles() ?: return null
            // Arrays.sort<OtaFileS>(file, Comparator<java.io.OtaFileS> { lhs, rhs -> lhs.path.compareTo(rhs.path, ignoreCase = true) })
            Log.d("Files", "Size: " + file.size)
            val names = ArrayList<String>()
            for (i in file.indices) {
                Log.d("Files", "FileName:" + file[i].name)
                names.add(file[i].name)
            }
            return names
        }

        fun createFileDirectory(): Boolean {
            val directoryName = filesDir
            val directory: java.io.File
            directory = java.io.File(directoryName)
            return directory.exists() || directory.mkdirs()
        }
    }
}