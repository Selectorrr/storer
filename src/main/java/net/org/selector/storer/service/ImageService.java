package net.org.selector.storer.service;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import org.apache.commons.io.FilenameUtils;
import org.imgscalr.Scalr;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * SLitvinov on 26.02.2015.
 */
@Service
public class ImageService {
    public Table.Cell<String, String, InputStream> resizeImage(String name, InputStream input, String contentType, String imageSize) throws IOException {
        Iterable<String> sizes = Splitter.on('x').split(imageSize);
        int targetWidth = Integer.parseInt(Iterables.get(sizes, 0));
        int targetHeight = Integer.parseInt(Iterables.get(sizes, 1));
        BufferedImage image = ImageIO.read(input);
        if (image.getWidth() > targetWidth || image.getHeight() > targetHeight) {
            image = Scalr.resize(image, Scalr.Method.QUALITY, Scalr.Mode.FIT_TO_HEIGHT, targetWidth, targetHeight, Scalr.OP_ANTIALIAS);
            if (image.getWidth() > targetWidth) {
                image = Scalr.crop(image, (image.getWidth() - targetWidth) / 2, 0, targetWidth, targetHeight);
            }
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        String extension = FilenameUtils.getExtension(name);
        name = name.replace(extension, String.format("%sx%s.%s", targetWidth, targetHeight, extension));
        ImageIO.write(image, extension, os);
        return Tables.immutableCell(name, contentType, new ByteArrayInputStream(os.toByteArray()));
    }
}
