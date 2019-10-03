/*
 * The MIT License
 *
 * Copyright 2017 Intuit Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.UUID;

/**
 *
 * @author pthomas3
 */
@Path("ghc")
public class GHCResource {

    private static final Logger logger = LoggerFactory.getLogger(GHCResource.class);

    @GET
    @Produces("text/plain")
    public String getHello() {
        return "hello ghc attendees";
    }

    @POST
    @Produces("application/json")
    @Path("/login")
    public String createLogin(String request) {
        logger.debug("Received request to create login: {}", request);
        if(request.contains("mary@zz.com")) {
            return "{ \"userName\": \"mary@zz.com\" }";
        }
        else if(request.contains("mary@xx.com")) {
            return "{ \"errorCode\": \"login-svc-100\", \"errorMessage\": \"Login already exists with the given email\" }";
        } else if (request.contains("yuhj")) {
            return "{ \"errorCode\": \"login-svc-200\", \"errorMessage\": \"Password should be at least 8 characters or more.\" }";
        }
        else if (request.contains("jenny@zz.com.com")) {
            return "{ \"errorCode\": \"login-svc-300\", \"errorMessage\": \"Email address is invalid.\" }";
        }
        else {
            return "{ \"userName\": \"mary@xx.com\" }";
        }

    }

    @POST
    @Produces("application/json")
    @Path("/register")
    public String register(String request) {
        logger.info("Received registration request: {}", request);

        String confirmationNumber = UUID.randomUUID().toString();;
        return "{ \"confirmationNumber\": " +  confirmationNumber + "}";


    }

}
