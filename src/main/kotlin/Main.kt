import annotating.TagmeAnnotator
import evaluation.rouge.RougeEvaluator
import experiment.ExperimentRunner
import experiment.RankStat
import experiment.RougeExperimentRunner
import indexing.QuickAndDirtyParagraphIndexer
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.TermQuery
import parsing.*
import utils.AnalyzerFunctions
import utils.createSpearmanMatrix
import utils.getSpearman
import utils.lucene.getIndexSearcher
import java.io.File

fun generateClickQrels() {
    val clickstreamLoc = "/home/jsc57/data/clickstream/clickstream-enwiki-2017-11.tsv"
    val qrelParams = ClickstreamQrelCombos.TestTree

//    val qrels = "/home/jsc57/data/benchmark/benchmarkY1/benchmarkY1-train/train.pages.cbor-article.entity.qrels"
//    val hierQrels = "/home/jsc57/y2_tree_entity.qrels"
    val hierQrels = "/home/jsc57/y2_tree_entity.qrels"
//    val spotQrels = "/mnt/grapes/share/trec-car-allruns-2018/joinparas/joined.tree.entity.qrels"
//    val hierQrels = "/home/jsc57/data/benchmark/benchmarkY1/benchmarkY1-train/train.pages.cbor-hierarchical.entity.qrels"

    doParse(qrelParams.entityQrelLoc, clickstreamLoc)
//    doParse(hierQrels, clickstreamLoc)

//    createParagraphClickQrels(
//            indexLoc = "/home/jsc57/data/backup/extractions/paragraph2/",
//            paraQrelLoc = qrelParams.paragraphQrelLoc,
//            entityQrelLoc = qrelParams.entityQrelLoc,
//            clickLoc = clickstreamLoc
//    )
}

fun runPageAnnotator() {
    val cbor = "/mnt/grapes/share/trec-car-allruns-2018/benchmarkY2/benchmarkY2.cbor"
    val entityMap = "/home/jsc57/data/extra/unprocessedAllButBenchmark.Y2.legal-entities.tsv"
    TagmeAnnotator(cbor, entityMap)
        .run()

//    PageCborParser()
//        .parse(cbor)

}

fun runComparison() {
    val trec = "/home/jsc57/programs/trec_eval"
    val spotQrels = "/mnt/grapes/share/trec-car-allruns-2018/joinparas/joined.tree.entity.qrels"
//    val treeQrels = "/home/jsc57/y2_tree_entity.qrels"
    val rougeManualQrels = "/home/jsc57/new_qrels/filtered/paragraph_rouge_manual.qrels"
    val runner = ExperimentRunner(trec, "/home/jsc57/entruns")
    val clickQrels = "/home/jsc57/projects/run_comparison/entity_clicks.qrels"
    val enwikiQrels = "merged.qrels"

//    runner.evaluateQrel(hierQrels)
    println("========")
//    runner.evaluateQrel(qrels2)
//    println("========")
//    runner.evaluateQrel(spotQrels)
    println("========")
//    runner.evaluateQrel(enwikiQrels, "tagme_annotated")
//    runner.evaluateQrel(treeQrels, "tree_standard")
    runner.evaluateQrel(rougeManualQrels, "rouge_manual")
//    runner.evaluateQrel(clickQrels, "clickstream")
}


//fun testParse() {
//    val evaluator = RougeEvaluator("/home/jsc57/data/backup/extractions/paragraph2", "/home/ben/trec-car/data/benchmarkY2/benchmarkY2/benchmarkY2.cbor.tree.qrels")
//    evaluator.compare("/mnt/grapes/share/trec-car-allruns-2018/psgruns/UNH-p-sdm")
//
//}

