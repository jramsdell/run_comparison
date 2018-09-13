package evaluation.rouge

import org.apache.lucene.index.Term
import org.apache.lucene.search.TermQuery
import utils.AnalyzerFunctions
import utils.ParseFunctions
import utils.defaultWhenNotFinite
import utils.lucene.getIndexSearcher
import utils.pmap


data class RougeStatSummary(
        val name: String,
        val precision: Double,
        val recall: Double,
        val f1: Double
)

class RougeEvaluator(indexLoc: String, qrelDist: String) {
    val searcher = getIndexSearcher(indexLoc)
    val targetDist = readRunfiles(qrelDist)
//    val sourceDist: HashMap<String, Int> = HashMap()

//    fun parseDist(text: ): HashMap<String, Int> {
//        corpus.flatMap { text ->
//            AnalyzerFunctions.createTokenList(text, AnalyzerFunctions.AnalyzerType.ANALYZER_ENGLISH_STOPPED)
//            tokens.forEach { token ->
//                if (token !in dist) {
//                    dist[token] = 0
//                }
//                dist[token] = dist[token]!! + 1
//            }
//        }
//        return dist
//    }

    fun parseDist(corpus: Iterable<String>): HashMap<String, Int> {
        val dist = HashMap<String, Int>()
        corpus.forEach { text ->
            val tokens = AnalyzerFunctions.createTokenList(text, AnalyzerFunctions.AnalyzerType.ANALYZER_ENGLISH_STOPPED)
            tokens.forEach { token ->
                if (token !in dist) {
                    dist[token] = 0
                }
                dist[token] = dist[token]!! + 1
            }
        }
        return dist
    }

//    fun readRunfiles(runfileLoc: String, cutoff: Int = 0) =
//            ParseFunctions
//                .parseRunfile(runfileLoc)
//                .map { (query, pids) ->
//                    query to parseDist(createParagraphTextIterator(pids).let { iterator ->
//                        if (cutoff > 0)
//                            iterator.take(cutoff)
//                        else iterator
//                    })}
//                .toMap()

    fun readRunfiles(runfileLoc: String, cutoff: Int = 0) =
            ParseFunctions
                .parseRunfile(runfileLoc, cutoff)
                .entries
                .pmap { (query, pids) ->
                    val rougeDocs = createRougeDocs(pids)
                        .map { doc -> doc.id to doc }
                        .toMap()
                    query to rougeDocs
                    }
                .toMap()


    fun createRougeDocs(pList: Iterable<String>) =
            pList.map { id ->
                val result = searcher.search(TermQuery(Term("paragraphid", id)), 1)
                    .scoreDocs
                    .firstOrNull()
                result?.doc?.let { id to searcher.doc(it).get("text") } }
                .filterNotNull()
                .map { (id, text) ->
                    val tokens = AnalyzerFunctions.createTokenList(text, AnalyzerFunctions.AnalyzerType.ANALYZER_ENGLISH_STOPPED)
                    val unigrams = tokens.toSet()
                    val bigrams = tokens.windowed(2, 1, false)
                        .map { (t1, t2) -> t1 + "_" + t2  }
                        .toSet()

                    RougeDoc(id = id, unigrams = unigrams, bigrams = bigrams)
                }



//    fun createParagraphTextIterator(pList: Iterable<String>) =
//        pList.pmap { id ->
//            val result = searcher.search(TermQuery(Term("paragraphid", id)), 1)
//                .scoreDocs
//                .firstOrNull()
//            result?.doc?.let { searcher.doc(it).get("text") }
//        }
//            .filterNotNull()
//            .asIterable()


//    fun getRecall(source: HashMap<String, Int>, target: HashMap<String, Int>): Double {
//        val results = source.entries.sumBy { (k,v) -> Math.min(v, (target[k] ?: 0)) }
//        return results.toDouble() / (target.values.sum())
//    }
//
//    fun getPrecision(source: HashMap<String, Int>, target: HashMap<String, Int>): Double {
//        val results = source.entries.sumBy { (k,v) ->
//            val t = target[k] ?: 0
//            if (v > t) t else v
//        }
//        return results.toDouble() / (source.values.sum())
//    }

    fun getStats(source: Map<String, RougeDoc>, target: Map<String, RougeDoc>) =
            source.entries.pmap { (id, doc) ->
                val stats = RougeDocStats(doc = doc)
                stats.statFunctions.add( RougeEvaluationFunction(name = "Unigrams", doc = doc,
                        docSetRetriever = { it.unigrams }) )
                stats.statFunctions.add( RougeEvaluationFunction(name = "Bigrams", doc = doc,
                        docSetRetriever = { it.bigrams }) )

                target.values.forEach { relevantDoc -> stats.calculateStats(relevantDoc) }
                stats }

    fun summarizeStats(stats: List<RougeDocStats>) =
            stats
                .flatMap { it.statFunctions }
                .groupBy { statFunction -> statFunction.name }
                .map { (name, statFunctions) ->
                    val averagePrecision = statFunctions.sumByDouble { it.precision } / statFunctions.size
                    val averageRecall = statFunctions.sumByDouble { it.recall } / statFunctions.size
                    val averageF1 = statFunctions.sumByDouble { it.f1 } / statFunctions.size

                    RougeStatSummary(
                            name = name,
                            precision = averagePrecision,
                            recall = averageRecall,
                            f1 = averageF1 )
                }


//    fun compare(runfileLoc: String) {
//        print("Reading source")
//        val sourceDist = readRunfiles(runfileLoc, 100)
//        println("Finished")
//        targetDist.forEach { (k, v) ->
//            val s = sourceDist[k] ?: HashMap()
//            val precision = getPrecision(s, v).run { if (isNaN()) 0.0 else this }
//            val recall = getRecall(s, v).run { if (isNaN()) 0.0 else this }
//            println("Precision: $precision, Recall: $recall")
//        }
//
//    }

    fun compare(runfileLoc: String): Double {
        val sourceDist = readRunfiles(runfileLoc, 100)
        val queryStats = targetDist.keys.flatMap { query ->
            val qTarget = targetDist[query] ?: emptyMap()
            val qSource = sourceDist[query] ?: emptyMap()
            val stats =getStats(qTarget, qSource)
             summarizeStats(stats) }

        queryStats.groupBy { it.name }
            .forEach { (name, stats) ->
                val averagePrecision = stats.map { it.precision.defaultWhenNotFinite(0.0) }.average()
                val averageRecall = stats.map { it.recall.defaultWhenNotFinite(0.0) }.average()
                val averageF1 = stats.map { it.f1.defaultWhenNotFinite(0.0) }.average()

//                println("$name : $averagePrecision : $averageRecall : $averageF1")
                if (name == "Bigrams")
                    return averageF1
            }
        return 0.0



    }

}