package com.freight.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.freight.entity.Announcement;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

public interface AnnouncementService {
    IPage<Announcement> page(Integer pageNum, Integer pageSize, String title);
    Announcement getById(Long id);
    void create(Announcement announcement);
    void update(Long id, Announcement announcement);
    void delete(Long id);
    List<String> listAttachments(Long id);
    List<String> uploadAttachments(Long id, List<MultipartFile> files);
    void deleteAttachment(Long id, String filename);
    Path getAttachmentFile(Long id, String filename);
}
