package parsing

import edu.unh.cs.treccar_v2.read_data.DeserializeData
import utils.lucene.foldOverSection
import java.io.File


class PageCborParser() {
    fun parse(cborLoc: String) {
        val stream = File(cborLoc).inputStream().buffered()
        var count = 0
        DeserializeData.iterableAnnotations(stream)
            .filter { page -> page.pageId.startsWith("enwiki:") }
            .forEach { page ->
                page.foldOverSection(false) { path, section, paragraphs ->
                    println(path)
                }
                count += 1

            }

        println(count)
    }
}