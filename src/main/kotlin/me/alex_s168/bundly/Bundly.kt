package me.alex_s168.bundly

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private fun unzipfile(zipFilePath: String, destDir: String) {
    val dir = File(destDir)
    // creating an output directory if it doesn't exist already
    if (!dir.exists()) dir.mkdirs()
    val fis: FileInputStream
    // buffer to read and write data in the file
    val buffer = ByteArray(1024)
    try {
        fis = FileInputStream(zipFilePath)
        val zis = ZipInputStream(fis)
        var ze: ZipEntry? = zis.getNextEntry()
        while (ze != null) {
            val fileName = ze.name
            val newFile = File(destDir + File.separator + fileName)
            // create directories for sub directories in zip
            newFile.parentFile?.mkdirs()
            if (ze.isDirectory) {
                newFile.mkdir()
                ze = zis.getNextEntry()
                continue
            }
            val fos = FileOutputStream(newFile)
            var len: Int
            while (zis.read(buffer).also { len = it } > 0) {
                fos.write(buffer, 0, len)
            }
            fos.close()
            // close this ZipEntry
            zis.closeEntry()
            ze = zis.getNextEntry()
        }
        // close last ZipEntry
        zis.closeEntry()
        zis.close()
        fis.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

fun zipFile(path: File): File {
    // unzips the given path and returns a path where the unzipped files are located
    val tmp = File("tmp_${System.nanoTime()}/")
    tmp.mkdir()
    unzipfile(path.absolutePath, tmp.absolutePath)
    tmp.deleteOnExit()
    return tmp
}

fun jarFile(path: File): File =
    zipFile(path)

fun jarFile(path: String): File =
    jarFile(File(path))

fun zipFile(path: String): File =
    zipFile(File(path))

fun remoteFile(urlIn: String): File {
    val url = URL(urlIn)
    val tmp = File("tmp_${System.nanoTime()}/")
    val readableByteChannel = Channels.newChannel(url.openStream())
    val fileOutputStream = FileOutputStream(tmp)
    val fileChannel = fileOutputStream.getChannel()
    fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE)
    tmp.deleteOnExit()
    return tmp
}

fun net(url: String): File =
    remoteFile(url)

fun File.named(name: String): FileFile =
    FileFile(name, this)

open class Bundle(
    val dir: File
) {

    init {
        dir.deleteOnExit()
    }

    fun jar(path: String) {
        val f = zipFile(path)
        file(f)
        f.deleteRecursively()
    }

    fun jar(file: File) {
        val f = zipFile(file)
        file(f)
        f.deleteRecursively()
    }

    fun zip(path: String) {
        val f = zipFile(path)
        file(f)
        f.deleteRecursively()
    }

    fun zip(file: File) {
        val f = zipFile(file)
        file(f)
        f.deleteRecursively()
    }

    fun file(file: File) {
        this with file
    }

    fun file(file: FileFile) {
        this with file
    }

    fun text(name: String, content: () -> String) {
        this with textFile(name, content)
    }

}

class BundleKnownName(
    dir: File,
    val outputName: String
): Bundle(dir)

infix fun Bundle.with(other: File): Bundle {
    other.copyRecursively(dir, Settings.overwrite) { _, ex ->
        if (ex is FileAlreadyExistsException) OnErrorAction.SKIP
        else OnErrorAction.TERMINATE
    }
    return this
}

infix fun Bundle.with(other: FileFile): Bundle {
    val y = File(dir, other.name)
    y.parentFile.mkdirs()
    other.file.copyTo(y, Settings.overwrite)
    return this
}

internal object Settings {
    var overwrite: Boolean = false
}

fun overwrite(new: Boolean) {
    Settings.overwrite = new
}

data class Bundled(
    val file: File
)

@Throws(IOException::class)
private fun pack(sourceDirPath: Path, zipFilePath: Path) {
    val p = Files.createFile(zipFilePath)
    ZipOutputStream(Files.newOutputStream(p)).use { zs ->
        Files.walk(sourceDirPath)
            .filter { path: Path ->
                !Files.isDirectory(
                    path
                )
            }
            .forEach { path: Path ->
                val zipEntry = ZipEntry(sourceDirPath.relativize(path).toString())
                try {
                    zs.putNextEntry(zipEntry)
                    Files.copy(path, zs)
                    zs.closeEntry()
                } catch (e: IOException) {
                    System.err.println(e)
                }
            }
    }
}

fun Bundle.bundle(outputPath: String): Bundled {
    val outputFile = File(outputPath)
    outputFile.parentFile?.mkdirs()
    if (outputFile.isFile) {
        outputFile.delete()
    }
    pack(dir.toPath(), outputFile.toPath())
    dir.deleteRecursively()
    return Bundled(outputFile)
}

fun BundleKnownName.bundle(): Bundled =
    bundle(outputName)

fun bundle(outputPath: String, op: BundleKnownName.() -> Unit = {}): BundleKnownName {
    val dir = File("tmp_${System.nanoTime()}/")
    dir.mkdir()
    dir.deleteOnExit()
    val b = BundleKnownName(dir, outputPath)
    op(b)
    return b
}

data class FileFile(
    val name: String,
    val file: File
)

fun textFile(name: String, content: () -> String): FileFile {
    val file = File("tmp_${System.nanoTime()}")
    file.writeText(content())
    file.deleteOnExit()
    return FileFile(name, file)
}