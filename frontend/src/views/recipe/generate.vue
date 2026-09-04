<template>
  <el-card shadow="never">
    <!-- 四个功能Tab: 生成食谱 / 周食谱规划 / 购物清单 / 食谱改良 -->
    <el-tabs v-model="activeTab">
      <el-tab-pane label="生成食谱" name="generate">
        <GenerateTab @go-improve="activeTab = 'improve'" />
      </el-tab-pane>
      <el-tab-pane label="一周食谱规划" name="week">
        <WeekTab />
      </el-tab-pane>
      <el-tab-pane label="购物清单" name="shopping">
        <ShoppingTab />
      </el-tab-pane>
      <el-tab-pane label="食谱改良" name="improve">
        <ImproveTab />
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
import GenerateTab from './components/GenerateTab.vue'
import WeekTab from './components/WeekTab.vue'
import ShoppingTab from './components/ShoppingTab.vue'
import ImproveTab from './components/ImproveTab.vue'

// 当前激活的Tab
const activeTab = ref('generate')
</script>