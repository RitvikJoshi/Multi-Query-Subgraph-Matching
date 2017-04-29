import org.apache.commons.compress.archivers.sevenz.CLI;
import scala.Int;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

/**
 * Created by ritvi on 4/13/2017.
 */

class node{
    String id;
    String label;
    ArrayList<node> edge;
    node(String id,String label,ArrayList<node> edge){
        this.id=id;
        this.label=label;
        this.edge=edge;
    }

}

class query {
    String ID;
    int weight;

    public query(String id, int wt){
        ID = id;
        weight = wt;
    }
}


class findMCS{
    HashMap<String, node> graph1;
    HashMap<String, node> graph2;
    HashMap<String,ArrayList<String> > QuerySearchSpace =  new HashMap<>();
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
                node temp_node =  new node(graph_node.id,graph_node.label,graph_node.edge);
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
                Q_mcs.add(Solution);
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
                Q_mcs.add(Solution);
//                Collection<String> searched_nodes = Solution.values();
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

//Pattern Containment Map
class PCM{

    int[][] grpMat;
    ArrayList<Set> Cliques = new ArrayList<>();
    HashMap<String, HashMap<String, node>> Query = new HashMap<>();
    Map<query,List<query>> childrenMapToParents = new LinkedHashMap<>();
    Map<query,List<query>> parentsMapToChildren = new LinkedHashMap<>();
    PCM(int [][]grpMat ,HashMap<String, HashMap<String, node>> queries)
    {
        this.Query = queries;
        this.grpMat = grpMat;
    }

    public void buildPCM(){
        int Pcm_node =100;
        clique_helper();

        for(Set group: Cliques){
            Set<Integer> group_copy = new HashSet<>(group);
            Set<Integer> NextLevelGroup = new HashSet<>();
            if (group.size()%2!=0){
                Iterator iter =  group_copy.iterator();
                int last_query=(int)iter.next();
                NextLevelGroup.add(last_query);
                iter.remove();
            }
            ArrayList<Set> disjointPairs = findDisjoint(group_copy);
            System.out.println("Disjoint pair::");
            for(Set<Integer> pair: disjointPairs){

                for (int i: pair){
                    System.out.print(i+",");
                }
                System.out.println();

            }
            Random rand = new Random();
            //int random_pair_index = rand.nextInt(disjointPairs.size());


            for(Set pair: disjointPairs){
                ArrayList<HashMap<String,node>> Qmcs;
                Qmcs = MCS(pair);
                int  q_id1;
                int q_id2;
                int small_q_id;
                int max_q_id;
                Iterator p_iter = pair.iterator();
                q_id1=(int)p_iter.next();
                q_id2=(int)p_iter.next();

                HashMap<String,node> small_graph;
                HashMap<String,node> graph1 = Query.get(""+q_id1);
                HashMap<String,node> graph2 = Query.get(""+q_id2);
                if(graph1.size()>graph2.size()){
                    small_q_id=q_id2;
                    small_graph=graph2;
                    max_q_id=q_id1;
                }else{
                    small_q_id=q_id1;
                    small_graph=graph1;
                    max_q_id=q_id2;
                }
                if(Qmcs!=null){
                    for(HashMap<String, node> mcs : Qmcs ){
                        if(equate(mcs,small_graph)){
                            NextLevelGroup.add(small_q_id);
                            /*if(childrenMapToParents.containsKey()){

                            }*/
                        }else{
                            Query.put(""+(Pcm_node++),mcs);
                        }
                    }
                }
            }
        }


    }

    public boolean equate(HashMap<String, node> graph1,HashMap<String, node> graph2){
        HashMap<String, ArrayList<String>> label_map_g1;
        HashMap<String, ArrayList<String>> label_map_g2;

        label_map_g1 = coverttoLabelMap(graph1);
        label_map_g2 = coverttoLabelMap(graph2);

        Iterator iter = label_map_g1.entrySet().iterator();
        while(iter.hasNext()){
            Map.Entry pair = (Map.Entry) iter.next();

            String label_g1_key = (String)pair.getKey();
            if(label_map_g2.containsKey(label_g1_key)){
                ArrayList<String> edge = new ArrayList<>(label_map_g1.get(label_g1_key));
                edge.removeAll(label_map_g2.get(label_g1_key));
                if(edge.size()==0){
                    continue;
                }
                else {
                    return false;
                }
            }else{
                return false;
            }


        }

        return true;
    }

