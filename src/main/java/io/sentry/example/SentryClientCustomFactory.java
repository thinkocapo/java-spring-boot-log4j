package io.sentry.example;

import io.sentry.DefaultSentryClientFactory;
import io.sentry.dsn.Dsn;
import io.sentry.SentryClient;
import io.sentry.connection.NoopConnection;
import io.sentry.event.helper.ContextBuilderHelper;
import io.sentry.context.ThreadLocalContextManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SentryClientCustomFactory extends DefaultSentryClientFactory {

    private static final Logger log = LogManager.getLogger("example.Application");

    @Override
    public SentryClient createSentryClient(Dsn dsn) {

        try {
            SentryClient sentryClient = new SentryClient(createConnection(dsn), getContextManager(dsn));
            try {
                // `ServletRequestListener` was added in the Servlet 2.4 API, and
                // is used as part of the `HttpEventBuilderHelper`, see:
                // https://tomcat.apache.org/tomcat-5.5-doc/servletapi/
                Class.forName("javax.servlet.ServletRequestListener", false, this.getClass().getClassLoader());
                // sentryClient.addBuilderHelper(new HttpEventBuilderHelper());
                sentryClient.addBuilderHelper(new HttpEventBuilderHelperWithBody());
            } catch (ClassNotFoundException e) {
                log.debug("The current environment doesn't provide access to servlets,"
                        + " or provides an unsupported version.");
            }

            sentryClient.addBuilderHelper(new ContextBuilderHelper(sentryClient));
            return configureSentryClient(sentryClient, dsn);
        } catch (Exception e) {
            log.error("Failed to initialize sentry, falling back to no-op client", e);
            return new SentryClient(new NoopConnection(), new ThreadLocalContextManager());
        }
    }

}
