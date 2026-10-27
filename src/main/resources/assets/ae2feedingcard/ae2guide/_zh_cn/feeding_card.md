---
navigation:
  title: "喂食卡"
  icon: ae2feedingcard:feeding_card
  position: 99
categories:
- tools
item_ids:
- ae2feedingcard:feeding_card
---

# 喂食卡

<ItemImage id="ae2feedingcard:feeding_card" scale="4" />

喂食卡是[无线终端](ae2:items-blocks-machines/wireless_terminals.md)的[升级卡](ae2:items-blocks-machines/upgrade_cards.md)。

它会从[网络存储](ae2:ae2-mechanics/import-export-storage.md)中抽出食物，并喂给你。

## 界面

喂食卡的UI分为若干部分。

中间部分是过滤槽，用于设置食物列表。

左侧部分有如下按钮：

*   打开详细设置窗口
*   装有<ItemLink id="ae2:redstone_card" />时，打开或关闭喂食卡

右侧部分是[升级卡](ae2:items-blocks-machines/upgrade_cards.md)槽位。

## 过滤

放入过滤槽的食物会加入白名单，也即只会吃其中指明的食物。名单为空时，喂食卡不会吃任何东西。

若没有食物过滤，可手持食物左击槽位，或从JEI/REI中拖拽以放入过滤槽。

<a name="settings"></a>

## 设置

“食物容器存入ME网络”和“食物容器放入物品栏”是两个独立开关，决定吃剩的容器先去哪。两个都放不下时，容器会掉落在脚下。

“进食时播放音效”只影响喂食卡触发的进食。

“主进食策略”有两种：

*   优先不浪费：按能填满饥饿缺口的比例分档，同档里挑浪费最少的
*   优先选高饱和度的：先挑补到的饱和度最高的，再挑浪费最少的

多个候选食物并列时，由“副进食策略”决定顺序。“副进食策略”有三种：

*   不管食物效果：效果不参与排序
*   优先无效果食物：无效果、有益、中性、有害
*   优先有益效果食物：有益、中性、无效果、有害

“复合效果处理”有两种：

*   只要包含有害效果就算有害
*   有益与有害按数量抵消：一个有益效果抵一个有害效果，抵完还剩有害才算有害

## 溢出销毁卡

“无法存储时销毁食物容器”决定“食物容器存入ME网络”和“食物容器放入物品栏”这两个开关都放不下的容器如何处理：勾选时销毁。

“饥饿值满时的行为”有四种：

*   停止进食
*   饱和度低于15时进食
*   饱和度低于10时进食
*   饱和度低于5时进食

选后三种时，无饱和度的食物不会被选中喂食。

## 合成卡

如果装有<ItemLink id="ae2:crafting_card" />，喂食卡会向[自动合成](ae2:ae2-mechanics/autocrafting.md)系统发送所需食物的合成请求。

“合成时机”有两种：

*   库存没有食物才合成
*   可合成项参与比较（较耗性能）

“合成期间”有两种行为：

*   照常吃其他食物
*   等待合成完成

每次只请求1个。能合成的东西默认每20游戏刻扫描一次，与进食冷却无关。规划或提交失败后默认等100游戏刻再试，这期间会回落到库存进食。

## 反向卡

装有<ItemLink id="ae2:inverter_card" />时，食物列表从白名单变为黑名单，也即只会排除其中指明的食物。若名单为空，喂食卡会吃网络中的所有食物。

“有害效果处理”有三种：

*   不排除有害效果
*   排除确定性有害效果
*   排除所有有害效果

## 模组兼容

装有生活调味料：胡萝卜版时，“未吃过食物的处理”有三种：

*   不干预
*   优先吃没吃过的
*   优先吃没吃过的，哪怕有害：没吃过的食物不受“有害效果处理”限制

## 升级

喂食卡支持如下[升级](ae2:items-blocks-machines/upgrade_cards.md)：

*   <ItemLink id="ae2:capacity_card" />增加过滤槽位数
*   <ItemLink id="ae2:fuzzy_card" />使得过滤忽略物品组件
*   <ItemLink id="ae2:inverter_card" />将白名单变为黑名单
*   <ItemLink id="ae2:speed_card" />减少冷却时间，每张卡将冷却时间乘以0.5
*   <ItemLink id="ae2:crafting_card" />启用合成功能
*   <ItemLink id="ae2:equal_distribution_card" />按固定顺序轮换进食，同时使主进食策略、副进食策略和复合效果处理不可用
*   <ItemLink id="ae2:void_card" />启用饥饿值满后的进食，并解锁容器的销毁
*   <ItemLink id="ae2:redstone_card" />在GUI中添加打开或关闭喂食卡的切换按钮

## 速度

| 加速卡数量 | 冷却              |
|:-----------|:------------------|
| 0          | 200游戏刻（10秒）  |
| 1          | 100游戏刻（5秒）   |
| 2          | 50游戏刻（2.5秒）  |
| 3          | 25游戏刻（1.25秒） |
| 4          | 12游戏刻（0.6秒）  |

## 配方

<RecipeFor id="ae2feedingcard:feeding_card" />
