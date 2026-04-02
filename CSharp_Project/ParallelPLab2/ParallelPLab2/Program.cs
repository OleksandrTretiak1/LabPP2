using System;
using System.Diagnostics;
using System.Threading;

namespace ParallelPLab2
{
    class Program
    {
        int[] array = null!;
        int globalMin = int.MaxValue;
        int globalMinIndex = -1;
        readonly object lockObj = new object();
        Stopwatch globalTimer = new Stopwatch();

        static void Main(string[] args)
        {
            Console.OutputEncoding = System.Text.Encoding.UTF8;
            new Program().Execute();
        }

        void Execute()
        {
            int size = 1_000_000_000;
            Console.WriteLine("Генерація масиву (це займе кілька секунд)...");

            array = new int[size];
            for (int i = 0; i < size; i++)
            {
                array[i] = (i % 100) + 1;
            }

            Console.Write("Введіть від'ємне число для перевірки: ");
            if (!int.TryParse(Console.ReadLine(), out int negativeValue) || negativeValue >= 0)
            {
                Console.WriteLine("Помилка: потрібно ввести від'ємне число.");
                return;
            }

            Random rnd = new Random();
            int injectIndex = rnd.Next(0, size);
            array[injectIndex] = negativeValue;
            Console.WriteLine($"Число {negativeValue} успішно вставлено на випадковий індекс {injectIndex}.\n");

            Console.WriteLine("Починаємо однопотоковий пошук (Базовий час)...");
            RunSingleThreadSearch();
            Console.WriteLine("--------------------------------------------------");

            int[] threadCounts = { 2, 4, 6, 8 };
            foreach (int tc in threadCounts)
            {
                RunSearch(tc);
            }

            Console.WriteLine("\nТестування завершено.");
            Console.ReadLine();
        }

        (int minValue, int minIndex) FindMinInRange(int start, int end)
        {
            int min = int.MaxValue;
            int index = -1;

            for (int i = start; i < end; i++)
            {
                if (array[i] < min)
                {
                    min = array[i];
                    index = i;
                }
            }

            return (min, index);
        }

        void RunSingleThreadSearch()
        {
            globalTimer.Restart();

            var result = FindMinInRange(0, array.Length);

            globalTimer.Stop();
            Console.WriteLine($"Потоків: 1 (Головний) | Мінімум: {result.minValue} (Індекс: {result.minIndex}) | Час роботи: {globalTimer.ElapsedMilliseconds} мс");
        }

        void RunSearch(int numThreads)
        {
            globalMin = int.MaxValue;
            globalMinIndex = -1;

            Thread[] threads = new Thread[numThreads];
            int chunkSize = array.Length / numThreads;

            globalTimer.Restart();

            using (CountdownEvent countdown = new CountdownEvent(numThreads))
            {
                for (int i = 0; i < numThreads; i++)
                {
                    int threadIndex = i;
                    int start = threadIndex * chunkSize;
                    int end = (threadIndex == numThreads - 1) ? array.Length : start + chunkSize;

                    threads[threadIndex] = new Thread(() => {
                        var localResult = FindMinInRange(start, end);

                        lock (lockObj)
                        {
                            if (localResult.minValue < globalMin)
                            {
                                globalMin = localResult.minValue;
                                globalMinIndex = localResult.minIndex;
                            }
                        }

                        countdown.Signal();
                    });
                    threads[threadIndex].Start();
                }

                countdown.Wait();
            }

            globalTimer.Stop();
            Console.WriteLine($"Потоків: {numThreads} | Мінімум: {globalMin} (Індекс: {globalMinIndex}) | Час роботи: {globalTimer.ElapsedMilliseconds} мс");
        }
    }
}