package net.openhft.chronicle.queue.impl.single.preroucher;

import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.RollCycles;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.wire.AbstractMarshallable;
import net.openhft.chronicle.wire.MessageHistory;

import java.util.UUID;

import static net.openhft.chronicle.queue.impl.single.preroucher.ValidFields.validateAll;

/**
 * Created by Rob Austin
 */
public class PretoucherSoakTest {

    public static void main(String[] args) {
        SingleChronicleQueue outQueue = SingleChronicleQueueBuilder.binary("target/" + "monitor")
                .rollCycle(RollCycles.TEST_SECONDLY).build();
        ExcerptAppender outQueueAppender = outQueue.acquireAppender();

        HeartbeatListener heartbeatWriter = outQueueAppender.methodWriterBuilder(HeartbeatListener.class).methodWriterListener((m, a) -> validateAll(a)).recordHistory(true).build();

        Monitor.addPeriodicUpdateSource(10, () -> currentTimeMillis -> {
            outQueueAppender.pretouch();
        });

        long lastHB = 0;
        while (true) {
            if (System.currentTimeMillis() - lastHB > 1) {
                // write a hb to the queue
                MessageHistory.get().reset();
                Heartbeat heartBeat = new Heartbeat(UUID.randomUUID().toString());
                heartbeatWriter.heartbeat(heartBeat);
                lastHB = System.currentTimeMillis();
            }
        }
    }

    public interface HeartbeatListener {

        /**
         * called periodically under normal operation
         *
         * @param heartbeat
         */
        void heartbeat(Heartbeat heartbeat);

    }

    public static class Heartbeat extends AbstractMarshallable implements Validatable {
        final String source;
        long time;

        // TODO: should make sure this source is the same as Runner.name
        public Heartbeat(String source) {
            this.source = source;
        }

        public String source() {
            return source;
        }

        public long time() {
            return time;
        }

        public Heartbeat time(long time) {
            this.time = time;
            return this;
        }

        @Override
        public void validate() throws IllegalStateException {

        }
    }
}