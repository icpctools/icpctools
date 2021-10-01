package org.icpc.tools.contest.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static java.time.temporal.ChronoUnit.SECONDS;

/**
    Concurrently stream lots of CDS streams.
 */
public class StreamTest {
    static final String CDS = "172.29.1.207";
    static final int CONCURRENT_STREAMS = 300;

    static class StreamRate {
        long total;
        long fail;
        public synchronized void add(int n) {
            total += n;
        }
        public synchronized void add(byte[] b) {
            add(b.length);
            //System.out.println(new String(b));
        }
        public synchronized void fail() {
            fail++;
        }
        public synchronized long currentTotal() {
            return total;
        }
        public synchronized long currentFail() {
            return fail;
        }

        long lastTotal, lastFail;
        public synchronized String report(boolean shortForm) {
            long inc = total - lastTotal;
            long incFail = fail - lastFail;
            lastTotal = total;
            lastFail = fail;
            if (shortForm) {
                String s = String.format("%dk", inc / 1024);
                if (incFail > 0) {
                    s += " " + incFail + "F";
                }
                return s;
            } else {
                return String.format("%d %.3fM %dF", inc, inc / 1048576., incFail);
            }
        }
    }

    static class KeepRequesting implements Runnable {
        HttpClient client;
        String id;
        StreamRate totalRate, rate;

        public KeepRequesting(HttpClient client, String id, StreamRate totalRate) {
            this.client = client;
            this.id = id;
            this.totalRate = totalRate;
            rate = new StreamRate();
        }

        @Override
        public void run() {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(new URI("http://" + CDS + "/stream/" + id))
                        .timeout(Duration.of(10, SECONDS))
                        .GET()
                        .build();

                Consumer<Optional<byte[]>> consumer = new Consumer<Optional<byte[]>>() {
                    @Override
                    public void accept(Optional<byte[]> bytes) {
                        bytes.ifPresentOrElse(b -> {
                            rate.add(b);
                            totalRate.add(b);
                        }, () -> {
                            rate.fail();
                            totalRate.fail();
                        });
                    }
                };

                Callable<CompletableFuture<HttpResponse<Void>>> sendReq = () -> {
                    CompletableFuture<HttpResponse<Void>> future = client.sendAsync(req, HttpResponse.BodyHandlers.ofByteArrayConsumer(consumer));
                    return future;
                };

                sendReq.call().whenComplete((HttpResponse<Void> r, Throwable t) -> {
                    //System.out.println("re-run!");
                    if (t != null) {
                        System.out.println(t);
                        totalRate.fail();
                        rate.fail();
                    }
                    this.run();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String[] teams = args;
        if (teams.length == 0) {
            teams = new String[]{"11"};
        }

        ExecutorService executor = Executors.newWorkStealingPool();

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .executor(executor)
                .build();

        final StreamRate totalRate = new StreamRate();

        List<KeepRequesting> reqs = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_STREAMS; ++i) {
            // Avoid too many concurrent streams per http client! Seems to happen around 200.
            // https://stackoverflow.com/questions/54917885/java-11-httpclient-http2-too-many-streams-error
            if (i % 100 == 0) {
                client = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.ALWAYS)
                        .executor(executor)
                        .build();
            }
            String id = teams[i % teams.length];
            KeepRequesting req = new KeepRequesting(client, id, totalRate);
            reqs.add(req);
            req.run();
            Thread.sleep(10);
        }

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            int i;
            @Override
            public void run() {
                i++;
                System.out.println("" + i + ": " + totalRate.report(false));

                int j = 0;
                for (KeepRequesting req : reqs) {
                    if (j % 20 == 0) {
                        if (j > 0) {
                            System.out.println();
                        }
                        System.out.print("" + i + " " + j + ":");
                    }
                    System.out.print(" " + req.rate.report(true));
                    j++;
                }
                System.out.println();
                System.out.println();
            }
        }, 1000, 1000);
    }
}
