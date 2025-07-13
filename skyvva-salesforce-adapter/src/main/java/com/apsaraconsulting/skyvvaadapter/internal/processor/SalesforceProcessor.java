/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apsaraconsulting.skyvvaadapter.internal.processor;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Service;

import java.util.*;
import java.util.stream.Collectors;

public interface SalesforceProcessor extends Service {

    boolean process(Exchange exchange, AsyncCallback callback);

    default Map<String, List<String>> determineHeaders(final Exchange exchange) {
        final Message inboundMessage = exchange.getIn();

        final Map<String, Object> headers = inboundMessage.getHeaders();

        final Map<String, List<String>> answer = new HashMap<>();
        for (final String headerName : headers.keySet()) {
            final String headerNameLowercase = headerName.toLowerCase(Locale.US);
            if (headerNameLowercase.startsWith("sforce") || headerNameLowercase.startsWith("x-sfdc")) {
                final Object headerValue = inboundMessage.getHeader(headerName);

                if (headerValue instanceof String) {
                    answer.put(headerName, Collections.singletonList((String) headerValue));
                } else if (headerValue instanceof String[]) {
                    answer.put(headerName, Arrays.asList((String[]) headerValue));
                } else if (headerValue instanceof Collection) {
                    Collection<?> collection = (Collection<?>) headerValue;
                    answer.put(headerName, collection.stream().map(String.class::cast).collect(Collectors.toList()));
                } else {
                    answer.put(headerName, Collections.singletonList(String.valueOf(headerValue)));
                }
            }
        }

        return answer;
    }

}
