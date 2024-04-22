package com.rajantechies.routes;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mongodb.MongoDbComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.rajantechies.model.Kibana;

@Component
public class MondodbRouteBuilder extends RouteBuilder{
    
    
    @Bean(name = "mongodb")
    MongoDbComponent mongoDbComponent() {
        
        MongoDbComponent component = new MongoDbComponent();
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        component.setMongoConnection(mongoClient);
        return component;
    }

    @Override
    public void configure() throws Exception {
        
        
        onException(com.mongodb.MongoWriteException.class)
        .setHeader("isInsert",constant(false))
        .handled(true)
        ;
        
        from("direct:kibana").routeId("direct-kibana") 
        .process(exchange -> {
            Kibana kibana = new Kibana();
            kibana.set_id(exchange.getIn().getBody(Map.class).get("requestID").toString());
            exchange.getIn().setBody(kibana);
        })
        .to("mongodb:myDb?database=kibana&collection=kibana&operation=insert")
        .setHeader("isInsert",constant(true))
        .log("done .. ${body}")
        ;
        
        
    }

}
