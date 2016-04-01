package utils;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class Utilities {

    public static String readFileContent(String filePath) {
        BufferedReader fileReader;
        try {
            fileReader = new BufferedReader(new FileReader(filePath));
        } catch (Exception e) {
            System.out.println("Cannot read from file " + filePath);
            return null;
        }
        String datum = "";
        String line;
        try {
            while ((line = fileReader.readLine()) != null) {
                datum += line + "\n";
            }
        } catch (Exception e) {
            System.out.print(e.getMessage());
            return null;
        }
        return datum;
    }

    public static JSONObject readJSONObject(String filePath) {
        BufferedReader fileReader;
        try {
            fileReader = new BufferedReader(new FileReader(filePath));
        } catch (Exception e) {
            System.out.println("Cannot read from file " + filePath);
            return null;
        }
        String datum = "";
        String line;
        try {
            while ((line = fileReader.readLine()) != null) {
                datum += line;
            }
        } catch (Exception e) {
            System.out.print(e.getMessage());
            return null;
        }
        return JSONObject.fromObject(datum);
    }

    public static JSONArray readJSONArray(String filePath) {
        BufferedReader fileReader;
        try {
            fileReader = new BufferedReader(new FileReader(filePath));
        } catch (Exception e) {
            System.out.println("Cannot read from file " + filePath);
            return null;
        }
        String datum = "";
        String line;
        try {
            while ((line = fileReader.readLine()) != null) {
                datum += line;
            }
        } catch (Exception e) {
            System.out.print(e.getMessage());
            return null;
        }
        return JSONArray.fromObject(datum);
    }

    public static JSONArray readJSONArrayAndDelete(String filePath) {
        BufferedReader fileReader;
        try {
            fileReader = new BufferedReader(new FileReader(filePath));
        } catch (Exception e) {
            System.out.println("Cannot read from file " + filePath);
            return null;
        }
        String datum = "";
        String line;
        try {
            while ((line = fileReader.readLine()) != null) {
                datum += line;
            }
        } catch (Exception e) {
            System.out.print(e.getMessage());
            return null;
        }
        File f = new File(filePath);
        if (f.exists()) {
            f.delete();
        }
        return JSONArray.fromObject(datum);
    }

    public static String[] getNameTokens(String name) {
        name = name.trim();
        String first, last, middle;
        if (name.indexOf(',') >= 0) {
            int commaIndex = name.indexOf(',');
            last = name.substring(commaIndex).trim();
            first = name.substring(commaIndex+1).trim();
            int lastBlankIndex = first.lastIndexOf(' ');
            if (lastBlankIndex >= 0) {
                first = name.substring(0, lastBlankIndex);
                middle = name.substring(lastBlankIndex+1, name.length());
            }
            else middle = "";
        } else {
            String[] nameTokens = name.split(" ");
            if (nameTokens.length == 3) {
                first = nameTokens[0];
                middle = nameTokens[1];
                last = nameTokens[2];
            } else {
                first = nameTokens[0];
                last = nameTokens[nameTokens.length-1];
                middle = "";
            }
        }
        return new String[] {first, last, middle};
    }

    public static boolean generalComparison(Object o1, Object o2) {
        try {
            if (o1 instanceof String && o2 instanceof String) {
                return ((String)o1).trim().equalsIgnoreCase(((String)o2).trim());
            } else {
                return generalComparison(o1.toString(), o2.toString());
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static String constructFullSourceName(String aspectName, String sourceName) {
        return aspectName + Constants.fullSourceNameDelimiter + sourceName;
    }

}