fun testRouge() {
//    val rougeManualQrels = "/home/jsc57/new_qrels/filtered/paragraph_rouge_manual.qrels"
    val rougeAutomaticQrels = "/home/jsc57/new_qrels/filtered/paragraph_rouge_automatic.qrels"
    val trec = "/home/jsc57/programs/trec_eval"
//    val qrels = "/home/jsc57/data/benchmark/test/benchmarkY1/benchmarkY1-test/test.pages.cbor-hierarchical.qrels"
//    val qrels = "/home/jsc57/new_qrels/y2_manual_passage_enwiki.qrels"
//    val qrels = "/home/jsc57/new_qrels/tree_qrels_fixed.qrels"
    val unfilteredRougeAutomaticQrels = "/home/jsc57/new_qrels/unfiltered/rouge_auto.qrels"
//    val unfilteredRougeAutomaticQrels = "/home/jsc57/new_qrels/unfiltered/rouge_manual.qrels"
//    val qrels = "/home/jsc57/tree_tqa.qrels"
//    val runfiles = "/mnt/grapes/share/car2017/psg-all"
    val runfiles = "/mnt/grapes/share/trec-car-allruns-2018/psgruns"

    val runner = RougeExperimentRunner(trec, runfiles)
//    runner.evaluateQrel(qrels, "Hierarchical", "/home/jsc57/data/backup/extractions/paragraph2" )
//    runner.evaluateQrel(qrels, "Hierarchical",
//            "/home/jsc57/data/backup/extractions/paragraph2",
//            "/home/jsc57/data/backup/y2_benchmark/index" )

//    runner.evaluateQrel(unfilteredRougeAutomaticQrels, "rouge_automatic",
//            "/home/jsc57/data/backup/extractions/paragraph2",
//            "/home/jsc57/data/backup/y2_benchmark/index" )

//    runner.evaluateQrel(unfilteredRougeAutomaticQrels, "rouge_manual",
//            "/home/jsc57/data/backup/extractions/paragraph2",
//            "/home/jsc57/data/backup/extractions/paragraph2")

}

//fun evaluateAutomaticRouge() {
//    val trec = "/home/jsc57/programs/trec_eval"
//    val unfilteredRougeAutomaticQrels = "/home/jsc57/new_qrels/unfiltered/rouge_auto.qrels"
//    val runfiles = "/mnt/grapes/share/trec-car-allruns-2018/psgruns"
//    val runner = RougeExperimentRunner(trec, runfiles)
//    runner.evaluateQrel(unfilteredRougeAutomaticQrels, "rouge_automatic",
//            "/home/jsc57/data/backup/extractions/paragraph2",
//            "/home/jsc57/data/backup/y2_benchmark/index" )
//}
//
//fun evaluateManualRouge() {
//    val trec = "/home/jsc57/programs/trec_eval"
//    val unfilteredRougeAutomaticQrels = "/home/jsc57/new_qrels/unfiltered/rouge_manual.qrels"
//    val runfiles = "/mnt/grapes/share/trec-car-allruns-2018/psgruns"
//
//    val runner = RougeExperimentRunner(trec, runfiles)
//    runner.evaluateQrel(unfilteredRougeAutomaticQrels, "rouge_manual",
//            "/home/jsc57/data/backup/extractions/paragraph2",
//            "/home/jsc57/data/backup/y2_benchmark/index" )
//}

