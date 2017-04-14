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
    Map<String,Map<Long,Set<Long>>> neigborsForNodes = new HashMap<String,Map<Long,Set<Long>>>();
    Map<String,Map<Node,Set<Node>>> immediateNeighbors = new LinkedHashMap<String,Map<Node,Set<Node>>>();
    Map<String,List<List<Label>>> TLS_map = new LinkedHashMap<String,List<List<Label>>>();
    List<List<Label>> TLS_sequencesInLabels = new LinkedList<List<Label>>();

    public void parse_testQueries(File[] files) {
        String lineRead;
        BufferedReader formatBufferedReader;
        BatchInserter bInserter = null;
        for (File f : files) {
            if (f.isFile() && f.getName().split("\\.")[1].equals("txt")) {
                try {
                    File newDBFile = new File("/Users/adityapulekar/Documents/Neo4j/GraphDB_proj/" + f.getName().split("\\.")[0]);
                    neigborsForNodes.put(f.getName(), new HashMap<Long,Set<Long>>());
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
                                if(neigborsForNodes.get(f.getName()).containsKey((long) Integer.parseInt(splitLine[0]))){
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

    public boolean compareLabelValue(Node firstNodeOfSeq, Node lastNodeInSeq){
        Iterable<Label> labels = firstNodeOfSeq.getLabels();
        Iterable<Label> degreeTwoNodeLabels = lastNodeInSeq.getLabels();
        for(Label l_first: labels){
            for(Label l_last: degreeTwoNodeLabels){
                int result = l_first.toString().compareTo(l_last.toString());
                if(result > 0){
                    return false;
                }
            }
        }
        return true;
    }

    //Use this method only in case of the query graphs since only the query graphs will for sure have nodes with exactly one Label.
    public Label getLabel(Node nodes){
       Iterable<Label> labels = nodes.getLabels();
       //Since each node in the query graph will have a single Label.
       for(Label l : labels){
           return l;
       }
       return null;
    }

    public List<List<Node>> createTheSequence(Node firstNodeInSeq, Node middleNodeOfSeq, Map<Node,Set<Node>> TLS_wrtQuery){
        Set<Node> neighborsOfMiddleNode = TLS_wrtQuery.get(middleNodeOfSeq);
        List<List<Node>> toBeReturned = new LinkedList<List<Node>>();
        
        //Note that if there are less than three vertices (i.e. a third vertex is not chosen),
        // then the "toBeReturned" list will remain empty.
        for(Node lastNodeInSeq: neighborsOfMiddleNode){
            if(compareLabelValue(firstNodeInSeq,lastNodeInSeq) && firstNodeInSeq.getId() != lastNodeInSeq.getId()){
                toBeReturned.add(new LinkedList<Node>() {{ add(firstNodeInSeq); add(middleNodeOfSeq); add(lastNodeInSeq);}});

                //Note: We are assuming here that the getLabel() method will always return a Label.
                TLS_sequencesInLabels.add(new LinkedList<Label>() {{ add(getLabel(firstNodeInSeq)); add(getLabel(middleNodeOfSeq)); add(getLabel(lastNodeInSeq));}});
            }
        }
        return toBeReturned;
    }


    //Thsi method finally fetches us the Tri-vertex Label sequences.
    public List<List<Node>> checkTheThirdVertex(Map<Node,Set<Node>> TLS_wrtQuery){
        Iterator<Map.Entry<Node,Set<Node>>> itr = TLS_wrtQuery.entrySet().iterator();
        List<List<Node>> TLS = new LinkedList<List<Node>>();
        while(itr.hasNext()) {
            Map.Entry pairs = itr.next();
            Set<Node> firstDegreeNeigbors = (Set<Node>)pairs.getValue();
            for(Node degreeOneNeighbor : firstDegreeNeigbors) {
                List<List<Node>> listOfSeq = createTheSequence((Node) pairs.getKey(), degreeOneNeighbor, TLS_wrtQuery);
                if(!listOfSeq.isEmpty()) {
                   TLS.addAll(listOfSeq);
                }
            }
        }
        //Note: TLS will have all the sequences in terms of Nodes. We are going to maintain another list which will have
        //      all the sequences in terms of the corresponding label values.
        return TLS;
    }

    

    public void generateTLS(){
        String databaseDir = "/Users/adityapulekar/Documents/Neo4j/GraphDB_proj/";
        File[] queries = new File("/Users/adityapulekar/Documents/Neo4j/GraphDB_proj/").listFiles();
        GraphDatabaseFactory graphInstance = new GraphDatabaseFactory();
        for(File queryFile : queries){
            if (queryFile.isDirectory() && !queryFile.getName().equals(".DS_Store")) {
                GraphDatabaseService database = graphInstance.newEmbeddedDatabase(queryFile);
                TLS_sequencesInLabels = new LinkedList<List<Label>>();
                try (Transaction transac = database.beginTx()) {
                    ResourceIterable<Node> allNodes = database.getAllNodes();
                    immediateNeighbors.put(queryFile.getName().split("\\.")[0], new LinkedHashMap<Node, Set<Node>>());
                    Map<Node, Set<Node>> TLS_wrtQuery = immediateNeighbors.get(queryFile.getName().split("\\.")[0]);

                    //Here we fetch all the first degree neighbors of all the nodes.
                    for (Node nodes : allNodes) {
                        Iterable<Relationship> outRelationships = nodes.getRelationships(Direction.OUTGOING);
                        TLS_wrtQuery.put(nodes, new HashSet<Node>());
                        for (Relationship rel : outRelationships) {
                            TLS_wrtQuery.get(nodes).add(rel.getEndNode());
                        }
                    }
                    List<List<Node>> TLS_sequencesInNodes = checkTheThirdVertex(TLS_wrtQuery);
                    TLS_map.put(queryFile.getName().split("\\.")[0], TLS_sequencesInLabels);

                    transac.success();

                }
            }
        }
    }

    public static void main(String[] args){
        formGroupMatrix grouping = new formGroupMatrix();
        String currentDirectory = System.getProperty("user.dir");
        File[] allFiles = new File(currentDirectory).listFiles();
        grouping.parse_testQueries(allFiles);
        grouping.generateTLS();
        System.out.println("TLS_Map:-\n" + grouping.TLS_map);

    }
}

