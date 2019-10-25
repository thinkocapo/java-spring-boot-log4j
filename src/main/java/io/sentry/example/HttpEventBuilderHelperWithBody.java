package io.sentry.example;

import io.sentry.event.*;
import io.sentry.event.helper.*;

import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;

import io.sentry.event.interfaces.HttpInterface;
import io.sentry.event.interfaces.UserInterface;

import java.io.UnsupportedEncodingException;


public class HttpEventBuilderHelperWithBody extends HttpEventBuilderHelper {

    @Override
    public void helpBuildingEvent(EventBuilder eventBuilder) {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
        ContentCachingRequestWrapper wrapper =
                WebUtils.getNativeRequest(requestAttributes.getRequest(), ContentCachingRequestWrapper.class);
        String body = null;
        if (wrapper != null) {
            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length > 0) {
                try {
                    body = new String(buf,0,buf.length,wrapper.getCharacterEncoding());
                } catch (UnsupportedEncodingException e) {
                }
            }
        }
        eventBuilder.withSentryInterface(new HttpInterface(requestAttributes.getRequest(),super.getRemoteAddressResolver(),body),false);
        addUserInterface(eventBuilder,requestAttributes.getRequest());
    }

    private void addUserInterface(EventBuilder eventBuilder, HttpServletRequest servletRequest) {
        String username = null;
        if (servletRequest.getUserPrincipal() != null) {
            username = servletRequest.getUserPrincipal().getName();
        }

        UserInterface userInterface = new UserInterface(null, username,
                super.getRemoteAddressResolver().getRemoteAddress(servletRequest), null);
        eventBuilder.withSentryInterface(userInterface, false);
    }
}