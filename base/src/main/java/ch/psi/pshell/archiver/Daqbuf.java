package ch.psi.pshell.archiver;

import ch.psi.pshell.data.Manager;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Convert;
import ch.psi.pshell.utils.EncoderJson;
import ch.psi.pshell.utils.Str;
import ch.psi.pshell.utils.Threading;
import ch.psi.pshell.utils.Threading.VisibleCompletableFuture;
import ch.psi.pshell.utils.Time;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

/**
 * Access to Daqbuf service.
 */
public class Daqbuf implements ChannelQueryAPI {

    final static boolean SEARCH_HANDLES_BACKEND = true;
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

    static final Long PULSE_ID_START_TIME = 1504524711650L;
    static final double PULSE_ID_INTERVAL = 10;
    
    public static final String ADD_LAST_PREFIX = "<";     
    
    boolean timestampMillis = true;
    volatile String[] availableBackends;
    boolean streamed = true;
    CompletableFuture initialization;

    public static String getDefaultUrl() {
        return System.getenv().getOrDefault("DAQBUF_DEFAULT_URL", "https://data-api.psi.ch/api/4");
    }

    public static String getDefaultBackend() {
        return System.getenv().getOrDefault("DAQBUF_DEFAULT_BACKEND", "");
    }


    public String getAvailableDefaultBackend() {
        if (availableBackends!=null){
            if (!Arr.containsEqual(availableBackends, backend)){
                return availableBackends[0];
            }
        }
        return backend;
    }
    
    
    public String[] searchAvailableBackends() {
        try {
            Map<String, Object> params = new HashMap<>();
            WebTarget resource = client.target(url + "/backend/list");
            Response r = resource.request().accept(MediaType.APPLICATION_JSON).get();
            String json = r.readEntity(String.class);
            Map<String, Object> ret = (Map) EncoderJson.decode(json, Map.class);
            List<Map<String, Object>> list = (List<Map<String, Object>>) ret.getOrDefault("backends_available", null);

            List<String> backends = new ArrayList();
            for (Map m : list) {
                backends.add((String) m.get("name"));
            }
            return backends.toArray(new String[0]);
        } catch (Exception ex) {
            return null;
        }         
    }
    
    public String[] getAvailableBackends() {
        return availableBackends;
    }

    ObjectMapper mapper;    

    public Daqbuf() {
        this(getDefaultUrl());
    }

    public Daqbuf(String url) {
        this(url, getDefaultBackend());
    }

    public Daqbuf(String url, String backend) {
        if (url == null) {
            url = getDefaultUrl();
        }
        if (backend == null) {
            backend = getDefaultBackend();
        }
        if (!url.contains("://")) {
            url = "http://" + url;
        }
        this.url = url;
        this.backend = backend;
        ClientConfig config = new ClientConfig().register(JacksonFeature.class);
        client = ClientBuilder.newClient(config);
        mapper = new ObjectMapper(new CBORFactory());
        initialize();
    }

    public String getUrl() {
        return url;
    }

    public String getBackend() {
        return backend;
    }
    
    public boolean isBackendDefined(String backend) {
        return (backend!=null) && !backend.isBlank();
    }
    
    public boolean isBackendDefined() {
        return isBackendDefined(backend);
    }
    
    public void setTimestampMillis(boolean value){
        timestampMillis =value;
    }
    
    public boolean getTimestampMillis(){
        return timestampMillis;
    }    
        
    public void setStreamed(boolean value){
        streamed =value;
    }
    
    public boolean isStreamed(){
        return streamed;
    }    
    
    public CompletableFuture initialize(){
        if ((initialization==null) || initialization.isDone()){
            initialization =  Threading.getPrivateThreadFuture(() -> {
                availableBackends = searchAvailableBackends();
            });
        }
        return initialization;
    }
    
    
    public CompletableFuture getInitialization(){
        return initialization;
    }
    
    public boolean isInitialized(){
        return ((initialization!=null) && initialization.isDone());
    }

    
    static boolean isBinned(Integer bins) {
        return  (bins != null) && (bins > 0);
    }
    
    public List<Map<String, Object>> search(String regex) throws IOException {
        return search(regex, null);
    }

    public List<Map<String, Object>> search(String regex, Boolean caseInsensitive) throws IOException {
        return search(regex, caseInsensitive, null);
    }

