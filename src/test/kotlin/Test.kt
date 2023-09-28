import me.alex_s168.bundly.*

fun main() {
    bundle("test.jar") { // creates a bundle named test.jar
        overwrite(true)                              // enables overwriting for existing files
        file(local("a.txt"))                        // includes the local file a.txt
        file(local("aaa.xml") named "index.xml")    // includes the local file aaa.xml with the name index.xml
        file(remote("http://google.com"))            // includes the remote file http://google.com
        file(remote("http://google.com") named "google.html") // includes the remote file http://google.com with the name google.html
        zip(local("test.zip"))                      // includes all contents of the local zip file test.zip
        zip(remote("http://google.com/test.zip"))    // includes all contents of the remote zip file http://google.com/test.zip
        jar(local("test.jar"))                      // includes all contents of the local jar file test.jar
        jar(remote("http://google.com/test.jar"))    // includes all contents of the remote jar file http://google.com/test.jar
        text("test.txt") {      // includes a file named test.txt with the content "Hello, world!"
            "Hello, world!"
        }
        binary("test.bin") {    // includes a file named test.bin with the content [0x00, 0x01, 0x02]
            byteArrayOf(0x00, 0x01, 0x02)
        }
    }.bundle() // saves it
}