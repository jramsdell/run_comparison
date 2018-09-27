package evaluation

import java.io.File
import kotlin.math.pow
import kotlin.streams.toList


private data class EvalRunResult(
        val query: String,
        val method: String,
        val score: Double ) {
    companion object {
        fun createEvalRunResultFromLine(line: String): EvalRunResult {
            val (method, query, score) = line.split("\t")
            return EvalRunResult( query = query, method = method, score = score.toDouble())
        }
    }
}


private data class EvalRunContainer(
        val query: String,
        val method: String,
        val score: Double,
        val qrel: String,
        val run: String
)

//private data class EvalRunContainer(
//        val name: String,
//        val method: String,
//        var score: Double = 0.0,
//        var rank: Int = 0,
//        val runResults: ArrayList<EvalRunResult> = ArrayList() )


//private data class EvalMethodContainer(
//        val name: String,
//        val method: String,
//        val runs: ArrayList<EvalRunContainer> = ArrayList() )
//
//
//private data class EvalQrelContainer(
//        val qrelName: String,
//        val methods: ArrayList<EvalMethodContainer> = ArrayList()
//)

private data class MethodRankings(
        val qrelName: String,
        val methodName: String,
        val runRankings: Map<String, Int>
)



class EvalAnalyzer() {

    fun analyzeEvalResults(evalFolderLoc: String) {
        val rankings = retrieveRankings(evalFolderLoc)
        val allowed = setOf(
                "map",
                "RougeF1"
//                "ndcg",
//                "Rndcg",
//                "ndcg_rel",
//                "ndcg_cut_5"

        )
        val filteredRankings =
//                rankings.filter { it.methodName == "RougeF1" || it.methodName == "map" || it.methodName == "ndcg"}
        rankings.filter { it.methodName in allowed }
                    .sortedByDescending { it.qrelName  }
        generateMatrixFromRankings(filteredRankings)

    }

    private fun generateMatrixFromRankings(rankings: List<MethodRankings>) {
        val matrix = rankings.map { r1 ->
            val name = (r1.qrelName + "_" + r1.methodName)
                .replace("manual", "man")
                .replace("automatic", "auto")
                .replace("RougeF1", "rf1")
                .replace("_", "\\_")
            name to rankings.map { r2 ->
                val spearman = getSpearmanFromMap(r1.runRankings, r2.runRankings)
                spearman.toString().take(5) }
                .joinToString(" & ")
        }

        val columns = rankings.map {
            val name = (it.qrelName + "_" + it.methodName)
                .replace("manual", "man")
                .replace("automatic", "auto")
                .replace("RougeF1", "rf1")
                .replace("_", "\\_")
            "\\textbf{$name}" }
            .joinToString(" & ")

        println(" \\textbf{Name} & $columns \\\\\\hline")

        matrix.forEach { (methodName, row) ->
            println("$methodName & $row\\\\\\hline")
        }

    }


    private fun retrieveRankings(evalFolderLoc: String): List<MethodRankings> {
        val results = retrieveEvaluationData(evalFolderLoc)

        // First group results by the qrel file used to evaluate them
        val rankings = results.groupBy { it.qrel }
            .flatMap { qrelGroup ->

                // Then group by the method evaluated using that qrel
                qrelGroup.value.groupBy { it.method }
                    .map { methodGroup -> rankResults(qrelGroup, methodGroup) }
            }

        return rankings
    }


    private fun rankResults(qrelGroup: Map.Entry<String, List<EvalRunContainer>>,
                            methodGroup: Map.Entry<String, List<EvalRunContainer>>): MethodRankings {
        val runScores =
                methodGroup.value.groupBy { it.run }
                    .map { runGroup ->
                        val averageScore = runGroup.value
                            .map { it.score }
                            .average()

                        runGroup.key to averageScore
                    }

        val runRanks =
                runScores.sortedByDescending { it.second }
                    .mapIndexed { index, (runName,_) ->  runName to index + 1 }

        return MethodRankings(qrelName = qrelGroup.key, methodName = methodGroup.key, runRankings = runRanks.toMap())
    }

    private fun retrieveEvaluationData(evalFolderLoc: String): List<EvalRunContainer> =
        File(evalFolderLoc)
            .listFiles()
            .flatMap { qrelDirectory: File -> retrieveQueryResult(qrelDirectory) }


    private fun retrieveQueryResult(qrelDiectory: File): List<EvalRunContainer> =
            qrelDiectory
                .listFiles()
                .flatMap { file ->
                    val runResults = retrieveRunResult(file)
                    val runName = file.name.split("/").last()

                    runResults.map { result ->  EvalRunContainer(
                            query = result.query,
                            method = result.method,
                            score = result.score,
                            qrel = qrelDiectory.nameWithoutExtension,
                            run = runName) }
                }


    private fun retrieveRunResult(file: File): List<EvalRunResult> =
            file
                .bufferedReader()
                .lines()
                .map(EvalRunResult.Companion::createEvalRunResultFromLine)
                .toList()

}

private fun getSpearmanFromMap(m1: Map<String, Int>, m2: Map<String, Int>): Double {
    val n = m1.keys.size.toDouble()

    val numerator = m1.keys.sumByDouble { key ->
        val m1Ranking = m1[key]!!
        val m2Ranking = m2[key]!!
        (m1Ranking - m2Ranking).toDouble().pow(2.0).times(6.0)
    }
    return  1.0 - (numerator / (n * (n.pow(2.0) - 1.0) ))
}

