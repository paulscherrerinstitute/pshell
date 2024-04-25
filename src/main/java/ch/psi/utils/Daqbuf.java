package ch.psi.utils;

import ch.psi.utils.Threading.VisibleCompletableFuture;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Access to Daqbuf service.
 */
public class Daqbuf implements ChannelQueryAPI {

    final String url;
    final String backend;
    final Client client;

    public static final String FIELD_BACKEND = "backend";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_SERIES_ID = "seriesId";
    public static final String FIELD_SOURCE = "source";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_SHAPE = "shape";
    public static final String FIELD_UNIT = "unit";
    public static final String FIELD_DESC = "description";

    public static final String[] SEARCH_FIELDS = new String[]{FIELD_BACKEND, FIELD_NAME,
        FIELD_SERIES_ID, FIELD_SOURCE, FIELD_TYPE, FIELD_SHAPE, FIELD_UNIT, FIELD_DESC
    };
    static final String PRINT_SEARCH_FORMAT = "%-16s %-48s %-12s %-36s %-8s %-8s %-12s %-12s\n";

    public static final String FIELD_TIMESTAMP = "timestamp";
    public static final String FIELD_ID = "id";
    public static final String FIELD_VALUE = "value";
    public static final String[] QUERY_FIELDS = new String[]{FIELD_NAME, FIELD_TIMESTAMP, FIELD_ID, FIELD_VALUE};
    static final String PRINT_QUERY_FORMAT = "%-32s %-24s %-12s %-40s\n";

    public static final String FIELD_AVERAGE = "average";
    public static final String FIELD_START = "start";
    public static final String FIELD_END = "end";
    public static final String FIELD_MIN = "min";
    public static final String FIELD_MAX = "max";
    public static final String FIELD_COUNT = "count";
    public static final String[] BINNED_QUERY_FIELDS = new String[]{FIELD_NAME, FIELD_START, FIELD_END, FIELD_AVERAGE, FIELD_MIN, FIELD_MAX, FIELD_COUNT};
    static final String PRINT_BINNED_QUERY_FORMAT = "%-32s %-24s %-24s %-24s %-24s %-24s %-8s\n";

    static final String COLOR_CYAN = "\033[96m";
    static final String COLOR_RESET = "\033[0m";

    public static final String BACKEND_SEPARATOR = "@";

    static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    static final Long PULSE_ID_START_TIME = 1504524711650L;
    static final double PULSE_ID_INTERVAL = 10;

    public static String getDefaultUrl() {
        return System.getenv().getOrDefault("DAQBUF_DEFAULT_URL", "https://data-api.psi.ch/api/4");
    }

    public static String getDefaultBackend() {
        return System.getenv().getOrDefault("DAQBUF_DEFAULT_BACKEND", "sf-databuffer");
    }

    ObjectMapper mapper;

    public Daqbuf() {
        this(getDefaultUrl(), getDefaultBackend());
        mapper = new ObjectMapper(new CBORFactory());
    }

    public Daqbuf(String url, String backend) {
        if (!url.contains("://")) {
            url = "http://" + url;
        }
        this.url = url;
        this.backend = backend;
        ClientConfig config = new ClientConfig().register(JacksonFeature.class);
        client = ClientBuilder.newClient(config);
    }

    public String getUrl() {
        return url;
    }

    public String getBackend() {
        return backend;
    }

    public List<Map<String, Object>> search(String regex) throws IOException {
        return search(regex, null);
    }

    public List<Map<String, Object>> search(String regex, Boolean caseInsensitive) throws IOException {
        return search(regex, caseInsensitive, null);
    }

    public List<Map<String, Object>> search(String regex, Boolean caseInsensitive, Integer limit) throws IOException {
        return search(backend, regex, caseInsensitive, limit);
    }

