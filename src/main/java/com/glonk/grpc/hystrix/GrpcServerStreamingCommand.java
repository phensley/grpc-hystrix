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
public class GrpcServerStreamingCommand<Q, S> extends HystrixObservableCommand<S> {

  private final Channel channel;
  
  private final MethodDescriptor<Q, S> method;
  
  private final Q request;
  
  public GrpcServerStreamingCommand(
      Setter setter,
      Channel channel,
      MethodDescriptor<Q, S> method,
      Q request) {

    super(setter);
    this.channel = channel;
    this.method = method;
    this.request = request;
  }
  
  @Override
  protected Observable<S> construct() {
    return Observable.create(new Observable.OnSubscribe<S>() {
      @Override
      public void call(Subscriber<? super S> observer) {
        try {
          if (!observer.isUnsubscribed()) {
            Call<Q, S> call = channel.newCall(method);
            Iterator<S> iterator = Calls.blockingServerStreamingCall(call, request);
            while (iterator.hasNext()) {
              observer.onNext(iterator.next());
            }
            observer.onCompleted();
          }
        } catch (Exception e) {
          observer.onError(e);
        }
      }
    });
  }

}
