package process

import sun.misc.IOUtils
import java.io.File

data class TrecEvalResult(
        val map: Double,
        val rprec: Double,
        val p5: Double,
        val name: String
)


class TrecEvalRunner(val evalLoc: String) {

    fun evaluate(runLoc: String, qrelLoc: String): TrecEvalResult {
        val command = "$evalLoc -c $qrelLoc $runLoc"

        val processBuilder = ProcessBuilder(command.split(" "))
        val process = processBuilder.start()
        Runtime.getRuntime().addShutdownHook(Thread { process.destroy() })

        val result = process.inputStream.bufferedReader().readLines()
        process.waitFor()
        process.inputStream.close()
        val map = result[5]
        val rprec = result[7]
        val p5 = result[21]
        val rep = { s: String ->
                s.split("\\s+".toRegex())
                .last()
                .toDouble()
        }
        return TrecEvalResult(
                map = rep(map),
                rprec = rep(rprec),
                p5 = rep(p5),
                name = runLoc.split("/").last())
    }

}