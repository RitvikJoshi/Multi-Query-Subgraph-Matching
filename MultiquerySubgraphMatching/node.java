import java.util.ArrayList;

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