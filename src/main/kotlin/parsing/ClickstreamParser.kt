package parsing

import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.TermQuery
import utils.lucene.getIndexSearcher
import java.io.File

private data class QrelData(
        val query: String,
        val base: String,
        val entity: String,
        val rawEntity: String,
        var clicks: Int = 0
)

private data class ParagraphQrelData(
        val query: String,
        val pid: String,
        val entities: List<String>,
        var clicks: Int = 0
)

private fun cleanEntity(entry: String) =
        entry.replace("enwiki:", "")
            .replace("tqa:", "")
            .replace("%20", "_")
            .replace(" ", "_")

private fun reconvertEntity(entry: String) =
        "enwiki:" + entry.replace("_", "%20")


private fun getPageSet2(qrelFile: String): List<QrelData> =
        File(qrelFile)
            .readLines()
            .map { line ->
                val elements = line.split(" ")
                val page = elements[0]
                val rawEntity = elements[2]
                QrelData(
                        query = elements[0],
                        base = cleanEntity(page).split("/").first(),
                        entity = cleanEntity(rawEntity),
                        rawEntity = rawEntity)
            }
//            .toSet()

private fun getClickData2(clickStreamLoc: String, qrelData: List<QrelData>): List<QrelData> {
//    val pageMappings = HashMap<Pair<String, String>, Int>()
    val reader = File(clickStreamLoc).bufferedReader()
    val qrelMap = qrelData.map { q ->
        (q.base to q.entity) to q }
        .toMap()

    reader.forEachLine { line ->
        val elements = line.split("\t")
        val key = elements[0] to elements[1]
        if (key in qrelMap) {
            qrelMap[key]!!.clicks = elements[3].toInt()
        }
    }

    return qrelData.filter { it.clicks > 0 }

}


private fun reconstructQrels2(qrelClicks: List<QrelData>) {
    val out = File("entity_clicks.qrels").bufferedWriter()
//    val qrels = ArrayList<Triple<String, String, String>>()
    qrelClicks.groupBy { it.base }
        .forEach { (_, qrels) ->
            qrels.sortedByDescending { it.clicks }
                .forEach { qrel ->
                    out.write("${qrel.query} 0 ${reconvertEntity(qrel.entity)} ${qrel.clicks}\n")
                }
        }

    out.close()
}

private fun getParaToEntities(paraQrelLoc: String, searcher: IndexSearcher) =
        File(paraQrelLoc)
            .bufferedReader()
            .readLines()
            .mapNotNull { line ->
                val elements = line.split(" ")
                val query = elements[0]
                val pid = elements[2]
//                val bool = BooleanQuery.Builder()
//                    .add(TermQuery(Term("pid", pid)), BooleanClause.Occur.SHOULD)
//                    .build()

                val docId = searcher.search(TermQuery(Term("paragraphid", pid)), 1)
//                val docId = searcher.search(bool, 1)
                    .scoreDocs
                    .firstOrNull()?.doc

                if (docId != null) {
                    val doc = searcher.doc(docId)
                    val entities = doc.get("spotlight")
                        .split("\\s+".toRegex())

                    ParagraphQrelData(
                            pid = pid,
                            entities = entities,
                            query = query )
                } else null
            }


private fun createEntityClickMap(qrelClicks: List<QrelData>) =
    qrelClicks.groupBy { it.query }
        .map { (query, clickData) ->
            val clickMap = clickData.map { it.entity to it.clicks }.toMap()
            query to clickMap }
        .toMap()

private fun writeParagraphClickQrel(entityClickMap: Map<String, Map<String, Int>>, paraQrels: List<ParagraphQrelData>) {
    val out = File("paragraph_clicks.qrels").bufferedWriter()
    paraQrels
        .groupBy { it.query }
        .forEach { (query, pQrels) ->
            val queryMap = entityClickMap[query] ?: emptyMap()

            pQrels.forEach { p ->
//                p.clicks = p.entities.sumBy { entity -> queryMap[reconvertEntity(entity)] ?: 0 }
                p.clicks = p.entities.sumBy { entity -> queryMap[entity] ?: 0 }
            }

            pQrels.sortedByDescending { it.clicks }
                .filter { it.clicks > 0 }
                .forEach { p ->
                    out.write("$query 0 ${p.pid} ${p.clicks}\n")
                }
        }

    out.close()
}

fun createParagraphClickQrels(indexLoc: String, paraQrelLoc: String,
                                      entityQrelLoc: String, clickLoc: String) {
    val searcher = getIndexSearcher(indexLoc)
    val paraQrels = getParaToEntities(paraQrelLoc, searcher)
    val qrelData = getPageSet2(entityQrelLoc)
    val qrelClicks = getClickData2(clickLoc, qrelData)
    val entityClickMap = createEntityClickMap(qrelClicks)

    writeParagraphClickQrel(entityClickMap, paraQrels)
}


fun doParse(pageLoc: String, clickLoc: String) {
    val qrelData = getPageSet2(pageLoc)
    val qrelClicks = getClickData2(clickLoc, qrelData)
    reconstructQrels2(qrelClicks)
}