    public List<Map<String, Object>> search(String regex, Boolean caseInsensitive, Integer limit) throws IOException {
        String backend = getChannelBackend(regex);
        regex = getChannelName(regex);
        return search(backend, regex, caseInsensitive, limit);
    }

    volatile List<Map<String, Object>> lastSearch;

    public List<Map<String, Object>> getLastSearch() {
        return lastSearch;
    }

    public List<Map<String, Object>> search(String backend, String regex, Boolean caseInsensitive, Integer limit) throws IOException {

        Map<String, Object> params = new HashMap<>();
        params.put("nameRegex", regex);
        if (caseInsensitive != null) {
            params.put("icase", caseInsensitive);
        }
        if (SEARCH_HANDLES_BACKEND) {
            if (backend != null) {
                params.put("backend", backend);
            }
        }
        WebTarget resource = client.target(url + "/search/channel");
        for (String paramName : params.keySet()) {
            resource = resource.queryParam(paramName, params.get(paramName));
        }
        Response r = resource.request().accept(MediaType.APPLICATION_JSON).get();
        String json = r.readEntity(String.class);
        Map<String, Object> ret = (Map) EncoderJson.decode(json, Map.class);
        List<Map<String, Object>> list = (List<Map<String, Object>>) ret.getOrDefault("channels", null);

        if (!SEARCH_HANDLES_BACKEND) {
            if ((backend != null) && (!backend.isBlank())) {
                list = list.stream()
                        .filter(map -> backend.equals(map.get("backend")))
                        .collect(Collectors.toList());
            }
        }
        if ((limit != null) && (limit >= 0) && (list.size() > limit)) {
            list = list.subList(0, limit);
        }
        lastSearch = list;
        return list;
    }

    public CompletableFuture startSearch(String regex) {
        return (CompletableFuture) Threading.getPrivateThreadFuture(() -> search(regex));
    }

    public CompletableFuture startSearch(String regex, Boolean caseInsensitive) {
        return (CompletableFuture) Threading.getPrivateThreadFuture(() -> search(regex, caseInsensitive));
    }

    public CompletableFuture startSearch(String regex, Boolean caseInsensitive, Integer limit) {
        return (CompletableFuture) Threading.getPrivateThreadFuture(() -> search(regex, caseInsensitive, limit));
    }

    public CompletableFuture startSearch(String backend, String regex, Boolean caseInsensitive, Integer limit) {
        return (CompletableFuture) Threading.getPrivateThreadFuture(() -> search(backend, regex, caseInsensitive, limit));
    }

    String getChannelDesc(Map queryEntry) {
        String name = queryEntry.get("name").toString();
        //String backend = queryEntry.get("backend").toString() ;
        String shape = queryEntry.get("shape").toString();
        String type = queryEntry.get("type").toString();

        String desc = name;
        //desc = desc + " " + backend;
        //desc = desc + " " + type;
        //if ((shape != null) && (!shape.isBlank()) && !shape.trim().equals("[]")){
        //    desc = desc + " " + shape;
        //} 
        return desc;
    }

    @Override
    public List<String> queryChannels(String text, String backend, int limit) throws IOException {
        List<Map<String, Object>> ret = search(backend, text, null, limit);
        return ret.stream()
                //.map(map -> map.get("name").toString() )
                .map(map -> getChannelDesc(map))
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

    public class EndOfStreamException extends IOException {

        final int bytesRead;
        final int dataSize;

        EndOfStreamException(int bytesRead, int dataSize) {
            super(String.format("Unexpected end of stream (%d of %d bytes).",bytesRead, dataSize));
            this.bytesRead = bytesRead;
            this.dataSize = dataSize;
        }
    }

    public class TimeoutException extends IOException {

        TimeoutException() {
            super("Timeout receiving from stream");
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
                throw new EndOfStreamException(bytesRead, dataSize);
            } else if (count == 0) {
                if ((System.currentTimeMillis() - last) > timeout) {
                    throw new TimeoutException();
                }
                Thread.sleep(1);
            } else {
                bytesRead += count;
                last = System.currentTimeMillis();
            }
        }
        return data;
    }
    
