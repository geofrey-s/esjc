package com.github.msemys.esjc;

import com.github.msemys.esjc.system.SystemEventType;
import org.junit.Test;

import java.util.List;

import static com.github.msemys.esjc.matcher.RecordedEventListMatcher.containsInOrder;
import static com.github.msemys.esjc.system.SystemStreams.metastreamOf;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

public class ITReadAllEventsForwardWithSoftDeletedStream extends AbstractIntegrationTest {

    private static final Long DELETED_STREAM_EVENT_NUMBER = Long.MAX_VALUE;

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test
    public void ensuresDeletedStream() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvents(20)).join();
        eventstore.deleteStream(stream, ExpectedVersion.ANY).join();

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 0, 100, false).join();

        assertEquals(SliceReadStatus.StreamNotFound, slice.status);
        assertTrue(slice.events.isEmpty());
    }

    @Test
    public void returnsAllEventsIncludingTombstone() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        Position position = eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events.get(0)).join().logPosition;
        eventstore.appendToStream(stream, ExpectedVersion.of(0), events.stream().skip(1).collect(toList())).join();
        eventstore.deleteStream(stream, ExpectedVersion.ANY).join();

        AllEventsSlice slice = eventstore.readAllEventsForward(position, events.size() + 10, false).join();

        assertThat(slice.events.stream().limit(events.size()).map(e -> e.event).collect(toList()), containsInOrder(events));

        RecordedEvent lastEvent = slice.events.get(slice.events.size() - 1).event;
        assertEquals(metastreamOf(stream), lastEvent.eventStreamId);
        assertEquals(SystemEventType.STREAM_METADATA.value, lastEvent.eventType);

        StreamMetadata metadata = StreamMetadata.fromJson(lastEvent.data);
        assertEquals(DELETED_STREAM_EVENT_NUMBER, metadata.truncateBefore);
    }

}
