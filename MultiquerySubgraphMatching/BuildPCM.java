import org.apache.commons.compress.archivers.sevenz.CLI;
import org.apache.lucene.codecs.blockterms.VariableGapTermsIndexWriter;
import scala.Int;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

/**
 * Created by ritvi on 4/13/2017.
 */


//Disjoint pairs





public class BuildPCM {
    //class attribute
    //map for the matrix
    //NExt level group==> set
    //Parsing of file
    HashMap<String, HashMap<String, node>> Query = new HashMap<>();
    Map<String,Map<String,Float>> Grp =new HashMap<String,Map<String,Float>>() {{
        put("0", new HashMap<String,Float>(){{put("0", Float.valueOf(0));put("1", Float.valueOf(0));put("2", Float.valueOf(1));put("3", Float.valueOf(1));put("4", Float.valueOf(0));}});
        put("1", new HashMap<String,Float>(){{put("0", Float.valueOf(0));put("1", Float.valueOf(0));put("2", Float.valueOf(0));put("3", Float.valueOf(0));put("4", Float.valueOf(0));}});
        put("2", new HashMap<String,Float>(){{put("0", Float.valueOf(1));put("1", Float.valueOf(0));put("2", Float.valueOf(0));put("3", Float.valueOf(1));put("4", Float.valueOf(1));}});
        put("3", new HashMap<String,Float>(){{put("0", Float.valueOf(1));put("1", Float.valueOf(0));put("2", Float.valueOf(1));put("3", Float.valueOf(0));put("4", Float.valueOf(1));}});
        put("4", new HashMap<String,Float>(){{put("0", Float.valueOf(0));put("1", Float.valueOf(0));put("2", Float.valueOf(1));put("3", Float.valueOf(1));put("4", Float.valueOf(0));}});
    }};


//    int[][] groupingMatrix ={{0,0,1,1,0},
//                             {0,0,0,0,0},
//                             {1,0,0,1,1},
//                             {1,0,1,0,1},
//                             {0,0,1,1,0}};


    //main function starts
    public static void main(String args[]) {
        BuildPCM qr = new BuildPCM();
        File database = null;
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
                    database = new File(input.nextLine());

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
                    database = new File(input.nextLine());

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
        PCM pc = new PCM(qr.Grp,qr.Query);

        patternContainmentMap pattternMap= pc.buildPCM();

        queryExecutionOrder qeo = new queryExecutionOrder(pattternMap.childrenMapToParents,pattternMap.parentsMapToChildren);
        qeo.findQueryExecOrder();
        qeo.printQueries(qeo.execOrder,"QueryExecutionOrder");

        embedding emb = new embedding(qr.Query,qeo.execOrder,pattternMap.childrenMapToParents,database);
        emb.executeSubgraphIsomorphism();

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