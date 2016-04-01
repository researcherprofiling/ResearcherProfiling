package models.grouping;

import models.schema.Field;
import models.schema.Schema;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import utils.Constants;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

public class KMeans {

    public static JSONObject group(JSONArray examples, Schema schema) {

        int numRecords = examples.size();
        Field[] fields = schema.getAllFields();
        if (fields.length == 0) return new JSONObject();
        FastVector attrs = new FastVector(fields.length);
        String indices = "";
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            if (field.dataType.equalsIgnoreCase("Text") ||
                    field.dataType.equalsIgnoreCase("Location") ||
                    field.dataType.equalsIgnoreCase("Name")) {
                indices += Integer.toString(i+1) + ",";
                attrs.addElement(new Attribute("FIELD: " + field.fieldName, (FastVector) null));
            } else {
                attrs.addElement(new Attribute("FIELD: " + field.fieldName));
            }
        }
        Instances dataSet = new Instances("trainingSet", attrs, examples.size());
        for (int i = 0; i < examples.size(); i++) {
            Instance toAdd = buildInstance(examples.getJSONObject(i), dataSet, schema);
            toAdd.setDataset(dataSet);
            dataSet.add(toAdd);
        }

        //  Convert to word vector
        StringToWordVector filter = new StringToWordVector();
        filter.setOutputWordCounts(true);
        filter.setLowerCaseTokens(true);
        filter.setTFTransform(true);
        filter.setUseStoplist(true);
        filter.setStopwords(new File("utils/stopwords.txt"));
        try {
            filter.setInputFormat(dataSet);
            Instances transformedDataSet = Filter.useFilter(dataSet, filter);
            SimpleKMeans kMeans = new SimpleKMeans();
            kMeans.setNumClusters((int)Math.sqrt(((double)numRecords)/2)+1);
            kMeans.buildClusterer(transformedDataSet);
            Instances centroids = kMeans.getClusterCentroids();
            double[] means = new double[transformedDataSet.numAttributes()];
            for (int i = 0; i < transformedDataSet.numAttributes(); i++) {
                means[i] = transformedDataSet.meanOrMode(i);
            }
            ArrayList<JSONArray> groups = new ArrayList<>();
            JSONObject ret = new JSONObject();
            for (int i = 0; i < centroids.numInstances(); i++) {
                groups.add(new JSONArray());
            }
            for (int i = 0; i < transformedDataSet.numInstances(); i++) {
                Instance cur = transformedDataSet.instance(i);
                groups.get(kMeans.clusterInstance(cur)).add(examples.get(i));
            }
            for (int i = 0; i < centroids.numInstances(); i++) {
                Instance cent = centroids.instance(i);
                int kw1_index = -1;
                int kw2_index = -1;
                int kw3_index = -1;
                int sp1_index = -1;
                int sp2_index = -1;
                int sp3_index = -1;
                double freq1 = Integer.MIN_VALUE;
                double freq2 = Integer.MIN_VALUE;
                double freq3 = Integer.MIN_VALUE;
                double sp_freq1 = Integer.MIN_VALUE;
                double sp_freq2 = Integer.MIN_VALUE;
                double sp_freq3 = Integer.MIN_VALUE;
                for (int j = 0; j < centroids.numAttributes(); j++) {
                    if (centroids.attribute(j).name().startsWith("FIELD: ")) continue;
                    double cur = cent.value(j);
                    double diff = cur - means[j];
                    if (cur > freq1) {
                        freq3 = freq2;
                        kw3_index = kw2_index;
                        freq2 = freq1;
                        kw2_index = kw1_index;
                        freq1 = cur;
                        kw1_index = j;
                    } else if (cur > freq2) {
                        freq3 = freq2;
                        kw3_index = kw2_index;
                        freq2 = cur;
                        kw2_index = j;
                    } else if (cur > freq3) {
                        freq3 = cur;
                        kw3_index = j;
                    }
                    if (diff > sp_freq1) {
                        sp_freq3 = sp_freq2;
                        sp3_index = sp2_index;
                        sp_freq2 = sp_freq1;
                        sp2_index = sp1_index;
                        sp_freq1 = diff;
                        sp1_index = j;
                    } else if (diff > sp_freq2) {
                        sp_freq3 = sp_freq2;
                        sp3_index = sp2_index;
                        sp_freq2 = cur;
                        sp2_index = j;
                    } else if (diff > sp_freq3) {
                        sp_freq3 = cur;
                        sp3_index = j;
                    }
                }
                ret.put(transformedDataSet.attribute(kw1_index).name() + Constants.kwDelimiter + transformedDataSet.attribute(kw2_index).name() + Constants.kwDelimiter + transformedDataSet.attribute(kw3_index).name()
                        + Constants.uniquekwDelimiter + transformedDataSet.attribute(sp1_index).name() + Constants.kwDelimiter + transformedDataSet.attribute(sp2_index).name() + Constants.kwDelimiter + transformedDataSet.attribute(sp3_index).name(),
                        groups.get(i));
            }
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return null; //should not happen.
        }


    }

    private static Instance buildInstance (JSONObject example, Instances dataSet, Schema schema) {
        Field[] fields = schema.getAllFields();
        Instance ret = new Instance(fields.length);
        ret.setDataset(dataSet);
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
