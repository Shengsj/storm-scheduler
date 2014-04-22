package ch.uzh.ddis.stools.scheduler;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.gson.Gson;

import java.util.*;

/**
 * This class collects the send graph data for the sankey charts.
 * <p/>
 * As all methods in this class are synchronized, this class is <b>thread-safe</b> and instances of it can be
 * accessed from multiple threads concurrently.
 *
 * @author Thomas Hunziker
 * @author Lorenz Fischer
 */
class Sendgraph {

    /**
     * We store the id of all node that are either in a target or a source relationship with another node.
     */
    private Set<Integer> nodeIds = new HashSet<>();

    /**
     * Table&lt;fromNode, toNode, Counter&gt;
     */
    private Table<Integer, Integer, Long> graph = HashBasedTable.create();

    /**
     * Sets the weight for an edge to the given value.
     *
     * @param fromNode the id of the node that send the messages.
     * @param toNode   the id of the receiving node in the graph.
     * @param weight   the weight of the edge between the nodes.
     */
    public synchronized void setEdgeWeight(Integer fromNode, Integer toNode, long weight) {
        this.nodeIds.add(fromNode);
        this.nodeIds.add(toNode);

        this.graph.put(fromNode, toNode, weight);
    }


    /**
     * Creates and returns a Json representation of the graph in the form as it is expected by the Sankey hmtl file
     * used to visualize the graphs.
     *
     * @return the json representation.
     */
    public synchronized String toJson() {
        Map<String, Object> data;
        List<Map<String, String>> nodes;
        List<Map<String, Object>> links;
        Gson gson;
        String result;

        data = new HashMap<>();

        nodes = new ArrayList<>();
        for (Integer nodeId : nodeIds) {
            Map<String, String> name = new HashMap<>();
            name.put("name", nodeId.toString());
            nodes.add(name);
        }
        data.put("nodes", nodes);

        links = new ArrayList<>();
        for (Integer fromNode : this.graph.rowKeySet()) {
            for (Integer toNode : this.graph.columnKeySet()) {
                if (this.graph.contains(fromNode, toNode)) {
                    Map<String, Object> values = new HashMap<String, Object>();
                    values.put("source", fromNode);
                    values.put("target", toNode);
                    values.put("value", this.graph.get(fromNode, toNode));
                    links.add(values);
                }
            }
        }
        data.put("graph", links);

        gson = new Gson();
        result = gson.toJson(data);
        return result;
    }

}
