package net.org.selector.storer.web.rest;

import net.org.selector.storer.service.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

/**
 * SLitvinov on 25.02.2015.
 */
@Controller
@RequestMapping("/api/files")
public class ControlFileResource {
    @Inject
    private FileService fileService;

    @RequestMapping(value = "/markAsUsed", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Void> markAsUsed(@RequestBody String[] files) {
        fileService.markAsUsed(Arrays.asList(files));
        return ResponseEntity.ok().build();
    }
}
