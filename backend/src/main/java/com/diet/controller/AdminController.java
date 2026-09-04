package com.diet.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diet.common.BusinessException;
import com.diet.common.Result;
import com.diet.entity.CommunityPost;
import com.diet.entity.ContentReport;
import com.diet.entity.FoodNutrition;
import com.diet.entity.PostComment;
import com.diet.entity.SysUser;
import com.diet.mapper.CommunityPostMapper;
import com.diet.mapper.ContentReportMapper;
import com.diet.mapper.DietRecordMapper;
import com.diet.mapper.FoodNutritionMapper;
import com.diet.mapper.PostCommentMapper;
import com.diet.mapper.UserMapper;
import com.diet.mapper.WeightRecordMapper;
import com.diet.util.SensitiveWordFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理员后台控制器
 * 路径前缀 /api/admin 由 JwtInterceptor 统一做管理员角色校验(普通用户403)
 *
 * @author diet
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserMapper userMapper;
    private final CommunityPostMapper postMapper;
    private final PostCommentMapper commentMapper;
    private final ContentReportMapper reportMapper;
    private final FoodNutritionMapper foodMapper;
    private final DietRecordMapper dietRecordMapper;
    private final WeightRecordMapper weightRecordMapper;
    private final SensitiveWordFilter sensitiveFilter;

    public AdminController(UserMapper userMapper, CommunityPostMapper postMapper,
                            PostCommentMapper commentMapper, ContentReportMapper reportMapper,
                            FoodNutritionMapper foodMapper, DietRecordMapper dietRecordMapper,
                            WeightRecordMapper weightRecordMapper, SensitiveWordFilter sensitiveFilter) {
        this.userMapper = userMapper;
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
        this.reportMapper = reportMapper;
        this.foodMapper = foodMapper;
        this.dietRecordMapper = dietRecordMapper;
        this.weightRecordMapper = weightRecordMapper;
        this.sensitiveFilter = sensitiveFilter;
    }

    // ==================== 1. 用户管理 ====================

    /**
     * 用户列表(支持关键字搜索/角色/状态筛选)
     */
    @GetMapping("/user/list")
    public Result<IPage<Map<String, Object>>> userList(@RequestParam(defaultValue = "1") long current,
                                                       @RequestParam(defaultValue = "10") long size,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String role,
                                                       @RequestParam(required = false) Integer status) {
        IPage<SysUser> page = userMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<SysUser>()
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(SysUser::getUsername, keyword)
                                .or().like(SysUser::getNickname, keyword))
                        .eq(StringUtils.hasText(role), SysUser::getRole, role)
                        .eq(status != null, SysUser::getStatus, status)
                        .orderByDesc(SysUser::getCreateTime));
        Page<Map<String, Object>> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::userVO).toList());
        return Result.success(voPage);
    }

    /**
     * 禁用/启用用户
     */
    @PutMapping("/user/{id}/status")
    public Result<Void> toggleUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        if (status != 0 && status != 1) {
            throw new BusinessException("状态值不合法");
        }
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
        return Result.success();
    }

    /**
     * 分配角色(USER/ADMIN)
     */
    @PutMapping("/user/{id}/role")
    public Result<Void> assignRole(@PathVariable Long id, @RequestParam String role) {
        if (!"USER".equals(role) && !"ADMIN".equals(role)) {
            throw new BusinessException("角色值不合法");
        }
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setRole(role);
        userMapper.updateById(user);
        return Result.success();
    }

    // ==================== 2. 内容审核 ====================

    /**
     * 删除动态(管理端强制删除, 逻辑删除)
     */
    @org.springframework.web.bind.annotation.DeleteMapping("/content/post/{id}")
    public Result<Void> deletePost(@PathVariable Long id) {
        postMapper.deleteById(id);
        return Result.success();
    }

    /**
     * 动态列表(含被举报数统计)
     */
    @GetMapping("/content/posts")
    public Result<IPage<Map<String, Object>>> posts(@RequestParam(defaultValue = "1") long current,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) String keyword) {
        IPage<CommunityPost> page = postMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<CommunityPost>()
                        .like(StringUtils.hasText(keyword), CommunityPost::getTitle, keyword)
                        .orderByDesc(CommunityPost::getPublishTime));
        Page<Map<String, Object>> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        // 批量作者昵称
        Map<Long, SysUser> userMap = page.getRecords().isEmpty() ? Map.of()
                : userMapper.selectBatchIds(page.getRecords().stream()
                        .map(CommunityPost::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(SysUser::getId, Function.identity()));
        voPage.setRecords(page.getRecords().stream().map(p -> {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", p.getId());
            vo.put("title", p.getTitle());
            vo.put("authorId", p.getUserId());
            SysUser u = userMap.get(p.getUserId());
            vo.put("authorName", u != null ? u.getNickname() : "未知");
            vo.put("likeCount", p.getLikeCount());
            vo.put("commentCount", p.getCommentCount());
            vo.put("publishTime", p.getPublishTime());
            vo.put("reported", reportMapper.selectCount(new LambdaQueryWrapper<ContentReport>()
                    .eq(ContentReport::getTargetType, "POST")
                    .eq(ContentReport::getTargetId, p.getId())));
            return vo;
        }).toList());
        return Result.success(voPage);
    }

    /**
     * 评论列表
     */
    @GetMapping("/content/comments")
    public Result<IPage<Map<String, Object>>> comments(@RequestParam(defaultValue = "1") long current,
                                                       @RequestParam(defaultValue = "10") long size,
                                                       @RequestParam(required = false) Integer status) {
        IPage<PostComment> page = commentMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<PostComment>()
                        .eq(status != null, PostComment::getStatus, status)
                        .orderByDesc(PostComment::getCreateTime));
        Page<Map<String, Object>> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        Map<Long, SysUser> userMap = page.getRecords().isEmpty() ? Map.of()
                : userMapper.selectBatchIds(page.getRecords().stream()
                        .map(PostComment::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(SysUser::getId, Function.identity()));
        voPage.setRecords(page.getRecords().stream().map(c -> {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", c.getId());
            vo.put("postId", c.getPostId());
            vo.put("content", c.getContent());
            SysUser u = userMap.get(c.getUserId());
            vo.put("authorName", u != null ? u.getNickname() : "未知");
            vo.put("status", c.getStatus());
            vo.put("createTime", c.getCreateTime());
            return vo;
        }).toList());
        return Result.success(voPage);
    }

    /**
     * 屏蔽/恢复评论
     */
    @PutMapping("/content/comment/{id}/status")
    public Result<Void> toggleComment(@PathVariable Long id, @RequestParam Integer status) {
        PostComment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        comment.setStatus(status);
        commentMapper.updateById(comment);
        return Result.success();
    }

    /**
     * 举报列表(待处理/全部)
     */
    @GetMapping("/content/reports")
    public Result<IPage<Map<String, Object>>> reports(@RequestParam(defaultValue = "1") long current,
                                                      @RequestParam(defaultValue = "10") long size,
                                                      @RequestParam(required = false) Integer status) {
        IPage<ContentReport> page = reportMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<ContentReport>()
                        .eq(status != null, ContentReport::getStatus, status)
                        .orderByAsc(ContentReport::getStatus)
                        .orderByDesc(ContentReport::getCreateTime));
        Page<Map<String, Object>> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        Map<Long, SysUser> userMap = page.getRecords().isEmpty() ? Map.of()
                : userMapper.selectBatchIds(page.getRecords().stream()
                        .map(ContentReport::getReporterId).distinct().toList())
                .stream().collect(Collectors.toMap(SysUser::getId, Function.identity()));
        voPage.setRecords(page.getRecords().stream().map(r -> {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", r.getId());
            vo.put("targetType", r.getTargetType());
            vo.put("targetId", r.getTargetId());
            vo.put("reason", r.getReason());
            vo.put("status", r.getStatus());
            vo.put("handleResult", r.getHandleResult());
            vo.put("createTime", r.getCreateTime());
            SysUser u = userMap.get(r.getReporterId());
            vo.put("reporterName", u != null ? u.getNickname() : "未知");
            // 被举报内容预览
            if ("POST".equals(r.getTargetType())) {
                CommunityPost p = postMapper.selectById(r.getTargetId());
                vo.put("targetPreview", p != null ? p.getTitle() : "(已删除)");
            } else {
                PostComment c = commentMapper.selectById(r.getTargetId());
                vo.put("targetPreview", c != null ? c.getContent() : "(已删除)");
            }
            return vo;
        }).toList());
        return Result.success(voPage);
    }

    /**
     * 处理举报(记录结果, 可选屏蔽目标内容)
     */
    @PostMapping("/content/report/{id}/handle")
    public Result<Void> handleReport(@PathVariable Long id,
                                      @RequestParam(required = false) Integer blockTarget,
                                      @RequestParam(required = false) String result) {
        ContentReport report = reportMapper.selectById(id);
        if (report == null) {
            throw new BusinessException("举报不存在");
        }
        // 可选屏蔽目标内容
        if (blockTarget != null && blockTarget == 1) {
            if ("POST".equals(report.getTargetType())) {
                postMapper.deleteById(report.getTargetId());
            } else {
                PostComment c = commentMapper.selectById(report.getTargetId());
                if (c != null) {
                    c.setStatus(1);
                    commentMapper.updateById(c);
                }
            }
        }
        report.setStatus(1);
        report.setHandleResult(StringUtils.hasText(result) ? result : "已处理");
        report.setHandleTime(LocalDateTime.now());
        reportMapper.updateById(report);
        return Result.success();
    }

    // ==================== 3. 食材营养库管理 ====================

    /**
     * 食材分页管理(复用user_food接口规则, 管理端独立入口)
     */
    @GetMapping("/food/page")
    public Result<IPage<FoodNutrition>> foodPage(@RequestParam(defaultValue = "1") long current,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String category) {
        return Result.success(foodMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<FoodNutrition>()
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(FoodNutrition::getFoodName, keyword)
                                .or().like(FoodNutrition::getAlias, keyword))
                        .eq(StringUtils.hasText(category), FoodNutrition::getCategory, category)
                        .orderByAsc(FoodNutrition::getId)));
    }

    /**
     * 新增食材
     */
    @PostMapping("/food")
    public Result<Void> foodSave(@RequestBody FoodNutrition food) {
        // 名称唯一校验
        Long count = foodMapper.selectCount(new LambdaQueryWrapper<FoodNutrition>()
                .eq(FoodNutrition::getFoodName, food.getFoodName()));
        if (count > 0) {
            throw new BusinessException("食材名称已存在");
        }
        food.setId(null);
        foodMapper.insert(food);
        return Result.success();
    }

    /**
     * 修改食材
     */
    @PutMapping("/food")
    public Result<Void> foodUpdate(@RequestBody FoodNutrition food) {
        if (food.getId() == null) {
            throw new BusinessException("食材ID不能为空");
        }
        foodMapper.updateById(food);
        return Result.success();
    }

    /**
     * 删除食材(逻辑删除)
     */
    @DeleteMapping("/food/{id}")
    public Result<Void> foodDelete(@PathVariable Long id) {
        foodMapper.deleteById(id);
        return Result.success();
    }

    // ==================== 4. 平台运营数据统计 ====================

    /**
     * 运营总览: 用户/动态/评论/食材/打卡数 + 近7日新增用户与打卡趋势
     */
    @GetMapping("/stats/overview")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("userCount", userMapper.selectCount(null));
        vo.put("postCount", postMapper.selectCount(null));
        vo.put("commentCount", commentMapper.selectCount(null));
        vo.put("foodCount", foodMapper.selectCount(null));
        vo.put("dietRecordCount", dietRecordMapper.selectCount(null));
        vo.put("pendingReports", reportMapper.selectCount(
                new LambdaQueryWrapper<ContentReport>().eq(ContentReport::getStatus, 0)));
        // 近7日新增用户
        List<Map<String, Object>> newUsers = new java.util.ArrayList<>();
        List<Map<String, Object>> dietTrend = new java.util.ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            Map<String, Object> u = new LinkedHashMap<>();
            u.put("date", d.toString());
            u.put("count", userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .ge(SysUser::getCreateTime, d.atStartOfDay())
                    .lt(SysUser::getCreateTime, d.plusDays(1).atStartOfDay())));
            newUsers.add(u);
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("date", d.toString());
            t.put("count", dietRecordMapper.selectCount(
                    new LambdaQueryWrapper<com.diet.entity.DietRecord>()
                            .eq(com.diet.entity.DietRecord::getRecordDate, d)));
            dietTrend.add(t);
        }
        vo.put("newUserTrend", newUsers);
        vo.put("dietTrend", dietTrend);
        return Result.success(vo);
    }

    /**
     * 用户脱敏视图
     */
    private Map<String, Object> userVO(SysUser u) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", u.getId());
        vo.put("username", u.getUsername());
        vo.put("nickname", u.getNickname());
        vo.put("role", u.getRole());
        vo.put("status", u.getStatus());
        vo.put("gender", u.getGender());
        vo.put("age", u.getAge());
        vo.put("createTime", u.getCreateTime());
        return vo;
    }
}