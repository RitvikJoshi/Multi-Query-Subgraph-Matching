import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.util.*;

/**
 * Created by ritvi on 5/10/2017.
 */
class subGraphMatching {


    HashMap<String, node> Query;
    HashMap<String,ArrayList<Long>>SearchSpace;
    ArrayList<String> Order = new ArrayList<>();
    HashMap<String,ArrayList<Long>> SolutionLabelToNodes = new HashMap<>();
    HashMap<String,ArrayList<Long>> Solution = new HashMap<>();
    int counter=0;
    static GraphDatabaseService db;
    //File Filename;

    subGraphMatching(HashMap<String, node> Query, GraphDatabaseService db, HashMap<String,ArrayList<Long>>SearchSpace){
        this.Query=Query;
        this.db=db;
        this.SearchSpace=SearchSpace;

    }

    public HashMap<String,ArrayList<Long>> matching(){
        searchspace();
        profiling();
        order();

        long start_time = System.currentTimeMillis();
        search(0);
        System.out.println("\nTotal solution::"+counter);
        long end_time = System.currentTimeMillis();
        System.out.println("Total time taken in searching::"+(end_time-start_time)+"ms");

        return SolutionLabelToNodes;
    }

    public void order() {
        String min_size_id="";
        int min_size = -1;
        HashMap<String,Integer> searchspace_size= new HashMap<>();
        Iterator iter = Query.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry pair = (Map.Entry) iter.next();
            String id = (String) pair.getKey();
            node current_node = (node) pair.getValue();
            ArrayList<Long> current_searchspace = SearchSpace.get(current_node.label);
            //System.out.println("Key::"+id+"Value::"+current_searchspace.size());
            searchspace_size.put(id,current_searchspace.size());
            if(min_size==-1){
                min_size=current_searchspace.size();
                min_size_id=id;
            }else{
                if(min_size>current_searchspace.size()){
                    min_size=current_searchspace.size();
                    min_size_id=id;
                }
            }
        }
        Order.add(min_size_id);
        double current_cost = searchspace_size.get(min_size_id);
        while(Order.size()<searchspace_size.size()){
            current_cost=findnext(current_cost,searchspace_size);
        }


