/*
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license.
 * For full license text, see LICENSE.TXT file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.apsaraconsulting.skyvvaadapter.internal.streaming.empconnector;

import java.util.concurrent.Future;

/**
 * A subscription to a topic
 *
 * @author hal.hildebrand
 * @since API v37.0
 */
public interface TopicSubscription {

    /**
     * Cancel the subscription
     */
    Future<Boolean> cancel();

    /**
     * @return the current replayFrom event id of the subscription
     */
    long getReplayFrom();

    /**
     * @return the topic subscribed to
     */
    String getTopic();

}
