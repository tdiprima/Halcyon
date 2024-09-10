package com.ebremer.halcyon.server.utils;

import com.ebremer.halcyon.lib.OperatingSystemInfo;
import com.ebremer.ns.HAL;
import com.ebremer.ns.LDP;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.rdf.model.Literal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A singleton class that manages configuration settings for the web application,
 * including file paths, server ports, resource mappings, and various other parameters, 
 * loading from an RDF model or generating default settings if the configuration file is not found.
 * 
 * @author erich
 */
public final class HalcyonSettings {

    private final String webfiles = "/ib";
    private final long MaxAgeReaderPool = 600;
    private final long ReaderPoolScanDelay = 600;
    private final long ReaderPoolScanRate = 60;
    private static HalcyonSettings settings = null;
    private Model m;
    private final Property urlpathprefix;
    private final Property MULTIVIEWERLOCATION;
    private final Property TALONLOCATION;
    private final Property ZEPHYRLOCATION;
    private static final String MasterSettingsLocation = "settings.ttl";
    private Resource Master;
    private final HashMap<String, String> http2fileMappings;
    private final HashMap<String, String> file2httpMappings;
    private final String Realm = "master";
    public static final String realm = "Halcyon";
    public static final int DEFAULTHTTPPORT = 8888;
    public static final int DEFAULTHTTPSPORT = 9999;
    public static final int DEFAULTSPARQLPORT = 8887;
    public static final int DEFAULTFILEPROCESSORTHREEADS = 4;
    public static final String DEFAULTHOSTNAME = "http://localhost";
    public static final String DEFAULTHOSTIP = "0.0.0.0";
    public static final String VERSION = "1.1.0";
    public static Resource HALCYONAGENT = ResourceFactory.createResource(HAL.NS + "VERSION/" + VERSION);
    public static String HALCYONSOFTWARE = "Halcyon Version " + VERSION;
    private String mode;
    private static final Logger logger = LoggerFactory.getLogger(HalcyonSettings.class);

    private HalcyonSettings() {
        File f = new File(MasterSettingsLocation);
        http2fileMappings = new HashMap<>();
        file2httpMappings = new HashMap<>();
        if (!f.exists()) {
            System.out.println("no config file found!");
            GenerateDefaultSettings();
        } else {
            System.out.println("loading configuration file : " + MasterSettingsLocation);
            // Load the RDF model from the settings.ttl file
            m = RDFDataMgr.loadModel(MasterSettingsLocation, Lang.TTL);
            System.out.println("# of triples " + m.size());
            GetMasterID();
        }
        // Define the properties for the settings
        urlpathprefix = m.createProperty(HAL.NS + "urlpathprefix");
        MULTIVIEWERLOCATION = m.createProperty(HAL.NS + "MultiviewerLocation");
        TALONLOCATION = m.createProperty(HAL.NS + "TalonLocation");
        ZEPHYRLOCATION = m.createProperty(HAL.NS + "ZephyrLocation");
        
        try {
            Property modeProperty = m.createProperty(HAL.NS + "mode");

            // Get the settings resource
            Resource settingsResource = m.getResource(DEFAULTHOSTNAME);

            // Extract the mode
            if (settingsResource.hasProperty(modeProperty)) {
                Literal modeLiteral = settingsResource.getProperty(modeProperty).getLiteral();
                mode = modeLiteral.getString();
                logger.info("Mode loaded from settings: " + mode);
            } else {
                logger.warn("Mode not found in settings, defaulting to 'release'");
                mode = "release";
            }

        } catch (Exception e) {
            logger.error("Error loading settings", e);
            mode = "release"; // Default to release if there's an error
        }
    }

    public String getwebfiles() {
        return webfiles;
    }

    public String getVersion() {
        return VERSION;
    }

    public String getRealm() {
        return Realm;
    }

    public String getHostName() {
        String qs = "prefix : <" + HAL.NS + "> select ?HostName where {?s :HostName ?HostName}";
        Query query = QueryFactory.create(qs);
        QueryExecution qe = QueryExecutionFactory.create(query, m);
        ResultSet results = qe.execSelect();
        if (results.hasNext()) {
            QuerySolution sol = results.nextSolution();
            return sol.get("HostName").asLiteral().getString();
        }
        return DEFAULTHOSTNAME + ":" + DEFAULTHTTPPORT;
    }

    public String getProxyHostName() {
        String qs = "prefix : <" + HAL.NS + "> select ?ProxyHostName where {?s :ProxyHostName ?ProxyHostName}";
        Query query = QueryFactory.create(qs);
        QueryExecution qe = QueryExecutionFactory.create(query, m);
        ResultSet results = qe.execSelect();
        if (results.hasNext()) {
            QuerySolution sol = results.nextSolution();
            return sol.get("ProxyHostName").asLiteral().getString();
        }
        return DEFAULTHOSTNAME + ":" + DEFAULTHTTPPORT;
    }

