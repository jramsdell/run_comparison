package evaluation.rouge

import utils.lucene.getIndexSearcher


class RougeentityF1(runDirectoryLoc: String, qrelLoc: String, sourceIndex: String, targetIndex: String) {
    val sourceSearcher = getIndexSearcher(sourceIndex)
    val targetSearcher = getIndexSearcher(targetIndex)
    val docRetriever = RougeDocRetriever(sourceSearcher, targetSearcher, true)

    val runRetriever = RougeRunRetriever(runDirectoryLoc, qrelLoc, docRetriever)

//    fun run() {
//        val firstRun = runRetriever.runs.first()
//        val result = runRetriever.computeF1(firstRun.first, firstRun.second)
//        println(result)
//    }

}