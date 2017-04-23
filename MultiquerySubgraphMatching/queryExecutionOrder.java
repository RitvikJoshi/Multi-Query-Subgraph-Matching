/**
 * Created by adityapulekar on 4/23/17.
 */
import java.util.*;

public class queryExecutionOrder {
    query q1 = new query("q1",0);
    query q2 = new query("q2",0);
    query q3 = new query("q3",0);
    query q4 = new query("q4",0);
    query q5 = new query("q5",0);
    query q6 = new query("q6",0);
    query q7 = new query("q7",0);

    Map<query,List<query>> childrenMapToParents = new LinkedHashMap<query,List<query>>() {{
        put(q1,new LinkedList<query>(){{ add(q6);}});
        put(q2,new LinkedList<query>());
        put(q3,new LinkedList<query>(){{ add(q1); add(q7);}});
        put(q4,new LinkedList<query>(){{ add(q6); add(q7);}});
        put(q5,new LinkedList<query>(){{ add(q7);}});
        put(q6,new LinkedList<query>());
        put(q7,new LinkedList<query>());
    }};

    Map<query,List<query>> parentsMapToChildren = new LinkedHashMap<query,List<query>>() {{
        put(q1, new LinkedList<query>(){{ add(q3);}});
        put(q2,new LinkedList<query>());
        put(q3,new LinkedList<query>());
        put(q4,new LinkedList<query>());
        put(q5,new LinkedList<query>());
        put(q6,new LinkedList<query>(){{ add(q1); add(q4);}});
        put(q7,new LinkedList<query>(){{ add(q3); add(q4); add(q5);}});
    }};

    List<query> roots = new LinkedList<query>();
    List<query> execOrder = new LinkedList<query>();

    class query {
        String ID;
        int weight;

        public query(String id, int wt){
            ID = id;
            weight = wt;
        }
    }

    public List<query> findTheRoots(){
        //Return a list of queries that have no parents. So, we make use of the map --> 'childrenMapToParents'
        Iterator itr = childrenMapToParents.entrySet().iterator();
        while(itr.hasNext()){
            Map.Entry pairs = (Map.Entry) itr.next();
            if(((List<query>)pairs.getValue()).isEmpty()){
                roots.add((query)pairs.getKey());
            }
        }
        return roots;

    }

    public query chooseQueryWithHighestWeight(List<query> setOfAddedQueries){
        query highestWeightQuery = null; int highestWeight = Integer.MIN_VALUE;
        for(query testQuery : setOfAddedQueries){
            if(testQuery.weight > highestWeight){
                highestWeight = testQuery.weight;
                highestWeightQuery = testQuery;
            }
        }
        return highestWeightQuery;
    }

    public boolean checkForAddedParents(List<query> queries){
        for(query testQuery : queries){
            if(!execOrder.contains(testQuery))
                return false;
        }
        return true;
    }

    public query nextQueryGraph(List<query> queries){
        List<query> setOfAddedQueries = new LinkedList<query>();

        for(query testQuery : queries){
            if(!execOrder.contains(testQuery) && (checkForAddedParents(childrenMapToParents.get(testQuery)))){ //|| roots.contains(testQuery)
                setOfAddedQueries.add(testQuery);
            }
        }
        //printQueries(setOfAddedQueries, "Set of Added Queries");
        if(!setOfAddedQueries.isEmpty()){
            return chooseQueryWithHighestWeight(setOfAddedQueries);
        } else {
            return null;
        }
    }

    public void changeParentsWeight(query q){
        List<query> parentOf_q = childrenMapToParents.get(q);
        for(query testQuery : parentOf_q){
            if(!execOrder.contains(testQuery)){
                testQuery.weight++;
                changeParentsWeight(testQuery);
            }
        }
    }

    public void Topo(query q){

        if(!checkForAddedParents(childrenMapToParents.get(q))){ //!execOrder.isEmpty() &&
            changeParentsWeight(q);
        } else {
            //System.out.println("Query " + q.ID + " added to the execution order!" );
            execOrder.add(q);

            //printQueries(parentsMapToChildren.get(q),"Children queries of " + q.ID + " --> ");
            query q_prime = nextQueryGraph(parentsMapToChildren.get(q));
            while(q_prime != null){
                Topo(q_prime);
                q_prime = nextQueryGraph(parentsMapToChildren.get(q));
            }
        }
    }

    public void findQueryExecOrder(){
        roots = findTheRoots();
        //printQueries(roots, "ROOTS");
        query q = nextQueryGraph(roots);

        while(q != null){
            Topo(q);
            q = nextQueryGraph(roots);
        }
    }

    public void printQueries(List<query> queries, String toPrint){
        System.out.print(toPrint + " --> " );
        Iterator itr = queries.iterator();
        while(itr.hasNext()){
            System.out.print(((query)itr.next()).ID + "  ");
        }
        System.out.println();
    }

    public static void main(String[] args){
        queryExecutionOrder qeo = new queryExecutionOrder();
        qeo.findQueryExecOrder();
        qeo.printQueries(qeo.execOrder,"QueryExecutionOrder");
    }

}
