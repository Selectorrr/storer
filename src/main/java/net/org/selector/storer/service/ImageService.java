package net.org.selector.storer.service;

import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import net.org.selector.storer.domain.ResizeInfo;
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

    private static final String MODE_RESIZE = "resize";
    private static final String MODE_CROP = "crop";

    public ResizeInfo resizeImage(String name, InputStream input, String contentType, String imageSize, String mode) throws IOException {
        Iterable<String> sizes = Splitter.on('x').split(imageSize);
        int targetWidth = Integer.parseInt(Iterables.get(sizes, 0));
        int targetHeight = Integer.parseInt(Iterables.get(sizes, 1));
        BufferedImage image = ImageIO.read(input);
        mode = Objects.firstNonNull(mode, MODE_RESIZE);
        if (image.getWidth() > targetWidth || image.getHeight() > targetHeight) {
            if (MODE_RESIZE.equals(mode)) {
                if (image.getWidth() < image.getHeight()) {
                    image = Scalr.resize(image, Scalr.Method.QUALITY, Scalr.Mode.FIT_TO_HEIGHT, targetWidth, targetHeight, Scalr.OP_ANTIALIAS);
                } else {
                    image = Scalr.resize(image, Scalr.Method.QUALITY, Scalr.Mode.FIT_TO_WIDTH, targetWidth, targetHeight, Scalr.OP_ANTIALIAS);
                }
            } else if (MODE_CROP.equals(mode)) {
                if (image.getWidth() < image.getHeight()) {
                    image = Scalr.resize(image, Scalr.Method.QUALITY, Scalr.Mode.FIT_TO_WIDTH, targetWidth, targetHeight, Scalr.OP_ANTIALIAS);
                } else {
                    image = Scalr.resize(image, Scalr.Method.QUALITY, Scalr.Mode.FIT_TO_HEIGHT, targetWidth, targetHeight, Scalr.OP_ANTIALIAS);
                }
                if (image.getWidth() > targetWidth) {
                    image = Scalr.crop(image, (image.getWidth() - targetWidth) / 2, 0, targetWidth, targetHeight);
                }
                if (image.getHeight() > targetHeight) {
                    image = Scalr.crop(image, 0, (image.getHeight() - targetHeight) / 2, targetWidth, targetHeight);
                }
            }


        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        String extension = FilenameUtils.getExtension(name);
        name = name.replace(extension, String.format("%sx%s.%s", targetWidth, targetHeight, extension));
        ImageIO.write(image, extension, os);
        ResizeInfo result = new ResizeInfo();
        result.setName(name);
        result.setContentType(contentType);
        result.setByteArrayInputStream(new ByteArrayInputStream(os.toByteArray()));
        result.setWidth(String.valueOf(image.getWidth()));
        result.setHeight(String.valueOf(image.getHeight()));
        return result;
    }


}
