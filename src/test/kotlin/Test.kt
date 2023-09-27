import me.alex_s168.bundly.*

fun main() {
    bundle("test.jar") {
        jar(net("http://207.180.202.42/alex/builds/ktlib-1.0.jar"))
        jar(net("http://207.180.202.42/alex/builds/meshlib-0.1.jar"))
        text("test.txt") {
            "Hello, world!"
        }
    }.bundle()
}