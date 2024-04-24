package com.rajantechies.routes;

import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rajantechies.linux.ProductionExec;
import com.rajantechies.linux.StagingExec;

@Component
public class OcPodlogRouteBuilder extends RouteBuilder{
    
    @Autowired
    ProductionExec prodExec;
    
    @Autowired
    StagingExec stagExec;

    @Override
    public void configure() throws Exception {
        
        from("timer://runOnce?repeatCount=1&delay=5000").autoStartup(true)
        .setBody(simple("oc get pods | grep -i 'apicast' | grep -i 'running'"))
        .to("ssh://opsvc:GmBbhAckVZLR1@172.31.35.3")
         .split().tokenize("\n")
            .to("direct:podsLogs")
          .end()
         ;
        
        from("direct:podsLogs").routeId("direct-podsLogs")
           .split().tokenize(" ").parallelProcessing().streaming()
             .choice()
                .when(simple("${header.CamelSplitIndex} == 0"))
                .log("pods= ${body}")
                .setProperty("pod").simple("${body}")
                .setExchangePattern(ExchangePattern.InOnly)
                .to("seda:GET_LOGS")
             .endChoice()
           .end()
        ;
        
        from("seda:GET_LOGS?concurrentConsumers=10").routeId("direct-GET_LOGS")
        .log("before call ${exchangeProperty.pod} log= ${body}")
           .setBody(simple("oc logs -f ${body}"))
           .bean(stagExec,"getPodLogs")
        ;
    }

}
