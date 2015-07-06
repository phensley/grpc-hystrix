package com.glonk.grpc.hystrix;

import java.util.concurrent.ExecutionException;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import com.glonk.grpc.hystrix.model.DummyServiceGrpc;
import com.glonk.grpc.hystrix.model.UnaryRequest;
import com.glonk.grpc.hystrix.model.UnaryResponse;
import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;

import io.grpc.Call;
import io.grpc.Channel;
import io.grpc.Metadata;
import io.grpc.Metadata.Headers;
import io.grpc.Status;
import rx.Observable;

// TODO: quick hack to get basics working

public class GrpcUnaryCommandTest {

  @AfterTest
  private void afterTest() {
    Hystrix.reset();
  }
  
  @Test
  public void testConstruct() throws ExecutionException, InterruptedException {
    int expected = 12345;

    UnaryRequest request = UnaryRequest.newBuilder()
        .setCode(expected)
        .build();
    
    Channel channel = Mockito.mock(Channel.class);
    
    // TODO: generalize all this setup
    
    Call<UnaryRequest, UnaryResponse> call = new TestCall<UnaryRequest, UnaryResponse>() {
      @Override
      public UnaryResponse respond(UnaryRequest request) {
        return UnaryResponse.newBuilder()
            .setCode(request.getCode())
            .build();
      }
    };
    Mockito.when(channel.newCall(DummyServiceGrpc.CONFIG.unary)).thenReturn(call);

    // TODO: need to create Setter builder with defaults as part of model.
    
    HystrixCommandProperties.Setter propertiesSetter = HystrixCommandProperties.Setter()
        .withCircuitBreakerEnabled(true)
        .withExecutionTimeoutEnabled(true)
        .withExecutionTimeoutInMilliseconds(2000);
        
    HystrixObservableCommand.Setter setter = HystrixObservableCommand.Setter.withGroupKey(
        HystrixCommandGroupKey.Factory.asKey("foo"))
        .andCommandKey(HystrixCommandKey.Factory.asKey("bar"))
        .andCommandPropertiesDefaults(propertiesSetter);
    
    Observable<UnaryResponse> observable = (new GrpcUnaryCommand<UnaryRequest, UnaryResponse>(
        setter,
        channel,
        DummyServiceGrpc.CONFIG.unary,
        request
      )).toObservable();
    
    UnaryResponse response = observable.toBlocking().toFuture().get();
    Assert.assertEquals(response.getCode(), expected);
  }

  private static abstract class TestCall<Q, S> extends Call<Q, S> {

    protected Call.Listener<S> listener;

    protected int numMessages;
    
    protected Q payload;

    public abstract S respond(Q request);
    
    @Override
    public void start(Call.Listener<S> listener, Headers headers) {
      System.out.println("start()");
      this.listener = listener;
    }
    
    @Override
    public void request(int numMessages) {
      System.out.printf("request(%d)\n", numMessages);
      this.numMessages = numMessages;
    }
    
    @Override
    public void cancel() {
      System.out.println("cancel()");
    }
    
    @Override
    public void halfClose() {
      System.out.println("halfClose()");
      S response = respond(payload);
      for (int i = 0; i < numMessages; i++) {
        listener.onPayload(response);
      }
      listener.onClose(Status.OK, new Metadata.Trailers());
    }

    @Override
    public void sendPayload(Q payload) {
      System.out.printf("sendPayload(%s)\n", payload);
      this.payload = payload;
    }
    
    @Override
    public boolean isReady() {
      System.out.println("isReady()");
      return true;
    }
    
  }
  
  
}
