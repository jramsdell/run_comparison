package parsing

enum class ClickstreamQrelCombos(val entityQrelLoc: String, val paragraphQrelLoc: String) {
    TestHierarchical(
            paragraphQrelLoc = "/home/jsc57/data/benchmark/test/benchmarkY1/benchmarkY1-test/test.pages.cbor-hierarchical.qrels",
            entityQrelLoc = "/home/jsc57/data/benchmark/test/benchmarkY1/benchmarkY1-test/test.pages.cbor-hierarchical.entity.qrels"
            ),

    TestArticle(
            paragraphQrelLoc = "/home/jsc57/data/benchmark/test/benchmarkY1/benchmarkY1-test/test.pages.cbor-article.qrels",
            entityQrelLoc = "/home/jsc57/data/benchmark/test/benchmarkY1/benchmarkY1-test/test.pages.cbor-article.entity.qrels"
    ),

    TestTree(
            paragraphQrelLoc = "/home/jsc57/data/benchmark/tree/Y1-tree-qrels/benchmarkY1-test/tree_qrels.qrels",
            entityQrelLoc = "/home/jsc57/data/benchmark/tree/Y1-tree-qrels/benchmarkY1-test/entity_tree.qrels"
    )
}