package com.enterprise.engine.core.component;

import io.joltcommunity.jolt.Chainr;
import io.joltcommunity.jolt.JsonUtils;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;
import java.util.List;

@Component("joltTransformer")
public class JoltTransformBean {

    public void transform(Exchange exchange) {
        // 1. Get the payload
        Object inputJson = exchange.getIn().getBody();
        
        // 2. Get the AI-generated spec path from a Camel header
        String specPath = exchange.getIn().getHeader("JoltSpecPath", String.class);
        
        if (specPath == null) {
            throw new IllegalArgumentException("JoltSpecPath header is missing");
        }

        // 3. Load the AI's mapping and execute
        List<Object> chainrSpecJSON = JsonUtils.classpathToList(specPath);
        Chainr chainr = Chainr.fromSpec(chainrSpecJSON);
        
        Object transformedOutput = chainr.transform(inputJson);
        
        // 4. Set the new mutable payload
        exchange.getIn().setBody(transformedOutput);
    }
}