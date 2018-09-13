package evaluation.rouge

import utils.defaultWhenNotFinite

data class RougeDoc(
        val id: String,
        val unigrams: Set<String>,
        val bigrams: Set<String>
)

data class RougeDocStats(
        val doc: RougeDoc,
        val statFunctions: ArrayList<RougeEvaluationFunction> = ArrayList() ) {
    fun calculateStats(target: RougeDoc) = statFunctions.forEach { f -> f.calcPrecisionRecallF1(target) }
}


class RougeEvaluationFunction(
        val name: String,
        val doc: RougeDoc,
        var precision: Double = 0.0,
        var recall: Double = 0.0,
        var f1: Double = 0.0,
        val docSetRetriever: (RougeDoc) -> Set<String> = { it.unigrams } )  {

    fun calcPrecisionRecallF1(target: RougeDoc) {
        val sourceSet = docSetRetriever(doc)
        val targetSet = docSetRetriever(target)
        val precisionV = precisionFun(sourceSet, targetSet).defaultWhenNotFinite(0.0)
        val recallV = recallFun(sourceSet, targetSet).defaultWhenNotFinite(0.0)
        val f1V = ((2 * precisionV * recallV) / (precisionV + recallV)).defaultWhenNotFinite(0.0)
        precision = Math.max(precisionV, precision)
        recall = Math.max(recall, recallV)
        f1 = Math.max(f1, f1V)
    }


}


private fun precisionFun(source: Set<String>, target: Set<String>): Double {
    val totalSource = source.size.toDouble()
    val intersection = (target.intersect(source)).size
    return intersection / totalSource
}

private fun recallFun(source: Set<String>, target: Set<String>): Double {
    val totalTarget = target.size.toDouble()
    val intersection = (target.intersect(source)).size
    return intersection / totalTarget
}
