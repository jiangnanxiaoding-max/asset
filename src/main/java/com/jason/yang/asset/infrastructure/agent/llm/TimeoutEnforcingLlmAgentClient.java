package com.jason.yang.asset.infrastructure.agent.llm;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/** Hard-timeout decorator backed by a bounded daemon-thread bulkhead. */
public class TimeoutEnforcingLlmAgentClient implements LlmAgentClient {
    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
            4, 4, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<Runnable>(32), new DaemonThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy());
    private final LlmAgentClient delegate;

    public TimeoutEnforcingLlmAgentClient(LlmAgentClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public Response next(final Request request, final Duration timeout) {
        final Future<Response> future;
        try {
            future = EXECUTOR.submit(() -> delegate.next(request, timeout));
        } catch (RuntimeException exception) {
            throw new ClientException("LLM bulkhead is full", true, exception);
        }
        try {
            return future.get(timeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw new ClientException("LLM hard timeout", true, exception);
        } catch (InterruptedException exception) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new ClientException("LLM call interrupted", false, exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof ClientException) throw (ClientException) cause;
            throw new ClientException("LLM client failed", false, cause);
        }
    }

    @Override
    public String provider() { return delegate.provider(); }

    @Override
    public String model() { return delegate.model(); }

    private static final class DaemonThreadFactory implements ThreadFactory {
        private final AtomicInteger sequence = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "llm-hard-timeout-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
