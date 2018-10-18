package annotating

import edu.unh.cs.treccar_v2.Data
import edu.unh.cs.treccar_v2.read_data.DeserializeData
import io.ConcurrentConnectionManager
import org.json.JSONException
import org.json.JSONObject
import utils.doIORequest
import utils.lucene.foldOverSectionWithId
import utils.pmap
import java.io.File


class TagmeAnnotator(val cborLoc: String, entityMapLoc: String) {
    val manager = ConcurrentConnectionManager()
    private val tok = "7fa2ade3-fce7-4f4a-b994-6f6fefc7e665-843339462"

    val entityMap = File(entityMapLoc).bufferedReader()
        .readLines()
        .map { line ->
            val (id, rawEntity) = line.replace(" ", "_").split("\t")
            rawEntity to id }
        .toMap()

    fun parse(page: Data.Page): ArrayList<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()

        page.foldOverSectionWithId(useFilter = false) { path, section, paragraphs ->
//            println("$path : ${paragraphs.size}")
            val nPath = path.replace(" ", "%20")
            val entitiesInSection = HashMap<String, ArrayList<String>>()
//            println(path)
            paragraphs.forEach { p ->
                val pid = p.paraId
                val results =  doTagMeQuery(p.textOnly)
                    .distinctBy { it.first }

                results.forEach { (entity, _) ->
                    entitiesInSection.computeIfAbsent(entity) { ArrayList() }
                        .add(pid)
                }
            }
//
            entitiesInSection.forEach { (entity, support) ->
                var lookup = entityMap[entity] ?: ""
                if (lookup == "") {
                    println("Something went wrong for: $entity ... at path: $nPath")
                    lookup = entity
                    out.add(nPath to "$nPath Q0 $lookup 1")
                } else {
//                val converted = entity.replace("_", " ")
//                    .replace(" ", "%20")
//                    .run { "enwiki:" + this }
                    out.add(nPath to "$nPath Q0 $lookup 1")
//                    support.forEach { supportPar ->
////                        out.add(nPath to "$nPath Q0 $supportPar/$lookup 1")
//                        out.add(nPath to "$nPath Q0 $lookup 1")
//                    }
                }
            }
        }

        return out
    }
//
    fun run() {
        println("File is: $cborLoc")
        val iFile = File(cborLoc)
            .inputStream()
            .buffered()

        val outFile = File("prototype_2_illegal.qrels").bufferedWriter()

        DeserializeData.iterableAnnotations(iFile)
//            .filter { it.pageId.startsWith("enwiki:") }
            .pmap { page -> parse(page) }
            .flatten()
            .sortedBy { it.first }
            .forEach { outFile.write(it.second + "\n") }

        outFile.flush()
        outFile.close()

    }

    fun doTagMeQuery(content: String, minRho: Double = 0.2): List<Pair<String, Double>> {
        val url = "https://tagme.d4science.org/tagme/tag"

        try {
            val json = doIORequest {
                manager.doPostOrGetRequest(url, data = mapOf("gcube-token" to tok, "text" to content))
            } ?: return emptyList()

            // Turn results into a JSon array and retrieve linked entities (and their rho values)
            val results = json.getJSONArray("annotations")
            return results
                .mapNotNull { result ->
                    (result as JSONObject).run {
                        if (getDouble("rho") <= minRho) null
                        else getString("title").replace(" ", "_") to getDouble("rho")
                    }
                }
        } catch (e: JSONException) {
            return emptyList()
        }
    }


}