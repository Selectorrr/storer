package net.org.selector.storer.config;

import com.fasterxml.jackson.datatype.joda.JodaModule;
import net.org.selector.storer.domain.util.CustomDateTimeDeserializer;
import net.org.selector.storer.domain.util.CustomDateTimeSerializer;
import net.org.selector.storer.domain.util.CustomLocalDateSerializer;
import net.org.selector.storer.domain.util.ISO8601LocalDateDeserializer;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfiguration {

    @Bean
    public JodaModule jacksonJodaModule() {
        JodaModule module = new JodaModule();
        module.addSerializer(DateTime.class, new CustomDateTimeSerializer());
        module.addDeserializer(DateTime.class, new CustomDateTimeDeserializer());
        module.addSerializer(LocalDate.class, new CustomLocalDateSerializer());
        module.addDeserializer(LocalDate.class, new ISO8601LocalDateDeserializer());
        return module;
    }
}
