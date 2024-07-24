package com.ebremer.halcyon.server.ldp;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.api.FramingApi;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.document.RdfDocument;
import com.apicatalog.jsonld.lang.Keywords;
import com.apicatalog.jsonld.processor.FromRdfProcessor;
import com.apicatalog.rdf.RdfDataset;
import com.ebremer.halcyon.data.DataCore;
import com.ebremer.halcyon.server.utils.PathMapper;
import com.ebremer.halcyon.utils.HalJsonLD;
import com.ebremer.ns.HAL;
import com.ebremer.ns.LDP;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.system.JenaTitanium;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;

/**
 *
 * @author erich
 */
public class Tools {
    public record Prefer(String _return, List<Resource> includes) {};
    
    public static void Save(String uri, String data) {
        try (JsonReader jsonReader = Json.createReader(new StringReader(data))) {
            JsonObject jo = jsonReader.readObject();
            if (jo.containsKey("image")) {
                String name = jo.getString("image");     
                Optional<URI> x = PathMapper.getPathMapper().http2file(uri);
                if (x.isPresent()) {
                    URI dest = x.get();
                    File file = new File(dest.getPath().substring(1));
                    file.getParentFile().mkdirs();
                    Model m = ModelFactory.createDefaultModel();  
                    Resource anno = m.createResource(uri)
                            .addProperty(RDF.type, LDP.NonRDFSource)
                            .addProperty(RDF.type, m.createResource("http://www.w3.org/ns/iana/media-types/application/json#Resource"))
                            .addProperty(RDF.type, HAL.Annotation)
                            .addLiteral(m.createProperty("http://www.w3.org/ns/posix/stat#mtime"), file.lastModified()/1000)
                            .addLiteral(m.createProperty("http://www.w3.org/ns/posix/stat#size"), file.length());
                    m.createResource(name).addProperty(HAL.annotation, anno );
                    BasicFileAttributes attrs;
                    try {
                        attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                        Instant instant = attrs.lastModifiedTime().toInstant();
                        String isoDateTime = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).format(instant);
                        anno.addProperty(DCTerms.modified, isoDateTime);
                    } catch (IOException ex) {
                        Logger.getLogger(Tools.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        fos.write(data.getBytes());
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(Tools.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(Tools.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    Dataset ds = DataCore.getInstance().getDataset();
                    ds.begin(ReadWrite.WRITE);
                    ds.getNamedModel(HAL.CollectionsAndResources).add(m);
                    ds.commit();
                    ds.end();
                }
            }
        }
    }
    
    public static void AddnonLDPResourceMeta(Resource r) {
        
    }
    
    public static Prefer getPreferHeader(HttpServletRequest request) {
        String header = request.getHeader("Prefer");
        if (header!=null) {
            String[] two = header.split(";");
            String[] rreturntwo = two[0].split("=");
            String[] includetwo = two[1].split("=");
            if (rreturntwo.length==2) {
                if (rreturntwo[0].trim().equals("return")) {
                    String representation = rreturntwo[1].trim();
                    if (includetwo[0].trim().equals("include")) {
                        String[] preds = includetwo[1].trim().replace("\"", "").split(" ");
                        Resource[] resourceArray = new Resource[preds.length];
                        for (int i = 0; i < preds.length; i++) {
                            resourceArray[i] = ResourceFactory.createProperty(preds[i]);
                        }
                        return new Prefer(representation,Arrays.asList(resourceArray));
                    }
                }
            }
        }
        return null;
    }
    
    public static Model getRDF(HttpServletRequest request) {
        Prefer prefer = Tools.getPreferHeader(request);
        ParameterizedSparqlString pss = new ParameterizedSparqlString(
            (prefer==null)?
                "construct { ?s ?p ?o } where { graph hal:CollectionsAndResources { ?s ?p ?o }}":
                "construct { ?s ?p ?o } where { graph hal:CollectionsAndResources { ?s ?p ?o values (?p) { ?includes }}}");
        pss.setNsPrefix("hal", HAL.NS);
        pss.setIri("s", request.getRequestURL().toString());
        if (prefer!=null) {
            pss.setValues("includes", prefer.includes());
        }
        Dataset ds = DataCore.getInstance().getDataset();
        ds.begin(ReadWrite.READ);
        Model k = QueryExecutionFactory.create(pss.toString(),ds).execConstruct();
        ds.end();
        return k;
    }

    public static void Annotations2JSONLD(Model m, OutputStream xout) {
        try {
            m.setNsPrefix("hal", HAL.NS);
            m.setNsPrefix("annotation", "hal:annotation");
            Dataset dsx = DatasetFactory.create(m);
            DatasetGraph dsg = dsx.asDatasetGraph();
            JsonObjectBuilder cxt = Json.createObjectBuilder();
            dsg.prefixes().forEach((k, v) -> {
                if ( ! k.isEmpty() )
                    cxt.add(k, v);
            });
            cxt.add("annotation", Json.createObjectBuilder()
                    .add(Keywords.ID, "hal:annotation")
                    .add(Keywords.TYPE, Keywords.ID)
            );
            RdfDataset ds = JenaTitanium.convert(dsg);
            Document doc = RdfDocument.of(ds);
            JsonLdOptions options = new JsonLdOptions();
            options.setOrdered(false);
            options.setUseNativeTypes(true);
            options.setOmitGraph(true);
            JsonArray array = FromRdfProcessor.fromRdf(doc, options);
            JsonObject frame = Json.createObjectBuilder()
                    .add(Keywords.CONTEXT, cxt)
                    .add(Keywords.EMBED, Keywords.ALWAYS)
                    .add(Keywords.OMIT_DEFAULT, true)
                    .add(Keywords.REQUIRE_ALL, false)
                    .add("annotation", Json.createObjectBuilder())
                    .build();
            FramingApi api = JsonLd.frame(JsonDocument.of(array), JsonDocument.of(frame));
            JsonStructure x = api.get();
            JsonWriterFactory writerFactory = Json.createWriterFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));
            JsonWriter out = writerFactory.createWriter(xout);
            out.write(x);
        } catch (JsonLdError ex) {
            Logger.getLogger(HalJsonLD.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    
}
