package com.mategka.dava.analyzer.wip;

import com.leakyabstractions.result.api.Result;
import com.leakyabstractions.result.core.Results;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import org.apache.commons.lang3.function.FailableCallable;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Supplier;

@Value
@Builder
public class RetryContext {

  @Builder.Default
  int maxRetries = 0;

  @Builder.Default
  Duration timeout = Duration.ofMinutes(1);

  @Builder.Default
  Duration delay = Duration.ZERO;

  public <S, F extends Exception> S applyToCallable(FailableCallable<S, F> callable)
    throws F, TimeoutException, InterruptedException {
    return applyToSupplier(() -> {
      try {
        return Results.success(callable.call());
      } catch (Exception e) {
        //noinspection unchecked
        return Results.failure((F) e);
      }
    });
  }

  @SuppressWarnings("RedundantThrows")
  @SneakyThrows
  public <S, F extends Exception> S applyToSupplier(Supplier<Result<? extends S, ? extends F>> callable)
    throws F, TimeoutException, InterruptedException {
    var executor = Executors.newSingleThreadExecutor();
    Exception lastException = null;
    try {
      for (int attempt = 0; attempt <= maxRetries; attempt++) {
        var future = CompletableFuture.supplyAsync(callable, executor);

        try {
          // Attempt to get the result within the timeout period
          var result = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
          if (result.hasSuccess()) {
            //noinspection OptionalGetWithoutIsPresent
            return result.getSuccess().get();
          } else {
            //noinspection OptionalGetWithoutIsPresent
            lastException = result.getFailure().get();
          }
        } catch (TimeoutException e) {
          // Cancel the task if it times out
          future.cancel(true);
          lastException = e;
        } catch (ExecutionException e) {
          // Unwrap the actual exception
          var cause = e.getCause();
          if (cause instanceof Exception ex) {
            lastException = ex;
          } else {
            throw cause;
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          lastException = e;
        }

        // If this wasn't the last attempt, wait for the delay period
        if (attempt < maxRetries) {
          try {
            Thread.sleep(delay.toMillis());
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
          }
        }
      }
    } finally {
      executor.shutdownNow();
      executor.close();
    }
    assert lastException != null;
    throw lastException;
  }

}
