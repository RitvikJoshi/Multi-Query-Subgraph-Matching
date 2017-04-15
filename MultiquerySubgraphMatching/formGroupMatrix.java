/**
 * Created by adityapulekar on 4/13/17.
 */

import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.File;
import java.util.*;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.unsafe.batchinsert.*;

public class formGroupMatrix {
    Map<String, Map<Long, Set<Long>>> neigborsForNodes = new HashMap<String, Map<Long, Set<Long>>>();
    Map<String, Map<Node, Set<Node>>> immediateNeighbors = new LinkedHashMap<String, Map<Node, Set<Node>>>();
    Map<String, List<List<Label>>> TLS_map = new LinkedHashMap<String, List<List<Label>>>();
    Map<String, List<List<Node>>> TLS_map_usingNodeIds = new LinkedHashMap<String, List<List<Node>>>();
    List<List<Label>> TLS_sequencesInLabels = new LinkedList<List<Label>>();
    Map<String, Map<String, List<List<Label>>>> TLS_QueryPairs = new LinkedHashMap<String, Map<String, List<List<Label>>>>();
    Map<String, Map<String, Float>> groupMatrix = new LinkedHashMap<String, Map<String, Float>>();
    Map<String, GraphDatabaseService> queryToDBServiceMapping = new LinkedHashMap<String, GraphDatabaseService>();

