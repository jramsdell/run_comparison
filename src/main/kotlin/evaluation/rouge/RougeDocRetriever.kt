package evaluation.rouge

import khttp.get
import org.apache.lucene.index.Term
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.TermQuery
import utils.AnalyzerFunctions
import java.util.concurrent.ConcurrentHashMap


class RougeDocRetriever( val sourceSearcher: IndexSearcher, val targetSearcher: IndexSearcher,
                         useEntityLinker: Boolean = false) {

    val linker = if (useEntityLinker) DocAnnotatorComponent() else null
    val memoizedDocs = ConcurrentHashMap<String, RougeDoc?>()

    // (Index, PID) -> (DocId, DocText)
    fun getSearchResult(searcher: IndexSearcher, id: String): Pair<String, String>? {
        val result = searcher.search(TermQuery(Term("paragraphid", id)), 1)
            .scoreDocs
            .firstOrNull()
        return result?.doc?.let { id to searcher.doc(it).get("text") }
    }

    // (Preferred Index, PID) -> (DocId, DocText) from either source or target index
    fun retrieveDocumentPair(preferredIndexSearcher: IndexSearcher, id: String): Pair<String, String>? =
            getSearchResult(preferredIndexSearcher, id) ?:

                    // If this fails, try the other index searcher!
                    if (preferredIndexSearcher == sourceSearcher)
                        getSearchResult(targetSearcher, id)
                    else
                        getSearchResult(sourceSearcher, id)


//    fun retrieveValidDocs(preferredIndexSearcher: IndexSearcher, ids: List<String>) =
//            ids.mapNotNull { id -> getSearchResult(preferredIndexSearcher, id) }


    fun getRougeDoc(preferredIndexSearcher: IndexSearcher, id: String) =
            memoizedDocs.computeIfAbsent(id) {
                val doc = retrieveDocumentPair(preferredIndexSearcher, id)
                doc?.let { (_, text) ->
                    val entities = linker?.annotateDoc(id, text)?.toSet() ?: emptySet()

                    val tokens = AnalyzerFunctions.createTokenList(text, AnalyzerFunctions.AnalyzerType.ANALYZER_ENGLISH_STOPPED)
                    val unigrams = tokens.toSet()
                    val bigrams = tokens.windowed(2, 1, false)
                        .map { (t1, t2) -> t1 + "_" + t2 }
                        .toSet()

                    RougeDoc(id, unigrams = unigrams, bigrams = bigrams, entities = entities.toMap())
                }
            }


    fun getRougeDocs(preferredIndexSearcher: IndexSearcher, ids: List<String>) =
            ids.mapNotNull { id -> getRougeDoc(preferredIndexSearcher, id) }





}