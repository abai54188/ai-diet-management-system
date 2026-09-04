package com.diet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 体重记录实体类(对应 weight_record 表)
 *
 * @author diet
 */
@Data
@TableName("weight_record")
public class WeightRecord {

    /** 记录ID(主键自增) */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 体重(kg) */
    private BigDecimal weight;

    /** 记录日期 */
    private LocalDate recordDate;

    /** 创建时间 */
    private LocalDateTime createTime;
}