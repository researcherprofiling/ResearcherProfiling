package models.recordLinkage;

import database.EmbeddedDB;
import models.schema.Field;
import models.schema.Schema;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.io.File;
import java.util.Iterator;

public class BinrayClassification {

    public static JSONArray link(JSONArray records, Schema schema, EmbeddedDB db, JSONObject searchConditions, String aspect) {
        int numRecords = records.size();
        Field[] fields = schema.getAllFields();
        if (fields.length == 0 || numRecords == 0) return new JSONArray();
        FastVector attrs = new FastVector(fields.length+1);
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            if (field.dataType.equalsIgnoreCase("Text") ||
                    field.dataType.equalsIgnoreCase("Location") ||
                    field.dataType.equalsIgnoreCase("Name")) {
                attrs.addElement(new Attribute("RECORD1_FIELD: " + field.fieldName, (FastVector) null));
                attrs.addElement(new Attribute("RECORD2_FIELD: " + field.fieldName, (FastVector) null));
            } else {
                attrs.addElement(new Attribute("RECORD1_FIELD: " + field.fieldName));
                attrs.addElement(new Attribute("RECORD2_FIELD: " + field.fieldName));
            }
        }

        JSONArray previousResults = db.getCachedLinkageResult(aspect, searchConditions);

        final FastVector classValues = new FastVector(2);
        classValues.addElement("positive");
        classValues.addElement("negative");
        Attribute classLabel = new Attribute("class label", classValues);
        attrs.addElement(classLabel);
        Instances traininSet = new Instances("trainingSet", attrs, previousResults.size());
        traininSet.setClass(classLabel);

        for (int i = 0; i < previousResults.getJSONArray(0).size(); i++) {
            Instance toAdd = buildInstance(previousResults.getJSONArray(0).getJSONArray(i), traininSet, schema);
            toAdd.setDataset(traininSet);
            toAdd.setClassValue("positive");
            traininSet.add(toAdd);
        }
        for (int i = 0; i < previousResults.getJSONArray(1).size(); i++) {
            Instance toAdd = buildInstance(previousResults.getJSONArray(1).getJSONArray(i), traininSet, schema);
            toAdd.setDataset(traininSet);
            toAdd.setClassValue("negative");
            traininSet.add(toAdd);
        }

        FilteredClassifier classifier = new FilteredClassifier();
        RandomForest base = new RandomForest();
        StringToWordVector filter = new StringToWordVector();
        filter.setOutputWordCounts(true);
        filter.setTFTransform(true);
        filter.setUseStoplist(true);
        filter.setStopwords(new File("utils/stopwords.txt"));
        classifier.setFilter(filter);
        classifier.setClassifier(base);

        if (traininSet.numInstances() > 0) {
            try {
                classifier.buildClassifier(traininSet);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Failed to train classifier.");
            }
        } else {
            classifier = null;
        }

        try {
            JSONArray ret = new JSONArray();
            while (!records.isEmpty()) {
                JSONObject curRec = (JSONObject)(records.remove(0));
                JSONObject toMerge = null;
                int i = 0;
                for (; i < ret.size(); i++) {
                    JSONObject other = ret.getJSONObject(i);
                    JSONArray temp = new JSONArray();
                    temp.add(curRec);
                    temp.add(other);
                    if (traininSet.classAttribute().value(
                            (int)classifier.classifyInstance(buildInstance(temp, traininSet, schema))
                    ).equalsIgnoreCase("positive")) {
                        toMerge = other;
                        break;
                    }
                }
                if (i >= ret.size()) {
                    ret.add(curRec);
                }
                else {
                    ret.remove(i);
                    schema.merge(curRec, toMerge);
                    records.add(curRec);
                }
            }
            return ret;
        }
        catch (NullPointerException e) {
            return records;
        }
        catch (Exception e) {
            e.printStackTrace();
            return records;
        }
    }

    private static Instance buildInstance (JSONArray example, Instances dataSet, Schema schema) {
        Field[] fields = schema.getAllFields();
        Instance ret = new Instance(fields.length * 2 + 1);
        ret.setDataset(dataSet);
        JSONObject e1 = example.getJSONObject(0);
        JSONObject e2 = example.getJSONObject(1);
        for (int j = 0; j < fields.length; j++) {
            if (e1.containsKey(fields[j].fieldName)) {
                if (fields[j].dataType.equalsIgnoreCase("Text") ||
                        fields[j].dataType.equalsIgnoreCase("Location") ||
                        fields[j].dataType.equalsIgnoreCase("Name")) {
                    Object fieldValue = e1.get(fields[j].fieldName);
                    if (fieldValue instanceof JSONObject) {
                        String temp = "";
                        for (Iterator iter = ((JSONObject) fieldValue).keys(); iter.hasNext();) {
                            temp += ((JSONObject) fieldValue).getString((String) iter.next());
                        }
                        ret.setValue(2*j, temp);
                    } else {
                        ret.setValue(2*j, e1.getString(fields[j].fieldName));
                    }
                }
                else {
                    Object fieldValue = e1.get(fields[j].fieldName);
                    if (fieldValue instanceof JSONObject) {
                        double temp = 0;
                        int count = 0;
                        for (Iterator iter = ((JSONObject) fieldValue).keys(); iter.hasNext();) {
                            temp += ((JSONObject) fieldValue).getDouble((String) iter.next());
                            count += 1;
                        }
                        ret.setValue(2*j, temp / count);
                    } else {
                        ret.setValue(2*j, e1.getDouble(fields[j].fieldName));
                    }
                }
            }
            else {
                ret.setMissing(2*j);
            }
            if (e2.containsKey(fields[j].fieldName)) {
                if (fields[j].dataType.equalsIgnoreCase("Text") ||
                        fields[j].dataType.equalsIgnoreCase("Location") ||
                        fields[j].dataType.equalsIgnoreCase("Name")) {
                    Object fieldValue = e2.get(fields[j].fieldName);
                    if (fieldValue instanceof JSONObject) {
                        String temp = "";
                        for (Iterator iter = ((JSONObject) fieldValue).keys(); iter.hasNext();) {
                            temp += ((JSONObject) fieldValue).getString((String) iter.next());
                        }
                        ret.setValue(2*j+1, temp);
                    } else {
                        ret.setValue(2*j+1, e2.getString(fields[j].fieldName));
                    }
                }
                else {
                    Object fieldValue = e2.get(fields[j].fieldName);
                    if (fieldValue instanceof JSONObject) {
                        double temp = 0;
                        int count = 0;
                        for (Iterator iter = ((JSONObject) fieldValue).keys(); iter.hasNext();) {
                            temp += ((JSONObject) fieldValue).getDouble((String) iter.next());
                            count += 1;
                        }
                        ret.setValue(2*j+1, temp / count);
                    } else {
                        ret.setValue(2*j+1, e2.getDouble(fields[j].fieldName));
                    }
                }
            }
            else {
                ret.setMissing(2*j+1);
            }
        }
        return ret;
    }

}
