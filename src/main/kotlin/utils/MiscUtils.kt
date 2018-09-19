package utils

import experiment.RankStat
import java.io.IOException
import java.util.concurrent.ThreadLocalRandom
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.runBlocking
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.store.FSDirectory
import java.nio.file.Paths
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.pow


fun<A> doIO(f: () -> A ): A? =
    try { f() } catch (e: IOException) { null }

inline fun<A> doIORequest(retryAttempts: Int = 5, retryDelay: Long = 5, requestFunction: () -> A): A? {
    (0 until retryAttempts).forEach {
        try {
            return requestFunction()
        } catch (e: IOException) { Thread.sleep(ThreadLocalRandom.current().nextLong(retryDelay)) }
    }
    return null
}

fun <A, B>Iterable<A>.pmap(f: suspend (A) -> B): List<B> = runBlocking {
    map { async(CommonPool) { f(it) } }.map { it.await() }
}

fun<A> Iterable<A>.countDuplicates(): Map<A, Int> =
        groupingBy { it }
            .eachCount()


fun Double.defaultWhenNotFinite(default: Double) = if (this.isFinite()) this else default

fun getIndexWriter(indexLocation: String, mode: IndexWriterConfig.OpenMode = IndexWriterConfig.OpenMode.CREATE_OR_APPEND,
                   analyzer: Analyzer = StandardAnalyzer()): IndexWriter {
    val indexPath = Paths.get(indexLocation)
    val indexDir = FSDirectory.open(indexPath)
    val conf = IndexWriterConfig(analyzer)
        .apply { openMode = mode }
    return IndexWriter(indexDir, conf)
}

fun <A>Iterable<A>.forEachParallelQ(qSize: Int = 1000, nThreads: Int = 30, f: (A) -> Unit) {
    val q = ArrayBlockingQueue<A>(qSize, true)
    val finished = AtomicBoolean(false)
    val taker = {
        while (true) {
            val next = q.poll(100, TimeUnit.MILLISECONDS)
            next?.run(f)
            if (next == null && finished.get())  break
        }
    }
    val threads = (0 until nThreads).map { Thread(taker).apply { start() } }
    forEach { element -> q.put(element)}
    finished.set(true)
    threads.forEach { thread -> thread.join() }
}

fun getSpearman(rank1: List<Int>, rank2: List<Int>): Double {
    val n = rank1.size.toDouble()
    val s1 = rank1.zip(rank2).sumByDouble { (x, y) -> (x - y).toDouble().pow(2.0) * 6.0  }
    return  1.0 - (s1 / (n * (n.pow(2.0) - 1.0) ))
}

fun createSpearmanMatrix(auto: List<RankStat>, manual: List<RankStat>) {
    val accessors: List<Pair<String, (RankStat) -> Int>> = listOf(
            "f1" to { rankStat -> rankStat.f1.rank },
            "map" to { rankStat -> rankStat.map.rank },
            "ndcg" to { rankStat -> rankStat.ndcg.rank },
            "p5" to { rankStat -> rankStat.p5.rank },
            "rprec" to { rankStat -> rankStat.rprec.rank }
    )
    val toBf = { i: String -> "\\textbf{" + i + "}" }

    println(accessors.map { toBf(it.first) }.joinToString(" & "))
    accessors.forEach { autoAccessor ->
        val row = accessors.map { manualAccessor ->
            val autoRanks = auto.map { autoAccessor.second(it) }
            val manualRanks = manual.map { manualAccessor.second(it) }
            val spearman = getSpearman(autoRanks, manualRanks)
            spearman.toString().take(5)
        }
        print(" & " + toBf(autoAccessor.first) + " & ")
        println(row.joinToString(" & ") + "\\\\\\hline")
    }


}

fun createSpearmanMatrix(comparisons: List<Pair<String, List<Int>>>) {
    val toBf = { i: String -> "\\textbf{" + i + "}" }

    println(comparisons.map { toBf(it.first) }.joinToString(" & "))
    comparisons.forEach { comp1 ->
        val row = comparisons.map { comp2 ->
            val spearman = getSpearman(comp1.second, comp2.second)
            spearman.toString().take(5)
        }
        print(toBf(comp1.first) + " & ")
        println(row.joinToString(" & ") + "\\\\\\hline")
    }


}
