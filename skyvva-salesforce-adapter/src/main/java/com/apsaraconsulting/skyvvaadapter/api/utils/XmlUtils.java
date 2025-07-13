package com.apsaraconsulting.skyvvaadapter.api.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.apsaraconsulting.skyvvaadapter.internal.dto.RestChoices;
import com.apsaraconsulting.skyvvaadapter.internal.dto.RestErrors;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * @author Ilya Nesterov
 */
public class XmlUtils {

    public static XmlMapper createXmlMapper(Class<?>... extraSubtypes) {
        XmlMapper xml = XmlMapper.builder()
            .defaultUseWrapper(false)
            .configure(SerializationFeature.INDENT_OUTPUT, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
            .build();

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ROOT);
        xml.setDateFormat(df);

        xml.registerModule(new TimeModule());

        xml.registerSubtypes(RestErrors.class);
        xml.registerSubtypes(RestChoices.class);

        for (Class<?> c : extraSubtypes) {
            xml.registerSubtypes(c);
        }

        return xml;
    }
}