    public void parse_testQueries(File[] files) {
        String lineRead;
        BufferedReader formatBufferedReader;
        BatchInserter bInserter = null;
        for (File f : files) {
            if (f.isFile() && f.getName().split("\\.")[1].equals("txt")) {
                try {
                    File newDBFile = new File("/Users/adityapulekar/Documents/Neo4j/GraphDB_proj/" + f.getName().split("\\.")[0]);
                    neigborsForNodes.put(f.getName(), new HashMap<Long, Set<Long>>());
                    bInserter = BatchInserters.inserter(newDBFile);

                    formatBufferedReader = new BufferedReader(new FileReader(f));
                    int totalNumberOfNodes = Integer.parseInt(formatBufferedReader.readLine());
                    int count = 0;

                    while ((lineRead = formatBufferedReader.readLine()) != null) {
                        String[] splitLine = lineRead.split(" ");
                        if (count < totalNumberOfNodes) {
                            // Identifying all the nodes mentioned in the
                            // file.
                            bInserter.createNode((long) Integer.parseInt(splitLine[0]), new HashMap<String, Object>(),
                                    Label.label(splitLine[1]));

                            count++;
                        } else {
                            // drawing relationships between the nodes.
                            if (lineRead.split(" ").length == 2) {
                                if (neigborsForNodes.get(f.getName()).containsKey((long) Integer.parseInt(splitLine[0]))) {
                                    neigborsForNodes.get(f.getName()).get((long) Integer.parseInt(splitLine[0])).add((long) Integer.parseInt(splitLine[1]));
                                } else {
                                    neigborsForNodes.get(f.getName()).put((long) Integer.parseInt(splitLine[0]), new HashSet<Long>(Arrays.asList((long) Integer.parseInt(splitLine[1]))));
                                }
                                bInserter.createRelationship((long) Integer.parseInt(splitLine[0]),
                                        (long) Integer.parseInt(splitLine[1]), oneRelationship.noRelType,
                                        new HashMap<String, Object>());
                            }
                        }


                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                bInserter.shutdown();
            }

        }

    }

    public boolean compareLabelValue(Node firstNodeOfSeq, Node lastNodeInSeq) {
        Iterable<Label> labels = firstNodeOfSeq.getLabels();
        Iterable<Label> degreeTwoNodeLabels = lastNodeInSeq.getLabels();
        for (Label l_first : labels) {
            for (Label l_last : degreeTwoNodeLabels) {
                int result = l_first.toString().compareTo(l_last.toString());
                if (result > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    //Use this method only in case of the query graphs since only the query graphs will for sure have nodes with exactly one Label.
    public Label getLabel(Node nodes) {
        Iterable<Label> labels = nodes.getLabels();
        //Since each node in the query graph will have a single Label.
        for (Label l : labels) {
            return l;
        }
        return null;
    }

    public List<List<Node>> createTheSequence(Node firstNodeInSeq, Node middleNodeOfSeq, Map<Node, Set<Node>> TLS_wrtQuery) {
        Set<Node> neighborsOfMiddleNode = TLS_wrtQuery.get(middleNodeOfSeq);
        List<List<Node>> toBeReturned = new LinkedList<List<Node>>();

        //Note that if there are less than three vertices (i.e. a third vertex is not chosen),
        // then the "toBeReturned" list will remain empty.
        for (Node lastNodeInSeq : neighborsOfMiddleNode) {
            if (compareLabelValue(firstNodeInSeq, lastNodeInSeq) && firstNodeInSeq.getId() != lastNodeInSeq.getId()) {
                toBeReturned.add(new LinkedList<Node>() {{
                    add(firstNodeInSeq);
                    add(middleNodeOfSeq);
                    add(lastNodeInSeq);
                }});

                //Note: We are assuming here that the getLabel() method will always return a Label.
                TLS_sequencesInLabels.add(new LinkedList<Label>() {{
                    add(getLabel(firstNodeInSeq));
                    add(getLabel(middleNodeOfSeq));
                    add(getLabel(lastNodeInSeq));
                }});
            }
        }
        return toBeReturned;
    }


    //This method finally fetches us the Tri-vertex Label sequences.
    public List<List<Node>> checkTheThirdVertex(Map<Node, Set<Node>> TLS_wrtQuery) {
        Iterator<Map.Entry<Node, Set<Node>>> itr = TLS_wrtQuery.entrySet().iterator();
        List<List<Node>> TLS = new LinkedList<List<Node>>();
        while (itr.hasNext()) {
            Map.Entry pairs = itr.next();
            Set<Node> firstDegreeNeigbors = (Set<Node>) pairs.getValue();
            for (Node degreeOneNeighbor : firstDegreeNeigbors) {
                List<List<Node>> listOfSeq = createTheSequence((Node) pairs.getKey(), degreeOneNeighbor, TLS_wrtQuery);
                if (!listOfSeq.isEmpty()) {
                    TLS.addAll(listOfSeq);
                }
            }
        }
        //Note: TLS will have all the sequences in terms of Nodes. We are going to maintain another list which will have
        //      all the sequences in terms of the corresponding label values.
        return TLS;
    }


    public void formIntersection_TLSseqs_BetweenQueries() {
        int numberOfQueries = TLS_map.size();
        int count = 1;
        while (count <= numberOfQueries) {
            Iterator itr = TLS_map.entrySet().iterator();
            TLS_QueryPairs.put("q" + String.valueOf(count), new LinkedHashMap<String, List<List<Label>>>());
            while (itr.hasNext()) {
                List<List<Label>> tls_seqs = TLS_map.get("q" + String.valueOf(count));

                Map.Entry pairs = (Map.Entry) itr.next();
                String queryName = (String) pairs.getKey();
                if (Integer.parseInt(queryName.split("q")[1]) > count) {
                    //System.out.println("Retrieved sequences: " + tls_seqs);
                    List<List<Label>> tls_seqs_copy = new LinkedList(tls_seqs);
                    //System.out.println("Query " + "q"+String.valueOf(count)+ " being compared to Query " + queryName );
                    List<List<Label>> tls_seqs_toCompare = (List<List<Label>>) pairs.getValue();
                    tls_seqs_copy.retainAll(tls_seqs_toCompare);
                    //System.out.println("Comparison output--> " + tls_seqs_copy + "\n");
                    TLS_QueryPairs.get("q" + String.valueOf(count)).put(queryName, tls_seqs_copy);
                }
            }
            count++;
        }

    }


    public void generateTLS() {
        String databaseDir = "/Users/adityapulekar/Documents/Neo4j/GraphDB_proj/";
        File[] queries = new File("/Users/adityapulekar/Documents/Neo4j/GraphDB_proj/").listFiles();
        GraphDatabaseFactory graphInstance = new GraphDatabaseFactory();
        for (File queryFile : queries) {
            if (queryFile.isDirectory() && !queryFile.getName().equals(".DS_Store")) {
                GraphDatabaseService database = graphInstance.newEmbeddedDatabase(queryFile);
                String queryFileName = queryFile.getName().split("\\.")[0];
                queryToDBServiceMapping.put(queryFileName, database);
                TLS_sequencesInLabels = new LinkedList<List<Label>>();
                try (Transaction transac = database.beginTx()) {
                    ResourceIterable<Node> allNodes = database.getAllNodes();
                    immediateNeighbors.put(queryFileName, new LinkedHashMap<Node, Set<Node>>());
                    Map<Node, Set<Node>> TLS_wrtQuery = immediateNeighbors.get(queryFileName);

                    //Here we fetch all the first degree neighbors of every node from the query graph.
                    for (Node nodes : allNodes) {
                        Iterable<Relationship> outRelationships = nodes.getRelationships(Direction.OUTGOING);
                        TLS_wrtQuery.put(nodes, new HashSet<Node>());
                        for (Relationship rel : outRelationships) {
                            TLS_wrtQuery.get(nodes).add(rel.getEndNode());
                        }
                    }
                    List<List<Node>> TLS_sequencesInNodes = checkTheThirdVertex(TLS_wrtQuery);
                    TLS_map_usingNodeIds.put(queryFileName, TLS_sequencesInNodes);
                    TLS_map.put(queryFileName, TLS_sequencesInLabels);

                    transac.success();

                }
            }
        }
    }

    //This method = LI(qi,TLS(qi,qj)).
    public void validateAgainstQi(String qi, List<List<Label>> qj_set, List<Node> solutionSet_Qi) {
        GraphDatabaseService DB = queryToDBServiceMapping.get(qi);
        try (Transaction tx = DB.beginTx()) {
            List<List<Node>> sequences_InNodes = TLS_map_usingNodeIds.get(qi);
            for (List<Label> sequences_L : qj_set) {
                for (List<Node> sequences_N : sequences_InNodes) {
                    if (checkForThisTriplet(sequences_L, sequences_N)) {
                        if (solutionSet_Qi.isEmpty()) {
                            solutionSet_Qi = new LinkedList<Node>() {{
                                add(sequences_N.get(0));
                                add(sequences_N.get(1));
                                add(sequences_N.get(2));
                            }};
                        } else {
                            if (sequencesN_NOT_DisjointFromSolutionSet(solutionSet_Qi, sequences_N)) { //i.e. solutionSet and sequences_N has one or two common nodes.
                                for (Node n : sequences_N) {
                                    if (!solutionSet_Qi.contains(n)) {
                                        solutionSet_Qi.add(n);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            tx.success();
        }
    }

    //Assuming every node can have a single label at the max.
    public boolean compareLabels(Label labelName, Iterable<Label> labels) {
        for (Label l : labels) {
            if (labelName.toString().equals(l.toString())) {
                return true;
            }
        }
        return false;
    }


    public boolean checkForThisTriplet(List<Label> sequences_L, List<Node> sequences_N) {
        Node first = sequences_N.get(0);
        Node middle = sequences_N.get(1);
        Node last = sequences_N.get(2);
        boolean res_first = compareLabels(sequences_L.get(0), first.getLabels());
        boolean res_middle = compareLabels(sequences_L.get(1), middle.getLabels());
        boolean res_last = compareLabels(sequences_L.get(2), last.getLabels());
        if (res_first && res_middle && res_last) {  //This means that given sequence of labels was found in the given list of nodes.
            return true;
        } else {
            return false;
        }
    }


    public boolean sequencesN_NOT_DisjointFromSolutionSet(List<Node> solutionSet_Qj, List<Node> sequences_N) {
        List<Node> solutionSet_Qj_copy = solutionSet_Qj;
        List<Node> sequences_N_copy = sequences_N;
        solutionSet_Qj_copy.retainAll(sequences_N_copy);
        if (solutionSet_Qj_copy.size() == 0) {
            return false;  //This means we don't want the "sequences_N" triplet to be added to the solutionSet.
        } else {
            return true; // This means the we want the "sequences_N" triplet to be added to the solutionSet.
        }
    }

    //Note that we are acting upon a single instance of "solutionSet" in this method.
    //Also, note that we have not covered the case if we were to start with an outcast TLS (like 'D-C-E') in the two "Validate" methods.

    //This method = LI(qj,TLS(qi,qj)).
    public void validateAgainstQj(String qj, List<List<Label>> qj_set, List<Node> solutionSet_Qj) {
        //while(solutionSet_Qj.size() <= 1) { //Since we don't want a subgraph with 2 or less edges. A single TLS has two edges.
        GraphDatabaseService DB = queryToDBServiceMapping.get(qj);
        try (Transaction tx = DB.beginTx()) {

            List<List<Node>> sequences_InNodes = TLS_map_usingNodeIds.get(qj);
            for (List<Label> sequences_L : qj_set) {
                for (List<Node> sequences_N : sequences_InNodes) {
                    if (checkForThisTriplet(sequences_L, sequences_N)) {
                        if (solutionSet_Qj.isEmpty()) {
                            solutionSet_Qj = new LinkedList<Node>() {{
                                add(sequences_N.get(0));
                                add(sequences_N.get(1));
                                add(sequences_N.get(2));
                            }};
                        } else {
                            if (sequencesN_NOT_DisjointFromSolutionSet(solutionSet_Qj, sequences_N)) { //i.e. solutionSet and sequences_N has one or two common nodes.
                                for (Node n : sequences_N) {
                                    if (!solutionSet_Qj.contains(n)) {
                                        solutionSet_Qj.add(n);
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }
            /*if(solutionSet_Qj.size() == 1){*/
            /*    qj_set.remove(solutionSet_Qj.get(0));*/
            /*    solutionSet_Qj.clear();*/
            /*}*/
            //}
            tx.success();
        }
    }

    public int checkWhichTLSInstancesFormLargestSubgraph(String qi, String qj, List<List<Label>> qj_set) {
        List<Node> solutionSet_Qi = new LinkedList<Node>();
        List<Node> solutionSet_Qj = new LinkedList<Node>();
        validateAgainstQi(qi, qj_set, solutionSet_Qi);
        validateAgainstQj(qj, qj_set, solutionSet_Qj);

        // So, we now have LI(qi,TLS(qi,qj)) and LI(qj,TLS(qi,qj)) for all qi and qj in the form of the Solution Set size.
        return Math.min(solutionSet_Qi.size(), solutionSet_Qj.size());
    }


    public void createGroupMatrix() {
        Iterator itr = TLS_QueryPairs.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry pairs = (Map.Entry) itr.next();
            Map<String, List<List<Label>>> qj = (Map<String, List<List<Label>>>) pairs.getValue();
            Iterator itr_qj = qj.entrySet().iterator();

            while (itr_qj.hasNext()) {
                Map.Entry pairs_qj = (Map.Entry) itr_qj.next();
                //I create a new instance here just to avoid editing the values in the map 'qj'.
                List<List<Label>> qj_set = new LinkedList<List<Label>>((List<List<Label>>) pairs_qj.getValue());
                int minimumLIValue = checkWhichTLSInstancesFormLargestSubgraph((String) pairs.getKey(), (String) pairs_qj.getKey(), qj_set);
                int minTLSSizeForQueries = Math.min(TLS_map.get((String) pairs.getKey()).size(), TLS_map.get((String) pairs_qj.getKey()).size());
                Float GF_value = Float.valueOf(minimumLIValue / minTLSSizeForQueries);
                groupMatrix.put((String) pairs.getKey(), new LinkedHashMap<String, Float>() {{
                    put((String) pairs_qj.getKey(), GF_value);
                }});
            }
        }

    }

    public void printTLS_Map_usingNodeIDs() {
        Iterator itr = TLS_map_usingNodeIds.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry pairs = (Map.Entry) itr.next();
            System.out.println(pairs.getKey() + " = " + pairs.getValue());
        }
    }


    public void printTLS_Map() {
        Iterator itr = TLS_map.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry pairs = (Map.Entry) itr.next();
            System.out.println(pairs.getKey() + " = " + pairs.getValue());
        }

    }

    public void printQueryPairs() {
        Iterator itr = TLS_QueryPairs.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry pairs = (Map.Entry) itr.next();
            System.out.println("Key: " + pairs.getKey());
            System.out.println("Value: " + pairs.getValue());
            System.out.println("\n");
        }
    }

    public static void main(String[] args) {
        formGroupMatrix grouping = new formGroupMatrix();
        String currentDirectory = System.getProperty("user.dir");
        File[] allFiles = new File(currentDirectory).listFiles();
        //grouping.parse_testQueries(allFiles);
        grouping.generateTLS();

        System.out.println("TLS_Map:-\n");
        grouping.printTLS_Map();
        System.out.println("\n\n");

        System.out.println("TLS_Map using NodeIDs:-\n");
        grouping.printTLS_Map_usingNodeIDs();
        System.out.println("\n\n");

        grouping.formIntersection_TLSseqs_BetweenQueries();
        grouping.printQueryPairs();

        //Forms the group matrix.
        grouping.createGroupMatrix();
        System.out.println("\n\nGroup Matrix: \n" + grouping.groupMatrix);
    }
}

