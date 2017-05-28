import java.util.*;

//Pattern Containment Map

class PCM{

    int[][] grpMat;
    int Pcm_node=100;
    ArrayList<Set> Cliques = new ArrayList<>();
    HashMap<String, HashMap<String, node>> Query = new HashMap<>();
    Map<String,query> stringToQueryMap =  new HashMap<>();
    Map<query,List<query>> childrenMapToParents = new LinkedHashMap<>();
    Map<query,List<query>> parentsMapToChildren = new LinkedHashMap<>();
    List<String> newQueries = new LinkedList<>();
    PCM(Map<String,Map<String,Float>> Grp,HashMap<String, HashMap<String, node>> queries)
    {
        this.Query = queries;
        this.grpMat = convertToInt(Grp);
        init_CPMAP();
    }

    public int[][] convertToInt(Map<String,Map<String,Float>> Grp){
        int [][] grpmat = new int[Grp.size()][Grp.size()];

        Iterator iter = Grp.entrySet().iterator();
        while(iter.hasNext()){
            Map.Entry pair = (Map.Entry)iter.next();
            String id = (String)pair.getKey();
            Map val = (Map)pair.getValue();
            Iterator iter2 = val.entrySet().iterator();
            while(iter2.hasNext()){
                Map.Entry pair2 = (Map.Entry)iter2.next();
                String id2 = (String)pair2.getKey();
                Float value = (Float)pair2.getValue();
                float val1 =value;
                grpmat[Integer.parseInt(id)][Integer.parseInt(id2)]=(int) val1;

            }
        }
        return grpmat;
    }


    public void init_CPMAP(){
        Iterator iter  =  Query.entrySet().iterator();
        while(iter.hasNext()){
            Map.Entry pair = (Map.Entry) iter.next();
            query query_node = new query ((String)pair.getKey(),0);
            stringToQueryMap.put((String)pair.getKey(),query_node);
            List<query> child_related_node = new LinkedList<>();
            childrenMapToParents.put(query_node,child_related_node);
            List<query> parent_related_node = new LinkedList<>();
            parentsMapToChildren.put(query_node,parent_related_node);
        }
    }

    public patternContainmentMap buildPCM(){

        clique_helper();

        for(Set group: Cliques){
            hierarchyDetermination(group);

            displayPCM();

        }
        mergeIsomorphicNodes();
        //clique group ends
        displayPCM();
        patternContainmentMap pcm = new patternContainmentMap(childrenMapToParents,parentsMapToChildren);
        return pcm;

    }
    public void displayPCM(){
            System.out.println("Displaying PCM MAP child to Parent::");
            //Displaying childrenToParentMap
            Iterator c_iter = childrenMapToParents.entrySet().iterator();
            while(c_iter.hasNext()){
                Map.Entry pair = (Map.Entry) c_iter.next();
                query query_node = (query) pair.getKey();
                List<query> parent_list = (List<query>) pair.getValue();
                System.out.print("Q::"+query_node.ID+"[");
                for(query parentnode: parent_list){
                    System.out.print(parentnode.ID+",");
                }
                System.out.println("]");
            }
            System.out.println("Displaying PCM MAP Parent to child::");
            Iterator p_iter = parentsMapToChildren.entrySet().iterator();
            while(p_iter.hasNext()){
                Map.Entry pair = (Map.Entry) p_iter.next();
                query query_node = (query) pair.getKey();
                List<query> child_list = (List<query>) pair.getValue();
                System.out.print("Q::"+query_node.ID+"[");
                for(query childnode: child_list){
                    System.out.print(childnode.ID+",");
                }
                System.out.println("]");
            }

    }
    public void mergeIsomorphicNodes(){

        for(int iter=0;iter<newQueries.size();iter++){
            for(int jiter=iter+1;jiter<newQueries.size();jiter++){
                String id1=newQueries.get(iter);
                String id2 = newQueries.get(jiter);
                if(id1.equals(id2)){
                    newQueries.remove(jiter);
                    jiter--;
                }else {
                    HashMap<String, node> first_query = Query.get(id1);
                    HashMap<String, node> second_query = Query.get(id2);
                    HashMap<String, node> main_query;
                    HashMap<String, node> small_query;
                    if (first_query.size() > second_query.size()){
                        main_query = first_query;
                        small_query = second_query;
                    }
                    else{
                        main_query = second_query;
                        small_query = first_query;
                    }
                    if(equate(main_query,small_query)){
                        newQueries.remove(jiter);
                        Query.remove(id2);
                        jiter--;
                        query id1_querynode = stringToQueryMap.get(id1);
                        query id2_querynode = stringToQueryMap.get(id2);

                        //adding node2 child to node1
                        List<query> id2_childList = parentsMapToChildren.get(id2_querynode);
                        List<query> id1_childList = parentsMapToChildren.get(id1_querynode);
                        if(id2_childList.size()!=0 ) {
                            for (query childNode : id2_childList) {
                                List<query> childNodeList = childrenMapToParents.get(childNode);
                                childNodeList.remove(id2_querynode);
                                if (id1_childList.contains(childNode)) {
                                    continue;
                                } else {
                                    id1_childList.add(childNode);
                                }
                                if (childNodeList.contains(id1_querynode)) {
                                    continue;
                                } else {
                                    childNodeList.add(id1_querynode);
                                }
                            }
                        }


                        //
                        List<query> parentList = childrenMapToParents.get(id2_querynode);
                        if(parentList.size()!=0 ) {
                            for (query parentNode : parentList) {
                                List<query> childList = parentsMapToChildren.get(parentNode);
                                childList.remove(id2_querynode);
                                if (childList.contains(id1_querynode)) {
                                    continue;
                                } else {
                                    childList.add(id1_querynode);
                                }

                            }
                        }
                        parentsMapToChildren.remove(id2_querynode);
                        childrenMapToParents.remove(id2_querynode);



                    }

                }

            }
        }

    }


