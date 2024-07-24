package com.ebremer.halcyon.server.ldp;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
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
        logger.trace("{} ----> {}",request.getRequestURI(),request.getContentType());
        String accept = request.getHeader("Accept");
        switch (accept) {
            case "text/turtle":
            case "application/ld+json":
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("text/turtle");
                Lang lang = accept.equals("text/turtle")?Lang.TURTLE:Lang.JSONLD;
                try (ServletOutputStream out = response.getOutputStream()) {
                    if (lang.equals(Lang.TURTLE)) {
                        RDFDataMgr.write(out, Tools.getRDF(request), lang);
                    } else if (lang.equals(Lang.JSONLD)) {
                        Tools.Annotations2JSONLD(Tools.getRDF(request), out);
                    }
                }
                break;
            case null:
            default:
                super.doGet(request, response);
        }
    }
       
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.trace("doPost {} ----> {}",request.getRequestURI(),request.getContentType());        
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
                Tools.Save(request.getRequestURL().toString(), Utils.getBody(request));
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


                /*
            case "application/json":
                Optional<URI> x = PathMapper.getPathMapper().http2file(request.getRequestURI());
                if (x.isPresent()) {
                    URI dest = x.get();
                    File file = new File(dest.getPath().substring(1));
                    try (FileInputStream fis = new FileInputStream(file)) {
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.setContentType("application/json");
                        IOUtils.copy(fis, response.getOutputStream());
                    }
                }                
                break;*/