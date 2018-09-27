import annotating.TagmeAnnotator
import evaluation.EvalAnalyzer
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
//            Triple("/home/jsc57/new_qrels/unfiltered/all-paragraph-automatic-enwiki.qrels", "/home/jsc57/new_qrels/unfiltered/rouge_auto_enwiki.qrels",  "rouge_automatic_enwiki"),
//            Triple("/home/jsc57/new_qrels/unfiltered/all-paragraph-automatic.qrels", "/home/jsc57/new_qrels/unfiltered/rouge_auto_tqa.qrels",  "rouge_automatic_tqa"),
//            Triple("/home/jsc57/new_qrels/unfiltered/all-paragraph-manual-enwiki.qrels", "/home/jsc57/new_qrels/unfiltered/rouge_manual_enwiki.qrels",  "rouge_manual_enwiki"),
//            Triple("/home/jsc57/new_qrels/unfiltered/all-paragraph-manual-tqa.qrels", "/home/jsc57/new_qrels/unfiltered/rouge_manual_tqa.qrels",  "rouge_manual_tqa"),
            Triple("/home/jsc57/new_qrels/unfiltered/rouge_generated_tqa_auto.qrels", "/home/jsc57/new_qrels/unfiltered/rouge_auto_tqa.qrels",  "rouge_generated_automatic_tqa")
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


//    createSpearmanMatrix(results)
//    println("=====================")
//
//    println(" \\textbf{Run} & " + rankings.map { " \\textbf{" + it.first + "} " }.joinToString(" & ") + "\\\\\\hline")
//    val n = rankings.first().second.size
//    (0 until n).forEach { index ->
//        val ranks = rankings.map { it.second[index].f1.rank }.joinToString(" & ")
//        println(rankings.first().second[index].name + " & " + ranks + "\\\\\\hline")
//    }
//
//    val onlyManual = listOf(rankings[2], rankings[3])
//    println(" \\textbf{Run} & " + onlyManual.map { " \\textbf{" + it.first + "} " }.joinToString(" & ") + "\\\\\\hline")
//    (0 until n).forEach { index ->
//        val ranks = onlyManual.map { it.second[index].map.rank }.joinToString(" & ")
//        println(onlyManual.first().second[index].name + " & " + ranks + "\\\\\\hline")
//    }
//
//
//    // MAP
//    println("========")
//    createSpearmanMatrix(listOf(
//            results2[0], results2[2], results2[3]
//    ))
}

fun generateEvals() {
    val trec = "/home/jsc57/programs/trec_eval"
    val runfiles = "/home/jsc57/fixed_psg_runs"
    val qrels = listOf(
            Triple("/home/jsc57/new_qrels/unfiltered/all-paragraph-automatic.qrels", "/home/jsc57/new_qrels/unfiltered/rouge_auto.qrels",  "automatic"),
            Triple("/home/jsc57/new_qrels/unfiltered/all-paragraph-manual.qrels", "/home/jsc57/new_qrels/unfiltered/rouge_manual.qrels",  "manual")
//            Triple("/home/jsc57/new_qrels/unfiltered/all-paragraph-manual-enwiki.qrels", "/home/jsc57/new_qrels/unfiltered/rouge_manual_enwiki.qrels",  "manual_enwiki"),
//            Triple("/home/jsc57/new_qrels/unfiltered/all-paragraph-manual-tqa.qrels", "/home/jsc57/new_qrels/unfiltered/rouge_manual_tqa.qrels",  "manual_tqa"),
//            Triple("/home/jsc57/new_qrels/unfiltered/all-paragraph-automatic-enwiki.qrels", "/home/jsc57/new_qrels/unfiltered/rouge_auto_enwiki.qrels",  "automatic_enwiki"),
//            Triple("/home/jsc57/new_qrels/unfiltered/rouge_generated_tqa_auto.qrels", "/home/jsc57/new_qrels/unfiltered/rouge_auto_tqa.qrels",  "automatic_tqa")
//            Triple("/home/jsc57/new_qrels/unfiltered/rouge_generated_tqa_auto.qrels", "/home/jsc57/new_qrels/unfiltered/rouge_auto_enwiki.qrels",  "gen_automatic_enwiki")
    )

    val runner = RougeExperimentRunner(trec, runfiles)
    qrels.forEach { (qrel, rougeQrel, name) ->
        runner.generateEvals(qrel, rougeQrel, name,
                "/home/jsc57/data/backup/extractions/paragraph2",
                "/home/jsc57/data/backup/y2_benchmark/index")
    }
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
//    val tqaAuto = "/home/jsc57/new_qrels/unfiltered/all-paragraph-automatic-tqa.qrels"
    val enwikiAuto = "/home/jsc57/new_qrels/unfiltered/all-paragraph-automatic-enwiki.qrels"

    val runner = RougeExperimentRunner(trec, runfiles)
    runner.generateRougeQrels(enwikiAuto, "enwiki_generated_automatic",
            "/home/jsc57/data/backup/extractions/paragraph2",
            "/home/jsc57/data/backup/y2_benchmark/index" )

}

