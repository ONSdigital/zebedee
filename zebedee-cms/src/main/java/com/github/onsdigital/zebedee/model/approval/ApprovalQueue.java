package com.github.onsdigital.zebedee.model.approval;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * In memory queue of tasks to approve collections.
 * <p>
 * The approval process may contain time / resource intensive tasks like generating timeseries. The tasks
 * are queued to ensure the user is not held up waiting for the process to finish, and to ensure only one approval
 * is done at a time.
 */
public class ApprovalQueue {

    /**
     * Single thread for processing the approvals.
     */
    private static ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * Add the given task instance to the queue.
     *
     * @param task
     * @return
     */
    public static Future<Boolean> add(ApproveTask task) {
        return executorService.submit(task);
    }
}