    public String getAuthServer() {
        ParameterizedSparqlString pss = new ParameterizedSparqlString("select ?AuthServer where {?s :AuthServer ?AuthServer}");
        pss.setNsPrefix("", HAL.NS);
        QueryExecution qe = QueryExecutionFactory.create(pss.toString(), m);
        ResultSet results = qe.execSelect();
        if (results.hasNext()) {
            return results.nextSolution().get("AuthServer").asLiteral().getString();
        }
        return DEFAULTHOSTNAME + ":" + DEFAULTHTTPPORT;
    }

    public boolean isDevMode() {
        return "dev".equalsIgnoreCase(mode);
//        ParameterizedSparqlString pss = new ParameterizedSparqlString("ask where {?s :devmode true; a :HalcyonSettingsFile}");
//        pss.setNsPrefix("", HAL.NS);
//        return QueryExecutionFactory.create(pss.toString(), m).execAsk();
    }

    public long getMaxAgeReaderPool() {
        return MaxAgeReaderPool;
    }

    public long getReaderPoolScanDelay() {
        return ReaderPoolScanDelay;
    }

    public long getReaderPoolScanRate() {
        return ReaderPoolScanRate;
    }

    public boolean IsFileScanDisabled() {
        ParameterizedSparqlString pss = new ParameterizedSparqlString("ask where {?s hal:fileScanDisabled true}");
        pss.setNsPrefix("hal", HAL.NS);
        return QueryExecutionFactory.create(pss.toString(), m).execAsk();
    }

    public static HalcyonSettings getSettings() {
        if (settings == null) {
            settings = new HalcyonSettings();
        }
        return settings;
    }

    public void GenerateDefaultSettings() {
        m = ModelFactory.createDefaultModel();
        Master = m.createResource(DEFAULTHOSTNAME);
        m.setNsPrefix("", HAL.NS);
        m.add(Master, RDF.type, HAL.HalcyonSettingsFile);
        m.add(Master, HAL.RDFStoreLocation, "tdb2");
        m.addLiteral(Master, HAL.SPARQLport, DEFAULTSPARQLPORT);
        try {
            RDFDataMgr.write(new FileOutputStream(MasterSettingsLocation), m, Lang.TTL);
        } catch (FileNotFoundException ex) {
            logger.error("Error in GenerateDefaultSettings", ex);
        }
    }

    public String GetMasterID() {
        String qs = "prefix : <" + HAL.NS + "> select ?s where {?s a :HalcyonSettingsFile}";
        Query query = QueryFactory.create(qs);
        QueryExecution qe = QueryExecutionFactory.create(query, m);
        ResultSet results = qe.execSelect();
        if (results.hasNext()) {
            QuerySolution sol = results.nextSolution();
            Master = sol.get("s").asResource();
            return Master.getURI();
        }
        return null;
    }

    public int GetNumberOfFileProcessorThreads() {
        ParameterizedSparqlString pss = new ParameterizedSparqlString("select ?threads where {?s :fileProcessors ?threads}");
        pss.setNsPrefix("", HAL.NS);
        QueryExecution qe = QueryExecutionFactory.create(pss.toString(), m);
        ResultSet results = qe.execSelect();
        if (results.hasNext()) {
            QuerySolution sol = results.nextSolution();
            return sol.get("threads").asLiteral().getInt();
        }
        return DEFAULTFILEPROCESSORTHREEADS;
    }

    public int GetSPARQLPort() {
        ParameterizedSparqlString pss = new ParameterizedSparqlString("select ?port where {?s :SPARQLport ?port}");
        pss.setNsPrefix("", HAL.NS);
        QueryExecution qe = QueryExecutionFactory.create(pss.toString(), m);
        ResultSet results = qe.execSelect();
        if (results.hasNext()) {
            QuerySolution sol = results.nextSolution();
            return sol.get("port").asLiteral().getInt();
        }
        return DEFAULTSPARQLPORT;
    }

    public String GetHostIP() {
        ParameterizedSparqlString pss = new ParameterizedSparqlString("select ?ip where {?s ?p ?ip}");
        pss.setNsPrefix("", HAL.NS);
        pss.setIri("p", HAL.HostIP.getURI());
        QueryExecution qe = QueryExecutionFactory.create(pss.toString(), m);
        ResultSet results = qe.execSelect();
        if (results.hasNext()) {
            QuerySolution sol = results.next();
            return sol.get("ip").asLiteral().getString();
        }
        return DEFAULTHOSTIP;
    }

