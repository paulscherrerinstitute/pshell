package ch.psi.pshell.xscan.core;

import java.util.Comparator;

/**
 * Comparator for comparint 2 timestamped values
 */
public class TimestampedValueComparator implements Comparator<TimestampedValue> {

	@Override
	public int compare(TimestampedValue o1, TimestampedValue o2) {
		if (o1.getTimestamp() < o2.getTimestamp()) {
			return -1;
		} else if (o1.getTimestamp() > o2.getTimestamp()) {
			return 1;
		} else if (o1.getTimestamp() == o2.getTimestamp()) {
			if (o1.getNanosecondsOffset() < o2.getNanosecondsOffset()) {
				return -1;
			} else if (o1.getNanosecondsOffset() > o2.getNanosecondsOffset()) {
				return 1;
			}
		}
		return 0;
	}
}
