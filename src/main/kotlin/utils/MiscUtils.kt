package utils

import java.io.IOException
import java.util.concurrent.ThreadLocalRandom
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.runBlocking


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
