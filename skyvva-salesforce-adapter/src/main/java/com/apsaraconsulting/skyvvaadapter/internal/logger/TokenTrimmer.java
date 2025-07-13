package com.apsaraconsulting.skyvvaadapter.internal.logger;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Ilya Nesterov
 */
public class TokenTrimmer {

    public static String trim(String token) {
        if (StringUtils.isEmpty(token) || token.length() < 14) {
            return token;
        }

        return token.substring(token.length() - 14);
    }
}
