import java.util.Scanner;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class Main {
    int[] array;
    int globalMin = Integer.MAX_VALUE;
    int globalMinIndex = -1;
    final Object lockObj = new Object();

    public static void main(String[] args) {
        new Main().execute();
    }

    void execute() {
        int size = 1_000_000_000;
        System.out.println("Генерація масиву (це займе кілька секунд)...");

        array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = (i % 100) + 1;
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("Введіть від'ємне число для перевірки: ");
        int negativeValue = scanner.nextInt();

        if (negativeValue >= 0) {
            System.out.println("Помилка: потрібно ввести від'ємне число.");
            return;
        }

        Random rnd = new Random();
        int injectIndex = rnd.nextInt(size);
        array[injectIndex] = negativeValue;
        System.out.println("Число " + negativeValue + " успішно вставлено на випадковий індекс " + injectIndex + ".\n");

        System.out.println("Починаємо однопотоковий пошук (Базовий час)...");
        runSingleThreadSearch();
        System.out.println("--------------------------------------------------");

        int[] threadCounts = { 2, 4, 6, 8 };
        for (int tc : threadCounts) {
            runSearch(tc);
        }

        System.out.println("\nТестування завершено.");
    }

    class SearchResult {
        int min;
        int index;
        SearchResult(int min, int index) {
            this.min = min;
            this.index = index;
        }
    }

    SearchResult findMinInRange(int start, int end) {
        int min = Integer.MAX_VALUE;
        int index = -1;

        for (int i = start; i < end; i++) {
            if (array[i] < min) {
                min = array[i];
                index = i;
            }
        }
        return new SearchResult(min, index);
    }

    void runSingleThreadSearch() {
        long startTime = System.currentTimeMillis();

        SearchResult result = findMinInRange(0, array.length);

        long endTime = System.currentTimeMillis();
        System.out.println("Потоків: 1 (Головний) | Мінімум: " + result.min + " (Індекс: " + result.index + ") | Час роботи: " + (endTime - startTime) + " мс");
    }

    void runSearch(int numThreads) {
        globalMin = Integer.MAX_VALUE;
        globalMinIndex = -1;

        int chunkSize = array.length / numThreads;
        CountDownLatch latch = new CountDownLatch(numThreads);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numThreads; i++) {
            final int start = i * chunkSize;
            final int end = (i == numThreads - 1) ? array.length : start + chunkSize;

            Thread t = new Thread(() -> {
                SearchResult localResult = findMinInRange(start, end);

                synchronized (lockObj) {
                    if (localResult.min < globalMin) {
                        globalMin = localResult.min;
                        globalMinIndex = localResult.index;
                    }
                }

                latch.countDown();
            });
            t.start();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Потоків: " + numThreads + " | Мінімум: " + globalMin + " (Індекс: " + globalMinIndex + ") | Час роботи: " + (endTime - startTime) + " мс");
    }
}