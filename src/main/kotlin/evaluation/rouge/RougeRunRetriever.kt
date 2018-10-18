package evaluation.rouge

import org.apache.lucene.search.IndexSearcher
import utils.ParseFunctions
import utils.pmap
import java.io.File




typealias QueryId = String
typealias DocId = String
typealias RunName = String


class RougeRunRetriever(runDirectoryLoc: String, qrelLoc: String,
                        val docRetriever: RougeDocRetriever) {


    val runs: List<Pair<RunName, Map<QueryId, Map<DocId, RougeDoc>>>> = readRunDirectory(runDirectoryLoc, 5)
    val relevantDocs: Map<QueryId, Map<DocId, RougeDoc>> = readRunfile(File(qrelLoc))


    fun readRunDirectory(runDirectoryLoc: String, cutoff: Int = 0) =
            File(runDirectoryLoc)
                .listFiles()
                .map { file ->
                    file.nameWithoutExtension to readRunfile(file, cutoff)
                }

    fun readRunfile(runfile: File, cutoff: Int = 0) =
            ParseFunctions
                .parseRunfile(runfile)
                .entries
                .sortedBy { it.key }
//                .take(5)
                .pmap { (query, pids) ->
                    val finalPids = if(cutoff == 0) pids else pids.take(cutoff)
                    val rougeDocs =
                            docRetriever.getRougeDocs(docRetriever.sourceSearcher, finalPids)
                                .map { doc -> doc.id to doc }
                                .toMap()
                    println(query)
                    query to rougeDocs
                }
                .toMap()


    fun computeF1(runMappings: Map<QueryId, Map<DocId, RougeDoc>>) =
        runMappings
            .map { (qid, runMap) ->
                val relMap = relevantDocs[qid] ?: emptyMap()

                val score = runMap
                    .map { (_, doc) ->
                        val maxF1 = relMap.values.map { relDoc -> doc.entityQuickF1(relDoc) }.max() ?: 0.0
                        maxF1 }
                    .average()
                    .let { result -> if (result.isFinite()) result else 0.0 }

                qid to score
            }



    fun computeAllF1(qrelName: String) {
        File("super_awesome_evals/$qrelName/").run { if(!exists()) mkdirs() }
        runs.forEach { (runName, runMappings) ->
            val out = File("super_awesome_evals/$qrelName/$runName").bufferedWriter()
            val scores = computeF1(runMappings)
            scores.forEach { (qid, score) -> out.write("RougeF1\t$qid\t$score\n") }
            out.close()
        }
    }



}