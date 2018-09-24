package process

import evaluation.rouge.EvalStat
import sun.misc.IOUtils
import java.io.File

data class TrecEvalResult(
        val map: Double,
        val rprec: Double,
        val p5: Double,
        val ndcg: Double,
        val name: String
)


class TrecEvalRunner(val evalLoc: String) {

    fun evaluate(runLoc: String, qrelLoc: String): TrecEvalResult {
        val command = "$evalLoc -m all_trec $qrelLoc $runLoc"

        val processBuilder = ProcessBuilder(command.split(" "))
        val process = processBuilder.start()
        Runtime.getRuntime().addShutdownHook(Thread { process.destroy() })

        val result = process.inputStream.bufferedReader().readLines()
        process.waitFor()
        process.inputStream.close()
        val map = result[5]
        val rprec = result[7]
        val p5 = result[21]
        val ndcg = result[55]
        val rep = { s: String ->
                s.split("\\s+".toRegex())
                .last()
                .toDouble()
        }
        return TrecEvalResult(
                map = rep(map),
                rprec = rep(rprec),
                p5 = rep(p5),
                ndcg = rep(ndcg),
                name = runLoc.split("/").last())
    }

    fun retrieveEvalStats(runLoc: String, qrelLoc: String): List<EvalStat> {
        val command = "$evalLoc -m all_trec -q $qrelLoc $runLoc"

        val processBuilder = ProcessBuilder(command.split(" "))
        val process = processBuilder.start()
        Runtime.getRuntime().addShutdownHook(Thread { process.destroy() })

        val validMethods = setOf( "map", "Rprec", "ndcg", "P_5" )
        val result = process.inputStream.bufferedReader().readLines()
        process.waitFor()
        process.inputStream.close()
        val evalStats = ArrayList<EvalStat>()

        result.forEach { line ->
            val elements = line.split("\\s+".toRegex())
            if (elements[0] in validMethods) {
                evalStats.add(EvalStat(query = elements[1], method = elements[0], score = elements[2].toDouble()))
            }
        }

        return evalStats
    }

}