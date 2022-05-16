package org.eclipse.jetty.demo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.IO;

import static java.nio.charset.StandardCharsets.UTF_8;

public class DemoServer
{
    public static class RejectNoHostHandler extends HandlerWrapper
    {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
        {
            if (baseRequest.getHeader("Host") == null)
            {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        }
    }

    public static class RedirectServlet extends HttpServlet
    {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
        {
            resp.sendRedirect("/hello/");
        }
    }

    public static class HelloServlet extends HttpServlet
    {
        String greeting = "Hello";

        public HelloServlet()
        {
        }

        public HelloServlet(String hi)
        {
            greeting = hi;
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
        {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("<h1>" + greeting + " SimpleServlet</h1>");
        }
    }

    public static void main(String[] args) throws Exception
    {
        int port = 9999;
        Server server = new Server(port);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");

        // Redirect servlet
        context.addServlet(new ServletHolder(new RedirectServlet()), "/*");

        // Serve some hello world servlets
        context.addServlet(new ServletHolder(new HelloServlet()), "/hello/*");
        context.addServlet(new ServletHolder(new HelloServlet("Buongiorno Mondo")), "/it/*");
        context.addServlet(new ServletHolder(new HelloServlet("Bonjour le Monde")), "/fr/*");

        RejectNoHostHandler rejectNoHostHandler = new RejectNoHostHandler();
        rejectNoHostHandler.setHandler(context);

        server.setHandler(rejectNoHostHandler);

        try
        {
            server.start();

            URI serverBaseUri = URI.create("http://localhost:" + port + "/");

            testRequest(serverBaseUri);
        }
        finally
        {
            server.stop();
        }
    }

    private static void testRequest(URI serverBaseUri) throws IOException
    {
        try (Socket client = new Socket(serverBaseUri.getHost(), serverBaseUri.getPort());
             OutputStream out = client.getOutputStream();
             InputStream in = client.getInputStream())
        {
            String rawRequest = "GET / HTTP/1.0\r\n" +
                "Accept-Charset: iso-8859-1,utf-8;q=0.9,*;q=0.1\r\n" +
                "Accept-Language: en\r\n" +
                "Connection: Close\r\n" +
                "User-Agent: Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0)\r\n" +
                "Pragma: no-cache\r\n" +
                "Accept: image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, image/png, /\r\n" +
                "\r\n";

            out.write(rawRequest.getBytes(UTF_8));

            String rawResponse = IO.toString(in, "utf-8");
            System.out.println(rawResponse);
        }
    }
}