    String readStreamLine(InputStream inputStream) throws IOException, InterruptedException {
        long start = System.currentTimeMillis();    
        StringBuilder ret = new StringBuilder();
        int timeout = 10000;
        while(true){
            char c = (char)readStream(inputStream,1)[0];
            if (c == '\n'){
                break;
            }
            if ((System.currentTimeMillis() - start) > timeout) {
                throw new TimeoutException();
            }      
            ret.append(c);
        }        
        return ret.toString();
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

        default void onMessage(Query query, List average, List min, List max, List<Integer> count, List<Long> start, List<Long> end) {
        }
    }

    public interface QueryBinnedRecordListener extends QueryBinnedListener {

        default void onRecord(Query query, Object average, Object min, Object max, Integer count, Long start, Long end) {
        }
    }   
            
    public static class Query {

        public final String channel;
        public final String backend;
        public final String start;
        public final String end;
        public final Integer bins;
        public final Boolean addLast;

        Query(String channel, String backend, String start, String end, Integer bins) {
            addLast = (end==null) || (start.startsWith(ADD_LAST_PREFIX));
            if (start.startsWith(ADD_LAST_PREFIX)){
                start=start.substring(1);
            }
            this.channel = channel;
            this.backend = backend;
            this.start = Time.convertToUTC(start);
            this.end = (end==null) ? this.start: Time.convertToUTC(end);
            this.bins = bins;
        }

        WebTarget setResourceParams(WebTarget resource) {
            if (Str.isDigit(channel)){
                resource = resource.queryParam("seriesId", channel);
            } else {
                resource = resource.queryParam("channelName", channel);
            }            
            resource = resource.queryParam("begDate", start);
            resource = resource.queryParam("endDate", end);
            if ((backend!=null) && !backend.isBlank()){
                resource = resource.queryParam("backend", backend);
            }
            if (isBinned(bins)) {
                resource = resource.queryParam("binCount", bins);
            }            
            if (addLast){
                resource = resource.queryParam("oneBeforeRange", "true");
            }
            return resource;

        }
    }

    public void query(String channel, String start, String end, QueryListener listener) throws IOException, InterruptedException {
        query(channel, start, end, listener, null);
    }

    public String getChannelName(String channel) {
        if (channel.contains(BACKEND_SEPARATOR)) {
            return channel.split(BACKEND_SEPARATOR)[0];
        }
        return channel;
    }
    public String getChannelBackend(String channel) {
        if (channel.contains(BACKEND_SEPARATOR)) {
            String[] tokens = channel.split(BACKEND_SEPARATOR);
            if ((tokens.length) > 1) {
                return tokens[1];
            }
        }
        return getAvailableDefaultBackend();
    }

