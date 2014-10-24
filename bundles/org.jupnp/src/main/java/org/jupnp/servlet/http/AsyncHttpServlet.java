package org.jupnp.servlet.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jupnp.servlet.impl.AsyncContextImpl;


public abstract class AsyncHttpServlet extends HttpServlet {

    private static final long serialVersionUID = 5234053730076572677L;

    private HttpServletResponse response;


    @Override
    protected final void service(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        this.response = response;

        AsyncHttpServletRequest asyncRequest = new AsyncHttpServletRequest(request, this);

        // now execute the code (usually this call returns fast)
        service(asyncRequest, response);

        // wait for completion
        if (asyncRequest.isAsyncStarted()) {
            AsyncContextImpl asyncContextImpl = (AsyncContextImpl) asyncRequest.getAsyncContext2();
            asyncContextImpl.setStarted();
            asyncContextImpl.waitForCompletion();
        }

        this.response = null;
    }

    protected void service(AsyncHttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
    }

    protected HttpServletResponse getResponse() {
        return this.response;
    }

}
