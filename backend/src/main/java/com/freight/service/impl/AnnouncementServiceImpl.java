package com.freight.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freight.common.exception.BusinessException;
import com.freight.entity.Announcement;
import com.freight.entity.SysUser;
import com.freight.mapper.AnnouncementMapper;
import com.freight.mapper.SysUserMapper;
import com.freight.service.AnnouncementService;
import com.freight.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private static final Path ATTACHMENT_DIR = Paths.get("/Users/zoujun/Desktop/公告");
    private final AnnouncementMapper announcementMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public IPage<Announcement> page(Integer pageNum, Integer pageSize, String title) {
        IPage<Announcement> page = announcementMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Announcement>()
                        .like(StringUtils.hasText(title), Announcement::getTitle, title)
                        .orderByDesc(Announcement::getCreateTime));
        page.getRecords().forEach(announcement -> {
            announcement.setAttachments(listAttachmentNames(announcement));
            announcement.setPublisherName(resolvePublisherName(announcement.getPublishedBy()));
        });
        return page;
    }

    @Override
    public Announcement getById(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) throw new BusinessException("公告不存在");
        announcement.setPublisherName(resolvePublisherName(announcement.getPublishedBy()));
        return announcement;
    }

    @Override
    public void create(Announcement announcement) {
        validate(announcement);
        announcement.setId(null);
        announcement.setAttachmentName(null);
        announcement.setAttachmentPath(null);
        announcement.setAttachments(null);
        announcement.setPublishedBy(SecurityUtil.getCurrentUserId());
        announcement.setDeleted(null);
        announcement.setCreateTime(null);
        announcement.setUpdateTime(null);
        announcementMapper.insert(announcement);
    }

    @Override
    public void update(Long id, Announcement announcement) {
        validate(announcement);
        Announcement existing = getById(id);
        existing.setTitle(announcement.getTitle().trim());
        existing.setContent(announcement.getContent().trim());
        announcementMapper.updateById(existing);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        announcementMapper.deleteById(id);
    }

    @Override
    public List<String> listAttachments(Long id) {
        return listAttachmentNames(getById(id));
    }

    @Override
    public List<String> uploadAttachments(Long id, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) throw new BusinessException("文件不能为空");
        Announcement announcement = getById(id);
        Path dir = attachmentDir(id);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new BusinessException("创建附件目录失败: " + e.getMessage());
        }

        // 已有附件名（含历史单附件）视为已占用，新文件追加保存、绝不覆盖
        Set<String> takenNames = new HashSet<>(listAttachmentNames(announcement));
        List<String> savedNames = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            String originalName = cleanFileName(file.getOriginalFilename());
            if (!StringUtils.hasText(originalName)) throw new BusinessException("文件名不能为空");
            String storedName = uniqueName(dir, originalName, takenNames);
            try {
                file.transferTo(dir.resolve(storedName).toFile());
            } catch (IOException e) {
                throw new BusinessException("文件保存失败: " + e.getMessage());
            }
            takenNames.add(storedName);
            savedNames.add(storedName);
        }
        if (savedNames.isEmpty()) throw new BusinessException("文件不能为空");
        return savedNames;
    }

    @Override
    public void deleteAttachment(Long id, String filename) {
        Announcement announcement = getById(id);
        String name = cleanFileName(filename);
        if (!StringUtils.hasText(name)) throw new BusinessException("文件名不能为空");

        if (name.equals(announcement.getAttachmentName())) {
            deleteStoredFile(announcement.getAttachmentPath());
            announcement.setAttachmentName(null);
            announcement.setAttachmentPath(null);
            announcementMapper.updateById(announcement);
            return;
        }

        Path dir = attachmentDir(id);
        Path file = dir.resolve(name).normalize();
        if (!file.startsWith(dir)) throw new BusinessException("文件名不合法");
        try {
            if (!Files.deleteIfExists(file)) throw new BusinessException("附件不存在");
        } catch (IOException e) {
            throw new BusinessException("附件删除失败: " + e.getMessage());
        }
    }

    @Override
    public Path getAttachmentFile(Long id, String filename) {
        Announcement announcement = getById(id);
        String name = cleanFileName(filename);
        if (!StringUtils.hasText(name)) return null;

        Path dir = attachmentDir(id);
        Path file = dir.resolve(name).normalize();
        if (file.startsWith(dir) && Files.isRegularFile(file)) return file;

        // 兼容历史单附件：以 UUID 名称平铺存储在公告根目录
        if (name.equals(announcement.getAttachmentName()) && StringUtils.hasText(announcement.getAttachmentPath())) {
            Path legacy = ATTACHMENT_DIR.resolve(cleanFileName(announcement.getAttachmentPath())).normalize();
            if (legacy.startsWith(ATTACHMENT_DIR) && Files.isRegularFile(legacy)) return legacy;
        }
        return null;
    }

    private Path attachmentDir(Long id) {
        return ATTACHMENT_DIR.resolve(String.valueOf(id)).normalize();
    }

    private List<String> listAttachmentNames(Announcement announcement) {
        List<String> names = new ArrayList<>();
        if (StringUtils.hasText(announcement.getAttachmentName())) names.add(announcement.getAttachmentName());
        if (announcement.getId() == null) return names;
        Path dir = attachmentDir(announcement.getId());
        if (!Files.isDirectory(dir)) return names;
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .forEach(name -> {
                        if (!names.contains(name)) names.add(name);
                    });
        } catch (IOException ignored) {
            // 读取附件目录失败时仅返回数据库中的历史附件。
        }
        return names;
    }

    private String uniqueName(Path dir, String name, Set<String> takenNames) {
        if (!Files.exists(dir.resolve(name)) && !takenNames.contains(name)) return name;
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        for (int i = 1; ; i++) {
            String candidate = base + "(" + i + ")" + ext;
            if (!Files.exists(dir.resolve(candidate)) && !takenNames.contains(candidate)) return candidate;
        }
    }

    private void validate(Announcement announcement) {
        if (!StringUtils.hasText(announcement.getTitle())) throw new BusinessException("公告标题不能为空");
        if (announcement.getTitle().trim().length() > 100) throw new BusinessException("公告标题不能超过100个字符");
        if (!StringUtils.hasText(announcement.getContent())) throw new BusinessException("公告内容不能为空");
    }

    private String resolvePublisherName(Long userId) {
        if (userId == null) return "未知用户";
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) return "未知用户";
        if (StringUtils.hasText(user.getRealName())) return user.getRealName();
        return StringUtils.hasText(user.getUsername()) ? user.getUsername() : "未知用户";
    }

    private String cleanFileName(String filename) {
        if (!StringUtils.hasText(filename)) return "";
        return Paths.get(filename).getFileName().toString();
    }

    private void deleteStoredFile(String storedName) {
        if (!StringUtils.hasText(storedName)) return;
        try {
            Files.deleteIfExists(ATTACHMENT_DIR.resolve(cleanFileName(storedName)).normalize());
        } catch (IOException ignored) {
            // 删除磁盘附件失败不影响公告记录的删除或替换。
        }
    }
}
