// Copyright Verizon Media. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.hosted.controller.api.integration.billing;

import com.yahoo.config.provision.ApplicationId;
import com.yahoo.config.provision.zone.ZoneId;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;


/**
 * An Invoice is an identifier with a status (with history) and line items.  A line item is the meat and
 * potatoes of the content of the invoice, and are a history of items.  Most line items are connected to
 * a given deployment in Vespa Cloud, but they can also be manually added to e.g. give a discount or represent
 * support.
 * <p>
 * All line items have a Plan associated with them - which was used to map from utilization to an actual price.
 * <p>
 * The invoice has a status history, but only the latest status is exposed through this API.
 *
 * @author ogronnesby
 */
public class Invoice {
    private static final BigDecimal SCALED_ZERO = new BigDecimal("0.00");

    private final Id id;
    private final List<LineItem> lineItems;
    private final StatusHistory statusHistory;
    private final ZonedDateTime startTime;
    private final ZonedDateTime endTime;

    public Invoice(Id id, StatusHistory statusHistory, List<LineItem> lineItems, ZonedDateTime startTime, ZonedDateTime endTime) {
        this.id = id;
        this.lineItems = List.copyOf(lineItems);
        this.statusHistory = statusHistory;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Id id() {
        return id;
    }

    public String status() {
        return statusHistory.current();
    }

    public StatusHistory statusHistory() {
        return statusHistory;
    }

    public List<LineItem> lineItems() {
        return lineItems;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public ZonedDateTime getEndTime() {
        return endTime;
    }

    public BigDecimal sum() {
        return lineItems.stream().map(LineItem::amount).reduce(SCALED_ZERO, BigDecimal::add);
    }

    public static final class Id {
        private final String value;

        public static Id of(String value) {
            Objects.requireNonNull(value);
            return new Id(value);
        }

        public static Id generate() {
            var id = UUID.randomUUID().toString();
            return new Id(id);
        }

        private Id(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Id invoiceId = (Id) o;
            return value.equals(invoiceId.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return "InvoiceId{" +
                    "value='" + value + '\'' +
                    '}';
        }
    }

    /**
     * Represents a chargeable line on an invoice.
     */
    public static class LineItem {
        private final String id;
        private final String description;
        private final BigDecimal amount;
        private final String plan;
        private final String agent;
        private final ZonedDateTime addedAt;
        private final Optional<ZonedDateTime> startedAt;
        private final Optional<ZonedDateTime> endedAt;
        private final Optional<ApplicationId> applicationId;
        private final Optional<ZoneId> zoneId;

        public LineItem(String id, String description, BigDecimal amount, String plan, String agent, ZonedDateTime addedAt, ZonedDateTime startedAt, ZonedDateTime endedAt, ApplicationId applicationId, ZoneId zoneId) {
            this.id = id;
            this.description = description;
            this.amount = amount;
            this.plan = plan;
            this.agent = agent;
            this.addedAt = addedAt;
            this.startedAt = Optional.ofNullable(startedAt);
            this.endedAt = Optional.ofNullable(endedAt);

            if (applicationId == null && zoneId != null)
                throw new IllegalArgumentException("Must supply applicationId if zoneId is supplied");

            this.applicationId = Optional.ofNullable(applicationId);
            this.zoneId = Optional.ofNullable(zoneId);
        }

        public LineItem(String id, String description, BigDecimal amount, String plan, String agent, ZonedDateTime addedAt) {
            this(id, description, amount, plan, agent, addedAt, null, null, null, null);
        }

        /** The opaque ID of this */
        public String id() {
            return id;
        }

        /** The string description of this - used for display purposes */
        public String description() {
            return description;
        }

        /** The dollar amount of this */
        public BigDecimal amount() {
            return SCALED_ZERO.add(amount);
        }

        /** The plan used to calculate amount of this */
        public String plan() {
            return plan;
        }

        /** Who created this line item */
        public String agent() {
            return agent;
        }

        /** When was this line item added */
        public ZonedDateTime addedAt() {
            return addedAt;
        }

        /** What time period is this line item for - time start */
        public Optional<ZonedDateTime> startedAt() {
            return startedAt;
        }

        /** What time period is this line item for - time end */
        public Optional<ZonedDateTime> endedAt() {
            return endedAt;
        }

        /** Optionally - what application is this line item about */
        public Optional<ApplicationId> applicationId() {
            return applicationId;
        }

        /** Optionally - what zone deployment is this line item about */
        public Optional<ZoneId> zoneId() {
            return zoneId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LineItem lineItem = (LineItem) o;
            return id.equals(lineItem.id) &&
                    description.equals(lineItem.description) &&
                    amount.equals(lineItem.amount) &&
                    plan.equals(lineItem.plan) &&
                    agent.equals(lineItem.agent) &&
                    addedAt.equals(lineItem.addedAt) &&
                    startedAt.equals(lineItem.startedAt) &&
                    endedAt.equals(lineItem.endedAt) &&
                    applicationId.equals(lineItem.applicationId) &&
                    zoneId.equals(lineItem.zoneId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, description, amount, plan, agent, addedAt, startedAt, endedAt, applicationId, zoneId);
        }

        @Override
        public String toString() {
            return "LineItem{" +
                    "id='" + id + '\'' +
                    ", description='" + description + '\'' +
                    ", amount=" + amount +
                    ", plan='" + plan + '\'' +
                    ", agent='" + agent + '\'' +
                    ", addedAt=" + addedAt +
                    ", startedAt=" + startedAt +
                    ", endedAt=" + endedAt +
                    ", applicationId=" + applicationId +
                    ", zoneId=" + zoneId +
                    '}';
        }
    }

    public static class StatusHistory {
        SortedMap<ZonedDateTime, String> history;

        public StatusHistory(SortedMap<ZonedDateTime, String> history) {
            this.history = history;
        }

        public static StatusHistory open() {
            return new StatusHistory(
                    new TreeMap<>(Map.of(ZonedDateTime.now(), "OPEN"))
            );
        }

        public String current() {
            return history.get(history.lastKey());
        }

        public SortedMap<ZonedDateTime, String> getHistory() {
            return history;
        }

    }

}
