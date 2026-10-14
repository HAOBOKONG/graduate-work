package com.example.market.controller;

import com.example.market.service.ImageUploadService;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/uploads/images")
public class ImageUploadController {
    private final ImageUploadService uploads;

    public ImageUploadController(ImageUploadService uploads) { this.uploads = uploads; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) {
        return Map.of("url", uploads.save(file));
    }

    @GetMapping("/{name}")
    public ResponseEntity<Resource> image(@PathVariable String name) {
        Resource image = uploads.load(name);
        MediaType type = name.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type).cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS)).body(image);
    }
}