    public void query(String channel, String start, String end, QueryListener listener, Integer bins) throws IOException, InterruptedException {
        boolean cbor = !isBinned(bins);
        String backend = getChannelBackend(channel);
        channel = getChannelName(channel);
        //Query last value

        Query query = new Query(channel, backend, start, end, bins);
        String path = "/events";
        String accept = cbor ? "application/cbor-framed" : (isStreamed() ? "application/json-framed" : MediaType.APPLICATION_JSON);
        if (isBinned(bins)) {
            path = "/binned";
        }

        WebTarget resource = client.target(url + path);
        resource = query.setResourceParams(resource);
        Response response = resource.request().header("Accept", accept).get();
        IOException e = null;

        if (response.getStatus() != 200) {
            try{
                String json = response.readEntity(String.class);
                Map body = (Map) EncoderJson.decode(json, Map.class);                
                String message = (String) body.get("message");
                String id =  (String) body.get("requestid");                
                if (message.isBlank()){
                    throw new Exception();
                }
                message = Str.capitalizeFirst(message);                
                e = new IOException(String.format("%s\nChannel: %s\nRequest ID: %s", message , channel, id));
            } catch (Exception ex){
                e = new IOException(String.format("Error retrieving data: %s [%d]\nChannel: %s",  response.getStatusInfo().getReasonPhrase(), response.getStatus(), channel));
            } finally{
                throw e;
            }
        }

        listener.onStarted(query);
        Exception queryException = null;
        try {
            if (cbor) {
                final QueryRecordListener recordListener = (listener instanceof QueryRecordListener) ? (QueryRecordListener) listener : null;
                try (InputStream inputStream = response.readEntity(InputStream.class)) {

                    while (true) {
                        byte[] sizeBytes = null;
                        try {
                            sizeBytes = readStream(inputStream, 4);
                        } catch (EndOfStreamException ex) {
                            if (ex.bytesRead > 0) {
                                throw ex;
                            }
                            break;
                        }
                        int dataSize = ByteBuffer.wrap(sizeBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
                        byte[] padding = readStream(inputStream, 12);
                        // Read the data blob of the specified size
                        byte[] data = readStream(inputStream, dataSize);
                        padding = readStream(inputStream, (8 - (dataSize % 8)) % 8);

                        Map<String, Object> frame = mapper.readValue(new ByteArrayInputStream(data), Map.class);
                        String error = (String) frame.getOrDefault("error", null);
                        if ((error != null) && (!error.isEmpty())) {
                            throw new IOException(error.trim());
                        }
                        if (!frame.getOrDefault("type", "").equals("keepalive")) {
                            Boolean rangeFinal = (Boolean) frame.getOrDefault("rangeFinal", false);
                            readCborEvents(query, listener, recordListener, frame);
                            if (rangeFinal == true) {
                                break;
                            }
                            if (Thread.currentThread().isInterrupted()) {
                                throw new InterruptedException("Query has been aborted");
                            }
                        }
                    }
                }
            } else {
                if (isStreamed()){
                    try (InputStream inputStream = response.readEntity(InputStream.class)) {
                        while (true) {
                            int dataSize=0;                            
                            try {
                                String line = readStreamLine(inputStream).trim();
                                dataSize = Integer.valueOf(line);
                            } catch (EndOfStreamException ex) {
                                break;
                            }
                            byte[] bytes = readStream(inputStream, dataSize);
                            byte[] lf  = readStream(inputStream, 1);
                            String json = new String(bytes);
                            Map<String, Object>  frame = (Map<String, Object> ) EncoderJson.decode(json, Map.class);                                    
                            String error = (String) frame.getOrDefault("error", null);
                            if ((error != null) && (!error.isEmpty())) {
                                throw new IOException(error.trim());
                            }
                            if (!frame.getOrDefault("type", "").equals("keepalive")) {
                                Boolean rangeFinal = (Boolean) frame.getOrDefault("rangeFinal", false);
                                readJsonBinned(query, listener, frame);
                                if (rangeFinal == true) {
                                    break;
                                }
                                if (Thread.currentThread().isInterrupted()) {
                                    throw new InterruptedException("Query has been aborted");
                                }
                            }
                        }                   
                    }
                } else {
                    String json = response.readEntity(String.class);
                    Map<String, Object>  frame = (Map<String, Object> ) EncoderJson.decode(json, Map.class);
                    readJsonBinned(query, listener, frame);
                }
            }
        } catch (InterruptedException|IOException ex) {
            queryException = ex;
            throw ex;
        } catch (Exception ex) {
            queryException = ex;
            throw new IOException(ex);
        } finally {
            listener.onFinished(query, queryException);
            Logger.getLogger(Daqbuf.class.getName()).finer("Quit query task for channel: " + channel);
        }
    }
    
    protected void readJsonBinned(Query query, QueryListener listener, Map<String, Object>  frame) throws IOException{        
        List averages = (List) frame.getOrDefault("avgs", null);
        List maxs = (List) frame.getOrDefault("maxs", null);
        List mins = (List) frame.getOrDefault("mins", null);
        List<Integer> counts = (List) frame.getOrDefault("counts", null);
        List<Number> ts1Ms = (List) frame.getOrDefault("ts1Ms", null);
        List<Number> ts1Ns = (List) frame.getOrDefault("ts1Ns", null);
        List<Number> ts2Ms = (List) frame.getOrDefault("ts2Ms", null);
        List<Number> ts2Ns = (List) frame.getOrDefault("ts2Ns", null);
        Integer tsAnchor = (Integer) frame.getOrDefault("tsAnchor", null);
        Boolean rangeFinal = (Boolean) frame.getOrDefault("rangeFinal", false);
        if ((averages==null) || (averages.size()==0) || ((averages.size()==1) && (averages.get(0)==null))){
            return;
        }
        if ((mins==null) || (mins.size()!=averages.size())){
            return;
        }
        if ((maxs==null) || (maxs.size()!=averages.size())){
            return;
        }        
        averages.replaceAll(e -> e == null ? Double.NaN : e);
        mins.replaceAll(e -> e == null ? Double.NaN : e);
        maxs.replaceAll(e -> e == null ? Double.NaN : e);
        
        long anchor_ms = tsAnchor.longValue() * 1000;
        long aux =  getTimestampMillis() ? 1L : 1_000_000L;

        List<Long> ts1 = ts1Ms.stream()
                .map(num -> (num.longValue() + anchor_ms) * aux)
                .collect(Collectors.toList());
        List<Long> ts2 = ts2Ms.stream()
                .map(num -> (num.longValue() + anchor_ms) * aux)
                .collect(Collectors.toList());

        if (listener instanceof QueryBinnedListener queryBinnedListener) {
            queryBinnedListener.onMessage(query, averages, mins, maxs, counts, ts1, ts2);
            if (listener instanceof QueryBinnedRecordListener queryBinnedRecordListener) {
                if (averages != null) {
                    for (int i = 0; i < averages.size(); i++) {
                        queryBinnedRecordListener.onRecord(query, averages.get(i), mins.get(i), maxs.get(i), counts.get(i), ts1.get(i), ts2.get(i));
                    }
                }
            }
        }        
    }
    
    public static class EnumValue extends Number{
        final Number value;
        final String string;
        
        EnumValue(Number value, String string){
            this.value = value;            
            this.string = string;
        }

        public String stringValue() {
            return string;
        }
        
        @Override
        public int intValue() {
            return value.intValue();
        }

        @Override
        public long longValue() {
            return value.longValue();
        }

        @Override
        public float floatValue() {
            return value.floatValue();
        }

        @Override
        public double doubleValue() {
            return value.doubleValue();
        }
        
    }
    
    protected void readCborEvents(Query query, QueryListener listener, QueryRecordListener recordListener, Map<String, Object>  frame) throws IOException{        
        List<Long> timestamps = (List) frame.getOrDefault("tss", null);
        List ids = (List) frame.getOrDefault("pulses", null);
        List values = (List) frame.getOrDefault("values", null);
        String scalar_type = (String) frame.getOrDefault("scalar_type", null);
        List valuestrings = (List) frame.getOrDefault("valuestrings", null);
        
        if ((values==null) || (values.size()==0) || ((values.size()==1) && (values.get(0)==null))){
            return;
        }
        if ((timestamps==null) || (timestamps.size()!=values.size())){
            return;
        }
        
        if (scalar_type != null) {
            if (getTimestampMillis()){
                timestamps.replaceAll(value -> value / 1_000_000);
            }
            switch (scalar_type){
                case "f64" -> values.replaceAll(e -> e == null ? Double.NaN : e);
                case "f32" -> values.replaceAll(e -> e == null ? Float.NaN : e);
                case "enum" -> {
                    if ((valuestrings!=null) && (valuestrings.size()==values.size())){
                        for (int i=0; i<values.size();i++){
                            values.set(i, new EnumValue((Number)values.get(i), valuestrings.get(i).toString()));
                        }
                    }
                }
            }
            
            listener.onMessage(query, values, ids, timestamps);
            if (recordListener != null) {
                boolean have_id = (ids!=null) && (ids.size()==values.size());
                boolean have_timestamp = (timestamps!=null) && (timestamps.size()==values.size());
                if (values != null) {
                    for (int i = 0; i < values.size(); i++) {
                        recordListener.onRecord(query, values.get(i), have_id ? (Long) ids.get(i) : null, have_timestamp ? (Long) timestamps.get(i): null);
                    }
                }
            }
        } else if (scalar_type == null) {
            throw new IOException("Invalid cbor frame keys: " + Str.toString(frame.keySet()));
        }
    }

    public CompletableFuture startQuery(String channel, String start, String end, QueryListener listener) {
        return startQuery(channel, start, end, listener, null);
    }

    public CompletableFuture startQuery(String[] channels, String start, String end, QueryListener listener) {
        return startQuery(channels, start, end, listener, null);
    }

    public CompletableFuture startQuery(String channel, String start, String end, QueryListener listener, Integer bins) {
        return (CompletableFuture) Threading.getPrivateThreadFuture(()
                -> query(channel, start, end, listener, bins)
        );
    }

    public CompletableFuture startQuery(String[] channels, String start, String end, QueryListener listener, Integer bins) {
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
        if (isBinned(bins)) {
            return new QueryBinnedRecordListener() {
                @Override
                public void onStarted(Query query) {
                    if (!printedHeader.get()) {
                        System.out.printf("%s%s%s", COLOR_CYAN, String.format(PRINT_BINNED_QUERY_FORMAT, (Object[]) BINNED_QUERY_FIELDS), COLOR_RESET);
                        printedHeader.set(true);
                    }
                }

                @Override
                public void onRecord(Query query, Object average, Object min, Object max, Integer count, Long start, Long end) {
                    System.out.printf("%s", String.format(PRINT_BINNED_QUERY_FORMAT, query.channel, Time.timestampToStr(start, false), Time.timestampToStr(end, false),
                            average.toString(), min.toString(), max.toString(), count.toString()));
                }
            };
        } else {
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
                    System.out.printf("%s", String.format(PRINT_QUERY_FORMAT, query.channel, Time.timestampToStr(timestamp, false), id.toString(), value.toString()));
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

    public Map<String, List> fetchQuery(String channel, String start, String end, Integer bins) throws IOException, InterruptedException {
        Map ret = new HashMap();
        QueryListener listener = null;
        
        String[] fields = Arr.getSubArray(isBinned(bins) ? BINNED_QUERY_FIELDS : QUERY_FIELDS, 1); //Remove FIELD_NAME
        for (String field : fields) {
            ret.put(field, new ArrayList());
        }

        if (isBinned(bins)) {
            listener = new QueryBinnedListener() {
                @Override
                public void onMessage(Query query, List averages, List min, List max, List<Integer> count, List<Long> start, List<Long> end) {                   
                    ((List) ret.get(FIELD_AVERAGE)).addAll(averages);
                    ((List) ret.get(FIELD_MIN)).addAll(min);
                    ((List) ret.get(FIELD_MAX)).addAll(max);
                    ((List) ret.get(FIELD_START)).addAll(start);
                    ((List) ret.get(FIELD_END)).addAll(end);
                    ((List) ret.get(FIELD_COUNT)).addAll(count);                    
                }
            };
        } else {
            listener = new QueryListener() {
                @Override
                public void onMessage(Query query, List values, List<Long> ids, List<Long> timestamps) {
                    ((List) ret.get(FIELD_VALUE)).addAll(values);
                    ((List<Long>) ret.get(FIELD_TIMESTAMP)).addAll(ids);
                    ((List<Long>) ret.get(FIELD_ID)).addAll(timestamps);
                }
            };
        }
        query(channel, start, end, listener, bins);
        return ret;
    }

    public Map<String, Map<String, List>> fetchQuery(String[] channels, String start, String end) throws IOException, InterruptedException {
        return fetchQuery(channels, start, end, null);
    }

    public Map<String, Map<String, List>> fetchQuery(String[] channels, String start, String end, Integer bins) throws IOException, InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        Map<String, CompletableFuture<Map<String, List>>> futures = new HashMap<>();

        // Submit tasks for each channel
        for (String channel : channels) {
            CompletableFuture<Map<String, List>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return fetchQuery(channel, start, end, bins);
                } catch (Exception e) {
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
                if (ex.getCause() instanceof InterruptedException e) {
                    throw e;
                }
                if (ex.getCause() instanceof IOException e) {
                    throw e;
                }
                throw new IOException(ex.getCause());
            }
        }
        return results;
    }

    public CompletableFuture startFetchQuery(String channel, String start, String end) {
        return (CompletableFuture) Threading.getPrivateThreadFuture(() -> fetchQuery(channel, start, end));
    }

    public CompletableFuture startFetchQuery(String channel, String start, String end, Integer bins) {
        return (CompletableFuture) Threading.getPrivateThreadFuture(() -> fetchQuery(channel, start, end, bins));
    }

    public CompletableFuture startFetchQuery(String[] channels, String start, String end) {
        return (CompletableFuture) Threading.getPrivateThreadFuture(() -> fetchQuery(channels, start, end));
    }

    public CompletableFuture startFetchQuery(String[] channels, String start, String end, Integer bins) {
        return (CompletableFuture) Threading.getPrivateThreadFuture(() -> fetchQuery(channels, start, end, bins));
    }

    public void saveQuery(String filename, String channel, String start, String end) throws IOException, InterruptedException {
        saveQuery(filename, channel, start, end, null);
    }

    public void saveQuery(String filename, String channel, String start, String end, Integer bins) throws IOException, InterruptedException {
        try (Manager dm = getDataManager(filename)) {
            saveQuery(dm, channel, start, end, bins);
        }
    }

    Manager getDataManager(String filename) throws IOException, InterruptedException {
        try {
            return new Manager(filename, "h5");
        } catch (InterruptedException | IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex);
        }

    }

