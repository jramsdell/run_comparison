package evaluation.rouge

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.AnalyzerWrapper
import org.apache.lucene.analysis.core.SimpleAnalyzer
import org.apache.lucene.analysis.core.StopFilter
import org.apache.lucene.index.Term
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.TermQuery
import utils.AnalyzerFunctions
import utils.ParseFunctions
import utils.defaultWhenNotFinite
import utils.lucene.getIndexSearcher
import utils.pmap
import java.io.File


data class RougeStatSummary(
        val name: String,
        val precision: Double,
        val recall: Double,
        val f1: Double,
        val nRel: Int = 0
)

class RougeEvaluator(sourceLoc: String, targetLoc: String, qrelDist: String) {
    val sourceSearcher = getIndexSearcher(sourceLoc)
    val targetSearcher = getIndexSearcher(targetLoc)
    val targetDist: Map<String, Map<String, RougeDoc>> = readRunfiles(qrelDist, searcher = targetSearcher)
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

//    var good: Int = 0
//    var bad: Int = 0

    fun readRunfiles(runfileLoc: String, searcher: IndexSearcher, cutoff: Int = 0) =
            ParseFunctions
                .parseRunfile(runfileLoc, cutoff)
                .entries
                .pmap { (query, pids) ->
                    val rougeDocs = createRougeDocs(pids, searcher)
                        .map { doc -> doc.id to doc }
                        .toMap()
                    query to rougeDocs
                }
                .toMap()

    fun readSourceRunfiles(runfileLoc: String, searcher: IndexSearcher) =
            ParseFunctions
                .parseRunfile(runfileLoc)
                .mapValues { (query, pds) ->
                    val nRel = targetDist[query]?.size ?: 0
                    pds.take(nRel)
                }
                .entries
                .pmap { (query, pids) ->
                    val rougeDocs = createRougeDocs(pids, searcher)
                        .map { doc -> doc.id to doc }
                        .toMap()
                    query to rougeDocs
                }
                .toMap()


    fun getSearchResult(searcher: IndexSearcher, id: String): Pair<String, String>? {
        val result = searcher.search(TermQuery(Term("paragraphid", id)), 1)
            .scoreDocs
            .firstOrNull()
        return result?.doc?.let { id to searcher.doc(it).get("text") }
    }

    fun createRougeDocs(pList: Iterable<String>, searcher: IndexSearcher) =
            pList.map { id ->
//                val result = searcher.search(TermQuery(Term("paragraphid", id)), 1)
//                    .scoreDocs
//                    .firstOrNull()
//                val final = result?.doc?.let { id to searcher.doc(it).get("text") }
                val final = getSearchResult(searcher, id) ?:
                    if (searcher == targetSearcher) getSearchResult(sourceSearcher, id)
                    else getSearchResult(targetSearcher, id)

                if (final == null && searcher == targetSearcher) {
                    println("Something went wrong for: $id")
                }
                final
            }
                .filterNotNull()
                .map { (id, text) ->
                    if (searcher == targetSearcher) {
                    }
                    val tokens = AnalyzerFunctions.createTokenList(text, AnalyzerFunctions.AnalyzerType.ANALYZER_ENGLISH_STOPPED)
                    val unigrams = tokens.toSet()
                    val bigrams = tokens.windowed(2, 1, false)
                        .map { (t1, t2) -> t1 + "_" + t2 }
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
                stats.statFunctions.add(RougeEvaluationFunction(name = "Unigrams", doc = doc,
                        docSetRetriever = { it.unigrams }))
                stats.statFunctions.add(RougeEvaluationFunction(name = "Bigrams", doc = doc,
                        docSetRetriever = { it.bigrams }))

                target.values.forEach { relevantDoc -> stats.calculateStats(relevantDoc) }
                stats
            }

    fun summarizeStats(stats: List<RougeDocStats>, nRel: Int) =
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
                            f1 = averageF1,
                            nRel = nRel)
                }


    fun generateRougeQrels(runfiles: List<File>) {
        val candidates = retrieveCandidateDocuments(runfiles)
        analyzeCandidates(candidates)
    }

    private fun retrieveCandidateDocuments(runfiles: List<File>) =
            runfiles
                .pmap { f ->
                    f.readLines()
                        .map { it.split(" ").let { elements -> elements[0] to elements[2] } }
                        .groupBy { it.first }
                        .map { it.key to it.value.map { it.second }.take(100) }}
                .flatten()
                .groupBy { it.first }
                .entries
                .pmap {
                    val candidates = it.value.flatMap { it.second }.toSet()
                    it.key to createRougeDocs(candidates, sourceSearcher)}
                .toMap()


    private fun analyzeCandidates(candidates: Map<String, List<RougeDoc>>) {
        targetDist.forEach { (query, relevant) ->
            val qCandidates = candidates[query] ?: emptyList()
            println(qCandidates.size)
        }
    }



    fun compare(runfileLoc: String): Double {
        val sourceDist = readSourceRunfiles(runfileLoc, searcher = sourceSearcher)

        val queryStats = targetDist.keys.flatMap { query ->
            val qTarget = targetDist[query] ?: emptyMap()
            val qSource = sourceDist[query] ?: emptyMap()
            val stats = getStats(qTarget, qSource)
             summarizeStats(stats, qSource.size) }

        queryStats.groupBy { it.name }
            .forEach { (name, stats) ->
                val nRel = stats.first().nRel
                val averagePrecision = stats.map { it.precision.defaultWhenNotFinite(0.0) }.sum().div(nRel)
                val averageRecall = stats.map { it.recall.defaultWhenNotFinite(0.0) }.sum().div(nRel)
                val averageF1 = stats.map { it.f1.defaultWhenNotFinite(0.0) }.sum().div(nRel)

                if (name == "Bigrams")
                    return averageF1
            }
        return 0.0



    }

}