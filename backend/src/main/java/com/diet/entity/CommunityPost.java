package com.diet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 社区动态实体类(对应 community_post 表)
 *
 * @author diet
 */
@Data
@TableName("community_post")
public class CommunityPost {

    /** 动态ID(主键自增) */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发布用户ID */
    private Long userId;

    /** 标题 */
    private String title;

    /** 正文内容 */
    private String content;

    /** 图片URL列表(JSON数组) */
    private String images;

    /** 关联食谱ID(分享食谱时) */
    private Long recipeId;

    /** 点赞数 */
    private Integer likeCount;

    /** 是否置顶 */
    private Integer isTop;

    /** 评论数 */
    private Integer commentCount;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 逻辑删除: 0-未删除, 1-已删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间(插入时自动填充) */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间(插入/更新时自动填充) */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}