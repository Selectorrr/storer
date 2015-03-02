package net.org.selector.storer.repository;

import net.org.selector.storer.domain.Prop;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Map;

/**
 * SLitvinov on 02.03.2015.
 */
public interface FilePropsRepository extends MongoRepository<Prop, String> {
}
