package evaluation.rouge

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.AnalyzerWrapper
import org.apache.lucene.analysis.core.SimpleAnalyzer
import org.apache.lucene.analysis.core.StopFilter
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.TermQuery
import utils.AnalyzerFunctions
import utils.ParseFunctions
import utils.defaultWhenNotFinite
import utils.lucene.getIndexSearcher
import utils.pmap
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class EvalStat(
        val query: String,
        val method: String,
        val score: Double
)


data class RougeStatSummary(
        val name: String,
        val query: String,
        val precision: Double,
        val recall: Double,
        val f1: Double,
        val nRel: Int = 0
)

class RougeEvaluator(sourceLoc: String, targetLoc: String, qrelDist: String) {
    val sourceSearcher = getIndexSearcher(sourceLoc)
    val targetSearcher = getIndexSearcher(targetLoc)
    val targetDist: Map<String, Map<String, RougeDoc>> = readRunfiles(qrelDist, searcher = targetSearcher)
    val rels: ConcurrentHashMap<String, List<String>> = ConcurrentHashMap()
//    val sourceDist: HashMap<String, Int> = HashMap()


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



    fun getStats(source: Map<String, RougeDoc>, target: Map<String, RougeDoc>) =
            source.entries.pmap { (id, doc) ->
                val stats = RougeDocStats(doc = doc)
                stats.statFunctions.add(RougeEvaluationFunction(name = "Unigrams", doc = doc,
                        query = id,
                        docSetRetriever = { it.unigrams }))
                stats.statFunctions.add(RougeEvaluationFunction(name = "Bigrams", doc = doc,
                        query = id,
                        docSetRetriever = { it.bigrams }))

                target.values.forEach { relevantDoc -> stats.calculateStats(relevantDoc) }
                stats
            }

    fun summarizeStats(stats: List<RougeDocStats>, nRel: Int, query: String) =
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
                            nRel = nRel,
                            query = query)
                }


    fun generateRougeQrels(runfiles: List<File>) {
//        val candidates = retrieveCandidateDocuments(runfiles)
//        val candidates = determineCandidatesByRelevantDocuments()
//        analyzeCandidates(candidates)
        val candidates = determineCandidatesByRelevantDocuments2()
        val modWriter = File("modded.qrels").bufferedWriter()
        candidates.forEach { (query, pids) ->
            pids.forEach { pid ->
                modWriter.write("$query Q0 $pid 1\n")
            }
        }
        modWriter.close()
    }

    private fun retrieveCandidateDocuments(runfiles: List<File>) =
            runfiles
                .pmap { f ->
                    f.readLines()
                        .map { it.split(" ").let { elements -> elements[0] to elements[2] } }
                        .groupBy { it.first }
                        .map { it.key to it.value.map { it.second }.take(20) }}
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
            asignQrelsToCandidates(qCandidates, query, relevant.values.toList() )
        }
    }

    fun asignQrelsToCandidates(qCandidates: List<RougeDoc>, query: String, relevantDocs: List<RougeDoc>) {
        val stats = qCandidates.map { candidateDoc ->
            val stat = RougeDocStats(doc = candidateDoc)

            stat.statFunctions.add(RougeEvaluationFunction(name = "Bigrams", doc = candidateDoc,
                    query = query,
                    docSetRetriever = { it.bigrams }))

            relevantDocs.forEach { relDoc -> stat.calculateStats(relDoc) }
            stat
        }
        println("======$query======")
        stats
            .map { stat -> stat to stat.statFunctions.first().f1 }
            .filter { it.second > 0.6 }
            .sortedByDescending { it.second }
            .forEach { (stat, score)  ->
                println("${stat.doc.id} : $score")
                if (score <= 0.9) {
                    val tq = TermQuery(Term("paragraphid", stat.doc.id))
                    val id = sourceSearcher.search(tq, 1)
                        .scoreDocs
                        .firstOrNull()
                        ?.doc

                    if (id != null) {
                        val text = sourceSearcher.doc(id).get("text")
                        println()
                        println("\t$text\n~~~~~~~")
                    }
                }
            }
    }

    fun determineCandidatesByRelevantDocuments() =
        targetDist
            .entries
            .take(100)
            .map { (query,v) ->
            println(query)
            query to v.values.pmap { retrieveCandidatesClosestToRelevantDocument(it) }
                .flatten()
                .distinctBy { it.id } }
            .toMap()

    fun determineCandidatesByRelevantDocuments2() =
            targetDist
                .entries
                .map { (query,v) ->
                    query to v.values.pmap { relDoc ->
                        val candidates = retrieveCandidatesClosestToRelevantDocument(relDoc)
                        candidates.sortedByDescending { candidate -> candidate.quickF1(relDoc) }
//                            .take(2)
                        candidates.filter { candidate -> candidate.quickF1(relDoc) >= 0.6 } }
                        .flatten()
                        .map { it.id }.distinct() }
                .toMap()

    fun retrieveCandidatesClosestToRelevantDocument(relDoc: RougeDoc): List<RougeDoc> {
        val query = relDoc.unigrams
            .map { unigram -> TermQuery(Term("unigram", unigram)) }
            .fold(BooleanQuery.Builder()) { builder, qt -> builder.add(qt, BooleanClause.Occur.SHOULD)}
            .build()

        val candidates = sourceSearcher.search(query, 10)
            .scoreDocs
            .take(20)
            .map { it.doc }
            .map { docId ->
                val doc = sourceSearcher.doc(docId)
                val pid = doc.get("paragraphid")
                val text = doc.get("text")

                val tokens = AnalyzerFunctions.createTokenList(text, AnalyzerFunctions.AnalyzerType.ANALYZER_ENGLISH_STOPPED)
                val unigrams = tokens.toSet()
                val bigrams = tokens.windowed(2, 1, false)
                    .map { (t1, t2) -> t1 + "_" + t2 }
                    .toSet()

                RougeDoc(pid, unigrams, bigrams)
            }

        return candidates
    }



    fun compare(runfileLoc: String): Double {
        val sourceDist = readSourceRunfiles(runfileLoc, searcher = sourceSearcher)

        val queryStats = targetDist.keys.flatMap { query ->
            val qTarget = targetDist[query] ?: emptyMap()
            val qSource = sourceDist[query] ?: emptyMap()
            val stats = getStats(qTarget, qSource)
             summarizeStats(stats, qSource.size, query) }

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

    fun getEval(runfileLoc: String): List<EvalStat> {
        val sourceDist = readSourceRunfiles(runfileLoc, searcher = sourceSearcher)

        val queryStats = targetDist.keys.flatMap { query ->
            val qTarget = targetDist[query] ?: emptyMap()
            val qSource = sourceDist[query] ?: emptyMap()
            val stats = getStats(qTarget, qSource)
            summarizeStats(stats, qSource.size, query) }

        queryStats.groupBy { it.name }
            .forEach { (name, stats) ->
                val nRel = stats.first().nRel

                if (name == "Bigrams") {
                    return stats.map { EvalStat(
                            query = it.query, method = "RougeF1", score = it.f1
                    ) }
                }

            }
        return emptyList()
    }

}