import annotating.TagmeAnnotator
import experiment.ExperimentRunner
import parsing.*

fun generateClickQrels() {
    val clickstreamLoc = "/home/jsc57/data/clickstream/clickstream-enwiki-2017-11.tsv"
    val qrelParams = ClickstreamQrelCombos.TestTree

//    val qrels = "/home/jsc57/data/benchmark/benchmarkY1/benchmarkY1-train/train.pages.cbor-article.entity.qrels"
//    val hierQrels = "/home/jsc57/y2_tree_entity.qrels"
    val hierQrels = "/home/jsc57/y2_tree_entity.qrels"
//    val spotQrels = "/mnt/grapes/share/trec-car-allruns-2018/joinparas/joined.tree.entity.qrels"
//    val hierQrels = "/home/jsc57/data/benchmark/benchmarkY1/benchmarkY1-train/train.pages.cbor-hierarchical.entity.qrels"

//    doParse(qrelParams.entityQrelLoc, clickstreamLoc)
    doParse(hierQrels, clickstreamLoc)

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
    val treeQrels = "/home/jsc57/y2_tree_entity.qrels"
    val runner = ExperimentRunner(trec, "/home/jsc57/entruns")
    val clickQrels = "/home/jsc57/projects/run_comparison/entity_clicks.qrels"
    val enwikiQrels = "merged.qrels"

//    runner.evaluateQrel(hierQrels)
    println("========")
//    runner.evaluateQrel(qrels2)
//    println("========")
//    runner.evaluateQrel(spotQrels)
    println("========")
    runner.evaluateQrel(enwikiQrels, "tagme_annotated")
    runner.evaluateQrel(treeQrels, "tree_standard")
    runner.evaluateQrel(clickQrels, "clickstream")

}

fun main(args: Array<String>) {

    generateClickQrels()
//    runPageAnnotator()
    runComparison()

}