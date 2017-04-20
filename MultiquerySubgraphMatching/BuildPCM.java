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

class findMCS{
    HashMap<String, node> graph1;
    HashMap<String, node> graph2;
    HashMap<String,ArrayList<String> > QuerySearchSpace =  new HashMap<>();
    ArrayList<String> Order = new ArrayList<>();
    HashMap<String,String> Solution = new HashMap<>();
    findMCS(HashMap<String, node> graph1, HashMap<String, node> graph2){
        this.graph1 = graph1;
        this.graph2 = graph2 ;
    }

    public void executefindMCS(){
        if(graph1.size()>graph2.size()){
            createSearchSpace(graph1);
            Ordering(graph2);
        }else{
            createSearchSpace(graph2);
            Ordering(graph1);
        }




    }

    public void search(int index,HashMap<String,node > Query){
        String query_node = Order.get(index);
        String label = Query.get(query_node).label;
        ArrayList<String> graph_nodes = QuerySearchSpace.get(label);

        for(String node: graph_nodes){
            boolean enc = false;

            Collection<String> nodes = Solution.values();
            if(nodes.contains(node))
                enc = true;
            if(!check(query_node,node,Query) || enc){
                continue;
            }

            Solution.put(query_node,node);
            if(index < Order.size()-1)
                search(index+1,Query);
            else if(Solution.size()==Order.size()){
                Iterator iter= Solution.entrySet().iterator();
                while(iter.hasNext()) {
                    Map.Entry pair = (Map.Entry) iter.next();
                    Long id = (Long) pair.getValue();
                    System.out.print(id+",");
                }
                System.out.println();
                //Solution.remove(query_node);
            }
        }
        Solution.remove(query_node);

    }
    public boolean check(String query_node,String graph_node,HashMap<String,node > Query){

        if(Solution.size()==0)
            return true;
        else{
            ArrayList<node> neighbhors = Query.get(query_node).edge;
            for(node nbh:neighbhors){
                if(Solution.containsKey(nbh.id)){
                    Long node_id = Solution.get(nbh.id);
                    Node graph_node1 = db.getNodeById(node_id);
                    Node graph_node2 = db.getNodeById(graph_node);
                    Iterable<Relationship> rel_list = graph_node1.getRelationships(Direction.BOTH);
                    boolean flag=false;
                    Iterator iter = rel_list.iterator();
                    while(iter.hasNext()){
                        Relationship rel =(Relationship) iter.next();
                        if(graph_node2.equals(rel.getOtherNode(graph_node1)))
                            flag=true;
                    }
                    if(!flag){
                        return false;
                    }
                }
            }
        }
        return true;
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
        for(String id:Order){
            System.out.print(id+",");
        }
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
    PCM(int [][]grpMat ,HashMap<String, HashMap<String, node>> queries)
    {
        this.Query = queries;
        this.grpMat = grpMat;
    }

    public void buildPCM(){
        clique_helper();

        for(Set group: Cliques){
            Set<Integer> NextLevelGroup = new HashSet<>();
            ArrayList<Set> disjointPairs = findDisjoint(group);
            for(Set pair: disjointPairs){
                MCS(pair);

            }
        }


    }




    public void MCS(Set<Integer> pair){
        Iterator iter =pair.iterator();
        HashMap<String, node> graph1 = Query.get(iter.next());
        HashMap<String, node> graph2 = Query.get(iter.next());




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
            pair.add(i);
            for (int j=i+1;j<query_size;j++){
                pair.add(j);
            }
            pair_list.add(pair);
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

    public Set findDiffernce(Set<Integer> set1,Set<Integer> set2){
        Set<Integer> difference =  new HashSet<>(set1);
        difference.removeAll(set2);
        return difference;
    }
}


public class BuildPCM {
    //class attribute
    HashMap<String, HashMap<String, node>> Query = new HashMap<>();
    int[][] groupingMatrix ={{0,0,1,1,0},
                             {0,0,0,0,0},
                             {1,0,0,1,1},
                             {1,0,1,0,1},
                             {0,0,1,1,0}};


    //main function starts
    public static void main(String args[]) {
        BuildPCM qr = new BuildPCM();
        PCM pc = new PCM(qr.groupingMatrix,qr.Query);
        pc.buildPCM();
        /*
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
                    System.out.println("Enter Human Database path::");
                    File database = new File(input.nextLine());

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
                    System.out.println("Enter Protein Database path::");
                    File database = new File(input.nextLine());


                    //String protein_path = "C:\\Users\\ritvi\\Desktop\\Graph\\assignment 4\\Proteins\\Proteins\\part3_Proteins\\Proteins\\target";

                    HashMap<String, node> graph = qr.read_data_Protein(protein_path);

                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }


            default:
                System.out.println("Wrong input...Please choose from one of the options");
                break;

        }//switch end
        */
    }//main end

    public void read_data_iGraph(String filename) {
        try {
            System.out.println(filename);
            File file = new File(filename);
            Scanner reader = new Scanner(new FileInputStream(file));
            HashMap<String, node> sub_graph = new HashMap<>();
            while (reader.hasNext()) {
                String input_line = reader.nextLine();
                String line_buffer[] = input_line.split("\\s+");
                switch (line_buffer[0]) {

                    case "t":
                        String id = line_buffer[2];
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
                        v2.edge.add(v1);

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