fun evaluateSetQrels() {
    val trec = "/home/jsc57/programs/trec_eval"
    val unfilteredRougeAutomaticQrels = "/home/jsc57/new_qrels/unfiltered/rouge_manual.qrels"
//    val runfiles = "/mnt/grapes/share/trec-car-allruns-2018/psgruns"
    val runfiles = "/home/jsc57/fixed_psg_runs"
    val qrels = listOf(
            Triple("/home/jsc57/new_qrels/unfiltered/all-paragraph-automatic.qrels", "/home/jsc57/new_qrels/unfiltered/rouge_auto.qrels",  "rouge_automatic"),
            Triple("/home/jsc57/new_qrels/unfiltered/all-paragraph-manual.qrels", "/home/jsc57/new_qrels/unfiltered/rouge_manual.qrels",  "rouge_manual")
//            Triple("/home/jsc57/new_qrels/unfiltered/all-paragraph-agreement.qrels", "/home/jsc57/new_qrels/unfiltered/rouge_manual.qrels",  "rouge_agreement")
    )

    val runner = RougeExperimentRunner(trec, runfiles)
    val results = ArrayList<ArrayList<RankStat>>()
    qrels.forEach { (qrel, rougeQrel, name) ->
        val qrelResult = runner.evaluateQrel(qrel, rougeQrel, name,
                "/home/jsc57/data/backup/extractions/paragraph2",
                "/home/jsc57/data/backup/y2_benchmark/index" )
        results.add(qrelResult)
    }


    val auto = results[0]
    val manual = results[1]
    createSpearmanMatrix(auto, manual)
}


fun evaluateTqaEnwikiQrels() {
    val trec = "/home/jsc57/programs/trec_eval"
    val unfilteredRougeAutomaticQrels = "/home/jsc57/new_qrels/unfiltered/rouge_manual.qrels"
//    val runfiles = "/mnt/grapes/share/trec-car-allruns-2018/psgruns"
    val runfiles = "/home/jsc57/fixed_psg_runs"
    val qrels = listOf(
            Triple("/home/jsc57/new_qrels/unfiltered/all-paragraph-automatic.qrels", "/home/jsc57/new_qrels/unfiltered/rouge_auto_enwiki.qrels",  "rouge_automatic_enwiki"),
            Triple("/home/jsc57/new_qrels/unfiltered/all-paragraph-automatic.qrels", "/home/jsc57/new_qrels/unfiltered/rouge_auto_tqa.qrels",  "rouge_automatic_tqa"),
            Triple("/home/jsc57/new_qrels/unfiltered/all-paragraph-manual-enwiki.qrels", "/home/jsc57/new_qrels/unfiltered/rouge_manual_enwiki.qrels",  "rouge_manual_enwiki"),
            Triple("/home/jsc57/new_qrels/unfiltered/all-paragraph-manual-tqa.qrels", "/home/jsc57/new_qrels/unfiltered/rouge_manual_tqa.qrels",  "rouge_manual_tqa")
//            Triple("/home/jsc57/new_qrels/unfiltered/all-paragraph-agreement.qrels", "/home/jsc57/new_qrels/unfiltered/rouge_manual.qrels",  "rouge_agreement")
    )


    val runner = RougeExperimentRunner(trec, runfiles)
    val results = ArrayList<Pair<String, List<Int>>>()
    val rankings = ArrayList<Pair<String, List<RankStat>>>()
    val results2 = ArrayList<Pair<String, List<Int>>>()
    qrels.forEach { (qrel, rougeQrel, name) ->
        val qrelResult = runner.evaluateQrel(qrel, rougeQrel, name,
                "/home/jsc57/data/backup/extractions/paragraph2",
                "/home/jsc57/data/backup/y2_benchmark/index" )
        results.add(name to qrelResult.map { it.f1.rank })
        results2.add(name to qrelResult.map { it.map.rank })
        rankings.add(name to qrelResult)
    }

    createSpearmanMatrix(results)
    println("=====================")

    println(" \\textbf{Run} & " + rankings.map { " \\textbf{" + it.first + "} " }.joinToString(" & ") + "\\\\\\hline")
    val n = rankings.first().second.size
    (0 until n).forEach { index ->
        val ranks = rankings.map { it.second[index].f1.rank }.joinToString(" & ")
        println(rankings.first().second[index].name + " & " + ranks + "\\\\\\hline")
    }

    val onlyManual = listOf(rankings[2], rankings[3])
    println(" \\textbf{Run} & " + onlyManual.map { " \\textbf{" + it.first + "} " }.joinToString(" & ") + "\\\\\\hline")
    (0 until n).forEach { index ->
        val ranks = onlyManual.map { it.second[index].map.rank }.joinToString(" & ")
        println(onlyManual.first().second[index].name + " & " + ranks + "\\\\\\hline")
    }

    println("========")
    createSpearmanMatrix(results2.drop(2))





}



