// ========================================================================
// Copyright (c) 2006-2009 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
// The Eclipse Public License is available at
// http://www.eclipse.org/legal/epl-v10.html
// The Apache License v2.0 is available at
// http://www.opensource.org/licenses/apache2.0.php
// You may elect to redistribute this code under either of these licenses.
// ========================================================================

package org.eclipse.jetty.server.nohost;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.testing.HttpTester;
import org.eclipse.jetty.util.IO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public class NoHostTest
{
    private Server server;
    private URI serverBaseURI;
    private String hostname;

    @Before
    public void setup() throws Exception
    {
        hostname = InetAddress.getLocalHost().getCanonicalHostName();

        server = new Server();

        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setHost(hostname);
        connector.setPort(0);
        connector.setMaxIdleTime(5000);

        server.addConnector(connector);

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
        server.start();

        serverBaseURI = URI.create("http://" + connector.getHost() + ":" + connector.getLocalPort() + "/");
    }

    @After
    public void teardown() throws Exception
    {
        server.stop();
    }

    @Test
    public void testNoHostHeader() throws IOException
    {
        try (Socket client = new Socket(serverBaseURI.getHost(), serverBaseURI.getPort());
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

            HttpTester response = new HttpTester();
            response.parse(rawResponse);
            assertEquals("Response status code", 400, response.getStatus());
        }
    }

    @Test
    public void testEmptyHostHeader() throws IOException
    {
        try (Socket client = new Socket(serverBaseURI.getHost(), serverBaseURI.getPort());
             OutputStream out = client.getOutputStream();
             InputStream in = client.getInputStream())
        {
            String rawRequest = "GET / HTTP/1.0\r\n" +
                "Host: \r\n" +
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

            HttpTester response = new HttpTester();
            response.parse(rawResponse);
            assertEquals("Response status code", 400, response.getStatus());
        }
    }

    @Test
    public void testValidHostHeader() throws IOException
    {
        try (Socket client = new Socket(serverBaseURI.getHost(), serverBaseURI.getPort());
             OutputStream out = client.getOutputStream();
             InputStream in = client.getInputStream())
        {
            String rawRequest = "GET / HTTP/1.0\r\n" +
                "Host: " + hostname + "\r\n" +
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

            HttpTester response = new HttpTester();
            response.parse(rawResponse);
            assertEquals("Response status code", 302, response.getStatus());
        }
    }
}
