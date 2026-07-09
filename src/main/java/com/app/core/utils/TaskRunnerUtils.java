package com.app.core.utils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class TaskRunnerUtils {

    /**
     * Devide work into chunks and execute them in parallel in batches 
     *
     * @param totalCount Total number of elements to be processed.
     * @param chunkSize Number of elements in each chunk.
     * @param ccuRate Number of chunks processed simultaneously in each batch.
     * @param executor ThreadPoolTaskExecutor to run task. 
     * @param chunkProcessor Logic function.
     * @throws InterruptedException thread wait CountDownLatch.
     */
    public static void runInParallelBatches(
        int totalCount,
        int chunkSize,
        int ccuRate,
        ThreadPoolTaskExecutor executor,
        BiConsumer<Integer, Integer> chunkProcesser
    ) throws InterruptedException {
        if (totalCount <= 0 || chunkSize <= 0 || ccuRate <= 0) {
            return;
        }

        int chunkLoop = (totalCount + chunkSize - 1) / chunkSize;

        int threadLoop = (chunkLoop + ccuRate - 1) / ccuRate;

        AtomicInteger errroCount = new AtomicInteger(0);

        for (int i = 0; i < threadLoop; i++) {
            int startIndexChunk = i * ccuRate;
            int actualTasksInBatch = 0;

            for (int j = 0; j < ccuRate; j++) {
                if (startIndexChunk + j < chunkLoop) {
                    actualTasksInBatch++;
                }
            }

            if (actualTasksInBatch == 0) break;

            CountDownLatch countDownLatch = new CountDownLatch(actualTasksInBatch);

            for (int j = 0; j < ccuRate; j++) {
                final int chunkIndex = startIndexChunk + j;

                if (chunkIndex >= chunkLoop) {
                    break;
                }

                executor.submit(() -> {
                    try {
                        int startRows = chunkIndex * chunkSize + 1;
                        int endRows = Math.min((chunkIndex + 1) * chunkSize, totalCount);

                        chunkProcesser.accept(startRows, endRows);
                    } catch (Exception e) {
                        errroCount.incrementAndGet();
                    } finally {
                        countDownLatch.countDown();
                    }
                });
            }
            countDownLatch.await();
        }
    }
}
