package com.github.msemys.esjc;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static com.github.msemys.esjc.matcher.RecordedEventListMatcher.containsInOrder;
import static java.util.stream.IntStream.range;
import static org.junit.Assert.*;

public class ITStreamEventsIterator extends AbstractEventStoreTest {

    public ITStreamEventsIterator(EventStore eventstore) {
        super(eventstore);
    }

    @Test
    public void lazyReadsBatchesForward() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        StreamEventsIteratorWithBatchCounter iterator = new StreamEventsIteratorWithBatchCounter(
            StreamPosition.START,
            i -> eventstore.readStreamEventsForward(stream, i, 3, false));

        assertEquals(0, iterator.batchCount);

        List<ResolvedEvent> result = new ArrayList<>();

        range(0, 18).forEach(i -> result.add(iterator.next()));

        assertEquals(18, result.size());
        assertEquals(6, iterator.batchCount);

        assertTrue(iterator.hasNext());
        assertEquals(7, iterator.batchCount);
        result.add(iterator.next());
        assertEquals(7, iterator.batchCount);

        assertTrue(iterator.hasNext());
        assertEquals(7, iterator.batchCount);
        result.add(iterator.next());
        assertEquals(7, iterator.batchCount);

        assertFalse(iterator.hasNext());
        assertEquals(7, iterator.batchCount);

        range(0, 50).forEach(i -> {
            assertFalse(iterator.hasNext());
            assertEquals(7, iterator.batchCount);
        });

        assertEquals(20, result.size());
        assertThat(recordedEventsFrom(result), containsInOrder(events));
    }

    @Test
    public void lazyReadsBatchesBackward() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        StreamEventsIteratorWithBatchCounter iterator = new StreamEventsIteratorWithBatchCounter(
            19,
            i -> eventstore.readStreamEventsBackward(stream, i, 3, false));

        assertEquals(0, iterator.batchCount);

        List<ResolvedEvent> result = new ArrayList<>();

        range(0, 9).forEach(i -> result.add(iterator.next()));

        assertEquals(9, result.size());
        assertEquals(3, iterator.batchCount);

        assertTrue(iterator.hasNext());
        assertEquals(4, iterator.batchCount);

        range(0, 3).forEach(i -> result.add(iterator.next()));
        assertEquals(4, iterator.batchCount);

        range(0, 3).forEach(i -> result.add(iterator.next()));
        assertEquals(5, iterator.batchCount);

        range(0, 3).forEach(i -> result.add(iterator.next()));
        assertEquals(6, iterator.batchCount);

        result.add(iterator.next());
        assertEquals(7, iterator.batchCount);

        result.add(iterator.next());
        assertEquals(7, iterator.batchCount);

        assertFalse(iterator.hasNext());
        assertEquals(7, iterator.batchCount);

        range(0, 50).forEach(i -> {
            assertFalse(iterator.hasNext());
            assertEquals(7, iterator.batchCount);
        });

        assertEquals(20, result.size());
        assertThat(recordedEventsFrom(result), containsInOrder(reverse(events)));
    }

    private static class StreamEventsIteratorWithBatchCounter extends StreamEventsIterator {
        int batchCount;

        StreamEventsIteratorWithBatchCounter(long eventNumber, Function<Long, CompletableFuture<StreamEventsSlice>> reader) {
            super(eventNumber, reader);
        }

        @Override
        protected void onBatchReceived(StreamEventsSlice slice) {
            batchCount++;
            super.onBatchReceived(slice);
        }
    }

}
