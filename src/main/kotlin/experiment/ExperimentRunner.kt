package experiment

import process.TrecEvalRunner
import java.io.File


class ExperimentRunner(
        trecEvalLoc: String,
        runDirectoryLoc: String) {

    val runner = TrecEvalRunner(trecEvalLoc)
    val runfiles = File(runDirectoryLoc).listFiles().toList()

    fun evaluateQrel(qrelLoc: String, qrelName: String) {
        val out = File(qrelName + ".txt").bufferedWriter()
        out.write("""
            \begin{table}[ht!]
                \begin{center}
                \captionof{table}{$qrelName}
                \begin{tabular}{|l|l|l|l|l|}
                \hline
                \textbf{Name} & \textbf{MAP} & \textbf{RPREC} & \textbf{P5} \\\hline

        """.trimIndent())
        runfiles.forEach { runfile ->
            val path = runfile.absolutePath
            val result = runner.evaluate(path, qrelLoc)
            with (result) {
                out.write("""$name & $map & $rprec & $p5 \\\hline
            """.trimIndent() + "\n")
            }
//            println(result)
        }
        out.close()
    }

}

