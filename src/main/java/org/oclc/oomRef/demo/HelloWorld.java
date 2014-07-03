/**
 * Copyright 2006 OCLC Online Computer Library Center Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.oclc.oomRef.demo;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import info.openurl.oom.ContextObject;
import info.openurl.oom.OpenURLRequest;
import info.openurl.oom.OpenURLRequestProcessor;
import info.openurl.oom.OpenURLResponse;
import info.openurl.oom.Service;
import info.openurl.oom.config.ClassConfig;
import info.openurl.oom.config.OpenURLConfig;
import info.openurl.oom.entities.ServiceType;

/**
 * A web service to say "Hello" to someone.
 *
 * @author Jeffrey A. Young
 */
public class HelloWorld implements Service {

    private final String something;

    /**
     * Construct a Hello World web service class.
     *
     * @param openURLConfig
     * @param classConfig
     * @throws TransformerException
     */
    public HelloWorld(final OpenURLConfig openURLConfig, final ClassConfig classConfig) throws TransformerException {
        something = classConfig.getArg("something");
    }

    /**
     * Returns the service ID.
     */
    @Override
    public URI getServiceID() throws URISyntaxException {
        return new URI("info:localhost/svc_id/HelloWorld");
    }

    /**
     * Say "Hello" to someone.
     *
     * @param name the name of the person you want to greet.
     * @return a personal greeting
     */
    public String sayHello(final String name) {
        return "Hello " + name + " (" + something + ")";
    }

    /**
     * Resolves the OpenURLResponse.
     *
     * @param serviceType A service type
     * @param contextObject A context object
     * @param openURLRequest An OpenURL request
     * @param processor An OpenURL request processor
     */
    @Override
    public OpenURLResponse resolve(final ServiceType serviceType, final ContextObject contextObject,
            final OpenURLRequest openURLRequest, final OpenURLRequestProcessor processor)
            throws UnsupportedEncodingException {
        final String[] privateData = (String[]) contextObject.getReferent().getDescriptors(String.class);
        final String name = privateData[0];
        return new OpenURLResponse(HttpServletResponse.SC_OK, "text/plain; charset=utf-8", sayHello(name).getBytes(
                "UTF-8"));
    }

}
