微信小游戏跳一跳自动跳
===

这是微信小游戏《跳一跳》的不正确玩法，通过截图，分析棋子的近似跳跃起始及结束点，计算出两点距离，并转换成时间，然后模拟发送长按事件。整个过程全自动，但不保证能一直玩下去。

本项目为 Idea Gradle 结构。

本项目仅用于学习研究，不提供直接运行的脚本或其他可执行文件。

目前存在的问题：

- 跳跃的距离有误差。
- ~~距离与时间的对应关系不够准确~~。(目前调优后，最后一次到17000分仍然停不下来）

# 效果图
![识别结果](./0.png)
![跳跃结果](./1.png)

----

享受用技术解决问题的过程，但不沉迷于实现外挂。