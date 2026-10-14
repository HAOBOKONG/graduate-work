package com.example.market.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ImageUploadService {
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Pattern FILE_NAME = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png)");
    private final Path directory;

    public ImageUploadService(@Value("${app.upload.dir:uploads}") String uploadDirectory) {
        this.directory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    public String save(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片不能为空且不能超过5MB");
        }
        String type = file.getContentType();
        String format;
        if ("image/jpeg".equals(type)) format = "jpg";
        else if ("image/png".equals(type)) format = "png";
        else throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持JPG和PNG图片");

        BufferedImage image;
        try {
            image = ImageIO.read(file.getInputStream());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片内容无效");
        }
        if (image == null || image.getWidth() > 6000 || image.getHeight() > 6000
                || image.getWidth() < 1 || image.getHeight() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片内容无效或尺寸超过6000像素");
        }
        try {
            Files.createDirectories(directory);
            String name = UUID.randomUUID() + "." + format;
            Path target = directory.resolve(name);
            if (!ImageIO.write(image, format.equals("jpg") ? "jpeg" : "png", target.toFile())) {
                throw new IOException("No image writer for " + format);
            }
            return "/api/uploads/images/" + name;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "图片保存失败");
        }
    }

    public Resource load(String name) {
        if (!FILE_NAME.matcher(name).matches()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图片不存在");
        }
        Path target = directory.resolve(name);
        if (!Files.isRegularFile(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图片不存在");
        }
        return new FileSystemResource(target);
    }

    public boolean isManagedImage(String url) {
        String prefix = "/api/uploads/images/";
        return url.startsWith(prefix) && FILE_NAME.matcher(url.substring(prefix.length())).matches()
            && Files.isRegularFile(directory.resolve(url.substring(prefix.length())));
    }
}