fun generateRougeQrels() {
    val rougeAutomaticQrels = "/home/jsc57/new_qrels/filtered/paragraph_rouge_automatic.qrels"
    val trec = "/home/jsc57/programs/trec_eval"
//    val qrels = "/home/jsc57/data/benchmark/test/benchmarkY1/benchmarkY1-test/test.pages.cbor-hierarchical.qrels"
//    val qrels = "/home/jsc57/new_qrels/y2_manual_passage_enwiki.qrels"
//    val qrels = "/home/jsc57/new_qrels/tree_qrels_fixed.qrels"
    val qrels = "/home/jsc57/new_qrels/fixed_enwiki_qrels.qrels"
//    val qrels = "/home/jsc57/tree_tqa.qrels"
//    val runfiles = "/mnt/grapes/share/car2017/psg-all"
    val runfiles = "/mnt/grapes/share/trec-car-allruns-2018/psgruns"

    val runner = RougeExperimentRunner(trec, runfiles)

    runner.generateRougeQrels(rougeAutomaticQrels, "rouge_automatic",
            "/home/jsc57/data/backup/extractions/paragraph2",
            "/home/jsc57/data/backup/extractions/paragraph2")

}


val potentials = """enwiki:Aerobic%20fermentation
enwiki:Aged%20care%20in%20Australia
enwiki:Algorithmic%20bias
enwiki:Alternative%20medicine
enwiki:Arab%20slave%20trade
enwiki:Atomic%20force%20microscopy
enwiki:Blockchain
enwiki:Blue-ringed%20octopus
enwiki:Breakdancing
enwiki:Chatbot
enwiki:Chiropractic
enwiki:Darknet%20market
enwiki:Daylight%20saving%20time
enwiki:Economics%20of%20bitcoin
enwiki:Eggnog
enwiki:Electric%20car
enwiki:Entomophagy
enwiki:Great%20white%20shark
enwiki:Gunshot%20wound
enwiki:Healthcare%20in%20Canada
enwiki:Herbalism
enwiki:Huguenots
enwiki:Hybrid%20electric%20vehicle
enwiki:Loot%20box
enwiki:Mate%20(drink)
enwiki:Microaggression
enwiki:Opioid%20epidemic
enwiki:Question%20mark
enwiki:Radiocarbon%20dating
enwiki:Salmon%20run
enwiki:Theory%20of%20forms
enwiki:Workhouse
enwiki:Xerostomia
enwiki:Zika%20fever""".trimIndent()


fun doSearch() {
    val pLoc = "/home/jsc57/data/backup/new/extractions/page"
    val searcher = getIndexSearcher(pLoc)

    potentials.split("\n")
        .forEach { line ->
            val tokens = AnalyzerFunctions.createTokenList(line.replace("enwiki:", "").replace("%20", " "))
            val q = tokens
                .map { TermQuery(Term("name", it)) }
                .fold(BooleanQuery.Builder()) { builder, token -> builder.add(token, BooleanClause.Occur.SHOULD) }
                .build()

            searcher.search(q, 3)
                .scoreDocs
                .forEach { sd ->

                    println(searcher.doc(sd.doc).get("name"))
                }
            println()
        }
}


fun main(args: Array<String>) {
    System.setProperty("file.encoding", "UTF-8")


//    generateClickQrels()
//    runPageAnnotator()
//    runComparison()
//    testParse()
//    evaluateAutomaticRouge()
//    evaluateManualRouge()
//    evaluateSetQrels()
    evaluateTqaEnwikiQrels()
//    getIds()
//    generateRougeQrels()
//    testSpec()
//    doSearch()
//    fixQrels()
//    QuickAndDirtyParagraphIndexer()
//        .run()

    System.exit(0)

}