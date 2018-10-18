package evaluation.rouge

import io.ConcurrentConnectionManager
import org.apache.lucene.search.IndexSearcher
import org.json.JSONException
import org.json.JSONObject
import utils.doIORequest
import utils.lucene.getIndexSearcher
import java.util.concurrent.ConcurrentHashMap



class DocAnnotatorComponent() {
    val manager = ConcurrentConnectionManager()
    private val tok = "7fa2ade3-fce7-4f4a-b994-6f6fefc7e665-843339462"

    val memoizedEntityLinks = ConcurrentHashMap<String, List<Pair<String, Double>>>()

    fun annotateDoc(id: String, text: String) =
        memoizedEntityLinks.computeIfAbsent(id) { doTagMeQuery(text) }


    // Text -> List of (Entity,Rho)
    private fun doTagMeQuery(content: String, minRho: Double = 0.2): List<Pair<String, Double>> {
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