    public void hierarchyDetermination(Set<Integer> group){
        Set<Integer> NextLevelGroup = new HashSet<>();
        Set<Integer> group_copy = new HashSet<>(group);
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
        //Random rand = new Random();
        //int random_pair_index = rand.nextInt(disjointPairs.size());


        for(Set pair: disjointPairs){
            ArrayList<HashMap<String,node>> Qmcs;

            int small_q_id;
            int max_q_id;
            Iterator p_iter = pair.iterator();
            int q_id1=(int)p_iter.next();
            int q_id2=(int)p_iter.next();

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

            Qmcs = MCS(pair);
            //Displaying QMCS


            if(Qmcs!=null){
                System.out.println("Evaluating QMCS");
                for(HashMap<String, node> mcs : Qmcs ){
                    if(equate(small_graph,mcs)){
                        System.out.println("graph are equal");
                        NextLevelGroup.add(small_q_id);
                        query parent_node = stringToQueryMap.get(""+small_q_id);
                        query child_node = stringToQueryMap.get(""+max_q_id);
                        System.out.println("sg:"+small_q_id);
                        System.out.println("mg:"+max_q_id);
                        //Adding parent node in child hash map
                        List<query> childMap = childrenMapToParents.get(child_node);
                        childMap.add(parent_node);

                        //Adding child node in parent hash map
                        List<query> parentMap = parentsMapToChildren.get(parent_node);
                        parentMap.add(child_node);

                        displayPCM();

                    }else{
                        System.out.println("graphs are not equal");
                        System.out.println("ng:"+Pcm_node);
                        String new_node_id =""+ Pcm_node;
                        newQueries.add(new_node_id);
                        Query.put(new_node_id,mcs);
                        query newPCMNode = new query(new_node_id,0);
                        stringToQueryMap.put(new_node_id,newPCMNode);
                        NextLevelGroup.add(Pcm_node);
                        Pcm_node++;
                        query child_node1 = stringToQueryMap.get(""+small_q_id);
                        query child_node2 = stringToQueryMap.get(""+max_q_id);

                        System.out.println("sg:"+small_q_id);
                        System.out.println("mg:"+max_q_id);
                        List<query> child_related_node = new LinkedList<>();
                        childrenMapToParents.put(newPCMNode,child_related_node);
                        List<query> parent_related_node = new LinkedList<>();
                        parentsMapToChildren.put(newPCMNode,parent_related_node);



                        //Adding parent node in child hash map

                        List<query> childMap1 = childrenMapToParents.get(child_node1);
                        childMap1.add(newPCMNode);
                        List<query> childMap2 = childrenMapToParents.get(child_node2);
                        childMap2.add(newPCMNode);

                        //Adding child node in parent hash map
                        List<query> parentMap =  new LinkedList<>();
                        parentMap.add(child_node1);
                        parentMap.add(child_node2);
                        parentsMapToChildren.put(newPCMNode,parentMap);

                        displayPCM();
                    }
                }
            }
        }
        System.out.println("NextLevelGroup::");
        for(int nlg: NextLevelGroup){
            System.out.print(nlg+",");
        }
        System.out.println();
        if(NextLevelGroup.size()>1){
            hierarchyDetermination(NextLevelGroup);
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



        Set<Integer> pair = new HashSet<>();
        if (query_size==2){
                pair.add(temp_array[0]);
                pair.add(temp_array[1]);
                pair_list.add(pair);
        }
        else {
            for (int i = 0; i < query_size; i++) {
                if (pair.size() < 2) {
                    pair.add(temp_array[i]);
                    System.out.println(temp_array[i]);
                } else {
                    pair_list.add(pair);
                    pair = new HashSet<>();
                }

            }
        }
//        for(int i =0 ;i<query_size-1;i++){
//            Set<Integer> pair = new HashSet<>();
//            for (int j=i+1;j<query_size;j++){
//                pair.add(temp_array[i]);
//                pair.add(temp_array[j]);
//                pair_list.add(pair);
//                pair = new HashSet<>();
//            }
//
//        }

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