    public HashMap<String, ArrayList<String>> coverttoLabelMap(HashMap<String,node > graph){
        HashMap<String, ArrayList<String>> label_map = new HashMap<>();
        Iterator iter = graph.entrySet().iterator();
        while(iter.hasNext()){
            Map.Entry pair = (Map.Entry) iter.next();
            //String key = (String) pair.getKey();
            node  node_g1 = (node) pair.getValue();

            if(label_map.containsKey(node_g1.label)){
                for(node nbh: node_g1.edge)
                    label_map.get(node_g1.label).add(nbh.label);
            }else{
                ArrayList<String> temp =  new ArrayList<>();
                for(node nbh: node_g1.edge)
                    temp.add(nbh.label);
                label_map.put(node_g1.label,temp);
            }
        }
        return label_map;
    }

    public ArrayList<HashMap<String,node>>  MCS(Set<Integer> pair){
        System.out.println("Random pair::");
        for(int i:pair){
            System.out.print(i+",");
        }
        System.out.println();
        Iterator iter =pair.iterator();

        HashMap<String, node> graph1 = Query.get(""+iter.next());
        HashMap<String, node> graph2 = Query.get(""+iter.next());
        /*
        HashMap<String, node> graph1 = Query.get("4");
        HashMap<String, node> graph2 = Query.get("5");
        */

        System.out.println("Mcs input G1:"+graph1+","+graph2);
        ArrayList<HashMap<String,node>> Qmcs = new ArrayList<>();
        findMCS mcs =  new findMCS(graph1,graph2);
        Qmcs=mcs.executefindMCS();

        return Qmcs;
    }



    public ArrayList<Set> findDisjoint(Set<Integer> queries ){
        ArrayList<Set> pair_list = new ArrayList<>();
        int query_size = queries.size();

        int[] temp_array = new int[query_size];
        int counter=0;
        for(int i : queries){
            temp_array[counter] = i;
            counter++;
        }

        for(int i =0 ;i<query_size-1;i++){
            Set<Integer> pair = new HashSet<>();
            for (int j=i+1;j<query_size;j++){
                pair.add(temp_array[i]);
                pair.add(temp_array[j]);
                pair_list.add(pair);
                pair = new HashSet<>();
            }

        }

        return pair_list;
    }

    public void clique_helper(){
        Set<Integer> max_clique = new HashSet<>();
        Set<Integer> vertices  = new HashSet<>();
        Set<Integer> Used_vertices =  new HashSet<>();
        HashMap<Integer,Set> nbhrs = new HashMap<>();


        for(int i=0;i<grpMat.length;i++){
            for(int j=0;j<grpMat.length;j++){
                if(grpMat[i][j]==1){
                    if(nbhrs.containsKey(i+1)){
                        Set<Integer> temp = nbhrs.get(i+1);
                        temp.add(j+1);
                    }else{
                        Set<Integer> temp = new HashSet<>();
                        temp.add(j+1);
                        nbhrs.put(i+1,temp);
                    }
                    vertices.add(i+1);
                }
            }
        }
        System.out.println("Neighbors");
        Iterator iter= nbhrs.entrySet().iterator();
        while(iter.hasNext()){
            Map.Entry pair = (Map.Entry) iter.next();
            int key = (Integer)pair.getKey();
            Set<Integer> s = (Set)pair.getValue();
            String temp=key+":";
            for(Integer v: s){
                temp+=v+",";
            }

            System.out.println(temp);
        }




        System.out.println("Clique");
        clique_finder(max_clique,vertices,Used_vertices,nbhrs);

        for (Set s:Cliques){
            for(Object i: s){
                System.out.print(i+",");
            }
            System.out.println();
        }

    }

    public void clique_finder(Set<Integer> max_clique, Set<Integer> vertices, Set<Integer> Used_vertices,HashMap<Integer,Set> nbhrs){

        if(findUnion(vertices,Used_vertices).isEmpty()){
            /*for(int i : max_clique) {
                System.out.println(i);
            }*/
            Cliques.add(max_clique);
        }
        Iterator iter = vertices.iterator();
        while(iter.hasNext()){
            int vertex = (Integer)iter.next();
            Set nbh = nbhrs.get(vertex);
            Set<Integer> new_max_clique = new HashSet<>(max_clique);
            new_max_clique.add(vertex);
            clique_finder(new_max_clique,findIntersection(vertices,nbh),findIntersection(Used_vertices,nbh),nbhrs);

            iter.remove();
            Used_vertices.add(vertex);
        }
    }

    public Set findUnion(Set<Integer> set1,Set<Integer> set2){
        Set<Integer> union =  new HashSet<>();
        union.addAll(set1);
        union.addAll(set2);
        return union;
    }

