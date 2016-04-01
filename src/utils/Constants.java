package utils;

public class Constants {

    public static final String fullSourceNameDelimiter = "__NICE_LITTLE_DELIMITER__";
    public static final String kwDelimiter = "__NICE_LITTLE_DELIMITER__";
    public static final String uniquekwDelimiter = "__NICE_LITTLE_UNIQUE_KW_DELIMITER__";
    public static final int localServerPortNumber = 8888;
    public static final int connectTimeout = 20000;  //  Timeout if connection has not been established after 5 seconds.
    public static final int readTimeout = 20000; //  Timeout if input has not been available after 5 seconds.
    public static final int sourceExecutionTimeout = 30000; //    Timeout if a source executed for more than 5 seconds without returning.
    public static final String[] kwIgnore = {"a", "an", "the", "is", "and", "of", "at", "from", "to", "or", "for",
            "in", "on", "onto", "into", "based", "with", "within", "journal", ",", ".", ":", ";", "-", "", " ", "  ",
            "   ", "    ", "\t", "\t\t", "\t\t\t", "la", "de", "da", "no", "et", "univ", "university", "college",
            "department", "dept", "school"};
    public static final String kwTableName = "KWS";

    public enum linkType{ ACADEMIC, HOMEPAGE, LINKEDIN, WIKIPEDIA, RESEARCHGATE, RATEMYPROFESSOR, OTHER}

    // Academic Keywords can be added here (Add more for finer results)
    public static final String[] academicKW = {"edu", "cs", "stanford", "illinois", "michigan", "mit", "harvard", "berkeley", "utexas", "ucla", "nyu", "wisc",
                                               "uwec", "uwm", "rutgers", "cmu", "uiuc", "gatech", "caltech", "yale", "princeton", "cornell", "upenn", "ox",
                                               "uchicago", "umich", "northwestern"};

    // LinkedIn related keywords can be added here
    public static final String[] linkedinKW = {"linkedin", "in"};

    // Research Gate releated keywords can be added here
    public static final String[] researchgateKW = {"researchgate"};

    // Content Based feature keywords can be added here
    public static final String[] contextKw = {"my", "I", "computer", "science", "research", "professor", "theory", "teach", "department", "university"};

}
