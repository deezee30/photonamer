/*
 * Part of photonamer.
 *
 * @author deezee30 (2020).
 */

package me.deezee.photonamer;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public final class ServiceExecutor {

    // Will never be shut down as it will be reused
    private static final ListeningExecutorService executor = newAsyncExecutor();

    private ServiceExecutor() {}

    public static ListeningExecutorService getCachedExecutor() {
        return executor;
    }

    public static ListeningExecutorService newAsyncExecutor() {
        return MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));
    }

    public static ListeningExecutorService newAsyncExecutor(ThreadFactory tf) {
        return MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1, tf));
    }

    public static ListeningExecutorService newAsyncExecutor(String name) {
        return newAsyncExecutor(new ThreadFactoryBuilder().setNameFormat(name + "-%d").build());
    }
}