/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.terenceproject.utils.rest;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

/**
 *
 * @author pierpaolo
 */
public class REST {

    protected WebResource service;
    protected ClientConfig config;
    protected Client client;

    public REST(String URL) {
        if (URL != null) {
            config = new DefaultClientConfig();
            client = Client.create(config);
            service = client.resource(UriBuilder.fromUri(URL).build());
        } else {
            service = null;
        }
    }

    protected String call(String path, Object o) {
        if (service == null) {
            return null;
        } else {
            return service.path(path).type(MediaType.TEXT_XML).post(ClientResponse.class, o).getEntity(String.class);
        }
    }

    protected String post(String path, String data) {
        URL url;
        URLConnection urlConn;
        DataOutputStream output;
        BufferedReader input;
        String result = "";

        try {
            url = new URL(service.getURI() + path);

            urlConn = url.openConnection();
            urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConn.setRequestProperty("Content-Length", Integer.toString(data.getBytes().length));
            urlConn.setDoInput(true);
            urlConn.setDoOutput(true);
            urlConn.setUseCaches(false);

            output = new DataOutputStream(urlConn.getOutputStream());
            output.writeBytes(data);
            output.flush();
            output.close();

            DataInputStream in = new DataInputStream(urlConn.getInputStream());
            input = new BufferedReader(new InputStreamReader(in));
            String str;
            while ((str = input.readLine()) != null) {
                result = result + str + "\n";
            }
            input.close();
            
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
