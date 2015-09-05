package net.org.selector.storer.service.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Created by Selector on 23.08.2015.
 */
public class FailSafeObjectMapper extends ObjectMapper {
    public FailSafeObjectMapper() {
        this.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        this.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);
        this.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        this.configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, false);
        this.configure(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY, false);
    }
}
