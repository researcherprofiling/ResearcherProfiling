package models.schema;

import net.sf.json.JSONObject;

public abstract class Field <T> {

    public String fieldName;
    public boolean isPrimaryKey;
    public String dataType;

    public Field(String fieldName, boolean isPrimaryKey) {
        this.fieldName = fieldName;
        this.isPrimaryKey = isPrimaryKey;
    }

    public JSONObject extractValues(JSONObject record) {
        return record.getJSONObject(fieldName);
    }

    public abstract double equal(T o1, T o2);

    public abstract String convertToString(JSONObject record);

}