    public Set findIntersection(Set<Integer> set1,Set<Integer> set2){
        Set<Integer> intersection =  new HashSet<>();
        intersection.addAll(set1);
        intersection.retainAll(set2);
        return intersection;
    }


}


public class BuildPCM {
    //class attribute
    //map for the matrix
    //NExt level group==> set
    //Parsing of file
    HashMap<String, HashMap<String, node>> Query = new HashMap<>();
    int[][] groupingMatrix ={{0,0,1,1,0},
            {0,0,0,0,0},
            {1,0,0,1,1},
            {1,0,1,0,1},
            {0,0,1,1,0}};


    //main function starts
    public static void main(String args[]) {
        BuildPCM qr = new BuildPCM();

        System.out.println("Options::");
        System.out.println("1. Human/Yeast");
        System.out.println("2. Protein");
        Scanner input = new Scanner(System.in);
        switch (input.nextInt()) {
            case 1:
                try {
                    System.out.println("Enter Human query path file name::");
                    input = new Scanner(System.in);
                    String filename = input.nextLine();
                    /*
                    System.out.println("Enter Human Database path::");
                    File database = new File(input.nextLine());
                    */
                    qr.read_data_iGraph(filename);


                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }

            case 2:
                try {
                    System.out.println("Enter Protein query path file name::");
                    input = new Scanner(System.in);
                    String protein_path = input.nextLine();
                    /*
                    System.out.println("Enter Protein Database path::");
                    File database = new File(input.nextLine());


                    //String protein_path = "C:\\Users\\ritvi\\Desktop\\Graph\\assignment 4\\Proteins\\Proteins\\part3_Proteins\\Proteins\\target";
                    */
                    HashMap<String, node> graph = qr.read_data_Protein(protein_path);

                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }


            default:
                System.out.println("Wrong input...Please choose from one of the options");
                break;

        }//switch end
        PCM pc = new PCM(qr.groupingMatrix,qr.Query);
        pc.buildPCM();

    }//main end

    public void read_data_iGraph(String filename) {
        try {
            System.out.println(filename);
            File file = new File(filename);
            Scanner reader = new Scanner(new FileInputStream(file));
            HashMap<String, node> sub_graph = new HashMap<>();
            String Q_id="";
            while (reader.hasNext()) {
                String input_line = reader.nextLine();
                String line_buffer[] = input_line.split("\\s+");

                switch (line_buffer[0]) {

                    case "t":
                        String id = line_buffer[2];
                        Q_id=id;
                        sub_graph = new HashMap<>();
                        Query.put(id, sub_graph);
                        break;
                    case "v":
                        String vid = line_buffer[1];
                        String label = line_buffer[2];
                        sub_graph.put(vid, new node(vid, label, new ArrayList<node>()));
                        break;
                    case "e":
                        String vid1 = line_buffer[1];
                        String vid2 = line_buffer[2];

                        node v1 = sub_graph.get(vid1);
                        node v2 = sub_graph.get(vid2);
                        v1.edge.add(v2);
                        //v2.edge.add(v1);
                        System.out.println("Q:"+Q_id+"E:"+vid1+"->"+vid2);
                        break;

                    default:
                        break;


                }//switch end
            }//while end
        } catch (Exception e) {
            e.printStackTrace();
        }//catch end
    }

    public HashMap read_data_Protein(String filename) {
        HashMap<String, node> sub_graph = new HashMap<>();
        try {
            System.out.println(filename);
            File file = new File(filename);
            Scanner reader = new Scanner(new FileInputStream(file));

            boolean vertex_flag = true;
            boolean first_occ = true;

            while (reader.hasNext()) {
                String input_line = reader.nextLine();
                String line_buffer[] = input_line.split("\\s+");
                if (line_buffer.length == 1) {
                    if (first_occ) {

                        first_occ = false;
                    } else {
                        vertex_flag = false;
                    }

                } else {
                    if (vertex_flag) {
                        String id = line_buffer[0];
                        String label = line_buffer[1];
                        sub_graph.put(id, new node(id, label, new ArrayList<node>()));

                    } else {
                        String id1 = line_buffer[0];
                        String id2 = line_buffer[1];
                        node node1 = sub_graph.get(id1);
                        node node2 = sub_graph.get(id2);
                        node1.edge.add(node2);
                        node2.edge.add(node1);

                    }

                }


            }

        }//try end
        catch (Exception e) {
            e.printStackTrace();
        }//catch end

        return sub_graph;

    }//protein function end
}