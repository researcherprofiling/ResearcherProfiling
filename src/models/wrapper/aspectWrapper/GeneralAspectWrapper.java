package models.wrapper.aspectWrapper;

import database.EmbeddedDB;
import models.schema.Schema;
import models.wrapper.GeneralWrapper;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


/*
* General schema for aspect wrappers.
* */
public abstract class GeneralAspectWrapper implements GeneralWrapper {

    public String name;
    protected Schema schema;
    protected boolean isValid;
    protected EmbeddedDB db;

    public GeneralAspectWrapper(EmbeddedDB db, String name) {
        this.name = name;
        this.schema = null;     //  Null since this class should not be instantiated
        this.isValid = false;   //  Not valid for the same reason
        this.db = db;
    }

    public boolean isValid() {
        return this.isValid;
    }

    public Schema getSchema() {
        return this.schema;
    }

    public abstract JSONObject getResultAsJSON(JSONObject searchConditions);

    public abstract JSONArray getRegisteredSources();

    public abstract JSONObject timedGetResultAsJSON(JSONObject searchConditions);

    public abstract JSONObject redoSearch(JSONObject searchConditions);

    public abstract boolean isActivated();

    public abstract void setActivation(String source, boolean newIsActive);

}
