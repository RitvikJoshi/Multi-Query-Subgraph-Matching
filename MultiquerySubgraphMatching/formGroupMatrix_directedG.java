import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.io.fs.DefaultFileSystemAbstraction;
import org.neo4j.kernel.internal.StoreLocker;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by adityapulekar on 4/27/17.
 */

//NOTE THAT IN IGRAPH, NODES WILL HAVE MULTIPLE LABELS.

public class formGroupMatrix_directedG {
    Map<String, Map<Long, Set<Long>>> neigborsForNodes = new HashMap<String, Map<Long, Set<Long>>>();
    Map<String, Map<Node, Set<Node>>> immediateNeighbors = new LinkedHashMap<String, Map<Node, Set<Node>>>();
    Map<String, List<List<Label>>> TLS_map = new LinkedHashMap<String, List<List<Label>>>();
    Map<String, List<List<Node>>> TLS_map_usingNodeIds = new LinkedHashMap<String, List<List<Node>>>();
    List<List<Label>> TLS_sequencesInLabels = new LinkedList<List<Label>>();
    Map<String, Map<String, List<List<Label>>>> TLS_QueryPairs = new LinkedHashMap<String, Map<String, List<List<Label>>>>();
    Map<String, Map<String, Float>> groupMatrix = new LinkedHashMap<String, Map<String, Float>>();
    Map<String, GraphDatabaseService> queryToDBServiceMapping = new LinkedHashMap<String, GraphDatabaseService>();


