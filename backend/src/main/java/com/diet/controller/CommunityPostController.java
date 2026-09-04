package com.diet.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diet.common.BusinessException;
import com.diet.common.Result;
import com.diet.common.UserContext;
import com.diet.entity.CommunityPost;
import com.diet.service.CommunityPostService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 社区动态控制器
 * 支持分页、标题/内容关键字搜索、按发布时间排序；仅能删除本人发布的动态
 *
 * @author diet
 */
@RestController
@RequestMapping("/api/post")
public class CommunityPostController {

    private final CommunityPostService postService;

    public CommunityPostController(CommunityPostService postService) {
        this.postService = postService;
    }

    /**
     * 分页查询社区动态(按发布时间倒序)
     *
     * @param current 当前页码
     * @param size    每页条数
     * @param keyword 标题/内容关键字(模糊匹配)
     * @param userId  发布者ID(可选，查看指定用户的动态)
     */
    @GetMapping("/page")
    public Result<IPage<CommunityPost>> page(@RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Long userId) {
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<CommunityPost>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(CommunityPost::getTitle, keyword)
                        .or()
                        .like(CommunityPost::getContent, keyword))
                .eq(userId != null, CommunityPost::getUserId, userId)
                .orderByDesc(CommunityPost::getPublishTime);
        return Result.success(postService.page(new Page<>(current, size), wrapper));
    }

    /**
     * 查询动态详情
     */
    @GetMapping("/{id}")
    public Result<CommunityPost> detail(@PathVariable Long id) {
        CommunityPost post = postService.getById(id);
        if (post == null) {
            throw new BusinessException("动态不存在");
        }
        return Result.success(post);
    }

    /**
     * 发布动态(自动绑定当前用户)
     */
    @PostMapping
    public Result<Void> save(@RequestBody CommunityPost post) {
        post.setId(null);
        post.setUserId(UserContext.get().getUserId());
        post.setLikeCount(0);
        post.setCommentCount(0);
        postService.save(post);
        return Result.success();
    }

    /**
     * 点赞(点赞数原子性+1)
     */
    @PostMapping("/{id}/like")
    public Result<Void> like(@PathVariable Long id) {
        CommunityPost post = postService.getById(id);
        if (post == null) {
            throw new BusinessException("动态不存在");
        }
        post.setLikeCount(post.getLikeCount() + 1);
        postService.updateById(post);
        return Result.success();
    }

    /**
     * 删除动态(仅允许删除本人发布的动态)
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean removed = postService.lambdaUpdate()
                .eq(CommunityPost::getId, id)
                .eq(CommunityPost::getUserId, UserContext.get().getUserId())
                .remove();
        if (!removed) {
            throw new BusinessException("动态不存在或无权删除");
        }
        return Result.success();
    }
}