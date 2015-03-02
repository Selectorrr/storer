package net.org.selector.storer.web.rest;

import net.org.selector.storer.domain.Prop;
import net.org.selector.storer.repository.FilePropsRepository;
import net.org.selector.storer.service.FileService;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import java.util.Arrays;

/**
 * SLitvinov on 25.02.2015.
 */
@Controller
@RequestMapping("/api/files")
public class ControlFileResource {
    @Inject
    private FileService fileService;
    @Inject
    private FilePropsRepository filePropsRepository;

    @RequestMapping(value = "/markAsUsed", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Void> markAsUsed(@RequestBody String[] files) {
        fileService.markAsUsed(Arrays.asList(files));
        filePropsRepository.save(new Prop("lastSync", new LocalDateTime()));
        return ResponseEntity.ok().build();
    }
}