        System.out.print("Order::");
        for(String ord:Order){
            System.out.print(ord+",");
        }
        System.out.println();

    }

    public double findnext(double current_Cost,HashMap<String, Integer> searchspace_Size){
        HashMap<String, Integer> Edge_count = new HashMap<>();
        String min_cost_id="";
        double min_cost=-1;
        for(String ord: Order){

            node current_node = Query.get(ord);
            ArrayList<node> edges= current_node.edge;
            for(node node1:edges){
                if(!Order.contains(node1.id)) {
                    if (Edge_count.containsKey(node1.id)) {
                        Edge_count.put(node1.id, Edge_count.get(node1.id) + 1);
                    } else {
                        Edge_count.put(node1.id, 1);
                    }
                }else{
                    continue;

                }
            }
        }
        Iterator iter = Edge_count.entrySet().iterator();
        while(iter.hasNext()){
            Map.Entry pair = (Map.Entry) iter.next();
            String key = (String) pair.getKey();
            double val = (Integer) pair.getValue();
            if(min_cost==-1){
                min_cost=   current_Cost * val *(Math.pow(0.5,val));
                min_cost_id =key;
            }else{
                double temp_cost =  current_Cost * val *(Math.pow(0.5,val));
                if(min_cost>temp_cost){
                    min_cost= temp_cost;
                    min_cost_id =key;
                }
            }
        }
        Order.add(min_cost_id);

        return min_cost;


    }

    public void profiling(){
        Transaction tx= db.beginTx();
        ArrayList<Long> opt_searchspace = new ArrayList<>();

        Iterator iter = Query.entrySet().iterator();
        while(iter.hasNext()){
            Map.Entry pair = (Map.Entry) iter.next();
            String id = (String)pair.getKey();
            node current_node = (node)pair.getValue();
            ArrayList<node> edges = Query.get(id).edge;
            Hashtable<String,Boolean> connected_label = new Hashtable<>();
            for(node nbh: edges){
                connected_label.put(nbh.label,false);
            }
            ArrayList<Long> current_searchspace = SearchSpace.get(current_node.label);
            for(Long node_id:current_searchspace){
                Node nodei = db.getNodeById(node_id);
                Iterable<Relationship> rel = nodei.getRelationships();
                Iterator rel_iter =  rel.iterator();

                while(rel_iter.hasNext()){
                    Relationship relation = (Relationship)rel_iter.next();
                    Node nodej = relation.getOtherNode(nodei);
                    Iterable<Label> labels = nodej.getLabels();
                    Iterator lbl_iter = labels.iterator();
                    while(lbl_iter.hasNext()){
                        Label lbl = (Label) lbl_iter.next();
                        if(connected_label.containsKey(lbl.name())){
                            connected_label.put(lbl.name(),true);
                        }
                    }

                }
                boolean good_node=false;
                Collection<Boolean> con_lbl = connected_label.values();
                for(Boolean flag:con_lbl){
                    if(flag){
                        good_node=true;
                    }else{
                        good_node=false;
                        break;
                    }
                }
                if(good_node){
                    opt_searchspace.add(node_id);
                }

            }
            current_searchspace=opt_searchspace;
        }
        tx.close();
    }



    public void searchspace(){

        Iterator iter= Query.entrySet().iterator();

        Transaction tx = db.beginTx();
        while(iter.hasNext()){

            Map.Entry pair = (Map.Entry) iter.next();
            String id = (String) pair.getKey();
            node graph_node = (node) pair.getValue();
            if(!SearchSpace.containsKey(graph_node.label)) {
                Label label = Label.label(graph_node.label);
                ResourceIterator<Node> result = db.findNodes(label);
                ArrayList<Long> node_id = new ArrayList<>();
                while (result.hasNext()) {
                    Node node = result.next();
                    node_id.add(node.getId());

                }
                SearchSpace.put(graph_node.label, node_id);
            }
        }
        tx.close();


    }

    public void search(int index) {
        String query_node = Order.get(index);
        String label = Query.get(query_node).label;
        ArrayList<Long> graph_nodes = SearchSpace.get(label);

        for (Long node : graph_nodes) {
            boolean enc = false;
            Collection<ArrayList<Long>> allLabelsNodes = Solution.values();
            Collection<Long> nodes = new ArrayList<>();
            for (ArrayList<Long> nodeList : allLabelsNodes) {
                nodes.addAll(nodeList);
            }
            //Collection<Long> nodes = Solution.values();
            if (nodes.contains(node))
                enc = true;
            if (!check(query_node, node) || enc) {
                continue;
            }
            if (Solution.containsKey(query_node)) {
                ArrayList<Long> solNodeList = Solution.get(query_node);
                solNodeList.add(node);
            } else {
                ArrayList<Long> solNodeList = new ArrayList<>();
                solNodeList.add(node);
                Solution.put(query_node, solNodeList);
            }

            if (index < Order.size() - 1)
                search(index + 1);
            else if (Solution.size() == Order.size()) {
                counter++;
                System.out.println(counter + ":");
                Iterator iter = Solution.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry pair = (Map.Entry) iter.next();
                    ArrayList<Long> solNodeList = (ArrayList<Long>) pair.getValue();
                    //Long id = (Long) pair.getValue();
                    for(Long id: solNodeList) {
                        System.out.print(id + ",");
                    }
                }
                System.out.println();

                solutionToLabelNodes(Solution);
                //Solution.remove(query_node);
            }
        }
        if(Solution.containsKey(query_node)){
            if (Solution.get(query_node).size() > 1) {
                ArrayList<Long> solNodeList = Solution.get(query_node);
                solNodeList.remove(solNodeList.size() - 1);
            }else {
                Solution.remove(query_node);
            }
        }else {
            Solution.remove(query_node);
        }
    }
    public boolean check(String query_node,Long graph_node){
        Transaction tx= db.beginTx();
        if(Solution.size()==0)
            return true;
        else{
            ArrayList<node> neighbhors = Query.get(query_node).edge;
            for(node nbh:neighbhors){
                if(Solution.containsKey(nbh.id)){
                    ArrayList<Long> solNodeList = Solution.get(nbh.id);
                    boolean flag = false;
                    for (Long node_id:solNodeList) {
                        //Long node_id = Solution.get(nbh.id);
                        Node graph_node1 = db.getNodeById(node_id);
                        Node graph_node2 = db.getNodeById(graph_node);
                        Iterable<Relationship> rel_list = graph_node1.getRelationships(Direction.BOTH);
                        Iterator iter = rel_list.iterator();
                        while (iter.hasNext()) {
                            Relationship rel = (Relationship) iter.next();
                            if (graph_node2.equals(rel.getOtherNode(graph_node1))) {
                                flag = true;
                                break;
                            }
                        }
                    }
                    if (!flag) {
                        return false;
                    }

                }
            }
        }
        return true;
    }

    public void solutionToLabelNodes(HashMap<String,ArrayList<Long>> Solution){
        Iterator iter = Solution.entrySet().iterator();
        while(iter.hasNext()){
            Map.Entry pair = (Map.Entry) iter.next();
            String label = (String) pair.getKey();
            if(SolutionLabelToNodes.containsKey(label)){
                ArrayList<Long> allSolNodeList = SolutionLabelToNodes.get(label);
                ArrayList<Long> solNodeList = (ArrayList<Long>) pair.getValue();
                for(Long node_id:solNodeList){
                    if(!allSolNodeList.contains(node_id)){
                        allSolNodeList.add(node_id);
                    }
                }
            }else{
                ArrayList<Long> allSolNodeList = new ArrayList<>();
                ArrayList<Long> solNodeList = (ArrayList<Long>) pair.getValue();
                allSolNodeList.addAll((ArrayList<Long>)solNodeList.clone());
                SolutionLabelToNodes.put(label,allSolNodeList);
            }
        }

    }
}


