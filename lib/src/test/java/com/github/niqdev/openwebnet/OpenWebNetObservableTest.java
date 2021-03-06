package com.github.niqdev.openwebnet;

import com.github.niqdev.openwebnet.message.OpenMessage;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * RUN
 * gradle :lib:test --debug
 */
public class OpenWebNetObservableTest {

    OpenContext mockedContext;
    SocketChannel mockedSocketChannel;

    @Before
    public void initialize() {
        mockedContext = mock(OpenContext.class);
        mockedSocketChannel = mock(SocketChannel.class);

        when(mockedContext.getClient()).thenReturn(mockedSocketChannel);
    }

    @Test
    public void testRead() throws IOException {
        String value = "myFrameValue";
        final ByteBuffer buffer = ByteBuffer.allocate(1024);

        when(mockedContext.getEmptyBuffer()).thenReturn(buffer);
        when(mockedSocketChannel.read(buffer)).then(invocation -> {
            buffer.put(value.getBytes());
            return value.length();
        });

        TestSubscriber<String> tester = new TestSubscriber<>();

        Observable.just(mockedContext)
            .flatMap(OpenWebNetObservable.read())
            .subscribe(tester);

        verify(mockedContext, times(1)).getEmptyBuffer();
        verify(mockedSocketChannel, times(1)).read(buffer);

        tester.assertValue(value);
        tester.assertCompleted();
    }

    @Test
    public void testExpectedAckValid() {
        TestSubscriber<OpenContext> tester = new TestSubscriber<>();

        Observable.just(OpenMessage.ACK)
            .flatMap(OpenWebNetObservable.expectedAck(mockedContext))
            .subscribe(tester);

        tester.assertValue(mockedContext);
        tester.assertCompleted();
        tester.assertNoErrors();
    }

    @Test
    public void testExpectedAckInvalid() {
        TestSubscriber<OpenContext> tester = new TestSubscriber<>();

        Observable.just("INVALID-ACK")
            .flatMap(OpenWebNetObservable.expectedAck(mockedContext))
            .subscribe(tester);

        tester.assertNotCompleted();
        tester.assertError(IllegalStateException.class);
    }

    @Test
    public void testWrite() throws IOException {
        String value = "myFrameValue";
        ByteBuffer buffer = ByteBuffer.wrap(value.getBytes());

        when(mockedSocketChannel.write(buffer)).then(invocation -> value.length());

        TestSubscriber<OpenContext> tester = new TestSubscriber<>();

        Observable.just(mockedContext)
            .flatMap(OpenWebNetObservable.write(value))
            .subscribe(tester);

        verify(mockedSocketChannel, times(1)).write(buffer);

        tester.assertValue(mockedContext);
        tester.assertCompleted();
    }

    @Test
    public void testParseMessages() {
        Observable.just("*1*0*21##*#*1##FRAME##")
            .flatMap(OpenWebNetObservable.parseMessages())
            .subscribe(messages -> {
                assertEquals("invalid frame", messages.get(0).getValue(), "*1*0*21##");
                assertEquals("invalid frame", messages.get(1).getValue(), "*#*1##");
                assertEquals("invalid frame", messages.get(2).getValue(), "FRAME##");
            });
    }
}
