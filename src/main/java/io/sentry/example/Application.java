package io.sentry.example;

import io.sentry.Sentry;
import io.sentry.event.Breadcrumb.Level;
import io.sentry.event.Breadcrumb.Type;
import io.sentry.event.BreadcrumbBuilder;
import io.sentry.event.Event;
import io.sentry.event.UserBuilder;
import io.sentry.event.helper.ShouldSendEventCallback;
import io.sentry.event.interfaces.SentryInterface;
import io.sentry.event.interfaces.ExceptionInterface;

// import org.apache.logging.log4j.ThreadContext;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
// import org.apache.logging.log4j.LogManager;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;


@Controller
@CrossOrigin
@EnableAutoConfiguration
@SpringBootApplication
public class Application {

    private static final Logger logger = LoggerFactory.getLogger("example.Application");

    private static Map<String, Integer> inventory = new HashMap<>();

    private void checkout(List<Item> cart) {
        Map<String, Integer> tempInventory = inventory;

        for (Item item : cart) {
            int currentInventory = tempInventory.get(item.getId());
            if (currentInventory <= 0) {
                throw new RuntimeException("No inventory for " + item.getId());
            }

            tempInventory.put(item.getId(), currentInventory-1);
        }
        inventory = tempInventory;
    }

    @PostMapping(value="/checkout", consumes = "application/json")
    @ResponseBody
    public ResponseEntity checkoutCart(@RequestHeader(name = "X-Session-ID", required = true) String sessionId,
                                       @RequestHeader(name = "X-Transaction-ID", required = true) String transactionId,
                                       @RequestBody Order order) {
        try {
            // set session and transaction id as tags
            // ThreadContext.put("session_id", sessionId);
            // ThreadContext.put("transaction_id", transactionId);

            String userEmail = order.getEmail();
            logger.info("Processing order for: " + userEmail);
            
            // Set the user in the current context.
            Sentry.getContext().setUser(
                new UserBuilder().setEmail(userEmail).build()
            );
            
            Sentry.getContext().recordBreadcrumb(
                new BreadcrumbBuilder().setMessage("Processing checkout for user: " + userEmail).setType(Type.USER).setLevel(Level.INFO).setCategory("custom").build()
            );
            
            // perform checkout
            checkout(order.getCart());
            
        } catch (Exception e) {
        	
            // log error + return 500, if exception
            logger.error("Error while checking out", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Checkout error");
        }

        // return 200 if checkout was successful
        return ResponseEntity.status(HttpStatus.OK).body("Success");
    }

    @RequestMapping("/capture-message")
    @ResponseBody
    String captureMessage() {
    	// MDC extras (added to Sentry event as ADDITIONAL DATA) 
        // ThreadContext.put("extra_key", "extra_value");
   
        // NDC extras are sent under 'log4j2-NDC'
        // ThreadContext.push("Extra_details");

        logger.debug("Debug message");
        logger.info("Info message");
        logger.warn("Warn message {}", 11); // warning message that will be sent to Sentry

        return "Success";
    }

    @RequestMapping("/handled")
    @ResponseBody
    String handledError() {
        String someLocalVariable = "stack locals";

        // MDC extras (added to Sentry event as ADDITIONAL DATA)
        //ThreadContext.put("customKey1", "customValue2");
        
        try {
            logger.info("this is from INFO");
            logger.error("this is from ERROR");
            // int example = 1 / 0;
        } catch (Exception e) {
            // caught exception that will be sent to Sentry
            logger.error("Caught exception!", e);
        }
        return "Success";
    }
    
    @RequestMapping("/filtered")
    @ResponseBody
    String handledFilteredError() {
        try {
            int example = 1 / 0;
        } catch (Exception e) {
            logger.error("Message contains foo!", e);
        }
        return "Success";
    }  
    

    @RequestMapping("/unhandled")
    @ResponseBody
    String unhandledError() {
    	 String someLocalVariable = "stack locals";
        // ThreadContext.put("customKey1", "value1");
    	 
        throw new RuntimeException("Unhandled exception!");
    }


    @RequestMapping("/errorgenerator")
    @ResponseBody
    String errorGenerator() {

        // create an array of Strings
        String array[] = new String[] {
                "syncObjectType=USER", "syncObjectType=ALL"};

        int randomNum = ThreadLocalRandom.current().nextInt(1000, 8000 + 1);

        long number = Math.round( Math.random() ) ;




        try {
            int example = 1 / 0;
        } catch (Exception e) {
            logger.error("Failed to complete Salesforce sync; companyId=" +
                    number + "," + array[(int) number], e);
        }
        return "Success";





    }

    public static void main(String[] args) {
    	initSentry();
        inventory.put("wrench", 0);
        inventory.put("nails", 0);
        inventory.put("hammer", 1);
        SpringApplication.run(Application.class, args);
    }
    
    private static void initSentry() {
    	Sentry.init();


        
    	Sentry.getStoredClient().setServerName("fe1");

        Sentry.getStoredClient().addShouldSendEventCallback(new ShouldSendEventCallback() {


            @Override
            public boolean shouldSend(Event event) {

                System.out.print("EVENT " + event);

                System.out.print("EVENT " + event.getLevel());

                System.out.print("EVENT " + event.getMessage());

                if (event.getMessage().contains("foo")) {
                    List<String> listA = new ArrayList<>();

                    listA.add("SecurityError");
                    //Fingerprint can be set on
                    event.setFingerprint(listA);
                    return false;
                }
                List<String> listA = new ArrayList<>();

                listA.add("SecurityError");
                //Fingerprint can be set on
                event.setFingerprint(listA);

                return true;

            }
        });
    	
    	//Added as tags to Sentry event
        Sentry.getStoredClient().addTag("dynamicTag1", "1.0");
        
        Sentry.getStoredClient().addShouldSendEventCallback(new ShouldSendEventCallback() {
		    @Override
		    public boolean shouldSend(Event event) {		    	 
		        if (event.getMessage().contains("foo")) {
		            return false;
		        }		   		
		        return true;
		    }
		});
    }
}
