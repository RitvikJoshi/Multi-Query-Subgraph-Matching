import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ritvi on 5/16/2017.
 */
public class patternContainmentMap {
    Map<query,List<query>> childrenMapToParents = new LinkedHashMap<>();
    Map<query,List<query>> parentsMapToChildren = new LinkedHashMap<>();

    patternContainmentMap(Map<query,List<query>> childrenMapToParents, Map<query,List<query>> parentsMapToChildren){
        this.childrenMapToParents=childrenMapToParents;
        this.parentsMapToChildren=parentsMapToChildren;
    }

}
