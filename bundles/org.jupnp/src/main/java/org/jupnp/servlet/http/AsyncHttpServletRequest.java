package org.jupnp.servlet.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.jupnp.servlet.AsyncContext;
import org.jupnp.servlet.impl.AsyncContextImpl;


public class AsyncHttpServletRequest extends HttpServletRequestWrapper
        implements HttpServletRequest {

    private AsyncContext asyncContext;
    private AsyncHttpServlet asyncHttpServlet;


    public AsyncHttpServletRequest(HttpServletRequest request, AsyncHttpServlet asyncHttpServlet) {
        super(request);
        this.asyncHttpServlet = asyncHttpServlet;
    }

    public AsyncContext startAsync2() {
        this.asyncContext = new AsyncContextImpl(
                super.getRequest(), this.asyncHttpServlet.getResponse());

        return this.asyncContext;
    }

    public boolean isAsyncStarted() {
        return (this.asyncContext != null);
    }

    public AsyncContext getAsyncContext2() throws IllegalStateException {
        if (isAsyncStarted()) {
            return this.asyncContext;
        }
 
        throw new IllegalStateException("The request is not in asynchronous mode!");
    }

}
