package annotating

import utils.lucene.getIndexSearcher
import java.io.File

private data class RunLine(
        val query: String,
        val id: String,
        val rank: Int
)


class CreateLinksForRouge(sourceIndex: String, targetIndex: String, runLoc: String, qrels: String) {
    val sourceSearcher = getIndexSearcher(sourceIndex)
    val targetSearcher = getIndexSearcher(targetIndex)
    val runfiles = File(runLoc).listFiles()

    private fun parseRunfile(runfile: File) =
            runfile.bufferedReader()
                .readLines()
                .map { line ->
                    val elements = line.split(" ")
                    RunLine(
                            query = elements[0],
                            id = elements[2],
                            rank = elements[3].toInt()
                    )
                }


    fun run() {

    }

}