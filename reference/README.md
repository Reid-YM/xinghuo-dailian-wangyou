# 反编译参考代码

`decompiled/com/xinghuo` 是从当前基线 JAR 反编译出的参考代码，用于阅读现有实现。

注意：

- 该目录不保证直接通过 `javac`，CFR 会在泛型、内部类和注解处理上产生差异。
- 修改已有类时，请复制到 `src/main/java/com/xinghuo/...` 修到可编译，再使用 `scripts/dev_build.sh`。
- 不要直接编辑此目录并期待构建脚本使用它；构建只编译 `src/main/java`。
