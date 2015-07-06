package com.glonk.grpc.hystrix;

import java.util.Iterator;

import com.netflix.hystrix.HystrixObservableCommand;

import io.grpc.Call;
import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.stub.Calls;
import rx.Observable;
import rx.Subscriber;


//TODO: this is just an initial example to gradually rework
public class GrpcClientStreamingCommand<Q, S> extends HystrixObservableCommand<S> {

  private final Channel channel;
  
  private final MethodDescriptor<Q, S> method;
  
  private final Iterator<Q> stream;
  
  public GrpcClientStreamingCommand(
      Setter setter,
      Channel channel, 
      MethodDescriptor<Q, S> method, 
      Iterator<Q> stream) {

    super(setter);
    this.channel = channel;
    this.method = method;
    this.stream = stream;
  }
  
  @Override
  protected Observable<S> construct() {
    return Observable.create(new Observable.OnSubscribe<S>() {
      @Override
      public void call(Subscriber<? super S> observer) {
        try {
          if (!observer.isUnsubscribed()) {
            Call<Q, S> call = channel.newCall(method);
            S result = Calls.blockingClientStreamingCall(call, stream);
            observer.onNext(result);
            observer.onCompleted();
          }
        } catch (Exception e) {
          observer.onError(e);
        }
      }
    });
  }
  
}
