package com.diet.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 社区动态视图对象(信息流卡片)
 *
 * @author diet
 */
@Data
public class PostFeedVO {

    /** 动态ID */
    private Long id;

    /** 作者信息 */
    private Long authorId;
    private String authorName;
    private String authorNickname;

    /** 标题/正文/图片 */
    private String title;
    private String content;
    private List<String> images;

    /** 互动数据 */
    private Integer likeCount;
    private Integer commentCount;
    private Integer isTop;

    /** 当前用户是否已点赞/已关注作者 */
    private Boolean liked;
    private Boolean followed;

    /** 关联食谱名称(分享食谱时) */
    private Long recipeId;
    private String recipeName;

    /** 发布时间 */
    private LocalDateTime publishTime;
}