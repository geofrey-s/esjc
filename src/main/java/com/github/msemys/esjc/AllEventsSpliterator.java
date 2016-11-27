package com.github.msemys.esjc;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * $all stream events spliterator.
 */
public class AllEventsSpliterator extends AbstractEventsSpliterator<Position, AllEventsSlice> {

    AllEventsSpliterator(Position position, Function<Position, CompletableFuture<AllEventsSlice>> reader) {
        super(position, reader);
    }

    @Override
    protected Position getNextCursor(AllEventsSlice slice) {
        return slice.nextPosition;
    }

    @Override
    protected List<ResolvedEvent> getEvents(AllEventsSlice slice) {
        return slice.events;
    }

    @Override
    protected boolean isEndOfStream(AllEventsSlice slice) {
        return slice.isEndOfStream();
    }

}
