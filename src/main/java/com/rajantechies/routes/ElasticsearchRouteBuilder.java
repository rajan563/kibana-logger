package com.rajantechies.routes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.es.ElasticsearchComponent;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.rajantechies.utils.Utilities;

import co.elastic.clients.transport.TransportUtils;

@Component
public class ElasticsearchRouteBuilder extends RouteBuilder {
    
    @Autowired
    private ProducerTemplate template;

    @Bean(name = "elasticsearch")
    ElasticsearchComponent elasticsearchComponent() {
        
        
        String fingerprint = "7c585f24126a9a7d8acbef3e1f00f4ca4e4131844326433ad893e080de253486";

        SSLContext sslContext = TransportUtils.sslContextFromCaFingerprint(fingerprint); 

        BasicCredentialsProvider credsProv = new BasicCredentialsProvider(); 
        credsProv.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("elastic", "X7vyx*Rn_OtdVsjiAdG2"));

        RestClient restClient = RestClient
            .builder(new HttpHost("localhost", 9200, "https")) 
            .setHttpClientConfigCallback(hc -> hc
                .setSSLContext(sslContext) 
                .setDefaultCredentialsProvider(credsProv)
            )
            .build();
        
        
        
        ElasticsearchComponent elasticsearchComponent = new ElasticsearchComponent();
        elasticsearchComponent.setClient(restClient);
        
        return elasticsearchComponent;
    }

    @Override
    public void configure() throws Exception {
        // @formatter:off
 
        from("file://order?noop=true").autoStartup(false)
            .log("file name ")
            .process(exchange -> {
            File file = exchange.getIn().getBody(File.class);
            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            System.out.println("Reading File line by line using BufferedReader");
            String line = null;
            Map<String, Object> logData = new LinkedHashMap<>();
            while ((line = reader.readLine()) != null) {

                int requestIDIndex = line.indexOf("requestID=");
                String requestID = "";
                if (requestIDIndex != -1) {
                    requestID = line.substring(requestIDIndex + 10, requestIDIndex + 42);
                }

                if (logData.containsValue(requestID)) {
                    if (line.contains("policy_loader.lua")) {
                        String hit = Utilities.hitReceived(reader);
                        if (!hit.isEmpty()) {
                            logData.put("hitReceived", hit);
                        }
                    }

                    if (line.contains("connection to backend-listener")) {

                        String backendURL = Utilities.callBackendTransaction(reader);
                        if (!backendURL.isEmpty()) {
                            logData.put("backendURL", backendURL);
                        }
                    }

                    if (line.contains("log(): policy chain execute phase: log, policy")) {

                        String ss = Utilities.readMultipleChars(reader);
                        if (ss.contains("logging_bigin:")) {

                            logData.put("host", ss.substring(ss.indexOf("host=") + 5, ss.indexOf(":serviceName")));
                            logData.put("serviceName",
                                    ss.substring(ss.indexOf("serviceName=") + 12, ss.indexOf(":serviceID")));
                            logData.put("serviceID",
                                    ss.substring(ss.indexOf("serviceID=") + 12, ss.indexOf(":request_body")));
                            logData.put("requestBody",
                                    ss.substring(ss.indexOf("request_body=") + 13, ss.indexOf(":time")));
                            String time = ss.substring(ss.indexOf("time=") + 6, ss.indexOf(":url") - 1);
                            logData.put("timestamp", Utilities.utcToIst(time));
                            logData.put("url", ss.substring(ss.indexOf("url=") + 4, ss.indexOf(":status_code")));
                            logData.put("responseCode",
                                    ss.substring(ss.indexOf("status_code=") + 12, ss.indexOf(":TR")));
                            logData.put("BackendResTime", ss.substring(ss.indexOf("TR=") + 3));
                        }

                    }
                    System.out.println("logData.keySet().size()=="+logData.keySet().size());
                    if (logData.keySet().size() > 8) {
                        System.out.println("logdatda=" + logData);
                        template.sendBody("direct:elasticsearch", logData);
                        System.out.println("sent");
                        logData.clear();

                    }
                } else {
                    if (!requestID.isEmpty()) {
                        logData.put("requestID", requestID);
                    }
                }
            }

            reader.close();
            fis.close();

        })
        
        ;
        // @formatter:off
        from("direct:elasticsearch").routeId("elasticsearch")
                .log("line2 ## ${body}")
                .marshal(new JacksonDataFormat())
                .log("before send ")
                .to("elasticsearch://elasticsearch?operation=Index&indexName=3scale")
                .log("message send to elastic search ")
                ;
    }

}
