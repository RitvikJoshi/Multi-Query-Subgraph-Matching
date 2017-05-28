import java.util.*;

class findMCS{
    HashMap<String, node> graph1;
    HashMap<String, node> graph2;
    HashMap<String,ArrayList<String>> QuerySearchSpace =  new HashMap<>();
    ArrayList<String> Order = new ArrayList<>();

    HashMap<String,String> Solution = new HashMap<>();
    ArrayList<HashMap> Q_mcs = new ArrayList<>();
    findMCS(HashMap<String, node> graph1, HashMap<String, node> graph2){
        this.graph1 = graph1;
        this.graph2 = graph2 ;
    }

    public ArrayList<HashMap<String, node>> executefindMCS(){
        Set<Integer> node_visited = new HashSet<>();
        HashMap<String, node> main_graph;
        if(graph1.size()>graph2.size()){
            main_graph=graph1;
            createSearchSpace(graph1);
            Ordering(graph2);
            while(Order.size()!=0) {
                search(0, graph2, graph1);
                //System.out.println("Performing to Q delete ");
                Order.remove(0);

            }
        }else{
            main_graph=graph2;
            createSearchSpace(graph2);
            Ordering(graph1);
            while(Order.size()!=0) {
                search(0,graph1,graph2);
                Order.remove(0);
            }
        }
        ArrayList<Set<String>> mcs_id = new ArrayList<>();
        int max_size=0;

        Set<String> max_nodes = new HashSet<>();
        if(Q_mcs.size()!=0){
            for(HashMap cs: Q_mcs){
                if(cs.size()>max_size){
                    max_nodes.clear();
                    Iterator m_iter = cs.entrySet().iterator();
                    while(m_iter.hasNext()){
                        Map.Entry pair= (Map.Entry) m_iter.next();
                        max_nodes.add((String)pair.getKey());
                    }
                    max_size = cs.size();
                    mcs_id.clear();
                    mcs_id.add(max_nodes);

                }else if(cs.size()==max_size){
                    Set<String> eq_nodes = new HashSet<>();
                    Iterator e_iter = cs.entrySet().iterator();
                    while(e_iter.hasNext()){
                        Map.Entry pair= (Map.Entry) e_iter.next();
                        eq_nodes.add((String)pair.getKey());
                    }
                    if(findDiffernce(max_nodes,eq_nodes).size()!=0){
                        mcs_id.add(eq_nodes);
                    }
                }

            }
        }
        ArrayList<HashMap<String, node>> MCS_list = new ArrayList<>();
        //Building mcs graphs
        for(Set<String> max_set: mcs_id){
            HashMap<String, node> max_cs = new HashMap<>();
            // node creation
            for (String key: max_set) {
                node graph_node = main_graph.get(key);
                node temp_node =  new node(new String(graph_node.id),new String(graph_node.label),new ArrayList<>(graph_node.edge));
                max_cs.put(key,temp_node);
            }
            //edge filteration
            Iterator iter  = max_cs.entrySet().iterator();
            while(iter.hasNext()){
                Map.Entry pair= (Map.Entry) iter.next();
                node temp_node = (node) pair.getValue();
                Iterator edge_iter = temp_node.edge.iterator();
                while(edge_iter.hasNext()){
                    if(!max_cs.containsKey(edge_iter.next())){
                        edge_iter.remove();
                    }
                }
            }
            //final maximum common subgraph
            MCS_list.add(max_cs);
        }


        return MCS_list;

    }


    public Set findDiffernce(Set<String> set1,Set<String> set2){
        Set<String> difference =  new HashSet<>(set1);
        difference.removeAll(set2);
        return difference;
    }

