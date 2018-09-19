package indexing

import co.nstant.`in`.cbor.CborDecoder
import co.nstant.`in`.cbor.model.Array
import co.nstant.`in`.cbor.model.ByteString
import edu.unh.cs.treccar_v2.Data
import edu.unh.cs.treccar_v2.read_data.CborDataItemIterator
import edu.unh.cs.treccar_v2.read_data.DeserializeData
import org.apache.lucene.index.IndexWriterConfig
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.StreamSupport
import co.nstant.`in`.cbor.model.DataItem
import co.nstant.`in`.cbor.model.UnicodeString
import edu.unh.cs.treccar_v2.read_data.CborListWithHeaderIterator
import org.apache.lucene.document.Document
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.Term
import org.apache.lucene.search.IndexSearcher
import utils.AnalyzerFunctions
import utils.AnalyzerFunctions.AnalyzerType.ANALYZER_ENGLISH_STOPPED
import utils.lucene.*
import org.apache.lucene.analysis.StopwordAnalyzerBase
import org.apache.lucene.document.Field
import org.apache.lucene.document.StoredField
import org.apache.lucene.document.TextField
import utils.AnalyzerFunctions.AnalyzerType.ANALYZER_STANDARD_STOPPED
import utils.forEachParallelQ
import utils.getIndexWriter
import java.lang.Integer.min


class TqaIndexer() {
//    val corpus = "/home/jsc57/data/corpus/paragraphCorpus/dedup.articles-paragraphs.cbor"
    val paragraphCounter = AtomicInteger()
    val paragraphIndex = getIndexWriter("/home/jsc57/data/backup/old_corpus_1_5/index", mode = IndexWriterConfig.OpenMode.CREATE)
//    val linker = SpotlightEntityLinker("/home/jsc57/projects/jsr-joint-learning/spotlight_server")
//        .apply { (0 until 100).forEach { queryServer("Test") }   }






    fun processParagraphs(paragraph: Data.Paragraph) {
        val doc = Document()
        doc.add(TextField("paragraphid", paragraph.paraId, Field.Store.YES))
        doc.add(TextField("text", paragraph.textOnly, Field.Store.YES))
        paragraphIndex.addDocument(doc)

    }

    fun run() {
        val corpusStream = File("/home/jsc57/data/old_v1_5_corpus/paragraphcorpus/paragraphcorpus.cbor")
            .inputStream()
            .buffered()
            DeserializeData.iterableParagraphs(corpusStream)
                .forEachParallelQ(1000, 30) { paragraph: Data.Paragraph ->
                    processParagraphs(paragraph)
                    val result = paragraphCounter.incrementAndGet()
                    if (result % 10000 == 0) {
                        println(result)
                        paragraphIndex.commit()
                    }
                }
        paragraphIndex.commit()
        paragraphIndex.close()
    }

}



