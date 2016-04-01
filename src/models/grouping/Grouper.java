package models.grouping;


import models.schema.Field;
import models.schema.Schema;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import utils.Constants;

import java.util.*;
import java.util.Map.Entry;

public class Grouper {

    public static JSONObject group(JSONArray records, Schema schema) {

        int numRecords = records.size();
        Field[] fields = schema.getAllFields();

        if(fields[0].fieldName.equalsIgnoreCase("name")){
            JSONObject ret = new JSONObject();
            ret.put("About the Researcher", records);
            return ret;
        }

        //  Computes keywords
        TreeMap keywords = new TreeMap<String, ArrayList<String>>();
        for (Field field : fields) {
            if (field.dataType.equalsIgnoreCase("Text") || field.dataType.equalsIgnoreCase("Location")) {
                Map<String, Integer> counts = new HashMap<>();
                for (Object record : records) {
                    JSONObject cur = (JSONObject)record;
                    if (cur.containsKey(field.fieldName)) {
                        String curVal = cur.getString(field.fieldName).trim();
                        String[] words = curVal.split("\\W");
                        for (String word : words) {
                            if (word.length() == 1) continue;
                            boolean ignored = false;
                            for (String ignore : Constants.kwIgnore) {
                                if (word.equalsIgnoreCase(ignore)) ignored = true;
                            }
                            if (ignored) continue;
                            if (counts.containsKey(word)) {
                                int c = counts.get(word);
                                counts.replace(word, c+1);
                            } else {
                                counts.put(word, 1);
                            }
                        }
                    }
                }

                ArrayList<String> kws = new ArrayList<>();
                Set ent = counts.entrySet();
                for (Object e : ent) {
                    Entry curEnt = (Entry) e;
                    if ((Integer)curEnt.getValue() * 10 >= numRecords) {
                        kws.add((String)curEnt.getKey());
                    }
                }
                keywords.put(field.fieldName, kws);
            }
        }

        //  Total number of keywords
        int numKWs = 0;
        for (Object e : keywords.entrySet()) {
            Entry<String, ArrayList<String>> ent = (Entry<String, ArrayList<String>>)e;
            numKWs += ent.getValue().size();
        }

        //  Transform the records into a new feature space
        int[][] newFeatures = new int[numRecords][numKWs];
        for (int i = 0; i < numRecords; i++) {
            int j = 0;
            for (Object e : keywords.entrySet()) {
                Entry<String, ArrayList<String>> ent = (Entry<String, ArrayList<String>>)e;
                JSONObject rec = (JSONObject)records.get(i);
                String curVal;
                if (rec.containsKey(ent.getKey())) curVal = rec.getString(ent.getKey());
                else curVal = "";
                for (String kw : ent.getValue()) {
                    newFeatures[i][j] = StringUtils.countMatches(curVal, kw);
                    j += 1;
                }
            }
        }

        //  From wikipedia: rule of thumb: # of clusters = sqrt(n/2)
        int numClusters = Math.min((int)Math.sqrt(((double)numRecords)/2), 5);

        //  K-mean clustering with L-1 norm as measure of distance
        Random rng = new Random();
        double[][] centroids = new double[numClusters][numKWs];
        double[] avg = new double[numKWs];
        for (int i = 0; i < numClusters; i++) {
            int[] from = newFeatures[Math.abs(rng.nextInt()) % numRecords];
            for (int j = 0; j < numKWs; j++) {
                centroids[i][j] = (double)(from[j]);
            }
        }
        int[] belongings = new int[numRecords];
        boolean shouldContinue = true;
        while (shouldContinue) {
            shouldContinue = false;

            int[] numBelongings = new int[numClusters];
            Arrays.fill(numBelongings, 0);
            //  Re-assign clustering
            for (int i = 0; i < numRecords; i++) {
                //  Compute closest centroid
                int closest = -1;
                double closestDist = Double.MAX_VALUE;
                for (int j = 0; j < numClusters; j++) {
                    double dist = 0;
                    for (int k = 0; k < numKWs; k++) {
                        dist += Math.abs(centroids[j][k] - newFeatures[i][k]);
                    }
                    if (dist < closestDist) {
                        closestDist = dist;
                        closest = j;
                    }
                }
                numBelongings[closest] += 1;
                if (belongings[i] != closest) {
                    belongings[i] = closest;
                    shouldContinue = true;
                }

            }

            if (!shouldContinue) {
                break;
            }

            //  Re-calculating centroids
            for (int i = 0; i < numClusters; i++) {
                Arrays.fill(centroids[i], 0.0);
            }
            for (int i = 0; i < numRecords; i++) {
                int c = belongings[i];
                for (int j = 0; j < numKWs; j++) {
                    centroids[c][j] += newFeatures[i][j];
                    avg[j] += newFeatures[i][j];
                }
            }
            for (int i = 0; i < numClusters; i++) {
                for (int j = 0; j < numKWs; j++) {
                    centroids[i][j] /= numBelongings[i];
                }
            }
            for (int i = 0; i < numKWs; i++) {
                avg[i] /= numRecords;
            }

        }

        //  Construct final grouped result
        JSONArray[] clusters = new JSONArray[numClusters];
        for (int i = 0; i < numClusters; i++) {
            clusters[i] = new JSONArray();
        }
        for (int i = 0; i < numRecords; i++) {
            int c = belongings[i];
            clusters[c].add(records.get(i));
        }

        JSONObject ret = new JSONObject();
        for (int i = 0; i < numClusters; i++) {
            //  Finding keywords
            int[] maxes = findMax(centroids[i], numKWs);
            int kw1_index = maxes[0];
            int kw2_index = maxes[1];
            int kw3_index = maxes[2];

            double[] diffFromMean = minus(centroids[i], avg, numKWs);
            maxes = findMax(diffFromMean, numKWs);
            int unique1_index = maxes[0];
            int unique2_index = maxes[1];
            int unique3_index = maxes[2];

            //  Convert back to String
            String kw1 = "", kw2 = "", kw3 = "", unique1 = "", unique2 = "", unique3 = "";
            int passed = 0;
            for (Object e : keywords.entrySet()) {
                Entry<String, ArrayList<String>> ent = (Entry<String, ArrayList<String>>)e;
                ArrayList<String> kws = ent.getValue();
                if (passed <= kw1_index && kw1_index < passed + kws.size()) {
                    kw1 = kws.get(kw1_index - passed);
                }
                if (passed <= kw2_index && kw2_index < passed + kws.size()) {
                    kw2 = kws.get(kw2_index - passed);
                }
                if (passed <= kw3_index && kw3_index < passed + kws.size()) {
                    kw3 = kws.get(kw3_index - passed);
                }
                if (passed <= unique1_index && unique1_index < passed + kws.size()) {
                    unique1 = kws.get(unique1_index - passed);
                }
                if (passed <= unique2_index && unique2_index < passed + kws.size()) {
                    unique2 = kws.get(unique2_index - passed);
                }
                if (passed <= unique3_index && unique3_index < passed + kws.size()) {
                    unique3 = kws.get(unique3_index - passed);
                }
                passed += kws.size();
            }

            ret.put(
                    kw1 + Constants.kwDelimiter + kw2 + Constants.kwDelimiter + kw3 + Constants.uniquekwDelimiter +
                    unique1 + Constants.kwDelimiter + unique2 + Constants.kwDelimiter + unique3,
                    clusters[i]);
        }

        return ret;
    }

    private static int[] findMax(double[] x, int numKWs) {
        int kw1_index = -1;
        int kw2_index = -1;
        int kw3_index = -1;
        double freq1 = Integer.MIN_VALUE;
        double freq2 = Integer.MIN_VALUE;
        double freq3 = Integer.MIN_VALUE;
        for (int j = 0; j < numKWs; j++) {
            double cur = x[j];
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
        }
        return new int[]{kw1_index, kw2_index, kw3_index};
    }

    private static double[] minus(double[] x, double[] y, int length) {
        double[] ret = new double[length];
        for (int i = 0; i < length; i++) {
            ret[i] = x[i] - y[i];
        }
        return ret;
    }

}





