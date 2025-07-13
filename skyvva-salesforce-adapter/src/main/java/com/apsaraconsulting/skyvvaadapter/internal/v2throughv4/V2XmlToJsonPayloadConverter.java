package com.apsaraconsulting.skyvvaadapter.internal.v2throughv4;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.*;
import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.apache.commons.collections4.IteratorUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

/**
 * Transforms the V2 XML payload (hierarchical also supported) to the V2-based JSON that can be accepted by V4 API with the special flag
 * See unit tests for more details
 * @author Ilya Nesterov
 */
public class V2XmlToJsonPayloadConverter {

    private final XmlMapper XML_MAPPER = XmlMapper.builder().build();
    private final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(V2XmlToJsonPayloadConverter.class);

    public ConversionResult convert(String xmlPayload) {
        LOGGER.debug("Converting V2 xml payload to the V2 based JSON payload");
        try {
            JsonNode parsedPayload = XML_MAPPER.readTree(xmlPayload);

            if (!parsedPayload.hasNonNull("fromSystem") || !parsedPayload.hasNonNull("targetObject")) {
                throw new IllegalArgumentException("\"fromSystem\" or \"targetObject\" are null or not present in the input payload");
            }

            String skyvvaIntegration = parsedPayload.get("fromSystem").asText();
            String skyvvaInterface = parsedPayload.get("targetObject").asText();
            LOGGER.debug("Integration: {}, interface: {}", skyvvaIntegration, skyvvaInterface);
            String jsonPayload = convertToV2BasedJson(parsedPayload);

            return new ConversionResult(skyvvaIntegration, skyvvaInterface, jsonPayload);
        } catch (Exception ex) {
            throw new ConversionException("Could not convert v2 xml payload to v2-based json" , ex);
        }
    }

    private String convertToV2BasedJson(JsonNode documentRootElement) throws JsonProcessingException {
        List<SObjectRecord> sObjectRecords = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> rootElementFields = documentRootElement.fields();
        while (rootElementFields.hasNext()) {
            Map.Entry<String, JsonNode> firstLevelChildEntry = rootElementFields.next();
            sObjectRecords.addAll(processObjectOrArrayElement(
                firstLevelChildEntry.getKey(),
                firstLevelChildEntry.getValue()
            ));
        }
        return JSON_MAPPER.writeValueAsString(sObjectRecords);
    }

    /*
        The main purpose of the method is to handle different cases during iteration of object/array elements at some level. Element can be:
        - single SObject
        - collection of SObjects
        - wrapping element with the single SObject inside
        - wrapping element with the collection of SObjects inside

        Identification of wrapping element is simple - it has only one child element.
        We don't expect that SObject will have just 1 child, so, it should be ok.

        Non object/array elements are skipped.
     */
    private List<SObjectRecord> processObjectOrArrayElement(String elementName, JsonNode element) {
        List<SObjectRecord> sObjectRecords = new ArrayList<>();
        if (element.isArray()) {
            // collection of SObjects
            for (JsonNode sObjectNode : element) {
                sObjectRecords.addAll(parseSObject(elementName, sObjectNode));
            }
        } else if (element.isObject()) {
            List<String> elementFields = IteratorUtils.toList(element.fieldNames());
            if (elementFields.size() == 1) {
                // first level child object is the aggregation element, so we should expect the main element for parsing inside
                // either as a collection or as a single object
                String elementField = elementFields.get(0);
                if (element.get(elementField).isArray()) {
                    // wrapping element with the collection of SObjects inside
                    for (JsonNode sObjectNode : element.get(elementField)) {
                        sObjectRecords.addAll(parseSObject(elementField, sObjectNode));
                    }
                } else {
                    // wrapping element with the single SObject inside
                    JsonNode sObjectNode = element.get(elementField);
                    sObjectRecords.addAll(parseSObject(elementField, sObjectNode));
                }
            } else {
                // single SObject
                sObjectRecords.addAll(parseSObject(elementName, element));
            }
        }
        return sObjectRecords;
    }


    private List<SObjectRecord> parseSObject(
        String sObjectName,
        JsonNode sObjectNode
    ) {
        List<SObjectRecord> sObjectRecords = new ArrayList<>();
        SObjectRecord sObjectRecord = new SObjectRecord();

        List<Map.Entry<String, JsonNode>> sObjectFields = IteratorUtils.toList(sObjectNode.fields());
        boolean hierarchical = checkIfHierarchical(sObjectFields);

        String valueFieldItemPrefix = sObjectName + ".";

        // only process value nodes to prepare fields related to the main record
        for (Map.Entry<String, JsonNode> field: sObjectFields) {
            if (field.getValue().isValueNode()) {
                sObjectRecord.getOneRecord().add(new SObjectRecordItem(
                    format("%s%s", valueFieldItemPrefix, field.getKey()),
                    field.getValue().asText()
                ));
            }
        }

        // for non-hierarchical nothing more to do
        if (!hierarchical) {
            sObjectRecords.add(sObjectRecord);
            return sObjectRecords;
        }

        // for hierarchical we should iterate all nested sObjects and convert them into the flat structure
        // appending the items parsed from the current sObject
        for (Map.Entry<String, JsonNode> field: sObjectFields) {
            List<SObjectRecord> nestedSObjectRecords = processObjectOrArrayElement(field.getKey(), field.getValue());
            for (SObjectRecord nestedSObjectRecord : nestedSObjectRecords) {
                SObjectRecord flatNestedSObject = sObjectRecord.copy();
                flatNestedSObject.getOneRecord().addAll(nestedSObjectRecord.getOneRecord());
                sObjectRecords.add(flatNestedSObject);
            }
        }
        return sObjectRecords;
    }

    private boolean checkIfHierarchical(List<Map.Entry<String, JsonNode>> sObjectFields) {
        for (Map.Entry<String, JsonNode> field: sObjectFields) {
            if (field.getValue().isObject() || field.getValue().isArray()) {
                return true;
            }
        }
        return false;
    }

    private static class SObjectRecord {
        private List<SObjectRecordItem> oneRecord;

        public List<SObjectRecordItem> getOneRecord() {
            if (oneRecord == null) {
                oneRecord = new ArrayList<>();
            }
            return oneRecord;
        }

        public SObjectRecord copy() {
            SObjectRecord sObjectRecord = new SObjectRecord();
            for (SObjectRecordItem sObjectRecordItem : getOneRecord()) {
                sObjectRecord.getOneRecord().add(sObjectRecordItem.copy());
            }
            return sObjectRecord;
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter @Setter
    @ToString
    private static class SObjectRecordItem {
        private String name;
        private String value;

        public SObjectRecordItem copy() {
            return new SObjectRecordItem(name, value);
        }
    }
}
