import java.io.File;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by adityapulekar on 4/27/17.
 */
public class multiQueryOpt_testQueries {

    public static void main(String[] args) {
        formGroupMatrix grouping = new formGroupMatrix();
        String currentDirectory = System.getProperty("user.dir");
        File[] allFiles = new File(currentDirectory).listFiles();

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
        System.out.println("\n\nGroup Matrix (After Thresholding): \n" + grouping.groupMatrix);


        //Build the pattern containment map.
        //--MISSING---


        //Find the query execution order.
        queryExecutionOrder qeo = new queryExecutionOrder();
        qeo.findQueryExecOrder();
        qeo.printQueries(qeo.execOrder,"\nQueryExecutionOrder");


        //Perform Subgraph Isomorphism Search and cache the intermediate results.

    }
}