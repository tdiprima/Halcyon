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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("LDPServer "+request.getRequestURI()+" "+request.getHeader("Accept"));
        logger.info("{} ----> {}",request.getRequestURI(),request.getContentType());
        String accept = request.getHeader("Accept");
        Optional<Tools.Prefer> prefer = Tools.getPreferHeader(request);
        switch (accept) {
            case "application/n-triples":
                try (ServletOutputStream out = response.getOutputStream()) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType(accept);
                    RDFDataMgr.write(out, Tools.getRDF(prefer, request.getRequestURL().toString()).getModel(), Lang.NTRIPLES);
                }
                break;
            case "text/turtle":
                try (ServletOutputStream out = response.getOutputStream()) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType(accept);
                    RDFDataMgr.write(out, Tools.getRDF(prefer, request.getRequestURL().toString()).getModel(), Lang.TURTLE);
                }
                break;
            case "application/ld+json":
                try (ServletOutputStream out = response.getOutputStream()) {
                    Tools.Resource2JSONLD(prefer,Tools.getRDF(prefer, request.getRequestURL().toString()), out);
                }
                break;
            case null:
            default:
                logger.info("{} ----> {} Passing to default handler.",request.getRequestURI(),request.getContentType());
                super.doGet(request, response);
        }
    }
       
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.trace("doPost {} ----> {}",request.getRequestURI(),request.getContentType());
        System.out.println("POST : "+request.getRequestURI());
        switch (request.getContentType()) {
            case "text/turtle":               
                break;
            case "application/json":
                System.out.println(request.getContentLengthLong());
                System.out.println(Utils.getBody(request));
                break;
            case "application/octet-stream":
                Utils.UploadFile(request);
                break;
            default:
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("text/html;charset=UTF-8");
                try (PrintWriter out = response.getWriter()) {
                    out.println("<html>");
                    out.println("<head>");
                    out.println("<title>Status OK</title>");
                    out.println("</head>");
                    out.println("<body>");
                    out.println("<h1>All is well!</h1>");
                    out.println("</body>");
                    out.println("</html>");
                }
        }
    }
    
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.trace("doPut {} ----> {}",request.getRequestURI(),request.getContentType());        
        switch (request.getContentType()) {
            case "text/turtle":
                break;
            case "application/json":
                Tools.Save(request, response);
                break;
            case "application/octet-stream":
                Utils.UploadFile(request);
                break;
            default:
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("text/html;charset=UTF-8");
                try (PrintWriter out = response.getWriter()) {
                    out.println("<html>");
                    out.println("<head>");
                    out.println("<title>Status OK</title>");
                    out.println("</head>");
                    out.println("<body>");
                    out.println("<h1>All is well!</h1>");
                    out.println("</body>");
                    out.println("</html>");
                }
        }
    }
}
