# 单元测试运行指南

## 文件位置

所有测试文件位于：
```
backend/src/test/java/com/smartelderly/service/location/
```

包含以下测试类：
1. **GeofenceAnalysisHelperTest.java** - 地理围栏分析工具测试
2. **LocationSummaryServiceTest.java** - 定位摘要服务测试
3. **ActivityAnalysisServiceTest.java** - 活动状态分析服务测试

---

## 运行方式

### 方式1：使用 Maven（推荐）

#### 运行所有测试
```bash
cd backend
mvn test
```

#### 运行特定测试类
```bash
mvn test -Dtest=GeofenceAnalysisHelperTest
mvn test -Dtest=LocationSummaryServiceTest
mvn test -Dtest=ActivityAnalysisServiceTest
```

#### 运行特定测试方法
```bash
mvn test -Dtest=GeofenceAnalysisHelperTest#testIsLocationWithinGeofences_LocationInside
```

### 方式2：使用 IDE (IntelliJ IDEA / Eclipse)

#### IntelliJ IDEA
1. 打开测试文件
2. 右键点击类名或方法名
3. 点击 "Run '测试类名'" 或 "Run '方法名'"
4. 或使用快捷键：`Ctrl + Shift + F10` (Windows/Linux) 或 `Ctrl + Shift + R` (Mac)

#### Eclipse
1. 打开测试文件
2. 右键点击类名或方法名
3. 选择 "Run As" → "JUnit Test"
4. 或使用快捷键：`Alt + Shift + X, T`

### 方式3：使用 Gradle（如果项目使用 Gradle）

```bash
gradle test
gradle test --tests GeofenceAnalysisHelperTest
```

---

## 测试覆盖哪些场景

### GeofenceAnalysisHelperTest（地理围栏测试）

| 测试方法 | 测试场景 |
|---------|--------|
| testIsLocationWithinGeofences_LocationInside | 位置在围栏范围内 → 返回 true |
| testIsLocationWithinGeofences_LocationOutside | 位置不在围栏范围内 → 返回 false |
| testIsLocationWithinGeofences_MultipleGeofences | 多个围栏，位置在其中一个 → 返回 true |
| testIsLocationWithinGeofences_EmptyGeofenceList | 空围栏列表 → 返回 false |
| testIsLocationWithinGeofences_LocationNull | 位置为 null → 返回 false |
| testCalculateDistance_SameLocation | 同一位置距离 → 接近 0 |
| testCalculateDistance_DifferentLocations | 不同位置距离 → 大于 0 |

### LocationSummaryServiceTest（定位摘要测试）

| 测试方法 | 测试场景 |
|---------|--------|
| testGetLocationSummary_Success_AtHome | 成功获取摘要 - 在家状态 |
| testGetLocationSummary_Success_Away | 成功获取摘要 - 外出状态 |
| testGetLocationSummary_ElderNotFound | 老人不存在 → 抛出异常 |
| testGetLocationSummary_LocationNotFound | 位置不存在 → 抛出异常 |
| testGetLocationSummary_ResponseDataCompleteness | 响应数据完整性 |

### ActivityAnalysisServiceTest（活动分析测试）

| 测试方法 | 测试场景 |
|---------|--------|
| testRunAnalysisJob_NoEnabledRules | 无启用规则 → 不生成告警 |
| testRunAnalysisJob_MultipleEnabledRules | 多个启用规则 → 正确处理 |
| testRunAnalysisJob_AtHomeWithinThreshold | 在家且未超过阈值 → 不生成告警 |
| testRunAnalysisJob_AtHomeExceedsThreshold | 在家且超过阈值 → 生成告警 |
| testRunAnalysisJob_AwayExceedsThreshold | 外出且超过阈值 → 生成告警 |
| testRunAnalysisJob_AlertMinIntervalRestriction | 最小间隔限制 → 不生成重复告警 |
| testRunAnalysisJob_ElderNotFound | 老人不存在 → 跳过 |

---

## 查看测试结果

### Maven 输出示例
```
[INFO] Tests run: 20, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.234 s
[INFO] BUILD SUCCESS
```

### IDE 输出
- 绿色 ✓：测试通过
- 红色 ✗：测试失败
- 黄色 ⊘：测试跳过

---

## 代码覆盖率检查

### 使用 JaCoCo 生成覆盖率报告

JaCoCo 插件已在 `pom.xml` 中配置，运行以下命令生成覆盖率报告：

```bash
mvn clean test
```

测试完成后，覆盖率报告会自动生成在：
```
backend/target/site/jacoco/index.html
```

### 查看覆盖率报告

1. **打开浏览器** 打开上述 HTML 文件
2. **查看覆盖率统计** - 可以看到代码行覆盖率、分支覆盖率等信息
3. **钻取具体文件** - 点击包名或类名查看详细的覆盖情况

---

## 常见问题

### Q1: 测试找不到依赖
**解决方案**：确保 `pom.xml` 中包含以下依赖：
```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.9.x</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.x</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <version>5.x</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

### Q2: 如何 Debug 测试？
**解决方案**：
- IntelliJ：右键测试 → "Debug '测试名'"
- Eclipse：右键测试 → "Debug As" → "JUnit Test"
- 或在测试方法中设置断点，使用 Debug 运行

### Q3: 怎样忽略某个测试？
**解决方案**：在测试方法上添加注解：
```java
@Disabled("暂时跳过此测试")
@Test
public void testSomething() {
    // ...
}
```

---

## 最佳实践

1. **定期运行测试** - 每次代码改动后都应运行相关测试
2. **保持高覆盖率** - 目标是达到 80%+ 的代码覆盖率
3. **清晰的测试命名** - 使用 `@DisplayName` 描述测试目的
4. **隔离测试** - 每个测试应该相互独立，不依赖执行顺序
5. **使用 Mock** - 通过 Mockito mock 掉外部依赖，确保单元测试的纯粹性

---

## 后续扩展

可以继续添加的测试：
- [ ] EmergencyAlertScheduler 的集成测试
- [ ] GuardRuleService 的单元测试
- [ ] 定时任务执行频率的测试
- [ ] 端到端的集成测试（使用 H2 内存数据库）
