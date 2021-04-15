package example;

//import static org.junit.jupiter.api.Assertions.assertTrue;

//import com.accenture.msdynamics.HandlerGetAccount;
//import com.amazonaws.services.lambda.runtime.Context;

import org.junit.jupiter.api.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class InvokeTest {
  private static final Logger logger = LoggerFactory.getLogger(InvokeTest.class);

  @Test
  void invokeTest() {
    logger.info("Invoke TEST");
    //Object event = new Object();
    //Context context = new TestContext();
    //HandlerGetAccount handler = new HandlerGetAccount();
    //String result = handler.handleRequest(event, context);
    //assertTrue(result != null);
  }

}
