
package ch.psi.utils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;


public class Time {
    public static final DateTimeFormatter defaultTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    public static String getTimeString(Number timestamp, boolean utc, boolean strict) {
        DateTimeFormatter formatter = 
                strict ?
                (utc 
                    ? DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    : DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"))
                :
                (utc 
                    ? DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS'Z'")
                    : DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                );

        Instant instant = Instant.ofEpochMilli(timestamp.longValue());
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, utc ? ZoneOffset.UTC : ZoneOffset.systemDefault());
        return localDateTime.format(formatter);
    }
    
    public static int compare(String timeStr1, String timeStr2) {
        OffsetDateTime time1 = parseIsoTime(timeStr1);
        OffsetDateTime time2 = parseIsoTime(timeStr2);

        // Compare the two times
        if (time1.isAfter(time2)) {
            return 1;
        } else if (time1.isBefore(time2)) {
            return -1;
        } else {
            return 0;
        }
    }

  private static OffsetDateTime parseIsoTime(String timeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd['T'][' ']HH:mm[:ss[.SSS]][XXX]");
        try {
            // Parse without offset and assume the system default time zone
            LocalDateTime localDateTime = LocalDateTime.parse(timeStr, formatter);
            return localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
        } catch (DateTimeParseException e3) {
            throw new DateTimeParseException("Unable to parse time string: " + timeStr, timeStr, 0);
        }
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

    public static String timestampToStr(Long timestamp) {
        return timestampToStr(timestamp, false);
    }
    
    public static String timestampToStr(Long timestamp, boolean utc) {
        return timestampToStr(timestamp, utc, defaultTimeFormatter);
    }
    
    public static String timestampToStr(Long timestamp, boolean utc, DateTimeFormatter timeFormatter) {
        if (timestamp == null) {
            return "";
        }
        if (timeFormatter==null){
            timeFormatter = defaultTimeFormatter;
        }
        
        LocalDateTime currentTime = null;
        if (timestamp >= 1000000000000000L){
            //nanos
            currentTime = fromNanoseconds(timestamp, utc);
        } else if (timestamp >= 1000000000){
            //millis
            currentTime = fromMilliseconds(timestamp, utc);
        } 
        //LocalDateTime currentTime = fromNanoseconds(timestamp, utc);
        String ret = currentTime.format(timeFormatter);
        if (utc) {
            ret = ret + "Z";
        }
        return ret;
    }
    
    public static Long timestampToMillis(long timestamp) {
        if (timestamp >= 1000000000000000L){
            //nanos
            return timestamp / 1_000_000L;
        }
        return timestamp;
    }
    
    public static String millisToStr(Long timestamp) {
        return millisToStr(timestamp, false);
    }
    
    public static String millisToStr(Long timestamp, boolean utc) {
        return millisToStr(timestamp, utc, null);
    }

    public static String millisToStr(Long timestamp, boolean utc, DateTimeFormatter timeFormatter) {
        if (timestamp == null) {
            timestamp = System.currentTimeMillis();
        }
        if (timeFormatter==null){
            timeFormatter = defaultTimeFormatter;
        }
        LocalDateTime currentTime = fromMilliseconds(timestamp, utc);
        String ret = currentTime.format(timeFormatter);
        if (utc) {
            ret = ret + "Z";
        }
        return ret;
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
