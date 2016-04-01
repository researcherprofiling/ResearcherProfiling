package models.schema;

import models.schema.fields.IntegerField;
import models.schema.fields.LongIntegerField;
import models.schema.fields.NameField;
import models.schema.fields.TextField;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Schema {

    public ArrayList<Field> fields;
    public int numPKs;

    public Schema() {
        this.fields = new ArrayList<>(0);
        this.numPKs = 0;
    }

    public void addField(Field f) {
        this.fields.add(f);
        if (f.isPrimaryKey) this.numPKs += 1;
    }

    public Field[] getPrimaryKeys() {
        Field[] pk = new Field[numPKs];
        int i = 0;
        for (Field f : this.fields) {
            if (f.isPrimaryKey) {
                pk[i++] = f;
            }
        }
        return pk;
    }

    public JSONArray toJSONArray() {
        JSONArray ret = new JSONArray();
        for (Field field : fields) {
            JSONObject f = new JSONObject();
            f.put("name", field.fieldName);
            f.put("isPK", field.isPrimaryKey);
            f.put("syntax", field.dataType);
            ret.add(f);
        }
        return ret;
    }

    public static Schema fromJSONArray(JSONArray datum) {
        Schema ret = new Schema();
        for (Object o : datum) {
            JSONObject field = (JSONObject)o;
            switch (field.getString("syntax").toLowerCase()) {
                case "text":
                    ret.addField(new TextField(field.getString("name"), field.getBoolean("isPK")));
                    break;
                case "integer":
                    ret.addField(new IntegerField(field.getString("name"), field.getBoolean("isPK")));
                    break;
                case "longinteger":
                    ret.addField(new LongIntegerField(field.getString("name"), field.getBoolean("isPK")));
                    break;
                case "name":
                    ret.addField(new NameField(field.getString("name"), field.getBoolean("isPK")));
                    break;
            }
        }
        return ret;
    }

    public Field[] getAllFields() {
        Field[] ret = new Field[fields.size()];
        for (int i = 0; i < fields.size(); i++) {
            ret[i] = fields.get(i);
        }
        return ret;
    }

    //  Constructing Schema from file
    public static Schema readFromFile(String filepath) throws Exception {
        //  Initialize reader
        BufferedReader fileReader = null;
        boolean validReader;
        try {
            fileReader = new BufferedReader(new FileReader(filepath));
            validReader = true;
        } catch (Exception e) {
            validReader = false;
        }
        if (!validReader) throw new Exception("Cannot read from file "+filepath);

        //  Read header
        String line = null;
        boolean readLine;
        String[] headers = null;
        try {
            line = fileReader.readLine();
            readLine = true;
        } catch (Exception e) {
            readLine = false;
        }

        if (!readLine) throw new Exception("Cannot read header from file "+filepath);

        headers = line.split("\\t");

        // Read content
        Schema ret = new Schema();
        try {
            while ((line = fileReader.readLine()) != null) {
                String[] fields = line.split("\\t");
                if (fields.length != headers.length || fields.length <= 2) {
                    return ret;
                } else {
                    switch (fields[2].toLowerCase()) {
                        case "text":
                            ret.addField(new TextField(fields[0], fields[1].equalsIgnoreCase("yes")));
                            break;
                        case "integer":
                            ret.addField(new IntegerField(fields[0], fields[1].equalsIgnoreCase("yes")));
                            break;
                        case "longinteger":
                            ret.addField(new LongIntegerField(fields[0], fields[1].equalsIgnoreCase("yes")));
                            break;
                        case "name":
                            ret.addField(new NameField(fields[0], fields[1].equalsIgnoreCase("yes")));
                            break;
                    }
                }
            }
            return ret;
        } catch (Exception e) {/* Nothing */}
        throw new Exception("Cannot read fields from file "+filepath);
    }

    public double fieldSimilarity(JSONObject record1, JSONObject record2, Field field) {
        String fieldName = field.fieldName;
        Object rec1 = record1.get(fieldName);
        Object rec2 = record2.get(fieldName);
        if (rec1 instanceof JSONObject) {
            double innerSum = 0;
            int innerCount = 0;
            for (Iterator kiter = ((JSONObject) rec1).keys(); kiter.hasNext(); ) {
                Object k1 = ((JSONObject) rec1).get(kiter.next());
                if (rec2 instanceof JSONObject) {
                    for (Iterator k2iter = ((JSONObject) rec2).keys(); k2iter.hasNext(); ) {
                        innerSum += field.equal(
                                k1,
                                ((JSONObject) rec2).get(k2iter.next())
                        );
                        innerCount += 1;
                    }
                } else {
                    innerSum += field.equal(k1, rec2);
                    innerCount += 1;
                }
            }
            if (innerCount == 0) return 0;
            else return innerSum / innerCount;
        } else {
            if (rec2 instanceof JSONObject) {
                double innerSum = 0;
                int innerCount = 0;
                for (Iterator k2iter = ((JSONObject) rec2).keys(); k2iter.hasNext(); ) {
                    innerSum += field.equal(rec1, ((JSONObject) rec2).get(k2iter.next()));
                    innerCount += 1;
                }
                if (innerCount == 0) return 0;
                else return innerSum / innerCount;
            } else {
                return field.equal(rec1, rec2);
            }
        }
    }

    public double mergeable(JSONObject record1, JSONObject record2) {
        /**
         * Set of rules to determine whether the records match
         */
        double sum = 0;
        int count = 0;
        for (Field field : this.fields) {
            String fieldName = field.fieldName;
            if (field.isPrimaryKey && record1.containsKey(fieldName) && record2.containsKey(fieldName)) {
                double sim = fieldSimilarity(record1, record2, field);
                if (!Double.isNaN(sim)) {
                    sum += Double.isFinite(sim) ? sim : 1;
                    count += 1;
                }
            }
        }
        if (count == 0) return 0;
        else return sum / count;
    }

    public void merge(JSONObject curResolvedRec, JSONObject rec) {
        for (Field field : fields) {
            String fieldName = field.fieldName;
            if (!curResolvedRec.containsKey(fieldName) && rec.containsKey(fieldName)) {
                curResolvedRec.put(fieldName, rec.get(fieldName));
            } else if (curResolvedRec.containsKey(fieldName) && rec.containsKey(fieldName)) {
                Object resolvedField = curResolvedRec.get(fieldName);
                Object newField = rec.get(fieldName);
                ((JSONObject) resolvedField).putAll((JSONObject)newField);
                curResolvedRec.replace(fieldName, resolvedField);
            }
        }
        Set<String> unique = new HashSet<>();
        for (String s : curResolvedRec.getString("Provided by").split(", ")) {
            unique.add(s);
        }
        for (String s : rec.getString("Provided by").split(", ")) {
            unique.add(s);
        }
        String newInclusion = "";
        for (String s : unique) {
            newInclusion += s + ", ";
        }
        newInclusion = newInclusion.substring(0, newInclusion.length()-2);
        curResolvedRec.put("Provided by", newInclusion);
        JSONArray oldORs = curResolvedRec.getJSONArray("Original record(s)");
        oldORs.addAll(rec.getJSONArray("Original record(s)"));
        curResolvedRec.replace("Original record(s)", oldORs);
    }

}
