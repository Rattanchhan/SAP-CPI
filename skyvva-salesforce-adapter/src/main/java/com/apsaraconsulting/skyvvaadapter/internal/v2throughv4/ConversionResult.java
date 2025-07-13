package com.apsaraconsulting.skyvvaadapter.internal.v2throughv4;

import lombok.*;

/**
 * @author Ilya Nesterov
 */
@AllArgsConstructor
@Getter
@ToString(of = {"skyvvaIntegration", "skyvvaInterface"})
public class ConversionResult {

    private String skyvvaIntegration;
    private String skyvvaInterface;
    private String v2BasedJsonPayload;
}
