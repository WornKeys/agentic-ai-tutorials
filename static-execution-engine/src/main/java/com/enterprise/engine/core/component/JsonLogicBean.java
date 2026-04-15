package com.enterprise.engine.core.component;

import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.JsonLogicException;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@Component("jsonLogicBean")
public class JsonLogicBean {

  private final JsonLogic jsonLogic;

  public JsonLogicBean() {
    // Initialize the sandboxed evaluator
    this.jsonLogic = new JsonLogic();
  }

  public void evaluate(Exchange exchange) {
    try {
      // 1. Get original Kafka event
      var kafkaEvent = exchange.getIn().getBody(Map.class);

      // 2. Get the enriched data fetched by the previous step
      var overlayData = exchange.getProperty("EnrichedOverlayData", Map.class);

      // 3. Build the "Super Context"
      // The AI will write rules targeting "event.field" or "overlay.field"
      Map<String, Object> evaluationContext =
          Map.of("event", kafkaEvent, "overlay", overlayData != null ? overlayData : Map.of());

      // 4. Load AI rule and evaluate against the combined context
      String rulePath = exchange.getIn().getHeader("RulesSpecPath", String.class);
      String jsonRule = new String(Files.readAllBytes(Paths.get("src/main/resources" + rulePath)));

      boolean rulePassed = (boolean) jsonLogic.apply(jsonRule, evaluationContext);
      exchange.getIn().setHeader("RulesPassed", String.valueOf(rulePassed));
    } catch (JsonLogicException | java.io.IOException e) {
      // If the AI generated malformed logic, catch it and fail closed
      exchange.getIn().setHeader("RulesPassed", "false");
      exchange.getIn().setHeader("RuleEvaluationError", e.getMessage());
    }
  }
}
