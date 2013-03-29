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

import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

/**
 * This is a Schedule based Timer which will get tweets information from Twitter and send as Server
 * Sent Events
 * @author Bhakti Mehta
 */
@Stateless
@Named
public class TwitterBean {

   private final static String SEARCH_URL =
              "http://search.twitter.com/search.json?q=glassfish&rpp=5&include_entities=true" +
                      "&with_twitter_user_id=true&result_type=mixed";


    private final static String TARGET_URI= "http://localhost:8080/jersey-sse-twitter-sample";

    @Schedule(hour="*", minute="*", second="*/10")
    public void sendTweets() {

        Client client = ClientBuilder.newClient();
        try {
            WebTarget webTarget= client.target(new URI(TARGET_URI)) ;

            String message = getFeedData();
            System.out.println("Posting message");
            webTarget.path("twittersse").request().post(Entity.text(message));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }


private  String getFeedData() {
        StringBuilder sb = new StringBuilder();
        try {
            URL twitter = new URL(SEARCH_URL);
            URLConnection yc = null;

            yc = twitter.openConnection();


            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            yc.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                System.out.println("Input line" + inputLine);
                sb.append(inputLine)  ;

            }
            in.close();

            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
    return new String("Error in getting data from the feeds ") ;
    }


}
