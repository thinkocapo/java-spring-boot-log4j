package io.sentry.example;

import io.sentry.Sentry;
import io.sentry.event.Breadcrumb.Level;
import io.sentry.event.Breadcrumb.Type;
import io.sentry.event.BreadcrumbBuilder;
import io.sentry.event.Event;
import io.sentry.event.UserBuilder;
import io.sentry.event.helper.ShouldSendEventCallback;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import io.sentry.DefaultSentryClientFactory;
import io.sentry.dsn.Dsn;
import io.sentry.SentryClient;


@Controller
@CrossOrigin
@EnableAutoConfiguration
@SpringBootApplication
public class Application {

    private static final Logger logger = LogManager.getLogger("example.Application");

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
            ThreadContext.put("session_id", sessionId);
            ThreadContext.put("transaction_id", transactionId);

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
        ThreadContext.put("extra_key", "extra_value");
   
        // NDC extras are sent under 'log4j2-NDC'
        ThreadContext.push("Extra_details");

        logger.debug("Debug message");
        logger.info("Info message");
        logger.warn("Warn message"); // warning message that will be sent to Sentry
        return "Success";
    }

    @RequestMapping("/handled")
    @ResponseBody
    String handledError() {
        String someLocalVariable = "stack locals";
        
        try {
            int example = 1 / 0;
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
    	 
        throw new RuntimeException("Unhandled exception!");
    }

    public static void main(String[] args) {
    	initSentry();
        inventory.put("wrench", 0);
        inventory.put("nails", 0);
        inventory.put("hammer", 1);
        SpringApplication.run(Application.class, args);
    }
    
    private static void initSentry() {
        Dsn dsn = new Dsn("https://26eb66bd0a85474698fe5691cd7f21e0@sentry.io/1399858");

        SentryClientCustomFactory sccf = new SentryClientCustomFactory();

        SentryClient sentryClient = sccf.createSentryClient(dsn);
        Sentry.setStoredClient(sentryClient);


    	Sentry.init();
        
    	Sentry.getStoredClient().setServerName("fe1");
    	
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
