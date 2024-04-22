package com.rajantechies;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication(exclude ={ElasticsearchRestClientAutoConfiguration.class}) 
//@ImportResource("classpath:camel-context.xml")
public class KibanaLoggerApplication {

	public static void main(String[] args) {
	    
	    String keyStore = "C:\\test\\ssl2.jks";
        String keyStorePassword = "changeit";
 
        System.setProperty("javax.net.ssl.keyStore", keyStore);
        System.setProperty("javax.net.ssl.trustStore", keyStore);
        System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);
        System.setProperty("javax.net.ssl.trustStorePassword", keyStorePassword);
        System.setProperty("javax.net.ssl.keyStoreType", "JKS");
        System.setProperty("javax.net.ssl.keyStoreProvider", "SUN");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
		SpringApplication.run(KibanaLoggerApplication.class, args);
	}

}
