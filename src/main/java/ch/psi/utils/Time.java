
package ch.psi.utils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;


public class Time {
    
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
}
