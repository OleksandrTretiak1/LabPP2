import java.util.Scanner
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis
import kotlin.random.Random

fun main() {
    val lab = ParallelSearcher()
    lab.execute()
}

class ParallelSearcher {
    private lateinit var array: IntArray
    private var globalMin = Int.MAX_VALUE
    private var globalMinIndex = -1
    private val lockObj = Any()

    fun execute() {
        val size = 1_000_000_000
        println("Генерація масиву (це займе кілька секунд)...")

        array = IntArray(size) { (it % 100) + 1 }

        val scanner = Scanner(System.`in`)
        print("Введіть від'ємне число для перевірки: ")
        val negativeValue = scanner.nextInt()

        if (negativeValue >= 0) {
            println("Помилка: потрібно ввести від'ємне число.")
            return
        }

        val injectIndex = Random.nextInt(0, size)
        array[injectIndex] = negativeValue
        println("Число $negativeValue успішно вставлено на випадковий індекс $injectIndex.\n")

        println("Починаємо однопотоковий пошук (Базовий час)...")
        runSingleThreadSearch()
        println("--------------------------------------------------")

        val threadCounts = intArrayOf(2, 4, 6, 8)
        for (tc in threadCounts) {
            runSearch(tc)
        }

        println("\nТестування завершено.")
    }

    private fun findMinInRange(start: Int, end: Int): Pair<Int, Int> {
        var min = Int.MAX_VALUE
        var index = -1

        for (i in start until end) {
            if (array[i] < min) {
                min = array[i]
                index = i
            }
        }

        return Pair(min, index)
    }

    private fun runSingleThreadSearch() {
        var result: Pair<Int, Int>? = null
        val timeMillis = measureTimeMillis {
            result = findMinInRange(0, array.size)
        }
        println("Потоків: 1 (Головний) | Мінімум: ${result?.first} (Індекс: ${result?.second}) | Час роботи: $timeMillis мс")
    }

    private fun runSearch(numThreads: Int) {
        globalMin = Int.MAX_VALUE
        globalMinIndex = -1

        val chunkSize = array.size / numThreads
        val latch = CountDownLatch(numThreads)

        val timeMillis = measureTimeMillis {
            for (i in 0 until numThreads) {
                val start = i * chunkSize
                val end = if (i == numThreads - 1) array.size else start + chunkSize

                thread(start = true) {
                    val localResult = findMinInRange(start, end)

                    synchronized(lockObj) {
                        if (localResult.first < globalMin) {
                            globalMin = localResult.first
                            globalMinIndex = localResult.second
                        }
                    }

                    latch.countDown()
                }
            }

            latch.await()
        }

        println("Потоків: $numThreads | Мінімум: $globalMin (Індекс: $globalMinIndex) | Час роботи: $timeMillis мс")
    }
}