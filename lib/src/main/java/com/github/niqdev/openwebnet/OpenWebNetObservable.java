package com.github.niqdev.openwebnet;

import com.github.niqdev.openwebnet.message.OpenMessage;
import com.github.niqdev.openwebnet.message.ResponseOpenMessage;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import rx.Observable;
import rx.Statement;
import rx.functions.Func1;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.niqdev.openwebnet.OpenSession.newSession;
import static com.github.niqdev.openwebnet.OpenWebNet.Channel;
import static com.github.niqdev.openwebnet.OpenWebNet.OpenGateway;
import static com.github.niqdev.openwebnet.message.OpenMessage.ACK;
import static com.github.niqdev.openwebnet.message.OpenMessage.FRAME_END;

/**
 * @author niqdev
 */
public class OpenWebNetObservable {

    private static void log(String message) {
        System.out.println(String.format("[%s] - %s", Thread.currentThread().getName(), message));
    }

    // no instance
    private OpenWebNetObservable() {}

    static Observable<OpenContext> connect(OpenGateway gateway) {
        return Observable.defer(() -> {
            try {
                OpenContext context = OpenContext.setup(gateway);
                context.connect();
                log("connected!");
                return Observable.just(context);
            } catch (IOException e) {
                return Observable.error(e);
            }
        })
        .finallyDo(() -> {
            // TODO handle unsubscribe/close socket
            //context.disconnect();
        })
        .timeout(5, TimeUnit.SECONDS)
        .doOnError(throwable -> {
            System.out.println("ERROR connect: " + throwable);
        });
    }

    static Func1<OpenContext, Observable<OpenContext>> doHandshake(Channel channel) {
        return context -> Observable.just(context)
            .flatMap(read())
            .flatMap(expectedAck(context))
            .flatMap(write(channel.value()))
            .flatMap(read())
            .flatMap(expectedAck(context));
    }

    static Func1<OpenContext, Observable<List<OpenSession>>> doRequests(List<OpenMessage> requests) {
        return context -> Observable.just(requests)
            .flatMapIterable(messages -> messages)
            .flatMap(request -> Observable.just(context).flatMap(doRequest(request)))
            .reduce(new ArrayList<>(), (openSessions, session) -> {
                openSessions.add(session);
                return openSessions;
            });
    }

    static Func1<OpenContext, Observable<OpenSession>> doRequest(OpenMessage request) {
        return context -> Observable.just(context)
            .flatMap(write(request.getValue()))
            .flatMap(read())
            .flatMap(parseMessages())
            .map(response -> newSession(request).addAllResponse(response));
    }

    static Func1<OpenContext, Observable<String>> read() {
        return context -> {
            try {
                ByteBuffer buffer = context.getEmptyBuffer();
                Integer count = context.getClient().read(buffer);
                String message = new String(buffer.array()).trim();
                log(String.format("read: %d|%s", count, message));
                return Observable.just(message);
            } catch (IOException e) {
                return Observable.error(e);
            }
        };
    }

    static Func1<String, Observable<OpenContext>> expectedAck(OpenContext context) {
        return s -> Statement.ifThen(
            () -> s.equals(ACK),
            Observable.just(context),
            Observable.error(new IllegalStateException("expected ACK")));
    }

    static Func1<OpenContext, Observable<OpenContext>> write(String value) {
        return context -> {
            try {
                byte[] message = new String(value).getBytes();
                ByteBuffer buffer = ByteBuffer.wrap(message);
                Integer count = context.getClient().write(buffer);
                log(String.format("write: %d|%s", count, value));
                return Observable.just(context);
            } catch (IOException e) {
                return Observable.error(e);
            }
        };
    }

    static Func1<String, Observable<List<OpenMessage>>> parseMessages() {
        return messages -> {
            ImmutableList<OpenMessage> response =
                FluentIterable
                    .from(Splitter.on(FRAME_END)
                        .trimResults()
                        .omitEmptyStrings()
                        .split(messages))
                    .transform(value -> value.concat(FRAME_END))
                    .transform(message -> (OpenMessage) new ResponseOpenMessage(message))
                    .toList();
            return Observable.just(response);
        };
    }

}
