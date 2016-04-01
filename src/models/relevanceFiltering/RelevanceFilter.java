package models.relevanceFiltering;

import database.EmbeddedDB;
import models.schema.Field;
import models.schema.Schema;
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

public class RelevanceFilter {

    private String aspectName;
    private Schema schema;
    private EmbeddedDB db;
    private FilteredClassifier classifier;
    private Instances traininSet;

    public RelevanceFilter(String aspectName, Schema schema, EmbeddedDB db) {
        this.aspectName = aspectName;
        this.schema = schema;
        this.db = db;
    }

    public void train(JSONObject searchConditions) {
        JSONObject examples = db.getAllRecords(aspectName, searchConditions);
        Field[] fields = schema.getAllFields();
        if (fields.length == 0) return;
        FastVector attrs = new FastVector(fields.length + 1);
        String indices = "";
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            if (field.dataType.equalsIgnoreCase("Text") ||
                    field.dataType.equalsIgnoreCase("Location") ||
                    field.dataType.equalsIgnoreCase("Name")) {
                indices += Integer.toString(i+1) + ",";
                attrs.addElement(new Attribute(field.fieldName, (FastVector) null));
            } else {
                attrs.addElement(new Attribute(field.fieldName));
            }
        }
        final FastVector classValues = new FastVector(2);
        classValues.addElement("positive");
        classValues.addElement("negative");
        Attribute classLabel = new Attribute("class label", classValues);
        attrs.addElement(classLabel);
        traininSet = new Instances("trainingSet", attrs, examples.size());
        traininSet.setClass(classLabel);

        for (int i = 0; i < examples.getJSONArray("positive").size(); i++) {
            Instance toAdd = buildInstance(examples.getJSONArray("positive").getJSONObject(i));
            toAdd.setDataset(traininSet);
            toAdd.setClassValue("positive");
            traininSet.add(toAdd);
        }

        for (int i = 0; i < examples.getJSONArray("negative").size(); i++) {
            Instance toAdd = buildInstance(examples.getJSONArray("negative").getJSONObject(i));
            toAdd.setDataset(traininSet);
            toAdd.setClassValue("negative");
            traininSet.add(toAdd);
        }

        this.classifier = new FilteredClassifier();
        RandomForest base = new RandomForest();
        StringToWordVector filter = new StringToWordVector();
        filter.setOutputWordCounts(true);
        filter.setTFTransform(true);
        filter.setUseStoplist(true);
        filter.setStopwords(new File("utils/stopwords.txt"));
        if (indices.length() >= 0) filter.setAttributeIndices(indices.substring(0, indices.length()-1));
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

    }

    public boolean predict(JSONObject example) {
        Instance toAdd = buildInstance(example);
        try {
            return traininSet.classAttribute().value(
                    (int)classifier.classifyInstance(toAdd)
            ).equalsIgnoreCase("positive");
        } catch (Exception e) {
            return false;
        }
    }

    private Instance buildInstance (JSONObject example) {
        Field[] fields = schema.getAllFields();
        Instance ret = new Instance(fields.length + 1);
        ret.setDataset(traininSet);
        for (int j = 0; j < fields.length; j++) {
            if (example.containsKey(fields[j].fieldName)) {
                if (fields[j].dataType.equalsIgnoreCase("Text") ||
                        fields[j].dataType.equalsIgnoreCase("Location") ||
                        fields[j].dataType.equalsIgnoreCase("Name")) {
                    Object fieldValue = example.get(fields[j].fieldName);
                    if (fieldValue instanceof JSONObject) {
                        String temp = "";
                        for (Iterator iter = ((JSONObject) fieldValue).keys(); iter.hasNext();) {
                            temp += ((JSONObject) fieldValue).getString((String) iter.next());
                        }
                        ret.setValue(j, temp);
                    } else {
                        ret.setValue(j, example.getString(fields[j].fieldName));
                    }
                }
                else {
                    Object fieldValue = example.get(fields[j].fieldName);
                    if (fieldValue instanceof JSONObject) {
                        double temp = 0;
                        int count = 0;
                        for (Iterator iter = ((JSONObject) fieldValue).keys(); iter.hasNext();) {
                            temp += ((JSONObject) fieldValue).getDouble((String) iter.next());
                            count += 1;
                        }
                        ret.setValue(j, temp / count);
                    } else {
                        ret.setValue(j, example.getDouble(fields[j].fieldName));
                    }
                }
            }
            else {
                ret.setMissing(j);
            }
        }
        return ret;
    }

}
