package com.ebremer.halcyon.server.ldp;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MultipartConfig
public class LDPServer extends DefaultServlet {

    private static final Logger logger = LoggerFactory.getLogger(LDPServer.class);
    private static final String NTRIPLES = "application/n-triples";
    private static final String TURTLE = "text/turtle";
    private static final String JSON_LD = "application/ld+json";
    private static final String OCTET_STREAM = "application/octet-stream";
    private static final String APPLICATION_JSON = "application/json";

    /**
     * Handles GET requests. Based on the Accept header, it returns RDF data in
     * different formats (N-Triples, Turtle, or JSON-LD). If the format is
     * unsupported, the request is passed to the default handler.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.info("GET request for URI: {} with Accept header: {}", request.getRequestURI(), request.getHeader("Accept"));
        String accept = request.getHeader("Accept");
        Optional<Tools.Prefer> prefer = Tools.getPreferHeader(request);

        try (ServletOutputStream out = response.getOutputStream()) {
            switch (accept) {
                case NTRIPLES:
                    setResponse(response, NTRIPLES, HttpServletResponse.SC_OK);
                    RDFDataMgr.write(out, Tools.getRDF(prefer, request.getRequestURL().toString()).getModel(), Lang.NTRIPLES);
                    break;
                case TURTLE:
                    setResponse(response, TURTLE, HttpServletResponse.SC_OK);
                    RDFDataMgr.write(out, Tools.getRDF(prefer, request.getRequestURL().toString()).getModel(), Lang.TURTLE);
                    break;
                case JSON_LD:
                    setResponse(response, JSON_LD, HttpServletResponse.SC_OK);
                    Tools.Resource2JSONLD(prefer, Tools.getRDF(prefer, request.getRequestURL().toString()), out);
                    break;
                default:
                    logger.info("Passing request to default handler for URI: {}", request.getRequestURI());
                    super.doGet(request, response);
                    break;
            }
        } catch (Exception e) {
            logger.error("Error processing GET request for URI: {}", request.getRequestURI(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server encountered an error");
        }
    }

    /**
     * Handles POST requests. Depending on the content type (Turtle, JSON, or
     * binary data), it performs actions such as saving data, uploading files,
     * or responding with a default HTML page if the content type is unsupported.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.info("POST request for URI: {} with Content-Type: {}", request.getRequestURI(), request.getContentType());
        handleRequestWithContentType(request, response, request.getContentType(), "POST");
    }

    /**
     * Handles PUT requests (similar to POST).
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.info("PUT request for URI: {} with Content-Type: {}", request.getRequestURI(), request.getContentType());
        handleRequestWithContentType(request, response, request.getContentType(), "PUT");
    }

    /**
     * Helper function to handle POST and PUT requests based on content type.
     * Depending on the content type, it processes the request accordingly, such
     * as saving JSON, uploading files, or sending a default HTML response.
     */
    private void handleRequestWithContentType(HttpServletRequest request, HttpServletResponse response, String contentType, String method) throws IOException {
        try {
            switch (contentType) {
                case TURTLE:
                    // Add logic for text/turtle here
                    break;
                case APPLICATION_JSON:
                    if ("PUT".equals(method)) {
                        Tools.Save(request, response);
                    }
                    break;
                case OCTET_STREAM:
                    Utils.UploadFile(request);
                    break;
                default:
                    sendDefaultResponse(response);
                    break;
            }
        } catch (Exception e) {
            logger.error("Error processing {} request for URI: {}", method, request.getRequestURI(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server encountered an error");
        }
    }

    /**
     * Helper function to set the HTTP response with the specified status and
     * content type.
     */
    private void setResponse(HttpServletResponse response, String contentType, int status) {
        response.setStatus(status);
        response.setContentType(contentType);
    }

    /**
     * Sends a default HTML response with "Status OK" message. Used when content
     * type is unsupported in POST or PUT requests.
     */
    private void sendDefaultResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.println("<html>");
            out.println("<head><title>Status OK</title></head>");
            out.println("<body><h1>All is well!</h1></body>");
            out.println("</html>");
        }
    }
}
