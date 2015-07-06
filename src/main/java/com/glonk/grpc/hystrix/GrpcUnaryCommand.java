package com.glonk.grpc.hystrix;

import com.netflix.hystrix.HystrixObservableCommand;

import io.grpc.Call;
import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.stub.Calls;
import rx.Observable;
import rx.Subscriber;


// TODO: this is just an initial example to gradually rework
public class GrpcUnaryCommand<Q, S> extends HystrixObservableCommand<S> {

  private final Channel channel;
  
  private final MethodDescriptor<Q, S> method;
  
  private final Q request;
  
  public GrpcUnaryCommand(
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
        Call<Q, S> call = null;
        try {
          if (!observer.isUnsubscribed()) {
            call = channel.newCall(method);
            S result = Calls.blockingUnaryCall(call, request);
            observer.onNext(result);
            observer.onCompleted();
          }
        } catch (Exception e) {
          observer.onError(e);
          if (call != null) {
            call.cancel();
          }
        }
      }        
    });
  }
  
}