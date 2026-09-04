package com.diet.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 目标体重设置参数
 *
 * @author diet
 */
@Data
public class TargetWeightDTO {

    /** 目标体重(kg) */
    private BigDecimal targetWeight;
}