    void saveQuery(Manager dm, String channel, String start, String end, Integer bins) throws IOException, InterruptedException {
        String channelBackend = getChannelBackend(channel);
        String channelName = getChannelName(channel);
        String dataGroup = "/" + channelBackend + "/" + channelName + "/";
        dm.createGroup(dataGroup);
        dm.setAttribute(dataGroup, "backend", channelBackend);
        dm.setAttribute(dataGroup, "name", channelName);
        dm.setAttribute(dataGroup, "start", start);
        dm.setAttribute(dataGroup, "end", end);
        dm.setAttribute(dataGroup, "bins", isBinned(bins) ? bins : Integer.valueOf(0));
               
        QueryListener listener = null;
        if (isBinned(bins)) {
            Map<String, List> data = fetchQuery(channel, start, end, bins);
            for (String field : data.keySet()) {
                List list = data.get(field);
                if (!list.isEmpty()) {
                    Object array = Convert.toPrimitiveArray(list);                    
                    dm.setDataset(dataGroup + field, array);
                }
            }            
        } else {
            String datasetValue = dataGroup + "value";
            String datasetId = dataGroup + "id";
            String datasetTimestamp = dataGroup + "timestamp";
            
            listener = new QueryListener() {
                @Override
                public void onMessage(Query query, List values, List<Long> ids, List<Long> timestamps) {
                    if (!values.isEmpty()) {
                    try {                                                    
                            Object value = Convert.toPrimitiveArray(values);                                                                                    
                            if (!dm.exists(datasetValue)) {
                                dm.createCompressedDataset(datasetId, Long.class, new int[0]);
                                dm.createCompressedDataset(datasetTimestamp, Long.class, new int[0]);
                                Object obj = Array.get(value, 0);
                                dm.createCompressedDataset(datasetValue, obj);
                            }
                            if (timestamps!=null){
                                long[] timestamp = (long[]) Convert.toPrimitiveArray(timestamps, Long.class);
                                dm.appendItem(datasetTimestamp, timestamp);
                            }                            
                            if (ids!=null){
                                long[] id = (long[]) Convert.toPrimitiveArray(ids, Long.class);
                                dm.appendItem(datasetId, id);
                            }
                            dm.appendItem(datasetValue, value);
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
            };
            query(channel, start, end, listener, bins);            
        }
    }

    public void saveQuery(String filename, String[] channels, String start, String end) throws IOException, InterruptedException {
        saveQuery(filename, channels, start, end, null);
    }

    public void saveQuery(String filename, String[] channels, String start, String end, Integer bins) throws IOException, InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        Map<String, CompletableFuture> futures = new HashMap<>();
        try (Manager dm = getDataManager(filename)) {
            // Submit tasks for each channel
            for (String channel : channels) {
                CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        saveQuery(dm, channel, start, end, bins);
                        return null;
                    } catch (Exception e) {
                        Logger.getLogger(Daqbuf.class.getName()).log(Level.WARNING, null, e);
                        throw new CompletionException(e);
                    }
                }, executor).exceptionally(ex -> {
                    return ex;
                });
                futures.put(channel, future);
            }

            executor.shutdown();

            // Wait for all tasks to complete and collect results
            Map<String, Map<String, List>> results = new HashMap<>();
            for (Map.Entry<String, CompletableFuture> entry : futures.entrySet()) {
                String channel = entry.getKey();
                CompletableFuture<Boolean> future = entry.getValue();
                try {
                    Object ret = future.get();
                    if (ret instanceof InterruptedException e) {
                        throw e;
                    }
                } catch (InterruptedException ex) {
                    throw ex;
                } catch (Exception ex) {
                    Logger.getLogger(Daqbuf.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }  catch (IOException | InterruptedException ex){
            throw ex;
        }  catch (Throwable t){
            Logger.getLogger(Daqbuf.class.getName()).log(Level.SEVERE, null, t);
        }
    }

    public CompletableFuture startSaveQuery(String filename, String channel, String start, String end) {
        return (CompletableFuture) Threading.getPrivateThreadFuture(() -> saveQuery(filename, channel, start, end));
    }

    public CompletableFuture startSaveQuery(String filename, String channel, String start, String end, Integer bins) {
        return (CompletableFuture) Threading.getPrivateThreadFuture(() -> saveQuery(filename, channel, start, end, bins));
    }

    public CompletableFuture startSaveQuery(String filename, String[] channels, String start, String end) {
        return (CompletableFuture) Threading.getPrivateThreadFuture(() -> saveQuery(filename, channels, start, end));
    }

    public CompletableFuture startSaveQuery(String filename, String[] channels, String start, String end, Integer bins) {
        return (CompletableFuture) Threading.getPrivateThreadFuture(() -> saveQuery(filename, channels, start, end, bins));
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

    public static String pulseIdToStr(Long pulseId, boolean utc) {
        long millis = pulseIdToMillis(pulseId);
        return Time.millisToStr(millis, utc);
    }


    public static void main(String[] args) throws Exception {
        CompletableFuture cf;
        Map ret;
        Object obj;
        Daqbuf daqbuf = new Daqbuf();

        /*
        String[] channels = new String[]{"S10BC01-DBPM010:Q1@sf-databuffer", "S10BC01-DBPM010:X1@sf-databuffer"};
        String channel = "S10BC01-DBPM010:Q1";
        
        
        String start = "2024-05-02 09:00:00"; //"2024-03-15T12:41:00Z", "2024-03-15T15:42:00Z"
        //String end = "2024-05-02 09:00:01";
        String end = "2024-05-02 10:00:00";
        
        long s = System.currentTimeMillis();        
        daqbuf.saveQuery("/Users/gobbo_a/pshell.h5", channels, start, end);
        System.out.println(System.currentTimeMillis()-s);        
         */
        
       
        System.out.println(daqbuf.searchAvailableBackends());
        int bins = 20;
        String start = "2024-07-07 16:00:00";
        String end = "2024-07-07 16:00:01" ;
        String channel = "SARFE10-PSSS059:SPECTRUM_X";
        //ret = daqbuf.fetchQuery(channel, start, end);
        //System.out.println(ret);

        
        //cf = daqbuf.startQuery("S10BC01-DBPM010:Q1", start, end , new QueryListener(){}) ;

        
        //cf = daqbuf.printQuery(channel, start, end );
        //cf = daqbuf.printQuery(channel, start, end ,bins);
        /*
        cf = daqbuf.printQuery(channels, start, end );
        cf = daqbuf.printQuery(channels, start, end , bins);
        

        ret = daqbuf.fetchQuery(channel, start, end );
        ret = daqbuf.fetchQuery(channel, start, end, bins);
        ret = daqbuf.fetchQuery(channels, start, end );
        ret = daqbuf.fetchQuery(channels, start, end ,bins);


        cf = daqbuf.startFetchQuery(channel, start, end);
        cf.handle((val, ex) -> {
                if (ex==null){
                    System.out.println(val);
                }
                return val;
        });
        obj = cf.get();
        System.out.println(obj);
        
        QueryListener listener = new  QueryRecordListener() {
            public void onRecord(Query query, Object value, Long id, Long timestamp) {
                System.out.println(query.channel + " - " + timestamp + " - " + value);
            }   
        };
        cf = daqbuf.startQuery(channels, start, end, listener);         
        cf.get();
        
        List search = daqbuf.search("PSSS@sf-imagebuffer");
        daqbuf.printSearch(search);
        search = daqbuf.search("PSSS@");
        daqbuf.printSearch(search);
        search = daqbuf.queryChannels("PSSS", daqbuf.backend, 10);
        search = daqbuf.queryChannels("PSSSx", daqbuf.backend, 10);
        
        cf = daqbuf.startSearch("PSSS");
        cf.handle((val, ex) -> {
                if (ex==null){
                    System.out.println(val);
                }
                return val;
        });
        obj = cf.get();
        System.out.println(obj);
         */
        

    }      
}