    public List<Map<String, Object>> search(String backend, String regex, Boolean caseInsensitive, Integer limit) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("nameRegex", regex);
        if (caseInsensitive != null) {
            params.put("icase", caseInsensitive);
        }
        if (backend != null) {
            params.put("backend", backend);
        }
        WebTarget resource = client.target(url + "/search/channel");
        for (String paramName : params.keySet()) {
            resource = resource.queryParam(paramName, params.get(paramName));
        }
        Response r = resource.request().accept(MediaType.APPLICATION_JSON).get();
        String json = r.readEntity(String.class);
        Map<String, Object> ret = (Map) EncoderJson.decode(json, Map.class);
        List<Map<String, Object>> list = (List<Map<String, Object>>) ret.getOrDefault("channels", null);
        return list;
    }

    public CompletableFuture startSearch(String regex) {
        return (CompletableFuture) Threading.getPrivateThreadFuture(() -> search(regex));
    }

    public CompletableFuture startSearch(String regex, Boolean caseInsensitive) throws IOException {
        return (CompletableFuture) Threading.getPrivateThreadFuture(() -> search(regex, caseInsensitive));
    }

    public CompletableFuture startSearch(String regex, Boolean caseInsensitive, Integer limit) throws IOException {
        return (CompletableFuture) Threading.getPrivateThreadFuture(() -> search(regex, caseInsensitive, limit));
    }

    public CompletableFuture startSearch(String backend, String regex, Boolean caseInsensitive, Integer limit) throws IOException {
        return (CompletableFuture) Threading.getPrivateThreadFuture(() -> search(backend, regex, caseInsensitive, limit));
    }

    @Override
    public List<String> queryChannels(String text, String backend, int limit) throws IOException {
        List<Map<String, Object>> ret = search(backend, text, null, limit);
        return ret.stream()
                .map(map -> map.get("name").toString())
                .collect(Collectors.toList());
    }

    public static List<Map<String, Object>> sort(List<Map<String, Object>> list, String fieldName, boolean ascending) {
        Comparator<Map<String, Object>> comparator = (map1, map2) -> {
            Comparable<Object> value1 = (Comparable<Object>) map1.get(fieldName);
            Comparable<Object> value2 = (Comparable<Object>) map2.get(fieldName);
            int result = value1.compareTo(value2);
            return ascending ? result : -result; // Flip result if descending
        };

        return list.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    public static void printSearch(List<Map<String, Object>> list) {
        System.out.printf("%s%s%s", COLOR_CYAN, String.format(PRINT_SEARCH_FORMAT, (Object[]) SEARCH_FIELDS), COLOR_RESET);
        list = sort(list, FIELD_NAME, true);
        list = sort(list, FIELD_BACKEND, true);
        for (Map<String, Object> channel : list) {
            Object[] cols = new Object[SEARCH_FIELDS.length];
            for (int i = 0; i < SEARCH_FIELDS.length; i++) {
                cols[i] = channel.get(SEARCH_FIELDS[i]);
            }
            System.out.printf("%s", String.format(PRINT_SEARCH_FORMAT, cols));
        }
    }

    byte[] readStream(InputStream inputStream, int dataSize) throws IOException, InterruptedException {
        long last = System.currentTimeMillis();
        int timeout = 10000;

        byte[] data = new byte[dataSize];
        int bytesRead = 0;
        while (bytesRead < dataSize) {
            int count = inputStream.read(data, bytesRead, dataSize - bytesRead);
            if (count == -1) {
                throw new IOException("Unexpected end of stream.");
            } else if (count == 0) {
                if ((System.currentTimeMillis() - last) > timeout) {
                    throw new IOException("Timeout receiving from stream");
                }
                Thread.sleep(1);
            } else {
                bytesRead += count;
                last = System.currentTimeMillis();
            }
        }
        return data;
    }

    public interface QueryListener {

        default void onStarted(Query query) {
        }

        default void onFinished(Query query, Exception ex) {
        }

        default void onMessage(Query query, List values, List<Long> ids, List<Long> timestamps) {
        }
    }

    public interface QueryRecordListener extends QueryListener {

        default void onRecord(Query query, Object value, Long id, Long timestamp) {
        }
    }

    public interface QueryBinnedListener extends QueryListener {

        default void onMessage(Query query, List<Double> average, List<Double> min, List<Double> max, List<Integer> count, List<Long> start, List<Long> end) {
        }
    }

    public interface QueryBinnedRecordListener extends QueryBinnedListener {

        default void onRecord(Query query, Double average, Double min, Double max, Integer count, Long start, Long end) {
        }
    }

    public static class Query {

        public final String channel;
        public final String backend;
        public final String start;
        public final String end;
        public final Integer bins;

        Query(String channel, String backend, String start, String end, Integer bins) {
            this.channel = channel;
            this.backend = backend;
            this.start = convertToUTC(start);
            this.end = convertToUTC(end);
            this.bins = bins;
        }

        WebTarget setResourceParams(WebTarget resource) {
            resource = resource.queryParam("channelName", channel);
            resource = resource.queryParam("backend", backend);
            resource = resource.queryParam("begDate", start);
            resource = resource.queryParam("endDate", end);
            if (bins != null) {
                resource = resource.queryParam("binCount", bins);
            }
            return resource;

        }
    }

    void query(String channel, String start, String end, QueryListener listener) throws IOException, InterruptedException {
        query(channel, start, end, listener, null);
    }

    void query(String channel, String start, String end, QueryListener listener, Integer bins) throws IOException, InterruptedException {
        boolean cbor = bins == null;
        String backend = this.backend;
        if (channel.contains(BACKEND_SEPARATOR)) {
            String[] tokens = channel.split(BACKEND_SEPARATOR);
            backend = tokens[1];
            channel = tokens[0];
        }
        Query query = new Query(channel, backend, start, end, bins);
        String path = "/events";
        String accept = cbor ? "application/cbor-framed" : MediaType.APPLICATION_JSON;
        if (bins != null) {
            path = "/binned";
        }

        WebTarget resource = client.target(url + path);
        resource = query.setResourceParams(resource);
        Response response = resource.request().header("Accept", accept).get();

        if (response.getStatus() != 200) {
            throw new IOException(String.format("Unable to retrieve data from server: %s [%d]", response.getStatusInfo().getReasonPhrase(), response.getStatus()));
        }

        listener.onStarted(query);

        try {
            if (cbor) {
                final QueryRecordListener recordListener = (listener instanceof QueryRecordListener) ? (QueryRecordListener) listener : null;
                try (InputStream inputStream = response.readEntity(InputStream.class)) {

                    while (true) {
                        byte[] sizeBytes = readStream(inputStream, 4);
                        int dataSize = ByteBuffer.wrap(sizeBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
                        byte[] padding = readStream(inputStream, 12);
                        // Read the data blob of the specified size
                        byte[] data = readStream(inputStream, dataSize);
                        padding = readStream(inputStream, (8 - (dataSize % 8)) % 8);

                        Map<String, Object> frame = mapper.readValue(new ByteArrayInputStream(data), Map.class);
                        List timestamps = (List) frame.getOrDefault("tss", null);
                        List ids = (List) frame.getOrDefault("pulses", null);
                        List values = (List) frame.getOrDefault("values", null);
                        String scalar_type = (String) frame.getOrDefault("scalar_type", null);
                        Boolean rangeFinal = (Boolean) frame.getOrDefault("rangeFinal", false);
                        if (scalar_type != null) {
                            listener.onMessage(query, values, ids, timestamps);
                            if (recordListener != null) {
                                if (values != null) {
                                    for (int i = 0; i < values.size(); i++) {
                                        recordListener.onRecord(query, values.get(i), (Long) ids.get(i), (Long) timestamps.get(i));
                                    }
                                }
                            }
                        }
                        if (rangeFinal == true) {
                            listener.onFinished(query, null);
                            break;
                        } else if (scalar_type == null) {
                            throw new IOException("Invalid cbor frame keys: " + Str.toString(frame.keySet()));
                        }
                        if (Thread.currentThread().isInterrupted()) {
                            throw new InterruptedException("Query has been aborted");
                        }
                    }
                }
            } else {
                String json = response.readEntity(String.class);
                Map frame = (Map) EncoderJson.decode(json, Map.class);
                List<Double> averages = (List) frame.getOrDefault("avgs", null);
                List<Integer> counts = (List) frame.getOrDefault("counts", null);
                List<Double> maxs = (List) frame.getOrDefault("maxs", null);
                List<Double> mins = (List) frame.getOrDefault("mins", null);
                List<Integer> ts1Ms = (List) frame.getOrDefault("ts1Ms", null);
                List<Integer> ts1Ns = (List) frame.getOrDefault("ts1Ns", null);
                List<Integer> ts2Ms = (List) frame.getOrDefault("ts2Ms", null);
                List<Integer> ts2Ns = (List) frame.getOrDefault("ts2Ns", null);
                Integer tsAnchor = (Integer) frame.getOrDefault("tsAnchor", null);
                Boolean rangeFinal = (Boolean) frame.getOrDefault("rangeFinal", false);
                long anchor_ms = tsAnchor.longValue() * 1000;

                List<Long> ts1 = ts1Ms.stream()
                        .map(num -> ((long) num + anchor_ms) * 1000000)
                        .collect(Collectors.toList());
                List<Long> ts2 = ts2Ms.stream()
                        .map(num -> ((long) num + anchor_ms) * 1000000)
                        .collect(Collectors.toList());

                if (listener instanceof QueryBinnedListener) {
                    ((QueryBinnedListener) listener).onMessage(query, averages, mins, maxs, counts, ts1, ts2);
                    if (listener instanceof QueryBinnedRecordListener) {
                        QueryBinnedRecordListener recordListener = (QueryBinnedRecordListener) listener;
                        if (averages != null) {
                            for (int i = 0; i < averages.size(); i++) {
                                recordListener.onRecord(query, averages.get(i), mins.get(i), maxs.get(i), counts.get(i), ts1.get(i), ts2.get(i));
                            }
                        }
                    }
                }
            }
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            listener.onFinished(query, ex);
            if (ex instanceof IOException) {
                throw (IOException) ex;
            }
            throw new IOException(ex);
        } finally {
            Logger.getLogger(Daqbuf.class.getName()).finer("Quit query task for channel: " + channel);
        }
    }

    public CompletableFuture startQuery(String channel, String start, String end, QueryListener listener) {
        return startQuery(channel, start, end, listener, null);
    }

    public CompletableFuture startQuery(String[] channels, String start, String end, QueryListener listener) throws IOException, InterruptedException {
        return startQuery(channels, start, end, listener, null);
    }

    public CompletableFuture startQuery(String channel, String start, String end, QueryListener listener, Integer bins) {
        return (CompletableFuture) Threading.getPrivateThreadFuture(()
                -> query(channel, start, end, listener, bins)
        );
    }

    public CompletableFuture startQuery(String[] channels, String start, String end, QueryListener listener, Integer bins) throws IOException, InterruptedException {
        return (CompletableFuture) Threading.getPrivateThreadFuture(() -> {
            List<CompletableFuture> futures = new ArrayList<>();
            for (String channel : channels) {
                futures.add(startQuery(channel, start, end, listener, bins));
            }
            boolean finished = false;

            try {
                while (!finished) {
                    Thread.sleep(10);
                    finished = true;
                    for (CompletableFuture future : futures) {
                        if (!future.isDone()) {
                            finished = false;
                        }
                    }
                }
            } catch (InterruptedException ex) {
                for (CompletableFuture future : futures) {
                    System.err.println("Interrupting " + future);
                    ((VisibleCompletableFuture) future).interrupt();
                }
                throw new InterruptedException("Query has been aborted");
            }
        }
        );
    }

    QueryListener getPrintQueryListener(Integer bins) {
        AtomicBoolean printedHeader = new AtomicBoolean(false);
        if (bins == null) {
            return new QueryRecordListener() {
                @Override
                public void onStarted(Query query) {
                    if (!printedHeader.get()) {
                        System.out.printf("%s%s%s", COLOR_CYAN, String.format(PRINT_QUERY_FORMAT, (Object[]) QUERY_FIELDS), COLOR_RESET);
                        printedHeader.set(true);
                    }
                }

                @Override
                public void onRecord(Query query, Object value, Long id, Long timestamp) {
                    System.out.printf("%s", String.format(PRINT_QUERY_FORMAT, query.channel, timestampToStr(timestamp, false), id.toString(), value.toString()));
                }

            };
        } else {
            return new QueryBinnedRecordListener() {
                @Override
                public void onStarted(Query query) {
                    if (!printedHeader.get()) {
                        System.out.printf("%s%s%s", COLOR_CYAN, String.format(PRINT_BINNED_QUERY_FORMAT, (Object[]) BINNED_QUERY_FIELDS), COLOR_RESET);
                        printedHeader.set(true);
                    }
                }

                @Override
                public void onRecord(Query query, Double average, Double min, Double max, Integer count, Long start, Long end) {
                    System.out.printf("%s", String.format(PRINT_BINNED_QUERY_FORMAT, query.channel, timestampToStr(start, false), timestampToStr(end, false),
                            average.toString(), min.toString(), max.toString(), count.toString()));
                }
            };
        }
    }

    public CompletableFuture printQuery(String channel, String start, String end) throws IOException {
        return printQuery(channel, start, end, null);
    }

    public CompletableFuture printQuery(String[] channels, String start, String end) throws IOException, InterruptedException {
        return printQuery(channels, start, end, null);
    }

    public CompletableFuture printQuery(String channel, String start, String end, Integer bins) throws IOException {
        return startQuery(channel, start, end, getPrintQueryListener(bins), bins);
    }

    public CompletableFuture printQuery(String[] channels, String start, String end, Integer bins) throws IOException, InterruptedException {
        return startQuery(channels, start, end, getPrintQueryListener(bins), bins);
    }

    public Map<String, List> fetchQuery(String channel, String start, String end) throws IOException, InterruptedException {
        return fetchQuery(channel, start, end, null);
    }

    public Map<String, List> fetchQuery(String channel, String start, String end, Integer binSize) throws IOException, InterruptedException {
        Map ret = new HashMap();
        QueryListener listener = null;
        String[] fields = Arr.getSubArray((binSize == null) ? QUERY_FIELDS : BINNED_QUERY_FIELDS, 1); //Remove FIELD_NAME
        for (String field : fields) {
            ret.put(field, new ArrayList());
        }

        if (binSize == null) {
            listener = new QueryListener() {
                @Override
                public void onMessage(Query query, List values, List<Long> ids, List<Long> timestamps) {
                    ((List) ret.get(FIELD_VALUE)).addAll(values);
                    ((List<Long>) ret.get(FIELD_TIMESTAMP)).addAll(ids);
                    ((List<Long>) ret.get(FIELD_ID)).addAll(timestamps);
                }
            };
        } else {
            listener = new QueryBinnedListener() {
                @Override
                public void onMessage(Query query, List<Double> averages, List<Double> min, List<Double> max, List<Integer> count, List<Long> start, List<Long> end) {
                    //Single message in JSON frame
                    ret.put(FIELD_AVERAGE, averages);
                    ret.put(FIELD_MIN, min);
                    ret.put(FIELD_MAX, max);
                    ret.put(FIELD_START, start);
                    ret.put(FIELD_END, end);
                    ret.put(FIELD_COUNT, count);

                }
            };
        }
        query(channel, start, end, listener, binSize);
        return ret;
    }

    public Map<String, Map<String, List>> fetchQuery(String[] channels, String start, String end) throws IOException, InterruptedException {
        return fetchQuery(channels, start, end, null);
    }

    public Map<String, Map<String, List>> fetchQuery(String[] channels, String start, String end, Integer binSize) throws IOException, InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        Map<String, CompletableFuture<Map<String, List>>> futures = new HashMap<>();

        // Submit tasks for each channel
        for (String channel : channels) {
            CompletableFuture<Map<String, List>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return fetchQuery(channel, start, end, binSize);
                } catch (IOException | InterruptedException e) {
                    throw new CompletionException(e);
                }
            }, executor).exceptionally(ex -> {
                return Collections.emptyMap();
            });
            futures.put(channel, future);
        }

        executor.shutdown();

        // Wait for all tasks to complete and collect results
        Map<String, Map<String, List>> results = new HashMap<>();
        for (Map.Entry<String, CompletableFuture<Map<String, List>>> entry : futures.entrySet()) {
            String channel = entry.getKey();
            CompletableFuture<Map<String, List>> future = entry.getValue();
            try {
                results.put(channel, future.get());
            } catch (InterruptedException ex) {
                throw ex;
            } catch (ExecutionException ex) {
                if (ex.getCause() instanceof InterruptedException) {
                    throw (InterruptedException) ex.getCause();
                }
                if (ex.getCause() instanceof IOException) {
                    throw (IOException) ex.getCause();
                }
                throw new IOException(ex.getCause());
            }
        }
        return results;
    }

    public CompletableFuture startFetchQuery(String channel, String start, String end) throws IOException, InterruptedException {
        return (CompletableFuture) Threading.getPrivateThreadFuture(() -> fetchQuery(channel, start, end));
    }

    public CompletableFuture startFetchQuery(String channel, String start, String end, Integer binSize) throws IOException, InterruptedException {
        return (CompletableFuture) Threading.getPrivateThreadFuture(() -> fetchQuery(channel, start, end, binSize));
    }

    public CompletableFuture startFetchQuery(String[] channels, String start, String end) throws IOException, InterruptedException {
        return (CompletableFuture) Threading.getPrivateThreadFuture(() -> fetchQuery(channels, start, end));
    }

    public CompletableFuture startFetchQuery(String[] channels, String start, String end, Integer binSize) throws IOException, InterruptedException {
        return (CompletableFuture) Threading.getPrivateThreadFuture(() -> fetchQuery(channels, start, end, binSize));
    }

    public static LocalDateTime fromNanoseconds(long nanoseconds, boolean utc) {
        long seconds = nanoseconds / 1_000_000_000L;
        int nano = (int) (nanoseconds % 1_000_000_000L);
        Instant instant = Instant.ofEpochSecond(seconds, nano);
        return LocalDateTime.ofInstant(instant, utc ? ZoneOffset.UTC : ZoneOffset.systemDefault());
    }

    public static LocalDateTime fromMilliseconds(long milliseconds, boolean utc) {
        Instant instant = Instant.ofEpochMilli(milliseconds);
        return LocalDateTime.ofInstant(instant, utc ? ZoneOffset.UTC : ZoneOffset.systemDefault());
    }

    public static String timestampToStr(Long timestamp, boolean utc) {
        if (timestamp == null) {
            return "";
        }
        LocalDateTime currentTime = fromNanoseconds(timestamp, utc);
        String ret = currentTime.format(timeFormatter);
        if (utc) {
            ret = ret + "Z";
        }
        return ret;
    }

    public static long millisToPulseId(Long timestamp) {
        if (timestamp == null) {
            timestamp = System.currentTimeMillis();
        }
        long offset = timestamp - PULSE_ID_START_TIME;
        return (long) (offset / PULSE_ID_INTERVAL);
    }

    public static long pulseIdToMillis(Long pulseId) {
        if (pulseId == null) {
            return System.currentTimeMillis();
        }
        double offset = pulseId * PULSE_ID_INTERVAL;
        return (long) (PULSE_ID_START_TIME + offset);
    }

    public static String millisToStr(Long timestamp, boolean utc) {
        if (timestamp == null) {
            timestamp = System.currentTimeMillis();
        }
        LocalDateTime currentTime = fromMilliseconds(timestamp, utc);
        String ret = currentTime.format(timeFormatter);
        if (utc) {
            ret = ret + "Z";
        }
        return ret;
    }

    public static String pulseIdToStr(Long pulseId, boolean utc) {
        long millis = pulseIdToMillis(pulseId);
        return millisToStr(millis, utc);
    }

    public static String convertToUTC(String inputDateTime) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("[yyyy-MM-dd['T'][ HH:mm[:ss][.SSS]][X]]");
            OffsetDateTime offsetDateTime;
            try {
                offsetDateTime = OffsetDateTime.parse(inputDateTime, formatter);
            } catch (DateTimeParseException e) {
                LocalDateTime localDateTime = LocalDateTime.parse(inputDateTime, formatter);
                ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
                offsetDateTime = zonedDateTime.toOffsetDateTime();
            }
            if (inputDateTime.endsWith("Z")) {
                return offsetDateTime.toInstant().toString();
            }
            Instant instant = offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC).toInstant();
            return instant.toString();
        } catch (Exception ex) {
            return inputDateTime;
        }
    }

}