    public List<ResourceHandler> GetResourceHandlers() {
        ParameterizedSparqlString pss = new ParameterizedSparqlString(
                """
                select distinct ?resourceBase ?urlPath where {
                    ?s :hasResourceHandler [ a ldp:Container; :resourceBase ?resourceBase ; :urlPath ?urlPath ]
                }
                """
        );
        pss.setNsPrefix("", HAL.NS);
        pss.setNsPrefix("ldp", LDP.NS);
        QueryExecution qe = QueryExecutionFactory.create(pss.toString(), m);
        ResultSet results = qe.execSelect();
        ArrayList<ResourceHandler> list = new ArrayList<>();
        while (results.hasNext()) {
            QuerySolution sol = results.next();
            try {
                URI srcbase = new URI(sol.get("resourceBase").asResource().getURI());
                ResourceHandler rh = new ResourceHandler(srcbase, sol.get("urlPath").asLiteral().getString());
                list.add(rh);
                if (!http2fileMappings.containsKey(rh.urlPath())) {
                    URI whoa = rh.resourceBase();
                    String temp = whoa.getPath();
                    String correctedOSPath = OperatingSystemInfo.ifWindows() ? temp.substring(1) : temp;
                    http2fileMappings.put(rh.urlPath(), correctedOSPath);
                    file2httpMappings.put(correctedOSPath, rh.urlPath());
                }
            } catch (URISyntaxException ex) {
                logger.error("Error in GetResourceHandlers", ex);
            }
        }
        return list;
    }

    public List<String> getRootContainers() {
        ParameterizedSparqlString pss = new ParameterizedSparqlString(
                """
                select distinct ?urlPath where {
                    ?s :hasResourceHandler [ a ldp:Container; :resourceBase ?resourceBase ; :urlPath ?urlPath ]
                } order by ?urlPath
                """
        );
        pss.setNsPrefix("", HAL.NS);
        pss.setNsPrefix("ldp", LDP.NS);
        QueryExecution qe = QueryExecutionFactory.create(pss.toString(), m);
        ResultSet results = qe.execSelect().materialise();
        boolean ha = results.hasNext();
        ArrayList<String> list = new ArrayList<>();
        results.forEachRemaining(qs -> {
            String key = qs.get("urlPath").asLiteral().getString();
            Iterator<String> i = list.stream().iterator();
            boolean add = true;
            while (i.hasNext()) {
                add = !key.startsWith(i.next());
            }
            if (add) {
                list.add(key);
            }
        });
        return list;
    }

    public int GetHTTPPort() {
        String qs = "prefix : <" + HAL.NS + "> select ?port where {?s :HTTPPort ?port}";
        Query query = QueryFactory.create(qs);
        QueryExecution qe = QueryExecutionFactory.create(query, m);
        ResultSet results = qe.execSelect();
        if (results.hasNext()) {
            QuerySolution sol = results.nextSolution();
            return sol.get("port").asLiteral().getInt();
        }
        return DEFAULTHTTPPORT;
    }

    public boolean isHTTPS2enabled() {
        ParameterizedSparqlString pss = new ParameterizedSparqlString("ask where {?s hal:HTTPS2enabled true}");
        pss.setNsPrefix("hal", HAL.NS);
        return QueryExecutionFactory.create(pss.toString(), m).execAsk();
    }

    public boolean isHTTPS3enabled() {
        ParameterizedSparqlString pss = new ParameterizedSparqlString("ask where {?s hal:HTTPS3enabled true}");
        pss.setNsPrefix("hal", HAL.NS);
        return QueryExecutionFactory.create(pss.toString(), m).execAsk();
    }

    public int GetHTTPSPort() {
        ParameterizedSparqlString pss = new ParameterizedSparqlString("select ?port where {?s hal:HTTPSPort ?port}");
        pss.setNsPrefix("hal", HAL.NS);
        ResultSet results = QueryExecutionFactory.create(pss.toString(), m).execSelect();
        if (results.hasNext()) {
            QuerySolution sol = results.nextSolution();
            return sol.get("port").asLiteral().getInt();
        }
        return DEFAULTHTTPSPORT;
    }

    public String getRDFStoreLocation() {
        if (m.contains(Master, m.createProperty(HAL.NS + "RDFStoreLocation"))) {
            //System.out.println(Master.toString()+"  "+RDFStoreLocation.getURI());
            return m.getProperty(Master, HAL.RDFStoreLocation).getString();
        }
        return null;
    }

    public String getRDFSecurityStoreLocation() {
        if (m.contains(Master, m.createProperty(HAL.NS + "RDFSecurityStoreLocation"))) {
            return m.getProperty(Master, HAL.RDFStoreLocation).getString();
        }
        return null;
    }

    public String getMultiewerLocation() {
        if (m.contains(Master, MULTIVIEWERLOCATION)) {
            return m.getProperty(Master, MULTIVIEWERLOCATION).getObject().asResource().getURI();
        }
        return null;
    }

    public String getTalonLocation() {
        if (m.contains(Master, TALONLOCATION)) {
            return m.getProperty(Master, TALONLOCATION).getObject().asResource().getURI();
        }
        return null;
    }

    public String getZephyrLocation() {
        if (m.contains(Master, ZEPHYRLOCATION)) {
            return m.getProperty(Master, ZEPHYRLOCATION).getObject().asResource().getURI();
        }
        return null;
    }

    public HashMap<String, String> gethttp2fileMappings() {
        return http2fileMappings;
    }

    public HashMap<String, String> getfile2httpMappings() {
        return file2httpMappings;
    }
}
