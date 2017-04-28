/**
 * Created by adityapulekar on 4/27/17.
 */
import org.neo4j.graphdb.RelationshipType;

public enum onlyRelationship_iGraph implements RelationshipType {
    ZERO(0);

    int relationLabel;

    onlyRelationship_iGraph(int label) {
        this.relationLabel = label;
    }
}