    public void search(int index,HashMap<String,node > Query1,HashMap<String,node > Query2){

        String query_node = Order.get(index);
        String label = Query1.get(query_node).label;
        ArrayList<String> graph_nodes=null;
        if(QuerySearchSpace.containsKey(label)) {
            graph_nodes = QuerySearchSpace.get(label);
        }else{
            if (index < Order.size()-1){
                search(index+1,Query1,Query2);
            }else if(Solution.size()>2){
                System.out.println("Solution::");
                Iterator iter= Solution.entrySet().iterator();
                while(iter.hasNext()) {
                    Map.Entry pair = (Map.Entry) iter.next();
                    String id = (String) pair.getKey();
                    System.out.print(id+",");
                }
                System.out.println();
                Q_mcs.add((HashMap)Solution.clone());
                Solution.clear();

            }
            return;
        }
        for(String node: graph_nodes){
            boolean match=false;
            boolean enc = false;

            Collection<String> nodes = Solution.keySet();
            if(nodes.contains(node))
                enc = true;
            if(!check(query_node,node,Query2) || enc){
                //continue;
            }else {
                match = true;
                Solution.put(node, query_node);
            }
            /*
            if(index < Order.size()-1)
                index=search(index+1,Query1,Query2);
            else if(Solution.size()>2) {
                Iterator iter= Solution.entrySet().iterator();
                System.out.println("Solution::");
                while(iter.hasNext()) {
                    Map.Entry pair = (Map.Entry) iter.next();
                    String id = (String) pair.getKey();
                    System.out.print(id+",");
                }
                System.out.println();
                Q_mcs.add(Solution);
                //Solution.remove(query_node);
            }*/
            if(index < Order.size()-1)
                search(index+1,Query1,Query2);
            else if(Solution.size()>2){
                System.out.println("Solution::");
                Iterator iter= Solution.entrySet().iterator();
                while(iter.hasNext()) {
                    Map.Entry pair = (Map.Entry) iter.next();
                    String id = (String) pair.getKey();
                    System.out.print(id+",");
                }
                System.out.println();
                Q_mcs.add((HashMap)Solution.clone());
//                Collection<String> searched_nodes = Solution.values();
//                for (String rm_node: searched_nodes){
//                for (String rm_node: searched_nodes){
//                    System.out.println("Removing node:"+rm_node);
//                    Order.remove(Order.indexOf(rm_node));
//                }
                Solution.clear();
            }
            if(match==true && Solution.size() <3){
                //System.out.println("Trying to S delete "+ node);
                Solution.remove(node);
            }

        }/*
        if(match==false && Solution.size()>2){
            System.out.println("Solution::");
            Iterator iter= Solution.entrySet().iterator();
            while(iter.hasNext()) {
                Map.Entry pair = (Map.Entry) iter.next();
                String id = (String) pair.getKey();
                System.out.print(id+",");
            }
            System.out.println();
            Q_mcs.add(Solution);
            Solution.clear();
        }else{

        if(index < Order.size()-1)
            index=search(index+1,Query1,Query2);
        if(index == Order.size()-1 && Solution.size()>2){
            System.out.println("Solution::");
            Iterator iter= Solution.entrySet().iterator();
            while(iter.hasNext()) {
                Map.Entry pair = (Map.Entry) iter.next();
                String id = (String) pair.getKey();
                System.out.print(id+",");
            }
            System.out.println();
            Q_mcs.add(Solution);
            Collection<String> searched_nodes = Solution.values();
            for (String node: searched_nodes){
                System.out.println("Removing node:"+node);
                Order.remove(Order.indexOf(node));
            }
            Solution.clear();
        }
        else if(Solution.size()<=2){
            Order.remove(Order.indexOf(query_node));
            Solution.remove(node);
        }
        */

    }
    public boolean check(String query_node,String graph_node,HashMap<String,node > Query){
        boolean match=false;
        if(Solution.size()==0)
            return true;
        else{
            ArrayList<node> neighbhors = Query.get(graph_node).edge;
            for(node nbh:neighbhors){
                if(Solution.containsKey(nbh.id)){
                    match=true;
                    //String query2_node_id = Solution.get(nbh.id);
                    String query2_node_id=nbh.id;
                    node query2_graph_node1 = Query.get(query2_node_id);
                    node query2_graph_node2 = Query.get(graph_node);
                    ArrayList<node> gh1_nbhs = query2_graph_node1.edge;
                    ArrayList<node> gh2_nbhs = query2_graph_node2.edge;
                    boolean flag=false;
                    if(gh1_nbhs.contains(query2_graph_node2) || gh2_nbhs.contains(query2_graph_node1)){
                        flag=true;
                    }
                    if(!flag){
                        return false;
                    }
                }
            }
        }
        if(match==true) {
            return true;
        }
        else{
            return false;
        }
    }

    public void createSearchSpace(HashMap<String, node> graph){
        //generating searchspace for MCS
        Iterator g1_iter = graph.entrySet().iterator();
        while(g1_iter.hasNext()){
            Map.Entry g1_pair = (Map.Entry)g1_iter.next();

            node g1_node = (node) g1_pair.getValue();

            if(QuerySearchSpace.containsKey(g1_node.label)){
                ArrayList<String> label_nodes = QuerySearchSpace.get(g1_node.label);
                label_nodes.add(g1_node.id);
            }else{
                ArrayList<String> label_nodes = new ArrayList<>();
                label_nodes.add(g1_node.id);
                QuerySearchSpace.put(g1_node.label,label_nodes);
            }

        } // g1_iter while end

    }

    public void Ordering(HashMap<String, node> graph){
        //generating order id for MCS
        Iterator g2_iter= graph.entrySet().iterator();
        while(g2_iter.hasNext()) {
            Map.Entry g2_pair = (Map.Entry) g2_iter.next();
            String id = (String) g2_pair.getKey();
            if(!Order.contains(id))
                order(graph,Order,id);
        }
        System.out.println("Ordering");
        for(String id:Order){
            System.out.print(id+",");
        }
        System.out.println();
    }

    public void order(HashMap<String,node> Query,ArrayList<String> edgevisited,String id){

        if(!edgevisited.contains(id)) {
            edgevisited.add(id);
            ArrayList<node> edge = Query.get(id).edge;
            for (node nbh : edge) {
                order(Query, edgevisited,nbh.id);
            }
        }

    }
}
