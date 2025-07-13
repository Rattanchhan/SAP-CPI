package com.apsaraconsulting.skyvvaadapter.v2throughv4;

import com.apsaraconsulting.skyvvaadapter.utils.TestDataLoader;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.apsaraconsulting.skyvvaadapter.internal.v2throughv4.ConversionResult;
import com.apsaraconsulting.skyvvaadapter.internal.v2throughv4.V2XmlToJsonPayloadConverter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.stream.Stream;

import static com.apsaraconsulting.skyvvaadapter.utils.TestDataLoader.loadTestDataAsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;

/**
 * @author Ilya Nesterov
 */
class V2XmlToJsonPayloadConverterTest {

    @ParameterizedTest
    @MethodSource("testDataFor_convert")
    void test_convert(
        String skyvvaIntegration,
        String skyvvaInterface,
        String xmlPayloadFileName,
        String expectedJsonPayloadFileName
    ) throws IOException {
        String xmlPayload = loadTestDataAsString(xmlPayloadFileName);
        String expectedJsonPayload = loadTestDataAsString(expectedJsonPayloadFileName);

        V2XmlToJsonPayloadConverter converter = new V2XmlToJsonPayloadConverter();
        ConversionResult conversionResult = converter.convert(xmlPayload);

        assertThat(conversionResult)
            .isNotNull()
            .returns(skyvvaIntegration, from(ConversionResult::getSkyvvaIntegration))
            .returns(skyvvaInterface, from(ConversionResult::getSkyvvaInterface));

        JsonMapper mapper = new JsonMapper();
        JsonNode actualNode   = mapper.readTree(conversionResult.getV2BasedJsonPayload());
        JsonNode expectedNode = mapper.readTree(expectedJsonPayload);

        TestDataLoader.writeStringToFile(
            expectedJsonPayloadFileName.replace(".json", "-actual.json"),
            mapper.writer(new DefaultPrettyPrinter()
                .withArrayIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
            ).writeValueAsString(actualNode)
        );

        assertThat(actualNode).isEqualTo(expectedNode);
    }

    private static Stream<Arguments> testDataFor_convert() {
        return Stream.of(
            Arguments.of(
                "Ly-Na",
                "Ticket_12470_V2Account_IN",
                "test-data/v2/throughv4/flat/integrate/accounts-v2-flat.xml",
                "test-data/v2/throughv4/flat/integrate/accounts-v2-flat-converted.json"
            ),
            Arguments.of(
                "Ly-Na",
                "Ticket_12470_V2Account_IN",
                "test-data/v2/throughv4/hierarchical/level-2/accounts-v2-hierarchical.xml",
                "test-data/v2/throughv4/hierarchical/level-2/accounts-v2-hierarchical-converted.json"
            ),
            Arguments.of(
                "Ly-Na",
                "V2InterfaceViaV4IntegrateWith3LevelPayload",
                /*
                    That test case also has wrapping for all collection elements for all 3 levels.
                 */
                "test-data/v2/throughv4/hierarchical/level-3/orders-v2-hierarchical.xml",
                "test-data/v2/throughv4/hierarchical/level-3/orders-v2-hierarchical-converted.json"
            ),
            Arguments.of(
                "SMA_SAP_PRD",
                "GMS_MaterialDelivery_Async_In",
                /*
                    That test case also has wrapping for all collection elements for all 3 levels.
                 */
                "test-data/v2/throughv4/hierarchical/level-3/orders-v2-hierarchical-mixed-wrapping.xml",
                "test-data/v2/throughv4/hierarchical/level-3/orders-v2-hierarchical-mixed-wrapping-converted.json"
            )
        );
    }

}