    public void parse_queries_IGraph(List<File> queryFiles) {
        String lineRead;
        BufferedReader formatBufferedReader;
        BatchInserter bInserter = null;
        for (File f : queryFiles) {
            if (f.isFile()) {
                try {
                    formatBufferedReader = new BufferedReader(new FileReader(f));
                    lineRead = formatBufferedReader.readLine();
                    String targetGraphName = f.getName().split("_")[0];
                    while (lineRead != null) {
                        String[] graphNum = lineRead.split(" ");
                        if (graphNum[0].equals("t")) {
                            // Create a new graph database for every target or
                            // query
                            File newDBFile = new File("/Users/adityapulekar/Documents/Neo4j/GraphDB_proj/" + targetGraphName
                                    + ".query" + Integer.parseInt(graphNum[2]));
                            bInserter = BatchInserters.inserter(newDBFile);
                        }

                        while ((lineRead = formatBufferedReader.readLine()) != null
                                && !lineRead.split(" ")[0].equals("t")) {
                            String[] splitLine = lineRead.split(" ");
                            if (splitLine[0].equals("v")) {
                                int numberOfLabels = splitLine.length - 2;
                                Label[] allLabels = new Label[numberOfLabels];
                                int itr = 0;
                                for (int index = 2; index < splitLine.length; index++) {
                                    allLabels[itr] = Label.label("IGraph_" + splitLine[index]);
                                    itr++;
                                }
                                bInserter.createNode((long) Integer.parseInt(splitLine[1]),
                                        new HashMap<String, Object>(), allLabels);
                            } else if (splitLine[0].equals("e")) {
                                bInserter.createRelationship((long) Integer.parseInt(splitLine[1]),
                                        (long) Integer.parseInt(splitLine[2]), onlyRelationship_iGraph.ZERO,
                                        new HashMap<String, Object>());
                            }

                        }
                        bInserter.shutdown();
                    }

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        System.out.println("All IGraph query graphs successfully loaded!");
    }

    public void forIGraph(String pathforIGraph) {
        File[] files = new File(pathforIGraph).listFiles();
        List<File> targetFiles = new ArrayList<File>();
        List<File> queryFiles = new ArrayList<File>();
        for (File f : files) {
            if (f.getName().contains("_q10.igraph")) {
                // query graph
                queryFiles.add(f);
            } else if (f.getName().contains(".igraph")) {
                // target graph
                targetFiles.add(f);
            }
        }
        parse_queries_IGraph(queryFiles);
    }

    //In case of IGraphs, the integers are compared on the basis of the trailing integer values in the label name.
    public boolean compareLabelValue(Node firstNodeOfSeq, Node lastNodeInSeq) {
        Iterable<Label> labels = firstNodeOfSeq.getLabels();
        Iterable<Label> degreeTwoNodeLabels = lastNodeInSeq.getLabels();

        //IN CASE OF MULTIPLE LABELS, ALL THE LABELS OF THE FIRST NODE SHOULD HAVE A VALUES LESSER THAN OR EQUAL TO THE LAST NODE.
        //(VERIFY THE ABOVE STATEMENT).
        for (Label l_first : labels) {
            for (Label l_last : degreeTwoNodeLabels) {
                if(Integer.parseInt(l_first.toString().split("_")[1]) > Integer.parseInt(l_last.toString().split("_")[1])){
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

    public boolean CheckTLS_Labels_ForSequence(List<Label> listOfSeq, List<List<Label>> TLS){
        for(List<Label> l : TLS){
            if(l.contains(listOfSeq)){
                return true;
            }
        }
        return false;
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
                List<Label> newSeq = new LinkedList<Label>() {{
                    add(getLabel(firstNodeInSeq));
                    add(getLabel(middleNodeOfSeq));
                    add(getLabel(lastNodeInSeq));
                }};
                if(!TLS_sequencesInLabels.contains(newSeq)){
                    TLS_sequencesInLabels.add(newSeq);
                }
                /*if(!CheckTLS_Labels_ForSequence(newSeq,TLS_sequencesInLabels) ){

                }*/
            }
        }
        return toBeReturned;
    }

    public boolean CheckTLS_NodesForSequence(List<List<Node>> listOfSeq, List<List<Node>> TLS){
        for(List<Node> l : listOfSeq){
            if(TLS.contains(l)){
                return true;
            }
        }
        return false;
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
                if (!listOfSeq.isEmpty() && !CheckTLS_NodesForSequence(listOfSeq,TLS)) {
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
        int count = 0;
        while (count < numberOfQueries) {
            Iterator itr = TLS_map.entrySet().iterator();
            TLS_QueryPairs.put("q" + String.valueOf(count), new LinkedHashMap<String, List<List<Label>>>());
            while (itr.hasNext()) {
                List<List<Label>> tls_seqs = TLS_map.get("q" + String.valueOf(count));

                Map.Entry pairs = (Map.Entry) itr.next();
                String queryName = (String) pairs.getKey();
                if (Integer.parseInt(queryName.split("q")[1]) > count) {
                    //System.out.println("Retrieved sequences: " + tls_seqs);
                    List<List<Label>> tls_seqs_copy = new LinkedList(tls_seqs);
                    System.out.println(tls_seqs);
                    System.out.println(" vs " );
                    //System.out.println("Query " + "q"+String.valueOf(count)+ " being compared to Query " + queryName );
                    List<List<Label>> tls_seqs_toCompare = (List<List<Label>>) pairs.getValue();
                    System.out.println(tls_seqs_toCompare + "\n");
                    tls_seqs_copy.retainAll(tls_seqs_toCompare);
                    //System.out.println("Comparison output--> " + tls_seqs_copy + "\n");
                    TLS_QueryPairs.get("q" + String.valueOf(count)).put(queryName, tls_seqs_copy);
                }
            }
            count++;
        }

    }


    public void generateTLS() {
        //String databaseDir = "/Users/adityapulekar/Documents/Neo4j/GraphDB_proj/";
        File[] queries = new File("/Users/adityapulekar/Documents/Neo4j/GraphDB_proj/yeastQueries_test/").listFiles();
        GraphDatabaseFactory graphInstance = new GraphDatabaseFactory();
        //String[] testQueries = {"q1","q2","q3","q4","q5"};
        for (File queryFile : queries) {
            if (queryFile.isDirectory()) {    // !queryFile.getName().equals(".DS_Store") && !Arrays.asList(testQueries).contains(queryFile.getName())
                GraphDatabaseService database = releaseLockANDReturnService(queryFile);

                //BELOW LINE EDITED.
                String queryFileName = "q" + queryFile.getName().split("\\.")[1].split("query")[1];
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
    public int validateAgainstQi(String qi, List<List<Label>> qj_set, List<Node> solutionSet_Qi) {
        GraphDatabaseService DB = queryToDBServiceMapping.get(qi);
        int numberOfInstancesInQi = 0;
        try (Transaction tx = DB.beginTx()) {
            List<List<Node>> sequences_InNodes = TLS_map_usingNodeIds.get(qi);
            for (List<Label> sequences_L : qj_set) {
                for (List<Node> sequences_N : sequences_InNodes) {
                    if (checkForThisTriplet(sequences_L, sequences_N)) {
                        //System.out.println("sequences_L--> " + sequences_L);
                        //System.out.println("sequences_N--> " + sequences_N);

                        if (solutionSet_Qi.isEmpty()) {
                            solutionSet_Qi.add(sequences_N.get(0));
                            solutionSet_Qi.add(sequences_N.get(1));
                            solutionSet_Qi.add(sequences_N.get(2));
                            numberOfInstancesInQi++;
                        } else {
                            if (sequencesN_NOT_DisjointFromSolutionSet(solutionSet_Qi, sequences_N)) { //i.e. solutionSet and sequences_N has one or two common nodes.
                                numberOfInstancesInQi++;
                                for (Node n : sequences_N) {
                                    if (!solutionSet_Qi.contains(n)) {
                                        solutionSet_Qi.add(n);
                                    }
                                }
                            }
                        }
                        //System.out.println("Solution Set Qi--> " + solutionSet_Qi);
                        break;
                    }
                }
            }
            tx.success();
        }
        return numberOfInstancesInQi;
    }

    //Assuming every node can have a single label at the max.
    public boolean compareLabels(Label labelName, Label targetLabel) {
        /*for (Label l : labels) {*/
        /*    if (labelName.toString().equals(l.toString())) {*/
        /*        return true;*/
        /*    }*/
        /*}*/
        /*return false;*/
        return labelName.toString().equals(targetLabel.toString());
    }


    public boolean checkForThisTriplet(List<Label> sequences_L, List<Node> sequences_N) {
        Node first = sequences_N.get(0);
        Node middle = sequences_N.get(1);
        Node last = sequences_N.get(2);
        boolean res_first = compareLabels(sequences_L.get(0), getLabel(first));
        boolean res_middle = compareLabels(sequences_L.get(1), getLabel(middle));
        boolean res_last = compareLabels(sequences_L.get(2), getLabel(last));
        if (res_first && res_middle && res_last) {  //This means that given sequence of labels was found in the given list of nodes.
            return true;
        } else {
            return false;
        }
    }


    public boolean sequencesN_NOT_DisjointFromSolutionSet(List<Node> solutionSet_Qj, List<Node> sequences_N) {
        List<Node> solutionSet_Qj_copy = new LinkedList<Node>(solutionSet_Qj);
        List<Node> sequences_N_copy = new LinkedList<Node>(sequences_N);
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
    public int validateAgainstQj(String qj, List<List<Label>> qj_set, List<Node> solutionSet_Qj) {

        GraphDatabaseService DB = queryToDBServiceMapping.get(qj);
        int numberOfInstancesInQj = 0;
        try (Transaction tx = DB.beginTx()) {

            List<List<Node>> sequences_InNodes = TLS_map_usingNodeIds.get(qj);
            for (List<Label> sequences_L : qj_set) {
                for (List<Node> sequences_N : sequences_InNodes) {
                    if (checkForThisTriplet(sequences_L, sequences_N)) {
                        //System.out.println("sequences_L--> " + sequences_L);
                        //System.out.println("sequences_N--> " + sequences_N);

                        if (solutionSet_Qj.isEmpty()) {
                            solutionSet_Qj.add(sequences_N.get(0));
                            solutionSet_Qj.add(sequences_N.get(1));
                            solutionSet_Qj.add(sequences_N.get(2));
                            numberOfInstancesInQj++;
                        } else {
                            if (sequencesN_NOT_DisjointFromSolutionSet(solutionSet_Qj, sequences_N)) { //i.e. solutionSet and sequences_N has one or two common nodes.
                                numberOfInstancesInQj++;
                                for (Node n : sequences_N) {
                                    if (!solutionSet_Qj.contains(n)) {
                                        solutionSet_Qj.add(n);
                                    }
                                }
                            }
                        }
                        //System.out.println("Solution Set Qj--> " + solutionSet_Qj);
                        break;
                    }
                }
            }
            tx.success();
        }
        return numberOfInstancesInQj;
    }

    public int checkWhichTLSInstancesFormLargestSubgraph(String qi, String qj, List<List<Label>> qj_set) {
        List<Node> solutionSet_Qi = new LinkedList<Node>();
        List<Node> solutionSet_Qj = new LinkedList<Node>();
        int numberOfInstancesInQi = validateAgainstQi(qi, qj_set, solutionSet_Qi);
        //System.out.println();
        int numberOfInstancesInQj = validateAgainstQj(qj, qj_set, solutionSet_Qj);

        // So, we now have LI(qi,TLS(qi,qj)) and LI(qj,TLS(qi,qj)) for all qi and qj in the form of the Solution Set size.
        //System.out.println("numberOfInstancesInQi--> " + numberOfInstancesInQi);
        //System.out.println("numberOfInstancesInQj--> " + numberOfInstancesInQj);
        return Math.min(numberOfInstancesInQi, numberOfInstancesInQj);
    }


    public void createGroupMatrix() {
        Iterator itr = TLS_QueryPairs.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry pairs = (Map.Entry) itr.next();
            Map<String, List<List<Label>>> qj = (Map<String, List<List<Label>>>) pairs.getValue();
            Iterator itr_qj = qj.entrySet().iterator();
            groupMatrix.put((String) pairs.getKey(), new LinkedHashMap<String, Float>());
            while (itr_qj.hasNext()) {
                Map.Entry pairs_qj = (Map.Entry) itr_qj.next();
                //I create a new instance here just to avoid editing the values in the map 'qj'.
                List<List<Label>> qj_set = new LinkedList<List<Label>>((List<List<Label>>) pairs_qj.getValue());
                //System.out.println("Comparing Query: " + (String) pairs.getKey() + " with Query: " + (String) pairs_qj.getKey());
                int minimumLIValue = checkWhichTLSInstancesFormLargestSubgraph((String) pairs.getKey(), (String) pairs_qj.getKey(), qj_set);
                int minTLSSizeForQueries = Math.min(TLS_map.get((String) pairs.getKey()).size(), TLS_map.get((String) pairs_qj.getKey()).size());
                //System.out.println("Numerator of the GF equation: " + minimumLIValue);
                //System.out.println("Denominator of the GF equation: " + minTLSSizeForQueries + "\n");
                Float GF_value = minimumLIValue / Float.valueOf(minTLSSizeForQueries);

                groupMatrix.get((String) pairs.getKey()).put((String) pairs_qj.getKey(), GF_value);

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

    public GraphDatabaseService releaseLockANDReturnService(File db_loc){
        StoreLocker lock = new StoreLocker(new DefaultFileSystemAbstraction());
        lock.checkLock(db_loc);
        GraphDatabaseService database = null;
        GraphDatabaseFactory graphInstance = new GraphDatabaseFactory();
        try{
            lock.close();
            database = graphInstance.newEmbeddedDatabase(db_loc);
            /*database = graphInstance.newEmbeddedDatabaseBuilder(db_loc)
                    .setConfig(GraphDatabaseSettings.pagecache_memory,"512M")
                    .setConfig(GraphDatabaseSettings.string_block_size,"60")
                    .setConfig(GraphDatabaseSettings.array_block_size,"300").newGraphDatabase();*/
        } catch(Exception e){
            e.printStackTrace();
            System.exit(0);
        }
        return database;
    }

    public static void main(String[] args) {
        formGroupMatrix_directedG grouping = new formGroupMatrix_directedG();
        String currentDirectory = System.getProperty("user.dir");
        //File[] allFiles = new File(currentDirectory+"/Human_iGraph_Queries").listFiles();

        //grouping.forIGraph(currentDirectory+"/Yeast_iGraph_Queries");


        grouping.generateTLS();

        System.out.println("TLS_Map:-\n");
        grouping.printTLS_Map();
        System.out.println("\n\n");

        System.out.println("TLS_Map using NodeIDs:-\n");
        grouping.printTLS_Map_usingNodeIDs();
        System.out.println("\n\n");

        grouping.formIntersection_TLSseqs_BetweenQueries();
        grouping.printQueryPairs();

        //Forms the group matrix (not thresholded).
        grouping.createGroupMatrix();
        System.out.println("\n\nGroup Matrix: \n" + grouping.groupMatrix);


        //Now put a threshold limit of 0.35 on the Group factor values to make the matrix binary.
        Iterator itr = grouping.groupMatrix.entrySet().iterator();
        while(itr.hasNext()){
            Map.Entry pairs = (Map.Entry)itr.next();
            Map<String,Float> nested_map = (Map<String,Float>) pairs.getValue();
            Iterator itr_nested = nested_map.entrySet().iterator();
            while(itr_nested.hasNext()){
                Map.Entry pairs_nested = (Map.Entry) itr_nested.next();
                Float gf_value = (Float) pairs_nested.getValue();
                if(gf_value > 0.35){
                    nested_map.put((String)pairs_nested.getKey(),Float.valueOf(1));
                } else {
                    nested_map.put((String)pairs_nested.getKey(),Float.valueOf(0));
                }
            }
        }
        //System.out.println("\n\nGroup Matrix (After Thresholding): \n" + grouping.groupMatrix);
    }
}
