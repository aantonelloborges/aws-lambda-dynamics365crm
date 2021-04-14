package example;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.accenture.MSDynamics.HandlerGetAccount;

import java.util.HashMap;

class InvokeTest {
  private static final Logger logger = LoggerFactory.getLogger(InvokeTest.class);

  @Test
  void invokeTest() {
    logger.info("Invoke TEST");
    HashMap<String,String> event = new HashMap<String,String>();
    Context context = new TestContext();
    HandlerGetAccount handler = new HandlerGetAccount();
    String result = handler.handleRequest(event, context);
    assertTrue(result != null);
  }

}
