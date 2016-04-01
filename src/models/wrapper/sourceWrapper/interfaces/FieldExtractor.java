package models.wrapper.sourceWrapper.interfaces;

import net.sf.json.JSON;

/**
 * Standard interface for a field extractor
 */
public interface FieldExtractor {

    /*
    * Input: one sub-structure extracted by tuple extractor
    * Output: field value corresponding to a given field
    * */
    public Object extractField(JSON potentialTuple);

}
