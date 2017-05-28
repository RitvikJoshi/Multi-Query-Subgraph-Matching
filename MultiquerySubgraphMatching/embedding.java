import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.util.*;

/**
 * Created by ritvi on 5/9/2017.
 */

class embedding{

    HashMap<String, HashMap<String, node>> Query;
    List<query> execOrder;
    Map<query,List<query>> childrenMapToParents;
    // HashMap<query_id,HashMap<Label,ArrayList<neo4j_node_id>>>
    HashMap<String,HashMap<String,ArrayList<Long>>>ExecutedQuerySolution =  new HashMap<>();
    GraphDatabaseService db;

    embedding(HashMap<String, HashMap<String, node>> Query, List<query> execOrder,Map<query,List<query>> childrenMapToParents, File filename){
        this.Query=Query;
        this.execOrder=execOrder;
        this.childrenMapToParents=childrenMapToParents;
        this.db= new GraphDatabaseFactory().newEmbeddedDatabase(filename);

    }//constructor end

    public void executeSubgraphIsomorphism(){
        for (query query_child: execOrder){
            getParentSearchSpace(query_child);
        }
    }

    public void getParentSearchSpace(query child){
        String child_id = child.ID;
        HashMap<String,ArrayList<Long>> childSearchSpace= new HashMap<>();
        List<query> parents = childrenMapToParents.get(child);
        if(parents.size()>0){
            for(query parent:parents){
                String id=parent.ID;
                if(ExecutedQuerySolution.containsKey(id)){
                    HashMap<String,ArrayList<Long>> solution = ExecutedQuerySolution.get(id);
                    Iterator iter = solution.entrySet().iterator();
                    while(iter.hasNext()){
                        Map.Entry pair = (Map.Entry) iter.next();
                        String label = (String) pair.getKey();
                        ArrayList<Long> nodes_id = (ArrayList<Long>) pair.getValue();
                        if(childSearchSpace.containsKey(label)){
                            ArrayList<Long> labelNodes = childSearchSpace.get(label);
                            for(Long nid: nodes_id){
                                if(!labelNodes.contains(nid)){
                                    labelNodes.add(nid);
                                }
                            }
                        }else{
                            ArrayList<Long> labelNodes = new ArrayList<>();
                            labelNodes.addAll(nodes_id);
                        }
                    }
                }
            }//parent search space added
        }//parent check end

        HashMap<String,node> childQuery = Query.get(child_id);
        subGraphMatching sgm = new subGraphMatching(childQuery,db,childSearchSpace);
        ExecutedQuerySolution.put(child_id,sgm.matching());

    }





}




