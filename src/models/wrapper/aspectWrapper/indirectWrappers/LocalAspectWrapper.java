package models.wrapper.aspectWrapper.indirectWrappers;

import database.EmbeddedDB;
import models.relevanceFiltering.RelevanceFilter;
import models.schema.Schema;
import models.wrapper.aspectWrapper.GeneralAspectWrapper;
import models.wrapper.sourceWrapper.GeneralSourceWrapper;
import models.wrapper.sourceWrapper.IndirectSourceWrapper;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Semaphore;

import static models.grouping.KMeans.group;
import static models.recordLinkage.BinrayClassification.link;


public class LocalAspectWrapper extends GeneralAspectWrapper {

    private Set<GeneralSourceWrapper> registeredSources;
    private RelevanceFilter filter;
    private JSONArray searchResult = new JSONArray();
    private Semaphore sem = new Semaphore(1);

    public LocalAspectWrapper(EmbeddedDB db, String aspectName) {
        super(db, aspectName);
        String aspectBasePath = basePath + "/" + aspectName;
        try {
            this.schema = Schema.readFromFile(aspectBasePath + "/schema.tsv");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        this.registeredSources = new HashSet<>();
        File[] sourceNames = (new File(aspectBasePath)).listFiles();
        if (sourceNames != null) {
            for (File source : sourceNames) {
                if (source.getName().startsWith(".") || !(source.isDirectory())) {
                    continue;
                }
                this.registeredSources.add(new IndirectSourceWrapper(
                        schema,
                        source.getName(),
                        this.name
                ));
            }
            File remoteList = new File(aspectBasePath + "/remoteServers.tsv");
            if (remoteList.exists() && remoteList.isFile()) {
                try {
                    Scanner scanner = new Scanner(new BufferedReader(new FileReader(remoteList.getAbsoluteFile())));
                    scanner.useDelimiter("\\n");
                    while (scanner.hasNext()) {
                        String line = scanner.next();
                        int tabIndex = line.indexOf('\t');
                        String sourceName = line.substring(0, tabIndex);
                        String sourceAddress = line.substring(tabIndex+1);
                        this.registeredSources.add(new IndirectSourceWrapper(
                                schema,
                                sourceName,
                                this.name
                        ));
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                    this.isValid = false;
                }
            }
            this.isValid = true;
        } else {
            this.isValid = false;
        }
        //  Add aspect table in DB if it does not exist
        db.createEmptyRecordTable(name);
        db.createCacheTable(name);
        db.createLinkageTable(name);
        filter = new RelevanceFilter(name, schema, db);
    }

    public void setActivation(String source, boolean newIsActive) {
        for (GeneralSourceWrapper registeredSource : registeredSources) {
            if (registeredSource.name.equals(source)) {
                registeredSource.isActive = newIsActive;
            }
        }
    }

    @Override
    public JSONArray getRegisteredSources() {
        Set<String> ret = new HashSet<String>();
        for (GeneralSourceWrapper registeredSource : this.registeredSources) {
            ret.add(registeredSource.name);
        }
        return JSONArray.fromObject(ret);
    }

    @Override
    public JSONObject getResultAsJSON(JSONObject searchConditions) {
        Vector<searchSource> workers = new Vector<>(10);
        for (GeneralSourceWrapper registeredSource : this.registeredSources) {
            workers.addElement(new searchSource(registeredSource, searchConditions));
        }
        if (workers.size() > 4) {
            workers.get(0).start();
            workers.get(1).start();
            workers.get(2).start();
            workers.get(3).start();
            for (int i = 4; i < workers.size(); i++) {
                workers.get(i - 4).join();
                workers.get(i).start();
            }
            workers.get(workers.size() - 4).join();
            workers.get(workers.size() - 3).join();
            workers.get(workers.size() - 2).join();
            workers.get(workers.size() - 1).join();
        }
        else {
            for (searchSource sa : workers)
                sa.start();
            for (searchSource sa : workers)
                sa.join();
        }
        JSONArray resultFromEachSource = searchResult;
//        This commented line is for the previous matcher.
//        JSONArray merged = countInclusion(this.schema, resultFromEachSource);
        JSONArray merged = link(resultFromEachSource, schema, db, searchConditions, name);
//        JSONArray merged = new JSONArray();
//        int N = searchResult.size();
//        if (N > 0) {
//            double[][] simVector = new double[N * (N - 1) / 2][schema.fields.size()];
//            double[] fieldSum = new double[schema.fields.size()];
//            double[] fieldCount = new double[schema.fields.size()];
//            int index = 0;
//            for (int i = 0; i < N - 1; i++) {
//                for (int j = i + 1; j < N; j++) {
//                    JSONObject ri = resultFromEachSource.getJSONObject(i);
//                    JSONObject rj = resultFromEachSource.getJSONObject(j);
//                    for (int k = 0; k < schema.fields.size(); k++) {
//                        Field f = schema.getAllFields()[k];
//                        if (ri.containsKey(f.fieldName) && rj.containsKey(f.fieldName)) {
//                            double sim = schema.fieldSimilarity(ri, rj, f);
//                            if (Double.isFinite(sim)) {
//                                simVector[index][k] = sim;
//                                fieldSum[k] += sim;
//                                fieldCount[k] += 1;
//                            }
//                        }
//                        else {
//                            simVector[index][k] = -1;
//                        }
//                    }
//                    index += 1;
//                }
//            }
//            for (int i = 0; i < N * (N - 1) / 2; i++) {
//                for (int k = 0; k < schema.fields.size(); k++) {
//                    if (fieldCount[k] > 0 && simVector[i][k] < 0) {
//                        simVector[i][k] = fieldSum[k] / fieldCount[k];
//                    }
//                    else if (simVector[i][k] < 0) {
//                        simVector[i][k] = 0;
//                    }
//                }
//            }
//            double[] mergability = EM.computeLabels(simVector);
//            boolean[] processed = new boolean[N];
//            while (true) {
//                Set<Integer> current = new HashSet<>();
//                int i = 0;
//                for (; i < N; i++) {
//                    if (!processed[i]) {
//                        processed[i] = true;
//                        current.add(i);
//                        break;
//                    }
//                }
//                if (current.isEmpty()) break;
//                else {
//                    for (i += 1; i < N; i++) {
//                        if (processed[i]) continue;
//                        for (int j : current) {
//                            int ind = j * (j - 2 * N + 1) / -2; //  summation of n-x-1 from x = 0 to x = j-1
//                            ind += i - j - 1;
//                            if (mergability[ind] >= 0.9) {
//                                current.add(i);
//                                processed[i] = true;
//                                break;
//                            }
//                        }
//                    }
//                    JSONObject mergedRec = null;
//                    for (Integer idx : current) {
//                        if (mergedRec == null) mergedRec = resultFromEachSource.getJSONObject(idx);
//                        else schema.merge(mergedRec, resultFromEachSource.getJSONObject(idx));
//                    }
//                    merged.add(mergedRec);
//                }
//            }
//        }
        JSONArray relevant = new JSONArray();
        JSONArray irrelevant = new JSONArray();
        filter.train(searchConditions);
        for (Object o : merged) {
            JSONObject rec = JSONObject.fromObject(o);
            if (filter.predict(rec)) relevant.add(rec);
            else irrelevant.add(rec);
        }
        JSONObject ret = new JSONObject();
        ret.put("irrelevant", schema.sort(irrelevant));
        ret.put("relevant", group(relevant, schema));
        return ret;
    }

    @Override
    public boolean isActivated() {
        for (GeneralSourceWrapper registeredSource : registeredSources) {
            if (registeredSource.isActive) {
                return true;
            }
        }
        return false;
    }

    @Override
    public JSONObject timedGetResultAsJSON(JSONObject searchConditions) {
        return getResultAsJSON(searchConditions);
    }

    @Override
    public JSONObject redoSearch(JSONObject searchConditions) {
        JSONArray resultFromEachSource = new JSONArray();
        for (GeneralSourceWrapper registeredSource : this.registeredSources) {
            if (registeredSource.isActive) {
                JSONArray result = db.getCachedResult(name, registeredSource.name, searchConditions);
                if (result != null) {
                    resultFromEachSource.addAll(result);
                }
            }
        }
        JSONArray merged = link(resultFromEachSource, schema, db, searchConditions, name);
        JSONArray relevant = new JSONArray();
        JSONArray irrelevant = new JSONArray();
        filter.train(searchConditions);
        for (Object o : merged) {
            JSONObject rec = JSONObject.fromObject(o);
            if (filter.predict(rec)) relevant.add(rec);
            else irrelevant.add(rec);
        }
        JSONObject ret = new JSONObject();
        ret.put("irrelevant", irrelevant);
        ret.put("relevant", group(relevant, schema));
        return ret;
    }

    @Override
    public void print() {
        System.out.println(name + ":");
        for (GeneralSourceWrapper registeredSource : registeredSources) {
            registeredSource.print();
        }
    }

    class searchSource implements Runnable{
        public Thread t;
        private GeneralSourceWrapper registeredSource;
        private JSONObject searchConditions;
        public searchSource(GeneralSourceWrapper regSrc,JSONObject cond){
            registeredSource= regSrc;
            searchConditions= cond;
        }
        public void start(){
            if (t == null)
            {
                t = new Thread (this,"th");
                t.start ();
            }
        }
        public void run() {
            if (registeredSource.isActive) {
                JSONArray temp = registeredSource.timedGetResultAsJSON(searchConditions);

                try {
                    sem.acquire();
                    if (temp != null && temp.size() > 0) {
                        db.clearCache(name, registeredSource.name, searchConditions);
                        db.cacheResult(name, registeredSource.name, searchConditions, temp);
                        searchResult.addAll(temp);
                    }
                    else {
                        temp = db.getCachedResult(name, registeredSource.name, searchConditions);
                        System.out.println(registeredSource.name + " returned empty result. Used cache: " + temp.size() + " record(s).");
                        searchResult.addAll(temp);
                    }
                    sem.release();
                } catch (InterruptedException e) {
                    System.out.println(registeredSource.name + "'s execution interrupted.");
                    e.printStackTrace();
                }


            }
        }

        public void join(){
            try {
//                System.out.println("Waiting for threads to finish.");
                t.join();
            } catch (InterruptedException e) {
                System.out.println("Main thread Interrupted");
            }
        }
    }

}
