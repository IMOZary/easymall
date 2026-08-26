import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CheckoutLoadTest {
    private static final Pattern ID_PATTERN = Pattern.compile("\\\"id\\\"\\s*:\\s*(\\d+)");
    private static final Pattern STOCK_PATTERN = Pattern.compile("\\\"stock\\\"\\s*:\\s*(\\d+)");

    public static void main(String[] args) throws Exception {
        String baseUrl = args.length > 0 ? args[0] : "http://127.0.0.1:8080";
        int users = args.length > 1 ? Integer.parseInt(args[1]) : 100;
        int concurrency = args.length > 2 ? Integer.parseInt(args[2]) : 20;
        if (users < 1 || concurrency < 1) throw new IllegalArgumentException("users and concurrency must be positive");

        Session admin = new Session(baseUrl);
        admin.requireSuccess("/api/auth/login", "{\"username\":\"admin\",\"password\":\"admin123\"}");
        long categoryId = firstId(admin.get("/api/categories").body());
        String productJson = "{\"categoryId\":" + categoryId
                + ",\"name\":\"并发下单压测商品\",\"subtitle\":\"仅用于本地可复现测试\",\"price\":99.00"
                + ",\"stock\":" + users + ",\"icon\":\"TEST\",\"theme\":\"mint\""
                + ",\"description\":\"CheckoutLoadTest 自动创建\",\"status\":\"ON_SALE\"}";
        Response productResponse = admin.requireSuccess("/api/admin/products", productJson);
        long productId = firstId(productResponse.body());

        String runId = Long.toString(System.currentTimeMillis(), 36);
        List<Session> sessions = new ArrayList<>(users);
        for (int index = 0; index < users; index++) {
            Session session = new Session(baseUrl);
            String username = String.format(Locale.ROOT, "load%s%03d", runId, index);
            session.requireSuccess("/api/auth/register", "{\"username\":\"" + username
                    + "\",\"password\":\"load123\",\"nickname\":\"压测用户\"}");
            session.requireSuccess("/api/cart", "{\"productId\":" + productId + ",\"quantity\":1}");
            sessions.add(session);
        }

        ExecutorService pool = Executors.newFixedThreadPool(Math.min(users, concurrency));
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failure = new AtomicInteger();
        List<Long> latencyMillis = Collections.synchronizedList(new ArrayList<>());
        List<String> errors = Collections.synchronizedList(new ArrayList<>());

        long suiteStart = System.nanoTime();
        for (int index = 0; index < users; index++) {
            final int userIndex = index;
            pool.submit(() -> {
                try {
                    start.await();
                    String body = "{\"receiver\":\"张同学\",\"phone\":\"13800138000\","
                            + "\"address\":\"上海市测试路1号\",\"remark\":\"MySQL并发验证\","
                            + "\"idempotencyKey\":\"load-" + runId + "-" + userIndex + "-" + UUID.randomUUID() + "\"}";
                    long requestStart = System.nanoTime();
                    Response response = sessions.get(userIndex).post("/api/orders", body);
                    latencyMillis.add(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - requestStart));
                    if (response.statusCode() == 200 && response.body().contains("\"success\":true")) {
                        success.incrementAndGet();
                    } else {
                        failure.incrementAndGet();
                        addError(errors, response.statusCode() + ": " + response.body());
                    }
                } catch (Exception exception) {
                    failure.incrementAndGet();
                    addError(errors, exception.toString());
                }
            });
        }
        start.countDown();
        pool.shutdown();
        if (!pool.awaitTermination(5, TimeUnit.MINUTES)) throw new IllegalStateException("load test timed out");
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - suiteStart);

        List<Long> sorted = new ArrayList<>(latencyMillis);
        Collections.sort(sorted);
        double throughput = success.get() / Math.max(elapsedMillis / 1000.0, 0.001);
        double average = sorted.stream().mapToLong(Long::longValue).average().orElse(0);
        int remainingStock = firstInt(STOCK_PATTERN, admin.get("/api/products/" + productId).body());

        System.out.println("EasyMall checkout load test");
        System.out.println("baseUrl=" + baseUrl);
        System.out.println("users=" + users);
        System.out.println("concurrency=" + concurrency);
        System.out.println("success=" + success.get());
        System.out.println("failure=" + failure.get());
        System.out.println("elapsedMs=" + elapsedMillis);
        System.out.printf(Locale.ROOT, "throughputRps=%.2f%n", throughput);
        System.out.printf(Locale.ROOT, "averageMs=%.2f%n", average);
        System.out.println("p50Ms=" + percentile(sorted, 0.50));
        System.out.println("p95Ms=" + percentile(sorted, 0.95));
        System.out.println("p99Ms=" + percentile(sorted, 0.99));
        System.out.println("remainingStock=" + remainingStock);
        if (!errors.isEmpty()) errors.forEach(error -> System.out.println("error=" + error));
        if (failure.get() > 0 || remainingStock != 0) System.exit(1);
    }

    private static long firstId(String json) {
        return firstInt(ID_PATTERN, json);
    }

    private static int firstInt(Pattern pattern, String json) {
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) throw new IllegalStateException("Expected numeric field in response: " + json);
        return Integer.parseInt(matcher.group(1));
    }

    private static long percentile(List<Long> sorted, double percentile) {
        if (sorted.isEmpty()) return 0;
        int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    private static void addError(List<String> errors, String error) {
        synchronized (errors) {
            if (errors.size() < 5) errors.add(error);
        }
    }

    private record Response(int statusCode, String body) {}

    private static final class Session {
        private final String baseUrl;
        private final HttpClient client;

        private Session(String baseUrl) {
            this.baseUrl = baseUrl;
            this.client = HttpClient.newBuilder()
                    .cookieHandler(new CookieManager())
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
        }

        private Response get(String path) throws Exception {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(30)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.body());
        }

        private Response post(String path, String json) throws Exception {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.body());
        }

        private Response requireSuccess(String path, String json) throws Exception {
            Response response = post(path, json);
            if (response.statusCode() != 200 || !response.body().contains("\"success\":true")) {
                throw new IllegalStateException(path + " failed: " + response.statusCode() + " " + response.body());
            }
            return response;
        }
    }
}
