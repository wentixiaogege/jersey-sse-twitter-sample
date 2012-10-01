/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.jersey.sample.sse;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientFactory;
import javax.ws.rs.core.MediaType;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.glassfish.jersey.media.sse.OutboundEventWriter;

/**
 * A test client to use Jersey 2.0 EventSource client apis for ServerSentEvents
 *
 * @author Bhakti Mehta
 */
@WebServlet(name = "TestClient", urlPatterns = {"/TestClient"} ,asyncSupported=true)
public class TestClient extends HttpServlet {

    private final static String TARGET_URI = "http://localhost:8080/jersey-sse-twitter-sample/twittersse";
    private ExecutorService executorService;

    @Override
    public void init() throws ServletException {
        executorService = Executors.newFixedThreadPool(3);

    }

    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void service(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

        try {

            final AsyncContext asyncContext = request.startAsync();
            asyncContext.setTimeout(600000);
            asyncContext.addListener(new AsyncListener() {

                @Override
                public void onComplete(AsyncEvent event) throws IOException {
                }

                @Override
                public void onTimeout(AsyncEvent event) throws IOException {
                    System.out.println("Timeout" + event.toString());
                }

                @Override
                public void onError(AsyncEvent event) throws IOException {
                    System.out.println("Error" + event.toString());
                }

                @Override
                public void onStartAsync(AsyncEvent event) throws IOException {
                }
            });


            Thread t = new Thread(new AsyncRequestProcessor(asyncContext));
            t.start();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * This class will create the EventSource
     * and when the SSE are received will print the data
     * from the Inbound events
     */
    class AsyncRequestProcessor implements Runnable {

        private final AsyncContext context;

        public AsyncRequestProcessor(AsyncContext context) {
            this.context = context;
        }

        @Override
        public void run() {
            Client client = ClientFactory.newClient();
            client.configuration().register(OutboundEventWriter.class);
            context.getResponse().setContentType("text/html");
            final javax.ws.rs.client.WebTarget webTarget;
            try {
                final PrintWriter out = context.getResponse().getWriter();
                webTarget = client.target(new URI(TARGET_URI));
                out.println("<html>");
                out.println("<head>");
                out.println("<title>Glassfish SSE TestClient</title>");
                out.println("</head>");
                out.println("<body>");
                out.println("<h1>");
                out.println("All good tweets");
                out.println("</h1>");

                EventSource eventSource = new EventSource(webTarget, executorService) {
                    @Override
                    public void onEvent(InboundEvent inboundEvent) {
                        try {
                            //get the JSON data and parse it
                            JSONObject jsonObject = JSONObject.fromObject( inboundEvent.getData(String.class,
                                    MediaType.APPLICATION_JSON_TYPE ));
                            JSONArray jsonArray = (((JSONArray) (jsonObject.get("results"))));
                            for (int i = 0; i <jsonArray.size(); i++) {
                                JSONObject o = ((JSONObject)jsonArray.get(i)) ;
                                out.println( o.get("text"));
                                out.println("<br>");
                                out.println("Created at " + o.get("created_at"));
                                out.println("<br>");

                            }
                            out.println("</p>");
                            out.flush();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                };
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}