fun generateEntityEvals() {
    val trec = "/home/jsc57/programs/trec_eval"
    val automaticEntity = "/home/ben/trec-car/data/benchmarkY2/benchmarkY2/benchmarkY2.cbor.tree.entity.qrels"
    val manualEntity = "/mnt/grapes/share/trec-car-allruns-2018/all-judgments/manual/all-entity.qrels"
    val clickQrels = "/home/jsc57/projects/run_comparison/click_qrels_tree.qrels"
    val mergedQrels = "/home/jsc57/projects/run_comparison/merged.qrels"

    val runfiles = "/home/jsc57/entruns"
    val qrels = listOf(
            Triple(automaticEntity, automaticEntity, "automatic_entity"),
            Triple(manualEntity, manualEntity, "manual_entity"),
            Triple(clickQrels, clickQrels, "click_entity")
//            Triple(clickQrels, clickQrels, "merged_entity")
//            Triple("/home/jsc57/new_qrels/unfiltered/all-paragraph-manual.qrels", "/home/jsc57/new_qrels/unfiltered/rouge_manual.qrels",  "manual"),
//            Triple("/home/jsc57/new_qrels/unfiltered/all-paragraph-manual-enwiki.qrels", "/home/jsc57/new_qrels/unfiltered/rouge_manual_enwiki.qrels",  "manual_enwiki"),
//            Triple("/home/jsc57/new_qrels/unfiltered/all-paragraph-manual-tqa.qrels", "/home/jsc57/new_qrels/unfiltered/rouge_manual_tqa.qrels",  "manual_tqa"),
//            Triple("/home/jsc57/new_qrels/unfiltered/all-paragraph-automatic-enwiki.qrels", "/home/jsc57/new_qrels/unfiltered/rouge_auto_enwiki.qrels",  "automatic_enwiki"),
//            Triple("/home/jsc57/new_qrels/unfiltered/rouge_generated_tqa_auto.qrels", "/home/jsc57/new_qrels/unfiltered/rouge_auto_tqa.qrels",  "automatic_tqa")
//            Triple("/home/jsc57/new_qrels/unfiltered/rouge_generated_tqa_auto.qrels", "/home/jsc57/new_qrels/unfiltered/rouge_auto_enwiki.qrels",  "gen_automatic_enwiki")
    )

    val runner = RougeExperimentRunner(trec, runfiles)
    qrels.forEach { (qrel, rougeQrel, name) ->
        runner.generateEvals(qrel, rougeQrel, name,
                "/home/jsc57/data/backup/extractions/paragraph2",
                "/home/jsc57/data/backup/y2_benchmark/index",
                outputDir = "entity_eval_results",
                noRouge = true)
    }
}


fun analyzeEvals() {
    val analyzer = EvalAnalyzer()
    analyzer.analyzeEvalResults("/home/jsc57/projects/run_comparison/eval_results")
}

fun analyzeEntityEvals() {
    val analyzer = EvalAnalyzer()
    analyzer.analyzeEvalResults("/home/jsc57/projects/run_comparison/entity_eval_results")
}

fun analyzeNewEvals() {
    val analyzer = EvalAnalyzer()
    analyzer.analyzeEvalResults("/home/jsc57/projects/run_comparison/new_passage_eval_results")
}


fun quickDirtyConvert() {
    QuickAndDirtyHierToTree("/home/jsc57/projects/run_comparison/prototype_2_illegal.qrels",
            "prototype_2_illegal_tree.qrels")
        .run()
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
//    evaluateTqaEnwikiQrels()
    analyzeEvals()

//    generateRougeQrels()
//    generateEvals()
//    generateEntityEvals()
//    analyzeEvals()
//    analyzeEntityEvals()
//    analyzeNewEvals()
//    getIds()
//    testSpec()
//    doSearch()
//    fixQrels()
//    QuickAndDirtyParagraphIndexer()
//        .run()

//    quickDirtyConvert()
    System.exit(0)

}