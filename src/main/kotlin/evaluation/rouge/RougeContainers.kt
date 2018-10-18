package evaluation.rouge

import utils.defaultWhenNotFinite
import kotlin.math.pow

data class RougeDoc(
        val id: String,
        val unigrams: Set<String> = emptySet(),
        val bigrams: Set<String> = emptySet(),
        val entities: Map<String, Double> = emptyMap()) {

    fun quickF1(target: RougeDoc): Double {
        val precision = precisionFun(bigrams, target.bigrams)
        val recall = recallFun(bigrams, target.bigrams)
        val f1 = ((2 * precision * recall) / (precision + recall)).defaultWhenNotFinite(0.0)
        return f1
    }


    fun entityQuickF1(target: RougeDoc): Double {
        val precision = precisionFun(entities.keys, target.entities.keys)
        val recall = recallFun(entities.keys, target.entities.keys)
        val f1 = ((2 * precision * recall) / (precision + recall)).defaultWhenNotFinite(0.0)
//
//        val selfNorm = entities.values.map { it.pow(2.0) }.sum().pow(0.5)
//        val targetNorm = target.entities.values.map { it.pow(2.0) }.sum().pow(0.5)
//
//        val f1 = target.entities.entries.sumByDouble { (entity, rho) ->
//            rho * (entities[entity] ?: 0.0) } / (selfNorm * targetNorm)
        return f1
    }
}

data class RougeDocStats(
        val doc: RougeDoc,
        val statFunctions: ArrayList<RougeEvaluationFunction> = ArrayList() ) {
    fun calculateStats(target: RougeDoc) = statFunctions.forEach { f -> f.calcPrecisionRecallF1(target) }
}


class RougeEvaluationFunction(
        val name: String,
        val query: String,
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

//private fun precisionRhoFun(source: Map<String, Double>, target: Map<String, Double>): Double {
//    val totalSource = source.values.map { it.pow(2.0) }.sum()
//
//    val intersection = (target.intersect(source)).size
//    return intersection / totalSource
//}
//
//private fun recallRhoFun(source: Set<String>, target: Set<String>): Double {
//    val totalTarget = target.size.toDouble()
//    val intersection = (target.intersect(source)).size
//    return intersection / totalTarget
//}
