package com.github.niqdev.openwebnet.rx;

import com.github.niqdev.openwebnet.domain.OpenFrame;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.niqdev.openwebnet.rx.OpenWebNetObservable.logDebug;
import static com.github.niqdev.openwebnet.rx.OpenWebNetObservable.rawCommand;

/**
 * @author niqdev
 */
public class OpenWebNetUtils {

    public static final String LOCALHOST = "localhost";
    public static final String LOCALHOST_ANDROID = "10.0.2.2";
    public static final String HOST = "192.168.1.41";
    public static final int PORT = 20000;

    // no instance
    private OpenWebNetUtils() {}

    /*
     * Using Schedulers.io() RxNewThreadScheduler/RxCachedThreadScheduler
     * often the job is interrupted and thread is killed, moreover debugging doesn't work.
     */
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static Observable<List<OpenFrame>> rawCommandAsync(String host, int port, String command) {
        return rawCommand(host, port, command)
            .subscribeOn(Schedulers.from(executor))
            .doOnError(throwable -> {
                logDebug("ERROR-doOnError " + throwable);
            })
            .finallyDo(() -> {
                executor.shutdown();
            });
    }
}