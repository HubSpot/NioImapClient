package com.hubspot.imap.utils;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyCompletableFuture<T>
  extends CompletableFuture<T>
  implements FutureListener<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    NettyCompletableFuture.class
  );

  private NettyCompletableFuture(Future<T> future) {
    future.addListener(this);
  }

  @Override
  public void operationComplete(Future<T> tFuture) throws Exception {
    LOGGER.trace("Completing future from thread: {}", Thread.currentThread().getName());
    if (tFuture.isSuccess()) {
      complete(tFuture.getNow());
    } else if (tFuture.isCancelled()) {
      cancel(true);
    } else {
      completeExceptionally(tFuture.cause());
    }
  }

  public static <T> CompletableFuture<T> from(Future<T> future) {
    return new NettyCompletableFuture<>(future);
  }
}
