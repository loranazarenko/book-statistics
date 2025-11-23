package com.profitsoft.application.entities;

import lombok.*;

/**
 * Statistics item representing aggregated data for a single attribute value.
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class StatisticsItem implements Comparable<StatisticsItem> {

    private String value;
    private long count;

    /**
     * Compare by count in descending order (highest count first).
     */
    @Override
    public int compareTo(StatisticsItem other) {
        return Long.compare(other.count, this.count);
    }
}
