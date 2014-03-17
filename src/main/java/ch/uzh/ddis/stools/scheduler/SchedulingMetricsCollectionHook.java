/* TODO: License */
package ch.uzh.ddis.stools.scheduler;

import backtype.storm.hooks.ITaskHook;
import backtype.storm.hooks.info.*;
import backtype.storm.metric.api.IMetric;
import backtype.storm.task.TopologyContext;
import backtype.storm.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This hook will install metrics to collect statistics on the sending behavior of the topology this
 * hook is configured for.
 * <p/>
 * You can configure it in the storm.yaml file as:
 * <pre>
 *   topology.auto.task.hooks:
 *     - class: "ch.uzh.ddis.stools.scheduler.SchedulingMetricsCollectionHook"
 * </pre>
 *
 * @author "Lorenz Fischer" <lfischer@ifi.uzh.ch>
 */
public class SchedulingMetricsCollectionHook implements ITaskHook {

    /**
     * The name for the metric that measures all emitted messages.
     */
    public static final String METRIC_EMITTED_MESSAGES = "emitted_messages";

    /**
     * The intervalSecs in which the metrics (the sendgraph) will be collected.
     */
    public static final int DEFAULT_INTERVAL_SECS = 10;

    /**
     * The intervalSecs configured in this property will be used for counting (and resetting) the metrics collected
     * by this hook. If this is not set in the storm properties the default value of {@value #DEFAULT_INTERVAL_SECS}
     * will be used.
     */
    public static final String CONF_SCHEDULING_METRICS_INTERVAL = "scheduling.metrics.interval.secs";

    private final static Logger LOG = LoggerFactory.getLogger(SchedulingMetricsCollectionHook.class);

    /**
     * A reference to the send graph of the task this hook is attached to.
     */
    private final AtomicReference<Map<Integer, AtomicLong>> sendgraphRef = new AtomicReference<>();

    /**
     * Create an empty sendgraph map which will return 0L values for each non-existing entry. The semantics
     * of the entries in this map are 'how many messages have been sent from the task this hook is attached to
     * to other tasks in the storm task graph'. The source task id is therefore implicit.
     */
    private static Map<Integer, AtomicLong> createEmptySendgraphMap() {
        return new HashMap<Integer, AtomicLong>() {
            @Override
            public AtomicLong get(Object key) {
                AtomicLong result = super.get(key);
                if (result == null) {
                    result = new AtomicLong();
                    put((Integer) key, result);
                }
                return result;
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<Integer, AtomicLong> entry : entrySet()) {
                    if (sb.length() > 0) {
                        sb.append(" ");
                    }
                    sb.append(entry.getKey()).append(":").append(entry.getValue().get());
                }
                return sb.toString();
            }
        };
    }

    @Override
    public void prepare(Map conf, final TopologyContext context) {
        int intervalSecs;

        LOG.debug("Initializing metrics hook for task {}", context.getThisTaskId());

        intervalSecs = DEFAULT_INTERVAL_SECS;
        if (conf.containsKey(CONF_SCHEDULING_METRICS_INTERVAL)) {
            intervalSecs = Utils.getInt(conf.get(CONF_SCHEDULING_METRICS_INTERVAL)).intValue();
        }

        /*
         * We register one metric for each task. The full send graph will then be built up in the metric
         * consumer.
         */
        context.registerMetric(METRIC_EMITTED_MESSAGES, new IMetric() {
            @Override
            public Object getValueAndReset() {
                Map<Integer, AtomicLong> currentValue;

                currentValue = SchedulingMetricsCollectionHook.this.sendgraphRef.getAndSet(createEmptySendgraphMap());
                LOG.trace("Reset values for task {} and returning: {}", context.getThisTaskId(), currentValue.toString());

                return currentValue;
            }

        }, intervalSecs); // call every n seconds

        // put an empty send graph object.
        this.sendgraphRef.compareAndSet(null, createEmptySendgraphMap());
    }

    @Override
    public void cleanup() {

    }

    @Override
    public void emit(EmitInfo info) {
        for (Integer outTaskId : info.outTasks) {
            this.sendgraphRef.get().get(outTaskId).incrementAndGet();
        }
    }

    @Override
    public void spoutAck(SpoutAckInfo info) {

    }

    @Override
    public void spoutFail(SpoutFailInfo info) {

    }

    @Override
    public void boltExecute(BoltExecuteInfo info) {

    }

    @Override
    public void boltAck(BoltAckInfo info) {

    }

    @Override
    public void boltFail(BoltFailInfo info) {

    }

}