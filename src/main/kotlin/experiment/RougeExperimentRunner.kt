package experiment

import evaluation.rouge.RougeEvaluator
import process.TrecEvalRunner
import utils.createSpearmanMatrix
import utils.getSpearman
import java.io.File


data class RankData(
        var rank: Int = 0,
        var score: Double = 0.0 )

data class RankStat(
        val name: String,
        var map: RankData = RankData(),
        var rprec: RankData = RankData(),
        var ndcg: RankData = RankData(),
        var f1: RankData = RankData(),
        var p5: RankData = RankData()
)


class RougeExperimentRunner(
        trecEvalLoc: String,
        runDirectoryLoc: String) {

    val runner = TrecEvalRunner(trecEvalLoc)
    val runfiles = File(runDirectoryLoc).listFiles().toList()

    fun generateRougeQrels(qrelLoc: String, qrelName: String, sourceLoc: String,
                     targetLoc: String) {
        val rouge = RougeEvaluator(sourceLoc, targetLoc, qrelLoc)
        rouge.generateRougeQrels(runfiles)

    }

    fun evaluateQrel(qrelLoc: String, rougeQrelLoc: String, qrelName: String, sourceLoc: String,
                     targetLoc: String): ArrayList<RankStat> {
        val rouge = RougeEvaluator(sourceLoc, targetLoc, rougeQrelLoc)

        val out = File(qrelName + ".txt").bufferedWriter()
        val outRanked = File(qrelName + "_ranked" + ".txt").bufferedWriter()

        val results = ArrayList<RankStat>()


        runfiles.forEachIndexed { index, runfile ->
            println(index)
            val path = runfile.absolutePath
            val result = runner.evaluate(path, qrelLoc)
            val f1 = rouge.compare(path)

            val rStat = RankStat(
                    name = result.name,
                    map = RankData(score = result.map),
                    p5 = RankData(score = result.p5),
                    ndcg = RankData(score = result.ndcg),
                    rprec = RankData(score = result.rprec),
                    f1 = RankData(score = f1)
            )

            results.add(rStat)

            with (result) {
                out.write("""$name & $map & $rprec & $p5 & $ndcg & $f1 \\\hline
            """.trimIndent() + "\n")
            }

        }

        rankBy(results) { it.f1 }
        rankBy(results) { it.map }
        rankBy(results) { it.p5 }
        rankBy(results) { it.ndcg }
        rankBy(results) { it.rprec }

        results.forEach { result ->
            with (result) {
                outRanked.write("""$name & ${map.rank} & ${rprec.rank} & ${p5.rank} & ${ndcg.rank} & ${f1.rank} \\\hline
            """.trimIndent() + "\n")
            }

        }


        val rankMap = results.map { it.map.rank }
        val rankF1 = results.map { it.f1.rank }
//        val n = rankMap.size.toDouble()
//        val s1 = rankMap.zip(rankF1).sumByDouble { (x, y) -> (x - y).toDouble().pow(2.0) * 6.0  }
//        val spearman = 1.0 - (s1 / (n * (n.pow(2.0) - 1.0) ))
        val spearman = getSpearman(rankMap, rankF1)
        out.write("Spearman: $spearman\n")

        out.close()
        outRanked.close()

        println("========= $qrelName =========")
        createSpearmanMatrix(results, results)
        println("==================")
        return results
    }


}




fun rankBy(stats: List<RankStat>, f: (RankStat) -> RankData) {
    stats.map { rs -> f(rs) }
        .sortedByDescending { it.score }
        .forEachIndexed { index, rd ->
            rd.rank = index + 1
        }
}

