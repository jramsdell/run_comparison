package utils

import java.io.IOException
import java.util.concurrent.ThreadLocalRandom
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.runBlocking
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.store.FSDirectory
import java.nio.file.Paths
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean


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

fun getIndexWriter(indexLocation: String, mode: IndexWriterConfig.OpenMode = IndexWriterConfig.OpenMode.CREATE_OR_APPEND): IndexWriter {
    val indexPath = Paths.get(indexLocation)
    val indexDir = FSDirectory.open(indexPath)
    val conf = IndexWriterConfig(StandardAnalyzer())
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
