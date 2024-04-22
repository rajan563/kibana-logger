package com.rajantechies.linux;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.rajantechies.utils.Utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StagingExec {

    private static final String SSH_HOST = "172.31.35.3";
    private static final String SSH_LOGIN = "opsvc";
    private static final String SSH_PASSWORD = "GmBbhAckVZLR1";

    @Autowired
    private ProducerTemplate template;

    public void getPodLogs(Exchange exchange) {
        try {
            String command = exchange.getIn().getBody(String.class);
            runCommand(command, exchange);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void runCommand(String command, Exchange exchange) throws JSchException, ParseException {
        Session session = setupSshSession();
        session.connect();

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        try {
            channel.setCommand(command);
            channel.setInputStream(null);
            InputStream output = channel.getInputStream();
            channel.connect();
            System.out.println("command executed 11111 === "+command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(output));
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
                            logData.put("requestTime", time);
                            logData.put("url", ss.substring(ss.indexOf("url=") + 4, ss.indexOf(":status_code")));
                            logData.put("responseCode",
                                    ss.substring(ss.indexOf("status_code=") + 12, ss.indexOf(":TR")));
                            logData.put("BackendResTime", ss.substring(ss.indexOf("TR=") + 3));
                        }

                    }

                    if (logData.keySet().size() > 8) {
                        //System.out.println("logdatda=" + logData);
                        template.sendBody("direct:elasticsearch", logData);
                        System.out.println("sent of pod=="+command);
                        logData.clear();
                    }
                } else {
                    if (!requestID.isEmpty()) {
                        logData.put("requestID", requestID);
                    }
                }
                
                //System.out.println("command executed 2222 === "+command);
            }
            

        } catch (JSchException | IOException e) {
            closeConnection(channel, session);
            throw new RuntimeException(e);

        } finally {
            closeConnection(channel, session);
        }
    }

    private static Session setupSshSession() throws JSchException {
        Session session = new JSch().getSession(SSH_LOGIN, SSH_HOST, 22);
        session.setPassword(SSH_PASSWORD);
        session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
        session.setConfig("StrictHostKeyChecking", "no");
        return session;
    }

    private static void closeConnection(ChannelExec channel, Session session) {
        try {
            channel.disconnect();
        } catch (Exception ignored) {
        }
        session.disconnect();
    }

    

}
